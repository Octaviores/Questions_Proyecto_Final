# Microservicio de Questions - Documentación Completa

## Tabla de Contenidos

1. [Casos de Uso](#casos-de-uso)
2. [Modelo de Datos](#modelo-de-datos)
3. [Interfaz REST](#interfaz-rest)
4. [Interfaz Asíncrona (RabbitMQ)](#interfaz-asíncrona-rabbitmq)
5. [Arquitectura](#arquitectura)
6. [Configuración](#configuración)

---

## Casos de Uso

### CU: Creación de una pregunta sobre un artículo

**Precondición:** El usuario debe estar autenticado con un token JWT válido.

**Camino normal:**
1. El usuario envía una pregunta sobre un artículo específico mediante el endpoint REST.
2. El servicio valida el token JWT consultando el microservicio de Auth (o usando el cache local si el token ya fue validado previamente).
3. Se crea la pregunta en la base de datos con estado `PENDING` y se asocia al `userId` del usuario autenticado.
4. Se envía un mensaje asíncrono a Catalog vía RabbitMQ solicitando validación del `articleId`.
5. El servicio responde inmediatamente al usuario con HTTP 201 indicando que la pregunta fue creada y está en proceso de validación.

**Caminos alternativos:**
- Si el token es inválido o no se proporciona, se devuelve HTTP 401 Unauthorized.
- Si el artículo no existe en Catalog, la pregunta se marca como `INVALID` cuando Catalog responde.

---

### CU: Validación asíncrona de artículo

**Precondición:** Una pregunta fue creada y está en estado `PENDING`.

**Camino normal:**
1. El microservicio Catalog recibe el mensaje de validación en la cola `catalog_article_exist`.
2. Catalog verifica si el `articleId` existe y está habilitado en su base de datos.
3. Catalog envía un mensaje de respuesta a la cola `questions_article_exist` indicando si el artículo es válido (`valid: true/false`).
4. El consumer de Questions recibe la respuesta y actualiza el estado de la pregunta:
   - Si `valid: true` → estado cambia a `VALID`
   - Si `valid: false` → estado cambia a `INVALID`

**Caminos alternativos:**
- Si Catalog no responde en un tiempo razonable, la pregunta permanece en `PENDING` (requiere monitoreo manual).

---

### CU: Consulta de preguntas de un artículo

**Precondición:** Ninguna (endpoint público).

**Camino normal:**
1. Un cliente (usuario no autenticado o sistema externo) solicita las preguntas de un artículo específico.
2. El servicio busca todas las preguntas asociadas al `articleId` con estado `VALID`.
3. Opcionalmente, el cliente puede filtrar por preguntas contestadas o sin contestar.
4. Opcionalmente, el cliente puede ordenar las preguntas por fecha de creación (ascendente o descendente).
5. El servicio devuelve la lista de preguntas en formato JSON.

**Caminos alternativos:**
- Si no existen preguntas para ese artículo, se devuelve un array vacío `[]`.

---

### CU: Responder una pregunta (admin)

**Precondición:** El usuario debe tener permisos de `admin` y la pregunta debe estar en estado `VALID`.

**Camino normal:**
1. El admin envía una respuesta para una pregunta específica mediante el endpoint REST.
2. El servicio valida que el usuario tiene permisos de `admin`.
3. Se actualiza la pregunta agregando:
   - `respuesta`: texto de la respuesta
   - `respuestaUserId`: ID (UUID) del usuario admin que respondió
   - `fechaRespuesta`: timestamp de cuando se respondió
4. Se devuelve HTTP 200 con los datos de la pregunta actualizada.

**Caminos alternativos:**
- Si el usuario no es admin, se devuelve HTTP 401 Unauthorized.
- Si la pregunta no está en estado `VALID`, se devuelve HTTP 400 Bad Request.

---

### CU: Eliminar una pregunta (admin)

**Precondición:** El usuario debe tener permisos de `admin`.

**Camino normal:**
1. El admin solicita eliminar una pregunta (por ejemplo, por spam).
2. El servicio valida que el usuario tiene permisos de `admin`.
3. La pregunta se marca con estado `DELETED` (soft delete, no se elimina físicamente).
4. Se devuelve HTTP 200 confirmando la eliminación.

**Caminos alternativos:**
- Si el usuario no es admin, se devuelve HTTP 401 Unauthorized.

**Nota:** Las preguntas pueden eliminarse sin importar si están contestadas o no.

---

### CU: Listar preguntas contestadas/sin contestar (admin)

**Precondición:** El usuario debe tener permisos de `admin`.

**Camino normal:**
1. El admin solicita la lista de todas las preguntas contestadas o sin contestar.
2. El servicio valida que el usuario tiene permisos de `admin`.
3. Se consulta la base de datos filtrando por:
   - Preguntas contestadas: `respuesta IS NOT NULL`
   - Preguntas sin contestar: `respuesta IS NULL AND validationStatus = 'VALID'`
4. Se devuelve la lista de preguntas ordenada por fecha.

**Caminos alternativos:**
- Si el usuario no es admin, se devuelve HTTP 401 Unauthorized.

---

## Modelo de Datos

### **Question**

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | UUID | Identificador único de la pregunta |
| `titulo` | String | Título breve de la pregunta |
| `pregunta` | String | Contenido completo de la pregunta |
| `articuloId` | String | ID del artículo sobre el que se pregunta |
| `userId` | String (UUID) | ID del usuario que creó la pregunta |
| `respuesta` | String (nullable) | Respuesta del admin |
| `respuestaUserId` | String (UUID, nullable) | ID del admin que respondió |
| `fechaRespuesta` | Instant (nullable) | Timestamp de la respuesta |
| `fechaCreado` | Instant | Timestamp de creación de la pregunta |
| `validationStatus` | Enum | Estado de validación [`PENDING`, `VALID`, `INVALID`, `DELETED`] |

### **ValidationStatus (Enum)**

- `PENDING`: Esperando validación del artículo por parte de Catalog
- `VALID`: Artículo validado, pregunta activa
- `INVALID`: Artículo no existe o no está disponible
- `DELETED`: Pregunta eliminada por admin (spam u otro motivo)

---

## Interfaz REST

### **Crear pregunta**
```
POST /question
```

**Headers**
```
Authorization: Bearer <token>
Content-Type: application/json
```

**Body**
```json
{
  "articuloId": "690a144980f77c667b1aab85",
  "titulo": "Consulta sobre garantía",
  "pregunta": "¿Este producto tiene garantía de fábrica?"
}
```

**Response**

`201 CREATED`
```json
"Pregunta creada"
```

`401 UNAUTHORIZED`
```json
{
  "timestamp": "2025-11-08T10:30:00.000Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Token inválido"
}
```

---

### **Obtener todas las preguntas**
```
GET /questions
```

**Params path**  
*no tiene*

**Params query**  
*no tiene*

**Headers**  
*no requiere autenticación*

**Response**

`200 OK`
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "titulo": "Consulta sobre garantía",
    "pregunta": "¿Este producto tiene garantía?",
    "articuloId": "690a144980f77c667b1aab85",
    "userId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
    "respuesta": "Sí, tiene 12 meses de garantía",
    "respuestaUserId": "c3d4e5f6-a7b8-9012-3456-7890abcdef12",
    "fechaRespuesta": "2025-11-08T12:00:00.000Z",
    "fechaCreado": "2025-11-08T10:30:00.000Z",
    "validationStatus": "VALID"
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
    "titulo": "Duda sobre envío",
    "pregunta": "¿Hacen envíos internacionales?",
    "articuloId": "690a144980f77c667b1aab85",
    "userId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
    "respuesta": null,
    "respuestaUserId": null,
    "fechaRespuesta": null,
    "fechaCreado": "2025-11-08T11:00:00.000Z",
    "validationStatus": "VALID"
  }
]
```

---

### **Obtener pregunta por ID**
```
GET /questions/{id}
```

**Params path**
- `id`: UUID de la pregunta

**Headers**  
*no requiere autenticación*

**Response**

`200 OK`
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "titulo": "Consulta sobre garantía",
  "pregunta": "¿Este producto tiene garantía?",
  "articuloId": "690a144980f77c667b1aab85",
  "userId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
  "respuesta": "Sí, tiene 12 meses de garantía",
  "respuestaUserId": "c3d4e5f6-a7b8-9012-3456-7890abcdef12",
  "fechaRespuesta": "2025-11-08T12:00:00.000Z",
  "fechaCreado": "2025-11-08T10:30:00.000Z",
  "validationStatus": "VALID"
}
```

`404 NOT FOUND`  
si no existe la pregunta con el ID indicado

---

### **Listar preguntas por artículo**
```
GET /questions/article/{articleId}
```

**Params path**
- `articleId`: ID del artículo

**Params query**
- `contestada` (opcional): `true` para solo contestadas, `false` para solo sin contestar, omitir para todas
- `orden` (opcional): `asc` o `desc` (por fecha de creación). Default: `asc`

**Headers**  
*no requiere autenticación*

**Ejemplos de uso:**
```
/questions/article/690a144980f77c667b1aab85
/questions/article/690a144980f77c667b1aab85?contestada=true
/questions/article/690a144980f77c667b1aab85?contestada=false&orden=desc
```

**Ejemplo completo de request/response:**

**Request:**
```
GET /questions/article/690a144980f77c667b1aab85?contestada=false&orden=desc
```

**Response:**

`200 OK`
```json
[
  {
    "id": "c3d4e5f6-a7b8-9012-3456-7890abcdef12",
    "titulo": "¿Viene con cable?",
    "pregunta": "¿Este producto incluye cable USB?",
    "articuloId": "690a144980f77c667b1aab85",
    "userId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
    "respuesta": null,
    "fechaCreado": "2025-11-08T15:45:00.000Z",
    "validationStatus": "VALID"
  },
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
    "titulo": "Duda sobre envío",
    "pregunta": "¿Hacen envíos internacionales?",
    "articuloId": "690a144980f77c667b1aab85",
    "userId": "f1e2d3c4-b5a6-7890-1234-567890abcdef",
    "respuesta": null,
    "fechaCreado": "2025-11-08T11:00:00.000Z",
    "validationStatus": "VALID"
  }
]
```

---

### **Responder pregunta (admin)**
```
POST /questions/{id}/answer
```

**Params path**
- `id`: UUID de la pregunta

**Headers**
```
Authorization: Bearer <token-admin>
Content-Type: application/json
```

**Body**
```json
{
  "respuesta": "Sí, este producto cuenta con garantía de fábrica de 12 meses."
}
```

**Ejemplo completo de request/response:**

**Request:**
```
POST /questions/a1b2c3d4-e5f6-7890-abcd-ef1234567890/answer
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "respuesta": "Sí, este producto cuenta con garantía de fábrica de 12 meses contra defectos de fabricación."
}
```

**Response:**

`200 OK`
```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "respuesta": "Sí, este producto cuenta con garantía de fábrica de 12 meses contra defectos de fabricación.",
  "fechaRespuesta": "2025-11-08T12:00:00.000Z",
  "message": "Pregunta respondida exitosamente"
}
```

`401 UNAUTHORIZED`  
si el usuario no es admin o el token es inválido

`400 BAD REQUEST`  
si la pregunta no está en estado `VALID`

---

### **Eliminar pregunta (admin)**
```
POST /questions/{id}
```

**Params path**
- `id`: UUID de la pregunta

**Headers**
```
Authorization: Bearer <token-admin>
```

**Ejemplo completo de request/response:**

**Request:**
```
POST /questions/b2c3d4e5-f6a7-8901-bcde-f23456789012
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**

`200 OK`
```json
{
  "message": "Pregunta eliminada exitosamente"
}
```

`401 UNAUTHORIZED`  
si el usuario no es admin o el token es inválido

**Nota:** Las preguntas pueden eliminarse sin importar si están contestadas o no.

---

### **Listar preguntas contestadas (admin)**
```
GET /questions/answered
```

**Headers**
```
Authorization: Bearer <token-admin>
```

**Ejemplo completo de request/response:**

**Request:**
```
GET /questions/answered
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**

`200 OK`
```json
[
  {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "titulo": "Consulta sobre garantía",
    "pregunta": "¿Este producto tiene garantía?",
    "respuesta": "Sí, tiene 12 meses de garantía",
    "respuestaUserId": "c3d4e5f6-a7b8-9012-3456-7890abcdef12",
    "fechaRespuesta": "2025-11-08T12:00:00.000Z",
    "validationStatus": "VALID"
  }
]
```

`401 UNAUTHORIZED`  
si el usuario no es admin

---

### **Listar preguntas sin contestar (admin)**
```
GET /questions/unanswered
```

**Headers**
```
Authorization: Bearer <token-admin>
```

**Ejemplo completo de request/response:**

**Request:**
```
GET /questions/unanswered
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response:**

`200 OK`
```json
[
  {
    "id": "b2c3d4e5-f6a7-8901-bcde-f23456789012",
    "titulo": "Duda sobre envío",
    "pregunta": "¿Hacen envíos internacionales?",
    "respuesta": null,
    "fechaCreado": "2025-11-08T11:00:00.000Z",
    "validationStatus": "VALID"
  }
]
```

`401 UNAUTHORIZED`  
si el usuario no es admin

---

## Interfaz Asíncrona (RabbitMQ)

### **Solicitud de validación de artículo**

**Dirección:** Questions → Catalog

**Exchange:** `article_exist` (tipo: `direct`)  
**Routing key:** `article_exist`  
**Queue destino:** `catalog_article_exist` (escucha Catalog)

**Body enviado por Questions:**
```json
{
  "correlation_id": "123",
  "exchange": "article_exist",
  "routing_key": "questions_article_exist",
  "Message": {
    "articleId": "690a144980f77c667b1aab85",
    "referenceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }
}
```

**Descripción de campos:**
- `correlation_id`: ID para rastreo de mensajes (opcional, usado en logs)
- `exchange`: Exchange donde Catalog debe enviar la respuesta
- `routing_key`: Routing key que Catalog usará para responder (`questions_article_exist`)
- `Message.articleId`: ID del artículo a validar
- `Message.referenceId`: ID de la pregunta (UUID, para relacionar request-response)

---

### **Respuesta de validación de artículo**

**Dirección:** Catalog → Questions

**Exchange:** `article_exist` (tipo: `direct`)  
**Routing key:** `questions_article_exist`  
**Queue destino:** `questions_article_exist` (escucha Questions)

**Body recibido de Catalog:**
```json
{
  "correlation_id": "123",
  "exchange": "",
  "routing_key": "",
  "message": {
    "articleId": "690a144980f77c667b1aab85",
    "referenceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "valid": true
  }
}
```

**Descripción de campos:**
- `message.articleId`: ID del artículo validado
- `message.referenceId`: ID de la pregunta (UUID, mismo que se envió en la solicitud)
- `message.valid`: `true` si el artículo existe y está habilitado, `false` si no

**Nota:** Catalog puede incluir opcionalmente campos adicionales como `price` y `stock` con fines informativos, pero Questions solo utiliza los campos `articleId`, `referenceId` y `valid` para actualizar el estado de la pregunta.

---

## Arquitectura

El servicio sigue los principios de **Domain-Driven Design (DDD)**, organizando el código en capas claramente definidas:

### **Estructura de capas:**
```
com.ecommerce.questions/
│
├── web/                           # Capa de Presentación
│   ├── QuestionController.java   # Controlador REST
│   └── dto/
│       ├── QuestionDTO.java      # DTO de entrada
│       └── AnswerDTO.java        # DTO de respuesta
│
├── application/                   # Capa de Aplicación
│   └── QuestionService.java      # Lógica de negocio y casos de uso
│
├── domain/                        # Capa de Dominio
│   ├── model/
│   │   ├── Question.java         # Entidad principal
│   │   └── ValidationStatus.java # Enum de estados
│   └── repository/
│       └── QuestionRepository.java # Interfaz de repositorio
│
└── infrastructure/                # Capa de Infraestructura
    ├── messaging/                 # Comunicación asíncrona
    │   ├── config/
    │   │   └── RabbitMQConfig.java
    │   ├── producer/
    │   │   └── ArticleValidationProducer.java
    │   ├── consumer/
    │   │   └── ArticleValidationConsumer.java
    │   └── dto/
    │       ├── ArticleValidationRequest.java
    │       ├── ArticleValidationResponse.java
    │       ├── ArticleValidationMessage.java
    │       └── CatalogResponseWrapper.java
    │
    └── security/                  # Autenticación y autorización
        ├── TokenService.java
        ├── TokenDao.java
        ├── TokenCache.java
        ├── ValidateLoggedIn.java
        ├── UnauthorizedError.java
        └── dto/
            └── User.java
```

### **Responsabilidades de cada capa:**

| Capa | Responsabilidad |
|------|-----------------|
| **web** | Expone endpoints REST y maneja requests/responses HTTP |
| **application** | Orquesta casos de uso y lógica de negocio |
| **domain** | Define el modelo de dominio (entidades, enums, interfaces) |
| **infrastructure** | Implementaciones técnicas (RabbitMQ, seguridad, persistencia) |

---

## Configuración

### **Archivo `application.properties`**
```properties
# Aplicación
spring.application.name=questions
server.port=8081

# Base de datos PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/questionDB
spring.datasource.username=postgres
spring.datasource.password=tu_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# RabbitMQ
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Exchanges y queues
rabbitmq.exchange.article-exist=article_exist
rabbitmq.queue.questions-article-exist=questions_article_exist
rabbitmq.routing.key.to-catalog=article_exist
rabbitmq.routing.key.to-questions=questions_article_exist

# Servicio de autenticación
auth.service.url=http://localhost:3000
auth.current.user.path=/users/current

# Cache de tokens (Caffeine)
token.cache.expiration.seconds=3600
token.cache.max.size=1000
```

### **Explicación de propiedades clave:**

| Propiedad | Descripción |
|-----------|-------------|
| `server.port` | Puerto donde corre el microservicio |
| `spring.jpa.hibernate.ddl-auto` | `update` = actualiza schema automáticamente |
| `rabbitmq.exchange.article-exist` | Exchange compartido con Catalog |
| `rabbitmq.queue.questions-article-exist` | Queue exclusiva de Questions para respuestas |
| `auth.service.url` | URL base del microservicio de autenticación |
| `token.cache.expiration.seconds` | Tiempo de expiración del cache de tokens (3600s = 1 hora) |

---

## Dependencias

### **En `pom.xml`:**
```xml
<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- Spring AMQP (RabbitMQ) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- PostgreSQL Driver -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Caffeine Cache -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
        <version>3.1.8</version>
    </dependency>
    
    <!-- Jakarta Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## Diagrama de Flujo Completo
```
┌──────────┐         ┌──────────┐         ┌──────────┐
│  Cliente │         │ Questions│         │  Catalog │
└────┬─────┘         └────┬─────┘         └────┬─────┘
     │                    │                     │
     │ POST /question     │                     │
     │ (+ JWT token)      │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │                    │ Validate token      │
     │                    │ (Auth service)      │
     │                    │                     │
     │                    │ Save Question       │
     │                    │ (status: PENDING)   │
     │                    │                     │
     │ 201 Created        │                     │
     │<───────────────────┤                     │
     │                    │                     │
     │                    │ RabbitMQ: Validate  │
     │                    │ article request     │
     │                    ├────────────────────>│
     │                    │                     │
     │                    │                     │ Validate
     │                    │                     │ article
     │                    │                     │
     │                    │ RabbitMQ: Response  │
     │                    │ (valid: true/false) │
     │                    │<────────────────────┤
     │                    │                     │
     │                    │ Update Question     │
     │                    │ (status: VALID/     │
     │                    │  INVALID)           │
     │                    │                     │
     │ GET /questions/    │                     │
     │ article/{id}       │                     │
     ├───────────────────>│                     │
     │                    │                     │
     │ 200 OK             │                     │
     │ [questions list]   │                     │
     │<───────────────────┤                     │
     │                    │                     │
```

---

## Notas Adicionales

### **Soft Delete**
Las preguntas eliminadas por admin NO se borran físicamente de la base de datos. Se marcan con `validationStatus = DELETED`, permitiendo auditoría y recuperación si es necesario.

### **Cache de Tokens**
El cache de tokens (Caffeine) reduce la latencia y la carga sobre el servicio de autenticación:
- Primera validación: consulta a Auth (~200ms)
- Validaciones subsecuentes: consulta al cache local (~1ms)
- Expiración configurable (default: 1 hora)

### **Validación Asíncrona**
La validación de artículos es asíncrona para:
- Mejorar el tiempo de respuesta al usuario
- Desacoplar Questions de Catalog
- Permitir escalabilidad independiente

### **Permisos**
- Usuarios comunes (`user`): Pueden crear preguntas y consultarlas
- Administradores (`admin`): Pueden responder, eliminar y listar preguntas por estado

---

**Fin de la documentación**
