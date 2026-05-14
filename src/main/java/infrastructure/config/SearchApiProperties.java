package infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for Search API tunables.
 * All magic numbers that were previously hardcoded in controllers and
 * infrastructure classes are centralized here and bound from application.yml
 * under the {@code search.api} prefix.
 */
@Validated
@ConfigurationProperties(prefix = "search.api")
public class SearchApiProperties {

    /** Maximum character length allowed for the query parameter 'q'. */
    private int queryMaxLength = 200;

    /** Pages above this threshold require a {@code searchAfter} cursor. */
    private int paginationMaxPageWithoutSearchAfter = 100;

    /** {@code max_expansions} for Elasticsearch match_phrase_prefix queries. */
    private int esSuggestMaxExpansions = 10;

    public int getQueryMaxLength() {
        return queryMaxLength;
    }

    public void setQueryMaxLength(int queryMaxLength) {
        this.queryMaxLength = queryMaxLength;
    }

    public int getPaginationMaxPageWithoutSearchAfter() {
        return paginationMaxPageWithoutSearchAfter;
    }

    public void setPaginationMaxPageWithoutSearchAfter(int paginationMaxPageWithoutSearchAfter) {
        this.paginationMaxPageWithoutSearchAfter = paginationMaxPageWithoutSearchAfter;
    }

    public int getEsSuggestMaxExpansions() {
        return esSuggestMaxExpansions;
    }

    public void setEsSuggestMaxExpansions(int esSuggestMaxExpansions) {
        this.esSuggestMaxExpansions = esSuggestMaxExpansions;
    }
}
