package com.ecommerce.questions.infrastructure.messaging.dto;

/** DTO que contiene el mensaje de respuesta de catalog */


//JSON Serialización/Deserialización
import com.fasterxml.jackson.annotation.JsonProperty;

//Lombok
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogResponseWrapper {

    @JsonProperty("correlation_id")
    private String correlationId;

    @JsonProperty("exchange")
    private String exchange;

    @JsonProperty("routing_key")
    private String routingKey;

    @JsonProperty("message")
    private ArticleValidationResponse message;
}
