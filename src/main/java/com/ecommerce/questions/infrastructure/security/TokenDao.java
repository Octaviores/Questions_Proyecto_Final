package com.ecommerce.questions.infrastructure.security;

/** DAO para validar tokens JWT consultando al microservicio Auth */

//Modelos
import com.ecommerce.questions.infrastructure.security.dto.User;

//Spring
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TokenDao {

    //Dependencias
    private final RestTemplate restTemplate;

    public TokenDao() {
        this.restTemplate = new RestTemplate();
    }

    //Leo el application.properties para ver el URL base de Auth
    @Value("${auth.service.url}")
    private String authServiceUrl;

    //Metodo para solicitar validación del token a Auth y obtener el usuario
    public User retrieveUser(String token) {
        try {

            String url = authServiceUrl + "/users/current";     //URL completo

            //Ingreso el Token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);

            //Armo una entidad HTTP
            HttpEntity<String> entity = new HttpEntity<>(headers);

            //Ejecuto un GET para Auth
            ResponseEntity<User> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    User.class
            );

            //Devuelvo el Usuario
            return response.getBody();

        } catch (Exception e) {
            System.err.println("Error al validar token: " + e.getMessage());
            return null;
        }
    }
}