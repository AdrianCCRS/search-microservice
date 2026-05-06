package domain.queries;

import java.math.BigDecimal;

public class SearchQuery {
    private String term;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean available;
    private Double minRating;
    private String sortBy;
    private String sortDirection;
    private Integer page;
    private Integer pageSize;

    public SearchQuery() {}

    public SearchQuery(String term, String category, BigDecimal minPrice, BigDecimal maxPrice,
                       Boolean available, Double minRating, String sortBy, String sortDirection,
                       Integer page, Integer pageSize) {
        this.term = term;
        this.category = category;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.available = available;
        this.minRating = minRating;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
        this.page = page;
        this.pageSize = pageSize;
    }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }

    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Double getMinRating() { return minRating; }
    public void setMinRating(Double minRating) { this.minRating = minRating; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
