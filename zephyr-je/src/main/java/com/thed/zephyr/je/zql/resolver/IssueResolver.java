package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.jql.resolver.NameResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Resolves Project objects and ids from their names.
 *
 */
public class IssueResolver implements NameResolver<Issue>
{
    private final IssueManager issueManager;

    public IssueResolver(final IssueManager issueManager)
    {
        this.issueManager = notNull("issueManager", issueManager);
    }

    public List<String> getIdsFromName(final String key)
    {
        notNull("key", key);

        Issue issue = issueManager.getIssueObject(key);
       
        if (issue != null) {
            return Collections.singletonList(issue.getId().toString());
        } else {
            return Collections.emptyList();
        }
    }

    public boolean nameExists(final String key)
    {
        notNull("key", key);

        Issue issue = issueManager.getIssueObject(key);
        return issue != null;
    }

    public boolean idExists(final Long id)
    {
        notNull("id", id);
        final Issue issue = issueManager.getIssueObject(id);
        return issue != null;
    }

    public Issue get(final Long id)
    {
        return issueManager.getIssueObject(id);
    }

    ///CLOVER:OFF
    public Collection<Issue> getAll()
    {
        return new ArrayList<Issue>();
    }
    ///CLOVER:ON

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final IssueResolver that = (IssueResolver) o;

        if (!issueManager.equals(that.issueManager))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return issueManager.hashCode();
    }
}

