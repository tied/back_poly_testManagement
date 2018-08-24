package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.List;

import com.atlassian.jira.issue.comparator.VersionComparator;
import com.atlassian.jira.issue.index.indexers.impl.BaseFieldIndexer;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.EqualityWithSpecifiedEmptyValueQueryFactory;
import com.atlassian.jira.jql.query.GenericClauseQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.query.VersionSpecificRelationalOperatorQueryFactory;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.FixVersionIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.FixVersionResolver;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "Fix Version" ZQL clause.
 *
 */
public class VersionClauseQueryFactory implements ClauseQueryFactory
{
    private final JqlOperandResolver operandResolver;
    private final VersionManager versionManager;

    public VersionClauseQueryFactory(VersionManager versionManager,JqlOperandResolver operandResolver)
    {
    	this.operandResolver=operandResolver;
    	this.versionManager=versionManager;
    }
    

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
    	FixVersionResolver versionResolver = new FixVersionResolver(versionManager);

        final FixVersionIndexInfoResolver versionIndexInfoResolver = new FixVersionIndexInfoResolver(versionResolver);
        final VersionSpecificRelationalOperatorQueryFactory relationQueryFactory = new VersionSpecificRelationalOperatorQueryFactory(VersionComparator.COMPARATOR, versionResolver, versionIndexInfoResolver);
        final List<OperatorSpecificQueryFactory> operatorFactories =
                CollectionBuilder.<OperatorSpecificQueryFactory>newBuilder(
                    new EqualityWithSpecifiedEmptyValueQueryFactory<Version>(versionIndexInfoResolver, BaseFieldIndexer.NO_VALUE_INDEX_VALUE),
                    relationQueryFactory
                ).asList();
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(SystemSearchConstant.forFixForVersion().getIndexField(), operatorFactories, operandResolver);

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
            return oper.handleEquals(SystemSearchConstant.forFixForVersion().getIndexField(),literals,versionIndexInfoResolver);
        }
        else if (oper.isNegationOperator(operator))
        {
            return oper.handleNotEquals(SystemSearchConstant.forFixForVersion().getIndexField(),literals,versionIndexInfoResolver);
        } else
        {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }
    }
}