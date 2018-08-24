package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.validator.DateValidator;
import com.atlassian.jira.timezone.TimeZoneManager;

/**
 * Clause validator for the custom field date time field.
 */
public class CustomFieldDateTimeValidator extends DateValidator {

    public CustomFieldDateTimeValidator(JqlOperandResolver operandResolver, TimeZoneManager timeZoneManager) {
        super(operandResolver, timeZoneManager);
    }
}
