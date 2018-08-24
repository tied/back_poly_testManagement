package com.thed.zephyr.je.zql.core;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.context.QueryContext;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;
import com.atlassian.jira.security.RequestCacheKeys;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * The implementation for the QueryCache
 * 
 * The cache is stored in a common request cache, so caches are shared amongst
 * instances.
 * 
 */
public class QueryCacheImpl implements QueryCache {
    public static final String ZQL_CLAUSE_HANDLER_CACHE = "zfj.zql.clause.handler.cache";

	
	public Boolean getDoesQueryFitFilterFormCache(final User searcher,
			final Query query) {
		return getFitCache().get(new QueryCacheKey(searcher, query));
	}

	public void setDoesQueryFitFilterFormCache(final User searcher,
			final Query query, final boolean doesItFit) {
		getFitCache().put(new QueryCacheKey(searcher, query), doesItFit);
	}

	public QueryContext getQueryContextCache(final User searcher,
			final Query query) {
		return getQueryCache().get(new QueryCacheKey(searcher, query));
	}

	public void setQueryContextCache(final User searcher, final Query query,
			final QueryContext queryContext) {
		getQueryCache().put(new QueryCacheKey(searcher, query), queryContext);
	}

	public QueryContext getSimpleQueryContextCache(final User searcher,
			final Query query) {
		return getExplicitQueryCache().get(new QueryCacheKey(searcher, query));
	}

	public void setSimpleQueryContextCache(final User searcher,
			final Query query, final QueryContext queryContext) {
		getExplicitQueryCache().put(new QueryCacheKey(searcher, query),
				queryContext);
	}

	@Override
	public Collection<ClauseHandler> getClauseHandlers(ApplicationUser searcher,
			String zqlClauseName) {
		return getClauseHandlerCache().get(
				new QueryCacheClauseHandlerKey(searcher, zqlClauseName));
	}

	@Override
	public void setClauseHandlers(ApplicationUser searcher, String zqlClauseName,
			Collection<ClauseHandler> clauseHandlers) {
		getClauseHandlerCache().put(
				new QueryCacheClauseHandlerKey(searcher, zqlClauseName),
				clauseHandlers);
	}

	@Override
	public List<QueryLiteral> getValues(QueryCreationContext context,
			Operand operand, TerminalClause zqlClause) {
		return getValueCache().get(
				new QueryCacheLiteralsKey(context, operand, zqlClause));
	}

	@Override
	public void setValues(QueryCreationContext context, Operand operand,
			TerminalClause zqlClause, List<QueryLiteral> values) {
		getValueCache().put(
				new QueryCacheLiteralsKey(context, operand, zqlClause), values);
	}

	// /CLOVER:OFF
	Map<QueryCacheKey, QueryContext> getExplicitQueryCache() {
		return getCache(RequestCacheKeys.SIMPLE_QUERY_CONTEXT_CACHE);
	}

	Map<QueryCacheKey, QueryContext> getQueryCache() {
		return getCache(RequestCacheKeys.QUERY_CONTEXT_CACHE);
	}

	Map<QueryCacheKey, Boolean> getFitCache() {
		return getCache(RequestCacheKeys.QUERY_DOES_IT_FIT_NAVIGATOR_CACHE);
	}

	Map<QueryCacheClauseHandlerKey, Collection<ClauseHandler>> getClauseHandlerCache() {
		return getCache(ZQL_CLAUSE_HANDLER_CACHE);
	}

	Map<QueryCacheLiteralsKey, List<QueryLiteral>> getValueCache() {
		return getCache(RequestCacheKeys.QUERY_LITERALS_CACHE);
	}

	static <K, V> Map<K, V> getCache(final String key) {
		final Map<String, Object> requestCache = JiraAuthenticationContextImpl
				.getRequestCache();
		@SuppressWarnings("unchecked")
		Map<K, V> result = (Map<K, V>) requestCache.get(key);
		if (result == null) {
			result = new HashMap<K, V>();
			requestCache.put(key, result);
		}
		return result;
	}

	// /CLOVER:ON

	/**
	 * A a key used for caching on Query and user pairs.
	 * 
	 * @since v4.0
	 */
	static class QueryCacheKey {
		private final User searcher;
		private final Query query;

		public QueryCacheKey(final User searcher, final Query query) {
			this.searcher = searcher;
			this.query = notNull("query", query);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if ((o == null) || (getClass() != o.getClass())) {
				return false;
			}

			final QueryCacheKey that = (QueryCacheKey) o;

			if (!query.equals(that.query)) {
				return false;
			}
			if (searcher != null ? !searcher.equals(that.searcher)
					: that.searcher != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = searcher != null ? searcher.hashCode() : 0;
			result = 31 * result + query.hashCode();
			return result;
		}
	}

	/**
	 * A a key used for caching on zqlClauseName and user pairs.
	 * 
	 */
	static class QueryCacheClauseHandlerKey {
		private final ApplicationUser searcher;
		private final String zqlClauseName;

		public QueryCacheClauseHandlerKey(final ApplicationUser searcher,
				final String zqlClauseName) {
			this.searcher = searcher;
			this.zqlClauseName = notNull("zqlClauseName", zqlClauseName);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if ((o == null) || (getClass() != o.getClass())) {
				return false;
			}

			final QueryCacheClauseHandlerKey that = (QueryCacheClauseHandlerKey) o;

			if (!zqlClauseName.equals(that.zqlClauseName)) {
				return false;
			}
			if (searcher != null ? !searcher.equals(that.searcher)
					: that.searcher != null) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = searcher != null ? searcher.hashCode() : 0;
			result = 31 * result + zqlClauseName.hashCode();
			return result;
		}
	}

	/**
	 * A a key used for caching on Context, operand and TerminalClause triplets.
	 * 
	 * @since v4.4.5
	 */
	static class QueryCacheLiteralsKey {
		private final QueryCreationContext context;
		private final Operand operand;
		private final TerminalClause zqlClause;

		public QueryCacheLiteralsKey(final QueryCreationContext context,
				final Operand operand, final TerminalClause zqlClause) {
			this.context = notNull("context", context);
			this.operand = notNull("operand", operand);
			this.zqlClause = notNull("zqlClause", zqlClause);
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if ((o == null) || (getClass() != o.getClass())) {
				return false;
			}

			final QueryCacheLiteralsKey that = (QueryCacheLiteralsKey) o;

			if (!zqlClause.equals(that.zqlClause)) {
				return false;
			}
			if (!operand.equals(that.operand)) {
				return false;
			}
			if (!context.equals(that.context)) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = context.hashCode();
			result = 11 * result + operand.hashCode();
			result = 31 * result + zqlClause.hashCode();
			return result;
		}
	}
}
