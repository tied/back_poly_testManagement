package com.thed.zephyr.je.config.action;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;


public class DeleteZephyrTestStepStatuses extends JiraWebActionSupport {

    private String id;
    private String name;
    private boolean confirm;
    private int	stepsCount;
    private String newId;
    
    private StepResultManager stepResultManager;
    private final ZephyrLicenseManager zLicenseManager;
    
    
    public DeleteZephyrTestStepStatuses(StepResultManager stepResultManager, ZephyrLicenseManager zLicenseManager){
    	this.stepResultManager = stepResultManager;
		this.zLicenseManager = zLicenseManager;
    }
    
	@Override
    protected void doValidation(){
    }

    @Override
	public String doDefault() throws Exception{

    	ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());

    	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getStepExecutionStatuses();
    	ExecutionStatus execStatusObj = execStatusMap.get(new Integer(getId()));
		
	  	if( (execStatusObj == null) || (execStatusObj.getType() == 0) ){
    		//Execution statuses of type System can't be deleted.
    		return getRedirect(getRedirectPage());
    	}
  		
    	//Get list of steps matching input status.
    	log.debug("Getting steps with status id = " + getId() );
    	List<StepResult> stepResultList = stepResultManager.getStepResultsByExecutionStatus(getId());
    	
    	setStepsCount(stepResultList.size());
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
        	
        	//Add equivalent Teststep Manager call to get steps with given executions.
        	List<StepResult> stepResultList = stepResultManager.getStepResultsByExecutionStatus(getId());
        	for(StepResult sch : stepResultList){
        		sch.setStatus(newStatusId);
				if(JiraUtil.getStepExecutionStatuses().get(Integer.valueOf(newStatusId)).getId() == -1) {
					sch.setExecutedBy(null);
					sch.setExecutedOn(null);
				}
        		sch.save();
        	}
        	
        	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getStepExecutionStatuses();
        	execStatusMap.remove(new Integer(getId()));
        	
        	//Rebuild the list.
        	Collection coll = execStatusMap.values();
			ObjectMapper objMapper = new ObjectMapper();
			String execStatusesStringObject = null;
			
            try{
            	execStatusesStringObject = objMapper.writeValueAsString(coll);
            }
            catch (IOException e){
                throw new RuntimeException("After Test Step status deletion, failed to save Test Step ExecutionStatus List to JSON: " + coll, e);
            }

            log.info("Deleted Selected Step status. Storing updated status List back to JIRA.");
            JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
            							.setText(ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES,execStatusesStringObject );
        }

        //Update ExecutionStatusMap to reflect changes.
        JiraUtil.buildStepExecutionStatusMap();
        
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
    	return "ViewZephyrTestStepStatuses!default.jspa";
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

	public int getStepsCount() {
		return stepsCount;
	}

	public void setStepsCount(int stepsCount) {
		this.stepsCount = stepsCount;
	}

	//Returns the execution status list to which user can assign the schedules associated to status that Administrator wants to delete.
	public Collection<ExecutionStatus> getStepExecutionStatusList(){		
    	Map<Integer, ExecutionStatus> execStatusMap = JiraUtil.getStepExecutionStatuses();
    	// cloning the Map, so that value is not removed from the original execStatusMap in case user choose to cancel the operation in UI
    	Map<Integer, ExecutionStatus> clonedMap = new HashMap<Integer, ExecutionStatus>(execStatusMap);
    	clonedMap.remove(new Integer(getId()));
		
		return clonedMap.values();
	}
	
}
