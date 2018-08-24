package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.issue.comparator.VersionComparator;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.index.indexers.impl.BaseFieldIndexer;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.je.zql.helper.CustomFieldOperationHelper;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CustomFieldIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CustomFieldResolver;
import com.thed.zephyr.je.zql.resolver.FixVersionIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.FixVersionResolver;
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
import java.util.Iterator;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class CustomFieldNumberClauseQueryFactory implements ClauseQueryFactory  {
    private JqlOperandResolver operandResolver;
    private DoubleConverter doubleConverter;
    private CustomFieldValueManager customFieldValueManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;

    private static final Logger log = Logger.getLogger(CustomFieldOptionClauseQueryFactory.class);
    final Double emptyIndexValue = Double.MAX_VALUE;

    public CustomFieldNumberClauseQueryFactory(CustomFieldValueManager customFieldValueManager, JqlOperandResolver operandResolver,
                                               DoubleConverter doubleConverter, ZephyrCustomFieldManager zephyrCustomFieldManager)
    {
        this.customFieldValueManager=customFieldValueManager;
        this.doubleConverter=doubleConverter;
        this.operandResolver=operandResolver;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;

    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
        notNull("queryCreationContext", queryCreationContext);
        CustomFieldResolver indexResolver = new CustomFieldResolver(customFieldValueManager);
        CustomFieldIndexInfoResolver indexInfoResolver = new CustomFieldIndexInfoResolver(indexResolver);
        final List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        operatorFactories.add(new NumberEqualityQueryFactory(doubleConverter, Double.MAX_VALUE));
        operatorFactories.add(new NumberRelationalQueryFactory(doubleConverter, Double.MAX_VALUE));

        notNull("queryCreationContext", queryCreationContext);
        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();
        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);
        CustomField customFieldByName = zephyrCustomFieldManager.getCustomFieldByName(terminalClause.getName());
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(String.valueOf(customFieldByName.getID()), operatorFactories, operandResolver);

        OperatorHelper oper = new OperatorHelper();
        if(oper.isNegationOperator(operator)) {
            if(operator == Operator.NOT_EQUALS || operator == Operator.NOT_IN) {
                List<String> values =  getIndexValues(indexInfoResolver,literals);
                return handleCaseNotEquals(String.valueOf(customFieldByName.getID()),values);
            }
            return createQueryForEmptyOperand(String.valueOf(customFieldByName.getID()), terminalClause.getName(),operator);
        } else {
            return delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        }
    }


    private QueryFactoryResult createQueryForEmptyOperand(String customFieldId, String fieldName, Operator operator) {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        if(operator != Operator.IS && operator != Operator.EQUALS) {
            if(operator != Operator.IS_NOT && operator != Operator.NOT_EQUALS) {
                log.debug(String.format("Creating an equality query for an empty value for field '%s' using unsupported operator: '%s', returning a false result (no issues). Supported operators are: '%s','%s', '%s' and '%s'", new Object[]{fieldName, operator, Operator.IS, Operator.EQUALS, Operator.IS_NOT, Operator.NOT_EQUALS}));
                return QueryFactoryResult.createFalseResult();
            } else {
                BooleanQuery boolQuery = new BooleanQuery();
                BooleanQuery projectQuery = customFieldOperationHelper.addToBooleanWithProject(fieldName);
                Query isNotEmptyQuery = getIsNotEmptyQuery(customFieldId);
                if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                    boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
                }
                boolQuery.add(isNotEmptyQuery, BooleanClause.Occur.MUST);
                return new QueryFactoryResult(boolQuery);
            }
        } else {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = customFieldOperationHelper.addToBooleanWithProject(fieldName);
            Query isEmptyQuery = getIsEmptyQuery(customFieldId);
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(isEmptyQuery, BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        }
    }

    private Query getIsEmptyQuery(String fieldName) {
        return this.getTermQuery(fieldName, this.emptyIndexValue);
    }

    private Query getIsNotEmptyQuery(String fieldName) {
        final QueryFactoryResult result = new QueryFactoryResult(getTermQuery(fieldName, this.emptyIndexValue), true);
        final BooleanQuery finalQuery = new BooleanQuery();
        addToBooleanWithMust(result, finalQuery);
        return new QueryFactoryResult(finalQuery).getLuceneQuery();
    }

    private Query getTermQuery(String fieldName, Double value) {
        return new TermQuery(new Term("sort_" + fieldName, NumericUtils.doubleToPrefixCoded(value.doubleValue())));
    }

    private void addToBooleanWithMust(final QueryFactoryResult result, final BooleanQuery booleanQuery) {
        addToBooleanWithOccur(result, booleanQuery, BooleanClause.Occur.MUST);
    }

    private void addToBooleanWithOccur(final QueryFactoryResult result, final BooleanQuery booleanQuery, final BooleanClause.Occur occur) {
        if (result.mustNotOccur()) {
            booleanQuery.add(result.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        } else {
            booleanQuery.add(result.getLuceneQuery(), occur);
        }
    }

    private QueryFactoryResult handleCaseNotEquals(final String fieldName, final List<String> indexValues) {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        List<Query> notQueries = new ArrayList<Query>();
        if (indexValues != null) {
            for (String indexValue : indexValues)  {
                if (indexValue != null) {
                    Double indexDoubleValue = this.doubleConverter.getDouble(indexValue);
                    notQueries.add(getTermQuery(fieldName, indexDoubleValue));
                }
            }
        }
        return customFieldOperationHelper.getNotEqualsQueryFactoryResult(fieldName,notQueries);
    }

    /**
     * @param rawValues the raw values to convert
     * @return a list of index values in String form; never null, but may contain null values if empty literals were passed in.
     */
    private List<String> getIndexValues(IndexInfoResolver<?> indexInfoResolver, List<QueryLiteral> rawValues) {
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
}
