package searchengine.parser;

import lombok.RequiredArgsConstructor;
import searchengine.config.Site;
import searchengine.model.*;
import searchengine.repos.LemmaRepository;
import searchengine.repos.Repos;
import searchengine.services.IndexingServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

import static searchengine.repos.Repos.pageRepository;

@RequiredArgsConstructor
public class SiteParser extends Thread{
    private final Site site;
    @Override
    public void run() {
        Optional<SiteModel> sitePageOptional = Repos.siteRepository.findByUrl(site.getUrl());
        sitePageOptional.ifPresent(s -> {
            List<Page> pages = Repos.pageRepository.findBySiteModel(s);
            pages.forEach(page -> {
                List<Index> indexList = Repos.indexRepository.findByPage(page);
                indexList.forEach(Repos.indexRepository::delete);
            });
            List<Lemma> lemmaList = Repos.lemmaRepository.findBySiteModel(s);
            lemmaList.forEach(Repos.lemmaRepository::delete);
            Repos.siteRepository.delete(s);
        });
        SiteModel mainPage = new SiteModel(
                SiteStatusType.INDEXING,
                LocalDateTime.now(),
                null,
                site.getUrl(),
                site.getName()
        );
        synchronized (Repos.class){
            Repos.siteRepository.save(mainPage);
        }
        PageParser newTask = new PageParser(mainPage, mainPage.getUrl());
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(newTask);
        if (IndexingServiceImpl.isIndexing.get()) {
            mainPage.setStatus(SiteStatusType.INDEXED);
        } else {
            mainPage.setStatus(SiteStatusType.FAILED);
            mainPage.setLastError("Indexing was interrupted by user");
        }
        mainPage.setStatusTime(LocalDateTime.now());
        synchronized (Repos.class) {
            Repos.siteRepository.save(mainPage);
        }
        System.out.println("Сайт " + mainPage.getName() + " проиндексирован." + LocalDateTime.now());
    }
}
