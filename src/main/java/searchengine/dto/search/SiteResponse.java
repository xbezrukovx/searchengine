package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SiteResponse {
    String site;
    String siteName;
    String uri;
    String title;
    String snippet;
    float relevance;
}
