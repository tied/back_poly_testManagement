package com.thed.zephyr.je.zql.core;

import java.util.Collection;

import com.atlassian.jira.bc.user.search.UserPickerSearchService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.ValueGeneratingClauseHandler;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.jql.values.UserClauseValuesGenerator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.operator.Operator;

public class PredicateRegistryImpl implements PredicateRegistry {
    private final UserSearchService userSearchService;
    private final SearchHandlerManager searchHandlerManager;
    private final JiraAuthenticationContext authenticationContext;

    public PredicateRegistryImpl(UserSearchService userSearchService, SearchHandlerManager searchHandlerManager,
    		JiraAuthenticationContext authenticationContext) {
        this.userSearchService = userSearchService;
        this.searchHandlerManager = searchHandlerManager;
        this.authenticationContext = authenticationContext;
    }
    
    @Override
	public ClauseValuesGenerator getClauseValuesGenerator(
			final String predicateName, final String fieldName) {
		if (Operator.BY.name().equalsIgnoreCase(predicateName)) {
			return new UserClauseValuesGenerator(userSearchService);
		}
		if (Operator.FROM.name().equalsIgnoreCase(predicateName)
				|| Operator.TO.name().equalsIgnoreCase(predicateName)) {
			return getClauseValuesGeneratorForField(fieldName);
		}
		return null;
	}

	private ClauseValuesGenerator getClauseValuesGeneratorForField(
			final String fieldName) {
		final ApplicationUser searcher = authenticationContext.getLoggedInUser();
		final Collection<ClauseHandler> clauseHandlers = searchHandlerManager
				.getClauseHandler(searcher, fieldName);
		if (clauseHandlers != null && clauseHandlers.size() == 1) {
			ClauseHandler clauseHandler = clauseHandlers.iterator().next();

			if (clauseHandler instanceof ValueGeneratingClauseHandler) {
				return ((ValueGeneratingClauseHandler) clauseHandler)
						.getClauseValuesGenerator();
			}
		}
		return null;
	}
}
