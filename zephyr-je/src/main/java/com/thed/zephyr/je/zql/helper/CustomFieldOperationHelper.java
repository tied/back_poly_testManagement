package com.thed.zephyr.je.zql.helper;

import com.atlassian.jira.jql.query.QueryFactoryResult;
import com.atlassian.query.operator.Operator;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldProject;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.log4j.Logger;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by niravshah on 4/20/18.
 */
public class CustomFieldOperationHelper {
    private static final Logger log = Logger.getLogger(CustomFieldOperationHelper.class);

    public CustomFieldOperationHelper() {

    }

    public QueryFactoryResult getNotEqualsQueryFactoryResult(String fieldName, List<Query> notQueries) {
        if (notQueries.isEmpty()) {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = addToBooleanWithProject(fieldName);
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(this.getIsNotTextEmptyQuery(fieldName), BooleanClause.Occur.MUST);
            return new QueryFactoryResult(boolQuery);
        }  else {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = addToBooleanWithProject(fieldName);
            if(projectQuery.getClauses() != null && projectQuery.getClauses().length > 0) {
                boolQuery.add(new BooleanClause(projectQuery, BooleanClause.Occur.MUST));
            }
            boolQuery.add(this.getIsNotTextEmptyQuery(fieldName), BooleanClause.Occur.MUST);
            for (Query query : notQueries) {
                boolQuery.add(query, BooleanClause.Occur.MUST_NOT);
            }
            return new QueryFactoryResult(boolQuery, false);
        }
    }

    public QueryFactoryResult createQueryForEmptyOperand(final String fieldName, final Operator operator) {
        if (operator == Operator.IS || operator == Operator.EQUALS) {
            BooleanQuery boolQuery = new BooleanQuery();
            BooleanQuery projectQuery = addToBooleanWithProject(fieldName);
            Query isEmptyQuery = getIsTextEmptyQuery(fieldName);
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
            boolQuery.add(getIsNotTextEmptyQuery(fieldName), BooleanClause.Occur.MUST);
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


    public BooleanQuery addToBooleanWithProject(final String fieldName) {
        ZephyrCustomFieldManager zephyrCustomFieldManager = (ZephyrCustomFieldManager) ZephyrComponentAccessor.getInstance().getComponent("zephyrcf-manager");
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

    public Query getIsTextEmptyQuery(String fieldName) {
        return this.getTermQuery(fieldName, ApplicationConstants.NULL_VALUE);
    }

    public Query getIsNotTextEmptyQuery(String fieldName) {
        final QueryFactoryResult result = new QueryFactoryResult(getTermQuery(fieldName, ApplicationConstants.NULL_VALUE), true);
        final BooleanQuery finalQuery = new BooleanQuery();
        addToBooleanWithMust(result, finalQuery);
        return new QueryFactoryResult(finalQuery).getLuceneQuery();
    }


    public Query getTermQuery(String fieldName, String value) {
        return new TermQuery(new Term(fieldName, value));
    }

    public void addToBooleanWithMust(final QueryFactoryResult result, final BooleanQuery booleanQuery) {
        addToBooleanWithOccur(result, booleanQuery, BooleanClause.Occur.MUST);
    }

    public void addToBooleanWithOccur(final QueryFactoryResult result, final BooleanQuery booleanQuery, final BooleanClause.Occur occur) {
        if (result.mustNotOccur()) {
            booleanQuery.add(result.getLuceneQuery(), BooleanClause.Occur.MUST_NOT);
        } else {
            booleanQuery.add(result.getLuceneQuery(), occur);
        }
    }
}
