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
