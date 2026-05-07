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
        when(suggestProductsUseCase.execute("phone")).thenReturn(List.of("Phone Case", "Phone Charger"));

        mockMvc.perform(get("/api/search/suggest").param("q", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Phone Case"))
                .andExpect(jsonPath("$[1]").value("Phone Charger"));
    }

    @Test
    void suggestReturns400WhenQueryTooShort() throws Exception {
        mockMvc.perform(get("/api/search/suggest").param("q", "ab"))
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
    void suggestTrimsQueryBeforeValidation() throws Exception {
        when(suggestProductsUseCase.execute("abc")).thenReturn(List.of("Abc Product"));

        mockMvc.perform(get("/api/search/suggest").param("q", "  abc  "))
                .andExpect(status().isOk());

        verify(suggestProductsUseCase).execute("abc");
    }
}
