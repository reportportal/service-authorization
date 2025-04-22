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

import com.epam.reportportal.auth.entity.project.Project;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends ReportPortalRepository<Project, Long> {

  Optional<Project> findByName(String name);

  boolean existsByName(String name);

  @Query(value = "SELECT p.* FROM project p JOIN project_user pu on p.id = pu.project_id JOIN users u on pu.user_id = u.id WHERE u.login = :login", nativeQuery = true)
  List<Project> findUserProjects(@Param("login") String login);

  @Query(value = "SELECT p.* FROM project p JOIN project_user pu on p.id = pu.project_id JOIN users u on pu.user_id = u.id WHERE u.login = :login AND p.project_type = :projectType", nativeQuery = true)
  List<Project> findUserProjects(@Param("login") String login,
      @Param("projectType") String projectType);

  @Modifying
  @Query(value = """
          DELETE FROM project 
          WHERE id IN (
              SELECT p.id 
              FROM project p
              JOIN launch l ON p.id = l.project_id
              WHERE p.project_type = :projectType
              GROUP BY p.id
              HAVING MAX(l.start_time) <= :bound
              LIMIT :limit
          )
      """, nativeQuery = true)
  void deleteByTypeAndLastLaunchRunBefore(
      @Param("projectType") String projectType,
      @Param("bound") Instant bound,
      @Param("limit") int limit
  );

}
