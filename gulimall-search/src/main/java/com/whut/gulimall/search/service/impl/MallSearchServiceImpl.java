package com.whut.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.whut.common.to.es.SkuEsModel;
import com.whut.gulimall.search.config.GulimallElasticSearchConfig;
import com.whut.gulimall.search.constant.EsConstant;
import com.whut.gulimall.search.service.MallSearchService;
import com.whut.gulimall.search.vo.SearchParam;
import com.whut.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public SearchResult search(SearchParam searchParam) {

        //构建动态SDL语句
        SearchResult result = null;

        // 准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            // 执行检索请求
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            // 分析响应数据，封装成result
            result = buildSeaarchResult(response, searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 准备检索请求
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // 指定DSL，检索条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        /**
         * 模糊匹配，过滤（分类，品牌，属性，库存，价格区间）
         */
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1 模糊查询
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2 过滤
        // 1.2.1 分类
        if (param.getCatalog3Id() != null) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2.2 品牌
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2.3 属性
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {

            for (String attrStr : param.getAttrs()) {
                // attrs=1_5寸：8寸&attrs=2_8G:16G
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrs=1_5寸：8寸
                String[] s = attrStr.split("_");
                String attrId = s[0]; // 属性id
                String[] attrValues = s[1].split(":"); //属性值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一个属性都必须生成一个嵌入式查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }

        // 1.2.4 库存
        if (param.getHasStock() != null){
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }

        // 1.2.5 价格区间
        /**
         *  {
         *      "range": {
         *          "skuPrice": {
         *          "gte": 0,
         *          "lte": 3000
         *         }
         *      }
         * }
         */
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (param.getSkuPrice().startsWith("_")) {
                rangeQuery.lte(s[1]);
            } else if (param.getSkuPrice().endsWith("_")) {
                rangeQuery.gte(s[0]);
            } else {
                rangeQuery.gte(s[0]).lte(s[1]);
            }
            boolQuery.filter(rangeQuery);
        }
        // 查询条件封装
        sourceBuilder.query(boolQuery);

        /**
         * 排序，分页，高亮
         */
        // 2.1 排序
        if (!StringUtils.isEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            sourceBuilder.sort(s[0], SortOrder.fromString(s[1]));
        }
        // 2.2 分页 pageSize:5
        // pageNum:1 from:0 size:5 [0,1,2,3,4]
        //pageNum:2 from 5
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }
        /**
         * 聚合分析
         */

        // 3.1 品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);
        // 3.2 分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        // 分类子聚合
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);
        // 3.3 属性聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合出所有的attr_id
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        // attr_id对应的attrName
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // attr_id对应的attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        sourceBuilder.aggregation(attr_agg);

        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
        return searchRequest;
    }

    /**
     * 分析响应数据，封装成result
     *
     * @param response
     * @return
     */
    private SearchResult buildSeaarchResult(SearchResponse response, SearchParam param) throws UnsupportedEncodingException {
        SearchResult result = new SearchResult();
        // 1. 封装所有查询到的商品
        SearchHits hits = response.getHits();
        List<SkuEsModel> skuEsModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                // 高亮
                if (!StringUtils.isEmpty(param.getKeyword())){
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    skuEsModel.setSkuTitle(string);
                }
                skuEsModels.add(skuEsModel);
            }
        }
        result.setProducts(skuEsModels);
        // 2. 当前商品所涉及到的所有分类信息
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        ParsedLongTerms catalogAgg = response.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 得到分类id
            String catalogId = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(catalogId));
            // 得到分类名字
            ParsedStringTerms catalog_name_agg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalog_name_agg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);
        // 3. 当前商品所涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = response.getAggregations().get("brand_agg");
        for (Terms.Bucket attrAggBucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 获取品牌id
            long brandId = attrAggBucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            // 获取品牌名字
            ParsedStringTerms brand_name_agg = attrAggBucket.getAggregations().get("brand_name_agg");
            String brandName = brand_name_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);
            // 获取品牌的图片
            ParsedStringTerms brand_img_agg = attrAggBucket.getAggregations().get("brand_img_agg");
            String brandImg = brand_img_agg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
        // 4. 当前商品所涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attrAgg = response.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            // 属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            // 属性值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValue = attrValueAgg.getBuckets().stream().map(item -> item.getKeyAsString()).collect(Collectors.toList());
            attrVo.setAttrValue(attrValue);

            attrVos.add(attrVo);
        }
        result.setAttrs(attrVos);
        // 5. 分页信息
        // 5.1 页码
        result.setPageNum(param.getPageNum());
        // 5.2 总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 5.3 总页码
        int l = (int) total % EsConstant.PRODUCT_PAGESIZE;
        int totalPages = l == 0 ? (int) total / EsConstant.PRODUCT_PAGESIZE :  (int) total / EsConstant.PRODUCT_PAGESIZE + 1;
        result.setTotalPages(totalPages);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++){
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 6. 构建面包屑导航功能
        if (param.getAttrs() != null && param.getAttrs().size() > 0){
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // attrs=2_5G:4G
                String[] s = attr.split("_");
                List<SearchResult.AttrVo> collect = attrVos.stream().filter(attrVo -> {
                    if (attrVo.getAttrId() == Long.parseLong(s[0])) {
                        result.getAttrIds().add(Long.parseLong(s[0]));
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
                navVo.setNavValue(s[1]);
                navVo.setNavName(collect.get(0).getAttrName());
                // 取消面包屑的时候，跳转到哪个地址
                try {
                    String link = replaceQueryString("http://search.gulimall.com/list.html",param, attr,"attrs");
                    navVo.setLink(link);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVos);
        }

        // 品牌面包屑
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            List<Long> brandId = param.getBrandId();
            List<SearchResult.BrandVo> brands = brandVos.stream().filter(brandVo -> {
                if (brandId.contains(brandVo.getBrandId())) {
                    return true;
                }
                return false;
            }).collect(Collectors.toList());
            StringBuffer buffer = new StringBuffer();
            String link = null;
            if (brands != null && brands.size() > 0) {
                for (SearchResult.BrandVo brand : brands) {
                    buffer.append(brand.getBrandName()+";");
                    link = replaceQueryString("http://search.gulimall.com/list.html",param, brand.getBrandId() + "", "brandId");
                }
                navVo.setNavValue(buffer.toString());
                navVo.setLink(link);
            }
            navs.add(navVo);
        }

        return result;
    }

    private String replaceQueryString(String href,SearchParam param, String value, String key) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(value, "UTF-8");
        String link = href + "?" + param.getQueryString();
        if (link.contains("?"+key+"=") && param.getQueryString().contains("&")) {
            // 第一个请求参数
            link = link.replace(key+"="+encode+"&","");
        }else if (link.contains("?"+key+"=") && !param.getQueryString().contains("&")) {
            // 唯一请求参数
            link = link.replace("?"+key+"="+encode,"");
        } else {
            link = link.replace("&"+key+"="+encode,"");
        }
        link.replace("+","%20");//浏览器对空格的编码和java不一样
        return link;
    }


}
