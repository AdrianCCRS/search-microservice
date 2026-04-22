package application.usecases;

import application.events.ProductData;

public interface IndexProductUseCase {
    void execute(ProductData data);
}
