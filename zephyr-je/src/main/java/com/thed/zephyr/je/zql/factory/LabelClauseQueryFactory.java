package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.issue.index.indexers.impl.BaseFieldIndexer;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstants;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.LabelIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.LabelResolver;

import java.util.ArrayList;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * A {@link ClauseQueryFactory} for the "Label" clause.
 *
 */
public class LabelClauseQueryFactory implements ClauseQueryFactory
{
//	private final LabelResolver labelResolver;
	private final JqlOperandResolver operandResolver;
    private final LabelManager labelManager;


    public LabelClauseQueryFactory(LabelManager labelManager, JqlOperandResolver operandResolver)
    {
        this.labelManager = labelManager;
        this.operandResolver=operandResolver;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
        LabelResolver labelResolver = new LabelResolver(labelManager);

        final LabelIndexInfoResolver indexInfoResolver = new LabelIndexInfoResolver(labelResolver);

        final List<OperatorSpecificQueryFactory> operatorFactories =
                CollectionBuilder.<OperatorSpecificQueryFactory>newBuilder(
                        new EqualityWithSpecifiedEmptyValueQueryFactory<Label>(indexInfoResolver, BaseFieldIndexer.NO_VALUE_INDEX_VALUE)).asList();
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(SystemSearchConstant.forLabel().getIndexField(), operatorFactories, operandResolver);

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
            return oper.handleEquals(SystemSearchConstant.forLabel().getIndexField(),literals,indexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forLabel().getIndexField(),literals,indexInfoResolver);
        } else
        {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }
    }
}
