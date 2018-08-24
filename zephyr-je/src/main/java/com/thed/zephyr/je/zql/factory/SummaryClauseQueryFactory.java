/**
 * 
 */
package com.thed.zephyr.je.zql.factory;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.clause.TerminalClause;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

/**
 * @author niravshah
 *
 */
public class SummaryClauseQueryFactory implements ClauseQueryFactory {
    static final int SUMMARY_BOOST_FACTOR = 9;
    private final ClauseQueryFactory delegateClauseQueryFactory;


    public SummaryClauseQueryFactory(JqlOperandResolver jqlOperandResolver) {
    	delegateClauseQueryFactory = getDelegate(jqlOperandResolver);
    }
    
	public QueryFactoryResult getQuery(QueryCreationContext queryCreationContext,TerminalClause terminalClause) {
		final QueryFactoryResult queryFactoryResult = delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        if (queryFactoryResult != null && queryFactoryResult.getLuceneQuery() != null)
        {
            // Summary always gets a boost of 9. JIRA always does that to get accurate result
            queryFactoryResult.getLuceneQuery().setBoost(SUMMARY_BOOST_FACTOR);
        }
        return queryFactoryResult;
	}
	
	
    private ClauseQueryFactory getDelegate(final JqlOperandResolver operandResolver)
    {
        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        operatorFactories.add(new LikeQueryFactory());
        return new GenericClauseQueryFactory(SystemSearchConstant.forTestSummary(), operatorFactories, operandResolver);
    }
}
