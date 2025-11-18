package com.ecommerce.questions.domain.model;

/** Clase para crear las diferentes validaciones de un articulo */


public enum ValidationStatus {
    PENDING,       // Esperando validación de Catalog
    VALID,        // Articulo válido
    INVALID,     // Artículo no válido
    DELETED     // Pregunta eliminada por admin
}
