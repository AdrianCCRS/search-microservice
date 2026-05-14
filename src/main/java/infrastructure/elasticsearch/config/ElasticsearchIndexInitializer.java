package infrastructure.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${es.index.name:products}")
    private String indexName;

    @Value("${es.index.shards:1}")
    private int shards;

    @Value("${es.index.replicas:0}")
    private int replicas;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() throws Exception {

        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index(indexName))
                .value();

        if (!indexExists) {

            elasticsearchClient.indices().create(c -> c
                    .index(indexName)

                    .settings(s -> s
                            .numberOfShards(String.valueOf(shards))
                            .numberOfReplicas(String.valueOf(replicas))
                    )

                    .mappings(m -> m
                            .properties("productId", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("name", p -> p
                                    .text(t -> t
                                            .analyzer("spanish")
                                            .fields("keyword", f -> f
                                                    .keyword(k -> k)
                                            )
                                    )
                            )

                            .properties("description", p -> p
                                    .text(t -> t
                                            .analyzer("spanish")
                                    )
                            )

                            .properties("category", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("brand", p -> p
                                    .keyword(k -> k)
                            )

                            .properties("price", p -> p
                                    .float_(f -> f)
                            )

                            .properties("rating", p -> p
                                    .float_(f -> f)
                            )

                            .properties("available", p -> p
                                    .boolean_(b -> b)
                            )
                    )
            );

            System.out.println("Índice '" + indexName + "' creado correctamente en Elasticsearch.");

        } else {
            System.out.println("Índice '" + indexName + "' ya existe.");
        }
    }
}