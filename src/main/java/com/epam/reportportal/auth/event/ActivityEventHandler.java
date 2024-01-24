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

package com.epam.reportportal.auth.event;

import com.epam.reportportal.auth.event.activity.ActivityEvent;
import com.epam.ta.reportportal.entity.activity.Activity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Activity Event Handler catch events after commit, transforms to Activity and send to queue.
 *
 * @author Ryhor_Kukharenka
 */
@Component
public class ActivityEventHandler {

  private static final String EXCHANGE_ACTIVITY = "activity";
  private final RabbitTemplate rabbitTemplate;

  public ActivityEventHandler(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @EventListener
  @TransactionalEventListener
  public void onApplicationEvent(ActivityEvent event) {
    Activity activity = event.toActivity();
    String key = generateKey(activity);

    rabbitTemplate.convertAndSend(EXCHANGE_ACTIVITY, key, activity);
  }

  private String generateKey(Activity activity) {
    return String.format("activity.%d.%s.%s",
        activity.getProjectId(),
        activity.getObjectType(),
        activity.getEventName());
  }

}
