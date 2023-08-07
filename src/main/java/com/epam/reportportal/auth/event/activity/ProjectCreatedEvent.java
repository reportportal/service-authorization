package com.epam.reportportal.auth.event.activity;

import com.epam.ta.reportportal.builder.ActivityBuilder;
import com.epam.ta.reportportal.entity.activity.Activity;
import com.epam.ta.reportportal.entity.activity.ActivityAction;
import com.epam.ta.reportportal.entity.activity.EventAction;
import com.epam.ta.reportportal.entity.activity.EventObject;
import com.epam.ta.reportportal.entity.activity.EventPriority;
import com.epam.ta.reportportal.entity.activity.EventSubject;

/**
 * Publish an event when project is created.
 *
 * @author Ryhor_Kukharenka
 */
public class ProjectCreatedEvent implements ActivityEvent {

  private final Long projectId;
  private final String projectName;

  public ProjectCreatedEvent(Long projectId, String projectName) {
    this.projectId = projectId;
    this.projectName = projectName;
  }

  @Override
  public Activity toActivity() {
    return new ActivityBuilder()
        .addCreatedNow()
        .addAction(EventAction.CREATE)
        .addEventName(ActivityAction.CREATE_PROJECT.getValue())
        .addPriority(EventPriority.MEDIUM)
        .addObjectId(projectId)
        .addObjectName(projectName)
        .addObjectType(EventObject.PROJECT)
        .addProjectId(projectId)
        .addSubjectName("Auth Service")
        .addSubjectType(EventSubject.APPLICATION)
        .get();
  }

}
