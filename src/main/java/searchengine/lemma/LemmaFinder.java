package searchengine.lemma;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class LemmaFinder {

    private final String regex = "[^А-яЁё]"; //Only Russian
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph;

    public LemmaFinder() throws IOException{
        luceneMorph = new RussianLuceneMorphology();
    }

    public HashMap<String, Integer> countWords(String content) throws IOException{
        String text = Jsoup.parse(content).text();
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

    private boolean isParticle(String word) throws IOException {
        List<String> wordBaseForms = luceneMorph.getMorphInfo(word);
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
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
