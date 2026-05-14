package infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import domain.entities.SearchDocument;
import infrastructure.config.SearchApiProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class ElasticsearchSearchRepository {

    private final ElasticsearchClient client;
    private final SearchApiProperties searchApiProperties;

    @Value("${es.index.name:products}")
    private String indexName;

    public ElasticsearchSearchRepository(ElasticsearchClient client, SearchApiProperties searchApiProperties) {
        this.client = client;
        this.searchApiProperties = searchApiProperties;
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

    public List<String> suggest(String query, int size) {
        try {
            SearchResponse<SearchDocument> response = client.search(
                SearchRequest.of(s -> s
                    .index(indexName)
                    .size(size)
                    .query(q -> q
                        .matchPhrasePrefix(m -> m
                            .field("name")
                            .query(query)
                            .maxExpansions(searchApiProperties.getEsSuggestMaxExpansions())
                        )
                    )
                    .source(src -> src
                        .filter(f -> f.includes("name"))
                    )
                ),
                SearchDocument.class
            );

            List<String> suggestions = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                if (hit.source() != null && hit.source().getName() != null) {
                    suggestions.add(hit.source().getName());
                }
            });
            return suggestions;
        } catch (ElasticsearchException | java.io.IOException e) {
            System.err.println("Error obteniendo sugerencias: " + e.getMessage());
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
                            .fuzziness("AUTO")
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