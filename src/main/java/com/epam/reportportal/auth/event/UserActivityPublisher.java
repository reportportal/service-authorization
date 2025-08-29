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

package com.epam.reportportal.auth.event;

import com.epam.reportportal.auth.entity.user.User;
import com.epam.reportportal.auth.event.activity.UserCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes user activity events.
 *
 * @author <a href="mailto:reingold_shekhtel@epam.com">Reingold Shekhtel</a>
 */
@Component
public class UserActivityPublisher {

  private final ApplicationEventPublisher publisher;

  public UserActivityPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  public void publishOnUserCreated(User user) {
    publisher.publishEvent(new UserCreatedEvent(user.getId(), user.getLogin()));
  }
}
