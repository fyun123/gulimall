package com.whut.gulimall.search.service;

import com.whut.gulimall.search.vo.SearchParam;
import com.whut.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    /**
     *
     * @param searchParam 检索的所有参数
     * @return 返回检索的结果，包括返回页面的所有数据
     */
    SearchResult search(SearchParam searchParam);
}
