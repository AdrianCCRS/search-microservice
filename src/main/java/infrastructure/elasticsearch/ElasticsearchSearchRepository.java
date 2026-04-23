package infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.json.JsonData;

import org.springframework.stereotype.Repository;

import domain.entities.SearchDocument;
import domain.queries.SearchQuery;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

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

    public List<SearchDocument> search(SearchQuery query) {
        try {
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder().index(indexName);

            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
            boolean hasFilters = false;

            if (query.getTerm() != null && !query.getTerm().trim().isEmpty()) {
                boolQueryBuilder.must(m -> m
                    .multiMatch(mm -> mm
                        .query(query.getTerm())
                        .fields("name", "description", "brand")
                    )
                );
                hasFilters = true;
            }

            if (query.getCategory() != null && !query.getCategory().trim().isEmpty()) {
                boolQueryBuilder.filter(f -> f
                    .term(t -> t
                        .field("category")
                        .value(FieldValue.of(query.getCategory()))
                    )
                );
                hasFilters = true;
            }

            if (query.getMinPrice() != null || query.getMaxPrice() != null) {
                boolQueryBuilder.filter(f -> f
                    .range(r -> {
                        r.field("price");
                        if (query.getMinPrice() != null) r.gte(JsonData.of(query.getMinPrice()));
                        if (query.getMaxPrice() != null) r.lte(JsonData.of(query.getMaxPrice()));
                        return r;
                    })
                );
                hasFilters = true;
            }

            if (query.getAvailable() != null) {
                boolQueryBuilder.filter(f -> f
                    .term(t -> t
                        .field("available")
                        .value(FieldValue.of(query.getAvailable()))
                    )
                );
                hasFilters = true;
            }

            if (query.getMinRating() != null) {
                boolQueryBuilder.filter(f -> f
                    .range(r -> r
                        .field("rating")
                        .gte(JsonData.of(query.getMinRating()))
                    )
                );
                hasFilters = true;
            }

            if (hasFilters) {
                searchBuilder.query(q -> q.bool(boolQueryBuilder.build()));
            }

            if (query.getSortBy() != null && !query.getSortBy().trim().isEmpty()) {
                String sortField = query.getSortBy();
                if ("popularity".equalsIgnoreCase(sortField)) {
                    sortField = "rating";
                }
                
                SortOrder sortOrder = SortOrder.Asc;
                if ("desc".equalsIgnoreCase(query.getSortDirection())) {
                    sortOrder = SortOrder.Desc;
                }
                
                final String finalSortField = sortField;
                final SortOrder finalSortOrder = sortOrder;
                
                searchBuilder.sort(s -> s
                    .field(f -> f
                        .field(finalSortField)
                        .order(finalSortOrder)
                    )
                );
            }

            SearchResponse<SearchDocument> response = client.search(searchBuilder.build(), SearchDocument.class);

            List<SearchDocument> results = new ArrayList<>();
            for (Hit<SearchDocument> hit : response.hits().hits()) {
                results.add(hit.source());
            }
            return results;

        } catch (ElasticsearchException | IOException e) {
            System.err.println("Error ejecutando busqueda: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
