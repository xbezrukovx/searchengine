package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatusType;
import searchengine.parser.LemmaPageParser;
import searchengine.parser.SiteParser;
import searchengine.repos.Repos;
import searchengine.parser.PageParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
        sites.getSites().forEach(site -> {
            SiteParser siteParser = new SiteParser(site);
            tasks.add(siteParser);
            siteParser.start();
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

    @Override
    public String indexPage(String url) {
        String domain;
        String path;
        try {
            domain = new URL(url).getHost();
            path = new URL(url).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Optional<SiteModel> siteModelOptional = Repos.siteRepository.findByUrl(domain);
        if (siteModelOptional.isEmpty()) return "Сайта нет в конфигурации";
        SiteModel siteModel = siteModelOptional.get();
        Optional<Page> pageOptional = Repos.pageRepository.findByPathAndSiteModel(path, siteModel);
        Page page;
        page = pageOptional.orElseGet(() -> new PageParser(siteModel, path).processSinglePage(path));
        LemmaPageParser lemmaPageParser = new LemmaPageParser(page);
        try {
            lemmaPageParser.createLemma();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
