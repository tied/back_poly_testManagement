package com.thed.zephyr.je.config.customfield.searcher;

import com.atlassian.jira.issue.customfields.converters.StringConverter;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.util.IndexValueConverter;

/**
 * Converts a query literal into a number index representation.
 *
 * @since v4.0
 */
public class TeststepCustomfieldIndexValueConverter implements IndexValueConverter
{
    private final StringConverter stringConverter;

    public TeststepCustomfieldIndexValueConverter(StringConverter stringConverter)
    {
        this.stringConverter = stringConverter;
    }

    @Override
	public String convertToIndexValue(final QueryLiteral rawValue)
    {
        if (rawValue.isEmpty())
        {
            return null;
        }
        
        try
        {
            return stringConverter.getString(rawValue.getStringValue());
        }
        catch (FieldValidationException e)
        {
            return null;
        }
    }
}
