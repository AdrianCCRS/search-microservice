package presentation;

import application.usecases.SuggestProductsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import presentation.dto.SuggestResponse;

@RestController
@RequestMapping("/api/search")
public class SuggestController {

    private final SuggestProductsUseCase suggestProductsUseCase;

    public SuggestController(SuggestProductsUseCase suggestProductsUseCase) {
        this.suggestProductsUseCase = suggestProductsUseCase;
    }

    /**
     * Retorna sugerencias de autocompletado basadas en el prefijo {@code q}.
     *
     * @param q prefijo de búsqueda (mínimo 3 caracteres)
     * @return {@link SuggestResponse} con la query, lista de sugerencias y total
     */
    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(@RequestParam String q) {
        if (q == null || q.trim().length() < 3) {
            throw new IllegalArgumentException("El parámetro 'q' debe tener al menos 3 caracteres");
        }
        SuggestResponse response = suggestProductsUseCase.execute(q.trim());
        return ResponseEntity.ok(response);
    }
}
