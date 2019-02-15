/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-authorization
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.auth.integration.github;

import com.epam.reportportal.auth.integration.AbstractUserReplicator;
import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.ta.reportportal.BinaryData;
import com.epam.ta.reportportal.commons.validation.BusinessRule;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.Metadata;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.entity.user.UserRole;
import com.epam.ta.reportportal.entity.user.UserType;
import com.epam.ta.reportportal.filesystem.DataStore;
import com.epam.ta.reportportal.util.PersonalProjectService;
import com.epam.ta.reportportal.ws.model.ErrorType;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.epam.ta.reportportal.commons.EntityUtils.normalizeId;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Optional.ofNullable;

/**
 * Replicates GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class GitHubUserReplicator extends AbstractUserReplicator {

	@Autowired
	public GitHubUserReplicator(UserRepository userRepository, ProjectRepository projectRepository, DataStore dataStorage,
			PersonalProjectService personalProjectService) {
		super(userRepository, projectRepository, personalProjectService, dataStorage);
	}

	public User synchronizeUser(String accessToken) {
		GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
		UserResource userResource = gitHubClient.getUser();
		Optional<User> userOptional = userRepository.findByLogin(normalizeId(userResource.login));
		BusinessRule.expect(userOptional, Optional::isPresent).verify(ErrorType.USER_NOT_FOUND, userResource.login);
		User user = userOptional.get();
		BusinessRule.expect(user.getUserType(), userType -> Objects.equals(userType, UserType.GITHUB))
				.verify(ErrorType.INCORRECT_AUTHENTICATION_TYPE, "User '" + userResource.login + "' is not GitHUB user");
		user.setFullName(userResource.name);
		Metadata metadata = ofNullable(user.getMetadata()).orElse(new Metadata(Maps.newHashMap()));
		metadata.getMetadata().put("synchronizationDate", Date.from(ZonedDateTime.now().toInstant()));
		user.setMetadata(metadata);

		String newPhotoId = uploadAvatar(gitHubClient, userResource.login, userResource.avatarUrl);
		if (!Strings.isNullOrEmpty(newPhotoId)) {
			dataStorage.delete(user.getAttachment());
			user.setAttachment(newPhotoId);
		}
		userRepository.save(user);
		return user;
	}

	/**
	 * Replicates GitHub user to internal database (if does NOT exist). Updates if exist. Creates personal project for that user
	 *
	 * @param accessToken Access token to access GitHub
	 * @return Internal User representation
	 */
	public User replicateUser(String accessToken) {
		GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
		UserResource userInfo = gitHubClient.getUser();
		return replicateUser(userInfo, gitHubClient);
	}

	/**
	 * Replicates GitHub user to internal database (if does NOT exist). Updates if exist. Creates personal project for that user
	 *
	 * @param userResource GitHub user to be replicated
	 * @param gitHubClient Configured github client
	 * @return Internal User representation
	 */
	public User replicateUser(UserResource userResource, GitHubClient gitHubClient) {
		String login = normalizeId(userResource.login);
		Optional<User> userOptional = userRepository.findByLogin(login);
		boolean isExist = userOptional.isPresent();
		User user;
		if (!isExist) {
			user = new User();
			fillUserData(user, userResource, gitHubClient);
			fillDefaultUserData(user);
			userRepository.save(user);
		} else {
			user = userOptional.get();
			if (UserType.GITHUB.equals(user.getUserType())) {
				dataStorage.delete(user.getAttachment());
				fillUserData(user, userResource, gitHubClient);
			} else {
				//if user with such login exists, but it's not GitHub user than throw an exception
				throw new UserSynchronizationException("User with login '" + user.getId() + "' already exists");
			}
		}
		return user;
	}

	private void fillUserData(User user, UserResource userResource, GitHubClient gitHubClient) {
		String login = normalizeId(userResource.login);
		user.setLogin(login);
		String email = userResource.email;
		if (Strings.isNullOrEmpty(email)) {
			email = gitHubClient.getUserEmails()
					.stream()
					.filter(EmailResource::isVerified)
					.filter(EmailResource::isPrimary)
					.findAny()
					.get()
					.getEmail();
		}
		email = normalizeId(email);
		checkEmail(email);
		user.setEmail(email);
		user.setFullName(isNullOrEmpty(user.getFullName()) ? user.getLogin() : user.getFullName());
		user.setMetadata(defaultMetaData());
		Object avatarUrl = userResource.avatarUrl;
		user.setAttachment(uploadAvatar(gitHubClient, login, avatarUrl));
	}

	private void fillDefaultUserData(User user) {
		user.setUserType(UserType.GITHUB);
		user.setRole(UserRole.USER);
		user.setExpired(false);
		final Project project = generatePersonalProject(user);
		user.getProjects().add(project.getUsers().iterator().next());
	}

	private String uploadAvatar(GitHubClient gitHubClient, String login, Object avatarUrl) {
		String photoId = null;
		if (null != avatarUrl) {
			ResponseEntity<Resource> photoRs = gitHubClient.downloadResource(avatarUrl.toString());
			try (InputStream photoStream = photoRs.getBody().getInputStream()) {
				BinaryData photo = new BinaryData(photoRs.getHeaders().getContentType().toString(),
						photoRs.getBody().contentLength(),
						photoStream
				);
				photoId = uploadPhoto(login, photo);
			} catch (IOException e) {
				LOGGER.error("Unable to load photo for user {}", login);
			}
		}
		return photoId;
	}
}
