package presentation.advice;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import presentation.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 — Parámetro inválido (ej. query < 3 caracteres). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
    }

    /** 400 — Parámetro requerido ausente en la petición. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request",
                        "El parámetro '" + ex.getParameterName() + "' es requerido"));
    }

    /**
     * 503 — Elasticsearch no disponible.
     * Captura RuntimeException cuya causa sea de tipo Elasticsearch.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleElasticsearchDown(RuntimeException ex) {
        Throwable cause = ex.getCause();
        if (cause != null && cause.getClass().getName().contains("Elasticsearch")) {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(503, "Service Unavailable",
                            "El servicio de búsqueda no está disponible en este momento"));
        }
        System.err.println("Error no esperado: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Error interno del servidor"));
    }

    /**
     * Fallback silencioso si Redis cae (RedisConnectionFailureException / QueryTimeoutException).
     * El servicio continúa operando sin caché; no se expone el error al cliente.
     */
    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<Void> handleRedisFallback(QueryTimeoutException ex) {
        System.err.println("[WARN] Redis no disponible, operando sin cache: " + ex.getMessage());
        return ResponseEntity.noContent().build();
    }

    /** 500 — Cualquier otro error no controlado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        System.err.println("Error no esperado: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Error interno del servidor"));
    }
}
