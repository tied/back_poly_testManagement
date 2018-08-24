package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import java.util.List;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.google.common.collect.Lists;
import com.atlassian.jira.issue.Issue;

/**
 * Resolves index info with a lucene field using the id of the domain object T to get the
 * indexed values from a NameResolver&lt;T&gt; .
 *
 */
public class IssueConstantInfoResolver<T> implements IndexInfoResolver<Issue>
{
    private final NameResolver<Issue> resolver;

    /**
     * @param resolver         the name resolver to look up the id if necessary.
     */
    public IssueConstantInfoResolver(IssueResolver issueResolver)
    {
        this.resolver = notNull("resolver", issueResolver);
    }

    public List<String> getIndexedValues(final String singleValueOperand)
    {
        notNull("singleValueOperand", singleValueOperand);
        // our id is our index value

        final List<String> list = resolver.getIdsFromName(singleValueOperand);
        if (list.isEmpty())
        {
            // Since we could not find the value by name check to see if we can try by id
            Long valueAsLong = getValueAsLong(singleValueOperand);
            if (valueAsLong != null && resolver.idExists(valueAsLong))
            {
                return Lists.newArrayList(singleValueOperand);
            }
        }
        return list;
    }

    public List<String> getIndexedValues(final Long singleValueOperand)
    {
        notNull("singleValueOperand", singleValueOperand);
        if (resolver.idExists(singleValueOperand))
        {
            return Lists.newArrayList(singleValueOperand.toString());
        }
        else
        {
            return resolver.getIdsFromName(singleValueOperand.toString());
        }
    }

    public String getIndexedValue(final Issue indexedObject)
    {
        notNull("indexedObject", indexedObject);
        return String.valueOf(indexedObject.getId());
    }

    private Long getValueAsLong(final String singleValueOperand)
    {
        try
        {
            return new Long(singleValueOperand);
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
