package infrastructure.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ElasticsearchIndexInitializerTest {

    private ElasticsearchClient esClient;
    private ElasticsearchIndicesClient indicesClient;
    private ElasticsearchIndexInitializer initializer;

    @BeforeEach
    void setUp() {
        esClient = mock(ElasticsearchClient.class);
        indicesClient = mock(ElasticsearchIndicesClient.class);
        when(esClient.indices()).thenReturn(indicesClient);

        initializer = new ElasticsearchIndexInitializer(esClient);
        ReflectionTestUtils.setField(initializer, "indexName", "products");
        ReflectionTestUtils.setField(initializer, "shards", 1);
        ReflectionTestUtils.setField(initializer, "replicas", 0);
    }

    @Test
    void initializeIndex_createsIndex_whenIndexDoesNotExist() throws Exception {
        BooleanResponse existsResponse = mock(BooleanResponse.class);
        when(existsResponse.value()).thenReturn(false);
        doReturn(existsResponse).when(indicesClient).exists(any(Function.class));

        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);
        when(createResponse.acknowledged()).thenReturn(true);
        doReturn(createResponse).when(indicesClient).create(any(Function.class));

        initializer.initializeIndex();

        verify(indicesClient).exists(any(Function.class));
        verify(indicesClient).create(any(Function.class));
    }

    @Test
    void initializeIndex_skipsCreation_whenIndexAlreadyExists() throws Exception {
        BooleanResponse existsResponse = mock(BooleanResponse.class);
        when(existsResponse.value()).thenReturn(true);
        doReturn(existsResponse).when(indicesClient).exists(any(Function.class));

        initializer.initializeIndex();

        verify(indicesClient).exists(any(Function.class));
        verify(indicesClient, never()).create(any(Function.class));
    }

    @Test
    void initializeIndex_propagatesException_whenClientFails() throws Exception {
        doThrow(new IOException("ES unavailable")).when(indicesClient).exists(any(Function.class));

        // The method declares throws Exception; callers must handle it
        org.junit.jupiter.api.Assertions.assertThrows(
                IOException.class,
                () -> initializer.initializeIndex()
        );
    }
}
