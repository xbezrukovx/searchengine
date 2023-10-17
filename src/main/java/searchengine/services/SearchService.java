package searchengine.services;

import searchengine.dto.search.SearchResponse;
import searchengine.model.SiteModel;

import java.io.IOException;

public interface SearchService {
    SearchResponse siteSearch(String query, SiteModel site, int offset, int limit) throws IOException;
    SearchResponse allSiteSearch(String query, int offset, int limit) throws IOException;
    SiteModel findSiteModel(String url);
}
