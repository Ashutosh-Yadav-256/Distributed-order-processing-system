package com.platform.notification.config;

import com.platform.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(RabbitMQConstants.PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(RabbitMQConstants.NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationEventsQueue() {
        return QueueBuilder.durable(RabbitMQConstants.NOTIFICATION_EVENTS_QUEUE).build();
    }

    @Bean
    public Binding notificationOrderCreatedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("notificationEventsQueue") Queue notificationEventsQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationEventsQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CREATED_KEY);
    }

    @Bean
    public Binding notificationOrderConfirmedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("notificationEventsQueue") Queue notificationEventsQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationEventsQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CONFIRMED_KEY);
    }

    @Bean
    public Binding notificationOrderCancelledBinding(
            @org.springframework.beans.factory.annotation.Qualifier("notificationEventsQueue") Queue notificationEventsQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(notificationEventsQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding notificationPaymentFailedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("notificationEventsQueue") Queue notificationEventsQueue,
            @org.springframework.beans.factory.annotation.Qualifier("paymentExchange") TopicExchange paymentExchange) {
        return BindingBuilder.bind(notificationEventsQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.PAYMENT_FAILED_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
