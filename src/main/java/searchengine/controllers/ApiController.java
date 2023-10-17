package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.BadResponse;
import searchengine.dto.Response;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.SearchServiceImpl;
import searchengine.services.StatisticsService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing() {
        if (indexingService.indexingAll()) {
            return ResponseEntity.ok(new Response(true));

        }
        return new ResponseEntity<Response>(
                new BadResponse(false, "Indexing have been started"),
                HttpStatus.BAD_REQUEST
        );
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return ResponseEntity.ok(new Response(true));
        }
        return new ResponseEntity<Response>(
                new BadResponse(false, "Indexing is shutdown"),
                HttpStatus.BAD_REQUEST
        );
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Response> indexPage(String url) {
        String error = indexingService.indexPage(url);
        if (error == null) {
            return ResponseEntity.ok(new Response(true));
        }
        return new ResponseEntity<Response>(
                new BadResponse(false, error),
                HttpStatus.BAD_REQUEST
        );
    }

    //TODO: Return some model
    @GetMapping("/search")
    public SearchResponse search(String query, String site, int offset, int limit) throws IOException {
        if (query == null) return null;
        if (site == null){
            return searchService.allSiteSearch(query, offset, limit);
        } else {
            SiteModel siteModel = searchService.findSiteModel(site);
            if (siteModel == null) return null;
            return searchService.siteSearch(query, siteModel, offset, limit);
        }
    }
}
