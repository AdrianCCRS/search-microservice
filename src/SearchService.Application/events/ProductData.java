package application.events;

public record ProductData(
    String productId, 
    String name, 
    String description,
    String category, 
    Double price, 
    Double rating,
    Boolean available, 
    String brand
) {}
