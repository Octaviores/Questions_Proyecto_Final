package com.ecommerce.questions.infrastructure.security;

/** Cache local de tokens JWT validados con expiración automática */


//Modelos
import com.ecommerce.questions.infrastructure.security.dto.User;

//Librerías de caché (Caffeine)
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

//Spring
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//Libreria de tiempo y duración
import java.time.Duration;

@Component
public class TokenCache {

    private final Cache<String, User> cache;     //Un Map<>, pero de almacenamiento temporal

    public TokenCache(
            //Leo las propiedades en application.properties
            @Value("${token.cache.expiration.seconds:10}") int expirationSeconds,     //10 por defecto, si no hay valor
            @Value("${token.cache.max.size:1000}") int maxSize     //1000 por defecto, si no hay valor

    ) {
        //Defino tiempo y tamaño en caché
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(expirationSeconds))
                .maximumSize(maxSize)     //1000 tokens puedo almacenar
                .build();
    }

    //Obtiene un usuario del caché.
    //Devuelve null si el token no está o ya expiró
    public User get(String token) {
        User user = cache.getIfPresent(token);
        if (user != null) {
            System.out.println("Hay caché: " + user.getName());
        } else {
            System.out.println("No hay caché");
        }
        return user;
    }


    //Guarda el token en el caché y se elimina leugo del tiempo definido
    public void put(String token, User user) {
        cache.put(token, user);
        System.out.println("Token cacheado para usuario: " + user.getName() + " (expira en 1 hora)");
    }


    //Invalida manualmente un token específico (no creo que me sirva, pero por las dudas)
    public void invalidate(String token) {
        cache.invalidate(token);
        System.out.println("Token invalidado correctamente");
    }


    //Limpieza de todo el caché
    public void clear() {
        cache.invalidateAll();
        System.out.println("Cache limpiado correctamente");
    }


}