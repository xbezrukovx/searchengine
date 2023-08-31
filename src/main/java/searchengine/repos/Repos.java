package searchengine.repos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class Repos {
    public final SiteRepository siteRepo;
    public final PageRepository pageRepo;

    public static SiteRepository siteRepository;
    public static PageRepository pageRepository;

    public Repos(SiteRepository siteRepository, PageRepository pageRepository){
        siteRepo = siteRepository;
        pageRepo = pageRepository;
        Repos.siteRepository = siteRepo;
        Repos.pageRepository = pageRepo;
    }

}
