package searchengine.repos;

import org.springframework.data.repository.CrudRepository;
import searchengine.models.SiteModel;

import java.util.Optional;

public interface SiteRepository extends CrudRepository<SiteModel, Integer> {
    Optional<SiteModel> findByUrl(String url);
}
