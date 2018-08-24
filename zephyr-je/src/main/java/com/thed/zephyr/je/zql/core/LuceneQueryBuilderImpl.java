package com.thed.zephyr.je.zql.core;

import com.atlassian.jira.issue.search.ClauseTooComplexSearchException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.query.clause.Clause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * Creates a Lucene Query from a ZQL clause.
 * 
 */
public class LuceneQueryBuilderImpl implements LuceneQueryBuilder {
	private final QueryRegistry queryRegistry;
	private final LuceneQueryModifier luceneQueryModifier;

	public LuceneQueryBuilderImpl(QueryRegistry queryRegistry,LuceneQueryModifier luceneQueryModifier) {
		this.queryRegistry = queryRegistry;
		this.luceneQueryModifier = luceneQueryModifier;
	}

	public Query createLuceneQuery(QueryCreationContext queryCreationContext, Clause clause) throws SearchException {
		final QueryVisitor queryVisitor = createQueryVisitor(queryCreationContext);
		final Query luceneQuery;
		try {
			luceneQuery = queryVisitor.createQuery(clause);
		} catch (QueryVisitor.ZqlTooComplex jqlTooComplex) {
			throw new ClauseTooComplexSearchException(jqlTooComplex.getClause());
		}

		// we need to process the returned query so that it will run in lucene
		// correctly. For instance, we
		// will add positive queries where necessary so that negations work.
		try {
			return luceneQueryModifier.getModifiedQuery(luceneQuery);
		} catch (BooleanQuery.TooManyClauses tooManyClauses) {
			throw new ClauseTooComplexSearchException(clause);
		}
	}

	QueryVisitor createQueryVisitor(QueryCreationContext context) {
		return new QueryVisitor(queryRegistry, context);
	}
}
