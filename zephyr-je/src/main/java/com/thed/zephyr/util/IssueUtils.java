package com.thed.zephyr.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.index.DefectSummaryModel;
import com.thed.zephyr.je.vo.ExecutionDefectBean;

public class IssueUtils {

	/**
	 * @param issue
	 * @return
	 */
	public static Map<String, String> convertDefectToMap(Issue issue) {
		final Map<String, String> scheduleDefectMap = new TreeMap<String, String>();
		scheduleDefectMap.put("key", issue.getKey());
		if(JiraUtil.hasIssueViewPermission(null,issue,ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())) {
			scheduleDefectMap.put("summary", issue.getSummary());
			scheduleDefectMap.put("status", issue.getStatus().getNameTranslation());
			scheduleDefectMap.put("statusId", issue.getStatus().getId());
			if (issue.getResolutionId() != null)
				scheduleDefectMap.put("resolution", issue.getResolution().getName());
			else
				scheduleDefectMap.put("resolution", "");
		} else {
			scheduleDefectMap.put("maskedIssueKey", ApplicationConstants.MASKED_DATA);
			scheduleDefectMap.put("summary", ApplicationConstants.MASKED_DATA);
			scheduleDefectMap.put("status", ApplicationConstants.MASKED_DATA);
			scheduleDefectMap.put("statusId", ApplicationConstants.MASKED_DATA);
			if (issue.getResolutionId() != null)
				scheduleDefectMap.put("resolution", ApplicationConstants.MASKED_DATA);
			else
				scheduleDefectMap.put("resolution", "");
		}
		return scheduleDefectMap;
	}

	/**
	 * Get Masked data
	 * @param issue
	 * @return
	 */
	public static Map<String, String> convertDefectToMapMasked(Issue issue) {
		final Map<String, String> scheduleDefectMap = new TreeMap<String, String>();
		scheduleDefectMap.put("key", issue.getKey());
		scheduleDefectMap.put("maskedIssueKey", ApplicationConstants.MASKED_DATA);
		scheduleDefectMap.put("summary", ApplicationConstants.MASKED_DATA);
		scheduleDefectMap.put("status", ApplicationConstants.MASKED_DATA);
		scheduleDefectMap.put("statusId", ApplicationConstants.MASKED_DATA);
		if (issue.getResolutionId() != null)
			scheduleDefectMap.put("resolution", ApplicationConstants.MASKED_DATA);
		else
			scheduleDefectMap.put("resolution", "");

		return scheduleDefectMap;
	}

	public static ExecutionDefectBean convertIssueToExecutionDefect(String defectKey) {
		ExecutionDefectBean executionDefect = new ExecutionDefectBean();
		Issue defectIssue = ComponentAccessor.getIssueManager().getIssueObject(defectKey);
		if(null != defectIssue) { // Fix for ZFJ-1345
			executionDefect.setDefectId(defectIssue.getId().intValue());
			if(JiraUtil.hasIssueViewPermission(null,defectIssue,ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser())) {
				executionDefect.setDefectKey(defectKey);
				executionDefect.setDefectStatus(defectIssue.getStatus().getNameTranslation());
				executionDefect.setDefectSummary(defectIssue.getSummary());
				if (defectIssue.getResolutionId() != null)
					executionDefect.setDefectResolutionId(defectIssue.getResolution().getName());
				else
					executionDefect.setDefectResolutionId("");
			} else {
				executionDefect.setDefectKey(ApplicationConstants.MASKED_DATA);
				executionDefect.setDefectStatus(ApplicationConstants.MASKED_DATA);
				executionDefect.setDefectSummary(ApplicationConstants.MASKED_DATA);
				if (defectIssue.getResolutionId() != null)
					executionDefect.setDefectResolutionId(ApplicationConstants.MASKED_DATA);
				else
					executionDefect.setDefectResolutionId("");
			}
		}
		return executionDefect;
	}


	public static ExecutionDefectBean maskIssueToExecutionDefect(String defectKey) {
		ExecutionDefectBean executionDefect = new ExecutionDefectBean();
		Issue defectIssue = ComponentAccessor.getIssueManager().getIssueObject(defectKey);
		if(null != defectIssue) {
			executionDefect.setDefectKey(ApplicationConstants.MASKED_DATA);
			executionDefect.setDefectId(defectIssue.getId().intValue());
			executionDefect.setDefectStatus(ApplicationConstants.MASKED_DATA);
			executionDefect.setDefectSummary(ApplicationConstants.MASKED_DATA);
			if(defectIssue.getResolutionId() != null)
				executionDefect.setDefectResolutionId(ApplicationConstants.MASKED_DATA);
			else
				executionDefect.setDefectResolutionId("");
		}
		return executionDefect;
	}

	public static DefectSummaryModel convertIssueToDefectSummaryModel(Map<Integer, Integer> defectMap, List<String> associatedTestIds, Integer defectId, Issue issue) {
		DefectSummaryModel defectSummaryModel = new DefectSummaryModel();
		defectSummaryModel.setDefectId(defectId);
		defectSummaryModel.setDefectKey(issue.getKey());
		defectSummaryModel.setDefectSummary(issue.getSummary());
		defectSummaryModel.setDefectStatus(issue.getStatus().getNameTranslation());
		if(issue.getResolutionId() != null)
			defectSummaryModel.setDefectResolution(issue.getResolution().getName());
		else
			defectSummaryModel.setDefectResolution("");

		defectSummaryModel.setTestCount(defectMap.get(defectId));
		defectSummaryModel.setAssociatedTestIds(associatedTestIds);
		if(null != issue.getPriority() && null != issue.getPriority().getId())
			defectSummaryModel.setPriority(issue.getPriority().getId());
		else
			defectSummaryModel.setPriority("");
		return defectSummaryModel;
	}

	public static JSONObject defectSummaryToJSON(DefectSummaryModel defectSummaryModel) throws JSONException {
		JSONObject  object = new JSONObject();
		object.put("defectId", defectSummaryModel.getDefectId());
		object.put("defectKey", defectSummaryModel.getDefectKey());
		object.put("defectSummary", defectSummaryModel.getDefectSummary());
		object.put("defectStatus", defectSummaryModel.getDefectStatus());
		object.put("defectResolution", defectSummaryModel.getDefectResolution());
		object.put("testCount", defectSummaryModel.getTestCount());
		object.put("associatedTestIds", defectSummaryModel.getAssociatedTestIds());
		return object;
	}

}
