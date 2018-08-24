package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.UserResolver;
import com.atlassian.jira.jql.validator.AbstractUserValidator;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.util.I18nHelper;

/**
 * A Validator for the Executed By field clauses
 *
 */
public class ExecutedByValidator extends AbstractUserValidator implements ClauseValidator
{
    public ExecutedByValidator(UserResolver userResolver, JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory)
    {
        super(userResolver, operandResolver, beanFactory);
    }
}