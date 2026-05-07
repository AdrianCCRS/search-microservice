package application.usecases;

import application.events.ProductData;

public interface InvalidateCacheUseCase {
    void invalidateProductUpdated(ProductData data);

    void invalidateProductCreated(ProductData data);
}
