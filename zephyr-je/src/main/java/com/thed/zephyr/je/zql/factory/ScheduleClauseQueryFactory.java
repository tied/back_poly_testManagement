package com.thed.zephyr.je.zql.factory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.NumericUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.query.IssueIdClauseQueryFactory;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

/**
 * A {@link com.atlassian.jira.jql.query.ClauseQueryFactory} for the "Schedule Id" ZQL clause.
 *
 */
public class ScheduleClauseQueryFactory implements ClauseQueryFactory
{
    private static final Logger log = Logger.getLogger(IssueIdClauseQueryFactory.class);

    private final JqlOperandResolver operandResolver;
    private final ScheduleManager scheduleManager;

    public ScheduleClauseQueryFactory(final JqlOperandResolver operandResolver,ScheduleManager scheduleManager)
    {
        this.scheduleManager = notNull("scheduleManager", scheduleManager);
        this.operandResolver = notNull("operandResolver", operandResolver);
    }

    public QueryFactoryResult getQuery(final QueryCreationContext queryCreationContext, final TerminalClause terminalClause)
    {
        notNull("queryCreationContext", queryCreationContext);
        final Operand operand = terminalClause.getOperand();
        final Operator operator = terminalClause.getOperator();

        if (OperatorClasses.EMPTY_ONLY_OPERATORS.contains(operator) && !operand.equals(EmptyOperand.EMPTY))
        {
            return QueryFactoryResult.createFalseResult();
        }

        final List<QueryLiteral> literals = operandResolver.getValues(queryCreationContext, operand, terminalClause);

        if (literals == null)
        {
            log.warn(String.format("Unable to find operand values from operand '%s' for clause '%s'.", operand.getDisplayString(),
                terminalClause.getName()));
            return QueryFactoryResult.createFalseResult();
        }
        else if (isEqualityOperator(operator))
        {
            return handleEquals(literals);
        }
        else if (isNegationOperator(operator))
        {
            return handleNotEquals(literals);
        }
        else if (OperatorClasses.RELATIONAL_ONLY_OPERATORS.contains(operator))
        {
            if (operandResolver.isListOperand(operand))
            {
                log.warn(String.format("Tried to use list operand '%s' with relational operator '%s' in clause '%s'.", operand.getDisplayString(),
                    operator.getDisplayString(), terminalClause.getName()));
                return QueryFactoryResult.createFalseResult();
            }
            else
            {
                return handleRelational(queryCreationContext.getUser(), queryCreationContext.isSecurityOverriden(), operator, literals,
                    terminalClause);
            }
        }
        else
        {
            log.warn(String.format("The '%s' clause does not support the %s operator.", terminalClause.getName(), operator));
            return QueryFactoryResult.createFalseResult();
        }
    }

    private QueryFactoryResult handleRelational(final ApplicationUser user, final boolean overrideSecurity, final Operator operator, final List<QueryLiteral> literals, final TerminalClause clause)
    {
        return handleRelational(user, overrideSecurity, literals, clause, createRangeQueryGenerator(operator));
    }

