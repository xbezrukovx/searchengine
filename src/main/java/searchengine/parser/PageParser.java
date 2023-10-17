package searchengine.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repos.PageRepository;
import searchengine.repos.Repos;
import searchengine.services.IndexingServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PageParser extends RecursiveAction {
    private final String siteUrl;
    private final SiteModel mainPage;

    public PageParser(SiteModel mainPage, String siteUrl) {
        this.mainPage = mainPage;
        this.siteUrl = siteUrl;
    }

    private Page buildPage(Document doc, int statusCode){
        String sitePath = getSitePath(siteUrl);
        Page page = new Page();
        page.setCode(statusCode);
        page.setContent(doc.html());
        page.setSiteModel(mainPage);
        page.setPath(sitePath);
        synchronized (PageRepository.class) {
            Optional<Page> optionalPage = Repos.pageRepository.findByPathAndSiteModel(sitePath, mainPage);
            if(optionalPage.isPresent()) return null;
            page = Repos.pageRepository.save(page);
        }
        return page;
    }

    private List<String> getUnhandledLinks(List<String> links){
        List<String> paths = new ArrayList<>(links.stream()
                .map(this::getSitePath)
                .toList());
        List<Page> pages = Repos.pageRepository.findByPathInAndSiteModel(paths, mainPage);
        paths.removeAll(pages.stream().map(Page::getPath).toList());
        return paths.stream().map(p -> mainPage.getUrl() + p).distinct().toList();
    }

    @Override
    protected void compute() {
        if (!IndexingServiceImpl.isIndexing.get()) return;  // Try to exit if indexing had been interrupted
        Connection connection = getConnection();
        Document doc = null;
        try {
            doc = connection.get();
        } catch (IOException e){
            return;
        }
        int statusCode = connection.response().statusCode();
        Page page = buildPage(doc, statusCode);
        if (page == null) return;
        List<String> childrenUrls = getLinks(siteUrl, doc);
        List<String> unhandledLinks;

        unhandledLinks = getUnhandledLinks(childrenUrls);
        mainPage.setStatusTime(LocalDateTime.now());
        Repos.siteRepository.save(mainPage);

        LemmaPageParser lemmaPageParser = new LemmaPageParser(page);
        try {
            lemmaPageParser.createLemma();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Making branches
        List<RecursiveAction> tasks = new ArrayList<>();
        unhandledLinks.forEach(child -> {
            PageParser task = new PageParser(mainPage, child);
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
            throw new RuntimeException(e);
        }
        return sitePath;
    }

    private Connection getConnection() {
        try {
            Thread.sleep(130);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return Jsoup.connect(siteUrl)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
    }
}
