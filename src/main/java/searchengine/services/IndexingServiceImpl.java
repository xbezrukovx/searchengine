package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.lemma.LemmaFinder;
import searchengine.model.*;
import searchengine.parser.LemmaPageParser;
import searchengine.parser.SiteParser;
import searchengine.repos.Repos;
import searchengine.parser.PageParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    public static final AtomicBoolean isIndexing = new AtomicBoolean(false);
    private List<Thread> tasks = new ArrayList<>();
    private static Thread waitingThread;

    @Override
    public boolean indexingAll() {
        if (isIndexing.get()) return false;
        if (waitingThread != null && waitingThread.isAlive()) return false;
        isIndexing.set(true);
        sites.getSites().forEach(site -> {
            SiteParser siteParser = new SiteParser(site);
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
        });
        waitingThread.start();
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (waitingThread == null) return false;
        isIndexing.set(false);
        if (waitingThread.isAlive()){
            System.out.println("Потоки еще не завершились");
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
        System.out.println("domain: " + domain + " path: " + path);

        Optional<SiteModel> siteModelOptional = Repos.siteRepository.findByUrl("http://"+domain);
        if (siteModelOptional.isEmpty()) {
            siteModelOptional = Repos.siteRepository.findByUrl("https://"+domain);
            if (siteModelOptional.isEmpty()) return "Сайта нет в конфигурации";
        }

        SiteModel siteModel = siteModelOptional.get();
        System.out.println("site_model_id = " + siteModel.getId());
        Optional<Page> pageOptional = Repos.pageRepository.findByPathAndSiteModel(path, siteModel);
        if(pageOptional.isEmpty()) return "Страница не существует";
        Page page;
        //page = pageOptional.orElseGet(() -> new PageParser(siteModel, path).processSinglePage(path));
        //LemmaPageParser lemmaPageParser = new LemmaPageParser(page);
//        try {
//            lemmaPageParser.createLemma();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return null;
    }
}
