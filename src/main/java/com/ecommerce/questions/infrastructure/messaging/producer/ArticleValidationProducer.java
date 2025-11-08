package com.ecommerce.questions.infrastructure.messaging.producer;

/** Clase para enviar mensaje de validación del articulo a Catalog por Rabbit */


//DTOs
import com.ecommerce.questions.infrastructure.messaging.dto.ArticleValidationMessage;
import com.ecommerce.questions.infrastructure.messaging.dto.ArticleValidationRequest;

//RabbitMQ
import org.springframework.amqp.rabbit.core.RabbitTemplate;

//Spring
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArticleValidationProducer {

    //Dependencias
    private final RabbitTemplate rabbitTemplate;

    public ArticleValidationProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    //Leer atributos de application.properties
    @Value("${rabbitmq.exchange.article-exist}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key.to-catalog}")
    private String routingKeyToCatalog;

    @Value("${rabbitmq.routing.key.to-questions}")
    private String routingKeyToQuestions;


    //Metodo para enviar la petición (request) a Catalog
    public void sendArticleValidationRequest(String articleId, String questionId) {
        try {
            // Crear el mensaje interno
            ArticleValidationMessage message = ArticleValidationMessage.builder()
                    .articleId(articleId)
                    .referenceId(questionId)
                    .build();

            // Crear el request completo
            ArticleValidationRequest request = ArticleValidationRequest.builder()
                    .correlationId("123")           // Repetido? Lo pongo para compatibilidad con el mensaje de Catalog
                    .exchange(exchangeName)      // "article_exist"
                    .routingKey(routingKeyToQuestions) // "questions_article_exist"
                    .message(message)
                    .build();

            //Logs de prueba
            System.out.println("=== ENVIANDO MENSAJE A CATALOG ===");
            System.out.println("° Exchange: " + exchangeName);
            System.out.println("° Routing Key para Catalog: " + routingKeyToCatalog);
            System.out.println("° Routing Key de respuesta: " + routingKeyToQuestions);
            System.out.println("=====================================");

            //Envia el request con el exchange y routingKey correspondientes en JSON
            rabbitTemplate.convertAndSend(exchangeName, routingKeyToCatalog, request);

            System.out.println("Mensaje enviado");

        } catch (Exception e) {
            System.err.println("Error al enviar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
}