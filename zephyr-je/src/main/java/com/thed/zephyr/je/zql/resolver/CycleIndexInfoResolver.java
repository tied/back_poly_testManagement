package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for executionStatus.
 *
 */
public class CycleIndexInfoResolver implements IndexInfoResolver<Cycle>
{
    private final NameResolver<Cycle> indexInfoResolver;

    public CycleIndexInfoResolver(NameResolver<Cycle> indexInfoResolver)
    {
        this.indexInfoResolver = indexInfoResolver;
    }

    public List<String> getIndexedValues(final String rawValue)
    {
        notNull("rawValue", rawValue);
        if (indexInfoResolver.nameExists(rawValue))
        {
            if(StringUtils.equalsIgnoreCase(rawValue, ApplicationConstants.AD_HOC_CYCLE_NAME) || StringUtils.equalsIgnoreCase(rawValue, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.cycle.adhoc"))) {
                return Collections.singletonList(ApplicationConstants.AD_HOC_CYCLE_NAME);
            }
            return Collections.singletonList(rawValue);
        }
        return null;
    }

    public List<String> getIndexedValues(final Long rawValue)
    {
        notNull("rawValue", rawValue);
        if (indexInfoResolver.idExists(rawValue))
        {
            return CollectionBuilder.newBuilder(rawValue.toString()).asList();
        }
        else
        {
            return getIndexedValues(String.valueOf(rawValue));
        }
    }

    public String getIndexedValue(final Cycle cycle)
    {
        notNull("cycle", cycle);
        return getIdAsString(cycle);
    }

    private String getIdAsString(final Cycle cycle)
    {
        return String.valueOf(cycle.getID());
    }
}