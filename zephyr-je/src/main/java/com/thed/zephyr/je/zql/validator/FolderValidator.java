package com.thed.zephyr.je.zql.validator;

import java.util.List;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * Validator for folder clause.
 * 
 * @author manjunath
 *
 */
public class FolderValidator  implements ClauseValidator {
	
	 private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;
	 private final FolderManager folderManager;

	    
	 public FolderValidator(JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory , FolderManager folderManager) {
		 this.supportedOperatorsValidator = getSupportedOperatorsValidator();
		 this.operandResolver = operandResolver;
		 this.beanFactory = beanFactory;
		 level = MessageSet.Level.ERROR;
		 this.folderManager = folderManager;
	 }

	 public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
		 MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
		 if (!errors.hasAnyErrors()) {
		        final Operand operand = terminalClause.getOperand();
		        final String fieldName = terminalClause.getName();
		       
		        if (operandResolver.isValidOperand(operand) && !OperatorClasses.TEXT_OPERATORS.contains(terminalClause.getOperator())) {
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
		                } else if (rawValue.getLongValue() != null) {
		                    if (!longValueExist(searcher, rawValue.getLongValue())) {
		                        if (operandResolver.isFunctionOperand(rawValue.getSourceOperand())) {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name.from.function", rawValue.getSourceOperand().getName(), fieldName));
		                        } else {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.id", fieldName, rawValue.getLongValue().toString()));
		                        }
		                    }
		                }
		            }
		        }
		 }
		 return errors;
	 }

	 private boolean longValueExist(ApplicationUser searcher, Long longValue) {
		 if(longValue != null && (longValue.equals(ApplicationConstants.ADHOC_SYSTEM_FOLDER_ID))) {
			 return true;
		 }
		 Folder folder = folderManager.getFolder(longValue);
		 if(folder != null) {
			 return true;
		 } else {
			 return stringValueExists(searcher,"folderName", String.valueOf(longValue));
		 }
	 }

	private boolean stringValueExists(ApplicationUser searcher, String fieldName,String stringValue) {
		List<String> folderValues = folderManager.getValues(SystemSearchConstant.dbFieldName(fieldName),stringValue);
		if(folderValues != null && folderValues.size() > 0) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	SupportedOperatorsValidator getSupportedOperatorsValidator()  {
		 return new SupportedOperatorsValidator(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,OperatorClasses.TEXT_OPERATORS));
	}
	 
    I18nHelper getI18n(ApplicationUser user) {
        return beanFactory.getInstance(user);
    }
}
