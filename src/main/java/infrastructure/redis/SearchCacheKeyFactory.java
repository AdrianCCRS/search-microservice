package infrastructure.redis;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SearchCacheKeyFactory {

    private static final String SEARCH_QUERY_PREFIX = "search:query:";
    private static final String SEARCH_SUGGEST_PREFIX = "search:suggest:";

    public String searchKey(String query) {
        return SEARCH_QUERY_PREFIX + sha256(normalize(query));
    }

    public String suggestKey(String query) {
        return SEARCH_SUGGEST_PREFIX + normalize(query);
    }

    public String normalize(String query) {
        if (query == null) {
            throw new IllegalArgumentException("query is required");
        }

        return query.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
