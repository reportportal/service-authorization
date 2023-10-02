package com.epam.reportportal.auth.event.activity;

import com.epam.ta.reportportal.builder.ActivityBuilder;
import com.epam.ta.reportportal.entity.activity.Activity;
import com.epam.ta.reportportal.entity.activity.ActivityAction;
import com.epam.ta.reportportal.entity.activity.EventAction;
import com.epam.ta.reportportal.entity.activity.EventObject;
import com.epam.ta.reportportal.entity.activity.EventPriority;
import com.epam.ta.reportportal.entity.activity.EventSubject;

/**
 * Publish an event when user is created.
 *
 * @author Ryhor_Kukharenka
 */
public class UserCreatedEvent implements ActivityEvent {

  private final Long userId;
  private final String userLogin;

  public UserCreatedEvent(Long userId, String userLogin) {
    this.userId = userId;
    this.userLogin = userLogin;
  }

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
        .addSubjectName("Auth Service")
        .addSubjectType(EventSubject.APPLICATION)
        .get();
  }

}
