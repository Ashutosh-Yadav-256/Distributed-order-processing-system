package com.platform.payment.config;

import com.platform.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitMQConfig {

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(RabbitMQConstants.PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(RabbitMQConstants.INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(RabbitMQConstants.DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentInventoryReservedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.PAYMENT_INVENTORY_RESERVED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "payment")
                .build();
    }

    @Bean
    public Queue paymentOrderCancelledQueue() {
        return QueueBuilder.durable(RabbitMQConstants.PAYMENT_ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue paymentDlq() {
        return QueueBuilder.durable(RabbitMQConstants.PAYMENT_DLQ).build();
    }

    @Bean
    public Binding paymentInventoryReservedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("paymentInventoryReservedQueue") Queue paymentInventoryReservedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("inventoryExchange") TopicExchange inventoryExchange) {
        return BindingBuilder.bind(paymentInventoryReservedQueue)
                .to(inventoryExchange)
                .with(RabbitMQConstants.INVENTORY_RESERVED_KEY);
    }

    @Bean
    public Binding paymentOrderCancelledBinding(
            @org.springframework.beans.factory.annotation.Qualifier("paymentOrderCancelledQueue") Queue paymentOrderCancelledQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(paymentOrderCancelledQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding paymentDlqBinding(
            @org.springframework.beans.factory.annotation.Qualifier("paymentDlq") Queue paymentDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlqExchange") TopicExchange dlqExchange) {
        return BindingBuilder.bind(paymentDlq)
                .to(dlqExchange)
                .with(RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "payment.#");
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
