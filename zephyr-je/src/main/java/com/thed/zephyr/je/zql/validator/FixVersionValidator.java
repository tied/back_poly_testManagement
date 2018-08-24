package com.thed.zephyr.je.zql.validator;

import java.util.List;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.Operand;
import com.thed.zephyr.je.zql.resolver.FixVersionIndexInfoResolver;
import com.thed.zephyr.je.zql.resolver.FixVersionResolver;

public class FixVersionValidator  implements ClauseValidator {
	 private final SupportedOperatorsValidator supportedOperatorsValidator;
	 private final JqlOperandResolver operandResolver;
	 private final I18nHelper.BeanFactory beanFactory;
	 private final MessageSet.Level level;
	 private final VersionManager versionManager;
	 private final PermissionManager permissionManager;
	 private final FixVersionResolver fixVersionResolver;
	 private final JiraAuthenticationContext authContext;
	    
	 public FixVersionValidator(JqlOperandResolver operandResolver, 
			 I18nHelper.BeanFactory beanFactory,VersionManager versionManager,
			 FixVersionResolver fixVersionResolver,PermissionManager permissionManager,
			 JiraAuthenticationContext authContext)
	 {
		 this.supportedOperatorsValidator = getSupportedOperatorsValidator();
		 this.operandResolver=operandResolver;
		 this.beanFactory=beanFactory;
		 level = MessageSet.Level.ERROR;
		 this.versionManager=versionManager;
		 this.fixVersionResolver=fixVersionResolver;
		 this.permissionManager=permissionManager;
		 this.authContext=authContext;
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
		 if(longValue != null && (longValue.intValue() == ApplicationConstants.UNSCHEDULED_VERSION_ID)) {
			 return true;
		 }
		 Version version = versionManager.getVersion(longValue);
		 if(version != null) {
			 return true;
		 } else {
			 return stringValueExists(searcher,"VERSION",String.valueOf(longValue));
		 }
	}

	private boolean stringValueExists(ApplicationUser searcher, String fieldName,String stringValue) {
		//If Adhoc its true.we dont nee to lookup against database
		if(StringUtils.equalsIgnoreCase(stringValue, authContext.getI18nHelper().getText("zephyr.je.version.unscheduled"))) {
			return true;
		}
		FixVersionIndexInfoResolver fixVersionIndexInfoResolver = new FixVersionIndexInfoResolver(fixVersionResolver);
        final List<String> ids = fixVersionIndexInfoResolver.getIndexedValues(stringValue);
        return versionExists(searcher, ids);
	}
	
    boolean versionExists(final ApplicationUser searcher, final List<String> ids)
    {
        for (String sid : ids)
        {
            Long id = convertToLong(sid);
            if (id != null)
            {
                final Version version = versionManager.getVersion(id);
                if (version != null && permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, version.getProjectObject(), searcher))
                {
                    return true;
                }
            }
        }
        return false;
    }

	private SupportedOperatorsValidator getSupportedOperatorsValidator()
	 {
        return new SupportedOperatorsValidator(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY, OperatorClasses.RELATIONAL_ONLY_OPERATORS);
	 }
	 
	private I18nHelper getI18n(ApplicationUser user) {
        return beanFactory.getInstance(user);
    }
    
    private Long convertToLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
