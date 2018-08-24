package com.thed.zephyr.je.zql.factory;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import static com.atlassian.jira.util.dbc.Assertions.notNull;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.comparator.PriorityObjectComparator;
import com.atlassian.jira.issue.index.indexers.impl.BaseFieldIndexer;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.query.RelationalOperatorIdIndexValueQueryFactory;
import com.atlassian.jira.jql.resolver.IssueConstantInfoResolver;
import com.atlassian.jira.jql.resolver.PriorityResolver;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "Priority" ZQL clause.
 *
 */
public class PriorityClauseQueryFactory implements ClauseQueryFactory
{
    private static final Logger log = Logger.getLogger(PriorityClauseQueryFactory.class);
    private final JqlOperandResolver operandResolver;
    private final ConstantsManager constantsManager;

    public PriorityClauseQueryFactory(ConstantsManager constantsManager,JqlOperandResolver operandResolver)
    {
    	this.operandResolver=operandResolver;
    	this.constantsManager=constantsManager;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
    	PriorityResolver priorityResolver = new PriorityResolver(constantsManager);
        IssueConstantInfoResolver<Priority> constantInfoResolver = new IssueConstantInfoResolver<Priority>(priorityResolver);
        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        operatorFactories.add(new EqualitySpecifiedValueQueryFactory<Priority>(constantInfoResolver, BaseFieldIndexer.NO_VALUE_INDEX_VALUE));
        operatorFactories.add(new RelationalOperatorIdIndexValueQueryFactory<Priority>(PriorityObjectComparator.PRIORITY_OBJECT_COMPARATOR, priorityResolver, constantInfoResolver));
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(SystemSearchConstant.forPriority(), operatorFactories, operandResolver);

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
            return oper.handleEquals(SystemSearchConstant.forPriority().getIndexField(),literals,constantInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forPriority().getIndexField(),literals,constantInfoResolver);
        } else
        {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }    	
    }
}
