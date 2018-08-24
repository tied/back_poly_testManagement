package com.thed.zephyr.je.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.ZAPIValidationService;
import com.thed.zephyr.je.vo.StepResultBean;
import com.thed.zephyr.je.vo.TeststepBean;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

/**
 * ZAPI Validation for Rest API
 * @author niravshah
 *
 */
public class ZAPIValidationServiceImpl implements ZAPIValidationService {
    protected final Logger log = Logger.getLogger(ZAPIValidationServiceImpl.class);
    private static final String TESTSTATUS_LABEL = "Test";
    private static final String STEPSTATUS_LABEL = "Step";
    private static final String STEP_RESULT_ENTITY = "StepResult";

    private JiraAuthenticationContext authContext;
	
	public ZAPIValidationServiceImpl(JiraAuthenticationContext authContext) {
		this.authContext=authContext;
	}
	
	@Override
	public JSONObject validateTestStatus(String testStatus) {
       	JSONObject jsonObject = new JSONObject();
       	try {
       		if(StringUtils.isBlank(testStatus)) {
	       		jsonObject.put("status", 1);
	       		jsonObject.put("message", authContext.getI18nHelper().getText("schedule.status.null.error","status"));
	       		return jsonObject;
       		}
       		if(!JiraUtil.getExecutionStatuses().containsKey(Integer.valueOf(testStatus))) {
	       		jsonObject.put("status", 1);
	       		jsonObject.put("message", authContext.getI18nHelper().getText("schedule.status.invalid.error",TESTSTATUS_LABEL,testStatus));
	       		return jsonObject;
       		}
       	} catch(JSONException e) {
    		log.warn("Error creating JSON Object",e);
    	}
		return null;
	}
	
	
	@Override
	public JSONObject validateStepStatus(String stepStatus,boolean statusNullCheckRequired) {
       	JSONObject jsonObject = new JSONObject();
       	try {
       		if(statusNullCheckRequired) {
	       		if(StringUtils.isBlank(stepStatus)) {
		       		jsonObject.put("status", 1);
	       			jsonObject.put("message", authContext.getI18nHelper().getText("schedule.status.null.error","stepStatus"));
		       		return jsonObject;
	       		}
       		}
       		if(StringUtils.isNotBlank(stepStatus)) {
	       		if(!JiraUtil.getStepExecutionStatuses().containsKey(Integer.valueOf(stepStatus))) {
		       		jsonObject.put("status", 1);
		       		jsonObject.put("message", authContext.getI18nHelper().getText("schedule.status.invalid.error",STEPSTATUS_LABEL,stepStatus));
		    		return jsonObject;
	       		}
       		}
       	} catch(JSONException e) {
    		log.warn("Error creating JSON Object",e);
    	}
		return null;
	}
	
	@Override
	public JSONObject validateStepResultBean(StepResultBean stepResultBean) {
		//ZAPI Validate Step Status
		JSONObject errorJsonObject = new JSONObject();
		try {
			if(stepResultBean == null){
				errorJsonObject.put("status", 1);
				errorJsonObject.put("message",authContext.getI18nHelper().getText("zephyr.common.error.create",STEP_RESULT_ENTITY,"stepResult"));
			}else{
				if(stepResultBean.getExecutionId() == null || stepResultBean.getExecutionId() < 1){
					errorJsonObject.put("status", 2);
					errorJsonObject.put("message",authContext.getI18nHelper().getText("zephyr.common.error.create",STEP_RESULT_ENTITY,"executionId"));
				}
				if(stepResultBean.getStepId() == null || stepResultBean.getStepId() < 1){
					errorJsonObject.put("status", 3);
					errorJsonObject.put("message",authContext.getI18nHelper().getText("zephyr.common.error.create",STEP_RESULT_ENTITY,"stepId"));
				}
			}
			if(errorJsonObject.has("status"))
				return errorJsonObject;
		} catch (JSONException e) {
    		log.warn("Error creating JSON Object",e);
		}
		//ZAPI Validation for StepStatus
		errorJsonObject = validateStepStatus(stepResultBean.getStatus(), false);
		if(errorJsonObject != null) {
			return errorJsonObject;
		}
		return null;
	}

