package com.thed.zephyr.je.zql.core;

import static com.atlassian.jira.util.Predicates.allOf;
import static com.atlassian.jira.util.collect.CollectionUtil.copyAsImmutableList;
import static com.atlassian.jira.util.collect.CollectionUtil.filter;
import static com.atlassian.jira.util.collect.CollectionUtil.transform;
import static com.atlassian.jira.util.dbc.Assertions.notBlank;
import static java.util.Collections.unmodifiableCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CachedReference;
import com.atlassian.cache.Supplier;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.user.ApplicationUser;

import com.thed.zephyr.je.model.CustomField;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.SearchableField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.ClauseInformation;
import com.atlassian.jira.util.CaseFolding;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.Predicate;
import com.atlassian.jira.util.collect.IdentitySet;
import com.atlassian.util.concurrent.ResettableLazyReference;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * Default ZFJ implementation of
 * {@link com.thed.zephyr.je.zql.core.SearchHandlerManager}.
 *
 */
@ThreadSafe
public class SearchHandlerManagerImpl implements SearchHandlerManager {
	private static final Logger log = Logger.getLogger(SearchHandlerManagerImpl.class);

	private final ZephyrClauseHandlerFactory zephyrClauseHandlerFactory;
	private final QueryCache queryCache;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;
	private final CachedReference<Helper> helperResettableZFJLazyReference;



	public SearchHandlerManagerImpl(ZephyrClauseHandlerFactory zephyrClauseHandlerFactory, QueryCache queryCache,
									ZephyrCustomFieldManager zephyrCustomFieldManager,CacheManager cacheManager) {
 		this.queryCache = queryCache;
 		this.zephyrClauseHandlerFactory = zephyrClauseHandlerFactory;
		this.zephyrCustomFieldManager = zephyrCustomFieldManager;
		this.helperResettableZFJLazyReference = cacheManager.getCachedReference(SearchHandlerManagerImpl.class, "helperResettableZFJLazyReference",
			new Supplier<Helper>()
			{
				@Override
				public Helper get()
				{
					return createHelper();
				}
			});
 	}

	@Override
	public Collection<ClauseHandler> getClauseHandler(final ApplicationUser user,
			final String zqlClauseName) {
		Collection<ClauseHandler> clauseHandler = queryCache.getClauseHandlers(
				user, zqlClauseName);
		if (clauseHandler == null) {
			List<ClauseHandler> filteredHandlers = new ArrayList<ClauseHandler>();
			Collection<ClauseHandler> unfilteredHandlers = getClauseHandler(zqlClauseName);
				for (ClauseHandler handler : unfilteredHandlers) {
					if (handler.getPermissionHandler().hasPermissionToUseClause(
							user)) {
						filteredHandlers.add(handler);
					}
				}
			clauseHandler = unmodifiableCollection(filteredHandlers);
			queryCache.setClauseHandlers(user, zqlClauseName, clauseHandler);
		}

		return clauseHandler;
	}

	@Override
	public Collection<ClauseHandler> getClauseHandler(final String zqlClauseName) {
		notBlank("zqlClauseName", zqlClauseName);
		return unmodifiableCollection(getHelper().getSearchHandler(
				zqlClauseName));
	}

	@Override
	public Collection<ClauseHandler> getVisibleClauseHandlers(
			final ApplicationUser searcher) {
		Set<ClauseHandler> visibleClauseHandlers = new HashSet<ClauseHandler>();
		final Collection<ClauseHandler> clauseHandlers = getHelper()
				.getSearchHandlers();
		for (ClauseHandler clauseHandler : clauseHandlers) {
			if (clauseHandler.getPermissionHandler().hasPermissionToUseClause(
					searcher)) {
				if(!SystemSearchConstant.externallyInVisibleField().containsKey(clauseHandler.getInformation().getJqlClauseNames().getPrimaryName())) {
					visibleClauseHandlers.add(clauseHandler);
				}
			}
		}
		return visibleClauseHandlers;
	}

	private Helper getHelper() {
		return helperResettableZFJLazyReference.get();
	}

