package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.util.JqlIssueSupport;
import com.atlassian.query.clause.TerminalClause;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "Execution Defect Key" ZQL clause.
 *
 */
public class ExecutionDefectKeyClauseQueryFactory extends DefectKeyClauseQueryFactory implements ClauseQueryFactory 
{
    private final JqlOperandResolver operandResolver;
    private final JqlIssueSupport issueSupport;
    private final IssueManager issueManager;

    public ExecutionDefectKeyClauseQueryFactory(final JqlOperandResolver operandResolver, 
    		final JqlIssueSupport issueSupport,final IssueManager issueManager)
    {
        this.issueSupport = notNull("issueSupport", issueSupport);
        this.operandResolver = notNull("operandResolver", operandResolver);
        this.issueManager = notNull("issueManager", issueManager);
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
    	return this.getQuery(queryCreationContext, terminalClause, operandResolver, issueSupport, issueManager, SystemSearchConstant.forLinkedDefectKey());
    }
}
