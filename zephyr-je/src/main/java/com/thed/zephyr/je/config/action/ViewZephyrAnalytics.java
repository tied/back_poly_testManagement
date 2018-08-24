package com.thed.zephyr.je.config.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.codehaus.jackson.map.ObjectMapper;
import webwork.action.ServletActionContext;
import java.io.IOException;
import java.util.*;


public class ViewZephyrAnalytics extends JiraWebActionSupport {

	private String statusColor;
    private String name;
    private String description;
	private final ZephyrLicenseManager zLicenseManager;

    public ViewZephyrAnalytics(ZephyrLicenseManager zLicenseManager){
    	this.zLicenseManager = zLicenseManager;
    }
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
    protected void doValidation()
    {
        if (!TextUtils.stringSet(getName()))
            addError("name", getText("zephyr.errors.must.specify.name"));

        if (!TextUtils.stringSet(getStatusColor()) || (!JiraUtil.validateHexColor(getStatusColor())) )
            addError("statusColor", getText("zephyr.errors.must.specify.color"));

        super.doValidation();
    }
	
	@Override
	public String doDefault() throws Exception {

		ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());
		
		return SUCCESS;
	}
	
    public String doAddTestStepStatus() throws Exception
    {
    	
    	doValidation();
        if(getHasErrors())
        	return ERROR;
        
    	Map<Integer, ExecutionStatus> stepExecStatusMap = JiraUtil.getStepExecutionStatuses();
    	//New status will have id equal to max (number in stepExecStatusMap keys)+1
    	List<Integer>statusIds = new ArrayList<Integer>(stepExecStatusMap.keySet());    
    	Collections.sort(statusIds);    	
    	Integer newStatusID = new Integer(statusIds.get(-1+statusIds.size()) + 1 );
    	
    	//Build the new status execution object.
    	ExecutionStatus newExecStatus = new ExecutionStatus(newStatusID, getName().toUpperCase(), getDescription(), getStatusColor(), new Integer(1));
    	stepExecStatusMap.put(newStatusID, newExecStatus);
    	
    	//Rebuild the list.
    	Collection coll = stepExecStatusMap.values();
		ObjectMapper objMapper = new ObjectMapper();
		String stepExecStatusesStringObject = null;
		
        try{
        	stepExecStatusesStringObject = objMapper.writeValueAsString(coll);
        }
        catch (IOException e){
            throw new RuntimeException("Failed to save Test Step ExecutionStatus List to JSON: " + coll, e);
        }

        log.info("Adding new Status. Storing updated status List back to JIRA.");
        JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
        							.setText(ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES,stepExecStatusesStringObject );

        //Update ExecutionStatusMap to reflect changes.
        JiraUtil.buildStepExecutionStatusMap();

        if (getHasErrorMessages())
            return ERROR;
        else
        {
        	//New status has been created successfully. Hence reset the name, description and statusColor values to null.
        	//That way it display no values into Add Execution Status screen.
        	setName(null);
        	setDescription(null);
        	setStatusColor(null);
            return SUCCESS;
        }

    }

    public String getStatusColor()
    {
        return statusColor;
    }

    public void setStatusColor(String statusColor)
    {
        this.statusColor = statusColor;
    }
    
	public Collection<ExecutionStatus> getStepExecutionStatusList(){
		return JiraUtil.getStepExecutionStatusList();
	}

	public Boolean getAnalyticsCheck(){
		return JiraUtil.getZephyrAnalyticsFlag();
	}

	public String doAddAnalytics() {
		String zephyrAnalyticsCheck = ServletActionContext.getRequest().getParameter("zephyrAnalytics");
		String enabled = updateConfigProperty(ConfigurationConstants.ZEPHYR_ANALYTICS_SETTINGS, zephyrAnalyticsCheck);
		return enabled;
	}

	private String updateConfigProperty(String propertyName, String propertyValue) {
		try {
			JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
					.setString(propertyName, propertyValue);
		} catch (Exception e) {
			log.error("Exception while updating configuration:", e);
			addError("Exception Message", e.getMessage());
			return ERROR;
		}

		return SUCCESS;
	}
}
