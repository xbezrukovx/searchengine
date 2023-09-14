package searchengine.parser;

import lombok.AllArgsConstructor;
import searchengine.lemma.LemmaFinder;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.SiteModel;
import searchengine.repos.LemmaRepository;
import searchengine.repos.Repos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static searchengine.repos.Repos.indexRepository;

@AllArgsConstructor
public class LemmaPageParser{
    Page page;

    public void createLemma() throws IOException {
        SiteModel siteModel = page.getSiteModel();
        String textHtml = page.getContent();
        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmas = lemmaFinder.countWords(page.getContent());
        lemmas.forEach((l, c) -> {
            Lemma lemmaModel;
            synchronized (Repos.class) {
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

    private void createIndex(Lemma lemma, float count){
        Optional<Index> optionalIndex = Repos.indexRepository.findByPageAndLemma(page, lemma);
        Index index;
        if (optionalIndex.isPresent()) {
            index = optionalIndex.get();
            index.setRank(index.getRank()+count);
        } else {
            index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(count);
        }
        Repos.indexRepository.save(index);
    }
}
