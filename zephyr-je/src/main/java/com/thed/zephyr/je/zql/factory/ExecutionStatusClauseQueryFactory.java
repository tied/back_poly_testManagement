package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.List;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.ExecutionStatusIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.ExecutionStatusResolver;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "ExecutionStatus" ZQL clause.
 *
 */
public class ExecutionStatusClauseQueryFactory implements ClauseQueryFactory
{
	private final ExecutionStatusResolver executionStatusResolver;
	private final JqlOperandResolver operandResolver;
	
    public ExecutionStatusClauseQueryFactory(ExecutionStatusResolver executionStatusResolver,JqlOperandResolver operandResolver)
    {
    	this.executionStatusResolver=executionStatusResolver;
    	this.operandResolver=operandResolver;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
    	ExecutionStatusIndexInfoResolver indexInfoResolver = new ExecutionStatusIndexInfoResolver(executionStatusResolver);
        notNull("queryCreationContext", queryCreationContext);
        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();

        if (OperatorClasses.EMPTY_ONLY_OPERATORS.contains(operator) && !operand.equals(EmptyOperand.EMPTY))
        {
            return QueryFactoryResult.createFalseResult();
        }
        OperatorHelper oper = new OperatorHelper();
        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);

        if (literals == null)
        {
            return QueryFactoryResult.createFalseResult();
        }
        else if (oper.isEqualityOperator(operator))
        {
            return oper.handleEquals(SystemSearchConstant.forStatus().getIndexField(),literals,indexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forStatus().getIndexField(),literals,indexInfoResolver);
        } else
        {
            return QueryFactoryResult.createFalseResult();
        }
    }
}
