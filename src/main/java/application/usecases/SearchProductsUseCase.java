package application.usecases;

import java.util.List;
import domain.entities.SearchDocument;

public interface SearchProductsUseCase {
    List<SearchDocument> search(String q);
}