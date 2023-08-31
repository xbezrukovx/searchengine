package searchengine.repos;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.Optional;

@Repository
public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
    Optional<SiteModel> findByUrl(String url);
}
