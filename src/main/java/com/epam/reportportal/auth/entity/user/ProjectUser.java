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

import com.epam.reportportal.auth.entity.enums.PostgreSQLEnumType;
import com.epam.reportportal.auth.entity.project.Project;
import com.epam.reportportal.auth.entity.project.ProjectRole;
import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * @author Andrei Varabyeu
 */
@Setter
@Getter
@Entity
@Table(name = "project_user", schema = "public")
public class ProjectUser implements Serializable {

  @EmbeddedId
  private ProjectUserId id = new ProjectUserId();

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("projectId")
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  private User user;

  @Column(name = "project_role")
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private ProjectRole projectRole;

  public ProjectUser withProjectUserId(ProjectUserId id) {
    this.id = id;
    return this;
  }

  public ProjectUser withUser(User user) {
    this.user = user;
    return this;
  }

  public ProjectUser withProject(Project project) {
    this.project = project;
    return this;
  }

  public ProjectUser withProjectRole(ProjectRole projectRole) {
    this.projectRole = projectRole;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectUser that = (ProjectUser) o;
    return Objects.equals(id, that.id) && Objects.equals(project, that.project) && Objects.equals(
        user, that.user)
        && projectRole == that.projectRole;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, project, user, projectRole);
  }
}
