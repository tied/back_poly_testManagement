package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.issue.index.indexers.impl.BaseFieldIndexer;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.helper.CustomFieldOperationHelper;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CustomFieldListIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CustomFieldListResolver;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.NumericUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class CustomFieldOptionClauseQueryFactory implements ClauseQueryFactory  {

	private static final Logger log = Logger.getLogger(CustomFieldOptionClauseQueryFactory.class);

	private final JqlOperandResolver operandResolver;
	private ZephyrCustomFieldManager zephyrCustomFieldManager;


    public CustomFieldOptionClauseQueryFactory(JqlOperandResolver operandResolver, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.operandResolver = operandResolver;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }

	public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
        notNull("queryCreationContext", queryCreationContext);
        CustomFieldListResolver customFieldResolver = new CustomFieldListResolver(zephyrCustomFieldManager,terminalClause);
        final CustomFieldListIndexInfoResolver indexInfoResolver = new CustomFieldListIndexInfoResolver(customFieldResolver);

        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<>();
        CustomField customFieldByName = zephyrCustomFieldManager.getCustomFieldByName(terminalClause.getName());
        operatorFactories.add(new EqualityWithSpecifiedEmptyValueQueryFactory<>(indexInfoResolver, BaseFieldIndexer.NO_VALUE_INDEX_VALUE));
        String fieldName = String.valueOf(customFieldByName.getID());
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(fieldName, operatorFactories, operandResolver);

        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();

        if (OperatorClasses.EMPTY_ONLY_OPERATORS.contains(operator) && !operand.equals(EmptyOperand.EMPTY)) {
            return QueryFactoryResult.createFalseResult();
        }
        OperatorHelper oper = new OperatorHelper();
        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);
        if (literals == null) {
            return QueryFactoryResult.createFalseResult();
        } else if (oper.isEqualityOperator(operator)) {
            CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
            QueryFactoryResult factoryResult = oper.handleEquals(fieldName, literals, indexInfoResolver);
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = customFieldOperationHelper.addToBooleanWithProject(terminalClause.getName());
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(factoryResult.getLuceneQuery(), BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        } else if (oper.isNegationOperator(operator)) {
            if(operator == Operator.NOT_EQUALS || operator == Operator.NOT_IN) {
                List<String> values =  getIndexValues(indexInfoResolver,literals);
                return handleCaseNotEquals(fieldName,values);
            }
            return createQueryForEmptyOperand(fieldName,operator);
        } else {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }
    }

    private QueryFactoryResult createQueryForEmptyOperand(String fieldName, Operator operator) {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        return customFieldOperationHelper.createQueryForEmptyOperand(fieldName,operator);
    }

    /**
     * @param rawValues the raw values to convert
     * @return a list of index values in String form; never null, but may contain null values if empty literals were passed in.
     */
    private List<String> getIndexValues(IndexInfoResolver<?> indexInfoResolver, List<QueryLiteral> rawValues)
    {
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> indexValues = new ArrayList<>();
        for (QueryLiteral rawValue : rawValues) {
            if (rawValue != null) {
                final List<String> vals;
                if (rawValue.getStringValue() != null) {
                    vals = indexInfoResolver.getIndexedValues(rawValue.getStringValue());
                } else if (rawValue.getLongValue() != null) {
                    vals = indexInfoResolver.getIndexedValues(rawValue.getLongValue());
                } else {
                    indexValues.add(null);
                    continue;
                }

                if (vals != null && !vals.isEmpty()){
                    // Just aggregate all the values together into one big list.
                    indexValues.addAll(vals);
                }
            }
        }
        return indexValues;
    }

    private QueryFactoryResult handleCaseNotEquals(final String fieldName, final List<String> indexValues)
    {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        List<Query> notQueries = new ArrayList<Query>();
        if (indexValues != null) {
            for (String indexValue : indexValues)  {
                if (indexValue != null) {
                    notQueries.add(getTermQuery(fieldName, indexValue));
                }
            }
        }
        return customFieldOperationHelper.getNotEqualsQueryFactoryResult(fieldName,notQueries);
    }

    private Query getIsEmptyQuery(String fieldName) {
        return this.getTermQuery(fieldName, ApplicationConstants.NULL_VALUE);
    }

    private Query getIsNotEmptyQuery(String fieldName) {
        final QueryFactoryResult result = new QueryFactoryResult(getTermQuery(fieldName, ApplicationConstants.NULL_VALUE), true);
        final BooleanQuery finalQuery = new BooleanQuery();
        addToBooleanWithMust(result, finalQuery);
        return new QueryFactoryResult(finalQuery).getLuceneQuery();
    }

    private Query getTermQuery(String fieldName, String value) {
        return new TermQuery(new Term(fieldName, value));
    }

    public static void addToBooleanWithMust(final QueryFactoryResult result, final BooleanQuery booleanQuery) {
        addToBooleanWithOccur(result, booleanQuery, BooleanClause.Occur.MUST);
    }

    public static void addToBooleanWithOccur(final QueryFactoryResult result, final BooleanQuery booleanQuery, final BooleanClause.Occur occur) {
        if (result.mustNotOccur()) {
            booleanQuery.add(result.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        } else {
            booleanQuery.add(result.getLuceneQuery(), occur);
        }
    }
}
