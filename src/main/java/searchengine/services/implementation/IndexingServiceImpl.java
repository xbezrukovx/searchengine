package searchengine.services.implementation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.models.*;
import searchengine.repos.IndexRepository;
import searchengine.repos.LemmaRepository;
import searchengine.utils.LemmaPageParser;
import searchengine.utils.PageParser;
import searchengine.utils.SiteParser;
import searchengine.repos.PageRepository;
import searchengine.repos.SiteRepository;
import searchengine.services.IndexingService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final List<Thread> tasks = new ArrayList<>();
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaPageParser lemmaPageParser;
    private static Thread waitingThread;
    private static final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private final Logger logger = LogManager.getRootLogger();

    public IndexingServiceImpl(
            SiteRepository siteRepository,
            PageRepository pageRepository,
            IndexRepository indexRepository,
            LemmaRepository lemmaRepository,
            LemmaPageParser lemmaPageParser,
            SitesList sites
    ) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.lemmaPageParser = lemmaPageParser;
        this.sites = sites;
    }

    @Override
    public boolean indexingAll() {
        if (isIndexing.get()) return false;
        if (waitingThread != null && waitingThread.isAlive()) return false;
        isIndexing.set(true);
        logger.info("Indexing has been started.");
        sites.getSites().forEach(site -> {
            SiteParser siteParser = new SiteParser(site, pageRepository, siteRepository, indexRepository, lemmaRepository, lemmaPageParser);
            tasks.add(siteParser);
            siteParser.start();
        });
        waitingThread = new Thread(() -> {
            tasks.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            isIndexing.set(false);
            logger.info("Indexing has been finished.");
        });
        waitingThread.start();
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (waitingThread == null) return false;
        isIndexing.set(false);
        if (waitingThread.isAlive()){
            logger.info("Threads are working now.");
            return true;
        };
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

        Optional<SiteModel> siteModelOptional = siteRepository.findByUrl("http://"+domain);
        if (siteModelOptional.isEmpty()) {
            siteModelOptional = siteRepository.findByUrl("https://"+domain);
            if (siteModelOptional.isEmpty()) {
                logger.info("This site is not in the configuration.");
                return "This site is not in the configuration.";
            }
        }

        SiteModel siteModel = siteModelOptional.get();
        new PageParser(siteModel,siteModel.getUrl()+path, pageRepository, siteRepository, lemmaPageParser)
            .processThePage();

        return null;
    }

    public static boolean getIsIndexing() {
        return isIndexing.get();
    }
}
