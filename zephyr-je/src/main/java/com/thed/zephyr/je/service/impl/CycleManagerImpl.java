/**
 * 
 */
package com.thed.zephyr.je.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.ZFJCacheService;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import net.java.ao.EntityStreamCallback;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.jfree.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author smangal
 *
 */
public class CycleManagerImpl implements CycleManager, ApplicationContextAware{

	private static final Logger log = LoggerFactory.getLogger(CycleManagerImpl.class);
	
	private final ActiveObjects ao;
	private ApplicationContext applicationContext;
	private final EventPublisher eventPublisher;
	private Query query;
	private final JobProgressService jobProgressService;
	private final JiraAuthenticationContext authContext;
	private final ZFJCacheService zfjCacheService;

	public CycleManagerImpl(ActiveObjects ao,EventPublisher eventPublisher,
							JiraAuthenticationContext authContext,
							JobProgressService jobProgressService, ZFJCacheService zfjCacheService) {
		this.ao = checkNotNull(ao);
		this.eventPublisher=eventPublisher;
		this.authContext=authContext;
		this.jobProgressService=jobProgressService;
		this.zfjCacheService = zfjCacheService;
	}
	
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#getCycles(com.thed.jira.plugin.model.Cycle)
	 */
	@Override
	public List<Cycle> getCycles(Cycle cycle) {
		Cycle [] cycles =  ao.find(Cycle.class);
		return Arrays.asList(cycles);
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#getCycle(java.lang.Long)
	 */
	@Override
	public Cycle getCycle(Long id) {
		Cycle [] cycles =  ao.find(Cycle.class, Query.select().where("ID = ?", id));
		if(cycles != null && cycles.length > 0){
			return cycles[0];
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#getCyclesByVersion(java.lang.Long,java.lang.Long,java.lang.Integer)
	 */
	@Override
	public List<Cycle> getCyclesByVersion(Long versionId, Long projectId, Integer offset) {
		int limit = 9;
		if(projectId == null){
			return null;
		}

		//If Offset comes as -1, no pagination
		if(offset == null || offset == -1) {
			limit = -1;
			if(offset == null){
				offset = -1;
			}
		}
		
		Cycle [] cycles =  null;
		if(versionId != null)
			cycles = ao.find(Cycle.class, Query.select().where("VERSION_ID = ? and PROJECT_ID = ?", versionId, projectId).offset(offset).limit(limit)); 
		else{
			//Get all cycles for given project
			cycles = ao.find(Cycle.class, Query.select().where("PROJECT_ID = ?", projectId).offset(offset).limit(limit)); 
		}
			
		if(cycles != null && cycles.length > 0){
			 return Arrays.asList(cycles);
		}
		return new ArrayList<Cycle>();
	}

	
	@Override
	public Integer getCycleCountByVersionId(final Long versionId, final Long projectId) {
		Integer cycleCount = null;
		if(versionId != null)
			cycleCount = ao.count(Cycle.class, Query.select().where("VERSION_ID = ? and PROJECT_ID = ?", versionId, projectId)); 
		else{
			//Get all cycles for given project
			cycleCount = ao.count(Cycle.class, Query.select().where("PROJECT_ID = ?", projectId)); 
		}
		return cycleCount;
	}
	
	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#getCyclesByIssueId(java.lang.Long)
	 */
	@Override
	public List<Cycle> getCyclesByIssueId(Integer issueId) {
//		Cycle [] cycles =  ao.find(Cycle.class, Query.select().where("ISSUE_ID = ?", issueId));
//		return Arrays.asList(cycles);
		return null;
	}

	
	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#saveCycle(com.thed.jira.plugin.model.Cycle)
	 */
	@Override
	public Cycle saveCycle(Map<String, Object> cycleProperties) {
		return ao.create(Cycle.class, cycleProperties);
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#removeBulkCycle(java.lang.Long,java.lang.Long)
	 */
	@Override
	public Integer removeBulkCycle(final Long projectId, final Long versionId) {
		String jobProgressToken = "";
		//final List<Long> cyclesId = new ArrayList<Long>();
		final List<String> cyclesId = new ArrayList<String>();

		Integer noOfCycles = ao.executeInTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction() {
				List<Cycle> cycles = new ArrayList<Cycle>();
				if(versionId == null) {
					cycles = getCyclesByVersion(null,projectId,-1);
					//Delete Adhoc Cycle if Project is deleted. This is invoked for Project deletion.
					//we don't allow deletion of Adhoc Cycles unless project is deleted
					ScheduleManager scheduleManager = (ScheduleManager) applicationContext.getBean("schedule-manager");
					int schedules = scheduleManager.removeSchedulesByCycleIdAndProjectId(null, projectId);
					Log.debug("Total " + schedules + " schedules were deleted");
				} else {
					cycles = getCyclesByVersion(versionId,projectId,-1);
				}
				for(Cycle cycle : cycles) {
					cyclesId.add(String.valueOf(cycle.getID()));
                    removeCycleAndFolder(cycle.getProjectId(), cycle.getVersionId(), Long.valueOf(cycle.getID()),jobProgressToken);
				}
				return cycles.size();
			}
		});
		
		//deleteIndexBased on Cycles
    	Map<String,Object> param = new HashMap<String,Object>();
		param.put("ENTITY_TYPE", "CYCLE_ID");
		param.put("ENTITY_VALUE", cyclesId);
		try {
			eventPublisher.publish(new SingleScheduleEvent(null,param,com.thed.zephyr.je.event.EventType.CYCLE_DELETED));
		} catch(Exception e) {
			log.error("Error Deleting Index:",e);
		}
		return noOfCycles;
	}

	@Override
	public void removeCycle(Long id, String jobProgressToken) {
		Cycle cycle = getCycle(id);
		if (cycle == null) return;
		ScheduleManager scheduleManager = (ScheduleManager) applicationContext.getBean("schedule-manager");
		Future<Boolean> promise = scheduleManager.removeSchedulesByCycleIdPromise(id, jobProgressToken);
		try {
			promise.get();
			ao.delete(cycle);
			jobProgressService.addCompletedSteps(jobProgressToken, 1);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("success", authContext.getI18nHelper().getText("zephyr.common.success.delete",
					ApplicationConstants.CYCLE_ENTITY, cycle.getName()));
			jobProgressService.setMessage(jobProgressToken, jsonObject.toString());
		} catch (Exception e) {
			log.error("error during removing executions by cycleId ", e);
		}
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		log.info("Cycle with ID {} and name {} is deleted by {}", id,cycle.getName(), currentUser != null ? currentUser.getName() : " no user");
		return;
	}
	
	
	@Override
	public Integer swapVersion(final Long sourceVersionId, final Long targetVersionId, final Long projectId) {
		if (sourceVersionId == null || targetVersionId == null || projectId == null) {
			return null;
		}
		final List<Schedule> scheduleList = new ArrayList<Schedule>();
		Integer noOfCycles = ao.executeInTransaction(new TransactionCallback<Integer>() {
			@Override
			public Integer doInTransaction() {
				final int no_offset = -1;
				List<Cycle> cycles = getCyclesByVersion(sourceVersionId, projectId, no_offset);
				//we don't allow deletion of Adhoc Cycles unless project is deleted
				ScheduleManager scheduleManager = (ScheduleManager) applicationContext.getBean("schedule-manager");
				FolderManager folderManager = (FolderManager) applicationContext.getBean("folder-manager");

				//Update All Cycles First
				for (Cycle cycle : JiraUtil.safe(cycles)) {
					cycle.setVersionId(targetVersionId);
					cycle.save();
					List<Schedule> schedules = scheduleManager.getSchedulesByCycleId(sourceVersionId, projectId, cycle.getID(), no_offset, null, null);
					for (Schedule schedule : JiraUtil.safe(schedules)) {
						schedule.setVersionId(targetVersionId);
						schedule.save();
					}
					scheduleList.addAll(schedules);
					folderManager.updateDeletedVersionId(projectId, sourceVersionId, Long.valueOf(cycle.getID()+""), targetVersionId);
				}
				/*For Adhoc cycle - ZFJ-987*/
				List<Schedule> schedules = scheduleManager.getSchedulesByCycleId(sourceVersionId, projectId, ApplicationConstants.AD_HOC_CYCLE_ID, no_offset, null, null);
				for (Schedule schedule : JiraUtil.safe(schedules)) {
					schedule.setVersionId(targetVersionId);
					schedule.save();
				}
				scheduleList.addAll(schedules);
				return JiraUtil.safe(cycles).size();
			}
		});
		//ReIndex Event
		try {
			eventPublisher.publish(new SingleScheduleEvent(scheduleList, new HashMap<String, Object>(), com.thed.zephyr.je.event.EventType.EXECUTION_UPDATED));
		} catch (Exception e) {
			log.error("Failed to ReIndex:", e);
		}
		return noOfCycles;
	}

	
	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#getCyclesByCriteria(java.lang.String, int)
	 */
	@Override
	public List<Cycle> getCyclesByCriteria(String searchExpression, int maxAllowedRecord) {
		Query query = Query.select();
		if(searchExpression != null){
			query.setWhereClause(searchExpression);
		}
		if(maxAllowedRecord > 0)
			query.setLimit(maxAllowedRecord);
		Cycle[] cycles = ao.find(Cycle.class, query);
		return Arrays.asList(cycles);
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.CycleManager#addIssuesToCycle(java.lang.Long, java.util.List)
	 */
	@Override
	public List<Schedule> addIssuesToCycle(Long cycleId, List<Long> issuesIds) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Long getDistinctProjectIdByVersionId(Long versionId) {
		Long projectId = null;
		Query query = Query.select("PROJECT_ID").distinct();
		query.setLimit(1);
		Cycle[] cycles = ao.find(Cycle.class, query.where("VERSION_ID = ? ", versionId));
		if(cycles != null && cycles.length > 0) {
			projectId = cycles[0].getProjectId();
		}
		return projectId;
	}

	public List<Cycle> getValuesByKey(final String clause,final List<String> values) {
		List<Object> params = new ArrayList<Object>();

		String ques[] = new String[values.size()];
		for(int i=0; i< values.size(); i++){
			ques[i] = "?";
		}
		//This is a hack to get around AO's limitation of being able to pass only one object per query
		String whereClause = " NAME IN ( " + StringUtils.join(ques, ',') + " ) ";
		params.addAll(Arrays.asList(values.toArray()));
		Query query = Query.select().where(whereClause,params.toArray());
		Cycle[] arrCycles = ao.find(Cycle.class, query);
		return Arrays.asList(arrCycles);
	}
	
    public List<Cycle> getCycles(String whereClause) {
		final List<Cycle> allCycles = new ArrayList<Cycle>();
        getCycles(whereClause, new EntityStreamCallback<Cycle, Integer>() {
                @Override
                public void onRowRead(Cycle cycle) {
                	allCycles.add(cycle);
                }
        });
		return allCycles;
    }

	public void getCycles(String whereClause, EntityStreamCallback<Cycle, Integer> entityStreamCallback) {
		query = Query.select("*");
		if(whereClause != null){
			query.setWhereClause(whereClause);
		}

		ao.stream(Cycle.class, query, entityStreamCallback);
    }
    
	public List<String> getValues(final String fieldName,final String value) {
		Query query = Query.select().where(fieldName + " = ?", value).distinct();       	
		Cycle[] cycles = ao.find(Cycle.class, query);
		List<String> allValues = new ArrayList<String>();
		if(cycles != null && cycles.length > 0) {
			for(Cycle cycle : cycles) {
				try {
					if(StringUtils.equalsIgnoreCase(fieldName, "NAME")) {
						allValues.add(cycle.getName());
					} else if(StringUtils.equalsIgnoreCase(fieldName, "BUILD")) {
						allValues.add(cycle.getBuild());
					}
				} catch (SecurityException e) {
					log.error("Security exception fetching cycle details for zql",e);
				}
			}
			return allValues;
		} else {
			return allValues;
		}
	}
	
    /**
     * Gets List of Cycle information based on version id. For adhoc cycles,
     * versionId is null and hence search is done via projectId
     * @param projectIds
     * @param clauseName
     * @param valuePrefix
     * @return Cycle populated Cycle object
     */
	public List<Cycle> getCyclesByProjectId(final Collection<Integer> projectIds,String clauseName, String valuePrefix) {
		List<Object> params = new ArrayList<Object>();

		String ques[] = new String[projectIds.size()];
		for(int i=0; i< projectIds.size(); i++){
			ques[i] = "?";
		}
		//This is a hack to get around AO's limitation of being able to pass only one object per query
		String whereClause = " PROJECT_ID IN ( " + StringUtils.join(ques, ',') + " ) ";
		params.addAll(Arrays.asList(projectIds.toArray()));
		if(StringUtils.isNotBlank(valuePrefix)) {
			whereClause += " AND " + clauseName + " LIKE ?";
			params.add(valuePrefix+"%");
		}
		Query query = Query.select().where(whereClause,params.toArray());
		Cycle[] arrCycles = ao.find(Cycle.class, query);
		return Arrays.asList(arrCycles);		
	}
	
	
	public List<Cycle> getCyclesByProjectsAndVersions(List<Long> projectIdList, String[] versionIds, String[] sprintIds, Integer offset, Integer maxRecords) {
		List<Object> params = new ArrayList<Object>();

		String ques[] = new String[versionIds.length];
		for(int i=0; i< versionIds.length; i++){
			ques[i] = "?";
		}

		String quesSprint[] = new String[sprintIds.length];
		if(sprintIds != null) {
			for(int i=0; i< sprintIds.length; i++){
				quesSprint[i] = "?";
			}			
		}
		
		String quesProj[] = new String[projectIdList.size()];
		if(projectIdList != null) {
			for(int i=0; i< projectIdList.size(); i++){
				quesProj[i] = "?";
			}			
		}
		
		String whereClause = "";
		//This is a hack to get around AO's limitation of being able to pass only one object per query
		if(quesProj.length > 0) {
			whereClause = "PROJECT_ID IN ( " + StringUtils.join(quesProj, ',') + " )";
		}
		if(ques.length > 0) {
			if(StringUtils.isBlank(whereClause)) {
				whereClause = "VERSION_ID IN ( " + StringUtils.join(ques, ',') + " )";
			} else {
				whereClause += " AND VERSION_ID IN ( " + StringUtils.join(ques, ',') + " )";
			}
		}
		
		if(quesSprint.length > 0) {
			if(StringUtils.isBlank(whereClause)) {
				whereClause = "SPRINT_ID IN ( " + StringUtils.join(quesSprint, ',') + " )";
			} else {
				whereClause += " AND SPRINT_ID IN ( " + StringUtils.join(quesSprint, ',') + " )";
			}
		}

		Iterator<Long> itrProj = projectIdList.iterator();
		while (itrProj.hasNext()) {
			Long longVal = 0l;
			Object val = itrProj.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		List<String> versionList = Arrays.asList(versionIds);
		Iterator<String> itr = versionList.listIterator();
		while (itr.hasNext()) {
			Long longVal = 0l;
			Object val = itr.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		
		List<String> sprintList = Arrays.asList(sprintIds);
		Iterator<String> itr1 = sprintList.listIterator();
		while (itr1.hasNext()) {
			Long longVal = 0l;
			Object val = itr1.next();
			if(val instanceof Number)
				longVal = ((Number)val).longValue();
			if(val instanceof String)
				longVal = Long.parseLong((String)val);
			params.add(longVal);
		}
		Query query = Query.select().offset(offset).limit(maxRecords).where(whereClause,params.toArray());
		Cycle[] arrCycles = ao.find(Cycle.class, query);
		return Arrays.asList(arrCycles);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void removeCycleAndFolder(Long projectId, Long versionId, Long id, String jobProgressToken) {
		Cycle cycle = getCycle(id);
		if (cycle == null) return;
		ScheduleManager scheduleManager = (ScheduleManager) applicationContext.getBean("schedule-manager");
		FolderManager folderManager = (FolderManager) applicationContext.getBean("folder-manager");
		int foldersCount = folderManager.getFoldersCountForCycle(projectId, versionId, id);
		int limit = 20; int offset = 0;
		try {
			while(foldersCount > offset) {
				List<Folder> folderList = folderManager.fetchFolders(projectId, versionId, Arrays.asList(new Long[]{id}), limit, offset);
				folderList.stream().forEach(folder -> {
					try {
						Future<Boolean> promise = scheduleManager.removeSchedulesByFolderIdAndCycleIdPromise(projectId, versionId, id, Long.valueOf(folder.getID() + ""), jobProgressToken);
						promise.get();
						folderManager.removeCycleFolderMapping(projectId, versionId, id, (Long.valueOf(folder.getID() + "")));
						folderManager.removeFolder(Long.valueOf(folder.getID() + ""), (Map<String, Boolean>) zfjCacheService.getCacheByKey(id + "", null));
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException("Erro while deleting the executions for the folder - " + folder.getID(), e);
					}
				});
				offset = offset + limit;
			}
			//remove executions mapped under cycle instead of folder.
			Future<Boolean> promise = scheduleManager.removeSchedulesByCycleIdPromise(id, jobProgressToken);
			promise.get();
			ao.delete(cycle);
			jobProgressService.addCompletedSteps(jobProgressToken, 1);
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("success", authContext.getI18nHelper().getText("zephyr.common.success.delete",
					ApplicationConstants.CYCLE_ENTITY, cycle.getName()));
			jobProgressService.setMessage(jobProgressToken, jsonObject.toString());
		} catch (Exception e) {
			log.error("error during removing executions by cycleId ", e);
			jobProgressService.addCompletedSteps(jobProgressToken, ApplicationConstants.JOB_STATUS_FAILED);
		}
		ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
		log.info("Cycle with ID {} and name {} is deleted by {}", id,cycle.getName(), currentUser != null ? currentUser.getName() : " no user");
		return;
	}
	
	@Override
	public Integer getTotalCyclesCount() {
		return ao.count(Cycle.class, Query.select());
	}
}