    private QueryFactoryResult handleRelational(final ApplicationUser user, final boolean overrideSecurity, final List<QueryLiteral> literals, final TerminalClause clause, final RangeQueryGenerator rangeQueryGenerator)
    {
        final List<BooleanClause> clauses = new LinkedList<BooleanClause>();
        for (final QueryLiteral literal : literals)
        {
            if (literal.isEmpty())
            {
                log.warn(String.format("Encountered EMPTY literal from operand '%s' for operator '%s' on clause '%s'. Ignoring.",
                    clause.getOperand().getDisplayString(), clause.getOperator().getDisplayString(), clause.getName()));
            }

            final List<Schedule> schedules;
            if (literal.getLongValue() != null)
            {
                final Schedule schedule = scheduleManager.getSchedule(literal.getLongValue().intValue());
                if (schedule != null)
                {
                	schedules = Collections.singletonList(schedule);
                }
                else
                {
                	schedules = Collections.emptyList();
                }
            }
            else if (literal.getStringValue() != null)
            {
                final Schedule schedule = scheduleManager.getSchedule(Integer.valueOf(literal.getStringValue()));
                schedules = Collections.singletonList(schedule);
            }
            else
            {
                log.warn(String.format("Encountered weird literal from operand '%s' for operator '%s' on clause '%s'. Ignoring.",
                    clause.getOperand().getDisplayString(), clause.getOperator().getDisplayString(), clause.getName()));
                schedules = Collections.emptyList();
            }

            for (final Schedule schedule : schedules)
            {
                final BooleanQuery subQuery = new BooleanQuery();
                subQuery.add(rangeQueryGenerator.get(schedule.getID()), BooleanClause.Occur.MUST);
             //   subQuery.add(createProjectQuery(schedule.getProjectId()), BooleanClause.Occur.MUST);
                clauses.add(new BooleanClause(subQuery, BooleanClause.Occur.SHOULD));
            }
        }

        return createResult(clauses);
    }

    private QueryFactoryResult handleNotEquals(final List<QueryLiteral> rawValues)
    {
        return new QueryFactoryResult(createPositiveEqualsQuery(rawValues), true);
    }

    private QueryFactoryResult handleEquals(final List<QueryLiteral> rawValues)
    {
        return new QueryFactoryResult(createPositiveEqualsQuery(rawValues));
    }

    private Query createPositiveEqualsQuery(final List<QueryLiteral> rawValues)
    {
        if (rawValues.size() == 1)
        {
            return createQuery(rawValues.get(0));
        }
        else
        {
            final BooleanQuery query = new BooleanQuery();
            for (final QueryLiteral rawValue : rawValues)
            {
                if (!rawValue.isEmpty())
                {
                    query.add(createQuery(rawValue), BooleanClause.Occur.SHOULD);
                }
            }
            return query;
        }
    }

    private static Query createQuery(final QueryLiteral rawValue)
    {
        if (!rawValue.isEmpty())
        {
            final String fieldName;
            final String value;
            if (rawValue.getStringValue() != null)
            {
                fieldName = SystemSearchConstant.forSchedule().getIndexField();
                value =  rawValue.getStringValue(); //CaseFolding.foldString(rawValue.getStringValue(), Locale.ENGLISH);
            }
            else
            {
                fieldName = SystemSearchConstant.forSchedule().getIndexField();
                value = rawValue.asString();
            }
            return new TermQuery(new Term(fieldName, value));
        } else {
            return new BooleanQuery();
        }
    }

    private static Query createProjectQuery(final Long projectId)
    {
        return new TermQuery(new Term(SystemSearchConstant.forProject().getIndexField(), projectId.toString()));
    }

    private static Query createRangeQuery(final long min, final long max, final boolean minInclusive, final boolean maxInclusive) {
       return new TermRangeQuery(DocumentConstants.ISSUE_KEY_NUM_PART_RANGE, processRangeLong(min), processMaxRangeLong(max),
            minInclusive, maxInclusive);
    }

    private static String processRangeLong(final long value)
    {
        return (value < 0) ? null : NumberTools.longToString(value);
    }
    
    private static String processMaxRangeLong(final long value)
    {
        return (value < 0) ? "?" : NumberTools.longToString(value);
    }

    private boolean isNegationOperator(final Operator operator)
    {
        return (operator == Operator.NOT_EQUALS) || (operator == Operator.NOT_IN) || (operator == Operator.IS_NOT);
    }

    private boolean isEqualityOperator(final Operator operator)
    {
        return (operator == Operator.EQUALS) || (operator == Operator.IN) || (operator == Operator.IS);
    }

    private static QueryFactoryResult createResult(final List<BooleanClause> clauses)
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

    private static RangeQueryGenerator createRangeQueryGenerator(final Operator operator)
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

    private static interface RangeQueryGenerator
    {
        Query get(final long limit);
    }
}
