package com.platform.inventory.config;

import com.platform.common.constant.RabbitMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryRabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(RabbitMQConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(RabbitMQConstants.INVENTORY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlqExchange() {
        return new TopicExchange(RabbitMQConstants.DLQ_EXCHANGE, true, false);
    }

    @Bean
    public Queue inventoryOrderCreatedQueue() {
        return QueueBuilder.durable(RabbitMQConstants.INVENTORY_ORDER_CREATED_QUEUE)
                .withArgument("x-dead-letter-exchange", RabbitMQConstants.DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "inventory")
                .build();
    }

    @Bean
    public Queue inventoryOrderCancelledQueue() {
        return QueueBuilder.durable(RabbitMQConstants.INVENTORY_ORDER_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue inventoryDlq() {
        return QueueBuilder.durable(RabbitMQConstants.INVENTORY_DLQ).build();
    }

    @Bean
    public Binding inventoryOrderCreatedBinding(
            @org.springframework.beans.factory.annotation.Qualifier("inventoryOrderCreatedQueue") Queue inventoryOrderCreatedQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(inventoryOrderCreatedQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CREATED_KEY);
    }

    @Bean
    public Binding inventoryOrderCancelledBinding(
            @org.springframework.beans.factory.annotation.Qualifier("inventoryOrderCancelledQueue") Queue inventoryOrderCancelledQueue,
            @org.springframework.beans.factory.annotation.Qualifier("orderExchange") TopicExchange orderExchange) {
        return BindingBuilder.bind(inventoryOrderCancelledQueue)
                .to(orderExchange)
                .with(RabbitMQConstants.ORDER_CANCELLED_KEY);
    }

    @Bean
    public Binding inventoryDlqBinding(
            @org.springframework.beans.factory.annotation.Qualifier("inventoryDlq") Queue inventoryDlq,
            @org.springframework.beans.factory.annotation.Qualifier("dlqExchange") TopicExchange dlqExchange) {
        return BindingBuilder.bind(inventoryDlq)
                .to(dlqExchange)
                .with(RabbitMQConstants.DLQ_ROUTING_KEY_PREFIX + "inventory.#");
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
