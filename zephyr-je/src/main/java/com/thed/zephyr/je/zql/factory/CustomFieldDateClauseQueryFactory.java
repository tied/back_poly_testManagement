package com.thed.zephyr.je.zql.factory;


import com.atlassian.jira.datetime.LocalDate;
import com.atlassian.jira.datetime.LocalDateFactory;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.*;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.util.JqlLocalDateSupport;
import com.atlassian.jira.util.LuceneUtils;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldProject;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.je.zql.helper.CustomFieldOperationHelper;
import com.thed.zephyr.je.zql.helper.OperatorHelper;
import com.thed.zephyr.je.zql.resolver.CustomFieldIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.CustomFieldResolver;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CustomFieldDateClauseQueryFactory implements ClauseQueryFactory {
    private static final Logger log = Logger.getLogger(CustomFieldLargeTextClauseQueryFactory.class);

    private JqlLocalDateSupport jqlLocalDateSupport;
    private JqlOperandResolver operandResolver;
    private CustomFieldValueManager customFieldValueManager;
    private ZephyrCustomFieldManager zephyrCustomFieldManager;

    public CustomFieldDateClauseQueryFactory(CustomFieldValueManager customFieldValueManager,JqlLocalDateSupport jqlLocalDateSupport,
                                             JqlOperandResolver operandResolver, ZephyrCustomFieldManager zephyrCustomFieldManager) {
        this.customFieldValueManager=customFieldValueManager;
        this.jqlLocalDateSupport=jqlLocalDateSupport;
        this.operandResolver=operandResolver;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;
    }

    public QueryFactoryResult getQuery(QueryCreationContext queryCreationContext, TerminalClause terminalClause) {
        CustomFieldResolver customFieldResolver = new CustomFieldResolver(customFieldValueManager);
        final CustomFieldIndexInfoResolver indexInfoResolver = new CustomFieldIndexInfoResolver(customFieldResolver);
        List<OperatorSpecificQueryFactory> operatorFactories = new ArrayList();
        operatorFactories.add(new LocalDateEqualityQueryFactory(jqlLocalDateSupport));
        operatorFactories.add(new LocalDateRelationalQueryFactory(jqlLocalDateSupport));
        CustomField customFieldByName = zephyrCustomFieldManager.getCustomFieldByName(terminalClause.getName());
        ClauseQueryFactory delegateClauseQueryFactory = new GenericClauseQueryFactory(String.valueOf(customFieldByName.getID()), operatorFactories, operandResolver);
        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();
        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);
        OperatorHelper oper = new OperatorHelper();

       if (oper.isNegationOperator(operator)) {
            if(operator == Operator.NOT_EQUALS || operator == Operator.NOT_IN) {
                List<String> values =  getIndexValues(indexInfoResolver,literals);
                return handleCaseNotEquals(String.valueOf(customFieldByName.getID()),values);
            }
            return createQueryForEmptyOperand(String.valueOf(customFieldByName.getID()),terminalClause.getName(),operator);
        } else if(operator == Operator.IN) {
            List<LocalDate> values =  getDateValues(literals);
            return handleIn(String.valueOf(customFieldByName.getID()),values);
       } else if(operator == Operator.IS) {
           return createQueryForEmptyOperand(String.valueOf(customFieldByName.getID()),terminalClause.getName(),operator);
       }  else {
            return delegateClauseQueryFactory.getQuery(queryCreationContext,terminalClause);
        }
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


    private QueryFactoryResult createQueryForEmptyOperand(final String customFieldId, final String fieldName, final Operator operator) {
        if (operator == Operator.IS || operator == Operator.EQUALS) {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = addToBooleanWithProject(fieldName);
            Query isEmptyQuery = getIsEmptyQuery(customFieldId);
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(isEmptyQuery, BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        } else if (operator == Operator.IS_NOT || operator == Operator.NOT_EQUALS) {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = addToBooleanWithProject(fieldName);
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(getIsNotEmptyQuery(customFieldId), BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        }
        else
        {
            log.debug(String.format("Creating an equality query for an empty value for field '%s' using unsupported operator: '%s', returning "
                            + "a false result (no issues). Supported operators are: '%s','%s', '%s' and '%s'", fieldName, operator,
                    Operator.IS, Operator.EQUALS, Operator.IS_NOT, Operator.NOT_EQUALS));

            return QueryFactoryResult.createFalseResult();
        }
    }

    private Query getIsEmptyQuery(String fieldName) {
        return this.getTermQuery(fieldName, LuceneUtils.dateToString(null));
    }

    private Query getIsNotEmptyQuery(String fieldName) {
        final QueryFactoryResult result = new QueryFactoryResult(getTermQuery(fieldName, LuceneUtils.dateToString(null)), true);
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
                    LocalDate localDate = jqlLocalDateSupport.convertToLocalDate(vals.get(0));
                    String dateToString = LuceneUtils.localDateToString(localDate);
                    indexValues.add(dateToString);
                }
            }
        }
        return indexValues;
    }

    /*
    * The IN operator is represented by a series of Equals clauses, ORed together
    */
    private QueryFactoryResult handleIn(final String fieldName, final List<LocalDate> values) {
        if (values.size() == 1) {
            LocalDate localDate = values.get(0);
            final Query query = (localDate == null) ? getIsEmptyQuery(fieldName) : handleEquals(fieldName, localDate);
            return new QueryFactoryResult(query);
        } else {
            final BooleanQuery combined = new BooleanQuery();
            for (final LocalDate value : values) {
                if (value == null) {
                    combined.add(getIsEmptyQuery(fieldName), BooleanClause.Occur.SHOULD);
                } else {
                    combined.add(handleEquals(fieldName, value), BooleanClause.Occur.SHOULD);
                }
            }
            return new QueryFactoryResult(combined);
        }
    }


    /*
     * Equals is represented by the range [date .. date + 1)
     */
    private Query handleEquals(final String fieldName, final LocalDate value) {
        return new TermQuery(new Term(fieldName, jqlLocalDateSupport.getIndexedValue(value)));
    }

    /**
     * @param rawValues the query literals representing the dates
     * @return a list of dates represented by the literals; never null, but may contain null if an empty literal was specified.
     */
    List<LocalDate> getDateValues(List<QueryLiteral> rawValues) {
        //For the time being, assume jqlDateSupport returns 1 to 1
        final List<LocalDate> dates = Lists.newArrayListWithCapacity(rawValues.size());
        for (QueryLiteral rawValue : rawValues) {
            if (rawValue.getLongValue() != null) {
                final LocalDate date = jqlLocalDateSupport.convertToLocalDate(rawValue.getLongValue());
                if (date != null) {
                    dates.add(date);
                }
            } else if (rawValue.getStringValue() != null) {
                final LocalDate date = jqlLocalDateSupport.convertToLocalDate(rawValue.getStringValue());
                if (date != null) {
                    dates.add(date);
                }
            } else {
                dates.add(null);
            }
        }
        return dates;
    }

    private BooleanQuery addToBooleanWithProject(final String fieldName) {
        BooleanQuery boolQuery = new BooleanQuery();
        CustomFieldProject[] customFieldByProjects = zephyrCustomFieldManager.getActiveCustomFieldProjectByName(fieldName);
        if(customFieldByProjects != null && customFieldByProjects.length > 0) {
            for(CustomFieldProject customFieldByProject : customFieldByProjects) {
                final QueryFactoryResult result = new QueryFactoryResult(new TermQuery(new Term("PROJECT_ID", String.valueOf(customFieldByProject.getProjectId()))));
                boolQuery.add(result.getLuceneQuery(), BooleanClause.Occur.SHOULD);
            }
        }
        return boolQuery;
    }
}
