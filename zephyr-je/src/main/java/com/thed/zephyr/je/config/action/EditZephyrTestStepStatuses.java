package com.thed.zephyr.je.config.action;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;


public class EditZephyrTestStepStatuses extends JiraWebActionSupport {
	
    private String id;
    private String name;
    private String description;
    private String color;
    private final ZephyrLicenseManager zLicenseManager;
	
	public EditZephyrTestStepStatuses(ZephyrLicenseManager zLicenseManager){
		this.zLicenseManager = zLicenseManager;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	@Override
    protected void doValidation()
    {
        if (!TextUtils.stringSet(getName()))
            addError("name", getText("zephyr.errors.must.specify.name"));

        if (!TextUtils.stringSet(getColor()) || (!JiraUtil.validateHexColor(getColor())) )
            addError("color", getText("zephyr.errors.must.specify.color"));

        super.doValidation();
    }
	
	@Override
	public String doDefault() throws Exception {

		ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());

    	Map<Integer, ExecutionStatus> stepExecStatusMap = JiraUtil.getStepExecutionStatuses();
    	ExecutionStatus execStatusObj = stepExecStatusMap.get(new Integer(getId()));
    	//if execStatusObj is null that means user entered wrong status ID, may be by just changing the URL!
    	if(execStatusObj == null)
    		return getRedirect(getRedirectPage());
    	
    	setName(execStatusObj.getName());
    	setDescription(execStatusObj.getDescription());
    	setColor(execStatusObj.getColor());

		return INPUT;
	}

    @Override
	protected String doExecute() throws Exception
    {
    	doValidation();
        if(getHasErrors())
        	return ERROR;

    	Integer intObj = new Integer(getId());
    	log.debug("Execution status with id=" + getId() + " got edited");
    	
     	Map<Integer, ExecutionStatus> stepExecStatusMap = JiraUtil.getStepExecutionStatuses();
     	ExecutionStatus updateExecStatusObj = stepExecStatusMap.get(intObj);
    	stepExecStatusMap.remove(intObj);

    	//Rebuild the status object edited by Administrator.
    	ExecutionStatus newExecStatus = new ExecutionStatus(intObj, getName().toUpperCase(), getDescription(), getColor(), updateExecStatusObj.getType());
    	stepExecStatusMap.put(intObj, newExecStatus);
    	
    	//Rebuild the list.
    	Collection coll = stepExecStatusMap.values();
		ObjectMapper objMapper = new ObjectMapper();
		String execStatusesStringObject = null;
		
        try
        {
        	execStatusesStringObject = objMapper.writeValueAsString(coll);
        }
        catch (IOException e)
        {
        	log.debug("IOException: " + e.getMessage());
            throw new RuntimeException("Failed to save updated Test Step Execution Status List to JSON: " + coll, e);
        }

        log.info("Edited status. Storing updated status List back to JIRA.");
        JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
        							.setText(ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES,execStatusesStringObject );

        //Update ExecutionStatusMap to reflect changes.
        JiraUtil.buildStepExecutionStatusMap();

        if (getHasErrorMessages())
            return ERROR;
        else
            return getRedirect(getRedirectPage());
    }

    public String getRedirectPage(){
    	return "ViewZephyrTestStepStatuses!default.jspa";
    }
    
	
}


