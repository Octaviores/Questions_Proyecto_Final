# Microservicio de Preguntas

Este repositorio contiene el código fuente del microservicio **“Questions”**, un componente de una plataforma de comercio electrónico más amplia.  
Este servicio es responsable de **gestionar las preguntas enviadas por los usuarios sobre los productos**.  
Está desarrollado en **Java** utilizando el framework **Spring Boot**.

La funcionalidad principal consiste en **crear y recuperar preguntas**.  
Cuando se envía una nueva pregunta, inicialmente queda en estado `PENDING`.  
El servicio se comunica de forma **asíncrona** con el microservicio **“Catalog”** a través de **RabbitMQ** para verificar que el producto asociado exista.  
Una vez que se recibe una respuesta, el estado de la pregunta se actualiza a `VALID` o `INVALID`.

---

## Características

*   **API REST:** expone endpoints para crear y recuperar preguntas.
*   **Validación asíncrona:** integra RabbitMQ para validar de forma asíncrona los identificadores de productos contra el servicio de Catálogo.
*   **Autenticación JWT:** protege el endpoint de creación mediante tokens JWT, validados contra un microservicio de autenticación independiente.
*   **Caché de tokens:** implementa una caché local con Caffeine para almacenar tokens validados, reduciendo la latencia y la carga sobre el servicio de autenticación.
*   **Persistencia de datos:** utiliza Spring Data JPA y una base de datos PostgreSQL para almacenar la información de las preguntas.

---

## Arquitectura

El servicio está estructurado siguiendo los principios de la **Arquitectura Limpia (Clean Architecture)**, con capas separadas para la lógica de aplicación, los modelos de dominio y las implementaciones de infraestructura.

*   **`web`**: contiene el controlador REST (`QuestionController`) que expone los endpoints de la API.  
*   **`application`**: aloja la lógica central de negocio (`QuestionService`), orquestando la creación y validación de preguntas.  
*   **`domain`**: define el modelo de datos principal (`Question`), sus estados (`ValidationStatus`) y la interfaz del repositorio (`QuestionRepository`).  
*   **`infrastructure`**: maneja la comunicación con servicios externos y las implementaciones concretas.
    *   **`messaging`**: gestiona la integración con RabbitMQ para producir solicitudes de validación y consumir respuestas.  
    *   **`security`**: se encarga de la validación de tokens JWT comunicándose con el servicio de autenticación externo y almacenando los resultados en caché.  
    *   **`data`**: (de forma implícita mediante Spring Data JPA) administra la conexión y la persistencia de datos con PostgreSQL.

---

### Interacción del Sistema

1.  Un cliente envía una solicitud `POST /question` con un token JWT válido a este servicio.  
2.  El `TokenService` valida el JWT, verificando primero en la caché local y, si es necesario, consultando el microservicio de autenticación externo.  
3.  El `QuestionService` crea una nueva entidad `Question` con estado `PENDING` y la guarda en la base de datos.  
4.  El `ArticleValidationProducer` envía un mensaje al *exchange* `article_exist` en RabbitMQ, solicitando al servicio Catalog la validación del ID del producto.  
5.  El servicio Catalog procesa la solicitud y responde en la cola `questions_article_exist`.  
6.  El `ArticleValidationConsumer` recibe la respuesta y llama a `QuestionService` para actualizar el estado de la pregunta a `VALID` o `INVALID`.

---

## Endpoints de la API

| Método | Endpoint              | Descripción                                                                                                  | Autenticación     |
| :----- | :-------------------- | :----------------------------------------------------------------------------------------------------------- | :---------------- |
| `POST` | `/question`           | Crea una nueva pregunta para un producto específico. El cuerpo de la solicitud debe incluir `articuloId`, `titulo`, `pregunta`. | Token JWT Bearer  |
| `GET`  | `/questions`          | Recupera una lista de todas las preguntas.                                                                   | Ninguna           |
| `GET`  | `/questions/{id}`     | Recupera una pregunta específica según su ID.                                                                | Ninguna           |

---

### Ejemplo de solicitud `POST /question`

**Headers:**
```
Authorization: Bearer <tu-token-jwt>
Content-Type: application/json
```

**Body:**
```json
{
    "articuloId": "uuid-del-producto",
    "titulo": "Pregunta sobre el tamaño",
    "pregunta": "¿Cuáles son las dimensiones de este producto?"
}
```
## Configuración

Toda la configuración se gestiona en el archivo src/main/resources/application.properties.
Los siguientes valores deben configurarse de acuerdo al entorno:

*   **Base de Datos:**
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/questionDB
    spring.datasource.username=postgres
    spring.datasource.password=alfalfa
    ```
*   **RabbitMQ:**
    ```properties
    spring.rabbitmq.host=localhost
    spring.rabbitmq.port=5672
    spring.rabbitmq.username=guest
    spring.rabbitmq.password=guest
    ```
*   **Sevicio de Autenticación:**
    ```properties
    auth.service.url=http://localhost:3000
    ```
*   **Token Cache:**
    ```properties
    token.cache.expiration.seconds=20
    token.cache.max.size=1000
    ```

## Cómo ejecutar el servicio

### Requisitos previos

*   Java 17 o superior
*   Maven
*   Una instancia en ejecución de PostgreSQL
*   Una instancia en ejecución de RabbitMQ
*   Los microservicios de autenticación y catálogo deben estar corriendo y accesibles.

### Configuración Inicial

1.  **Clonar el repositorio:**
    ```sh
    git clone https://github.com/octaviores/questions_proyecto_final.git
    cd questions_proyecto_final
    ```
2.  **Crear la base de datos:**
    Asegurarse de tener una base de datos PostgreSQL llamada `questionDB` o actualiza la propiedad `spring.datasource.url` en `application.properties` para apuntar a la base de datos correcta.
 
3.  **Configurar la aplicación:**
    Abrir `src/main/resources/application.properties` y actualiza los datos de la base, RabbitMQ y el servicio de autenticación si difieren de los valores predeterminados.

4.  **Build and run the application:**
    Usa el wrapper de Maven para construir y ejecutar el servicio:
    ```sh
    ./mvnw spring-boot:run
    ```
    El servicio se iniciará por defecto en el puerto `8081`.
