package searchengine.services;

import org.springframework.stereotype.Service;

public interface IndexingService {
    boolean indexingAll();
    boolean stopIndexing();
}
