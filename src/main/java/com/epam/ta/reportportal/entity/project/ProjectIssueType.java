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

package com.epam.ta.reportportal.entity.project;

import com.epam.ta.reportportal.entity.item.issue.IssueType;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Pavel Bortnik
 */
@Setter
@Getter
@Entity
@Table(name = "issue_type_project")
@IdClass(ProjectIssueTypeKey.class)
public class ProjectIssueType implements Serializable {

  @Id
  @ManyToOne
  @JoinColumn(name = "issue_type_id")
  private IssueType issueType;

  @Id
  @ManyToOne
  @JoinColumn(name = "project_id")
  private Project project;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectIssueType that = (ProjectIssueType) o;
    return Objects.equals(issueType, that.issueType) && Objects.equals(project, that.project);
  }

  @Override
  public int hashCode() {

    return Objects.hash(issueType, project);
  }
}
