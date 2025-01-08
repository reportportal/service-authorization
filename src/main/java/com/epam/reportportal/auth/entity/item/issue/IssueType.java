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

package com.epam.reportportal.auth.entity.item.issue;

import com.epam.reportportal.auth.entity.enums.PostgreSQLEnumType;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;

/**
 * @author Pavel Bortnik
 */
@Setter
@Getter
@Entity
@TypeDef(name = "pqsql_enum", typeClass = PostgreSQLEnumType.class)
@Table(name = "issue_type", schema = "public", indexes = {
    @Index(name = "issue_type_pk", unique = true, columnList = "id ASC")})
public class IssueType implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false)
  private Long id;

  @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JoinColumn(name = "issue_group_id")
  private IssueGroup issueGroup;

  @Column(name = "locator", length = 64)
  private String locator;

  @Column(name = "issue_name", length = 256)
  private String longName;

  @Column(name = "abbreviation", length = 64)
  private String shortName;

  @Column(name = "hex_color", length = 7)
  private String hexColor;

  public IssueType() {
  }

  public IssueType(IssueGroup issueGroup, String locator, String longName, String shortName,
      String hexColor) {
    this.issueGroup = issueGroup;
    this.locator = locator;
    this.longName = longName;
    this.shortName = shortName;
    this.hexColor = hexColor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueType issueType = (IssueType) o;
    return Objects.equals(id, issueType.id) && Objects.equals(issueGroup, issueType.issueGroup)
        && Objects.equals(locator,
        issueType.locator
    ) && Objects.equals(longName, issueType.longName) && Objects.equals(
        shortName,
        issueType.shortName
    ) && Objects.equals(hexColor, issueType.hexColor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, issueGroup, locator, longName, shortName, hexColor);
  }
}
