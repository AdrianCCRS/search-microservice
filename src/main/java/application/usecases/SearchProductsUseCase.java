package application.usecases;

import java.util.List;
import application.domain.SearchDocument;

public interface SearchProductsUseCase {
    List<SearchDocument> search(String q);
}