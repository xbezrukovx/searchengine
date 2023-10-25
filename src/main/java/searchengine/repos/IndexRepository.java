package searchengine.repos;

import org.springframework.data.repository.CrudRepository;
import searchengine.models.Index;
import searchengine.models.Lemma;
import searchengine.models.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface IndexRepository extends CrudRepository<Index, Integer> {
    Optional<Index> findByPageAndLemma(Page page, Lemma lemma);
    ArrayList<Index> findByPage(Page page);
    List<Index> findByLemma(Lemma lemma);
    List<Index> findByPageInAndLemma(List<Page> pages, Lemma lemma);
}
