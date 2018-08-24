package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.Collections;
import java.util.List;

import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.util.collect.CollectionBuilder;

/**
 * Index resolver that can find the index values for executionStatus.
 *
 */
public class FixVersionIndexInfoResolver implements IndexInfoResolver<Version>
{
    private final NameResolver<Version> fixVersionResolver;

    public FixVersionIndexInfoResolver(NameResolver<Version> fixVersionResolver)
    {
        this.fixVersionResolver = fixVersionResolver;
    }

    public List<String> getIndexedValues(final String rawValue)
    {
        notNull("rawValue", rawValue);
        List<String> components = fixVersionResolver.getIdsFromName(rawValue);
        if (components.isEmpty())
        {
            final Long componentId = getValueAsLong(rawValue);
            if (componentId != null && fixVersionResolver.idExists(componentId))
            {
                components = Collections.singletonList(rawValue);
            }
        }
        return components;
    }

    public List<String> getIndexedValues(final Long rawValue)
    {
        notNull("rawValue", rawValue);
        if (fixVersionResolver.idExists(rawValue))
        {
            return CollectionBuilder.newBuilder(rawValue.toString()).asList();
        }
        else
        {
            return fixVersionResolver.getIdsFromName(rawValue.toString());
        }
    }

    public String getIndexedValue(final Version version)
    {
        notNull("version", version);
        return getIdAsString(version);
    }

    private String getIdAsString(final Version version)
    {
        return String.valueOf(version.getId());
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