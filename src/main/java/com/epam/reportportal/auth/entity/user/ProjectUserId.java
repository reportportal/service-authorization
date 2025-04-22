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

package com.epam.reportportal.auth.entity.user;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Andrei Varabyeu
 */
@Setter
@Getter
@Embeddable
public class ProjectUserId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "project_id")
  private Long projectId;

  public ProjectUserId() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectUserId that = (ProjectUserId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(projectId, that.projectId);
  }

  @Override
  public int hashCode() {

    return Objects.hash(userId, projectId);
  }
}
