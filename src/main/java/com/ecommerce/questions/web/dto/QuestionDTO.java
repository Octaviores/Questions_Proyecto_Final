package com.ecommerce.questions.web.dto;

/** DTO para obtener los datos de la pregunta del Postman y crearla en la base de datos */

//Lombok
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionDTO {
    private String titulo;
    private String pregunta;
    private String articuloId;
    private String userId;
}
