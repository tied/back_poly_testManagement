package com.thed.zephyr.je.action.enav;

import java.io.UnsupportedEncodingException;

import com.atlassian.jira.permission.GlobalPermissionKey;
import webwork.action.ActionContext;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.transport.impl.ActionParamsImpl;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.URLCodec;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.zql.core.AutoCompleteJsonGenerator;
import com.thed.zephyr.util.JiraUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * @author smangal
 *
 */
public class ExecutionNavigatorAction extends JiraWebActionSupport {
	
	VelocityRequestContextFactory velocityRequestContextFactory;
	private AutoCompleteJsonGenerator autocompleteJsonGenerator;  
    private final ZephyrLicenseManager zLicenseManager;
    private final JiraAuthenticationContext authenticationContext;

	/**
	 * 
	 */
	public ExecutionNavigatorAction(VelocityRequestContextFactory velocityRequestContextFactory, 
			final AutoCompleteJsonGenerator autocompleteJsonGenerator,
			ZephyrLicenseManager zLicenseManager,
			JiraAuthenticationContext authenticationContext) {
		this.velocityRequestContextFactory = velocityRequestContextFactory;
		this.autocompleteJsonGenerator=autocompleteJsonGenerator;
		this.zLicenseManager=zLicenseManager;
		this.authenticationContext=authenticationContext;
	}

	@Override
	public String doDefault() throws Exception{
		return super.doDefault();
	}
	
	@Override
	public String doExecute() throws Exception{
		if(authenticationContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI();
        	return getRedirect("/login.jsp?permissionViolation=true&os_destination=" + uri);
        }
		//Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = performLicenseValidation();
	    if(!licVerificationResult.isValid()) {
	    	if(JiraUtil.hasGlobalRights(GlobalPermissionKey.ADMINISTER)){
	        	return getRedirect(licVerificationResult.getForwardURI().toString());
		    }
	    	return ERROR;
	    }

		
		Project currentProject = super.getSelectedProjectObject();
		if(currentProject == null){
			//TODO
			//setSelectedProjectId(???);
		}

		String json = autocompleteJsonGenerator.getVisibleFieldNamesJson(ComponentAccessor.getComponent(JiraAuthenticationContext.class).getLoggedInUser(),getI18nHelper().getLocale());
		ActionContext.getRequest().setAttribute("jqlFieldZ",json);

		String reservedWords = autocompleteJsonGenerator.getJqlReservedWordsJson();
		ActionContext.getRequest().setAttribute("reservedWords",reservedWords);

		String functionZ = autocompleteJsonGenerator.getVisibleFunctionNamesJson(ComponentAccessor.getComponent(JiraAuthenticationContext.class).getLoggedInUser(),getI18nHelper().getLocale());
		ActionContext.getRequest().setAttribute("functionZ",functionZ);
		
		return super.doExecute();
	}
	
	protected ActionParamsImpl getActionParams(){
        /*Jira has it at class level, does it makes sense. Shouldnt it be ThreadLocal?*/
		ActionParamsImpl actionParams = null;
		if (actionParams == null){
            actionParams = new ActionParamsImpl(ActionContext.getParameters());
        }

        return actionParams;
    }
	
	/**
	 * Verifies if License is Valid
	 */
	private ZephyrLicenseVerificationResult performLicenseValidation() {
		ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
	    
	    //license is invalid
	    if( !licVerificationResult.isValid()) {
        	ActionContext.getRequest().setAttribute("errors", licVerificationResult.getGeneralMessage());
		}
	    return licVerificationResult;
	}  
	
    /**
	 * @throws UnsupportedEncodingException 
	 * @returns uri that can be appended to the login page 
	 */
    //TODO: move to common Util
	private String getFWDRequestURI() throws UnsupportedEncodingException {
		HttpServletRequest localRequest = ActionContext.getRequest();
		String uri = localRequest.getServletPath();
		if(localRequest.getQueryString() != null ){
			uri += "?" + localRequest.getQueryString();
		}
		return URLCodec.encode(uri);
	}
}
