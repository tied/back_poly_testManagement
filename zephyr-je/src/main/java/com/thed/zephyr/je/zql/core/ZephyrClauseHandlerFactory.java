package com.thed.zephyr.je.zql.core;

import java.util.Collection;

import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.jql.ClauseHandler;

/**
 * Provides access to Zephyr clause handlers for clauses supported by ZQL.
 *
 */
public interface ZephyrClauseHandlerFactory {
	Collection<SearchHandler> getZQLClauseSearchHandlers();

	Collection<SearchHandler> getZQLCustomClauseSearchHandlers();


	SearchHandler addClauseHandlerForCustomFieldType(String customFieldType, Integer customFieldId, String clauseName);
}
