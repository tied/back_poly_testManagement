package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.helper.CustomFieldOperationHelper;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CustomFieldIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CustomFieldResolver;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Created by niravshah on 4/20/18.
 */
public class TextClauseQueryFactory implements ClauseQueryFactory {
    static final int BOOST_FACTOR = 9;
    private final JqlOperandResolver operandResolver;
    private CustomFieldValueManager customFieldValueManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;


    public TextClauseQueryFactory(JqlOperandResolver operandResolver, CustomFieldValueManager customFieldValueManager,
                                  ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.operandResolver = operandResolver;
        this.customFieldValueManager=customFieldValueManager;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause) {
        notNull("queryCreationContext", queryCreationContext);
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        CustomFieldResolver customFieldResolver = new CustomFieldResolver(customFieldValueManager);
        final CustomFieldIndexInfoResolver indexInfoResolver = new CustomFieldIndexInfoResolver(customFieldResolver);

        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList<OperatorSpecificQueryFactory>();
        operatorFactories.add(new LikeQueryFactory());
        operatorFactories.add(new EqualityQueryFactory<>(indexInfoResolver));
        CustomField customFieldByName = zephyrCustomFieldManager.getCustomFieldByName(terminalClause.getName());

        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(String.valueOf(customFieldByName.getID()), operatorFactories, operandResolver);
        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();

        OperatorHelper oper = new OperatorHelper();
        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);


        final QueryFactoryResult queryFactoryResult = delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
        if (queryFactoryResult != null && queryFactoryResult.getLuceneQuery() != null) {
            // STRING always gets a boost of 9. JIRA always does that to get accurate result
            queryFactoryResult.getLuceneQuery().setBoost(BOOST_FACTOR);
        }

        if (literals == null) {
            return QueryFactoryResult.createFalseResult();
        } else if (oper.isEqualityOperator(operator)) {
            QueryFactoryResult factoryResult = oper.handleEquals(String.valueOf(customFieldByName.getID()), literals, indexInfoResolver);
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
                return handleCaseNotEquals(String.valueOf(customFieldByName.getID()),values);
            }
            return createQueryForEmptyOperand(String.valueOf(customFieldByName.getID()),operator);
        } else {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = customFieldOperationHelper.addToBooleanWithProject(terminalClause.getName());
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            QueryFactoryResult delegateClauseQueryFactoryQuery = delegateClauseQueryFactory.getQuery(queryCreationContext, terminalClause);
            boolQuery.add(delegateClauseQueryFactoryQuery.getLuceneQuery(), BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        }
    }


    private QueryFactoryResult handleCaseNotEquals(final String fieldName, final List<String> indexValues) {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        List<Query> notQueries = new ArrayList<Query>();
        if (indexValues != null) {
            for (String indexValue : indexValues)  {
                if (indexValue != null) {
                    notQueries.add(customFieldOperationHelper.getTermQuery(fieldName, indexValue));
                }
            }
        }
        return customFieldOperationHelper.getNotEqualsQueryFactoryResult(fieldName, notQueries);
    }

    private QueryFactoryResult createQueryForEmptyOperand(final String fieldName, final Operator operator) {
        CustomFieldOperationHelper customFieldOperationHelper = new CustomFieldOperationHelper();
        QueryFactoryResult queryFactoryResult = customFieldOperationHelper.createQueryForEmptyOperand(fieldName,operator);
        return queryFactoryResult;
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
