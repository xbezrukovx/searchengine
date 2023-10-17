package searchengine.services;

import lombok.NoArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SiteResponse;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repos.Repos;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@NoArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final static int SNIPPET_OFFSET = 10;
    @Override
    public SearchResponse siteSearch(String query, SiteModel site, int offset, int limit) throws IOException {
        SearchResponse searchResponse = new SearchResponse();
        List<String> queryLemmas = getLemmas(query);
        List<Lemma> lemmaList = Repos.lemmaRepository.findBySiteModelAndLemmaInOrderByFrequencyAsc(site, queryLemmas);
        if (lemmaList.size() == 0) return searchResponse;
        int countPages = Repos.pageRepository.findCountPages(lemmaList.get(0).getSiteModel().getId());
        List<Index> indexes = new ArrayList<>();
        for (int i = 0; i < lemmaList.size(); i++) {
            Lemma lemma = lemmaList.get(i);
            if ((float) lemma.getFrequency() / countPages > 0.8f) continue;
            if (i == 0) {
                indexes = Repos.indexRepository.findByLemma(lemma);
            }
            List<Page> pages = indexes.stream().map(Index::getPage).toList();
            indexes = Repos.indexRepository.findByPageInAndLemma(pages, lemma);
        }
        HashMap<Page, Float> pageRelevanceMap = calculateRelevance(indexes);
        HashMap<Page, Float> pageRelRelevanceMap = calculateRelRelevance(pageRelevanceMap);

        List<SiteResponse> siteResponses = new ArrayList<>();
        int count = 0;
        for (Page p : pageRelRelevanceMap.keySet()) {
            count++;
            LemmaFinder lemmaFinder = new LemmaFinder();
            String snippet = lemmaFinder.getSnippet(p.getContent(), lemmaList);
            SiteResponse siteResponse = new SiteResponse();
            siteResponse.setSite(p.getSiteModel().getUrl());
            siteResponse.setSiteName(p.getSiteModel().getName());
            siteResponse.setSnippet(snippet);
            siteResponse.setUri(p.getPath());
            siteResponse.setRelevance(pageRelRelevanceMap.get(p));
            siteResponse.setTitle(getPageTitle(p));
            siteResponses.add(siteResponse);
        }
        searchResponse.setCount(count);
        searchResponse.setData(siteResponses);
        searchResponse.setResult(true);
        return searchResponse;
    }

    @Override
    public SearchResponse allSiteSearch(String query, int offset, int limit) throws IOException {
        SearchResponse searchResponse = new SearchResponse();
        List<String> queryLemmas = getLemmas(query);
        List<Lemma> lemmaList = Repos.lemmaRepository.findByLemmaInOrderByFrequencyAsc(queryLemmas);
        if (lemmaList.size() == 0) return searchResponse;
        List<SiteModel> siteModels = lemmaList.stream().map(Lemma::getSiteModel).distinct().toList();
        HashMap<SiteModel, List<Lemma>> siteLemmas = new HashMap<>();
        List<List<Index>> indexesList = new ArrayList<>();
        for (SiteModel s : siteModels) {
            List<Lemma> lemmas = lemmaList.stream().filter(l -> l.getSiteModel().equals(s)).toList();
            if (lemmas.size() == 0) continue;
            int countPages = Repos.pageRepository.findCountPages(s.getId());
            List<Index> indexes = new ArrayList<>();
            for (int i = 0; i < lemmaList.size(); i++) {
                Lemma lemma = lemmaList.get(i);
                if ((float) lemma.getFrequency() / countPages > 0.8f) continue;
                if (i==0) {
                    indexes =  Repos.indexRepository.findByLemma(lemmas.get(0));
                }
                List<Page> pages = indexes.stream().map(Index::getPage).toList();
                indexes = Repos.indexRepository.findByPageInAndLemma(pages, lemma);
            }
            indexesList.add(indexes);
        }

        List<Index> indexes = indexesList.stream().flatMap(Collection::stream).toList();
        HashMap<Page, Float> pageRelevanceMap = calculateRelevance(indexes);
        HashMap<Page, Float> pageRelRelevanceMap = calculateRelRelevance(pageRelevanceMap);

        List<SiteResponse> siteResponses = new ArrayList<>();
        int count = 0;
        for (Page p : pageRelRelevanceMap.keySet()) {
            count++;
            LemmaFinder lemmaFinder = new LemmaFinder();
            String snippet = lemmaFinder.getSnippet(p.getContent(), lemmaList);
            SiteResponse siteResponse = new SiteResponse();
            siteResponse.setSite(p.getSiteModel().getUrl());
            siteResponse.setSiteName(p.getSiteModel().getName());
            siteResponse.setSnippet(snippet);
            siteResponse.setUri(p.getPath());
            siteResponse.setRelevance(pageRelRelevanceMap.get(p));
            siteResponse.setTitle(getPageTitle(p));
            siteResponses.add(siteResponse);
        }
        searchResponse.setCount(count);
        searchResponse.setData(siteResponses);
        searchResponse.setResult(true);
        return searchResponse;
    }

    private String getPageTitle(Page page) {
        return Jsoup.parse(page.getContent()).title();
    }

    private HashMap<Page, Float> calculateRelevance(List<Index> indexList) {
        List<Page> pages = indexList.stream().map(Index::getPage).distinct().toList();
        HashMap<Page, Float> relevanceMap = new HashMap<>();
        for (Page p : pages) {
            Optional<Float> relevanceOptional = indexList.stream()
                    .filter(i -> i.getPage().equals(p))
                    .map(Index::getRank)
                    .reduce(Float::sum);
            if (relevanceOptional.isEmpty()) continue;
            Float relevance = relevanceOptional.get();
            relevanceMap.put(p, relevance);
        }
        return relevanceMap.entrySet().stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, HashMap::new));
    }

    private HashMap<Page, Float> calculateRelRelevance(HashMap<Page, Float> relevanceMap) {
        HashMap<Page, Float> relRelevanceMap = new HashMap<>();
        if (relevanceMap.size() == 0) return relRelevanceMap;
        Float absRelevantMax = Collections.max(relevanceMap.values());
        relevanceMap.forEach((p, r) -> {
            Float absRelevant = r / absRelevantMax;
            relRelevanceMap.put(p, absRelevant);
        });
        return relRelevanceMap;
    }

    private List<String> getLemmas(String text) throws IOException {
        LemmaFinder lemmaFinder = new LemmaFinder();
        Set<String> queryWordsSet = lemmaFinder.getLemmas(text).keySet();
        return new ArrayList<>(queryWordsSet);
    }

    @Override
    public SiteModel findSiteModel(String url) {
        String domain;
        try {
            domain = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Optional<SiteModel> siteModelOptional = Repos.siteRepository.findByUrl("http://" + domain);
        if (siteModelOptional.isEmpty()) {
            siteModelOptional = Repos.siteRepository.findByUrl("https://" + domain);
            if (siteModelOptional.isEmpty()) return null;
        }
        return siteModelOptional.get();
    }
}
