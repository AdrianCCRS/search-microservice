package presentation.controllers;

import domain.entities.SearchDocument;
import infrastructure.search.CachedSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerTest {

    private CachedSearchService cachedSearchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cachedSearchService = mock(CachedSearchService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SearchController(cachedSearchService))
                .build();
    }

    @Test
    void searchReturns200WithResults() throws Exception {
        SearchDocument doc = new SearchDocument(
                "prod-1", "Laptop", "Gaming", "Electronics",
                BigDecimal.valueOf(999.0), 4.5, true, "Dell");
        when(cachedSearchService.search("laptop")).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/search").param("q", "laptop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value("prod-1"))
                .andExpect(jsonPath("$[0].name").value("Laptop"));
    }

    @Test
    void searchReturns400WhenQueryIsBlank() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "  "))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cachedSearchService);
    }

    @Test
    void suggestReturns200WithSuggestions() throws Exception {
        when(cachedSearchService.suggest("phone")).thenReturn(List.of("Phone Case", "Phone Charger"));

        mockMvc.perform(get("/api/search/suggest").param("q", "phone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Phone Case"))
                .andExpect(jsonPath("$[1]").value("Phone Charger"));
    }

    @Test
    void suggestReturns400WhenQueryIsBlank() throws Exception {
        mockMvc.perform(get("/api/search/suggest").param("q", ""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cachedSearchService);
    }
}
