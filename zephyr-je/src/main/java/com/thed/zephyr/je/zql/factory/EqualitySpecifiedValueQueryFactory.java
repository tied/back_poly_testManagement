package com.thed.zephyr.je.zql.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.AbstractOperatorQueryFactory;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.query.operator.Operator;

/**
 * Used to generate equality lucene queries. When this searches for EMPTY values it will search the index for the
 * provided fieldName with the value that is provdied to represent an empty value.
 *
 */
public class EqualitySpecifiedValueQueryFactory<T> extends AbstractOperatorQueryFactory<T> implements OperatorSpecificQueryFactory
{    
	private static final Logger log = Logger.getLogger(EqualitySpecifiedValueQueryFactory.class);
    private final IndexInfoResolver<T> indexInfoResolver;
    private final String emptyValue;

    public EqualitySpecifiedValueQueryFactory(IndexInfoResolver<T> indexInfoResolver,String emptyValue) {
    	super(indexInfoResolver);
    	this.indexInfoResolver=indexInfoResolver;
    	this.emptyValue=emptyValue;
    }

	@Override
	public QueryFactoryResult createQueryForSingleValue(String fieldName,
			Operator operator, List<QueryLiteral> rawValues) {
        if (Operator.EQUALS.equals(operator))
        {
            return handleEquals(fieldName, getIndexValues(rawValues));
        }
        else if (Operator.NOT_EQUALS.equals(operator))
        {
            return handleNotEquals(fieldName, getIndexValues(rawValues));
        }
        else
        {
            log.debug("Create query for single value was called with operator '" + operator.getDisplayString() + "', this only handles '=' and '!='.");
            return QueryFactoryResult.createFalseResult();
        }
    }

	@Override
	public QueryFactoryResult createQueryForMultipleValues(String fieldName,
			Operator operator, List<QueryLiteral> rawValues) {
        if (Operator.IN.equals(operator))
        {
            return handleEquals(fieldName, getIndexValues(rawValues));
        }
        else if (Operator.NOT_IN.equals(operator))
        {
            return handleNotEquals(fieldName, getIndexValues(rawValues));
        }
        else
        {
            log.debug("Create query for multiple value was called with operator '" + operator.getDisplayString() + "', this only handles 'in'.");
            return QueryFactoryResult.createFalseResult();
        }
    }

	@Override
	public QueryFactoryResult createQueryForEmptyOperand(String fieldName,
			Operator operator) {
        if ((operator == Operator.IS) || (operator == Operator.EQUALS))
        {
            return new QueryFactoryResult(getIsEmptyQuery(fieldName));
        }
        else if ((operator == Operator.IS_NOT) || (operator == Operator.NOT_EQUALS))
        {
            return new QueryFactoryResult(getIsNotEmptyQuery(fieldName));
        }
        else
        {

            log.warn(String.format("Creating an equality query for an empty value for date field '%s' using unsupported operator: '%s', returning "
                    + "a false result (no issues). Supported operators are: '%s','%s', '%s' and '%s'", fieldName, operator,
                    Operator.IS, Operator.EQUALS, Operator.IS_NOT, Operator.NOT_EQUALS));

            return QueryFactoryResult.createFalseResult();
        }
    }

	@Override
	public boolean handlesOperator(Operator operator) {
        return OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY.contains(operator);
	}
   
	
    QueryFactoryResult handleNotEquals(final String fieldName, final List<String> indexValues)
    {
        List<Query> notQueries = new ArrayList<Query>();

        if (indexValues != null)
        {
            for (String indexValue : indexValues)
            {
                // don't bother keeping track of empty literals - empty query gets added later anyway
                if (indexValue != null)
                {
                    notQueries.add(getTermQuery(fieldName, indexValue));
                }
            }
        }
        if (notQueries.isEmpty())
        {
            // if we didn't find non-empty literals, then return the isNotEmpty query
            return new QueryFactoryResult(getIsNotEmptyQuery(fieldName));
        }
        else
        {
            BooleanQuery boolQuery = new BooleanQuery();
            // Because this is a NOT equality query we are generating we always need to explicity exclude the
            // EMPTY results from the query we are generating.
            boolQuery.add(getIsNotEmptyQuery(fieldName), BooleanClause.Occur.MUST);

            // Add all the not queries that were specified by the user.
            for (Query query : notQueries)
            {
                boolQuery.add(query, BooleanClause.Occur.MUST_NOT);
            }

            // We should add the visibility query so that we exclude documents which don't have fieldName indexed.
            return new QueryFactoryResult(boolQuery, false);
        }
    }

    QueryFactoryResult handleEquals(final String fieldName, final List<String> indexValues)
    {
        if (indexValues == null)
        {
            return QueryFactoryResult.createFalseResult();
        }
        if (indexValues.size() == 1)
        {
            final String id = indexValues.get(0);
            return (id == null) ? new QueryFactoryResult(getIsEmptyQuery(fieldName)) : new QueryFactoryResult(getTermQuery(fieldName, id));
        }
        else
        {
            BooleanQuery orQuery = new BooleanQuery();
            for (String id : indexValues)
            {
                if (id != null)
                {
                    orQuery.add(getTermQuery(fieldName, id), BooleanClause.Occur.SHOULD);
                }
                else
                {
                    orQuery.add(getIsEmptyQuery(fieldName), BooleanClause.Occur.SHOULD);
                }
            }

            return new QueryFactoryResult(orQuery);
        }
    }
    

    private Query getIsEmptyQuery(final String fieldName)
    {
        // We are returning a query that will include empties by specifying a MUST_NOT occurrance.
        // We should add the visibility query so that we exclude documents which don't have fieldName indexed.
        return getTermQuery(fieldName, emptyValue);
    }

    private Query getIsNotEmptyQuery(final String fieldName)
    {
        return nonEmptyQuery(fieldName);
    }
    
    Query nonEmptyQuery(final String fieldName)
    {
        return new TermQuery(new Term(DocumentConstants.ISSUE_NON_EMPTY_FIELD_IDS, fieldName));
    }
    
    List<String> getIndexValues(final List<QueryLiteral> rawValues)
    {
        if (rawValues == null || rawValues.isEmpty())
        {
            return Collections.emptyList();
        }
        
        List<String> indexValues = new ArrayList<String>();
        for (QueryLiteral rawValue : rawValues)
        {
            if (rawValue != null)
            {
                final List<String> vals;
                // Turn the raw values into index values
                if (rawValue.getStringValue() != null)
                {
                    vals = indexInfoResolver.getIndexedValues(rawValue.getStringValue());
                }
                else if (rawValue.getLongValue() != null)
                {
                    vals = indexInfoResolver.getIndexedValues(rawValue.getLongValue());
                }
                else
                {
                    // Note: we expect that the IndexInfoResolver result above does not contain nulls, so when we
                    // add null here to the indexValues, this is signifying that an Empty query literal was seen
                    indexValues.add(null);
                    continue;
                }

                if (vals != null && !vals.isEmpty())
                {
                    // Just aggregate all the values together into one big list.
                    indexValues.addAll(vals);
                }
            }
        }
        return indexValues;
    }
}

