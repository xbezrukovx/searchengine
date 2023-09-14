package searchengine.repos;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Lemma;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;

public interface LemmaRepository extends CrudRepository<Lemma, Integer> {
    Optional<Lemma> findBySiteModelAndLemma(SiteModel siteModel, String lemma);
    List<Lemma> findBySiteModel(SiteModel siteModel);
}
