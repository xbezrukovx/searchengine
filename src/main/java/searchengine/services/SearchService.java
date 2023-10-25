package searchengine.services;

import searchengine.dto.search.SearchResponse;
import searchengine.models.SiteModel;

import java.io.IOException;

public interface SearchService {
    SearchResponse siteSearch(String query, String site, int offset, int limit) throws IOException;
}
