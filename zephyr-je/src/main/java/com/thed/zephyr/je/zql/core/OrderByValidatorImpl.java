package com.thed.zephyr.je.zql.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.validator.OrderByValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.query.order.OrderBy;
import com.atlassian.query.order.SearchSort;

public class OrderByValidatorImpl implements OrderByValidator {
	private final SearchHandlerManager searchHandlerManager;
	private final I18nHelper.BeanFactory iBeanFactory;

	public OrderByValidatorImpl(SearchHandlerManager searchHandlerManager,
			I18nHelper.BeanFactory iBeanFactory) {
		this.searchHandlerManager = searchHandlerManager;
		this.iBeanFactory = iBeanFactory;
	}

	public MessageSet validate(final ApplicationUser searcher, final OrderBy orderBy) {
		final MessageSet messageSet = new MessageSetImpl();
		final List<SearchSort> searchSorts = orderBy.getSearchSorts();

		final Map<String, String> fieldToJqlNames = new HashMap<String, String>();

		for (SearchSort searchSort : searchSorts) {
			final String clauseName = searchSort.getField();
			final Collection<String> fieldIds = searchHandlerManager
					.getFieldIds(clauseName);

			if (fieldIds.isEmpty()) {
				// There is no visible associated field and therefore no Sorter
				messageSet
						.addErrorMessage("ZQL Field is not orderable");
			} else {
				// Check the field ids and make sure they resolve to a valid field that has a non-null sorter
					// Now that we know the field is cool and has a sorter. Now
					// validate that we can see the clause handler
					// and that there are not more than one sort of the same
					// kind in the sorts
					validateSortUnique(searcher, messageSet, fieldToJqlNames,
							fieldIds, clauseName);
			}
		}
		return messageSet;
	}

	private void validateSortUnique(final ApplicationUser searcher,
			final MessageSet messageSet,
			final Map<String, String> fieldIdToClauseName,
			final Collection<String> fieldIds, final String clause) {
		for (String fieldId : fieldIds) {
			final String origClauseName = fieldIdToClauseName.get(fieldId);
			if (origClauseName != null) {
				if (origClauseName.equals(clause)) {
					messageSet.addErrorMessage("Duplicate Order by Key found");
				} else {
					messageSet.addErrorMessage("Order by field is defined twice");
				}
			} else {
				fieldIdToClauseName.put(fieldId, clause);
			}
		}
	}
}
