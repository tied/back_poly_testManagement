package com.thed.zephyr.je.zql.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.document.NumberTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.util.CaseFolding;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.util.ApplicationConstants;

public class OperatorHelper {
	
    public QueryFactoryResult handleNotEquals(final String fieldName,final List<QueryLiteral> rawValues,IndexInfoResolver<?> indexInfoResolver)
    {
        return handleCaseNotEquals(fieldName,getIndexValues(indexInfoResolver,rawValues),rawValues);
    }

    public QueryFactoryResult handleEquals(final String fieldName,final List<QueryLiteral> rawValues,IndexInfoResolver<?> indexInfoResolver)
    {
        return handleEquals(fieldName,getIndexValues(indexInfoResolver,rawValues));
    }

    private QueryFactoryResult handleEquals(final String fieldName, final List<String> indexValues)
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
    
    private QueryFactoryResult handleCaseNotEquals(final String fieldName, final List<String> indexValues, List<QueryLiteral> rawValues)
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
           // return new QueryFactoryResult(createPositiveEqualsQuery(fieldName,rawValues), true);
           return new QueryFactoryResult(getIsNotEmptyQuery(fieldName));
        }
        else
        {
            BooleanQuery boolQuery = new BooleanQuery();
            // Because this is a NOT equality query we are generating we always need to explicity exclude the
            // EMPTY results from the query we are generating.
//            boolQuery.add(getIsNotEmptyQuery(fieldName), BooleanClause.Occur.MUST);

            // Add all the not queries that were specified by the user.
            for (Query query : notQueries)
            {
                boolQuery.add(query, BooleanClause.Occur.MUST_NOT);
            }

            return new QueryFactoryResult(boolQuery, false);
        }
    }
    
    public Query createQuery(final String fieldName,final QueryLiteral rawValue)
    {
        if (!rawValue.isEmpty())
        {
            final String value;
            if (rawValue.getStringValue() != null)
            {
                value = CaseFolding.foldString(rawValue.getStringValue(), Locale.ENGLISH);
            }
            else
            {
                value = rawValue.asString();
            }
            return new TermQuery(new Term(fieldName, value));
        }
        else
        {
            return new BooleanQuery();
        }
    }

    

    public Query createRangeQuery(final long min, final long max, final boolean minInclusive, final boolean maxInclusive)
    {
        return new TermRangeQuery(SystemSearchConstants.forIssueKey().getKeyIndexOrderField(), processRangeLong(min), processRangeLong(max),
            minInclusive, maxInclusive);
    }

    public String processRangeLong(final long value)
    {
        return (value < 0) ? null : NumberTools.longToString(value);
    }

    public boolean isNegationOperator(final Operator operator)
    {
        return (operator == Operator.NOT_EQUALS) || (operator == Operator.NOT_IN) || (operator == Operator.IS_NOT);
    }

    public boolean isEqualityOperator(final Operator operator)
    {
        return (operator == Operator.EQUALS) || (operator == Operator.IN) || (operator == Operator.IS);
    }

    public boolean isLikeOperator(final Operator operator)
    {
        return operator == Operator.LIKE;
    }

    public QueryFactoryResult createResult(final List<BooleanClause> clauses)
    {
        if (clauses.isEmpty())
        {
            return QueryFactoryResult.createFalseResult();
        }
        else if (clauses.size() == 1)
        {
            return new QueryFactoryResult(clauses.get(0).getQuery());
        }
        else
        {
            final BooleanQuery query = new BooleanQuery();
            for (final BooleanClause clause : clauses)
            {
                query.add(clause);
            }
            return new QueryFactoryResult(query);
        }
    }

    public RangeQueryGenerator createRangeQueryGenerator(final Operator operator)
    {
        switch (operator)
        {
            case LESS_THAN:
                return new RangeQueryGenerator()
                {
                    public Query get(final long limit)
                    {
                        return createRangeQuery(-1, limit, true, false);
                    }
                };
            case LESS_THAN_EQUALS:
                return new RangeQueryGenerator()
                {
                    public Query get(final long limit)
                    {
                        return createRangeQuery(-1, limit, true, true);
                    }
                };
            case GREATER_THAN:
                return new RangeQueryGenerator()
                {
                    public Query get(final long limit)
                    {
                        return createRangeQuery(limit, -1, false, true);
                    }
                };
            case GREATER_THAN_EQUALS:
                return new RangeQueryGenerator()
                {
                    public Query get(final long limit)
                    {
                        return createRangeQuery(limit, -1, true, true);
                    }
                };
            default:
                throw new IllegalArgumentException("Unsupported Operator:" + operator);
        }
    }

    
    /**
     * @param rawValues the raw values to convert
     * @return a list of index values in String form; never null, but may contain null values if empty literals were passed in.
     */
    public List<String> getIndexValues(IndexInfoResolver<?> indexInfoResolver,List<QueryLiteral> rawValues)
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

    
    /**
     * @param rawValues the raw values to convert
     * @return a list of index values in String form; never null, but may contain null values if empty literals were passed in.
     */
    public String getIndexValue(IndexInfoResolver<?> indexInfoResolver,QueryLiteral rawValue)
    {
        if (rawValue == null || rawValue.isEmpty())
        {
            return "";
        }
        String indexValue=null;
        if (rawValue != null)
        {
            // Turn the raw values into index values
            if (rawValue.getStringValue() != null)
            {
            	indexValue = indexInfoResolver.getIndexedValues(rawValue.getStringValue()).get(0).toString();
            }
            else if (rawValue.getLongValue() != null)
            {
            	indexValue = indexInfoResolver.getIndexedValues(rawValue.getLongValue()).get(0).toString();
            }
        }
        return indexValue;
    }
    
    public interface RangeQueryGenerator {
        Query get(final long limit);
    }
    
    
    public static Query getIsEmptyQuery(final String fieldName) {
    	// We are returning a query that will include empties by specifying a MUST_NOT occurrance.
    	// We should add the visibility query so that we exclude documents which don't have fieldName indexed.
    	final QueryFactoryResult result = new QueryFactoryResult(nonEmptyQuery(fieldName), true);
    	return result.getLuceneQuery();
    }

    public static Query nonEmptyQuery(final String fieldName) {
    	return getTermQuery(fieldName, ApplicationConstants.NULL_VALUE);
    }

    public static TermQuery getTermQuery(String fieldName, String indexValue) {
    	return new TermQuery(new Term(fieldName, indexValue));
    }

    public static Query getIsNotEmptyQuery(final String fieldName) {
        // We are returning a query that will exclude empties by specifying a MUST_NOT occurrance.
        // We should add the visibility query so that we exclude documents which don't have fieldName indexed.
        final QueryFactoryResult result = new QueryFactoryResult(getTermQuery(fieldName, ApplicationConstants.NULL_VALUE), true);
        final BooleanQuery finalQuery = new BooleanQuery();
        addToBooleanWithMust(result, finalQuery);
        return new QueryFactoryResult(finalQuery).getLuceneQuery();
    }
    
    public static void addToBooleanWithMust(final QueryFactoryResult result, final BooleanQuery booleanQuery) {
        addToBooleanWithOccur(result, booleanQuery, BooleanClause.Occur.MUST);
    }

    public static void addToBooleanWithOccur(final QueryFactoryResult result, final BooleanQuery booleanQuery, final BooleanClause.Occur occur) {
        if (result.mustNotOccur())
        {
            booleanQuery.add(result.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        }
        else
        {
            booleanQuery.add(result.getLuceneQuery(), occur);
        }
    }
    
    private Query createPositiveEqualsQuery(final String fieldName,final List<QueryLiteral> rawValues)
    {
    	if (rawValues.size() == 1)
    	{
    		return createQuery(fieldName,rawValues.get(0));
    	}
    	else
    	{
    		final BooleanQuery query = new BooleanQuery();
    		for (final QueryLiteral rawValue : rawValues)
    		{
    			if (!rawValue.isEmpty())
    			{
    				query.add(createQuery(fieldName,rawValue), BooleanClause.Occur.SHOULD);
    			}
    		}
    		return query;
    	}
    }
}
