package infrastructure.mongodb;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class MongoSeedDataInitializer {

    private static final String COLLECTION_NAME = "search_documents";

    private final MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialData() {
        long existingDocuments = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();

        if (existingDocuments > 0) {
            log.info("Mongo collection '{}' already contains {} documents. Skipping seed.",
                    COLLECTION_NAME, existingDocuments);
            return;
        }

        List<Document> seedDocuments = List.of(
                searchDocument("p-1001", "Gaming Laptop X", "High performance laptop for gaming and productivity.",
                        "Laptops", 1599.99, 4.8, true, "Acer"),
                searchDocument("p-1002", "Wireless Mouse Pro", "Ergonomic wireless mouse with silent clicks.",
                        "Accessories", 39.99, 4.6, true, "Logitech"),
                searchDocument("p-1003", "Laptop Stand Foldable", "Adjustable stand for better posture and cooling.",
                        "Accessories", 24.99, 4.4, true, "Nulaxy")
        );

        mongoTemplate.insert(seedDocuments, COLLECTION_NAME);

        log.info("Inserted {} seed documents into Mongo collection '{}'.", seedDocuments.size(), COLLECTION_NAME);
    }

    private Document searchDocument(String productId,
                                    String name,
                                    String description,
                                    String category,
                                    double price,
                                    double rating,
                                    boolean available,
                                    String brand) {
        return new Document("productId", productId)
                .append("name", name)
                .append("description", description)
                .append("category", category)
                .append("price", price)
                .append("rating", rating)
                .append("available", available)
                .append("brand", brand);
    }
}