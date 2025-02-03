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

package com.epam.reportportal;

import com.epam.reportportal.auth.config.TestConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;


/**
 * Base class for all test classes. It sets up the PostgreSQL container and applies migration
 * scripts. It also configures dynamic properties for the test context.
 */
@Testcontainers
@Log4j2
@SpringBootTest(classes = TestConfig.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles({"unittest"})
public abstract class BaseTest {

  private static final String MIGRATIONS_PATH = "migrations/migrations/";
  public static final String DB_MIGRATION_PATH = "build/resources/test/db/migration";

  private static final String POSTGRES_CONTAINER = "postgres:16-alpine";
  private static final String LDAP_CONTAINER = "bitnami/openldap:latest";
  private static final String MINIO_CONTAINER = "minio/minio";


  @LocalServerPort
  private Integer port;

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_CONTAINER)
      .withAccessToHost(true)
      .withDatabaseName("reportportal");

  static {
    applyMigrationScripts();
    postgres.start();
    log.info("PostgreSQL container started on port: {}", postgres.getFirstMappedPort());
  }

  @SneakyThrows
  private static void applyMigrationScripts() {
    try (Stream<Path> stream = Files.list(Paths.get(MIGRATIONS_PATH))) {
      log.info("Copy database scripts");
      stream
          .filter(file -> !Files.isDirectory(file))
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(filename -> filename.contains(".up.sql"))
          .map(filename -> {
            int n = 3 - filename.indexOf("_");
            return StringUtils.repeat(" ", n) + filename;
          })
          .forEach(filename -> {
            try {
              var file = new File(
                  DB_MIGRATION_PATH + "/V" + filename.replace(" ", "0"));
              FileUtils.writeStringToFile(file, "\n"
                  + FileUtils.readFileToString(new File(MIGRATIONS_PATH + filename.trim()),
                  StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });

      // get sorted list of migration files
      try (Stream<Path> copiedFiles = Files.list(Paths.get(DB_MIGRATION_PATH))) {
        var list = copiedFiles
            .map(Path::getFileName)
            .map(Path::toString)
            .sorted()
            .peek(System.out::println)
            .map(filename -> "db/migration/" + filename)
            .toList();

        postgres.withInitScripts(list);

      }

    }
  }


  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("rp.datasource.jdbcUrl", postgres::getJdbcUrl);
    registry.add("rp.datasource.username", postgres::getUsername);
    registry.add("rp.datasource.password", postgres::getPassword);
    registry.add("rp.initial.admin.password", () -> "testpassword");
  }

}





