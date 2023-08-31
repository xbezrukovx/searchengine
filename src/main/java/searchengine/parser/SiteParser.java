package searchengine.parser;

import lombok.RequiredArgsConstructor;
import searchengine.config.Site;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatusType;
import searchengine.repos.Repos;
import searchengine.services.IndexingServiceImpl;

import java.time.LocalDateTime;
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
            Repos.siteRepository.delete(s);
            List<Page> pages = Repos.pageRepository.findBySiteModel(s);
            pages.forEach(Repos.pageRepository::delete);
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
        synchronized (Repos.class) {
            Repos.siteRepository.save(mainPage);
        }
    }
}
