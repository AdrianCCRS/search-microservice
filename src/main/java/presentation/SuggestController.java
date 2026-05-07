package presentation;

import application.usecases.SuggestProductsUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SuggestController {

    private final SuggestProductsUseCase suggestProductsUseCase;

    public SuggestController(SuggestProductsUseCase suggestProductsUseCase) {
        this.suggestProductsUseCase = suggestProductsUseCase;
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(@RequestParam String q) {
        if (q == null || q.trim().length() < 3) {
            throw new IllegalArgumentException("El parámetro 'q' debe tener al menos 3 caracteres");
        }
        List<String> suggestions = suggestProductsUseCase.execute(q.trim());
        return ResponseEntity.ok(suggestions);
    }
}
