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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
