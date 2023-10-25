package searchengine.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SearchResponse {
    boolean result;
    int count;
    List<SiteResponse> data;
    String error;
}
