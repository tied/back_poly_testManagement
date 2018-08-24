package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstants;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.EqualityQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CycleIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CycleNameResolver;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "CycleName" clause.
 *
 */
public class CycleNameClauseQueryFactory implements ClauseQueryFactory
{
	private final CycleNameResolver cycleNameResolver;
	private final JqlOperandResolver operandResolver;

    
    public CycleNameClauseQueryFactory(CycleNameResolver cycleNameResolver,JqlOperandResolver operandResolver)
    {
        this.cycleNameResolver=cycleNameResolver;
        this.operandResolver=operandResolver;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
        final SimpleFieldSearchConstants searchConstants = SystemSearchConstant.forCycleName();
        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        final CycleIndexInfoResolver indexInfoResolver = new CycleIndexInfoResolver(cycleNameResolver);
        operatorFactories.add(new LikeQueryFactory());
        operatorFactories.add(new EqualityQueryFactory<Cycle>(indexInfoResolver));
    	ClauseQueryFactory delegateClauseQueryFactory =  new GenericClauseQueryFactory(searchConstants.getIndexField(), operatorFactories, operandResolver);
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
            return oper.handleEquals(SystemSearchConstant.forCycleName().getIndexField(),literals,indexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forCycleName().getIndexField(),literals,indexInfoResolver);
        } else
        {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        } 
    }
}
