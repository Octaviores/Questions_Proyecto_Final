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
    public Question createQuestion(String articuloId, String titulo, String pregunta) {
        Question question = new Question(articuloId, titulo, pregunta);
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

}

