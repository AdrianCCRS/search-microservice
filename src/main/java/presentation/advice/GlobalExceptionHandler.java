package presentation.advice;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import presentation.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 400 - Parámetros inválidos o faltantes */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request",
                        "El parámetro '" + ex.getParameterName() + "' es requerido"));
    }

    /**
     * 503 - Elasticsearch no disponible.
     * Se lanza cuando el repositorio no puede conectarse al motor de búsqueda.
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
        // Fallback para cualquier otro RuntimeException
        System.err.println("Error no esperado: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Error interno del servidor"));
    }

    /**
     * 503 - Fallback silencioso si Redis cae.
     * Redis lanza QueryTimeoutException cuando no responde. En ese caso
     * el servicio continúa operando sin caché y no expone el error al cliente.
     */
    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<Void> handleRedisFallback(QueryTimeoutException ex) {
        System.err.println("[WARN] Redis no disponible, operando sin cache: " + ex.getMessage());
        // Retorno vacío 204 para que el cliente sepa que la respuesta se procesó
        // sin revelar detalles de infraestructura interna.
        return ResponseEntity.noContent().build();
    }

    /** 500 - Cualquier otro error no controlado */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        System.err.println("Error no esperado: " + ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error",
                        "Error interno del servidor"));
    }
}
