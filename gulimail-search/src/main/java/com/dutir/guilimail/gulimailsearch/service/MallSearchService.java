package com.dutir.guilimail.gulimailsearch.service;

import com.dutir.guilimail.gulimailsearch.vo.SearchParam;
import com.dutir.guilimail.gulimailsearch.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}
