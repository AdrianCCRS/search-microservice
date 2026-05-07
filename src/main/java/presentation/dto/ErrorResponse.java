package presentation.dto;

import java.time.Instant;

/**
 * DTO de respuesta estandarizado para errores de la API.
 */
public record ErrorResponse(
    int status,
    String error,
    String message,
    String timestamp
) {
    /** Constructor de conveniencia que genera el timestamp automáticamente. */
    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, Instant.now().toString());
    }
}
