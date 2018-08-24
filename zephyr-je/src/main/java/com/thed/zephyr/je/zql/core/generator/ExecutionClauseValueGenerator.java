package com.thed.zephyr.je.zql.core.generator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.lang.StringUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.JiraUtil;

/**
 * Gets all execution status values
 *
 */
public class ExecutionClauseValueGenerator implements ClauseValuesGenerator
{
    public ExecutionClauseValueGenerator()
    {
    }

    public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults)
    {
        final List<ExecutionStatus> executionStatuses = JiraUtil.getExecutionStatusList();

        final Set<Result> executionValues = new LinkedHashSet<Result>();
        for (ExecutionStatus executionStatus : executionStatuses){
            final String lowerCaseConstName = executionStatus.getName().toLowerCase();
            if (StringUtils.isBlank(valuePrefix) ||
                lowerCaseConstName.startsWith(valuePrefix.toLowerCase()))
            {
            	executionValues.add(new Result(executionStatus.getName()));
            }
        }
        return new Results(new ArrayList<Result>(executionValues));
    }
}
