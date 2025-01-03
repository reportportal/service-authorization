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

package com.epam.ta.reportportal.entity.attachment;

import com.epam.ta.reportportal.dao.converters.JpaInstantConverter;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
@Setter
@Getter
@Entity
@Table(name = "attachment")
public class Attachment implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "file_id")
  private String fileId;

  @Column(name = "thumbnail_id")
  private String thumbnailId;

  @Column(name = "content_type")
  private String contentType;

  @Column(name = "file_size")
  private long fileSize;

  @Column(name = "creation_date")
  @Convert(converter = JpaInstantConverter.class)
  private Instant creationDate;

  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "launch_id")
  private Long launchId;

  @Column(name = "item_id")
  private Long itemId;

  public Attachment() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Attachment that = (Attachment) o;
    return Objects.equals(fileId, that.fileId) && Objects.equals(thumbnailId, that.thumbnailId)
        && Objects.equals(contentType,
        that.contentType
    ) && Objects.equals(fileSize, that.fileSize) && Objects.equals(creationDate, that.creationDate)
        && Objects.equals(fileName, that.fileName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, thumbnailId, contentType, fileSize, creationDate, fileName);
  }
}
