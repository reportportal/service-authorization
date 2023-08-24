package com.epam.reportportal.auth.event.activity;

import com.epam.ta.reportportal.entity.activity.Activity;

public interface ActivityEvent {

  /**
   * Method for transform Event to Activity.
   *
   * @return Activity entity
   */
  Activity toActivity();

}
