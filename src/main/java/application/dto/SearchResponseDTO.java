package application.dto;

import java.util.List;

public class SearchResponseDTO {

    private long totalResults;
    private int page;
    private int pageSize;
    private List<ProductDTO> products;

    public SearchResponseDTO(long totalResults, int page, int pageSize, List<ProductDTO> products) {
        this.totalResults = totalResults;
        this.page = page;
        this.pageSize = pageSize;
        this.products = products;
    }

    public long getTotalResults() { return totalResults; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public List<ProductDTO> getProducts() { return products; }
}