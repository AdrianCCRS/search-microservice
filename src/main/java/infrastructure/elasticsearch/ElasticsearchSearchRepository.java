package infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import domain.entities.SearchDocument;

@Repository
public class ElasticsearchSearchRepository {

    private final ElasticsearchClient client;
    private final String indexName = "products";

    public ElasticsearchSearchRepository(ElasticsearchClient client) {
        this.client = client;
    }

    public void indexDocument(SearchDocument doc) {
        try {
            IndexResponse response = client.index(
                IndexRequest.of(i -> i
                    .index(indexName)
                    .id(doc.getProductId())
                    .document(doc)
                )
            );
            System.out.println("Documento indexado: " + response.id());
        } catch (ElasticsearchException | java.io.IOException e) {
            System.err.println("Error indexando documento: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void deleteDocument(String productId) {
        try {
            DeleteResponse response = client.delete(
                DeleteRequest.of(d -> d
                    .index(indexName)
                    .id(productId)
                )
            );
            System.out.println("Documento eliminado: " + response.id());
        } catch (ElasticsearchException | java.io.IOException e) {
            System.err.println("Error eliminando documento: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<SearchDocument> search(String query) {
        try {
            SearchResponse<SearchDocument> response = client.search(s -> s
                    .index(indexName)
                    .query(q -> q.multiMatch(m -> m
                            .query(query)
                            .fields("name^3", "description", "category", "brand")
                    )),
                    SearchDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (ElasticsearchException | java.io.IOException e) {
            System.err.println("Error searching documents: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<String> suggest(String query) {
        try {
            SearchResponse<SearchDocument> response = client.search(s -> s
                    .index(indexName)
                    .size(10)
                    .query(q -> q.matchPhrasePrefix(m -> m
                            .field("name")
                            .query(query)
                    )),
                    SearchDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(SearchDocument::getName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(10)
                    .toList();
        } catch (ElasticsearchException | java.io.IOException e) {
            System.err.println("Error getting search suggestions: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
