package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.thed.zephyr.je.config.model.ExecutionStatus;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for executionStatus.
 *
 */
public class ExecutionStatusIndexInfoResolver implements IndexInfoResolver<ExecutionStatus>
{
    private final NameResolver<ExecutionStatus> executionStatusResolver;

    public ExecutionStatusIndexInfoResolver(NameResolver<ExecutionStatus> executionStatusResolver)
    {
        this.executionStatusResolver = executionStatusResolver;
    }

    public List<String> getIndexedValues(final String rawValue)
    {
        notNull("rawValue", rawValue);
        List<String> components = executionStatusResolver.getIdsFromName(rawValue);
        if (components.isEmpty())
        {
            final Long componentId = getValueAsLong(rawValue);
            if (componentId != null && executionStatusResolver.idExists(componentId))
            {
                components = Collections.singletonList(rawValue);
            }
        }
        return components;
    }

    public List<String> getIndexedValues(final Long rawValue)
    {
        notNull("rawValue", rawValue);
        if (executionStatusResolver.idExists(rawValue))
        {
            return CollectionBuilder.newBuilder(rawValue.toString()).asList();
        }
        else
        {
            return executionStatusResolver.getIdsFromName(rawValue.toString());
        }
    }

    public String getIndexedValue(final ExecutionStatus executionStatus)
    {
        notNull("executionStatus", executionStatus);
        return getIdAsString(executionStatus);
    }

    
    private String getIdAsString(final ExecutionStatus executionStatus)
    {
        return executionStatus.getId().toString();
    }

    private Long getValueAsLong(final String value)
    {
        try
        {
            return new Long(value);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}