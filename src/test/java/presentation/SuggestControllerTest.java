package presentation;

import application.usecases.SuggestProductsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import presentation.advice.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SuggestControllerTest {

    private SuggestProductsUseCase suggestProductsUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        suggestProductsUseCase = mock(SuggestProductsUseCase.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SuggestController(suggestProductsUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void suggestReturns200WithResults() throws Exception {
        when(suggestProductsUseCase.execute("Zap")).thenReturn(List.of("Zapato", "Zapatera"));

        mockMvc.perform(get("/api/search/suggest").param("q", "Zap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Zapato"))
                .andExpect(jsonPath("$[1]").value("Zapatera"));
    }

    @Test
    void suggestReturns400WhenQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/search/suggest").param("q", "Za"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(suggestProductsUseCase);
    }

    @Test
    void suggestReturns400WhenQueryIsEmpty() throws Exception {
        mockMvc.perform(get("/api/search/suggest").param("q", ""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(suggestProductsUseCase);
    }

    @Test
    void suggestReturns400WhenQueryIsMissing() throws Exception {
        mockMvc.perform(get("/api/search/suggest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'q' es requerido"));

        verifyNoInteractions(suggestProductsUseCase);
    }
}
