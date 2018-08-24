package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.issue.search.constants.SimpleFieldSearchConstantsWithEmpty;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.resolver.ComponentIndexInfoResolver;
import com.atlassian.jira.jql.resolver.ComponentResolver;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;

/**
 * Creates queries for component clauses.
 *
 */
public class ComponentClauseQueryFactory implements ClauseQueryFactory
{
    private final JqlOperandResolver operandResolver;
    private final ProjectComponentManager projectComponentManager;

    public ComponentClauseQueryFactory( ProjectComponentManager projectComponentManager,JqlOperandResolver operandResolver) {
    	this.projectComponentManager = projectComponentManager;
    	this.operandResolver=operandResolver;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
        final SimpleFieldSearchConstantsWithEmpty searchConstants = SystemSearchConstant.forComponent();
    	ComponentResolver componentResolver = new ComponentResolver(projectComponentManager);
    	ComponentIndexInfoResolver indexInfoResolver = new ComponentIndexInfoResolver(componentResolver);
        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        operatorFactories.add(new EqualitySpecifiedValueQueryFactory<ProjectComponent>(indexInfoResolver, searchConstants.getEmptyIndexValue()));
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
            return oper.handleEquals(SystemSearchConstant.forComponent().getIndexField(),literals,indexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forComponent().getIndexField(),literals,indexInfoResolver);
        } else
        {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }    	
    }
}