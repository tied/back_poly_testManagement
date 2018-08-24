package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.resolver.ProjectIndexInfoResolver;
import com.atlassian.jira.jql.resolver.ProjectResolver;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "Project" clause.
 *
 */
public class ProjectClauseQueryFactory implements ClauseQueryFactory
{
    private static final Logger log = Logger.getLogger(ProjectClauseQueryFactory.class);
    private final JqlOperandResolver operandResolver;
    private final ProjectManager projectManager;

    public ProjectClauseQueryFactory(ProjectManager projectManager,JqlOperandResolver operandResolver)
    {
    	this.projectManager = projectManager;
    	this.operandResolver=operandResolver;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
    	ProjectResolver projectResolver = new ProjectResolver(projectManager);
    	ProjectIndexInfoResolver indexInfoResolver = new ProjectIndexInfoResolver(projectResolver);
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
            return oper.handleEquals(SystemSearchConstant.forProject().getIndexField(),literals,indexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forProject().getIndexField(),literals,indexInfoResolver);
        } else
        {
            //log.warn(String.format("The '%s' clause does not support the %s operator.", terminalClause.getName(), operator));
            return QueryFactoryResult.createFalseResult();
        }
    }
}
