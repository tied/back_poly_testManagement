package com.thed.zephyr.je.config.customfield.searcher;

import static com.atlassian.util.concurrent.Assertions.notNull;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.NumberIndexValueConverter;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;


/**
 * A validator for custom field "Address".
 *
 * @since v4.0
 */
public class TeststepCustomfieldClauseValidator implements ClauseValidator
{
	protected static final Logger log = Logger.getLogger(TeststepCustomfieldClauseValidator.class);

	private final TeststepCustomfieldIndexValuesValidator indexValuesValidator;
    private final SupportedOperatorsValidator supportedOperatorsValidator;
    private final JqlOperandResolver jqlOperandResolver;
    private final I18nHelper.BeanFactory beanFactory;

    /**
     * Old constructor.
     * @param jqlOperandResolver
     * @param indexValueConverter
     * @deprecated Use {@link #NumberCustomFieldValidator(JqlOperandResolver, NumberIndexValueConverter, I18nHelper.BeanFactory)} instead. Since v5.0.
     */
    @Deprecated
	public TeststepCustomfieldClauseValidator(final JqlOperandResolver jqlOperandResolver)
    {
        this(jqlOperandResolver, ComponentAccessor.getI18nHelperFactory());
    }

    public TeststepCustomfieldClauseValidator(final JqlOperandResolver jqlOperandResolver, final I18nHelper.BeanFactory beanFactory)
    {
        this.beanFactory = notNull("beanFactory", beanFactory);
        this.jqlOperandResolver = notNull("jqlOperandResolver", jqlOperandResolver);
        this.supportedOperatorsValidator = getSupportedOperatorsValidator();
        this.indexValuesValidator = getIndexValuesValidator();
    }
    //CLOVER:OFF
    @Override
	public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause)
    {
        MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
        if (!errors.hasAnyErrors())
        {
            errors = indexValuesValidator.validate(searcher, terminalClause);
        }
        return errors;
    }
    ///CLOVER:ON

    TeststepCustomfieldIndexValuesValidator getIndexValuesValidator()
    {
        return new TeststepCustomfieldIndexValuesValidator(jqlOperandResolver)
        {
            @Override
            void addError(final MessageSet messageSet, final ApplicationUser searcher, TerminalClause terminalClause, final QueryLiteral literal)
            {
                String fieldName = terminalClause.getName();
                if (jqlOperandResolver.isFunctionOperand(literal.getSourceOperand()))
                {
                    messageSet.addErrorMessage(getI18n(searcher).getText("jira.jql.clause.invalid.number.value.function", literal.getSourceOperand().getName(), fieldName));
                }
                else
                {
                    messageSet.addErrorMessage(getI18n(searcher).getText("jira.jql.clause.invalid.number.value", fieldName, literal.asString()));
                }
            }
        };
    }

    ///CLOVER:OFF
    SupportedOperatorsValidator getSupportedOperatorsValidator()
    {
        return new SupportedOperatorsValidator(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, OperatorClasses.RELATIONAL_ONLY_OPERATORS);
    }
    ///CLOVER:ON

     ///CLOVER:OFF
    I18nHelper getI18n(ApplicationUser user)
    {
        return beanFactory.getInstance(user);
    }
    ///CLOVER:ON

}
