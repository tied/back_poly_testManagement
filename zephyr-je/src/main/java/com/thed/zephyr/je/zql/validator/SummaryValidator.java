package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.validator.FreeTextFieldValidator;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

public class SummaryValidator  extends FreeTextFieldValidator { 

	public SummaryValidator(JqlOperandResolver operandResolver)
    {
        super(SystemSearchConstant.forTestSummary().getIndexField(), operandResolver);
    }
}
