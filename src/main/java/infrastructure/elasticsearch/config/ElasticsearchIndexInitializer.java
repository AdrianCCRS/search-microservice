package infrastructure.elasticsearch.config;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;

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
            );
            System.out.println("Índice '" + indexName + "' creado correctamente en Elasticsearch.");
        } else {
            System.out.println("Índice '" + indexName + "' ya existe.");
        }
    }
}
