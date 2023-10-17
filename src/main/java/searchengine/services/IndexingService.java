package searchengine.services;

import org.springframework.stereotype.Service;

import java.io.IOException;

public interface IndexingService {
    boolean indexingAll();
    boolean stopIndexing();
    String indexPage(String url);
}
