package org.th.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import java.net.URI;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_FIREBASE_PUSH = "firebase.push.queue";
    public static final String EXCHANGE_FIREBASE_PUSH = "firebase.push.exchange";
    public static final String ROUTING_KEY_FIREBASE_PUSH = "firebase.push.routing.key";

    // Unified Notification Constants
    public static final String EXCHANGE_NOTIFICATIONS_TOPIC = "notifications.topic.exchange";
    public static final String QUEUE_NOTIFICATIONS_PUSH = "notifications.push.queue";
    public static final String ROUTING_KEY_NOTIFY_ALL = "notify.#";

    @Bean
    public ConnectionFactory connectionFactory(@Value("${RABBITMQ_URL:amqp://localhost:5672}") String rabbitUri) {
        try {
            return new CachingConnectionFactory(new URI(rabbitUri));
        } catch (Exception e) {
            System.err.println("CRITICAL FAILURE parsing RABBITMQ_URL. Value received: [" + rabbitUri + "]");
            e.printStackTrace();
            throw new RuntimeException("Failed to create RabbitMQ ConnectionFactory", e);
        }
    }

    @Bean
    public Queue firebasePushQueue() {
        return new Queue(QUEUE_FIREBASE_PUSH, true);
    }

    @Bean
    public TopicExchange firebasePushExchange() {
        return new TopicExchange(EXCHANGE_FIREBASE_PUSH);
    }

    @Bean
    public Binding bindingFirebasePush(Queue firebasePushQueue, TopicExchange firebasePushExchange) {
        return BindingBuilder.bind(firebasePushQueue).to(firebasePushExchange).with(ROUTING_KEY_FIREBASE_PUSH);
    }

    // Unified Notification Beans
    @Bean
    public Queue notificationsPushQueue() {
        return new Queue(QUEUE_NOTIFICATIONS_PUSH, true);
    }

    @Bean
    public TopicExchange notificationsTopicExchange() {
        return new TopicExchange(EXCHANGE_NOTIFICATIONS_TOPIC);
    }

    @Bean
    public Binding bindingNotificationsPush(Queue notificationsPushQueue, TopicExchange notificationsTopicExchange) {
        return BindingBuilder.bind(notificationsPushQueue).to(notificationsTopicExchange).with(ROUTING_KEY_NOTIFY_ALL);
    }

    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory = new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(20);
        factory.setPrefetchCount(20);
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        // Allow deserializing classes from any package. 
        // This is necessary because the DTO package names differ between Core and Worker.
        converter.setJavaTypeMapper(new org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper());
        ((org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper) converter.getJavaTypeMapper()).setTrustedPackages("*");
        return converter;
    }
}
