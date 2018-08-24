package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.validator.DateValidator;
import com.atlassian.jira.timezone.TimeZoneManager;

/**
 * Clause validator for the Schedule Date created system field.
 *
 */
public class DateCreatedValidator extends DateValidator
{
    public DateCreatedValidator(final JqlOperandResolver operandResolver, TimeZoneManager timeZoneManager)
    {
        super(operandResolver, timeZoneManager);
    }
}

