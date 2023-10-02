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