	@Override
	public Map<String, String> validateIssueById(String issueIdStr) {
		Map<String, String> errorMap = new HashMap<String, String>();
    	final I18nHelper i18n = authContext.getI18nHelper();
    	Long issueId = -1L;
    	try {
			issueId = Long.parseLong(issueIdStr);
	      	if(null == issueId || issueId <= 0)
	      		errorMap.put("issueId", i18n.getText("zapi.cycle.invalid.issue.id.error")); 	
	      	else{
	      		MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
	      		if(null == issue)
	      			errorMap.put("message",i18n.getText("zephyr.common.error.invalid","IssueId",String.valueOf(issueId)));
	      	}
		} catch (NumberFormatException nfe) {
			log.warn("Invalid IssueId",nfe);
			errorMap.put("issueId",i18n.getText("zapi.cycle.invalid.issue.id.error"));
		} catch (NullPointerException nfe) {
			log.warn("IssueId is null",nfe);
			errorMap.put("issueId",i18n.getText("zapi.cycle.invalid.issue.id.error"));
		}     	
		return errorMap;
	}
	
	@Override
	public Map<String, String> validateIssueByIdAndType(String issueIdStr) {
		Map<String, String> errorMap = validateIssueById(issueIdStr);
		if(errorMap.size() > 0) {
			return errorMap;
		} 
    	final I18nHelper i18n = authContext.getI18nHelper();
    	Long issueId = -1L;
    	try {
			issueId = Long.parseLong(issueIdStr);
      		MutableIssue issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
            String typeId = JiraUtil.getTestcaseIssueTypeId();
            if(!StringUtils.equalsIgnoreCase(issue.getIssueTypeObject().getId(),typeId)) {
            	errorMap.put("status","1");
            	errorMap.put("message",i18n.getText("zapi.cycle.get.issue.bytype.test.error",issueIdStr,issue.getKey()));
            }
		} catch (NumberFormatException nfe) {
			log.warn("Invalid IssueId",nfe);
			errorMap.put("issueId",i18n.getText("zapi.cycle.invalid.issue.id.error"));
		} catch (NullPointerException nfe) {
			log.warn("IssueId is null",nfe);
			errorMap.put("issueId",i18n.getText("zapi.cycle.invalid.issue.id.error"));
		}     	
		return errorMap;
	}
	
	@Override
	public JSONObject validateTestStepId(Integer stepId,TeststepBean stepBean) {
		JSONObject errorJsonObject = new JSONObject();
		try {
			if(stepId == null) {
					errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.update.ID.required", "id"));
					errorJsonObject.put("errors", new String(""));
					return errorJsonObject;
			} 
		} catch (JSONException e) {
			log.error("Error constructing JSON",e);
		}
		return errorJsonObject;
	}
	
	@Override
	public JSONObject validateIssueAndStepId(Long issueId, Teststep step, Integer id) throws JSONException {
		JSONObject errorJsonObject = new JSONObject();
		if(step == null) {
			errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("view.issues.steps.notfound.error", id));
			errorJsonObject.put("errors", "");
			return errorJsonObject;
		}

		if(step.getIssueId().intValue() != issueId.intValue()) {
			errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("view.issues.steps.mismatch.error",String.valueOf(id),String.valueOf(issueId)));
			errorJsonObject.put("errors", "");
			return errorJsonObject;
		}
		return errorJsonObject;
	}

	@Override
	public JSONObject validateExecutionId(String executionId) {	
		JSONObject errorJsonObject = null;
		try {
			try{
				Integer.parseInt(executionId);				
			}catch (Exception e) {
				errorJsonObject = new JSONObject();
				errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", "executionId"));
			}
		} catch (JSONException e) {
			log.error("Error constructing JSON",e);
		}
		return errorJsonObject;
	}

	@Override
	public JSONObject validateId(Integer id, String name) {
		JSONObject errorJsonObject = null;
		try {			
			if(null == id || id <= 0){
				errorJsonObject = new JSONObject();
				errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", name));
			}			
		} catch (JSONException e) {
			log.error("Error constructing JSON",e);
		}		
		return errorJsonObject;
	}
	
	@Override
	public JSONObject validateEntity(Object object,String name) {
		JSONObject errorJsonObject = null;
		try {			
			if(null == object){
				errorJsonObject = new JSONObject();
				errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", name));
			}			
		} catch (JSONException e) {
			log.error("Error constructing JSON",e);
		}		
		return errorJsonObject;
	}
	
	@Override
	public JSONObject validateEntityStr(String input,String name) {
		JSONObject errorJsonObject = null;
		try {			
			if(StringUtils.isBlank(input)){
				errorJsonObject = new JSONObject();
				errorJsonObject.put("errorMessages", authContext.getI18nHelper().getText("schedule.execute.update.stepresult.invalid.id", name));
			}			
		} catch (JSONException e) {
			log.error("Error constructing JSON",e);
		}		
		return errorJsonObject;
	}
}
