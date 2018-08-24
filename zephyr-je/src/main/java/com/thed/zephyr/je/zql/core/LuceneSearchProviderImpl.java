package com.thed.zephyr.je.zql.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import com.atlassian.jira.user.ApplicationUser;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.instrumentation.operations.OpTimer;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.instrumentation.Instrumentation;
import com.atlassian.jira.instrumentation.InstrumentationName;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.parameters.lucene.CachedWrappedFilterCache;
import com.atlassian.jira.issue.search.parameters.lucene.PermissionsFilterGenerator;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryCreationContextImpl;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;
import com.atlassian.jira.security.RequestCacheKeys;
import com.atlassian.query.Query;
import com.atlassian.jira.web.filters.ThreadLocalQueryProfiler;
import com.atlassian.query.order.SearchSort;
import com.atlassian.util.profiling.UtilTimerStack;
import com.thed.zephyr.je.index.ScheduleIndexer;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.je.zql.permission.ZephyrPermissionsFilterGenerator;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

public class LuceneSearchProviderImpl implements LuceneSearchProvider
{
    private static final Logger log = Logger.getLogger(LuceneSearchProviderImpl.class);
    private static final Logger slowLog = Logger.getLogger(LuceneSearchProviderImpl.class.getName() + "_SLOW");
    /*@TODO: Shall we read it from JIRA "jira.search.view.default.max"?*/
	private static final int MAX_RECORDS_ALLOWED = 1000;
    private final LuceneQueryBuilder luceneQueryBuilder;
    private final ScheduleIndexer scheduleIndexer;
    private final PermissionsFilterGenerator permissionsFilterGenerator;
    private final ZephyrPermissionsFilterGenerator zephyrPermissionsFilterGenerator;
    private final SearchHandlerManager searchHandlerManager;
    

    public LuceneSearchProviderImpl(ScheduleIndexer scheduleIndexer,LuceneQueryBuilder luceneQueryBuilder,
    		PermissionsFilterGenerator permissionsFilterGenerator,SearchHandlerManager searchHandlerManager,
    		ZephyrPermissionsFilterGenerator zephyrPermissionsFilterGenerator) {
        this.luceneQueryBuilder = luceneQueryBuilder;
        this.scheduleIndexer=scheduleIndexer;
        this.permissionsFilterGenerator=permissionsFilterGenerator;
        this.zephyrPermissionsFilterGenerator=zephyrPermissionsFilterGenerator;
        this.searchHandlerManager=searchHandlerManager;
    }
    

    public List<Document> search(final Query query, final ApplicationUser searcher) throws Exception {
        return searchWithOverride(query, searcher, null,true);
    }

    public SearchResult search(final Query query, final ApplicationUser searcher,Integer startIndex,boolean maxAllowedResult,Integer maxRecords,boolean overrideSecurityFlag) throws Exception {
        return search(query, searcher, null,overrideSecurityFlag,startIndex,maxAllowedResult,maxRecords);
    }
    
    public SearchResult searchMax(Query query, ApplicationUser searcher,boolean overrideSecurityFlag,boolean bypassPermissionFilter) throws Exception {
    	 return searchMax(query, searcher, null,overrideSecurityFlag,bypassPermissionFilter);
    }

    private org.apache.lucene.search.Query createLuceneQuery(Query searchQuery, org.apache.lucene.search.Query andQuery, ApplicationUser searchUser, boolean overrideSecurity)
            throws SearchException {
        final String jqlSearchQuery = searchQuery.toString();
        org.apache.lucene.search.Query finalQuery = andQuery;

        if (searchQuery.getWhereClause() != null) {
            final QueryCreationContext context = new QueryCreationContextImpl(searchUser, overrideSecurity);
            final org.apache.lucene.search.Query query = luceneQueryBuilder.createLuceneQuery(context, searchQuery.getWhereClause());
            if (query != null) {
                    log.debug("ZQL query to search executions by issue id: " + jqlSearchQuery);


                if (finalQuery != null) {
                    BooleanQuery join = new BooleanQuery();
                    join.add(finalQuery, BooleanClause.Occur.MUST);
                    join.add(query, BooleanClause.Occur.MUST);
                    finalQuery = join;
                } else {
                    finalQuery = query;
                }
            } else {
                    log.debug("Got a null query from the ZQL Query.");

            }
        }
        // NOTE: we do this because when you are searching for everything the query is null
        if (finalQuery == null) {
            finalQuery = new MatchAllDocsQuery();
        }

        log.debug("Updated query to search executions by issue id, ZQL lucene query: " + finalQuery);

        return finalQuery;
    }

