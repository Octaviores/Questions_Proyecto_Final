package com.ecommerce.questions.web;

/** Clase Controller REST principal */


//Servicios y lógica de negocio
import com.ecommerce.questions.application.QuestionService;
import com.ecommerce.questions.domain.model.Question;
import com.ecommerce.questions.domain.repository.QuestionRepository;
import com.ecommerce.questions.infrastructure.security.TokenService;
import com.ecommerce.questions.infrastructure.security.dto.User;
import com.ecommerce.questions.web.dto.AnswerDTO;
import com.ecommerce.questions.web.dto.QuestionDTO;

//Extras y Respuestas HTTP
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final TokenService tokenService;

    public QuestionController(QuestionService questionService,
                              QuestionRepository questionRepository,
                              TokenService tokenService) {
        this.questionService = questionService;
        this.questionRepository = questionRepository;
        this.tokenService = tokenService;
    }


    //Crear Pregunta
    @PostMapping("/question")
    public ResponseEntity<?> createQuestion(
            @RequestHeader("Authorization") @ValidateLoggedIn String token,
            @RequestBody QuestionDTO questionDTO) {

        String userId = extractUserIdFromToken(token);

        Question question = questionService.createQuestion(
                questionDTO.getArticuloId(),
                questionDTO.getTitulo(),
                questionDTO.getPregunta(),
                userId
        );
        return ResponseEntity.status(201).body("Pregunta creada");
    }

    //Obtener todas las preguntas (sin admin)
    @GetMapping("/questions")
    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }


    //Obtener pregunta por ID (sin admin)
    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable String id) {
        return questionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }



    // Nuevo. Listar preguntas de un artículo con filtros opcionales
    @GetMapping("/questions/article/{articleId}")
    public ResponseEntity<List<Question>> getQuestionsByArticle(
            @PathVariable String articleId,                       // ID del articulo
            @RequestParam(required = false) Boolean contestada,  // true= solo contestas, false=solo sin contestar, null=todas
            @RequestParam(defaultValue = "asc") String orden) { // "asc" o "desc" (por fecha de creación)

        List<Question> questions = questionService.listQuestions(articleId, contestada, orden);
        return ResponseEntity.ok(questions);
    }


    // Nuevo. Responder una pregunta (solo admin)
    @PostMapping("/questions/{id}/answer")
    public ResponseEntity<?> answerQuestion(
            @PathVariable String id,
            @RequestHeader("Authorization") @ValidateLoggedIn String token,
            @RequestBody AnswerDTO answerDTO) {

        // Validar que es admin
        tokenService.validateAdmin(token);

        // Extraer userId del admin
        String adminUserId = extractUserIdFromToken(token);

        Question question = questionService.answerQuestion(id, answerDTO.getRespuesta(), adminUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", question.getId());
        response.put("respuesta", question.getRespuesta());
        response.put("fechaRespuesta", question.getFechaRespuesta());
        response.put("message", "Pregunta respondida exitosamente");

        return ResponseEntity.ok(response);
    }


    // Nuevo. Eliminar una pregunta (solo admin, por spam)
    @PostMapping("/questions/{id}")
    public ResponseEntity<?> deleteQuestion(
            @PathVariable String id,
            @RequestHeader("Authorization") @ValidateLoggedIn String token) {

        // Validar que es admin
        tokenService.validateAdmin(token);

        questionService.deleteQuestion(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Pregunta eliminada exitosamente");

        return ResponseEntity.ok(response);
    }


    // Nuevo. Listar todas las preguntas contestadas (solo admin)
    @GetMapping("/questions/answered")
    public ResponseEntity<List<Question>> getAnsweredQuestions(
            @RequestHeader("Authorization") @ValidateLoggedIn String token) {

        // Validar que es admin
        tokenService.validateAdmin(token);

        List<Question> questions = questionService.listAllAnswered();
        return ResponseEntity.ok(questions);
    }

    // Nuevo. Listar todas las preguntas sin contestar (solo admin)
    @GetMapping("/questions/unanswered")
    public ResponseEntity<List<Question>> getUnansweredQuestions(
            @RequestHeader("Authorization") @ValidateLoggedIn String token) {

        // Validar que es admin
        tokenService.validateAdmin(token);

        List<Question> questions = questionService.listAllUnanswered();
        return ResponseEntity.ok(questions);
    }


    // Nuevo. Extrae el userId del token
    private String extractUserIdFromToken(String token) {
        User user = tokenService.getUserFromToken(token);
        return user.getId();
    }

}
