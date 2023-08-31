package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatusType;
import searchengine.parser.SiteParser;
import searchengine.repos.Repos;
import searchengine.parser.PageParser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    public static final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private List<Thread> tasks = new ArrayList<>();

    @Override
    public boolean indexingAll() {
        if (isIndexing.get()) {
            return false;
        }
        isIndexing.set(true);
        ExecutorService executorService = Executors.newCachedThreadPool();
        sites.getSites().forEach(site -> {
            SiteParser siteParser = new SiteParser(site);
            siteParser.start();
            tasks.add(siteParser);
        });
        Thread waitingThread = new Thread(() -> {
            tasks.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            isIndexing.set(false);
        });
        waitingThread.start();
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (!isIndexing.get()) {
            return false;
        }
        isIndexing.set(false);
        return true;
    }
}
