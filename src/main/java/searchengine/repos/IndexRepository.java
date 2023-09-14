package searchengine.repos;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.ArrayList;
import java.util.Optional;

public interface IndexRepository extends CrudRepository<Index, Integer> {
    Optional<Index> findByPageAndLemma(Page page, Lemma lemma);
    ArrayList<Index> findByPage(Page page);
}
