package com.ecommerce.questions.infrastructure.security;

/** Anotación para validar que el usuario esté logueado */

//Librerías de Validación (Jakarta)

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

//Spring
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//Librerías de Java (metadatos de anotaciones)
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {ValidateLoggedIn.Validator.class})
public @interface ValidateLoggedIn {
    String message() default "User must be logged in";  // obligatorio

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
    //Clase interna que implementa la lógica de validación.
    //Usa TokenService para comprobar si el token recibido es válido.
    @Component
    class Validator implements ConstraintValidator<ValidateLoggedIn, String> {

        @Autowired
        TokenService tokenService;

        @Override
        public boolean isValid(String token, ConstraintValidatorContext context) {

            // Normalizar token eliminando prefijo "bearer "
            if (token != null && token.startsWith("bearer ")) {
                token = token.substring(7);
            }

            // Validar token con TokenService (lanza excepción si no es válido)
            tokenService.validateLoggedIn(token);
            return true;
        }
    }
}