    public SearchResult searchMax(final Query query, final ApplicationUser searcher,final org.apache.lucene.search.Query andQuery,
    		final boolean overrideSecurity,final boolean bypassPermissionFilter) throws Exception {
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        UtilTimerStack.push("Lucene Query");
        final TopDocs luceneMatches = getHits(query, searcher, getSearchSorts(searcher, query),andQuery, overrideSecurity, bypassPermissionFilter, scheduleSearcher,0,Integer.MAX_VALUE);
        UtilTimerStack.pop("Lucene Query End");
        try
        {
            UtilTimerStack.push("Retrieve From cache/db and filter");
            final List<Document> matches;

        	final int totalScheduleCount = luceneMatches == null ? 0 : luceneMatches.totalHits;
            if (luceneMatches != null) {
                matches = new ArrayList();
                for (int i = 0 ; i < luceneMatches.totalHits; i++) {
                    Document doc = scheduleSearcher.doc(luceneMatches.scoreDocs[i].doc);
                    matches.add(doc);
                }
            } else {
                //if there were no lucene-matches, or the length of the matches is less than the page start index
                //return an empty list of issues.
                matches = Collections.emptyList();
            }
            UtilTimerStack.pop("Retrieve From cache/db and filter");
            return new SearchResult(0,totalScheduleCount,matches);
        } catch (final Exception e) {
            throw new Exception("Exception whilst searching for executions " + e.getMessage(), e);
        } finally {
        	scheduleSearcher.close();
        }
    }
    
    
    public SearchResult search(final Query query, final ApplicationUser searcher,final org.apache.lucene.search.Query andQuery,
    		final boolean overrideSecurity,final Integer startIndex,boolean maxAllowedResult,Integer maxRecords) throws Exception {
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        UtilTimerStack.push("Lucene Query");
    	Integer maxResultHit = Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
        String defaultMax = "5000";
        //For version below 6.0 will be null
        if(StringUtils.isBlank(defaultMax)) {
        	defaultMax = ComponentAccessor.getApplicationProperties().getDefaultBackedString(APKeys.JIRA_SEARCH_VIEWS_DEFAULT_MAX);
        }
        Integer allowedRecordLimit = defaultMax != null ? Integer.valueOf(defaultMax.trim()) : MAX_RECORDS_ALLOWED;
    	if(maxAllowedResult) {
        	maxResultHit = allowedRecordLimit;
        	maxRecords = maxResultHit;
        } else {
        	if(maxRecords != null && maxRecords != 0) {
	        	maxResultHit = Math.min(allowedRecordLimit, maxRecords);
	        	maxRecords = maxResultHit;
        	} else {
        		maxRecords = allowedRecordLimit;
        	}
        }

        final TopDocs luceneMatches = getHits(query, searcher, getSearchSorts(searcher, query),andQuery, overrideSecurity, false, scheduleSearcher,startIndex,maxRecords);
        UtilTimerStack.pop("Lucene Query End");
        try
        {
            UtilTimerStack.push("Retrieve From cache/db and filter");
            final List<Document> matches;

        	final int totalScheduleCount = luceneMatches == null ? 0 : luceneMatches.totalHits;
            if ((luceneMatches != null) && (luceneMatches.totalHits >= startIndex))
            {
                matches = new ArrayList();
            	final int end = Math.min(startIndex+maxResultHit, luceneMatches.totalHits);
                for (int i = startIndex; i < end; i++)
                {
                    Document doc = scheduleSearcher.doc(luceneMatches.scoreDocs[i].doc);
                    matches.add(doc);
                }
            } else {
                //if there were no lucene-matches, or the length of the matches is less than the page start index
                //return an empty list of issues.
                matches = Collections.emptyList();
            }
            UtilTimerStack.pop("Retrieve From cache/db and filter");
            return new SearchResult(startIndex,totalScheduleCount,matches);
        } catch (final Exception e) {
            throw new Exception("Exception whilst searching for executions " + e.getMessage(), e);
        } finally {
        	scheduleSearcher.close();
        }
    }

    
	private SortField[] getSearchSorts(final ApplicationUser searcher, Query query) {
		if (query == null) {
			return null;
		}
		SortUtil sortUtil = new SortUtil();
		List<SearchSort> sorts = sortUtil.getSearchSorts(query,
				searchHandlerManager, searcher);

		final List<SortField> luceneSortFields = new ArrayList<SortField>();
		// When the sorts have been specifically set to null then we run the
		// search with no sorts
		if (sorts != null) {
			for (SearchSort searchSort : sorts) {
				// Lets figure out what field this searchSort is referring to.
				// The {@link SearchHandlerManager#getField} method
				// actually a ZQL name.
				final List<String> fieldIds = new ArrayList<String>(searchHandlerManager.getFieldIds(searcher,searchSort.getField()));
				// sort to get consistent ordering of fields for clauses with
				// multiple fields
				Collections.sort(fieldIds);

				for (String fieldId : fieldIds) {
					luceneSortFields.addAll(sortUtil.getSortFields(sortUtil.getSortOrder(searchSort), fieldId));
				}
			}
		}

		return luceneSortFields.toArray(new SortField[luceneSortFields.size()]);
	}    

