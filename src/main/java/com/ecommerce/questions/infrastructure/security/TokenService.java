package com.ecommerce.questions.infrastructure.security;

/** Servicio de validación de tokens JWT */


//Modelos
import com.ecommerce.questions.infrastructure.security.dto.User;

//Spring
import org.springframework.stereotype.Service;


@Service
public class TokenService {

    //Dependencias
    private final TokenDao tokenDao;
    private final TokenCache tokenCache;

    public TokenService(TokenDao tokenDao, TokenCache tokenCache) {
        this.tokenDao = tokenDao;
        this.tokenCache = tokenCache;
    }


    //Metodo para validar usuario
    public void validateLoggedIn(String token) {
        System.out.println("token de validate: " + token);
        //Por si no ingresa nada
        if (token == null || token.isBlank()) {
            throw new UnauthorizedError("Token no proporcionado");
        }

        //Buscar en cache
        User cachedUser = tokenCache.get(token);
        if (cachedUser != null) {
            System.out.println("Token encontrado en cache: " + cachedUser.getName());
            return;
        }

        //Si no está en cache, validar con Auth
        User user = tokenDao.retrieveUser(token);
        if (user == null) {
            throw new UnauthorizedError("Token inválido");
        }

        //Cachear el token
        tokenCache.put(token, user);
        System.out.println("Token validado y cacheado: " + user.getName());
    }

    // Nuevo. Obtener usuario por un token
    public User getUserFromToken(String token) {
        System.out.println("token de getUser: " + token);
        if (token == null || token.isBlank()) {
            throw new UnauthorizedError("Token no proporcionado");
        }

        //Buscar en cache
        User cachedUser = tokenCache.get(token);
        if (cachedUser != null) {
            System.out.println("Token encontrado en cache: " + cachedUser.getName());
            return cachedUser;
        }

        //Si no está en cache, validar con Auth
        User user = tokenDao.retrieveUser(token);
        if (user == null) {
            throw new UnauthorizedError("Token inválido");
        }
        //Cachear el token
        tokenCache.put(token, user);
        System.out.println("Token validado y cacheado: " + user.getName());
        return user;
    }



    //Metodo para validar si un usuario es admin
    public void validateAdmin(String token) {

        //Validar que esté logueado
        validateLoggedIn(token);

        //Obtener usuario del cache (ya fue validado arriba)
        User user = tokenCache.get(token);
        if (user == null) {
            throw new UnauthorizedError("Usuario no encontrado");
        }

        //Verificar si tiene permiso "admin"
        if (!hasPermission(user.getPermissions(), "admin")) {
            throw new UnauthorizedError("Requiere permisos de administrador");
        }
    }


    //Invalidar caché manualmente. Tampoco lo voy a usar (creo)
    public void invalidate(String token) {
        tokenCache.invalidate(token);
    }


    //Ver qué usuarios tienen qué permiso. Y esto menos (ya que no van a haber admins)
    private boolean hasPermission(String[] permissions, String permission) {
        if (permissions == null) return false;

        for (String p : permissions) {
            if (p.equals(permission)) {
                return true;
            }
        }
        return false;
    }
}