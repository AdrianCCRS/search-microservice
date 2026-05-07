package application.usecases;

import java.util.List;

public interface SuggestProductsUseCase {
    List<String> execute(String query);
}
