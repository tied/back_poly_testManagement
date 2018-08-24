package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.*;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.jql.resolver.NameResolver;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.JiraUtil;

import javax.annotation.Nonnull;

/**
 * Resolves Execution Status ids from their names.
 *
 */
public class ExecutionStatusResolver implements NameResolver<ExecutionStatus>{

    public ExecutionStatusResolver(){
    }

    public List<String> getIdsFromName(final String key){
        notNull("key", key);
        Map<Integer, ExecutionStatus> allStatus = JiraUtil.getExecutionStatuses();
        for(Map.Entry<Integer, ExecutionStatus> executionStatus : allStatus.entrySet()) {
			if(StringUtils.equalsIgnoreCase(executionStatus.getValue().getName(), key)) {
	            return Collections.singletonList(executionStatus.getKey().toString());
			}
		}
        return Collections.emptyList();
    }

    public boolean nameExists(final String key){
        notNull("key", key);
		Map<Integer, ExecutionStatus> allStatus = JiraUtil.getExecutionStatuses();
		for(Map.Entry<Integer, ExecutionStatus> executionStatus : allStatus.entrySet()) {
			if(StringUtils.equalsIgnoreCase(executionStatus.getValue().getName(), key)) {
				return true;
			}
		}
		return false;
    }

    public boolean idExists(final Long id) {
        notNull("id", id);
        return JiraUtil.getExecutionStatuses().containsKey(id.intValue());
    }

    public ExecutionStatus get(@Nonnull final Long id) {
        return JiraUtil.getExecutionStatuses().get(id.intValue());
    }

	@Override
	public Collection<ExecutionStatus> getAll() {
        return new ArrayList<ExecutionStatus>();
	}
}

