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

package com.epam.reportportal.auth.entity.project;

import static java.util.Optional.ofNullable;

import com.epam.reportportal.auth.entity.attribute.Attribute;
import com.epam.reportportal.auth.entity.enums.ProjectAttributeEnum;
import com.epam.reportportal.auth.entity.enums.TestItemIssueGroup;
import com.epam.reportportal.auth.entity.item.issue.IssueType;
import com.epam.reportportal.auth.entity.user.ProjectUser;
import com.epam.reportportal.auth.rules.commons.validation.Suppliers;
import com.epam.reportportal.auth.rules.exception.ErrorType;
import com.epam.reportportal.auth.rules.exception.ReportPortalException;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Project related utility methods
 *
 * @author Andrei_Ramanchuk
 */
public class ProjectUtils {

  public static final String INIT_FROM = "reportportal@example.com";
  public static final String PERSONAL_PROJECT_POSTFIX_REGEX = "_personal(_?[0-9]+)?$";
  public static final String LINE_START_SYMBOL = "^";

  private static final String OWNER = "OWNER";

  private ProjectUtils() {

  }

  /**
   * @return Generated default project configuration
   */
  public static Set<ProjectAttribute> defaultProjectAttributes(Project project,
      Set<Attribute> defaultAttributes) {

    Map<String, Attribute> attributes = defaultAttributes.stream()
        .collect(Collectors.toMap(Attribute::getName, a -> a));

    Set<ProjectAttribute> projectAttributes = new HashSet<>(defaultAttributes.size());

    Arrays.stream(ProjectAttributeEnum.values())
        .map(ProjectAttributeEnum::getAttribute)
        .forEach(pa -> ofNullable(attributes.get(pa)).ifPresent(attr -> {
          ProjectAttribute projectAttribute = new ProjectAttribute();
          projectAttribute.setAttribute(attr);
          projectAttribute.setProject(project);

          projectAttribute.setValue(ProjectAttributeEnum.findByAttributeName(pa)
              .orElseThrow(() -> new ReportPortalException(ErrorType.BAD_REQUEST_ERROR,
                  Suppliers.formattedSupplier("Attribute - {} was not found", pa).get()
              ))
              .getDefaultValue());

          projectAttributes.add(projectAttribute);
        }));

    return projectAttributes;

  }

  public static Set<ProjectIssueType> defaultIssueTypes(Project project,
      List<IssueType> defaultIssueTypes) {

    Map<String, IssueType> issueTypes = defaultIssueTypes.stream()
        .collect(Collectors.toMap(IssueType::getLocator, i -> i));

    Set<ProjectIssueType> projectIssueTypes = new HashSet<>(defaultIssueTypes.size());
    Arrays.stream(TestItemIssueGroup.values())
        .map(TestItemIssueGroup::getLocator)
        .forEach(loc -> ofNullable(issueTypes.get(loc)).ifPresent(it -> {
          ProjectIssueType projectIssueType = new ProjectIssueType();
          projectIssueType.setIssueType(it);
          projectIssueType.setProject(project);
          projectIssueTypes.add(projectIssueType);
        }));
    return projectIssueTypes;
  }

  /**
   * Finds UserConfig for specified login. Returns null if it doesn't exists.
   *
   * @param user Login for search
   * @return UserConfig for specified login
   */
  @Nullable
  public static ProjectUser findUserConfigByLogin(Project project, String user) {
    return project.getUsers().stream().filter(it -> user.equalsIgnoreCase(it.getUser().getLogin()))
        .findAny().orElse(null);
  }

}
