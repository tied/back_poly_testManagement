package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.thed.zephyr.je.model.ExecutionCf;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.util.ApplicationConstants;

import java.util.List;
import java.util.Objects;

public class CustomFieldLargeTextValidator implements ClauseValidator {

	private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;
	 private final CustomFieldValueManager customFieldValueManager;


	 public CustomFieldLargeTextValidator(JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory , CustomFieldValueManager customFieldValueManager) {
		 this.supportedOperatorsValidator = getSupportedOperatorsValidator();
		 this.operandResolver = operandResolver;
		 this.beanFactory = beanFactory;
		 level = MessageSet.Level.ERROR;
		 this.customFieldValueManager = customFieldValueManager;
	 }

	 public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
		 MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
		 if (!errors.hasAnyErrors()) {
		        final Operand operand = terminalClause.getOperand();
		        final String fieldName = terminalClause.getName();
		       
		        if (operandResolver.isValidOperand(operand)) {
		            // visit every query literal and determine lookup failures
		            final List<QueryLiteral> rawValues = operandResolver.getValues(searcher, operand, terminalClause);
		            for (QueryLiteral rawValue : rawValues) {
		                if (rawValue.getStringValue() != null) {
		                    if (!stringValueExists(searcher, fieldName,rawValue.getStringValue())) {
		                        if (operandResolver.isFunctionOperand(rawValue.getSourceOperand())) {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name.from.function", rawValue.getSourceOperand().getName(), fieldName));
		                        } else {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name", fieldName, rawValue.getStringValue()));
		                        }
		                    }
		                }
		            }
		        }
		 }
		 return errors;
	 }

	private boolean stringValueExists(ApplicationUser searcher, String fieldName, String stringValue) {
		List<ExecutionCf> executionsCfList = customFieldValueManager.checkCustomFieldValueExistByTypeAndKey(ApplicationConstants.LARGE_VALUE, stringValue, null);
		if(Objects.nonNull(executionsCfList) && executionsCfList.size() > 0) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	SupportedOperatorsValidator getSupportedOperatorsValidator()  {
		 return new SupportedOperatorsValidator(OperatorClasses.TEXT_OPERATORS);
	}
	 
   I18nHelper getI18n(ApplicationUser user) {
       return beanFactory.getInstance(user);
   }


}
