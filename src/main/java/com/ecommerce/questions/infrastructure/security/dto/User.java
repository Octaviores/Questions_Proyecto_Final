package com.ecommerce.questions.infrastructure.security.dto;

/**  DTO que representa al usuario autenticado devuelto por Auth. */

//Serialización JSON
import com.fasterxml.jackson.annotation.JsonProperty;

//Lombok
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @JsonProperty("id")
    private String id;     //ID del usuario

    @JsonProperty("name")
    private String name;     //Nombre del usuario

    @JsonProperty("login")
    private String login;     //Mail del usuario

    @JsonProperty("permissions")
    private String[] permissions;     //Si tiene permisos de admin o usario común
}