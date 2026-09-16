package com.platform.order.config;

import com.platform.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchanges
    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(RabbitMQConstants.INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(RabbitMQConstants.PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(RabbitMQConstants.DLQ_EXCHANGE, true, false);
    }

    // Response Queues for Order Saga
    @Bean
    public Queue orderInventoryResponseQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_INVENTORY_RESPONSE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "order")
                .build();
    }

    @Bean
    public Queue orderPaymentResponseQueue() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_PAYMENT_RESPONSE_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "order")
                .build();
    }

    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(RabbitMQConstants.ORDER_DLQ).build();
    }

    // Bindings
    @Bean
    public Binding inventoryReservedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("orderInventoryResponseQueue") Queue orderInventoryResponseQueue,
            @org.springframework.beans.factory.annotation.Qualifier("inventoryExchange") TopicExchange inventoryExchange) {
        return BindingBuilder.bind(orderInventoryResponseQueue)
                .to(inventoryExchange)
                .with(RabbitMQConstants.INVENTORY_RESERVED_KEY);
    }

    @Bean
    public Binding inventoryFailedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("orderInventoryResponseQueue") Queue orderInventoryResponseQueue,
            @org.springframework.beans.factory.annotation.Qualifier("inventoryExchange") TopicExchange inventoryExchange) {
        return BindingBuilder.bind(orderInventoryResponseQueue)
                .to(inventoryExchange)
                .with(RabbitMQConstants.INVENTORY_FAILED_KEY);
    }

    @Bean
    public Binding paymentCompletedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("orderPaymentResponseQueue") Queue orderPaymentResponseQueue,
            @org.springframework.beans.factory.annotation.Qualifier("paymentExchange") TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentResponseQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.PAYMENT_COMPLETED_KEY);
    }

    @Bean
    public Binding paymentFailedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("orderPaymentResponseQueue") Queue orderPaymentResponseQueue,
            @org.springframework.beans.factory.annotation.Qualifier("paymentExchange") TopicExchange paymentExchange) {
        return BindingBuilder.bind(orderPaymentResponseQueue)
                .to(paymentExchange)
                .with(RabbitMQConstants.PAYMENT_FAILED_KEY);
    }

    @Bean
    public Binding orderDlqBinding(
            @org.springframework.beans.factory.annotation.Qualifier("orderDlq") Queue orderDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlqExchange") TopicExchange dlqExchange) {
        return BindingBuilder.bind(orderDlq)
                .to(dlqExchange)
                .with(RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "order.#");
    }

    // Message Converter
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
