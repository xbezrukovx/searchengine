package searchengine.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.models.Page;
import searchengine.models.SiteModel;
import searchengine.repos.PageRepository;
import searchengine.repos.SiteRepository;
import searchengine.services.implementation.IndexingServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class PageParser extends RecursiveAction {
    private final String siteUrl;
    private final SiteModel mainPage;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaPageParser lemmaPageParser;
    private final Logger logger = LogManager.getRootLogger();

    public PageParser(
            SiteModel mainPage,
            String siteUrl,
            PageRepository pageRepository,
            SiteRepository siteRepository,
            LemmaPageParser lemmaPageParser
    ) {
        this.mainPage = mainPage;
        this.siteUrl = siteUrl;
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.lemmaPageParser = lemmaPageParser;
    }

    private Page buildPage(Document doc, int statusCode){
        String sitePath = getSitePath(siteUrl);
        Page page = new Page();
        page.setCode(statusCode);
        page.setContent(doc.html());
        page.setSiteModel(mainPage);
        page.setPath(sitePath);
        synchronized (PageRepository.class) {
            Optional<Page> optionalPage = pageRepository.findByPathAndSiteModel(sitePath, mainPage);
            if(optionalPage.isPresent()) return null;
            page = pageRepository.save(page);
        }
        return page;
    }

    private List<String> getUnhandledLinks(List<String> links){
        List<String> paths = new ArrayList<>(links.stream()
                .map(this::getSitePath)
                .toList());
        List<Page> pages = pageRepository.findByPathInAndSiteModel(paths, mainPage);
        paths.removeAll(pages.stream().map(Page::getPath).toList());
        return paths.stream().map(p -> mainPage.getUrl() + p).distinct().toList();
    }

    public Document processThePage() {
        Connection connection = getConnection();
        Document doc;
        try {
            doc = connection.get();
        } catch (IOException e){
            logger.error(e.getMessage());
            return null;
        }
        int statusCode = connection.response().statusCode();
        Page page = buildPage(doc, statusCode);
        if (page == null) return null;
        try {
            lemmaPageParser.createLemma(page);
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        mainPage.setStatusTime(LocalDateTime.now());
        siteRepository.save(mainPage);
        return doc;
    }

    @Override
    protected void compute() {
        if (IndexingServiceImpl.getIsIndexing()) return;  // Try to exit if indexing had been interrupted
        Document doc = processThePage();
        if (doc == null) return;

        // Makes branches
        List<String> childrenUrls = getLinks(siteUrl, doc);
        List<String> unhandledLinks = getUnhandledLinks(childrenUrls);
        List<RecursiveAction> tasks = new ArrayList<>();
        unhandledLinks.forEach(child -> {
            PageParser task = new PageParser(mainPage, child, pageRepository, siteRepository, lemmaPageParser);
            task.fork();
            tasks.add(task);
        });
        tasks.forEach(RecursiveAction::join);
    }

    private List<String> getLinks(String url, Document document) {
        List<String> links = new ArrayList<>();
        Elements elements = document.body().getElementsByTag("a");
        for (Element element : elements) {
            String link = element.absUrl("href");
            if (isLink(link, url)) links.add(link);
        }
        return links;
    }

    private boolean isLink(String link, String owner) {
        try {
            URL url = new URL(link);
            String ownerHost = new URL(owner).getHost();
            String host = url.getHost();
            if (!host.equals(ownerHost) || link.endsWith(".jpg")
                    || link.endsWith(".png") || link.contains("#")
            ) {
                return false;
            }
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    private String getSitePath(String link) {
        String sitePath = "";
        try {
            sitePath = new URL(link).getPath();
        } catch (MalformedURLException e) {
            return sitePath;
        }
        return sitePath;
    }

    private Connection getConnection() {
        try {
            Thread.sleep(130);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        return Jsoup.connect(siteUrl)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
    }
}
