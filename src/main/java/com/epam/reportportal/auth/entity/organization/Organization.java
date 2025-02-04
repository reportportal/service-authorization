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

package com.epam.reportportal.auth.entity.organization;

import com.epam.reportportal.auth.dao.converters.JpaInstantConverter;
import com.epam.reportportal.auth.entity.enums.OrganizationType;
import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Siarhei Hrabko
 */
@Entity
@Table(name = "organization", schema = "public")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Organization implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false, precision = 64)
  private Long id;

  @Column(name = "created_at", nullable = false)
  @Convert(converter = JpaInstantConverter.class)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @Convert(converter = JpaInstantConverter.class)
  private Instant updatedAt;

  @Column(name = "name")
  private String name;

  @Column(name = "organization_type")
  @Enumerated(EnumType.STRING)
  private OrganizationType organizationType;

  @Column(name = "slug")
  private String slug;

  @Column(name = "external_id")
  private String externalId;


  @Override
  public String toString() {
    return "Organization{" +
        "id=" + id +
        ", creationDate=" + createdAt +
        ", updatedAt=" + updatedAt +
        ", externalId=" + externalId +
        ", name='" + name + '\'' +
        ", organizationType=" + organizationType +
        ", slug='" + slug + '\'' +
        '}';
  }
}
