package application.usecases;

import domain.entities.SearchDocument;
import java.util.List;

public interface SearchProductsUseCase {
    List<SearchDocument> search(String q);
}