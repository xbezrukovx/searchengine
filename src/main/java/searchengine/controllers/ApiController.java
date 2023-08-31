package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.BadResponse;
import searchengine.dto.Response;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Response> startIndexing(){
        if(indexingService.indexingAll()){
            return ResponseEntity.ok(new Response(true));

        }
        return new ResponseEntity<Response>(
                new BadResponse(false, "Indexing have been started"),
                HttpStatus.BAD_REQUEST
        );
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Response> stopIndexing(){
        if(indexingService.stopIndexing()){
            return ResponseEntity.ok(new Response(true));
        }
        return new ResponseEntity<Response>(
                new BadResponse(false, "Indexing is shutdown"),
                HttpStatus.BAD_REQUEST
        );
    }
}
