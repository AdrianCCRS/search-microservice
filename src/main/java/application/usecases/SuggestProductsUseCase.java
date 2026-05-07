package application.usecases;

import presentation.dto.SuggestResponse;

public interface SuggestProductsUseCase {
    SuggestResponse execute(String query);
}
