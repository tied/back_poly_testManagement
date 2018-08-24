package com.thed.zephyr.je.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.util.IssueUtils;

public class StepResultResourceHelper {
   protected final Logger log = Logger.getLogger(StepResultResourceHelper.class);
	private IssueManager issueManager;

	public StepResultResourceHelper() {
	}

	public StepResultResourceHelper(IssueManager issueManager) {
		this.issueManager=issueManager;
	}
	
    /**
	 * @param associatedDefects
	 * @return
	 */
	public List<Map<String, String>> convertScheduleDefectToMap(List<StepDefect> associatedDefects, List<String> defectKeys) {
		if(associatedDefects == null) {
			associatedDefects = new ArrayList<StepDefect>(0);
		}
		List<Map<String, String>> stepResultDefectList = new ArrayList<Map<String, String>>(associatedDefects.size());
		if(associatedDefects != null && associatedDefects.size() > 0){
			for(StepDefect sd : associatedDefects){
				MutableIssue issue = issueManager.getIssueObject(new Long(sd.getDefectId()));
				if(issue == null)
					continue;
				stepResultDefectList.add(IssueUtils.convertDefectToMap(issue));
				if(defectKeys != null){
					defectKeys.add(issue.getKey());
				}
			}
		}
		Collections.sort(stepResultDefectList, new Comparator<Map<String, String>>() {
			@Override
			public int compare(Map<String, String> first, Map<String, String> second) {
				return first.get("key").compareTo(second.get("key"));
			}
		});
		return stepResultDefectList;
	}
}
