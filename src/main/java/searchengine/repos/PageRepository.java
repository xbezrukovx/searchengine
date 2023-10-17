package searchengine.repos;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.SiteModel;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer> {
    List<Page> findBySiteModel(SiteModel siteModel);
    Optional<Page> findByPathAndSiteModel(String path, SiteModel siteModel);
    List<Page> findByPathInAndSiteModel(List<String> paths, SiteModel siteModel);
    @Query(value = "select count(1) from pages where site_model_id = :siteModelId", nativeQuery = true)
    int findCountPages(int siteModelId);
}
