package com.waylau.spring.boot.blog.service;

import com.waylau.spring.boot.blog.domain.User;
import com.waylau.spring.boot.blog.domain.es.EsBlog;
import com.waylau.spring.boot.blog.repository.es.EsBlogRepository;
import com.waylau.spring.boot.blog.vo.TagVO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.search.SearchParseException;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ResultsExtractor;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

/**
 * EsBlog 服务.
 */
@Service
public class EsBlogServiceImpl implements EsBlogService {
	@Autowired
	private EsBlogRepository esBlogRepository;
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;
	@Autowired
	private UserService userService;
	
	private static final Pageable TOP_5_PAGEABLE = new PageRequest(0, 5);
	private static final String EMPTY_KEYWORD = "";

	/**
	 * 删除博客
	 * @param id
	 */
	@Override
	public void removeEsBlog(String id) {
		esBlogRepository.delete(id);
	}

	/**
	 * 更新博客
	 * @param esBlog
	 * @return
	 */
	@Override
	public EsBlog updateEsBlog(EsBlog esBlog) {
		return esBlogRepository.save(esBlog);
	}

	/**
	 * 根据id查博客
	 * @param blogId
	 * @return
	 */
	@Override
	public EsBlog getEsBlogByBlogId(Long blogId) {
		return esBlogRepository.findByBlogId(blogId);
	}

	/**
	 * 显示最新博客
	 * @param keyword
	 * @param pageable
	 * @return
	 * @throws SearchParseException
	 */
	@Override
	public Page<EsBlog> listNewestEsBlogs(String keyword, Pageable pageable) throws SearchParseException {
		Page<EsBlog> pages = null;
		Sort sort = new Sort(Direction.DESC,"createTime"); 
		if (pageable.getSort() == null) {
			pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
		}
 
		pages = esBlogRepository.findDistinctEsBlogByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining(keyword,keyword,keyword,keyword, pageable);
 
		return pages;
	}

	/**
	 * 显示最热
	 * @param keyword
	 * @param pageable
	 * @return
	 * @throws SearchParseException
	 */
	@Override
	public Page<EsBlog> listHotestEsBlogs(String keyword, Pageable pageable) throws SearchParseException{
 
		Sort sort = new Sort(Direction.DESC,"readSize","commentSize","voteSize","createTime"); 
		if (pageable.getSort() == null) {
			pageable = new PageRequest(pageable.getPageNumber(), pageable.getPageSize(), sort);
		}
 
		return esBlogRepository.findDistinctEsBlogByTitleContainingOrSummaryContainingOrContentContainingOrTagsContaining(keyword, keyword, keyword, keyword, pageable);
	}

	@Override
	public Page<EsBlog> listEsBlogs(Pageable pageable) {
		return esBlogRepository.findAll(pageable);
	}
 
 
	/**
	 * 最新前5
	 * @return
	 */
	@Override
	public List<EsBlog> listTop5NewestEsBlogs() {
		Page<EsBlog> page = this.listHotestEsBlogs(EMPTY_KEYWORD, TOP_5_PAGEABLE);
		return page.getContent();
	}
	
	/**
	 * 最热前5
	 * @return
	 */
	@Override
	public List<EsBlog> listTop5HotestEsBlogs() {
		Page<EsBlog> page = this.listHotestEsBlogs(EMPTY_KEYWORD, TOP_5_PAGEABLE);
		return page.getContent();
	}

	@Override
	public List<TagVO> listTop30Tags() {
 
		List<TagVO> list = new ArrayList<>();
		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.QUERY_THEN_FETCH)
				.withIndices("blog").withTypes("blog")
				.addAggregation(terms("tags").field("tags").order(Terms.Order.count(false)).size(30))
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		
		StringTerms modelTerms =  (StringTerms)aggregations.asMap().get("tags"); 
	        
        Iterator<Bucket> modelBucketIt = modelTerms.getBuckets().iterator();
        while (modelBucketIt.hasNext()) {
            Bucket actiontypeBucket = modelBucketIt.next();
 
            list.add(new TagVO(actiontypeBucket.getKey().toString(),
                    actiontypeBucket.getDocCount()));
        }
		return list;
	}
	
	@Override
	public List<User> listTop12Users() {
 
		List<String> usernamelist = new ArrayList<>();
		// given
		SearchQuery searchQuery = new NativeSearchQueryBuilder()
				.withQuery(matchAllQuery())
				.withSearchType(SearchType.QUERY_THEN_FETCH)
				.withIndices("blog").withTypes("blog")
				.addAggregation(terms("users").field("username").order(Terms.Order.count(false)).size(12))
				.build();
		// when
		Aggregations aggregations = elasticsearchTemplate.query(searchQuery, new ResultsExtractor<Aggregations>() {
			@Override
			public Aggregations extract(SearchResponse response) {
				return response.getAggregations();
			}
		});
		
		StringTerms modelTerms =  (StringTerms)aggregations.asMap().get("users"); 
	        
        Iterator<Bucket> modelBucketIt = modelTerms.getBuckets().iterator();
        while (modelBucketIt.hasNext()) {
            Bucket actiontypeBucket = modelBucketIt.next();
            String username = actiontypeBucket.getKey().toString();
            usernamelist.add(username);
        }
        List<User> list = userService.listUsersByUsernames(usernamelist);
        
		return list;
	}
}
