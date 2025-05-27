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
package com.epam.reportportal.auth.dao;

import com.epam.reportportal.auth.entity.integration.Integration;
import com.epam.reportportal.auth.entity.integration.IntegrationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Ivan Budayeu
 * @author Andrei Varabyeu
 */
@Repository
public interface IntegrationRepository extends ReportPortalRepository<Integration, Long> {

  /**
   * @param name              {@link Integration#getName()}
   * @param integrationTypeId {@link Integration#getType()}#{@link IntegrationType#getId()}
   * @return {@link Optional} with {@link Integration}
   */
  Optional<Integration> findByNameAndTypeIdAndProjectIdIsNull(String name, Long integrationTypeId);

  /**
   * @param id                {@link Integration#getId()} ()}
   * @param integrationTypeId {@link Integration#getType()}#{@link IntegrationType#getId()}
   * @return {@link Optional} with {@link Integration}
   */
  Optional<Integration> findByIdAndTypeIdAndProjectIdIsNull(Long id, Long integrationTypeId);

  /**
   * Retrieve all {@link Integration} with {@link Integration#project} == null by integration type
   *
   * @param integrationType {@link Integration#type}
   * @return @return The {@link List} of the {@link Integration}
   */
  @Query(value = "SELECT i FROM Integration i WHERE i.project IS NULL AND i.type = :integrationType order by i.creationDate desc")
  List<Integration> findAllGlobalByType(@Param("integrationType") IntegrationType integrationType);


  @Query(value = "SELECT i.* FROM integration i LEFT OUTER JOIN integration_type it ON i.type = it.id WHERE it.name IN (:types) order by i.creation_date desc", nativeQuery = true)
  List<Integration> findAllByTypeIn(@Param("types") String... types);
}
