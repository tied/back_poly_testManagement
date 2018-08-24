package com.thed.zephyr.je.service;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.vo.StepResultBean;
import com.thed.zephyr.je.vo.TeststepBean;

import java.util.Map;


public interface ZAPIValidationService {
	JSONObject validateTestStatus(String testStatus);
	JSONObject validateStepStatus(String stepStatus,boolean statusNullCheckRequired);
	JSONObject validateStepResultBean(StepResultBean stepResultBean);
	Map<String, String> validateIssueById(String issueId);
	Map<String, String> validateIssueByIdAndType(String issueIdStr);
	JSONObject validateTestStepId(Integer stepId, TeststepBean stepBean);
	JSONObject validateIssueAndStepId(Long issueId, Teststep step, Integer id) throws JSONException;
	JSONObject validateExecutionId(String executionId);
	JSONObject validateId(Integer id, String name);
	JSONObject validateEntity(Object object,String name);
	JSONObject validateEntityStr(String defectIdOrKey, String string);
}