	private Helper createHelper() {

		// We must process all the system fields first to ensure that we don't
		// overwrite custom fields with
		// the system fields.
		final SearchHandlerIndexer indexer = new SearchHandlerIndexer();

		// Process all the system clause handlers, the ZQL clause elements 
		indexer.indexSystemClauseHandlers(zephyrClauseHandlerFactory.getZQLClauseSearchHandlers());

		final CustomField[] customFields = zephyrCustomFieldManager.getAllCustomFieldsByEntityType(ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
		if(customFields != null) {
			Collection<SearchHandler> searchHandlers = new ArrayList<>();
			for (final CustomField field : customFields) {
				SearchHandler searchHandler = getClauseHandlerForCustomField(field);
				if (searchHandler != null) {
					searchHandlers.add(searchHandler);
				}
			}
			indexer.indexCustomFieldClauseHandlers(searchHandlers);
		}

		return new Helper(indexer);
	}

	@Override
	public void refresh() {
		helperResettableZFJLazyReference.reset();
	}

	private static <T> Collection<T> returnNullAsEmpty(
			final Collection<T> collection) {
		if (collection == null) {
			return Collections.emptyList();
		} else {
			return collection;
		}
	}

	/**
	 * The delegate used by the manager to implement its functionality in a
	 * thread safe way.
	 * 
	 */
	@ThreadSafe
	static class Helper {
		/**
		 * ClauseName -> ClauseHandler.
		 */
		private final Map<String, List<ClauseHandler>> handlerIndex;

		/**
		 * ClauseName -> SearcherRegistration.
		 */
		private final Map<String, List<SearchHandler.SearcherRegistration>> searcherClauseNameIndex;

		/**
		 * All JIRA's searcher groups.
		 */

		public Helper(final SearchHandlerIndexer indexer) {
			handlerIndex = indexer.createHandlerIndex();
			searcherClauseNameIndex = indexer.createSearcherJqlNameIndex();
		}

		public Collection<ClauseHandler> getSearchHandler(final String jqlName) {
			return returnNullAsEmpty(handlerIndex.get(CaseFolding
					.foldString(jqlName)));
		}

		public Collection<ClauseHandler> getSearchHandlers() {
			final Set<ClauseHandler> allHandlers = new HashSet<>();
			final Collection<List<ClauseHandler>> handlersList = handlerIndex
					.values();
			for (final List<ClauseHandler> clauseHandlers : handlersList) {
				allHandlers.addAll(clauseHandlers);
			}
			return allHandlers;
		}
	}

	/**
	 * Class that is used by the manager to build its state from
	 * {@link com.atlassian.jira.issue.search.SearchHandler}s.
	 * 
	 */
	@NotThreadSafe
	private static class SearchHandlerIndexer {
		private final Set<String> systemClauses = new HashSet<String>();
		private final Map<String, Set<ClauseHandler>> handlerIndex = new HashMap<String, Set<ClauseHandler>>();
		private final Map<String, Set<SearchHandler.SearcherRegistration>> searcherClauseNameIndex = new LinkedHashMap<String, Set<SearchHandler.SearcherRegistration>>();

		SearchHandlerIndexer() {
		}

		public void indexSystemClauseHandlers(
				final Collection<SearchHandler> searchHandlers) {
			for (final SearchHandler searchHandler : searchHandlers) {
				indexClauseHandlers(null,
						searchHandler.getClauseRegistrations(), true);
			}
		}

		public void indexCustomFieldClauseHandlers(
				final Collection<SearchHandler> searchHandlers) {
			for (final SearchHandler searchHandler : searchHandlers) {
				indexClauseHandlers(null,
						searchHandler.getClauseRegistrations(), false);
			}
		}

		private void indexClauseHandlers(
				final SearchableField field,
				final Collection<? extends SearchHandler.ClauseRegistration> clauseHandlers,
				final boolean system) {
			for (final SearchHandler.ClauseRegistration clauseHandler : clauseHandlers) {
				indexClauseHandlerByZqlName(field, clauseHandler, system);
			}
		}

		Map<String, List<ClauseNames>> createFieldToClauseNamesIndex() {
			final Map<String, List<ClauseNames>> fieldToClauseNames = new HashMap<String, List<ClauseNames>>();
			for (final Set<ClauseHandler> handlers : handlerIndex.values()) {
				for (final ClauseHandler handler : handlers) {
					final ClauseInformation information = handler
							.getInformation();
					if (information.getFieldId() != null) {
						List<ClauseNames> names = fieldToClauseNames
								.get(information.getFieldId());
						if (names == null) {
							names = new ArrayList<ClauseNames>();
							fieldToClauseNames.put(information.getFieldId(),
									names);
						}

						names.add(information.getJqlClauseNames());
					}
				}
			}

			// Much to Dylan's disgust, we make a copy to make it safe.
			final Map<String, List<ClauseNames>> returnMe = new HashMap<String, List<ClauseNames>>();
			for (final Map.Entry<String, List<ClauseNames>> entry : fieldToClauseNames
					.entrySet()) {
				returnMe.put(entry.getKey(),
						Collections.unmodifiableList(entry.getValue()));
			}

			return Collections.unmodifiableMap(returnMe);
		}

		Map<String, List<ClauseHandler>> createHandlerIndex() {
			final Map<String, List<ClauseHandler>> tmpHandlerIndex = new HashMap<String, List<ClauseHandler>>();
			for (final Map.Entry<String, Set<ClauseHandler>> entry : handlerIndex
					.entrySet()) {
				tmpHandlerIndex.put(entry.getKey(),
						copyAsImmutableList(entry.getValue()));
			}
			return Collections.unmodifiableMap(tmpHandlerIndex);
		}

		Map<String, List<SearchHandler.SearcherRegistration>> createSearcherJqlNameIndex() {
			final HashMap<String, List<SearchHandler.SearcherRegistration>> tmpHandlerIndex = new HashMap<String, List<SearchHandler.SearcherRegistration>>();
			for (final Map.Entry<String, Set<SearchHandler.SearcherRegistration>> entry : searcherClauseNameIndex
					.entrySet()) {
				tmpHandlerIndex.put(entry.getKey(),
						copyAsImmutableList(entry.getValue()));
			}
			return Collections.unmodifiableMap(tmpHandlerIndex);
		}

		private void indexClauseHandlerByZqlName(final Field field,
				final SearchHandler.ClauseRegistration registration,
				final boolean system) {
			final Set<String> names = getClauseNames.get(
					registration.getHandler()).getJqlFieldNames();
			for (String name : names) {
				// We always want to look for a match in lowercase since that is
				// how we cache it
				name = CaseFolding.foldString(name);
				// Do we already have a system clause of that name registered.
				if (systemClauses.contains(name)) {
					if (system) {
						if (field != null) {
							throw new RuntimeException(
									String.format(
											"Two system clauses are trying to register against the same ZQL name. New Field = '%s', Jql Name = '%s'.",
											field.getName(), name));
						} else {
							throw new RuntimeException(
									String.format(
											"Two system clauses are trying to register against the same ZQL name. Clause with Jql Name = '%s'.",
											name));
						}
					} else {
						final CustomFieldType type = ((com.atlassian.jira.issue.fields.CustomField) field)
								.getCustomFieldType();
						final String typeName = (type != null) ? type.getName()
								: "Unknown Type";
						log.warn(String
								.format("A custom field '%s (%s)' is trying to register a clause handler against a system clause with name '%s'. Ignoring request.",
										field.getName(), typeName, name));
					}
				} else {
					if (system) {
						systemClauses.add(name);
					}

					register(name,registration);
				}
			}
		}

		private void register(String name, SearchHandler.ClauseRegistration registration) {
			Set<ClauseHandler> currentHandlers = handlerIndex.get(name);
			if (currentHandlers == null) {
				currentHandlers = IdentitySet.newListOrderedSet();
				currentHandlers.add(registration.getHandler());
				handlerIndex.put(name, currentHandlers);
			} else {
				currentHandlers.add(registration.getHandler());
			}
		}
	}

    public Collection<String> getFieldIds(final ApplicationUser searcher, final String zqlClauseName)
    {
        final Predicate<ClauseHandler> predicate = allOf(hasFieldId, new PermissionToUse(searcher));
        return transform(filter(getHelper().getSearchHandler(zqlClauseName), predicate), getFieldId);
    }

    public Collection<String> getFieldIds(final String zqlClauseName)
    {
        return transform(filter(getHelper().getSearchHandler(zqlClauseName), hasFieldId), getFieldId);
    }
    
    static final Function<ClauseHandler, String> getFieldId = new Function<ClauseHandler, String>()
    {
        @Override
        public String get(final ClauseHandler handler)
        {
            return handler.getInformation().getFieldId();
        }
    };
    
	static class PermissionToUse implements Predicate<ClauseHandler> {
		private final ApplicationUser user;

		PermissionToUse(final ApplicationUser user) {
			this.user = user;
		}

		public boolean evaluate(final ClauseHandler clauseHandler) {
			return clauseHandler.getPermissionHandler()
					.hasPermissionToUseClause(user);
		}
	}

    static final Predicate<ClauseHandler> hasFieldId = new Predicate<ClauseHandler>()
    {
        @Override
        public boolean evaluate(final ClauseHandler handler)
        {
            return getFieldId.get(handler) != null;
        }
    };
	
	static final Function<ClauseHandler, ClauseNames> getClauseNames = new Function<ClauseHandler, ClauseNames>() {
		@Override
		public ClauseNames get(final ClauseHandler clauseHandler) {
			return clauseHandler.getInformation().getJqlClauseNames();
		}
	};

	private SearchHandler getClauseHandlerForCustomField(CustomField customField) {
		//Collection<ClauseHandler> clauseHandlers = queryCache.getClauseHandlers(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), customField.getName());
		if(Objects.nonNull(customField)) {
            return zephyrClauseHandlerFactory.addClauseHandlerForCustomFieldType(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()),customField.getID(), customField.getName());
        }
		return null;
	}
}