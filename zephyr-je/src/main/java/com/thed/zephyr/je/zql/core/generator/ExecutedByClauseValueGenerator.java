package com.thed.zephyr.je.zql.core.generator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.EmailFormatter;
import org.apache.commons.lang.StringUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.thed.zephyr.je.service.ScheduleManager;

/**
 * Gets all execution status values
 * 
 */
public class ExecutedByClauseValueGenerator implements ClauseValuesGenerator {
	private final ScheduleManager scheduleManager;
	private final EmailFormatter emailFormatter;

	public ExecutedByClauseValueGenerator(ScheduleManager scheduleManager, EmailFormatter emailFormatter) {
		this.scheduleManager = scheduleManager;
		this.emailFormatter=emailFormatter;
	}

	public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {
		Map<String, User> users = scheduleManager.getExecutedByValues(null, null);
		final Set<Result> executedByValues = new LinkedHashSet<Result>();
		for (Map.Entry<String, User> userEntry : users.entrySet()) {
			User user = userEntry.getValue();
			final String lowerCaseConstName = user.getName().toLowerCase();
			if (StringUtils.isBlank(valuePrefix)
					|| lowerCaseConstName.startsWith(valuePrefix.toLowerCase())) {
				final String fullName = user.getDisplayName();
				final String name = user.getName();
				String email = user.getEmailAddress();
				email = emailFormatter.formatEmail(email,searcher);
				if(StringUtils.isBlank(email)) {
					executedByValues.add(new Result(name, new String[]{fullName, " (" + name + ")"}));
				} else {
					executedByValues.add(new Result(name, new String[]{fullName, "- " + email, " (" + name + ")"}));
				}
			}
		}
		return new Results(new ArrayList<ClauseValuesGenerator.Result>(executedByValues));
	}
}
