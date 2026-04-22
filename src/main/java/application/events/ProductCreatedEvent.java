package application.events;

public record ProductCreatedEvent(
    String eventType,
    String version,
    String timestamp,
    ProductData data
) {}
