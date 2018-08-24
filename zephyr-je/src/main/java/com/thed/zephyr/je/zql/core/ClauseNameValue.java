package com.thed.zephyr.je.zql.core;

import java.util.Set;

import com.atlassian.jira.JiraDataType;
import com.atlassian.query.operator.Operator;

public class ClauseNameValue {
    private final String clauseNameValue;
    private final String customFieldIdClauseName;
    private final boolean autoCompleteable;
    private final boolean orderByable;
    private final boolean searchable;
    private final boolean mustUseNameValue;
    private final Set<Operator> supportedOperators;
    private final JiraDataType supportedType;

    public ClauseNameValue(final String clauseNameValue, final boolean isAutoCompleteable,
            final boolean isOrderByable, final boolean isSearchable, final boolean mustUseNameValue,
            final Set<Operator> supportedOperators, final JiraDataType supportedType)
    {
        this.clauseNameValue = clauseNameValue;
        this.autoCompleteable = isAutoCompleteable;
        this.orderByable = isOrderByable;
        this.searchable = isSearchable;
        this.mustUseNameValue = mustUseNameValue;
        this.supportedOperators = supportedOperators;
        this.supportedType = supportedType;
        this.customFieldIdClauseName = null;
    }

    public ClauseNameValue(final String clauseNameValue, final String customFieldIdClauseName,
            final boolean isAutoCompleteable, final boolean isOrderByable, final boolean isSearchable,
            final boolean mustUseNameValue, final Set<Operator> supportedOperators,
            final JiraDataType supportedType)
    {
        this.clauseNameValue = clauseNameValue;
        this.customFieldIdClauseName = customFieldIdClauseName;
        this.autoCompleteable = isAutoCompleteable;
        this.orderByable = isOrderByable;
        this.searchable = isSearchable;
        this.mustUseNameValue = mustUseNameValue;
        this.supportedOperators = supportedOperators;
        this.supportedType = supportedType;
    }

    public String getClauseNameValue()
    {
        return clauseNameValue;
    }

    public String getCustomFieldIdClauseName()
    {
        return customFieldIdClauseName;
    }

    public boolean isAutoCompleteable()
    {
        return autoCompleteable;
    }

    public boolean isOrderByable()
    {
        return orderByable;
    }

    public boolean isMustUseNameValue()
    {
        return mustUseNameValue;
    }

    public boolean isSearchable()
    {
        return searchable;
    }

    public Set<Operator> getSupportedOperators()
    {
        return supportedOperators;
    }

    public JiraDataType getSupportedType()
    {
        return supportedType;
    }

    @SuppressWarnings ({ "RedundantIfStatement" })
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

        final ClauseNameValue that = (ClauseNameValue) o;

        if (autoCompleteable != that.autoCompleteable)
        {
            return false;
        }
        if (mustUseNameValue != that.mustUseNameValue)
        {
            return false;
        }
        if (orderByable != that.orderByable)
        {
            return false;
        }
        if (searchable != that.searchable)
        {
            return false;
        }
        if (clauseNameValue != null ? !clauseNameValue.equals(that.clauseNameValue) : that.clauseNameValue != null)
        {
            return false;
        }
        if (customFieldIdClauseName != null ? !customFieldIdClauseName.equals(that.customFieldIdClauseName) : that.customFieldIdClauseName != null)
        {
            return false;
        }
        if (supportedOperators != null ? !supportedOperators.equals(that.supportedOperators) : that.supportedOperators != null)
        {
            return false;
        }
        if (supportedType != null ? !supportedType.equals(that.supportedType) : that.supportedType != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = clauseNameValue != null ? clauseNameValue.hashCode() : 0;
        result = 31 * result + (customFieldIdClauseName != null ? customFieldIdClauseName.hashCode() : 0);
        result = 31 * result + (autoCompleteable ? 1 : 0);
        result = 31 * result + (orderByable ? 1 : 0);
        result = 31 * result + (searchable ? 1 : 0);
        result = 31 * result + (mustUseNameValue ? 1 : 0);
        result = 31 * result + (supportedOperators != null ? supportedOperators.hashCode() : 0);
        result = 31 * result + (supportedType != null ? supportedType.hashCode() : 0);
        return result;
    }
}