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

package com.epam.ta.reportportal.binary.impl;

import static com.epam.ta.reportportal.binary.impl.DataStoreUtils.ATTACHMENT_CONTENT_TYPE;
import static com.epam.ta.reportportal.binary.impl.DataStoreUtils.PHOTOS_PATH;
import static com.epam.ta.reportportal.binary.impl.DataStoreUtils.ROOT_USER_PHOTO_DIR;
import static com.epam.ta.reportportal.binary.impl.DataStoreUtils.USER_DATA_PATH;
import static com.epam.ta.reportportal.binary.impl.DataStoreUtils.buildThumbnailFileName;
import static java.util.Optional.ofNullable;

import com.epam.ta.reportportal.binary.DataStoreService;
import com.epam.ta.reportportal.binary.UserBinaryDataService;
import com.epam.ta.reportportal.entity.Metadata;
import com.epam.ta.reportportal.entity.attachment.BinaryData;
import com.epam.ta.reportportal.entity.enums.FeatureFlag;
import com.epam.ta.reportportal.entity.user.User;
import com.epam.ta.reportportal.util.FeatureFlagHandler;
import com.google.common.collect.Maps;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

/**
 * @author <a href="mailto:ihar_kahadouski@epam.com">Ihar Kahadouski</a>
 */
@Service
public class UserBinaryDataServiceImpl implements UserBinaryDataService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserBinaryDataServiceImpl.class);
  private static final String DEFAULT_USER_PHOTO = "image/defaultAvatar.png";

  private final DataStoreService dataStoreService;

  private final FeatureFlagHandler featureFlagHandler;

  @Autowired
  public UserBinaryDataServiceImpl(
      @Qualifier("userDataStoreService") DataStoreService dataStoreService,
      FeatureFlagHandler featureFlagHandler) {
    this.dataStoreService = dataStoreService;
    this.featureFlagHandler = featureFlagHandler;
  }


  @Override
  public void saveUserPhoto(User user, BinaryData binaryData) {
    saveUserPhoto(user, binaryData.getInputStream(), binaryData.getContentType());
  }

  @Override
  public void saveUserPhoto(User user, InputStream inputStream, String contentType) {
    try {
      byte[] data = StreamUtils.copyToByteArray(inputStream);
      try (InputStream userPhotoCopy = new ByteArrayInputStream(data);
          InputStream thumbnailCopy = new ByteArrayInputStream(data)) {
        if (featureFlagHandler.isEnabled(FeatureFlag.SINGLE_BUCKET)) {
          user.setAttachment(dataStoreService.save(
              Paths.get(USER_DATA_PATH, PHOTOS_PATH, user.getLogin()).toString(), userPhotoCopy));
          user.setAttachmentThumbnail(dataStoreService.saveThumbnail(
              buildThumbnailFileName(Paths.get(USER_DATA_PATH, PHOTOS_PATH).toString(),
                  user.getLogin()
              ), thumbnailCopy));
        } else {
          user.setAttachment(
              dataStoreService.save(Paths.get(ROOT_USER_PHOTO_DIR, user.getLogin()).toString(),
                  userPhotoCopy
              ));
          user.setAttachmentThumbnail(dataStoreService.saveThumbnail(
              buildThumbnailFileName(ROOT_USER_PHOTO_DIR, user.getLogin()), thumbnailCopy));
        }
      }
      ofNullable(user.getMetadata()).orElseGet(() -> new Metadata(Maps.newHashMap())).getMetadata()
          .put(ATTACHMENT_CONTENT_TYPE, contentType);
    } catch (IOException e) {
      LOGGER.error("Unable to save user photo", e);
    }
  }

}
