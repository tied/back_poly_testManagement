package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.util.NumberIndexValueConverter;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CustomFieldNumberValidator  implements ClauseValidator {
	
	 private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;
	 private final CustomFieldValueManager customFieldValueManager;
	 private final DoubleConverter doubleConverter;

	public CustomFieldNumberValidator(JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory ,
									   CustomFieldValueManager customFieldValueManager,DoubleConverter doubleConverter) {
		 this.supportedOperatorsValidator = getSupportedOperatorsValidator();
		 this.operandResolver = operandResolver;
		 this.beanFactory = beanFactory;
		 level = MessageSet.Level.ERROR;
		 this.customFieldValueManager = customFieldValueManager;
		 this.doubleConverter=doubleConverter;
	 }



	 public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
		 MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
		 if (!errors.hasAnyErrors()) {
            final Operand operand = terminalClause.getOperand();
            final String fieldName = terminalClause.getName();
			 if (operandResolver.isValidOperand(operand)) {
				 final List<QueryLiteral> rawValues = operandResolver.getValues(searcher, operand, terminalClause);
				 if (rawValues != null) {
					 for (QueryLiteral rawValue : rawValues) {
						 if (!longValueExists(rawValue)) {
							 if (operandResolver.isFunctionOperand(rawValue.getSourceOperand())) {
								 errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name.from.function", rawValue.getSourceOperand().getName(), fieldName));
							 } else {
								 errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name", fieldName, String.valueOf(rawValue.getLongValue())));
							 }
						 }
					 }
				 }
			 }
		 }
		 return errors;
	 }

	private boolean longValueExists(QueryLiteral rawValue) {
		NumberIndexValueConverter indexValueConverter = new NumberIndexValueConverter(doubleConverter);
		if (!rawValue.isEmpty() && indexValueConverter.convertToIndexValue(rawValue) == null) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}


	@SuppressWarnings("unchecked")
	SupportedOperatorsValidator getSupportedOperatorsValidator()  {
		 return new SupportedOperatorsValidator(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,OperatorClasses.RELATIONAL_ONLY_OPERATORS));
	}
	 
   I18nHelper getI18n(ApplicationUser user) {
       return beanFactory.getInstance(user);
   }
}
