package com.ecommerce.questions.infrastructure.messaging.dto;

/** DTO con Mensaje anidado dentro de ArticleValidationRequest */


//JSON Serialización/Deserialización
import com.fasterxml.jackson.annotation.JsonProperty;

//Lombok
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleValidationMessage {
    @JsonProperty("articleId")
    private String articleId;

    @JsonProperty("referenceId")
    private String referenceId;
}