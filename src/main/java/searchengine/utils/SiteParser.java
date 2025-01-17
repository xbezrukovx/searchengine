package searchengine.utils;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import searchengine.config.Site;
import searchengine.models.*;
import searchengine.repos.IndexRepository;
import searchengine.repos.LemmaRepository;
import searchengine.repos.PageRepository;
import searchengine.repos.SiteRepository;
import searchengine.services.implementation.IndexingServiceImpl;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class SiteParser extends Thread {
    private final Site site;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final LemmaPageParser lemmaPageParser;
    private final Logger logger = LogManager.getRootLogger();

    private void removeData(Site site) {
        Optional<SiteModel> sitePageOptional = siteRepository.findByUrl(site.getUrl());
        sitePageOptional.ifPresent(s -> {
            List<Page> pages = pageRepository.findBySiteModel(s);
            pages.forEach(page -> {
                List<Index> indexList = indexRepository.findByPage(page);
                indexList.forEach(indexRepository::delete);
            });
            List<Lemma> lemmaList = lemmaRepository.findBySiteModel(s);
            lemmaList.forEach(lemmaRepository::delete);
            siteRepository.delete(s);
        });
        logger.info(MessageFormat.format("Information about {0} was removed.",site.getName()));
    }

    @Override
    public void run() {
        removeData(site);
        SiteModel mainPage = new SiteModel(
                SiteStatusType.INDEXING,
                LocalDateTime.now(),
                null,
                site.getUrl(),
                site.getName()
        );
        siteRepository.save(mainPage);
        logger.info(MessageFormat.format("{0} is processing.", mainPage.getName()));
        PageParser newTask = new PageParser(mainPage, mainPage.getUrl(), pageRepository, siteRepository, lemmaPageParser);
        newTask.compute();
        newTask.join();
        if (IndexingServiceImpl.getIsIndexing()) {
            mainPage.setStatus(SiteStatusType.INDEXED);
        } else {
            mainPage.setStatus(SiteStatusType.FAILED);
            mainPage.setLastError("Indexing was interrupted by user");
            logger.info("Indexing was interrupted by user.");
        }
        mainPage.setStatusTime(LocalDateTime.now());
        siteRepository.save(mainPage);
        logger.info(MessageFormat.format("{0} has been indexed.", mainPage.getName()));
    }
}
