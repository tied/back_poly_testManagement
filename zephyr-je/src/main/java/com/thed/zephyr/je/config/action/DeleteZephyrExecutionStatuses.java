package com.thed.zephyr.je.config.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.audit.model.ChangeZJEItem;
import com.thed.zephyr.je.audit.service.AuditManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;


public class DeleteZephyrExecutionStatuses extends JiraWebActionSupport {

    private String id;
    private String name;
    private boolean confirm;
    private int	schedulesCount;
    private String newId;
    
    private ScheduleManager scheduleManager;
    private ScheduleIndexManager scheduleIndexManager;
    private final ZephyrLicenseManager zLicenseManager;
    private final AuditManager auditManager;
    
    
    public DeleteZephyrExecutionStatuses(ScheduleManager scheduleManager, ZephyrLicenseManager zLicenseManager,
    		ScheduleIndexManager scheduleIndexManager, AuditManager auditManager){
    	this.scheduleManager = scheduleManager;
		this.zLicenseManager = zLicenseManager;
		this.scheduleIndexManager=scheduleIndexManager;
		this.auditManager=auditManager;
    }
    
	@Override
    protected void doValidation(){
    }

    @Override
	public String doDefault() throws Exception{

    	ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());

    	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getExecutionStatuses();
    	ExecutionStatus execStatusObj = execStatusMap.get(new Integer(getId()));
		
	  	if( (execStatusObj == null) || (execStatusObj.getType() == 0) ){
    		//Execution statuses of type System can't be deleted.
    		return getRedirect(getRedirectPage());
    	}
  		
    	//Get list of schedules matching input status.
    	log.debug("Getting schedules with status id = " + getId() );
    	List<Schedule> scheduleList = scheduleManager.getSchedulesByExecutionStatus(getId());
    	
    	setSchedulesCount(scheduleList.size());
    	return INPUT;
    }
    
    @Override
	protected String doExecute() throws Exception{
    	log.debug("Confirm value is - "  + confirm);

    	if (confirm){
        	log.debug("Execution status with id=" + getId() + " will be deleted");

        	//Get new status Id.
        	String newStatusId = getNewId();
        	log.debug("New Status id - " +  newId);
        	
        	//Make sure all Testcases schedules with this status are moved under different status.
        	List<Schedule> scheduleList = scheduleManager.getSchedulesByExecutionStatus(getId());
        	List<Schedule> indexedSchedules = new ArrayList<Schedule>(0);
        	for(Schedule sch : scheduleList){
        		sch.setStatus(newStatusId);
				if(JiraUtil.getExecutionStatuses().get(Integer.valueOf(newStatusId)).getId() == -1) {
					sch.setExecutedBy(null);
					sch.setExecutedOn(null);
				}
        		sch.save();
        		indexedSchedules.add(sch);
        	}
        List<ChangeZJEItem> changeLogs = auditManager.getZephyrChangeLogs();
        for (ChangeZJEItem changeZJEItem : changeLogs) {
			if(getId().equals(changeZJEItem.getNewValue())) {
				changeZJEItem.setNewValue(getNewId());
				changeZJEItem.save();
			}
			else if(getId().equals(changeZJEItem.getOldValue())) {
				changeZJEItem.setOldValue(getNewId());
				changeZJEItem.save();
			}
			
		}
        	
        	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getExecutionStatuses();
        	execStatusMap.remove(new Integer(getId()));
        	
        	//Rebuild the list.
        	Collection coll = execStatusMap.values();
			ObjectMapper objMapper = new ObjectMapper();
			String execStatusesStringObject = null;
			
            try{
            	execStatusesStringObject = objMapper.writeValueAsString(coll);
            }
            catch (IOException e){
                throw new RuntimeException("Failed to save ExecutionStatus List to JSON: " + coll, e);
            }

            log.info("Deleted Selected status. Storing updated status List back to JIRA.");
            JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
            							.setText(ConfigurationConstants.ZEPHYR_EXECUTION_STATUSES,execStatusesStringObject);
            
            if(indexedSchedules.size() > 0) {
	            try {
					EnclosedIterable<Schedule> enclosedSchedules = CollectionEnclosedIterable.copy(indexedSchedules);
					scheduleIndexManager.reIndexSchedule(enclosedSchedules, Contexts.nullContext());
	            } catch(Exception e) {
	            	log.warn("Error Indexing schedules that were updated",e);
	            }
            }
        }

        //Update ExecutionStatusMap to reflect changes.
        JiraUtil.buildExecutionStatusMap();
        
        if (getHasErrorMessages())
            return ERROR;
        else
            return getRedirect(getRedirectPage());
    }
		
    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    
    public boolean isConfirm(){
        return confirm;
    }

    public void setConfirm(boolean confirm){
        this.confirm = confirm;
    }
    
    public String getRedirectPage(){
    	return "ViewZephyrExecutionStatuses!default.jspa";
    }

	public String getName() {
		return name;
	}

	public void setName(String name){
		this.name = name;
	}
	
	public String getNewId() {
		return newId;
	}

	public void setNewId(String newId) {
		this.newId = newId;
	}

	public int getSchedulesCount() {
		return schedulesCount;
	}

	public void setSchedulesCount(int schedulesCount) {
		this.schedulesCount = schedulesCount;
	}

	//Returns the execution status list to which user can assign the schedules associated to status that Administrator wants to delete.
	public Collection<ExecutionStatus> getExecutionStatusList(){
		
    	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getExecutionStatuses();
    	Map<Integer, ExecutionStatus> clonedMap = new HashMap<Integer, ExecutionStatus>(execStatusMap);
    	clonedMap.remove(new Integer(getId()));
		
		return clonedMap.values();
	}
	
}
