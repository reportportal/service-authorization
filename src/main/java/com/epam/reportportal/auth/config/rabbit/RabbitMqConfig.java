package com.epam.reportportal.auth.config.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitMqConfig {

  private final ObjectMapper objectMapper;

  public RabbitMqConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter(objectMapper);
  }

  @Bean
  public ConnectionFactory connectionFactory(@Value("${rp.amqp.addresses}") URI addresses,
      @Value("${rp.amqp.base-vhost}") String virtualHost) {
    final CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(addresses);
    cachingConnectionFactory.setVirtualHost(virtualHost);
    return cachingConnectionFactory;
  }

  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter());
    return rabbitTemplate;
  }

}
