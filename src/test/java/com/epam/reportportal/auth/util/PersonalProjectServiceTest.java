/*
 * Copyright 2025 EPAM Systems
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
package com.epam.reportportal.auth.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.epam.reportportal.auth.dao.AttributeRepository;
import com.epam.reportportal.auth.dao.IssueTypeRepository;
import com.epam.reportportal.auth.dao.ProjectRepository;
import com.epam.reportportal.auth.entity.attribute.Attribute;
import com.epam.reportportal.auth.entity.project.Project;
import com.epam.reportportal.auth.entity.project.ProjectRole;
import com.epam.reportportal.auth.entity.user.ProjectUser;
import com.epam.reportportal.auth.entity.user.User;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author <a href="mailto:andrei_piankouski@epam.com">Andrei Piankouski</a>
 */
@ExtendWith(MockitoExtension.class)
class PersonalProjectServiceTest {

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private AttributeRepository attributeRepository;

  @Mock
  private IssueTypeRepository issueTypeRepository;

  @InjectMocks
  private PersonalProjectService personalProjectService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setLogin("test.user@example.com");
    testUser.setFullName("Test User");
  }

  @Test
  void getProjectPrefix_shouldReplaceSpecialCharacters() {
    String username = "user.name@domain.com";
    String expected = "user_name_domain_com_personal";
    assertEquals(expected, personalProjectService.getProjectPrefix(username));
  }

  @Test
  void getProjectPrefix_shouldConvertToLowerCase() {
    String username = "USER.NAME";
    String expected = "user_name_personal";
    assertEquals(expected, personalProjectService.getProjectPrefix(username));
  }

  @Test
  void generatePersonalProjectName_shouldReturnBaseNameWhenAvailable() {
    when(projectRepository.existsByName("test_user_personal")).thenReturn(false);

    String result = personalProjectService.generatePersonalProjectName("test.user");
    assertEquals("test_user_personal", result);
  }

  @Test
  void generatePersonalProjectName_shouldAddSuffixWhenNameExists() {
    when(projectRepository.existsByName("test_user_personal")).thenReturn(true);
    when(projectRepository.existsByName("test_user_personal_1")).thenReturn(false);

    String result = personalProjectService.generatePersonalProjectName("test.user");
    assertEquals("test_user_personal_1", result);
  }

  @Test
  void generatePersonalProjectName_shouldIncrementSuffixUntilNameIsAvailable() {
    when(projectRepository.existsByName("test_user_personal")).thenReturn(true);
    when(projectRepository.existsByName("test_user_personal_1")).thenReturn(true);
    when(projectRepository.existsByName("test_user_personal_2")).thenReturn(false);

    String result = personalProjectService.generatePersonalProjectName("test.user");
    assertEquals("test_user_personal_2", result);
  }

  @Test
  void generatePersonalProject_shouldCreateProjectWithCorrectName() {
    when(projectRepository.existsByName(anyString())).thenReturn(false);
    when(attributeRepository.findAllByNameIn(anySet())).thenReturn(Collections.emptySet());
    when(issueTypeRepository.getDefaultIssueTypes(anyList())).thenReturn(Collections.emptyList());

    Project project = personalProjectService.generatePersonalProject(testUser);

    assertNotNull(project);
    assertEquals("test_user_example_com_personal", project.getName());
    assertNotNull(project.getCreationDate());
  }

  @Test
  void generatePersonalProject_shouldSetCorrectUserWithEditorRole() {
    when(projectRepository.existsByName(anyString())).thenReturn(false);
    when(attributeRepository.findAllByNameIn(anySet())).thenReturn(Collections.emptySet());
    when(issueTypeRepository.getDefaultIssueTypes(anyList())).thenReturn(Collections.emptyList());

    Project project = personalProjectService.generatePersonalProject(testUser);

    assertEquals(1, project.getUsers().size());
    ProjectUser projectUser = project.getUsers().iterator().next();
    assertEquals(testUser, projectUser.getUser());
    assertEquals(ProjectRole.EDITOR, projectUser.getProjectRole());
    assertEquals(project, projectUser.getProject());
  }
}