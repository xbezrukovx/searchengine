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
import java.util.List;
import java.util.Optional;

import static searchengine.repos.Repos.indexRepository;

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
        LemmaFinder lemmaFinder = new LemmaFinder();
        HashMap<String, Integer> lemmas = lemmaFinder.countWords(textHtml);
        deleteInfo();
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

    private void deleteInfo(){
        List<Index> indexList = Repos.indexRepository.findByPage(page);
        indexList.forEach(i -> {
            Lemma lemma = i.getLemma();
            lemma.setFrequency(lemma.getFrequency() - 1);
            Repos.lemmaRepository.save(lemma);
            Repos.indexRepository.delete(i);
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
