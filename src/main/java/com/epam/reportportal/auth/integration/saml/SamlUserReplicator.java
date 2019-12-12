/*
 * Copyright 2019 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.reportportal.auth.integration.saml;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.SamlProviderDetailsRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.saml.SamlProviderDetails;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.util.PersonalProjectService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.reportportal.auth.util.AuthUtils.CROP_DOMAIN;

/**
 * Replicates user from SAML response into database if it is not exist
 *
 * @author Yevgeniy Svalukhin
 */
@Component
@Transactional
public class SamlUserReplicator extends AbstractUserReplicator {

	private SamlProviderDetailsRepository samlProviderDetailsRepository;

	public SamlUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
			PersonalProjectService personalProjectService, UserBinaryDataService userBinaryDataService,
			SamlProviderDetailsRepository samlProviderDetailsRepository) {
		super(userRepository, projectRepository, personalProjectService, userBinaryDataService);
		this.samlProviderDetailsRepository = samlProviderDetailsRepository;
	}

	public User replicateUser(ReportPortalSamlAuthentication samlAuthentication) {
		String userName = samlAuthentication.getPrincipal();
		Optional<User> userOptional = userRepository.findByLogin(userName);

		if (userOptional.isPresent()) {
			return userOptional.get();
		}

		List<SamlProviderDetails> providers = samlProviderDetailsRepository.findAll();

		Optional<SamlProviderDetails> samlProvider = providers.stream()
				.filter(provider -> provider.getIdpUrl().equalsIgnoreCase(samlAuthentication.getIssuer()))
				.findFirst();

		User user = new User();
		user.setLogin(userName);

		List<Attribute> details = samlAuthentication.getDetails();

		if (samlProvider.isPresent()) {
			populateUserDetailsIfSettingsArePresent(user, samlProvider.get(), details);
		} else {
			populateUserDetails(user, details);
		}

		user.setUserType(UserType.SAML);
		user.setRole(UserRole.USER);
		user.setExpired(false);

		Project project = generatePersonalProject(user);
		user.getProjects().add(project.getUsers().iterator().next());

		user.setMetadata(defaultMetaData());

		userRepository.save(user);

		return user;
	}

	private void populateUserDetails(User user, List<Attribute> details) {
		String email = findAttributeValue(details, UserAttribute.EMAIL.toString(), String.class);
		checkEmail(email);
		user.setEmail(email);

		String firstName = findAttributeValue(details, UserAttribute.FIRST_NAME.toString(), String.class);
		String lastName = findAttributeValue(details, UserAttribute.LAST_NAME.toString(), String.class);
		user.setFullName(String.join(" ", firstName, lastName));
	}

	private void populateUserDetailsIfSettingsArePresent(User user, SamlProviderDetails providerDetails, List<Attribute> details) {
		String email = findAttributeValue(details, providerDetails.getEmailAttributeId(), String.class);
		checkEmail(email);
		user.setEmail(email);

		if (StringUtils.isEmpty(providerDetails.getFullNameAttributeId())) {
			String firstName = findAttributeValue(details, providerDetails.getFirstNameAttributeId(), String.class);
			String lastName = findAttributeValue(details, providerDetails.getLastNameAttributeId(), String.class);
			user.setFullName(String.join(" ", firstName, lastName));
		} else {
			String fullName = findAttributeValue(details, providerDetails.getFullNameAttributeId(), String.class);
			user.setFullName(fullName);
		}
	}

	private <T> T findAttributeValue(List<Attribute> attributes, String lookingFor, Class<T> castTo) {
		if (CollectionUtils.isEmpty(attributes)) {
			return null;
		}

		Optional<Attribute> attribute = attributes.stream().filter(it -> it.getName().equalsIgnoreCase(lookingFor)).findFirst();

		if (attribute.isPresent()) {
			List<Object> values = attribute.get().getValues();
			if (!CollectionUtils.isEmpty(values)) {
				List<T> resultList = values.stream().filter(castTo::isInstance).map(castTo::cast).collect(Collectors.toList());
				if (!resultList.isEmpty()) {
					return resultList.get(0);
				}
			}
		}
		return null;
	}
}
