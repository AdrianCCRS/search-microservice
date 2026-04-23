package application.dto;

public class ProductDTO {

    private String id;
    private String name;
    private String description;
    private String brand;
    private double score;

    public ProductDTO(String id, String name, String description, String brand, double score) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.score = score;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBrand() { return brand; }
    public double getScore() { return score; }
}