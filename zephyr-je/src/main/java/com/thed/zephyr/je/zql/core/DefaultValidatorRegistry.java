package com.thed.zephyr.je.zql.core;


import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.validator.ChangedClauseValidator;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.WasClauseValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.clause.ChangedClause;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.WasClause;

public class DefaultValidatorRegistry implements ValidatorRegistry
{
    private final SearchHandlerManager manager;
    private final List<ClauseValidator> wasClauseValidators= new ArrayList<ClauseValidator>();
    private final ChangedClauseValidator changedClauseValidator;


    public DefaultValidatorRegistry(final SearchHandlerManager manager) {
        this.manager = notNull("manager", manager);
        this.wasClauseValidators.add(notNull("wasClauseValidator", ComponentAccessor.getComponentOfType(WasClauseValidator.class)));
        this.changedClauseValidator = (notNull("changedClauseValidator", ComponentAccessor.getComponentOfType(ChangedClauseValidator.class)));

    }

    public Collection<ClauseValidator> getClauseValidator(final ApplicationUser searcher, final TerminalClause clause) {
        notNull("clause", clause);

        Collection<ClauseHandler> clauseHandlers = manager.getClauseHandler(searcher, clause.getName());

        // Collect the factories.
        List<ClauseValidator> clauseValidators = new ArrayList<ClauseValidator>(clauseHandlers.size());
        for (ClauseHandler clauseHandler : clauseHandlers)
        {
            clauseValidators.add(clauseHandler.getValidator());
        }
        return clauseValidators;
    }

    @Override
    public Collection<ClauseValidator> getClauseValidator(ApplicationUser searcher, WasClause clause) {
        return Collections.unmodifiableCollection(wasClauseValidators);
    }

    @Override
    public ChangedClauseValidator getClauseValidator(ApplicationUser searcher, ChangedClause clause) {
        return changedClauseValidator;
    }
}
