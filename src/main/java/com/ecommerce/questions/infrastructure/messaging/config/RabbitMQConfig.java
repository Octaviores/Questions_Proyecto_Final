package com.ecommerce.questions.infrastructure.messaging.config;

/** Clase para configuar Exchanges, Queue y Bindings para Rabbit */


//RabbitMQ
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

//RabbitMQ Connexión,template y serialización
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

//Spring
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    //Leemos las propiedades del application.properties
    @Value("${rabbitmq.exchange.article-exist}")
    private String exchangeName;

    @Value("${rabbitmq.queue.questions-article-exist}")
    private String queueName;

    @Value("${rabbitmq.routing.key.to-questions}")
    private String routingKeyToQuestions;


    //Si existe el Exchange, lo crea. Si no, no.
    //En este caso si existe
    @Bean
    public DirectExchange articleExistExchange() {
        return new DirectExchange(exchangeName, false, false);
    }


    //Si existe la Queue, la crea. Sino, no.
    @Bean
    public Queue questionsArticleExistQueue() {
        return new Queue(queueName, true); // true = durable (persiste si RabbitMQ se reinicia)
    }


    //Conecta el la Queue al exchange con la Routing Key (Bind)
    @Bean
    public Binding bindingQuestionsQueue(Queue questionsArticleExistQueue, DirectExchange articleExistExchange) {
        return BindingBuilder
                .bind(questionsArticleExistQueue)
                .to(articleExistExchange)
                .with(routingKeyToQuestions);
    }

    //Transformar a JSON
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    //Template para enviar mensajes
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}