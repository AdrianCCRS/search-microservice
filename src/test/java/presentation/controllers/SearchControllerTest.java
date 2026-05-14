package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.elasticsearch.EsQuerySanitizer;
import infrastructure.search.CachedSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import presentation.advice.GlobalExceptionHandler;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTest {

    private CachedSearchService cachedSearchService;
    private EsQuerySanitizer esQuerySanitizer;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cachedSearchService = mock(CachedSearchService.class);
        esQuerySanitizer = mock(EsQuerySanitizer.class);
        when(esQuerySanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SearchController(cachedSearchService, esQuerySanitizer))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchReturns200WithCompleteResponseStructure() throws Exception {
        SearchDocument doc = new SearchDocument(
                "prod-1", "Laptop", "Gaming", "Electronics",
                BigDecimal.valueOf(999.0), 4.5, true, "Dell");
        when(cachedSearchService.search("laptop")).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search").param("q", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("prod-1"))
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].description").value("Gaming"))
                .andExpect(jsonPath("$[0].category").value("Electronics"))
                .andExpect(jsonPath("$[0].price").value(999.0))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].brand").value("Dell"));
    }

    @Test
    void searchReturns400WithErrorResponseWhenQueryIsBlank() throws Exception {
        mockMvc.perform(get("/api/search").param("q", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'q' es requerido"));

        verifyNoInteractions(cachedSearchService);
    }

    @Test
    void searchReturns400WithErrorResponseWhenQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'q' es requerido"));

        verifyNoInteractions(cachedSearchService);
    }

    @Test
    void searchReturns200WithFilters() throws Exception {
        when(cachedSearchService.search("laptop")).thenReturn(List.of());

        mockMvc.perform(get("/api/search")
                        .param("q", "laptop")
                        .param("category", "Computación")
                        .param("minPrice", "100")
                        .param("maxPrice", "2000")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchReturns400WhenPageTooHighWithoutSearchAfter() throws Exception {
        mockMvc.perform(get("/api/search")
                        .param("q", "laptop")
                        .param("page", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Para page > 100 debes enviar searchAfter"));

        verifyNoInteractions(cachedSearchService);
    }
}