    public List<Document> searchWithOverride(final Query query, final ApplicationUser searcher,final org.apache.lucene.search.Query andQuery,
    		final boolean overrideSecurity) throws Exception {
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        UtilTimerStack.push("Lucene Query");
        final TopDocs luceneMatches = getHits(query, searcher, getSearchSorts(searcher, query),andQuery, overrideSecurity, false, scheduleSearcher,null,null);
        UtilTimerStack.pop("Lucene Query End");
        try {
            UtilTimerStack.push("Retrieve From cache/db and filter");
            final List<Document> matches;
            final int totalIssueCount = luceneMatches == null ? 0 : luceneMatches.totalHits;
            if (luceneMatches != null) {
                matches = new ArrayList<Document>();
                for (int i = 0; i < totalIssueCount; i++)
                {
                    Document doc = scheduleSearcher.doc(luceneMatches.scoreDocs[i].doc);
                    matches.add(doc);
                }
            } else {
                //if there were no lucene-matches, or the length of the matches is less than the page start index
                //return an empty list of issues.
                matches = Collections.emptyList();
            }
            UtilTimerStack.pop("Retrieve From cache/db and filter");
            return matches;
        } catch (final Exception e) {
            throw new Exception("Exception whilst searching for issues " + e.getMessage(), e);
        } finally {
        	scheduleSearcher.close();
        }
    }
    
    private TopDocs getHits(final Query searchQuery, final ApplicationUser searchUser, final SortField[] sortField,
    		final org.apache.lucene.search.Query andQuery, boolean overrideSecurity, boolean bypassPermissionFilter, IndexSearcher issueSearcher, Integer startIndex,Integer maxRecords) throws SearchException
    {
        if (searchQuery == null) {
            return null;
        }
        try {
        	final Filter permissionsFilter = getPermissionsFilter(overrideSecurity, bypassPermissionFilter, searchUser);
            final org.apache.lucene.search.Query finalQuery = createLuceneQuery(searchQuery, andQuery, searchUser, overrideSecurity);
            log.debug("Query to retrieve top hits: " + finalQuery);
            return runSearch(issueSearcher, finalQuery, permissionsFilter, searchQuery.toString(),sortField,startIndex,maxRecords);
        }
        catch (final Exception e)
        {
            throw new SearchException(e);
        }
    }
    
