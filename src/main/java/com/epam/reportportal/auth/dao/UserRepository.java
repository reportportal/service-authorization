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

import com.epam.reportportal.auth.entity.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Ivan Budayeu
 */
public interface UserRepository extends ReportPortalRepository<User, Long> {

  Optional<User> findByEmail(String email);

  /**
   * @param login user login for search
   * @return {@link Optional} of {@link User}
   */
  Optional<User> findByLogin(String login);


  /**
   * Find users by userName regex pattern
   *
   * @param regex username regex
   * @return list of usernames matching pattern
   */
  @Query(value = "SELECT login FROM users WHERE login ~ ?1", nativeQuery = true)
  List<String> findByLoginRegex(String regex);

  /**
   * Updates user's last login value
   *
   * @param username User
   */
  @Modifying(clearAutomatically = true)
  @Query(value = "UPDATE users SET metadata = jsonb_set(metadata, '{metadata,last_login}', to_jsonb(round(extract(EPOCH from clock_timestamp()) * 1000)), TRUE ) WHERE login = :username", nativeQuery = true)
  void updateLastLoginDate(@Param("username") String username);

  @Query(value = """
          SELECT * FROM users
          WHERE
              type = :type
              AND (
                  (metadata #>> '{metadata,synchronizationDate}') IS NOT NULL
                  AND TO_TIMESTAMP(
                      (metadata #>> '{metadata,synchronizationDate}')::bigint / 1000
                  ) < :deadline
                  OR
                  (metadata #>> '{metadata,synchronizationDate}') IS NULL
              )
          ORDER BY
              CASE
                  WHEN metadata #>> '{metadata,synchronizationDate}' IS NULL THEN 1
                  ELSE 0
              END,
              (metadata #>> '{metadata,synchronizationDate}')::bigint DESC
          OFFSET :offset ROWS FETCH NEXT :pageSize ROWS ONLY
      """,
      countQuery = """
              SELECT COUNT(*) FROM users
              WHERE
                  type = :type
                  AND (
                      (metadata #>> '{metadata,synchronizationDate}') IS NOT NULL
                      AND TO_TIMESTAMP(
                          (metadata #>> '{metadata,synchronizationDate}')::bigint / 1000
                      ) < :deadline
                      OR
                      (metadata #>> '{metadata,synchronizationDate}') IS NULL
                  )
          """,
      nativeQuery = true)
  List<User> findNotSynchronizedUsers(
      @Param("type") String type,
      @Param("deadline") LocalDateTime deadline,
      @Param("offset") int offset,
      @Param("pageSize") int pageSize
  );

}
