package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.models.SiteModel;
import searchengine.repos.LemmaRepository;
import searchengine.repos.PageRepository;
import searchengine.repos.SiteRepository;
import searchengine.services.StatisticsService;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;


    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(IndexingServiceImpl.getIsIndexing());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<SiteModel> sitesList = (List<SiteModel>) siteRepository.findAll();
        sitesList.forEach(site -> {
            int pages = pageRepository.findBySiteModel(site).size();
            int lemmas = lemmaRepository.findBySiteModel(site).size();
            DetailedStatisticsItem item = DetailedStatisticsItem.builder()
                    .name(site.getName())
                    .url(site.getUrl())
                    .pages(pages)
                    .lemmas(lemmas)
                    .status(site.getStatus().toString())
                    .error(site.getLastError())
                    .statusTime(Timestamp.valueOf(site.getStatusTime()).getTime())
                    .build();
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        });

        StatisticsData data = StatisticsData.builder()
                .total(total)
                .detailed(detailed)
                .build();
        return StatisticsResponse.builder()
                .statistics(data)
                .result(true)
                .build();
    }
}