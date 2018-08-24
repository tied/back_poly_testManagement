package com.thed.zephyr.je.zql.validator;

import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
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
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.JiraUtil;

public class ExecutionStatusValidator  implements ClauseValidator {
	 private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;

	    
	 public ExecutionStatusValidator(JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory)
	 {
		 this.supportedOperatorsValidator = getSupportedOperatorsValidator();
		 this.operandResolver=operandResolver;
		 this.beanFactory=beanFactory;
		 level = MessageSet.Level.ERROR;
	 }

	 public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause)
	 {
		 MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
		 if (!errors.hasAnyErrors())
		 {
		        final Operand operand = terminalClause.getOperand();
		        final String fieldName = terminalClause.getName();

		        if (operandResolver.isValidOperand(operand)) {
		            // visit every query literal and determine lookup failures
		            final List<QueryLiteral> rawValues = operandResolver.getValues(searcher, operand, terminalClause);
		            for (QueryLiteral rawValue : rawValues)
		            {
		                if (rawValue.getStringValue() != null)
		                {
		                    if (!stringValueExists(searcher, rawValue.getStringValue()))
		                    {
		                        if (operandResolver.isFunctionOperand(rawValue.getSourceOperand()))
		                        {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name.from.function", rawValue.getSourceOperand().getName(), fieldName));
		                        }
		                        else
		                        {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name", fieldName, rawValue.getStringValue()));
		                        }
		                    }
		                }
		                else if (rawValue.getLongValue() != null)
		                {
		                    if (!longValueExist(searcher, rawValue.getLongValue()))
		                    {
		                        if (operandResolver.isFunctionOperand(rawValue.getSourceOperand()))
		                        {
		                        	errors.addMessage(this.level, getI18n(searcher).getText("jira.jql.clause.no.value.for.name.from.function", rawValue.getSourceOperand().getName(), fieldName));
		                        }
		                        else
		                        {
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
		 ExecutionStatus execStatus = JiraUtil.getExecutionStatuses().get(longValue.intValue());
		 if(execStatus != null) 
			 return true;
		 return false;
	}

	private boolean stringValueExists(ApplicationUser searcher, String stringValue) {
        Map<Integer, ExecutionStatus> allStatus = JiraUtil.getExecutionStatuses();
        for(Map.Entry<Integer, ExecutionStatus> executionStatus : allStatus.entrySet()) {
			if(StringUtils.equalsIgnoreCase(executionStatus.getValue().getName(), stringValue)) {
				return true;
			}
		}
		return false;
	}

	SupportedOperatorsValidator getSupportedOperatorsValidator(){
		 return new SupportedOperatorsValidator(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY);
	 }
	 
    I18nHelper getI18n(ApplicationUser user) {
        return beanFactory.getInstance(user);
    }
}
