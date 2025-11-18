package com.ecommerce.questions.domain.repository;

/** Repositorio de Question */


//Spring
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//Modelos
import com.ecommerce.questions.domain.model.Question;
import com.ecommerce.questions.domain.model.ValidationStatus;

//Extras
import java.util.List;

public interface QuestionRepository extends JpaRepository <Question,String> {


    // Nuevo. Busca preguntas por articleId
    List<Question> findByArticuloIdAndValidationStatus(String articuloId, ValidationStatus status);



    // Nuevo. Busca preguntas por articleId, ordenadas por fecha
    List<Question> findByArticuloIdAndValidationStatusOrderByFechaCreadoAsc(String articuloId, ValidationStatus status);
    List<Question> findByArticuloIdAndValidationStatusOrderByFechaCreadoDesc(String articuloId, ValidationStatus status);



    // Nuevo. Busca preguntas contestadas por articleId
    @Query("SELECT q FROM Question q WHERE q.articuloId = :articuloId AND q.validationStatus = 'VALID' AND q.respuesta IS NOT NULL")
    List<Question> findAnsweredByArticleId(@Param("articuloId") String articuloId);



    // Nuevo. Busca preguntas sin contestar por articleId
    @Query("SELECT q FROM Question q WHERE q.articuloId = :articuloId AND q.validationStatus = 'VALID' AND q.respuesta IS NULL")
    List<Question> findUnansweredByArticleId(@Param("articuloId") String articuloId);



    // Nuevo. Busca todas las preguntas contestadas
    @Query("SELECT q FROM Question q WHERE q.respuesta IS NOT NULL ORDER BY q.fechaRespuesta DESC")
    List<Question> findAllAnswered();



    // Nuevo. Busca todas las preguntas sin contestar
    @Query("SELECT q FROM Question q WHERE q.respuesta IS NULL AND q.validationStatus = 'VALID' ORDER BY q.fechaCreado ASC")
    List<Question> findAllUnanswered();
}

