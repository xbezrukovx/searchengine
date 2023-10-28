package searchengine.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.models.Lemma;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MorphologyUtil {

    private final String regex = "[^А-яЁё]"; //Only Russian
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private final LuceneMorphology luceneMorph = new RussianLuceneMorphology();
    private final Logger logger = LogManager.getRootLogger();

    public MorphologyUtil()  throws IOException{
    }

    public HashMap<String, Integer> getLemmas(String content) throws IOException {
        if (content == null) return new HashMap<>();
        String text = Jsoup.parse(content).body().text();
        text = text.replaceAll(regex, " ");
        HashMap<String, Integer> lemmas = new HashMap<>();
        for (String word : text.split(" ")) {
            word = word.toLowerCase();
            if (word.isBlank() || isParticle(word)) continue;
            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            word = normalForms.get(0);
            if (!lemmas.containsKey(word)) {
                lemmas.put(word, 0);
            }
            lemmas.put(word, lemmas.get(word) + 1);
        }
        return lemmas;
    }

    public String getSnippet(String content, List<Lemma> keyLemmas) {
        Elements elements = Jsoup.parse(content).select("p");
        List<String> keyLemmasInString = keyLemmas.stream().map(Lemma::getLemma).toList();
        Map<String, Set <String>> map = elements.stream().map(Element::text).distinct()
                .collect(Collectors.toMap(key -> key, value -> {
                    try {
                        return getLemmas(value).keySet();
                    }catch (IOException ex) {
                        logger.error(ex.getMessage());
                        return new HashSet<>();
                    }
                }));
        Map.Entry<String, Set<String>> snippetEntry = map.entrySet().stream().max(Comparator.comparing(k->{
            Set<String> set = k.getValue();
            int count = 0;
            for (String s : keyLemmasInString) {
                if (set.contains(s)) count++;
            }
            return count;
        })).get();
        String finalString = snippetEntry.getKey();
        if (finalString.length() > 250) {
            int endPos = finalString.indexOf(" ", 250);
            if (endPos < 0) endPos = finalString.length();
            finalString = finalString.substring(0, endPos) + "...";
        }
        return makeKeyWordsBold(finalString, keyLemmasInString);
    }

    private String makeKeyWordsBold(String str, List<String> keyWords) {
        String[] strings = str.split(" ");
        for (int i = 0; i < strings.length; i++){
            String word = strings[i].replaceAll(regex,"").toLowerCase();
            try {
                if (word.isBlank() || isParticle(word)) continue;
            } catch (IOException e) {
                continue;
            }
            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            word = normalForms.get(0);
            if (keyWords.contains(word)) strings[i] = "<b>"+strings[i]+"</b>";
        }
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            result.append(string).append(" ");
        }
        return result.toString();
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
            if (baseWord.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
