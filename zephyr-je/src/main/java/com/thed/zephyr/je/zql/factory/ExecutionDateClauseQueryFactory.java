package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.DateClauseQueryFactory;
import com.atlassian.jira.jql.util.JqlDateSupport;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

import static com.atlassian.util.concurrent.Assertions.notNull;

/**
 * Creates clauses for queries on the execution date field.
 *
 */
public class ExecutionDateClauseQueryFactory extends DateClauseQueryFactory implements ClauseQueryFactory
{
    //CLOVER:OFF
    public ExecutionDateClauseQueryFactory(JqlDateSupport zqlDateSupport, JqlOperandResolver zqlOperandResolver)
    {
        super(SystemSearchConstant.forExecutionDate(), notNull("zqlDateSupport", zqlDateSupport), notNull("jqlOperandResolver", zqlOperandResolver));    
    }
    //CLOVER:ON
}
