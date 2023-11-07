package searchengine.utils;

import org.springframework.stereotype.Component;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;
import searchengine.models.SiteModel;
import searchengine.repos.IndexRepository;
import searchengine.repos.LemmaRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Component
public class LemmaPageParser{
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final MorphologyUtil morphologyUtil;

    public LemmaPageParser(
            LemmaRepository lemmaRepository,
            IndexRepository indexRepository,
            MorphologyUtil morphologyUtil
    ){
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.morphologyUtil = morphologyUtil;
    }


    public void createLemma(Page page) throws IOException {
        char firstCode = String.valueOf(page.getCode()).charAt(0);
        if(firstCode == '4' || firstCode == '5') {
            return;
        }
        String textHtml = page.getContent();
        SiteModel siteModel = page.getSiteModel();
        HashMap<String, Integer> lemmas = morphologyUtil.getLemmas(textHtml);
        deleteInfo(page);
        lemmas.forEach((l, c) -> {
            Lemma lemmaModel;
            synchronized (LemmaRepository.class) {
                Optional<Lemma> optionalLemma = lemmaRepository.findBySiteModelAndLemma(siteModel, l);
                if (optionalLemma.isPresent()) {
                    lemmaModel = optionalLemma.get();
                    lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
                } else {
                    lemmaModel = new Lemma();
                    lemmaModel.setLemma(l);
                    lemmaModel.setSiteModel(siteModel);
                    lemmaModel.setFrequency(1);
                }
                lemmaRepository.save(lemmaModel);
                createIndex(lemmaModel, c, page);
            }
        });
    }

    private synchronized void deleteInfo(Page page){
        List<Index> indexList = indexRepository.findByPage(page);
        indexList.forEach(i -> {
            Lemma lemma = i.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);
            lemmaRepository.save(lemma);
            indexRepository.delete(i);
        });
    }

    private void createIndex(Lemma lemma, float count, Page page){
        synchronized (IndexRepository.class) {
            Optional<Index> optionalIndex = indexRepository.findByPageAndLemma(page, lemma);
            Index index;
            if (optionalIndex.isPresent()) {
                index = optionalIndex.get();
                index.setRank(index.getRank() + count);
            } else {
                index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(count);
            }
            indexRepository.save(index);
        }
    }
}
