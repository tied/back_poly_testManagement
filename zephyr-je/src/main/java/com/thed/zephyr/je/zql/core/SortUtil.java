package com.thed.zephyr.je.zql.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.parameters.lucene.sort.MappedSortComparator;
import com.atlassian.jira.issue.search.util.FreeTextVisitor;
import com.atlassian.query.Query;
import com.atlassian.query.order.SearchSort;
import com.atlassian.query.order.SortOrder;
import com.atlassian.jira.issue.search.parameters.lucene.sort.StringSortComparator;

public class SortUtil {
    final static SearchSort DEFAULT_KEY_SORT = new SearchSort(SystemSearchConstant.forSchedule().getJqlClauseNames().getPrimaryName(), SortOrder.DESC);

    
	protected List<SearchSort> getSearchSorts(final Query query, final SearchHandlerManager searchHandlerManager, ApplicationUser searcher) {
		List<SearchSort> sorts;
		// NOTE: when the whole query is null we fall back to the default sorts.
		if (query == null) {
			sorts = Collections.emptyList();
		} else {
			// NOTE: when the order by clause is null we use this condition to
			// force us to use not sorts for our query at all.
			if (query.getOrderByClause() == null) {
				return null;
			}
			sorts = query.getOrderByClause().getSearchSorts();
		}

		// This is a special case where we want to put in the default JIRA sorts
		if (sorts.isEmpty()) {
			// If we have a free text query, then we don't want any sorts so
			// that Lucene's rank will work for us.
			if (query == null || !FreeTextVisitor.containsFreeTextCondition(query.getWhereClause())) {
                sorts = Collections.singletonList(DEFAULT_KEY_SORT);
			}
		}

		return sorts;
	}

	/**
	 * A default implementation that returns a {@link MappedSortComparator} from
	 * {@link #getSorter()}.
	 */
	protected FieldComparatorSource getSortComparatorSource(String documentConstant) {
		SorterFactory sortFactory = new SorterFactory();
		final LuceneFieldSorter sorter = sortFactory.getFieldSorter(documentConstant);
		if (sorter == null) {
			return null;
		} else {
			return new MappedSortComparator(sorter);
		}
	}

	protected List<SortField> getSortFields(boolean sortOrder,
			String documentConstant) {
		final FieldComparatorSource sorter = getSortComparatorSource(documentConstant);
		List<SortField> sortFields = new ArrayList<SortField>();
		if (sorter != null) {
			// lucene needs a field name. In some cases however, we don't have
			// one. as it just caches the
			// ScoreDocComparator for each field (and we can assume these are
			// the same for a given field, we can
			// just put the field name here if it isn't found.
			SortField sortField;
			if(StringUtils.equalsIgnoreCase("summary", documentConstant)) {
				sortField = new SortField("sort_summary", new StringSortComparator(), sortOrder);
			} else if(StringUtils.equalsIgnoreCase("cycle", documentConstant)) {
				sortField = new SortField("sort_cycle", sorter, sortOrder);
			} else if(StringUtils.equalsIgnoreCase("SCHEDULE_DEFECT_ID", documentConstant)) {
				sortField = new SortField("SCHEDULE_DEFECT_KEY", new StringSortComparator(), sortOrder);
			} else if(StringUtils.equalsIgnoreCase("folder", documentConstant)) {
				sortField = new SortField("sort_folder", sorter, sortOrder);
			} else {
				if(documentConstant == "schedule_id") {
					documentConstant = "ORDER_ID";
				}
				sortField = new SortField(documentConstant, sorter,sortOrder);				
			}
			sortFields.add(sortField);
		}
		return sortFields;
	}

	protected boolean getSortOrder(final SearchSort searchSort) {
		boolean order;
		if (searchSort.getOrder() == null) {
			// We need to handle the case where the sort order is null, we will
			// delegate off to the fields
			// default SearchSort for order in this case.
			order = true;
		} else {
			order = searchSort.isReverse();
		}
		return order;
	}
}
