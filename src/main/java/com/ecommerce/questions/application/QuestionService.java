package com.ecommerce.questions.application;

/** Lógica del Microservicio */


//Modelos y repositorio
import com.ecommerce.questions.domain.model.Question;
import com.ecommerce.questions.domain.model.ValidationStatus;
import com.ecommerce.questions.domain.repository.QuestionRepository;

//Messaging del RabbitMQ
import com.ecommerce.questions.infrastructure.messaging.producer.ArticleValidationProducer;

//Spring
import org.springframework.stereotype.Service;

//Extras
import java.time.Instant;
import java.util.List;

@Service
public class QuestionService {

    //Dependencias
    private final QuestionRepository questionRepository;
    private final ArticleValidationProducer validationProducer;

    public QuestionService(QuestionRepository questionRepository,
                           ArticleValidationProducer validationProducer) {
        this.questionRepository = questionRepository;
        this.validationProducer = validationProducer;
    }

    //Metodo para crear una pregunta
    public Question createQuestion(String articuloId, String titulo, String pregunta, String userId) {
        Question question = new Question(articuloId, titulo, pregunta, userId);
        question = questionRepository.save(question);

        validationProducer.sendArticleValidationRequest(articuloId, question.getId());

        System.out.println("Pregunta creada con ID: " + question.getId());
        System.out.println("Para artículo: " + articuloId);

        return question;
    }


    //Metodo para actualizar el estado de la pregunta
    //Se llama a través de consumer, cuando Catalog responde
    public void updateQuestionValidation(String questionId, boolean isValid) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada: " + questionId));

        if (isValid) {
            question.setValidationStatus(ValidationStatus.VALID);
            System.out.println("Pregunta " + questionId + " es VÁLIDA");
        } else {
            question.setValidationStatus(ValidationStatus.INVALID);
            System.out.println("Pregunta " + questionId + " es INVÁLIDA");
        }

        questionRepository.save(question);
    }


    // Nuevo. Responde una pregunta (solo admin)
    public Question answerQuestion(String questionId, String respuesta, String adminUserId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada: " + questionId));

        if (question.getValidationStatus() != ValidationStatus.VALID) {
            throw new RuntimeException("Solo se pueden responder preguntas válidas");
        }

        question.setRespuesta(respuesta);
        question.setRespuestaUserId(adminUserId);
        question.setFechaRespuesta(Instant.now());

        return questionRepository.save(question);
    }


    // Nuevo. Elimina una pregunta (marca como DELETED)
    public void deleteQuestion(String questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Pregunta no encontrada: " + questionId));

        question.setValidationStatus(ValidationStatus.DELETED);
        questionRepository.save(question);
    }


    // Nuevo. Lista preguntas por artículo con filtros opcionales
    public List<Question> listQuestions(String articuloId, Boolean contestada, String orden) {
        ValidationStatus status = ValidationStatus.VALID;

        if (contestada == null) {
            // Solo contestadas, filtradas por articleId y ordenadas por fecha asc o desc
            if ("asc".equalsIgnoreCase(orden)) {
                return questionRepository.findByArticuloIdAndValidationStatusOrderByFechaCreadoDesc(articuloId, status);
            } else {
                return questionRepository.findByArticuloIdAndValidationStatusOrderByFechaCreadoAsc(articuloId, status);
            }
        } else if (contestada) {
            // Solo contestadas
            return questionRepository.findAnsweredByArticleId(articuloId);
        } else {
            // Solo sin contestar
            return questionRepository.findUnansweredByArticleId(articuloId);
        }
    }

    // Nuevo. Lista todas las preguntas contestadas
    public List<Question> listAllAnswered() {
        return questionRepository.findAllAnswered();
    }

    // Nuevo. Lista todas las preguntas sin contestar
    public List<Question> listAllUnanswered() {
        return questionRepository.findAllUnanswered();
    }
}


