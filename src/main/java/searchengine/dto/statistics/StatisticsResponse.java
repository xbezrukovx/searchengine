package searchengine.dto.statistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
