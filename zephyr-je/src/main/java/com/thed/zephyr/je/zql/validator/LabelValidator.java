package com.thed.zephyr.je.zql.validator;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.label.LabelManager;
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
import com.thed.zephyr.je.zql.resolver.LabelIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.LabelResolver;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Set;

public class LabelValidator implements ClauseValidator {
	 private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;
	 private final LabelManager labelManager;
	 private final LabelResolver labelResolver;


	 public LabelValidator(JqlOperandResolver operandResolver, I18nHelper.BeanFactory beanFactory, LabelManager labelManager, LabelResolver labelResolver)
	 {
		 this.labelManager = labelManager;
		 this.labelResolver = labelResolver;
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
		       
		        if (operandResolver.isValidOperand(operand) && !OperatorClasses.TEXT_OPERATORS.contains(terminalClause.getOperator())) {
		            // visit every query literal and determine lookup failures
		            final List<QueryLiteral> rawValues = operandResolver.getValues(searcher, operand, terminalClause);
		            for (QueryLiteral rawValue : rawValues)
		            {
		                if (rawValue.getStringValue() != null)
		                {
		                    if (!stringValueExists(searcher, fieldName,rawValue.getStringValue()))
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
		Set<String> labels = labelManager.getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, String.valueOf(longValue));

		if(labels.size() > 0 ) {
			return true;
		} else {
			return false;
		}
	}

	private boolean stringValueExists(ApplicationUser searcher, String fieldName,String stringValue) {
		Set<String> labels = labelManager.getSuggestedLabels(ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser(), null, stringValue);
 		if(labels != null && labels.size() > 0) {
			return true;
		}

		LabelIndexInfoResolver fixVersionIndexInfoResolver = new LabelIndexInfoResolver(labelResolver);
		final List<String> lists = fixVersionIndexInfoResolver.getIndexedValues(stringValue);
		return labelExists(searcher, lists);

	}

	private boolean labelExists(ApplicationUser searcher, List<String> lists) {
		if(lists.size()>0){
			return true;
		}else{
			return false;
		}
	}

	private Long convertToLong(String str) {
		try {
			return Long.parseLong(str);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	SupportedOperatorsValidator getSupportedOperatorsValidator()
	{
		return new SupportedOperatorsValidator(CollectionUtils.union(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY,OperatorClasses.TEXT_OPERATORS));
	}

	I18nHelper getI18n(ApplicationUser user) {
		return beanFactory.getInstance(user);
	}
}
