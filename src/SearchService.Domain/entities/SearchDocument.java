package domain.entities;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.math.BigDecimal;

/**
 * Representa el documento derivado optimizado para indexación y búsqueda rápida (Read Model en CQRS).
 * Esta entidad no refleja datos transaccionales crudos de forma directa.
 */
@Document(indexName = "products") // Nombre del índice en Elasticsearch
public class SearchDocument {
    
    @Field(type = FieldType.Keyword)
    private String productId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double rating;

    @Field(type = FieldType.Boolean)
    private Boolean available;

    @Field(type = FieldType.Keyword)
    private String brand;

    // Constructores
    public SearchDocument() {}

    public SearchDocument(String productId, String name, String description, String category, 
                          BigDecimal price, Double rating, Boolean available, String brand) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.rating = rating;
        this.available = available;
        this.brand = brand;
    }

    // Getters y Setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
}