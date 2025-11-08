package com.ecommerce.questions.infrastructure.security;

/** Excepción lanzada cuando la autenticación/autorización falla */


//Spring
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class UnauthorizedError extends RuntimeException {
    public UnauthorizedError(String message) {
        super(message);
    }
}