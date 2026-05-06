package presentation.dto;

import java.util.List;

/**
 * DTO de respuesta unificado para el endpoint de búsqueda.
 * Incluye metadatos de paginación y la lista de productos con su score de relevancia.
 */
public class SearchResponse {

    private int totalResults;
    private int page;
    private int pageSize;
    private List<ProductResult> products;

    public SearchResponse() {}

    public SearchResponse(int totalResults, int page, int pageSize, List<ProductResult> products) {
        this.totalResults = totalResults;
        this.page = page;
        this.pageSize = pageSize;
        this.products = products;
    }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public List<ProductResult> getProducts() { return products; }
    public void setProducts(List<ProductResult> products) { this.products = products; }

    /**
     * Representa un producto individual dentro de la respuesta de búsqueda,
     * incluyendo su score de relevancia asignado por Elasticsearch.
     */
    public static class ProductResult {
        private String productId;
        private String name;
        private String description;
        private String category;
        private Double price;
        private Double rating;
        private Boolean available;
        private String brand;
        private Double score;

        public ProductResult() {}

        public ProductResult(String productId, String name, String description, String category,
                             Double price, Double rating, Boolean available, String brand, Double score) {
            this.productId = productId;
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
            this.rating = rating;
            this.available = available;
            this.brand = brand;
            this.score = score;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }

        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
    }
}
