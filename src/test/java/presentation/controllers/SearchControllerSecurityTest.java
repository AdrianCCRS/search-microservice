package presentation.controllers;

import infrastructure.config.SearchApiProperties;
import infrastructure.elasticsearch.EsQuerySanitizer;
import infrastructure.search.CachedSearchService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import presentation.advice.GlobalExceptionHandler;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SearchControllerSecurityTest {

    private CachedSearchService cachedSearchService;
    private EsQuerySanitizer esQuerySanitizer;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cachedSearchService = mock(CachedSearchService.class);
        esQuerySanitizer = new EsQuerySanitizer();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new SearchController(cachedSearchService, esQuerySanitizer, new SearchApiProperties()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchReturns400WhenQueryExceeds200Characters() throws Exception {
        String longQuery = "a".repeat(201);

        mockMvc.perform(get("/api/search").param("q", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'q' no debe exceder 200 caracteres"));

        verifyNoInteractions(cachedSearchService);
    }

    @Test
    void suggestReturns400WhenQueryExceeds200Characters() throws Exception {
        String longQuery = "a".repeat(201);

        mockMvc.perform(get("/api/search/suggest").param("q", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'q' no debe exceder 200 caracteres"));

        verifyNoInteractions(cachedSearchService);
    }

    @Test
    void searchAccepts200CharacterQuery() throws Exception {
        String maxQuery = "a".repeat(200);
        when(cachedSearchService.search(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", maxQuery))
                .andExpect(status().isOk());
    }

    @Test
    void suggestAccepts200CharacterQuery() throws Exception {
        String maxQuery = "a".repeat(200);
        when(cachedSearchService.suggest(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search/suggest").param("q", maxQuery))
                .andExpect(status().isOk());
    }

    @Test
    void searchSanitizesMaliciousDSLQuery() throws Exception {
        String maliciousQuery = "{\"bool\":{\"must\":[{\"match_all\":{}}]}}";
        when(cachedSearchService.search(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", maliciousQuery))
                .andExpect(status().isOk());

        verify(cachedSearchService).search("bool must match_all");
    }

    @Test
    void searchSanitizesWildcardInjection() throws Exception {
        when(cachedSearchService.search(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", "lap*"))
                .andExpect(status().isOk());

        verify(cachedSearchService).search("lap");
    }

    @Test
    void searchSanitizesQueryStringOperators() throws Exception {
        when(cachedSearchService.search(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", "name:admin OR 1=1"))
                .andExpect(status().isOk());

        verify(cachedSearchService).search("name admin OR 1 1");
    }

    @Test
    void suggestSanitizesMaliciousInput() throws Exception {
        when(cachedSearchService.suggest(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search/suggest").param("q", "test~^boost*"))
                .andExpect(status().isOk());

        verify(cachedSearchService).suggest("test boost");
    }

    @Test
    void searchSanitizesBoostInjection() throws Exception {
        when(cachedSearchService.search(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/search").param("q", "laptop^999 OR password"))
                .andExpect(status().isOk());

        verify(cachedSearchService).search("laptop 999 OR password");
    }
}
