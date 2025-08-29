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

import com.epam.reportportal.auth.builder.ActivityBuilder;
import com.epam.reportportal.auth.entity.activity.Activity;
import com.epam.reportportal.auth.entity.activity.ActivityAction;
import com.epam.reportportal.auth.entity.activity.EventAction;
import com.epam.reportportal.auth.entity.activity.EventObject;
import com.epam.reportportal.auth.entity.activity.EventPriority;
import com.epam.reportportal.auth.entity.activity.EventSubject;

/**
 * Publish an event when user is created.
 *
 * @author Ryhor_Kukharenka
 */
public record UserCreatedEvent(long userId, String userLogin) implements ActivityEvent {
  private static final String SUBJECT_NAME = "Auth Service";

  @Override
  public Activity toActivity() {
    return new ActivityBuilder()
        .addCreatedNow()
        .addAction(EventAction.CREATE)
        .addEventName(ActivityAction.CREATE_USER.getValue())
        .addPriority(EventPriority.HIGH)
        .addObjectId(userId)
        .addObjectName(userLogin)
        .addObjectType(EventObject.USER)
        .addSubjectName(SUBJECT_NAME)
        .addSubjectType(EventSubject.APPLICATION)
        .get();
  }
}
