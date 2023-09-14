package searchengine.parser;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repos.Repos;
import searchengine.services.IndexingServiceImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class PageParser extends RecursiveAction {
    private final List<String> urlsChildren = new ArrayList<>();
    private final String siteUrl;
    private final SiteModel mainPage;
    private final Connection connection;
    private List<Page> pages = new ArrayList<>();

    public PageParser(SiteModel mainPage, String siteUrl){
        this.mainPage = mainPage;
        this.siteUrl = siteUrl;
        try {
            Thread.sleep(130);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        connection = Jsoup.connect(siteUrl)
                .ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
    }
    @Override
    protected void compute() {
        if (!IndexingServiceImpl.isIndexing.get()){
            return;
        }
        List<String> links = getLinks(siteUrl);
        String sitePath = "";
        synchronized (Repos.class) {
            for (String link : links) {
                try {
                    sitePath = new URL(link).getPath();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                //String cropLink = link.substring(countSymbolsURL);
                Optional<Page> pageOptional = Repos.pageRepository.findByPath(sitePath);
                if (pageOptional.isPresent()) continue;
                urlsChildren.add(link);
                Page page = new Page();
                page.setPath(sitePath);
                page.setCode(connection.response().statusCode());
                page.setContent(connection.response().body());
                page.setSiteModel(mainPage);
                mainPage.setStatusTime(LocalDateTime.now());
                page = Repos.pageRepository.save(page);
                pages.add(page);
                Repos.siteRepository.save(mainPage);
            }
        }
        ArrayList<Thread> lemmasTask =  new ArrayList<>();
        pages.forEach(p -> {
            Thread task = new Thread(() -> {
                LemmaPageParser lemmaPageParser = new LemmaPageParser(p);
                try {
                    lemmaPageParser.createLemma();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            task.start();
            lemmasTask.add(task);
        });

        List<RecursiveAction> tasks = new ArrayList<>();
        urlsChildren.forEach(child -> {
            PageParser task = new PageParser(mainPage, child);
            task.fork();
            tasks.add(task);
        });
        tasks.forEach(RecursiveAction::join);
        lemmasTask.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private List<String> getLinks(String url) {
        List<String> links = new ArrayList<>();
        try {
            Document document = connection.get();
            Elements elements = document.body().getElementsByTag("a");
            for (Element element : elements) {
                String link = element.absUrl("href");
                if (isLink(link, url)) {
                    links.add(link);
                }
            }
        } catch (IOException e) {
            System.out.println(e + " - " + url);
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
}