    private Filter getPermissionsFilter(final boolean overRideSecurity, boolean bypassPermissionFilter, final ApplicationUser searchUser) {
        if (!overRideSecurity) {
            final CachedWrappedFilterCache cache = getCachedWrappedFilterCache();
            Filter filter = null;
            if (JiraUtil.isJIRAGreaterThan710()) {
                try {
                    Method getFilterMethod = cache.getClass().getMethod("getFilter", ApplicationUser.class, Collection.class);
                    try {
                        Object[] args = {searchUser, null};
                        filter = (Filter) getFilterMethod.invoke(cache, args);
                    } catch (IllegalAccessException iae) {
                        log.error("IllegalAccessException while calling getFilterMethod in getPermissionsFilter method.",iae);
                    } catch (InvocationTargetException ite) {
                        log.error("InvocationTargetException while calling getFilterMethod in getPermissionsFilter method ",ite);
                    }

                    if (filter != null) {
                        return filter;
                    }
                    // if not in cache, construct a query (also using a cache)
                    org.apache.lucene.search.Query permissionQuery = null;
                    if (JiraUtil.getPermissionSchemeFlag() && !bypassPermissionFilter) {
                        permissionQuery = zephyrPermissionsFilterGenerator.getQuery(searchUser);
                    } else {
                        permissionQuery = permissionsFilterGenerator.getQuery(searchUser);

                    }
                    filter = new CachingWrapperFilter(new QueryWrapperFilter(permissionQuery));

                    Method storeFilterMethod = cache.getClass().getMethod("storeFilter", Filter.class, ApplicationUser.class, Collection.class);
                    try {
                        Object[] args = {filter, searchUser, null};
                        storeFilterMethod.invoke(cache, args);
                    } catch (IllegalAccessException iae) {
                        log.error("IllegalAccessException while calling storeFilterMethod in getPermissionsFilter method.",iae);
                    } catch (InvocationTargetException ite) {
                        log.error("InvocationTargetException while calling storeFilterMethod in getPermissionsFilter method.",ite);
                    }

                } catch (NoSuchMethodException ex) {
                    log.error("no such method found.",ex);
                }

            } else {
                filter = cache.getFilter(searchUser);
                if (filter != null) {
                    return filter;
                }
                // if not in cache, construct a query (also using a cache)
                org.apache.lucene.search.Query permissionQuery = null;
                if (JiraUtil.getPermissionSchemeFlag() && !bypassPermissionFilter) {
                    permissionQuery = zephyrPermissionsFilterGenerator.getQuery(searchUser);
                } else {
                    permissionQuery = permissionsFilterGenerator.getQuery(searchUser);

                }
                filter = new CachingWrapperFilter(new QueryWrapperFilter(permissionQuery));
                cache.storeFilter(filter, searchUser);
            }
            return filter;
        } else {
            return null;
        }
    }

