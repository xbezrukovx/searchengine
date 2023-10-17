package searchengine.lemma;

import lombok.AllArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Lemma;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class LemmaFinder {

    private final String regex = "[^А-яЁё]"; //Only Russian
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();

    public LemmaFinder() throws IOException{
    }

    public HashMap<String, Integer> getLemmas(String content) throws IOException{
        if (content  == null) return new HashMap<>();
        String text = Jsoup.parse(content).body().text();
        text = text.replaceAll(regex, " ");
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : text.split(" ")){
            word = word.toLowerCase();
            if (word.isBlank() || isParticle(word)) continue;
            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            word = normalForms.get(0);
            if (!lemmas.containsKey(word)){
                lemmas.put(word, 0);
            }
            lemmas.put(word, lemmas.get(word) + 1);
        }
        return lemmas;
    }

    public String getSnippet(String content, List<Lemma> keyLemmas){
        @AllArgsConstructor
        class KeyElem {
            private Element element;
            private Integer countKeyWords;
        }
        Elements elements = Jsoup.parse(content).select("p");
        String[] snippetWords = elements.stream().map(e -> {
            try {
                int count = getLemmas(e.html()).size();
                return new KeyElem(e, count);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }).max(Comparator.comparing(k -> k.countKeyWords)).get().element.text().toLowerCase().split(" ");
        List<String> snippetLemmas = new ArrayList<>();
        for (String word : snippetWords){
            word = word.toLowerCase();
            try {
                if (word.isBlank() || isParticle(word)) {
                    snippetLemmas.add(" ");
                    continue;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            List<String> normalForms;
            try {
                normalForms = luceneMorph.getNormalForms(word);
            } catch (WrongCharaterException e) {
                normalForms = new ArrayList<>();
            }
            if (normalForms.isEmpty()) {
                snippetLemmas.add(" ");
                continue;
            }
            word = normalForms.get(0);
            snippetLemmas.add(word);
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < snippetLemmas.size(); i++){
            if (keyLemmas.stream().map(Lemma::getLemma).toList().contains(snippetLemmas.get(i).toLowerCase())){
                snippetWords[i] = "<b>"+snippetWords[i]+"</b>";
            }
            stringBuilder.append(snippetWords[i]).append(" ");
        }

        return stringBuilder.toString();
    }

    private boolean isParticle(String word) throws IOException {
        try {
            List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
            return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
        } catch (WrongCharaterException e) {
            return false;
        }
    }

    private boolean hasParticleProperty(String baseWord) {
        for (String property : particlesNames) {
            if (baseWord.toUpperCase().contains(property)){
                return true;
            }
        }
        return false;
    }
}
