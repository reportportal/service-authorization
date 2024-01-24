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

package com.epam.reportportal.auth.event.activity;

import static com.epam.ta.reportportal.entity.activity.ActivityAction.ASSIGN_USER;

import com.epam.ta.reportportal.builder.ActivityBuilder;
import com.epam.ta.reportportal.entity.activity.Activity;
import com.epam.ta.reportportal.entity.activity.EventAction;
import com.epam.ta.reportportal.entity.activity.EventObject;
import com.epam.ta.reportportal.entity.activity.EventPriority;
import com.epam.ta.reportportal.entity.activity.EventSubject;

/**
 * Event publish when user is assigned to a project.
 *
 * @author Ryhor_Kukharenka
 */
public class AssignUserEvent implements ActivityEvent {

  private final Long userId;
  private final String userLogin;
  private final Long projectId;

  public AssignUserEvent(Long userId, String userLogin, Long projectId) {
    this.userId = userId;
    this.userLogin = userLogin;
    this.projectId = projectId;
  }

  @Override
  public Activity toActivity() {
    return new ActivityBuilder()
        .addCreatedNow()
        .addAction(EventAction.ASSIGN)
        .addEventName(ASSIGN_USER.getValue())
        .addPriority(EventPriority.HIGH)
        .addObjectId(userId)
        .addObjectName(userLogin)
        .addObjectType(EventObject.USER)
        .addProjectId(projectId)
        .addSubjectName("Auth Service")
        .addSubjectType(EventSubject.APPLICATION)
        .get();
  }

}
