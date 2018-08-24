package com.thed.zephyr.je.action.enav;

import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.user.ApplicationUser;

public interface ZSearchRenderer {
	String getSearcherId();
	String getSearcherNameKey();
	String getTemplate();
	String getHTML(final ApplicationUser searcher, final FieldValuesHolder fieldValuesHolder, final Map displayParameters);
}
