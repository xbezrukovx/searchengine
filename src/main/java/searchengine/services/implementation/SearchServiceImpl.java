package searchengine.services.implementation;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.dto.BadResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.SiteResponse;
import searchengine.utils.MorphologyUtil;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.SiteModel;
import searchengine.repos.IndexRepository;
import searchengine.repos.LemmaRepository;
import searchengine.repos.PageRepository;
import searchengine.repos.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    public SearchServiceImpl(
            LemmaRepository lemmaRepository,
            PageRepository pageRepository,
            IndexRepository indexRepository,
            SiteRepository siteRepository
    ){
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.indexRepository = indexRepository;
        this.siteRepository = siteRepository;
    }

    @Override
    public SearchResponse siteSearch(String query, String site, int offset, int limit) throws IOException {
        SearchResponse searchResponse = new SearchResponse();
        SiteModel siteModel = null;
        if (site != null && site.isBlank()) {
            siteModel = findSiteModel(site);
            if (siteModel == null) {
                searchResponse.setError("No such site exists.");
                return searchResponse;
            }
        }
        List<String> queryLemmas = getLemmas(query);
        List<Lemma> lemmaList = lemmaRepository.findByLemmaInOrderByFrequencyAsc(queryLemmas);
        if (lemmaList.size() == 0) {
            searchResponse.setError("Search index doesn't exists.");
            return searchResponse;
        }
        List<SiteModel> siteModels = new ArrayList<>();
        if (siteModel == null) {
            siteModels = lemmaList.stream().map(Lemma::getSiteModel).distinct().toList();
        } else {
            siteModels.add(siteModel);
        }
        List<List<Index>> indexesList = new ArrayList<>();
        for (SiteModel s : siteModels) {
            List<Lemma> lemmas = lemmaList.stream().filter(l -> l.getSiteModel().equals(s)).toList();
            if (lemmas.size() == 0) continue;
            int countPages = pageRepository.findCountPages(s.getId());
            List<Index> indexes = new ArrayList<>();
            for (int i = 0; i < lemmaList.size(); i++) {
                Lemma lemma = lemmaList.get(i);
                if ((float) lemma.getFrequency() / countPages > 0.8f) continue;
                if (i==0) {
                    indexes =  indexRepository.findByLemma(lemmas.get(0));
                }
                List<Page> pages = indexes.stream().map(Index::getPage).toList();
                indexes = indexRepository.findByPageInAndLemma(pages, lemma);
            }
            indexesList.add(indexes);
        }

        List<Index> indexes = indexesList.stream().flatMap(Collection::stream).toList();
        HashMap<Page, Float> pageRelevanceMap = calculateRelevance(indexes);
        HashMap<Page, Float> pageRelRelevanceMap = calculateRelRelevance(pageRelevanceMap);

        List<SiteResponse> siteResponses = new ArrayList<>();
        int count = 0;
        List<Page> pages = pageRelRelevanceMap.keySet().stream().toList();
        int last =  Math.min(pages.size(), limit+offset);
        offset = Math.min(pages.size(), offset);
        for (int i = offset; i < last; i++) {
            Page p = pages.get(i);
            count++;
            MorphologyUtil morphologyUtil = new MorphologyUtil();
            String snippet = morphologyUtil.getSnippet(p.getContent(), lemmaList);
            SiteResponse siteResponse = new SiteResponse();
            siteResponse.setSite(p.getSiteModel().getUrl());
            siteResponse.setSiteName(p.getSiteModel().getName());
            siteResponse.setSnippet(snippet);
            siteResponse.setUri(p.getPath());
            siteResponse.setRelevance(pageRelRelevanceMap.get(p));
            siteResponse.setTitle(getPageTitle(p));
            siteResponses.add(siteResponse);
        }
        searchResponse.setCount(pages.size());
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
        MorphologyUtil morphologyUtil = new MorphologyUtil();
        Set<String> queryWordsSet = morphologyUtil.getLemmas(text).keySet();
        return new ArrayList<>(queryWordsSet);
    }

    private SiteModel findSiteModel(String url) {
        String domain;
        try {
            domain = new URL(url).getHost();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        Optional<SiteModel> siteModelOptional = siteRepository.findByUrl("http://" + domain);
        if (siteModelOptional.isEmpty()) {
            siteModelOptional = siteRepository.findByUrl("https://" + domain);
            if (siteModelOptional.isEmpty()) return null;
        }
        return siteModelOptional.get();
    }
}