    private TopDocs runSearch(final IndexSearcher searcher, final org.apache.lucene.search.Query query, final Filter filter, 
    		final String searchQueryString,final SortField[] sortFields, Integer startIndex,Integer maxRecords) 
    	throws IOException
    {
        log.debug("Lucene boolean Query:" + query.toString(""));

        UtilTimerStack.push("Lucene Search");

        TopDocs hits;
        final OpTimer opTimer = Instrumentation.pullTimer(InstrumentationName.ISSUE_INDEX_READS);
        try {        	
        	Integer maxResultHit = null;
            if(maxRecords != null && maxRecords > 0) {
            	maxResultHit = maxRecords;
            }else{
                maxResultHit = Integer.valueOf(JiraUtil.getSimpleDBProperty(ConfigurationConstants.ZEPHYR_ZQL_RESULT_MAX_ON_PAGE, "20").toString());
            }
        	int maxHits =  maxResultHit.intValue(); 
            int docEnd = 0;
            if(startIndex == null) {
            	docEnd = maxHits;
            } else {
            	docEnd = startIndex + maxHits;
            }
            if ((sortFields != null) && (sortFields.length > 0)) { // a zero length array sorts in very weird ways! JRA-5151
            	hits = searcher.search(query, filter, docEnd, new Sort(sortFields));
        	} else {
        		hits = searcher.search(query, filter, docEnd);
        	}
            // NOTE: this is only here so we can flag any queries in production that are taking long and try to figure out
            // why they are doing that.
            final long timeQueryTook = opTimer.end().getMillisecondsTaken();
            if (timeQueryTook > 400) {

                // truncate lucene query at 800 characters
                String msg = String.format("ZQL query '%s' produced lucene query '%-1.800s' and took '%d' ms to run.", searchQueryString, query.toString(), timeQueryTook);
                log.debug(msg);

                slowLog.info(msg);

            }
        } finally {
            UtilTimerStack.pop("Lucene Search");
        }
        return hits;
    }



    private CachedWrappedFilterCache getCachedWrappedFilterCache() {
        CachedWrappedFilterCache cache = (CachedWrappedFilterCache) JiraAuthenticationContextImpl.getRequestCache().get(
                RequestCacheKeys.CACHED_WRAPPED_FILTER_CACHE);

        if (cache == null) {
            log.debug("Creating new CachedWrappedFilterCache");

            cache = new CachedWrappedFilterCache();
            JiraAuthenticationContextImpl.getRequestCache().put(RequestCacheKeys.CACHED_WRAPPED_FILTER_CACHE, cache);
        }
        return cache;
    }
    
    public long searchCount(final Query query, final ApplicationUser user) throws SearchException {
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        try {
        	return getHitCount(query, user, null, false, scheduleSearcher);
        } catch(SearchException e) {
        	log.warn("Error retrieving count for zql=" + query ,e);
        } finally {
        	try {
				scheduleSearcher.close();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (searchCount) ",e);
			}
        }
        return 0;
    }

    /**
     * Returns 0 if there are no Lucene parameters (search request is null), otherwise returns the hit count
     * <p/>
     * The count is 0 if there are no matches.
     *
     * @param searchQuery    search request
     * @param searchUser user performing the search
     * @param andQuery   a query to join with the request
     * @param overrideSecurity ignore the user security permissions
     * @param scheduleSearcher the IndexSearcher to be used when searching
     * @return hit count
     * @throws SearchException if error occurs
     * @throws com.atlassian.jira.issue.search.ClauseTooComplexSearchException if query creates a lucene query that is too complex to be processed.
     */
    private long getHitCount(final Query searchQuery, final ApplicationUser searchUser, final org.apache.lucene.search.Query andQuery,
    		boolean overrideSecurity, IndexSearcher scheduleSearcher) throws SearchException {
        if (searchQuery == null) {
            return 0;
        }
        try {
            final Filter permissionsFilter = getPermissionsFilter(overrideSecurity, false, searchUser);
            final org.apache.lucene.search.Query finalQuery = createLuceneQuery(searchQuery, andQuery, searchUser, overrideSecurity);
            final TotalHitCountCollector hitCountCollector = new TotalHitCountCollector();
            scheduleSearcher.search(finalQuery, permissionsFilter, hitCountCollector);
            return hitCountCollector.getTotalHits();
        } catch (IOException e) {
            throw new SearchException(e);
        }
        
    }
    
