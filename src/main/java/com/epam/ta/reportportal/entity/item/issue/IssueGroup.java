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

package com.epam.ta.reportportal.entity.item.issue;

import com.epam.ta.reportportal.entity.enums.TestItemIssueGroup;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

/**
 * @author Pavel Bortnik
 */
@Setter
@Getter
@Entity
@Table(name = "issue_group", schema = "public")
public class IssueGroup implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "issue_group_id")
  private Integer id;

  @Column(name = "issue_group")
  @Enumerated(EnumType.STRING)
  @Type(type = "pqsql_enum")
  private TestItemIssueGroup testItemIssueGroup;

  public IssueGroup() {
  }

  public IssueGroup(TestItemIssueGroup testItemIssueGroup) {
    this.testItemIssueGroup = testItemIssueGroup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueGroup that = (IssueGroup) o;
    return testItemIssueGroup == that.testItemIssueGroup;
  }

  @Override
  public int hashCode() {

    return Objects.hash(testItemIssueGroup);
  }
}
