/*
 * Copyright 2023 EPAM Systems
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

package com.epam.reportportal.auth.config;

import com.epam.reportportal.auth.commons.ReportPortalUser;
import com.epam.reportportal.auth.entity.user.UserRole;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import jakarta.servlet.ServletContext;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.utils.SpringDocUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@Configuration
@ComponentScan(basePackages = "com.epam.reportportal.auth")
public class SpringDocConfiguration {

  static {
    SpringDocUtils.getConfig().addAnnotationsToIgnore(AuthenticationPrincipal.class);
    SpringDocUtils.getConfig().addRequestWrapperToIgnore(ReportPortalUser.class, UserRole.class);
    SpringDocUtils.getConfig().replaceWithClass(org.springframework.data.domain.Pageable.class,
        org.springdoc.core.converters.models.Pageable.class);
  }

  @Autowired
  private ServletContext servletContext;

  @Value("${info.build.version}")
  private String buildVersion;

  @Value("${server.servlet.context-path:/uat}")
  private String pathValue;

  @Bean
  public OpenAPI openAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI()
        .info(new Info().title("ReportPortal")
            .description("ReportPortal UAT documentation")
            .version(buildVersion)
            .contact(new Contact()
                .name("Support")
                .email("support@reportportal.io")
            )
            .license(new License().name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
        )
        .addServersItem(new Server().url(getPathValue()));
  }

  @Bean
  public OpenApiCustomizer sortSchemasAlphabetically() {
    return openApi -> {
      Map<String, Schema> schemas = openApi.getComponents().getSchemas();
      openApi.getComponents().setSchemas(new TreeMap<>(schemas));
    };
  }

  @Bean
  public OpenApiCustomizer sortTagsAlphabetically() {
    return openApi -> {
      List<Tag> sortedTags = openApi.getTags().stream()
          .sorted(Comparator.comparing(Tag::getName))
          .collect(Collectors.toList());
      openApi.setTags(sortedTags);
    };
  }

  private String getPathValue() {
    return StringUtils.isEmpty(pathValue) || pathValue.equals("/") ? "/uat" : pathValue;
  }
}
