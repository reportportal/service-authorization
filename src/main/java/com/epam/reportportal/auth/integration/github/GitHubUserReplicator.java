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

import com.epam.ta.reportportal.database.BinaryData;
import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.dao.ProjectRepository;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.Project;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.epam.ta.reportportal.database.entity.user.UserType;
import com.epam.ta.reportportal.database.personal.PersonalProjectUtils;
import com.epam.ta.reportportal.database.search.Filter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Date;

import static com.epam.ta.reportportal.database.search.FilterCondition.builder;

/**
 * Replicates GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class GitHubUserReplicator {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitHubUserReplicator.class);

	private final UserRepository userRepository;
	private final ProjectRepository projectRepository;
	private final DataStorage dataStorage;

	@Autowired
	public GitHubUserReplicator(UserRepository userRepository, ProjectRepository projectRepository, DataStorage dataStorage) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.dataStorage = dataStorage;
	}

	/**
	 * Replicates GitHub user to internal database (if does NOT exist). Creates personal project for that user
	 *
	 * @param accessToken Access token to access GitHub
	 * @return Internal User representation
	 */
	public User replicateUser(String accessToken) {
		GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
		UserResource userInfo = gitHubClient.getUser();
		String login = userInfo.login;
		User user = userRepository.findOne(login);
		if (null == user) {
			user = new User();
			user.setLogin(login);

			String email = userInfo.email;
			if (Strings.isNullOrEmpty(email)) {
				email =
						gitHubClient.getUserEmails().stream().filter(EmailResource::isVerified).filter(EmailResource::isPrimary).findAny()
								.get().getEmail();
			}
			if (userRepository.exists(Filter.builder().withTarget(User.class).withCondition(builder().eq("email", email).build()).build())){
				throw new EmailAlreadyExistsException("User with email '" + email + "' already exists");
			}
			user.setEmail(email.toLowerCase());


			if (!Strings.isNullOrEmpty(userInfo.name)) {
				user.setFullName(userInfo.name);
			}

			User.MetaInfo metaInfo = new User.MetaInfo();
			Date now = Date.from(ZonedDateTime.now().toInstant());
			metaInfo.setLastLogin(now);
			metaInfo.setSynchronizationDate(now);
			user.setMetaInfo(metaInfo);

			user.setType(UserType.GITHUB);
			user.setRole(UserRole.USER);
			Object avatar_url = userInfo.avatarUrl;
			if (null != avatar_url) {
				ResponseEntity<Resource> photoRs = gitHubClient.downloadResource(avatar_url.toString());
				try (InputStream photoStream = photoRs.getBody().getInputStream()) {
					BinaryData photo = new BinaryData(photoRs.getHeaders().getContentType().toString(), photoRs.getBody().contentLength(),
							photoStream);
					String photoId = dataStorage.saveData(photo, photoRs.getBody().getFilename());
					user.setPhotoId(photoId);
				} catch (IOException e) {
					LOGGER.error("Unable to load photo for user {}", login);
				}
			}

			user.setIsExpired(false);

			user.setDefaultProject(generatePersonalProject(user).getId());
			userRepository.save(user);

		}
		return user;
	}

	/**
	 * Generates personal project if does NOT exists
	 *
	 * @param user Owner of personal project
	 * @return Created project
	 */
	private Project generatePersonalProject(User user) {
		String personalProjectName = PersonalProjectUtils.personalProjectName(user.getLogin());
		Project personalProject = projectRepository.findOne(personalProjectName);
		if (null == personalProject) {
			personalProject = PersonalProjectUtils.generatePersonalProject(user);
			projectRepository.save(personalProject);
		}
		return personalProject;
	}


	public static class EmailAlreadyExistsException extends AuthenticationException {

		public EmailAlreadyExistsException(String msg) {
			super(msg);
		}
	}
}
