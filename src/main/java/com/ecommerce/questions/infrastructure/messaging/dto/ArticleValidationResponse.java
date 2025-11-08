package com.ecommerce.questions.infrastructure.messaging.dto;

/** DTO que contiene parte del mensaje de respuesta a Catalog anidado a CatalogResponseWrapper */

//JSON Serializaci&oacute;n/Deserializaci&oacute;n
import com.fasterxml.jackson.annotation.JsonProperty;

//Lombok
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ArticleValidationResponse {

    @JsonProperty("articleId")
    private String articleId;     //ID del articulo a validar

    @JsonProperty("referenceId")
    private String referenceId;     //ID para relacionar request-response

    @JsonProperty("valid")
    private boolean valid;     //guardar el resultado de la validez del articulo

    //Para que la salida sea legible. Sino en los logs del Consumer son ilegibles
    @Override
    public String toString() {
        return "ArticleValidationResponse{" +
                "articleId='" + articleId + '\'' +
                ", referenceId='" + referenceId + '\'' +
                ", valid=" + valid +
                '}';
    }
}