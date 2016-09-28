package com.epam.reportportal.auth.github;

import com.epam.ta.reportportal.database.BinaryData;
import com.epam.ta.reportportal.database.DataStorage;
import com.epam.ta.reportportal.database.dao.UserRepository;
import com.epam.ta.reportportal.database.entity.project.EntryType;
import com.epam.ta.reportportal.database.entity.user.User;
import com.epam.ta.reportportal.database.entity.user.UserRole;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Replicates GitHub account info with internal ReportPortal's database
 *
 * @author <a href="mailto:andrei_varabyeu@epam.com">Andrei Varabyeu</a>
 */
@Component
public class GitHubUserReplicator {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitHubUserReplicator.class);

	private final UserRepository userRepository;
	private final DataStorage dataStorage;

	public GitHubUserReplicator(UserRepository userRepository, DataStorage dataStorage) {
		this.userRepository = userRepository;
		this.dataStorage = dataStorage;
	}

	public User replicateUser(String accessToken) {
		GitHubClient gitHubClient = GitHubClient.withAccessToken(accessToken);
		UserResource userInfo = gitHubClient.getUser();
		String login = userInfo.login;
		User user = userRepository.findOne(login);
		if (null == user) {
			user = new User();
			user.setLogin(login);
			user.setDefaultProject("default_project");

			String email = userInfo.email;
			if (!Strings.isNullOrEmpty(email)) {
				user.setEmail(email);
			} else {
				user.setEmail(
						gitHubClient.getUserEmails().stream().filter(EmailResource::isVerified).filter(EmailResource::isPrimary).findAny()
								.get().getEmail());
			}


			if (!Strings.isNullOrEmpty(userInfo.name)) {
				user.setEmail(userInfo.name);
			}

			User.MetaInfo metaInfo = new User.MetaInfo();
			Date now = Date.from(ZonedDateTime.now().toInstant());
			metaInfo.setLastLogin(now);
			metaInfo.setSynchronizationDate(now);
			user.setMetaInfo(metaInfo);

			user.setEntryType(EntryType.INTERNAL);
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
		}
		return user;
	}
}
