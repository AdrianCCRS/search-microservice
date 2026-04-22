package presentation;

import application.dto.SearchPageResult;
import application.usecases.SearchProductsUseCase;
import domain.entities.SearchDocument;
import domain.search.SearchQuery;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = SearchController.class,
    excludeAutoConfiguration = {
        ElasticsearchClientAutoConfiguration.class,
        ElasticsearchDataAutoConfiguration.class,
        ElasticsearchRepositoriesAutoConfiguration.class,
        RabbitAutoConfiguration.class
    }
)
@ActiveProfiles("webtest")
@Import(RestExceptionHandler.class)
class SearchControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchProductsUseCase searchProductsUseCase;

    @Test
    void searchReturnsPaginationFields() throws Exception {
        SearchDocument doc = new SearchDocument(
            "p1", "Test", "Desc", "cat", BigDecimal.TEN, 4.5, true, "brand"
        );
        when(searchProductsUseCase.execute(any(SearchQuery.class)))
            .thenReturn(new SearchPageResult(42L, 0, 10, List.of(doc)));

        mockMvc.perform(get("/api/v1/products/search")
                .param("page", "0")
                .param("pageSize", "10")
                .param("sort", "price_asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalResults").value(42))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.items[0].productId").value("p1"));

        verify(searchProductsUseCase).execute(any(SearchQuery.class));
    }

    @Test
    void invalidSortReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/products/search").param("sort", "unknown"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
}
