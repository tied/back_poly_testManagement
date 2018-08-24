package com.thed.zephyr.je.zql.core;


import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.query.clause.TerminalClause;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Able to map clauses to query handlers.
 *
 */
public class QueryRegistryImpl implements QueryRegistry
{
    private final SearchHandlerManager manager;

    public QueryRegistryImpl(final SearchHandlerManager manager)
    {
        this.manager = notNull("manager", manager);
    }

    public Collection<ClauseQueryFactory> getClauseQueryFactory(final QueryCreationContext queryCreationContext, final TerminalClause clause)
    {
        notNull("clause", clause);
        final Collection<ClauseHandler> handlers;
        if (!queryCreationContext.isSecurityOverriden())
        {
            handlers = manager.getClauseHandler(queryCreationContext.getUser(), clause.getName());
        }
        else
        {
            handlers = manager.getClauseHandler(clause.getName());
        }
        // Collect the factories.
        List<ClauseQueryFactory> clauseQueryFactories = new ArrayList<ClauseQueryFactory>(handlers.size());
        for (ClauseHandler clauseHandler : handlers)
        {
            clauseQueryFactories.add(clauseHandler.getFactory());
        }
        return clauseQueryFactories;
    }
}