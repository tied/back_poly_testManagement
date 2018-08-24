package com.thed.zephyr.je.index.bridge;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.util.LuceneUtils;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.ScheduleDefect;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ExternalEntityFieldBridge implements JEFieldBridge {
	private final IssueManager issueManager;
	private final ScheduleManager scheduleManager;
	private final StepResultManager stepResultManager;
	private static final String SORT_ISSUE_SUMMARY_INDEX_LABEL = "sort_summary";
	private static final String ISSUE_SUMMARY_INDEX_LABEL = "summary";
	private static final String ISSUE_SECURITY_INDEX_LABEL = "issue_security_num";
	private static final String COMPONENT_ID_INDEX_LABEL = "COMPONENT_ID";
	private static final String COMPONENT_NAME_INDEX_LABEL = "COMPONENT_NAME";
	private static final String PRIORITY_ID_INDEX_LABEL = "PRIORITY_ID";
	private static final String PRIORITY_INDEX_LABEL = "PRIORITY";
	private static final String LABEL_INDEX_LABEL = "LABEL";
	private static final String EXECUTED_ON_INDEX_LABEL = "EXECUTED_ON";
	private static final String SCHEDULE_DEFECT_ID = "SCHEDULE_DEFECT_ID";
	private static final String SCHEDULE_DEFECT_KEY = "SCHEDULE_DEFECT_KEY";
	private static final String STEP_DEFECT_ID = "STEP_DEFECT_ID";
	private static final String STEP_DEFECT_KEY = "STEP_DEFECT_KEY";
	private static final String SCHEDULE_DEFECT_COUNT = "SCHEDULE_DEFECT_COUNT";
	private static final String STEP_DEFECT_COUNT = "STEP_DEFECT_COUNT";
	private static final String TOTAL_LINKED_DEFECT_COUNT = "TOTAL_DEFECT_COUNT";
	private static final String SCHEDULE_ISSUE_STATUS = "SCHEDULE_DEFECT_STATUS";

    protected final Logger log = Logger.getLogger(ExternalEntityFieldBridge.class);

	
	ExternalEntityFieldBridge(final IssueManager issueManager,final ScheduleManager scheduleManager,final StepResultManager stepResultManager) {
		this.issueManager=issueManager;
		this.scheduleManager=scheduleManager;
		this.stepResultManager=stepResultManager;
	}
	
	@Override
	public void set(String fieldName,Object value, Document document) {
		indexIssueRelatedFields(value,document);
		indexScheduleDefect(value,document);
	}

	/**
	 * Index Components based on IssueId
	 * @param value
	 * @param document
	 */
	private void indexIssueRelatedFields(Object value, Document document) {
		Schedule schedule = (Schedule)value;
		if(schedule == null) {
			return;
		}
		Issue issue = issueManager.getIssueObject(schedule.getIssueId().longValue());
		if(issue == null) {
			log.warn("No Issue found for IssueId:"+schedule.getIssueId() + " , associated with scheduleId:"+schedule.getID());
		} else {
			//Indexing Issue Summary
			if(StringUtils.isNotBlank(issue.getSummary())) {
                document.add(new Field(ISSUE_SUMMARY_INDEX_LABEL, issue.getSummary(), Field.Store.YES, Field.Index.ANALYZED));
                final String summary = getValueForSorting(issue.getSummary());
                if (StringUtils.isNotBlank(summary)) {
                	document.add(new Field(SORT_ISSUE_SUMMARY_INDEX_LABEL, summary, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			} else {
				document.add(new Field(ISSUE_SUMMARY_INDEX_LABEL, ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			
			Collection<ProjectComponent> components = issue.getComponents();
			if(components != null && components.size() > 0) {
				for(ProjectComponent projectComp : components) {
					document.add(new Field(COMPONENT_ID_INDEX_LABEL, String.valueOf(projectComp.getId()), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
					document.add(new Field(COMPONENT_NAME_INDEX_LABEL, projectComp.getName(), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}else{
				document.add(new Field(COMPONENT_ID_INDEX_LABEL, ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			if(issue.getPriority() != null){
				document.add(new Field(PRIORITY_INDEX_LABEL, issue.getPriority().getName(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				document.add(new Field(PRIORITY_ID_INDEX_LABEL, String.valueOf(issue.getPriority().getId()), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}else{
				document.add(new Field(PRIORITY_ID_INDEX_LABEL, ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			if(issue.getLabels() != null && issue.getLabels().size() > 0){
				for(Label label : issue.getLabels()) {
					document.add(new Field(LABEL_INDEX_LABEL, label.getLabel(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			} else{
				document.add(new Field(LABEL_INDEX_LABEL, ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
			if(schedule.getExecutedOn() != null){
				//Date executionDate = DateUtils.truncate(new Date(schedule.getExecutedOn()), Calendar.DATE);
				Date executionDate = new Date(schedule.getExecutedOn()); // Fix for ZFJ-1253
				document.add(new Field(EXECUTED_ON_INDEX_LABEL, LuceneUtils.dateToString(executionDate), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}

			if(issue.getSecurityLevelId() != null) {
				document.add(new Field(ISSUE_SECURITY_INDEX_LABEL, String.valueOf(issue.getSecurityLevelId()), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			} else{
				document.add(new Field(ISSUE_SECURITY_INDEX_LABEL, ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		}
	}
	
	/**
	 * Indexes Defects associated to the Schedule
	 * @param value
	 * @param document
	 */
	private void indexScheduleDefect(Object value, Document document) {
		Schedule schedule = (Schedule)value;
		if(schedule == null) {
			return;
		}
		List<ScheduleDefect> scheduleDefects = scheduleManager.getScheduleDefects(schedule.getID());
		int defectCount = scheduleDefects != null ? scheduleDefects.size() : 0;
		if(scheduleDefects != null && scheduleDefects.size() > 0) {
			for(ScheduleDefect scheduleDefect : scheduleDefects) {
				Issue issue = issueManager.getIssueObject(scheduleDefect.getDefectId().longValue());

				if(issue != null) {
					document.add(new Field(SCHEDULE_DEFECT_ID, String.valueOf(issue.getId()), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
					document.add(new Field(SCHEDULE_DEFECT_KEY, String.valueOf(issue.getKey()), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
					document.add(new Field(SCHEDULE_ISSUE_STATUS, issue.getStatusObject().getId(), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
		} else{
			document.add(new Field(SCHEDULE_DEFECT_ID, "-1", Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
		//Step Defect Count
		int stepDefectCount = indexScheduleStepDefect(schedule.getID(),document);
		document.add(new Field(SCHEDULE_DEFECT_COUNT, String.valueOf(defectCount), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		
		//Sum Up 
		int totalDefectCount = defectCount + stepDefectCount;
		document.add(new Field(TOTAL_LINKED_DEFECT_COUNT, String.valueOf(totalDefectCount), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		
	}

	
	/**
	 * Indexes Defects associated to the Steps
	 * @param scheduleId
	 * @param document
	 * @return total count of defects for given step executions
	 */
	private int indexScheduleStepDefect(int scheduleId, Document document) {
		List<StepDefect> stepDefects = stepResultManager.getStepResultsWithDefectBySchedule(scheduleId);
		int defectCount = stepDefects != null ? stepDefects.size() : 0;
		if(stepDefects != null && stepDefects.size() > 0) {
			for(StepDefect stepDefect : stepDefects) {
				Issue issue = issueManager.getIssueObject(stepDefect.getDefectId().longValue());
				if(issue != null) {
					document.add(new Field(STEP_DEFECT_ID, String.valueOf(issue.getId()), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
					document.add(new Field(STEP_DEFECT_KEY, String.valueOf(issue.getKey()), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
				}
			}
		} 
		document.add(new Field(STEP_DEFECT_COUNT, String.valueOf(defectCount), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		return defectCount;
	}


    public void indexLongAsKeyword(final Document doc, final String indexField, final Long fieldValue, final Issue issue) {
        if (fieldValue != null) {
            doc.add(new Field(indexField, fieldValue.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }
    
    private String getValueForSorting(final String fieldValue) {
        final String trimmed = (fieldValue == null) ? null : fieldValue.trim();
        if (!StringUtils.isBlank(trimmed)) {
            return trimmed;
        }
        else {
            return String.valueOf('\ufffd');
        }
    }
}
