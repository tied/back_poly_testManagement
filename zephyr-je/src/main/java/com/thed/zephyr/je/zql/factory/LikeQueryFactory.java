package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.issue.search.util.TextTermEscaper.escape;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.atlassian.jira.issue.index.JiraAnalyzer;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.OperatorSpecificQueryFactory;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.operator.Operator;

/**
 * A factory for creating a Query for the {@link com.atlassian.query.operator.Operator#LIKE equals operator}.
 *
 */
public class LikeQueryFactory implements OperatorSpecificQueryFactory
{
    private static final Logger log = Logger.getLogger(LikeQueryFactory.class);
    public static final Analyzer ANALYZER_FOR_SEARCHING = JiraAnalyzer.ANALYZER_FOR_SEARCHING;

    private final boolean usesMainIndex;

    public LikeQueryFactory()
    {
        this.usesMainIndex = true;
    }

    public LikeQueryFactory(boolean usesMainIndex)
    {
        this.usesMainIndex = usesMainIndex;
    }

    public QueryFactoryResult createQueryForSingleValue(final String fieldName, final Operator operator, final List<QueryLiteral> rawValues)
    {
        if (operator != Operator.LIKE && operator != Operator.NOT_LIKE)
        {
            log.debug(String.format("Operator '%s' is not a LIKE operator.", operator.getDisplayString()));
            return QueryFactoryResult.createFalseResult();
        }

        if (rawValues == null)
        {
            return QueryFactoryResult.createFalseResult();
        }

        return createResult(fieldName, rawValues, operator, usesMainIndex);
    }

    public QueryFactoryResult createResult(final String fieldName, final List<QueryLiteral> rawValues, final Operator operator, final boolean handleEmpty)
    {
        final List<Query> queries = getQueries(fieldName, rawValues);
        if (queries == null || queries.isEmpty())
        {
            return QueryFactoryResult.createFalseResult();
        }

        BooleanQuery fullQuery = new BooleanQuery();
        boolean hasEmpty = false;

        if (queries.size() == 1)
        {
            if (queries.get(0) == null && handleEmpty)
            {
                return createQueryForEmptyOperand(fieldName, operator);
            }
            else
            {
                fullQuery.add(queries.get(0), operator == Operator.NOT_LIKE ? BooleanClause.Occur.MUST_NOT : BooleanClause.Occur.MUST);
            }
        }
        else
        {
            BooleanQuery subQuery = new BooleanQuery();
            for (Query query : queries)
            {
                if (query == null)
                {
                    hasEmpty = true;
                }
                else
                {
                    subQuery.add(query, operator == Operator.NOT_LIKE ? BooleanClause.Occur.MUST_NOT: BooleanClause.Occur.SHOULD);
                }
            }
            if (handleEmpty && hasEmpty)
            {
                subQuery.add(createQueryForEmptyOperand(fieldName, operator).getLuceneQuery(), operator == Operator.NOT_LIKE ? BooleanClause.Occur.MUST: BooleanClause.Occur.SHOULD);
            }
            fullQuery.add(subQuery, BooleanClause.Occur.MUST);
        }

        return new QueryFactoryResult(fullQuery);
    }

    private List<Query> getQueries(String fieldName, List<QueryLiteral> rawValues)
    {
        final QueryParser parser = new QueryParser(Version.LUCENE_30, fieldName, ANALYZER_FOR_SEARCHING);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        final List<Query> queries = new ArrayList<Query>();
        for (QueryLiteral rawValue : rawValues)
        {
            if (rawValue.isEmpty())
            {
                queries.add(null);
            }
            else if (!StringUtils.isBlank(rawValue.asString()))
            {
                final Query query;
                try
                {
                    final String value = getEscapedValueFromRawValues(rawValue);
                    query = parser.parse(value);
                }
                catch (final ParseException e)
                {
                    log.debug(String.format("Unable to parse the text '%s' for field '%s'.", rawValue.asString(), fieldName));
                    return null;
                }
                catch (final RuntimeException e)
                {
                    // JRA-27018  FuzzyQuery throws IllegalArgumentException instead of ParseException
                    log.debug(String.format("Unable to parse the text '%s' for field '%s'.", rawValue.asString(), fieldName));
                    return null;
                }
                queries.add(query);
            }
        }
        return queries;
    }

    public QueryFactoryResult createQueryForEmptyOperand(final String fieldName, final Operator operator)
    {
        log.debug(String.format("Create query for empty operand was called with operator '%s', this only handles '=', '!=', 'is' and 'not is'.", operator.getDisplayString()));
        return QueryFactoryResult.createFalseResult();
    }

    private String getEscapedValueFromRawValues(final QueryLiteral rawValue)
    {
        if (rawValue.isEmpty())
        {
            return null;
        }
        final String value = rawValue.asString();

        // NOTE: we need this so that we do not allow users to search a different field by specifying 'field:val'
        // we only want them to search the field they have specified via the JQL.
        return escape(value);
    }

    public QueryFactoryResult createQueryForMultipleValues(final String fieldName, final Operator operator, final List<QueryLiteral> rawValues)
    {
        log.debug("LIKE clauses do not support multi value operands.");
        return QueryFactoryResult.createFalseResult();
    }

    public boolean handlesOperator(final Operator operator)
    {
        return OperatorClasses.TEXT_OPERATORS.contains(operator);
    }
}

