package application.usecases;

import application.dto.SearchPageResult;
import application.ports.ProductSearchPort;
import domain.search.SearchQuery;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!webtest")
public class SearchProductsUseCaseImpl implements SearchProductsUseCase {

    private final ProductSearchPort productSearchPort;

    public SearchProductsUseCaseImpl(ProductSearchPort productSearchPort) {
        this.productSearchPort = productSearchPort;
    }

    @Override
    public SearchPageResult execute(SearchQuery query) {
        return productSearchPort.search(query);
    }
}