    /**
     * Searches the result and sorts them in the collector
     *
     *
     * @param searchQuery
     * @param executionQuery
     * @param user
     * @param andQuery
     * @param overrideSecurity
     * @param offset
     * @throws SearchException
     *
     */
    public Map<String, Object> searchAndSort(final Query searchQuery, Query executionQuery, final ApplicationUser user,
                              org.apache.lucene.search.Query andQuery, boolean overrideSecurity, Integer offset) throws SearchException {
        final long start = System.currentTimeMillis();
        UtilTimerStack.push("Searching and sorting with Collector");
        Map<String, Object> executionDocMap = new HashMap<String, Object>();
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        try {
            final int executionDocId = getExecutionDocId(executionQuery, user, andQuery, overrideSecurity, scheduleSearcher);
            final TopDocs luceneMatches = getHits(searchQuery, user, getSearchSorts(user, searchQuery), null, overrideSecurity, false, scheduleSearcher, 0, 1000000);
            if ((luceneMatches != null) && luceneMatches.totalHits >= 0) {
                for (int i = 0; i < luceneMatches.scoreDocs.length; i++) {
                    int currentDoc = luceneMatches.scoreDocs[i].doc;
                    if(currentDoc == executionDocId){
                        final Document scheduleDocument = scheduleSearcher.doc(executionDocId);
                        String scheduleId = scheduleDocument.get("schedule_id");
                        executionDocMap.put(scheduleId, scheduleDocument);
                        if(i > 0){
                            String prevScheduleId = scheduleSearcher.doc(luceneMatches.scoreDocs[i-1].doc).get("schedule_id");
                            executionDocMap.put("prevExecutionId", prevScheduleId);
                        }
                        if(i < luceneMatches.scoreDocs.length - 1){
                            String nextScheduleId = scheduleSearcher.doc(luceneMatches.scoreDocs[i+1].doc).get("schedule_id");
                            executionDocMap.put("nextExecutionId", nextScheduleId);
                        }
                        executionDocMap.put("offset", (i>0)?i-1:0);
                        break;
                    }
                }
            }else{
                final Document scheduleDocument = scheduleSearcher.doc(executionDocId);
                String scheduleId = scheduleDocument.get("schedule_id");
                executionDocMap.put(scheduleId, scheduleDocument);
            }
        } catch (Exception e) {
            throw new SearchException("Exception whilst searching for schedules " + e.getMessage(), e);
        } finally {
        	try {
				scheduleSearcher.close();
			} catch (IOException e) {
	        	log.warn("Error closing searchers (searchAndSort) ",e);
			}
        }

        UtilTimerStack.pop("Searching and sorting with Collector");
        ThreadLocalQueryProfiler.store(ThreadLocalQueryProfiler.LUCENE_GROUP, String.valueOf(searchQuery), (System.currentTimeMillis() - start));
        return executionDocMap;
     }

    public long searchCountByPassSecurity(final Query query, final ApplicationUser user) throws SearchException {
        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
        try {
            return getHitCount(query, user, null, true, scheduleSearcher);
        } catch(SearchException e) {
            log.warn("Error retrieving count for zql=" + query ,e);
        } finally {
            try {
                scheduleSearcher.close();
            } catch (IOException e) {
                log.warn("Error closing searchers (searchCount) ",e);
            }
        }
        return 0;
    }

    /**
     * Fetched doc ID for current execution
     * @param executionQuery
     * @param user
     * @param andQuery
     * @param overrideSecurity
     * @param scheduleSearcher
     * @return
     * @throws Exception
     */
    private int getExecutionDocId(Query executionQuery, ApplicationUser user, org.apache.lucene.search.Query andQuery, boolean overrideSecurity, IndexSearcher scheduleSearcher) throws Exception {
        final TopDocs executionTopDoc = scheduleSearcher.search(createLuceneQuery(executionQuery, andQuery, user, overrideSecurity), 1);
        if(executionTopDoc == null || executionTopDoc.scoreDocs == null || executionTopDoc.scoreDocs.length < 0){
            throw new Exception("Execution Not Found, please check your indexes" );
        }
        return executionTopDoc.scoreDocs[0].doc;
    }
}
