package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.model.StepDefect;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.AttachmentManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ApplicationConstants;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class StepResultManagerImpl implements StepResultManager {

	private final ActiveObjects ao;
	private final ProjectManager projectManager;
	private final AttachmentManager attachmentManager;
	
	private static final Logger log = LoggerFactory.getLogger(StepResultManagerImpl.class);
	public StepResultManagerImpl(ActiveObjects ao,
								ProjectManager projectManager,
								AttachmentManager attachmentManager) {
		this.ao = checkNotNull(ao);
		this.projectManager = projectManager;
		this.attachmentManager = attachmentManager;
	}

	@Override
	public StepResult addStepResult(Map<String, Object> resultProperties) {
		StepResult stepResult = ao.create(StepResult.class, resultProperties);
		return stepResult;
	}
	
	@Override
	public StepResult getStepResult(Integer stepResultId) {
		StepResult stepResult = ao.get(StepResult.class, stepResultId);
		return stepResult;
	}

	
	@Override
	public Boolean stepResultExists(Integer scheduleId, Integer stepId) {
		Query query = Query.select().where("SCHEDULE_ID = ? AND STEP_ID = ?", scheduleId.intValue(), stepId.intValue());
		int count = ao.count(StepResult.class, query);
		return count > 0;
	}
	
	@Override
	public List<StepResult> getStepResultsBySchedule(Integer scheduleId) {
		Query query = Query.select().where("SCHEDULE_ID = ?", scheduleId.intValue());
		StepResult[] stepResult = ao.find(StepResult.class, query);
		return Arrays.asList(stepResult);
	}
	
	@Override
	public List<StepDefect> getAssociatedDefects(final Integer stepResultId) {
		Query query = Query.select().where("STEP_RESULT_ID = ?", stepResultId.intValue());
		StepDefect[] stepDefects = ao.find(StepDefect.class, query);
		return Arrays.asList(stepDefects);
	}

	@Override
	public Map<String, Object> saveAssociatedDefects(final Integer stepId, final Integer scheduleId, final Integer stepResultId, List<Integer> defectsToPersist) {
		StepDefect[] stepDefectArray = ao.find(StepDefect.class, Query.select().where("STEP_RESULT_ID = ?", stepResultId));
		final Map<Integer, StepDefect> existingDefectMap = new HashMap<Integer, StepDefect>();
		for(StepDefect sd : stepDefectArray){
			existingDefectMap.put(sd.getDefectId(), sd);
		}
		Set<Integer> existingDefects = existingDefectMap.keySet();
		if(defectsToPersist == null){
			defectsToPersist = new ArrayList<Integer>();
		}
		
		final Collection<Integer> defectsToAdd = CollectionUtils.subtract(defectsToPersist, existingDefects);
		final Collection<Integer> defectsToRemove = CollectionUtils.subtract(existingDefects, defectsToPersist);
		final Collection<Integer> defectsUnchanged = CollectionUtils.intersection(existingDefects, defectsToPersist);
		
		ao.executeInTransaction(new TransactionCallback<StepDefect>() {
			Map<String, Object> params = new HashMap<String, Object>();
			@Override
			public StepDefect doInTransaction(){
				for(Integer defectId : defectsToAdd){
					params.clear();
					params.put("STEP_ID", stepId);
					params.put("DEFECT_ID", defectId);
					params.put("SCHEDULE_ID", scheduleId);
					params.put("STEP_RESULT_ID", stepResultId);
					ao.create(StepDefect.class, params);
				}
				
				for(Integer defectId : defectsToRemove){
					ao.delete(existingDefectMap.get(defectId));
				}
				return null;
			}
		});
		
		stepDefectArray = ao.find(StepDefect.class, Query.select().where("STEP_ID = ? and SCHEDULE_ID = ? ", stepId, scheduleId));
		Map<String, Object> associatedDefects = new HashMap<String, Object>(); 
		associatedDefects.put("added", defectsToAdd);
		associatedDefects.put("deleted", defectsToRemove);
		associatedDefects.put("unchanged", defectsUnchanged);
		if(stepDefectArray != null){
			associatedDefects.put("final", Arrays.asList(stepDefectArray));
		}
		return associatedDefects;
	}

	public void removeStepDefectsAssociation(Long defectId) {
        if(defectId != null){
            StepDefect[] stepDefects = ao.find(StepDefect.class, Query.select().where("DEFECT_ID = ?", defectId));
            if(stepDefects != null && stepDefects.length > 0){
                log.info("Deleting " + stepDefects.length + " defects from StepDefects Table ");
                ao.delete(stepDefects);
            }
        }
	}

	@Override
	public List<StepResult> getStepResultsByExecutionStatus(String statusId) {
		Query query = Query.select().where("STATUS = ?", statusId);
		StepResult[] stepResultArray = ao.find(StepResult.class, query);
		return Arrays.asList(stepResultArray);
	}

	@Override
	public Integer removeStepResult(Integer id) {
		StepResult sr = ao.get(StepResult.class, id);
		if(sr != null){
			return deleteStepResults(new StepResult[]{sr});
		}
		return 0;
	}

	@Override
	public Integer removeStepResultByStep(final Integer stepId) {
		final StepResult[] stepResults = ao.find(StepResult.class, Query.select().where("STEP_ID = ?", stepId));
		return deleteStepResults(stepResults);
	}
	
	@Override
	public Integer removeStepResultsBySchedule(Integer scheduleId) {
		final StepResult[] stepResults = ao.find(StepResult.class, Query.select().where("SCHEDULE_ID = ?", scheduleId));
		return deleteStepResults(stepResults);
	}

	/**
	 * Private utility method to delete step results and their mappings. 
	 * @param stepResults
	 * @return
	 */
	private Integer deleteStepResults(final StepResult[] stepResults) {
		if (stepResults == null || stepResults.length < 1)
			return 0;

		Integer noOfStepResults = ao.executeInTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction() {
				int stepResultsCount = 0;
				int fromIndex = 0;
				int toIndex = 0;
				boolean stopSublist = false;
				final List<StepResult> stepResultsList = Arrays.asList(stepResults);
				while (!stopSublist) {
					toIndex = stepResultsList.size() > fromIndex &&
							stepResultsList.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ?
							(fromIndex + ApplicationConstants.MAX_IN_QUERY) : stepResultsList.size();
					List<StepResult> subList = stepResultsList.subList(fromIndex, toIndex);
					log.info("Deleting " + subList.size() + " step results");


					List<Integer> stepResultIds = new ArrayList<Integer>();
					Project project = null;
					for (StepResult result : subList) {
						project = projectManager.getProjectObj(result.getProjectId());
						stepResultIds.add(result.getID());
					}
					/*delete defects in bulk*/
					deleteStepDefects(stepResultIds);
					/*delete attachments in bulk*/
					attachmentManager.removeAttachmentsInBulk(ApplicationConstants.TESTSTEPRESULT_TYPE, stepResultIds, project);


					ao.delete((StepResult[]) subList.toArray(new StepResult[subList.size()]));

					fromIndex += ApplicationConstants.MAX_IN_QUERY;
					if (toIndex == stepResultsList.size()) {
						stopSublist = true;
					}
					stepResultsCount += stepResultIds.size();
				}
				return stepResultsCount;
			}
		});
		return noOfStepResults;
	}

	public void deleteStepDefects(List<Integer> stepResultIds){
		if(stepResultIds == null || stepResultIds.size() < 1)
			return;
		int fromIndex = 0;
		int toIndex = 0;
		boolean stopSublist=false;
		while(!stopSublist) {
			toIndex = stepResultIds.size() > fromIndex && 
							stepResultIds.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ? 
							(fromIndex + ApplicationConstants.MAX_IN_QUERY) : stepResultIds.size();
			List<Integer> subList = stepResultIds.subList(fromIndex, toIndex);
			//Create Query with Ques and delete with max in sublist as 999 
			String ques = StringUtils.repeat("?", ", ", subList.size());
			StepDefect []schDefects = ao.find(StepDefect.class, Query.select().where("STEP_RESULT_ID IN (" + ques + ")", subList.toArray()));
			removeRemoteLinks(schDefects);
			ao.delete(schDefects);

			fromIndex += ApplicationConstants.MAX_IN_QUERY;
			if(toIndex == stepResultIds.size()) {
				stopSublist = true;
			}
		}
	}
	
	@Override
	public List<StepDefect> getStepResultsWithDefectBySchedule(Integer scheduleId) {
		List<Object> inputParams = new ArrayList<Object>();
		Query query = Query.select();
		query.alias(StepResult.class, "stepResult");
	    query.alias(StepDefect.class, "stepDefect");
	    query = query.join(StepResult.class,
	                "stepDefect.STEP_RESULT_ID = stepResult.ID");
	    inputParams.add(scheduleId);
		query.where("stepDefect.SCHEDULE_ID = ?", inputParams.toArray());
	    StepDefect[] stepDefect = ao.find(StepDefect.class, query);
		return Arrays.asList(stepDefect);
	}

	@Override
	public List<StepResult> getStepResultsByScheduleByPagination(Integer scheduleId, Integer offset, Integer limit) {

	    if(null == offset) {
	        offset = -1;
        }
        if(null == limit) {
            limit = -1;
        }

        //Query query = Query.select().where("SCHEDULE_ID = ?", scheduleId).offset(offset).limit(limit)
        Query query = Query.select();
        query.alias(StepResult.class, "stepResult");
        query.alias(Teststep.class, "teststep");
        query = query.join(Teststep.class,
                "teststep.ID = stepResult.STEP_ID");
	    query.where("stepResult.SCHEDULE_ID = ?", scheduleId).offset(offset).limit(limit).order(" ORDER_ID ASC");

	    StepResult[] stepResults = ao.find(StepResult.class, query);
        if(Objects.nonNull(stepResults) && stepResults.length > 0) {
            return Arrays.asList(stepResults);
        }
        return null;
	}

    @Override
    public Integer getStepResultsCount(Integer scheduleId) {
        Query query = Query.select().where("SCHEDULE_ID = ?", scheduleId);
        return ao.count(StepResult.class, query);
    }

	@Override
	public Integer getStepResultsCountByExecutionStatus(Integer scheduleId, Integer statusId) {
		String status = statusId+StringUtils.EMPTY;
		return ao.count(StepResult.class, Query.select().where("SCHEDULE_ID = ? AND STATUS = ?", scheduleId,status));
	}

	/**
	 * Deletes all the remote links for associated defects
	 * @param stepDefects
	 */
	private void removeRemoteLinks(StepDefect []stepDefects) {
		RemoteIssueLinkManager rilManager = ComponentAccessor.getComponentOfType(RemoteIssueLinkManager.class);
		ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		for(StepDefect sd : stepDefects){
			try {
				MutableIssue issueObject = ComponentAccessor.getIssueManager().getIssueObject(new Long(sd.getDefectId()));
				if(issueObject != null)
					rilManager.removeRemoteIssueLinkByGlobalId(issueObject, String.valueOf(sd.getScheduleId()), user);
				else
					log.warn("Unable to find issue in DB");
			} catch (Exception e) {
				log.error("Error in cleaning up remote Link", e.getMessage());
			}
		}
	}
	
	@Override
	public boolean verifyAndAddStepResult(Map<String, Object> resultProperties) {
		ao.create(StepResult.class, resultProperties);
		return true;
	}
	
}
