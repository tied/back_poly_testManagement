package com.thed.zephyr.je.zql.core;


import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.thed.zephyr.je.zql.helper.SearchResult;

/**
 * A SearchProvider in Zephyr allows users to run structured searches against Zephyr Lucene index as opposed
 * to database (SQL) based queries.
 *
 * All search methods take a {@link com.atlassian.query.Query} which defines the criteria of the search,
 * including any sort information.
 */
public interface LuceneSearchProvider
{
    /**
     * Search the index, and only return schedules.
     * <em>Note: that this method returns read only {@link com.thed.zephyr.je.model.Schedule} objects, and should not be
     * used where you need the schedule for update</em>.
     *
     * Also note that if you are only after the number of search results use
     * {@link #searchCount(com.atlassian.query.Query ,User)} as it provides better performance.
     *
     * @param query contains the information required to perform the search.
     * @param searcher the user performing the search, which will be used to create a permission filter that filters out
     * any of the results the user is not able to see and will be used to provide context for the search.
     *
     * @return A {@link List<Document>} containing the resulting schedules.
     *
     * @throws Exception 
     * @throws java.lang.Exception if the query or part of the query produces
     * lucene that is too complex to be processed.
     */
	List<Document> search(Query query, ApplicationUser searcher) throws Exception;

	 /**
     * Search the index and return all schedules. 
     * @param query contains the information required to perform the search.
     * @param searcher the user performing the search, which will be used to create a permission filter that filters out
     * any of the results the user is not able to see and will be used to provide context for the search.
     *
     * @return A {@link List<Document>} containing the resulting schedules.
     *
     * @throws Exception 
     * @throws java.lang.Exception if the query or part of the query produces
     * lucene that is too complex to be processed.
     */
	SearchResult searchMax(Query query, ApplicationUser searcher,boolean overrideSecurityFlag,boolean bypassPermissionFilter) throws Exception;
	
	
	   /**
  * Search the index with startIndex parameter, and only return schedules. If startIndex is null itdefaults to 0 
  * <em>Note: that this method returns read only {@link com.thed.zephyr.je.model.Schedule} objects, and should not be
  * used where you need the schedule for update</em>.
  *
  * Also note that if you are only after the number of search results use
  * {@link #searchCount(com.atlassian.query.Query ,User)} as it provides better performance.
  *
  * @param query contains the information required to perform the search.
  * @param searcher the user performing the search, which will be used to create a permission filter that filters out
  * any of the results the user is not able to see and will be used to provide context for the search.
  *
  * @return A {@link List<Document>} containing the resulting schedules.
  *
  * @throws Exception 
  * @throws java.lang.Exception if the query or part of the query produces
  * lucene that is too complex to be processed.
  */
	SearchResult search(Query query, ApplicationUser searcher,Integer startIndex,boolean maxAllowedResult,Integer maxRecords,boolean overrideSecurityFlag) throws Exception;
	
    /**
     * Search the index, and only return schedules while AND'ing the raw lucene query
     * to the generated query from the provided searchQuery.
     *
     * Also note that if you are only after the number of search results use
     * {@link #searchCount(com.atlassian.query.Query ,User)} as it provides better performance.
     *
     * @param query contains the information required to perform the search.
     * @param searcher the user performing the search, which will be used to create a permission filter that filters out
     * any of the results the user is not able to see and will be used to provide context for the search.
     * @param andQuery raw lucene Query to AND with the request.
     *
     * @return A {@link List<Document>} containing the resulting schedules.
     *
     * @throws Exception thrown if there is a severe problem encountered with lucene when searching (wraps an
     * IOException).
     * lucene that is too complex to be processed.
     */
    List<Document> searchWithOverride(Query query, ApplicationUser searcher, org.apache.lucene.search.Query andQuery,boolean overrideSecurity) throws Exception;
    
    
    /**
     * Return the number of schedules matching the provided search criteria.
     * <b>Note:</b> This does not load all results into memory and provides better performance than
     * {@link #search(com.atlassian.query.Query ,User)}
     *
     * @param query contains the information required to perform the search.
     * @param searcher the user performing the search which will be used to provide context for the search.
     *
     * @return number of matching results.
     *
     * @throws SearchException thrown if there is a severe problem encountered with lucene when searching (wraps an
     * IOException).
     * @throws com.atlassian.jira.issue.search.ClauseTooComplexSearchException if the query or part of the query produces
     * lucene that is too complex to be processed.
     */
    long searchCount(Query query, ApplicationUser searcher) throws SearchException;
    
    
    
    /**
     * Return the number of schedules matching the provided search criteria.
     * {@link #search(com.atlassian.query.Query ,User)}
     *
     *
     *
     *
     * @param searchQuery query which we will look for results in
     * @param executionQuery
     * @param andQuery
     * @param offset
     *
     * @return number of matching results.
     *
     * @throws SearchException thrown if there is a severe problem encountered with lucene when searching (wraps an
     * IOException).
     * @throws com.atlassian.jira.issue.search.ClauseTooComplexSearchException if the query or part of the query produces
     * lucene that is too complex to be processed.
     */
    Map<String, Object> searchAndSort(final Query searchQuery, Query executionQuery, final ApplicationUser user,
                       org.apache.lucene.search.Query andQuery, boolean overrideSecurity, Integer offset) throws SearchException;


    long searchCountByPassSecurity(Query query, ApplicationUser user) throws SearchException;
}
