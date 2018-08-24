package com.thed.zephyr.je.zql.core;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.Query;
import com.thed.zephyr.je.zql.helper.SearchResult;

public interface SearchService {

	/**
	*  Search the index, and only return schedules that are available.
    *
    * @param searcher the user performing the search, which will be used to create a permission filter that filters out
    * any of the results the user is not able to see and will be used to provide context for the search.
    * @param query contains the information required to perform the search.
    *
    * @return A {@link List of Lucene Document} containing the resulting executions.
    * @throws Exception thrown if there is a severe problem encountered with lucene when searching (wraps an
    * IOException).
    */
	SearchResult search(ApplicationUser searcher, Query query, Integer startIndex,boolean maxAllowedResult,Integer maxRecords,boolean overrideSecurity) throws Exception;

	
	/**
	*  Search the index, and returns All schedules that are available.
    *
    * @param searcher the user performing the search, which will be used to create a permission filter that filters out
    * any of the results the user is not able to see and will be used to provide context for the search.
    * @param query contains the information required to perform the search.
    * @param override security
    * @param ByPass Zephyr Permission
    *
    * @return A {@link List of Lucene Document} containing the resulting executions.
    * @throws Exception thrown if there is a severe problem encountered with lucene when searching (wraps an
    * IOException).
    */
	SearchResult searchMax(ApplicationUser searcher, Query query, boolean overrideSecurity, boolean bypassPermissionFilter) throws Exception;

   
	/**
	*  Search the index, and only return schedules count that are available by executing the zql.
    *
    * @param searcher the user performing the search, which will be used to create a permission filter that filters out
    * any of the results the user is not able to see and will be used to provide context for the search.
    * @param query contains the information required to perform the search.
    *
    * @return long count of executions returned by zql.
    * @throws Exception thrown if there is a severe problem encountered with lucene when searching (wraps an
    * IOException).
    */
	long searchCount(ApplicationUser searcher, Query query) throws Exception;

	/**
	 * Retrieves Index Count bypassing Permission
	 * @param query
	 * @param applicationUser
	 * @return
	 * @throws Exception
	 */
	long searchCountByPassSecurity(Query query,ApplicationUser applicationUser) throws Exception;
   
	/**
    * Parses the query string into a ZQL {@link com.atlassian.query.Query}.
    *
    * @param searcher the user in context
    * @param query the query to parse into a {@link com.atlassian.query.Query}.
    * @return a result set that contains the query and a message set of any errors or warnings that occured during the parse.
    */
   ParseResult parseQuery(ApplicationUser searcher, String query);

   /**
    * Validates the specified {@link com.atlassian.query.Query} for the searching user.
    *
    * @param searcher the user performing the search
    * @param query the search query to validate
    * @return a message set containing any errors encountered; never null.
    */
   MessageSet validateQuery(ApplicationUser searcher, Query query);
   
   Map<String, Object> search(Query currentQuery, final Query searchQuery, final ApplicationUser user, org.apache.lucene.search.Query andQuery, boolean overrideSecurity, Integer offset) throws SearchException;
}
