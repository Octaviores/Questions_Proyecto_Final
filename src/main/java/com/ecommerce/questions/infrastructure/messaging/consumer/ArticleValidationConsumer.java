package com.ecommerce.questions.infrastructure.messaging.consumer;

/** Clase para escuchar la respuesta de Catalog por Rabbit */


//DTOs
import com.ecommerce.questions.infrastructure.messaging.dto.ArticleValidationResponse;
import com.ecommerce.questions.infrastructure.messaging.dto.CatalogResponseWrapper;

//QuestionService
import com.ecommerce.questions.application.QuestionService;

//RabbitMQ Listener
import org.springframework.amqp.rabbit.annotation.RabbitListener;

//Spring
import org.springframework.stereotype.Component;


@Component
public class ArticleValidationConsumer {

    private final QuestionService questionService;

    public ArticleValidationConsumer(QuestionService questionService) {
        this.questionService = questionService;
    }

    @RabbitListener(queues = "${rabbitmq.queue.questions-article-exist}")
    public void receiveArticleValidationResponse(CatalogResponseWrapper wrapper) {
        // Extraer el mensaje real del wrapper
        ArticleValidationResponse response = wrapper.getMessage();

        System.out.println("=== RESPUESTA RECIBIDA DE CATALOG ===");
        System.out.println("° Correlation ID: " + wrapper.getCorrelationId());
        System.out.println("° ArticleId: " + response.getArticleId());
        System.out.println("° ReferenceId: " + response.getReferenceId());
        System.out.println("° Valid: " + response.isValid());
        System.out.println("========================================");

        try {
            questionService.updateQuestionValidation(
                    response.getReferenceId(),
                    response.isValid()
            );
            System.out.println("- Pregunta actualizada");
        } catch (Exception e) {
            System.err.println("- Error al actualizar pregunta: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
