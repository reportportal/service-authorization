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

package com.epam.ta.reportportal.entity.integration;

import com.epam.ta.reportportal.dao.converters.JpaInstantConverter;
import com.epam.ta.reportportal.entity.enums.IntegrationAuthFlowEnum;
import com.epam.ta.reportportal.entity.enums.IntegrationGroupEnum;
import com.epam.ta.reportportal.entity.enums.PostgreSQLEnumType;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;

/**
 * @author Yauheni_Martynau
 */
@Setter
@Getter
@Entity
@TypeDef(name = "details", typeClass = IntegrationTypeDetails.class)
@TypeDef(name = "pgsql_enum", typeClass = PostgreSQLEnumType.class)
@Table(name = "integration_type", schema = "public")
public class IntegrationType implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", unique = true, nullable = false, precision = 64)
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Type(type = "pqsql_enum")
  @Column(name = "auth_flow")
  private IntegrationAuthFlowEnum authFlow;

  @CreatedDate
  @Column(name = "creation_date", nullable = false)
  @Convert(converter = JpaInstantConverter.class)
  private Instant creationDate;

  @Enumerated(EnumType.STRING)
  @Type(type = "pqsql_enum")
  @Column(name = "group_type", nullable = false)
  private IntegrationGroupEnum integrationGroup;

  @Column(name = "enabled")
  private boolean enabled;

  @Type(type = "details")
  @Column(name = "details")
  private IntegrationTypeDetails details;

  @OneToMany(mappedBy = "type", fetch = FetchType.LAZY, orphanRemoval = true)
  private Set<Integration> integrations = Sets.newHashSet();

}
