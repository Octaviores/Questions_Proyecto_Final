package com.ecommerce.questions.domain.model;

/** Clase de la pregunta base. Contiene la pregunta valga la redundancia */


//Persistencia
import jakarta.persistence.*;
import lombok.*;

//Extras
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)

    private String id;
    private String titulo;
    private String pregunta;
    private String articuloId;
    private Instant fechaCreado = Instant.now();

    //Campo para validación
    @Enumerated(EnumType.STRING) //Lo guardo con el nombre literal "PENDING", no por número
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    public Question(String articuloId, String titulo, String pregunta) {
        this.titulo = titulo;
        this.pregunta = pregunta;
        this.articuloId = articuloId;
        this.fechaCreado = Instant.now();
        this.validationStatus = ValidationStatus.PENDING;  //PENDING, porque espera la respuesta de Catalog
    }
}
