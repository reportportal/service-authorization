/*
 * Copyright 2024 EPAM Systems
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
import com.epam.reportportal.auth.entity.organization.Organization;
import com.epam.reportportal.auth.entity.organization.OrganizationRole;
import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

/**
 * @author Siarhei Hrabko
 */
@Entity
@TypeDef(name = "pqsql_enum", typeClass = PostgreSQLEnumType.class)
@Table(name = "organization_user", schema = "public")
public class OrganizationUser implements Serializable {

  @EmbeddedId
  private OrganizationUserId id = new OrganizationUserId();

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("organizationId")
  private Organization organization;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  private User user;

  @Column(name = "organization_role")
  @Enumerated(EnumType.STRING)
  @Type(type = "pqsql_enum")
  private OrganizationRole organizationRole;


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OrganizationUser)) {
      return false;
    }
    OrganizationUser that = (OrganizationUser) o;
    return organization.equals(that.organization)
        && id.equals(that.id)
        && user.equals(that.user)
        && organizationRole == that.organizationRole;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, organization, user, organizationRole);
  }

  public OrganizationUserId getId() {
    return id;
  }

  public void setId(OrganizationUserId id) {
    this.id = id;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public OrganizationRole getOrganizationRole() {
    return organizationRole;
  }

  public void setOrganizationRole(
      OrganizationRole organizationRole) {
    this.organizationRole = organizationRole;
  }
}
