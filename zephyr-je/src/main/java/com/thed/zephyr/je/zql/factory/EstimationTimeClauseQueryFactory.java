package com.thed.zephyr.je.zql.factory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.query.AbstractTimeTrackingClauseQueryFactory;
import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupport;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupportImpl;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

/**
 * Creates clauses for queries on the execution date field.
 *
 */
public class EstimationTimeClauseQueryFactory extends AbstractTimeTrackingClauseQueryFactory implements ClauseQueryFactory {
    public EstimationTimeClauseQueryFactory(JqlOperandResolver operandResolver, ApplicationProperties applicationProperties) {
        super(SystemSearchConstant.forEstimationTime().getIndexField(), operandResolver, new JqlTimetrackingDurationSupportImpl(ComponentAccessor.getJiraDurationUtils()), applicationProperties);
    }
}