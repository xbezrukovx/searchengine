package searchengine.repos;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class Repos {
    public final SiteRepository siteRepo;
    public final PageRepository pageRepo;
    public final LemmaRepository lemmaRepo;
    public final IndexRepository indexRepo;

    public static SiteRepository siteRepository;
    public static PageRepository pageRepository;
    public static LemmaRepository lemmaRepository;
    public static IndexRepository indexRepository;

    public Repos(SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository, IndexRepository indexRepository){
        siteRepo = siteRepository;
        pageRepo = pageRepository;
        lemmaRepo = lemmaRepository;
        indexRepo = indexRepository;
        Repos.siteRepository = siteRepo;
        Repos.pageRepository = pageRepo;
        Repos.lemmaRepository = lemmaRepo;
        Repos.indexRepository = indexRepo;
    }

}
