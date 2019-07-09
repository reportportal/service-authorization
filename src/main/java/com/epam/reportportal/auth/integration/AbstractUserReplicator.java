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
package com.epam.reportportal.auth.integration;

import com.epam.reportportal.auth.oauth.UserSynchronizationException;
import com.epam.ta.reportportal.BinaryData;
import com.epam.ta.reportportal.dao.ProjectRepository;
import com.epam.ta.reportportal.dao.UserRepository;
import com.epam.ta.reportportal.entity.project.Project;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.filesystem.DataEncoder;
import com.epam.ta.reportportal.filesystem.DataStore;
import com.epam.ta.reportportal.util.PersonalProjectService;
import com.google.common.collect.Maps;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.image.ImageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author Andrei Varabyeu
 */
public class AbstractUserReplicator {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractUserReplicator.class);

	protected final UserRepository userRepository;
	protected final ProjectRepository projectRepository;
	protected final PersonalProjectService personalProjectService;
	protected final DataStore dataStorage;
	protected final DataEncoder encoder;

	public AbstractUserReplicator(UserRepository userRepository, ProjectRepository projectRepository,
			PersonalProjectService personalProjectService, DataStore dataStorage, DataEncoder encoder) {
		this.userRepository = userRepository;
		this.projectRepository = projectRepository;
		this.personalProjectService = personalProjectService;
		this.dataStorage = dataStorage;
		this.encoder = encoder;
	}

	/**
	 * Generates personal project if does NOT exists
	 *
	 * @param user Owner of personal project
	 * @return Created project name
	 */
	protected Project generatePersonalProject(User user) {
		return projectRepository.findByName(personalProjectService.getProjectPrefix(user.getLogin()))
				.orElse(generatePersonalProjectByUser(user));
	}

	/**
	 * Generates default metainfo
	 *
	 * @return Default meta info
	 */
	protected com.epam.ta.reportportal.entity.Metadata defaultMetaData() {
		Map<String, Object> metaDataMap = new HashMap<>();
		long nowInMillis = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli();
		metaDataMap.put("lastLogin", nowInMillis);
		metaDataMap.put("synchronizationDate", nowInMillis);
		return new com.epam.ta.reportportal.entity.Metadata(metaDataMap);
	}

	/**
	 * Updates last syncronization data for specified user
	 *
	 * @param user User to be synchronized
	 */
	protected void updateSynchronizationDate(User user) {
		com.epam.ta.reportportal.entity.Metadata metadata = ofNullable(user.getMetadata()).orElse(new com.epam.ta.reportportal.entity.Metadata(
				Maps.newHashMap()));
		metadata.getMetadata().put("synchronizationDate", LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli());
		user.setMetadata(metadata);
	}

	/**
	 * Checks email is available
	 *
	 * @param email email to check
	 */
	protected void checkEmail(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new UserSynchronizationException("User with email '" + email + "' already exists");
		}
	}

	protected String uploadPhoto(String login, byte[] data) {
		return uploadPhoto(login, data, resolveContentType(data));
	}

	protected String uploadPhoto(String login, byte[] data, String contentType) {
		BinaryData photo = new BinaryData(contentType, (long) data.length, new ByteArrayInputStream(data));
		return uploadPhoto(login, photo);
	}

	protected String uploadPhoto(String login, BinaryData data) {
		return encoder.encode(dataStorage.save(login, data.getInputStream()));
	}

	private String resolveContentType(byte[] data) {
		AutoDetectParser parser = new AutoDetectParser(new ImageParser());
		try {
			return parser.getDetector().detect(TikaInputStream.get(data), new Metadata()).toString();
		} catch (IOException e) {
			return MediaType.OCTET_STREAM.toString();
		}
	}

	private Project generatePersonalProjectByUser(User user) {
		Project personalProject = personalProjectService.generatePersonalProject(user);
		return projectRepository.save(personalProject);
	}
}
