package com.thed.zephyr.je.zql.core.generator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelManager;
import com.atlassian.jira.issue.managers.DefaultIssueManager;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;

import java.util.*;

public class LabelClauseValueGenerator implements ClauseValuesGenerator {

	private final LabelManager labelManager;
	private final PermissionManager permissionManager;
    private final I18nHelper.BeanFactory beanFactory;
    private final IssueManager issueManager;

    public LabelClauseValueGenerator(LabelManager labelManager, IssueManager issueManager, PermissionManager permissionManager, I18nHelper.BeanFactory beanFactory)
    {
        this.labelManager = labelManager;
        this.issueManager = issueManager;
    	this.permissionManager=permissionManager;
    	this.beanFactory=beanFactory;
    }

    @SuppressWarnings("unchecked")
	public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults)
    {
        final  Set<Label> labels = new HashSet<Label>();
        List<String> values = new ArrayList<String>();

        values.addAll(labelManager.getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, ""));

         Collections.sort(values, new LocaleSensitiveStringComparator(getLocale(searcher)));

        final Set<Result> labelValues = new LinkedHashSet<Result>();
        for (String value : values)
        {
            if (values.size() == maxNumResults)
            {
                break;
            }
            labelValues.add(new Result(value));
        }
        return new Results(new ArrayList<Result>(labelValues));
    }
	
    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}
