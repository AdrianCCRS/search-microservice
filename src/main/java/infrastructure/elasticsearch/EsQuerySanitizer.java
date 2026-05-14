package infrastructure.elasticsearch;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EsQuerySanitizer {

    private static final Pattern ES_SPECIAL_CHARS = Pattern.compile("[+\\-=&|!(){}\\[\\]^\"~*?:\\\\/><]");

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public String sanitize(String query) {
        if (query == null) {
            return "";
        }
        String stripped = ES_SPECIAL_CHARS.matcher(query).replaceAll(" ");
        return MULTI_SPACE.matcher(stripped).replaceAll(" ").trim();
    }
}
