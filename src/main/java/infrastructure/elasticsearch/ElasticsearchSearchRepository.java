package infrastructure.elasticsearch;

import application.dto.SearchPageResult;
import application.ports.ProductSearchPort;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import domain.entities.SearchDocument;
import domain.search.SearchQuery;
import domain.search.SearchSort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
@Profile("!webtest")
public class ElasticsearchSearchRepository implements ProductSearchPort {

    private final ElasticsearchClient client;
    private final String indexName;

    public ElasticsearchSearchRepository(
        ElasticsearchClient client,
        @Value("${es.index.name:products}") String indexName
    ) {
        this.client = client;
        this.indexName = indexName;
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
        } catch (ElasticsearchException | IOException e) {
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
        } catch (ElasticsearchException | IOException e) {
            System.err.println("Error eliminando documento: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SearchPageResult search(SearchQuery query) {
        try {
            // Build sort options: primary sort + tiebreaker by productId (required for search_after correctness)
            List<SortOptions> sortOptions = buildElasticsearchSort(query.sort());
            sortOptions.add(SortOptions.of(s -> s.field(f -> f
                .field("productId")
                .order(SortOrder.Asc)
            )));

            SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
                .index(indexName)
                .size(query.pageSize())
                .trackTotalHits(t -> t.enabled(true))
                .query(q -> q.matchAll(m -> m));

            for (SortOptions so : sortOptions) {
                requestBuilder.sort(so);
            }

            if (query.hasCursor()) {
                // Cursor-based pagination: use search_after for deep navigation without performance penalty
                List<FieldValue> cursorValues = query.searchAfter().stream()
                    .map(FieldValue::of)
                    .toList();
                requestBuilder.searchAfter(cursorValues);
            } else {
                // Offset-based pagination: only allowed for page <= MAX_OFFSET_PAGE (enforced in SearchQuery)
                requestBuilder.from(query.fromIndex());
            }

            SearchResponse<SearchDocument> response = client.search(
                requestBuilder.build(),
                SearchDocument.class
            );

            long total = response.hits().total() != null ? response.hits().total().value() : 0L;

            List<Hit<SearchDocument>> hits = response.hits().hits();
            List<SearchDocument> items = hits.stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .toList();

            // Extract the sort values from the last hit to use as cursor for the next page
            List<String> nextSearchAfter = null;
            if (!hits.isEmpty()) {
                Hit<SearchDocument> lastHit = hits.get(hits.size() - 1);
                if (lastHit.sort() != null && !lastHit.sort().isEmpty()) {
                    nextSearchAfter = lastHit.sort().stream()
                        .map(FieldValue::_toJsonString)
                        .toList();
                }
            }

            return new SearchPageResult(total, query.page(), query.pageSize(), nextSearchAfter, items);

        } catch (ElasticsearchException | IOException e) {
            System.err.println("Error buscando documentos: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static List<SortOptions> buildElasticsearchSort(SearchSort sort) {
        List<SortOptions> sorts = new ArrayList<>();
        switch (sort) {
            case PRICE_ASC -> sorts.add(SortOptions.of(s -> s.field(f -> f
                .field("price")
                .order(SortOrder.Asc)
                .missing("_last")
            )));
            case PRICE_DESC -> sorts.add(SortOptions.of(s -> s.field(f -> f
                .field("price")
                .order(SortOrder.Desc)
                .missing("_last")
            )));
            case RATING_DESC, POPULARITY -> sorts.add(SortOptions.of(s -> s.field(f -> f
                .field("rating")
                .order(SortOrder.Desc)
                .missing("_last")
            )));
        }
        return sorts;
    }
}
