package infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import domain.entities.SearchDocument;
import infrastructure.config.SearchApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
class ElasticsearchSearchRepositoryTest {

    private ElasticsearchClient client;
    private SearchApiProperties properties;
    private ElasticsearchSearchRepository repository;

    @BeforeEach
    void setUp() {
        client = mock(ElasticsearchClient.class);
        properties = new SearchApiProperties();
        repository = new ElasticsearchSearchRepository(client, properties);
        ReflectionTestUtils.setField(repository, "indexName", "products");
    }

    // --- indexDocument (production uses IndexRequest.of() — IndexRequest overload) ---

    @Test
    void indexDocument_success() throws IOException {
        IndexResponse indexResponse = mock(IndexResponse.class);
        when(indexResponse.id()).thenReturn("prod-1");
        doReturn(indexResponse).when(client).index(any(IndexRequest.class));

        assertDoesNotThrow(() -> repository.indexDocument(sampleDocument("prod-1", "Laptop")));
        verify(client).index(any(IndexRequest.class));
    }

    @Test
    void indexDocument_throwsRuntimeException_onIOException() throws IOException {
        doThrow(new IOException("connection refused")).when(client).index(any(IndexRequest.class));

        assertThrows(RuntimeException.class,
                () -> repository.indexDocument(sampleDocument("prod-2", "Keyboard")));
    }

    // --- deleteDocument (production uses DeleteRequest.of() — DeleteRequest overload) ---

    @Test
    void deleteDocument_success() throws IOException {
        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        when(deleteResponse.id()).thenReturn("prod-1");
        doReturn(deleteResponse).when(client).delete(any(DeleteRequest.class));

        assertDoesNotThrow(() -> repository.deleteDocument("prod-1"));
        verify(client).delete(any(DeleteRequest.class));
    }

    @Test
    void deleteDocument_throwsRuntimeException_onIOException() throws IOException {
        doThrow(new IOException("connection refused")).when(client).delete(any(DeleteRequest.class));

        assertThrows(RuntimeException.class, () -> repository.deleteDocument("prod-1"));
    }

    // --- search(query) — lambda overload ---

    @Test
    void search_returnsDocuments_whenHitsPresent() throws IOException {
        SearchDocument doc = sampleDocument("prod-1", "Laptop");

        Hit hit = mock(Hit.class);
        when(hit.source()).thenReturn(doc);

        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));

        SearchResponse response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(hitsMetadata);

        doReturn(response).when(client).search((Function) any(), any(Class.class));

        List<SearchDocument> results = repository.search("laptop");

        assertEquals(1, results.size());
        assertEquals("prod-1", results.get(0).getProductId());
    }

    @Test
    void search_returnsEmptyList_whenNoHits() throws IOException {
        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(List.of());

        SearchResponse response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(hitsMetadata);

        doReturn(response).when(client).search((Function) any(), any(Class.class));

        assertTrue(repository.search("nonexistent").isEmpty());
    }

    @Test
    void search_throwsRuntimeException_onIOException() throws IOException {
        doThrow(new IOException("ES unavailable"))
                .when(client).search((Function) any(), any(Class.class));

        assertThrows(RuntimeException.class, () -> repository.search("query"));
    }

    // --- suggest(query, size) — SearchRequest.of() overload ---

    @Test
    void suggest_withSize_returnsNames() throws IOException {
        SearchDocument doc = sampleDocument("prod-1", "Laptop Pro");

        Hit hit = mock(Hit.class);
        when(hit.source()).thenReturn(doc);

        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(List.of(hit));

        SearchResponse response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(hitsMetadata);

        doReturn(response).when(client).search((SearchRequest) any(), any(Class.class));

        List<String> suggestions = repository.suggest("lap", 5);

        assertEquals(1, suggestions.size());
        assertEquals("Laptop Pro", suggestions.get(0));
    }

    @Test
    void suggest_withSize_throwsRuntimeException_onIOException() throws IOException {
        doThrow(new IOException("ES unavailable"))
                .when(client).search((SearchRequest) any(), any(Class.class));

        assertThrows(RuntimeException.class, () -> repository.suggest("lap", 5));
    }

    // --- suggest(query) — lambda overload ---

    @Test
    void suggest_noSize_returnsDistinctNames() throws IOException {
        SearchDocument doc1 = sampleDocument("prod-1", "Laptop");
        SearchDocument doc2 = sampleDocument("prod-2", "Laptop");

        Hit hit1 = mock(Hit.class);
        when(hit1.source()).thenReturn(doc1);
        Hit hit2 = mock(Hit.class);
        when(hit2.source()).thenReturn(doc2);

        HitsMetadata hitsMetadata = mock(HitsMetadata.class);
        when(hitsMetadata.hits()).thenReturn(List.of(hit1, hit2));

        SearchResponse response = mock(SearchResponse.class);
        when(response.hits()).thenReturn(hitsMetadata);

        doReturn(response).when(client).search((Function) any(), any(Class.class));

        List<String> suggestions = repository.suggest("lap");

        assertEquals(1, suggestions.size(), "Duplicate names should be deduplicated");
        assertEquals("Laptop", suggestions.get(0));
    }

    @Test
    void suggest_noSize_throwsRuntimeException_onIOException() throws IOException {
        doThrow(new IOException("ES unavailable"))
                .when(client).search((Function) any(), any(Class.class));

        assertThrows(RuntimeException.class, () -> repository.suggest("lap"));
    }

    // --- helpers ---

    private SearchDocument sampleDocument(String productId, String name) {
        return new SearchDocument(productId, name, "desc", "Electronics",
                BigDecimal.valueOf(999.99), 4.5, true, "TestBrand");
    }
}
