package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.validator.DateValidator;
import com.atlassian.jira.timezone.TimeZoneManager;

/**
 * Clause validator for the Execution Date system field.
 *
 */
public class ExecutionDateValidator extends DateValidator
{
    public ExecutionDateValidator(final JqlOperandResolver operandResolver, TimeZoneManager timeZoneManager)
    {
        super(operandResolver, timeZoneManager);
    }
}

