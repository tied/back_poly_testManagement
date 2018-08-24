package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.util.JqlLocalDateSupport;
import com.atlassian.jira.jql.validator.LocalDateValidator;
import com.atlassian.jira.timezone.TimeZoneManager;

/**
 * Clause validator for the custom field date time field.
 */
public class CustomFieldDateValidator extends LocalDateValidator {

    public CustomFieldDateValidator(JqlOperandResolver operandResolver, JqlLocalDateSupport jqlLocalDateSupport) {
        super(operandResolver, jqlLocalDateSupport);
    }
}
