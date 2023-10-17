package searchengine.parser;

import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repos.IndexRepository;
import searchengine.repos.LemmaRepository;
import searchengine.repos.Repos;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;


public class LemmaPageParser{
    private Page page;
    private final SiteModel siteModel;
    private final String textHtml;

    public LemmaPageParser(Page page){
        this.page = page;
        siteModel = page.getSiteModel();
        textHtml = page.getContent();
    }


    public void createLemma() throws IOException {
        char firstCode = String.valueOf(page.getCode()).charAt(0);
        if(firstCode == '4' || firstCode == '5') return;
        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmas = lemmaFinder.getLemmas(textHtml);
        deleteInfo();
        lemmas.forEach((l, c) -> {
            Lemma lemmaModel;
            synchronized (LemmaRepository.class) {
                Optional<Lemma> optionalLemma = Repos.lemmaRepository.findBySiteModelAndLemma(siteModel, l);
                if (optionalLemma.isPresent()) {
                    lemmaModel = optionalLemma.get();
                    lemmaModel.setFrequency(lemmaModel.getFrequency() + 1);
                } else {
                    lemmaModel = new Lemma();
                    lemmaModel.setLemma(l);
                    lemmaModel.setSiteModel(siteModel);
                    lemmaModel.setFrequency(1);
                }
                Repos.lemmaRepository.save(lemmaModel);
                createIndex(lemmaModel, c);
            }
        });
    }

    private synchronized void deleteInfo(){
        List<Index> indexList = Repos.indexRepository.findByPage(page);
        indexList.forEach(i -> {
            Lemma lemma = i.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);
            Repos.lemmaRepository.save(lemma);
            Repos.indexRepository.delete(i);
        });
    }

    private void createIndex(Lemma lemma, float count){
        synchronized (IndexRepository.class) {
            Optional<Index> optionalIndex = Repos.indexRepository.findByPageAndLemma(page, lemma);
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
            Repos.indexRepository.save(index);
        }
    }
}
