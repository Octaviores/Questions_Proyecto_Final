package com.ecommerce.questions.infrastructure.messaging.dto;

/** DTO que contiene el mensaje de petición a Catalog por Rabbit */

//JSON Serialización/Deserialización
import com.fasterxml.jackson.annotation.JsonProperty;

//Lombok
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleValidationRequest {


    @JsonProperty("correlation_id")
    private String correlationId;     //un referenceId repetido

    @JsonProperty("exchange")
    private String exchange;     //Exchange donde Catalog debe enviar la respuesta

    @JsonProperty("routing_key")
    private String routingKey;     //Routing Key que Catalog usará para responder a Questions

    @JsonProperty("Message")
    private ArticleValidationMessage message;     //Mensaje con ID del articulo y referencia


}

