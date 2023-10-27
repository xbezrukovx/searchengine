package searchengine.dto.search;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class SiteResponse {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;
}
