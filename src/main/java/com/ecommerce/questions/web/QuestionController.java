package com.ecommerce.questions.web;

/** Clase controlador REST principal */


//Servicios y lógica de negocio
import com.ecommerce.questions.application.QuestionService;
import com.ecommerce.questions.domain.model.Question;
import com.ecommerce.questions.domain.repository.QuestionRepository;
import com.ecommerce.questions.web.dto.QuestionDTO;

//Extras y Respuestas HTTP
import org.springframework.http.ResponseEntity;

import java.util.List;

//Endpoints REST
import org.springframework.web.bind.annotation.*;

//Seguridad
import com.ecommerce.questions.infrastructure.security.ValidateLoggedIn;


@RestController
@RequestMapping("/")
public class QuestionController {

    //Dependencias
    private final QuestionService questionService;
    private final QuestionRepository questionRepository;

    public QuestionController(QuestionService questionService, QuestionRepository questionRepository) {
        this.questionService = questionService;
        this.questionRepository = questionRepository;
    }


    //Crear Pregunta
    @PostMapping("/question")
    public ResponseEntity<?> createQuestion(
            @RequestHeader("Authorization") @ValidateLoggedIn String token,
            @RequestBody QuestionDTO questionDTO) {
        Question question = questionService.createQuestion(
                questionDTO.getArticuloId(),
                questionDTO.getTitulo(),
                questionDTO.getPregunta()
        );
        return ResponseEntity.status(201).body("Pregunta creada");
    }

    //Obtener todas las preguntas
    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }


    //Obtener pregunta por ID
    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable String id) {
        return questionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
