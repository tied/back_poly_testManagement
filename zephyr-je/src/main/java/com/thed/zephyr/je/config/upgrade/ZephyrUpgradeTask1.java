package com.thed.zephyr.je.config.upgrade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.sal.api.message.Message;
import com.atlassian.sal.api.upgrade.PluginUpgradeTask;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

public class ZephyrUpgradeTask1 implements PluginUpgradeTask{

	protected final I18nHelper i18n;
	private static final Logger log = Logger.getLogger(ZephyrUpgradeTask1.class);
	
	public ZephyrUpgradeTask1(JiraAuthenticationContext authenticationContext) {

		i18n = authenticationContext.getI18nHelper();
	}
	
	@Override
	/**
     * The build number for this upgrade task. Once this upgrade task has run the plugin manager will store this
     * build number against this plugin type.  After this only upgrade tasks with higher build numbers will be run
     */
	public int getBuildNumber() {
		return 1;
	}

	@Override
	public String getShortDescription() {
		return "Adds Step Execution statuses";
	}

	@Override
	public Collection<Message> doUpgrade() throws Exception {
		log.info("Performing upgrade task - step execution statuses will be added.");
		
		String defaultStatuses = JiraUtil.getPropertySet(
				ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(
				ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES);
		
		//Second condition is mainly for development / testing purposes.
		//If string is empty array, then add the Executionstatues again.
		if(defaultStatuses == null || defaultStatuses.equals("[]")){
			//First create all default execution status json objects.
			List<ExecutionStatus> aList = new ArrayList<ExecutionStatus>();

			/*
			 * We are not able to get the instance of i18n during upgrade Task.
			 * Hence we will use text directly.
			ExecutionStatus pass = new ExecutionStatus(1, i18n.getText("zephyr.je.admin.plugin.step.execstatus.pass.title"), i18n.getText("zephyr.je.admin.plugin.step.execstatus.pass.desc"), "#75B000",0);
			ExecutionStatus fail = new ExecutionStatus(2, i18n.getText("zephyr.je.admin.plugin.step.execstatus.fail.title"), i18n.getText("zephyr.je.admin.plugin.step.execstatus.fail.desc"),"#CC3300", 0);
			ExecutionStatus wip = new ExecutionStatus(3, i18n.getText("zephyr.je.admin.plugin.step.execstatus.wip.title"), i18n.getText("zephyr.je.admin.plugin.step.execstatus.wip.desc"), "#F2B000",0 );
			ExecutionStatus blocked = new ExecutionStatus(4, i18n.getText("zephyr.je.admin.plugin.step.execstatus.blocked.title"), i18n.getText("zephyr.je.admin.plugin.step.execstatus.blocked.desc"), "#6693B0", 0);
			ExecutionStatus unexecuted = new ExecutionStatus(-1, i18n.getText("zephyr.je.admin.plugin.step.execstatus.unexecuted.title"), i18n.getText("zephyr.je.admin.plugin.step.execstatus.unexecuted.desc"), "#A0A0A0", 0);
			*/
			
			ExecutionStatus pass = new ExecutionStatus(1, "PASS", "Test step was executed and passed successfully", "#75B000",0);
			ExecutionStatus fail = new ExecutionStatus(2, "FAIL", "Test step was executed and failed.","#CC3300", 0);
			ExecutionStatus wip = new ExecutionStatus(3, "WIP", "Test step execution is a work-in-progress.", "#F2B000",0 );
			ExecutionStatus blocked = new ExecutionStatus(4, "BLOCKED", "The Test step execution of this test was blocked for some reason.", "#6693B0", 0);
			ExecutionStatus unexecuted = new ExecutionStatus(-1, "UNEXECUTED", "The Test step has not yet been executed.", "#A0A0A0", 0);

			aList.add(pass);
			aList.add(fail);
			aList.add(wip);
			aList.add(blocked);
			aList.add(unexecuted);
			
			ObjectMapper objMapper = new ObjectMapper();
			String execStatusesStringObject = null;
			
            try
            {
            	execStatusesStringObject = objMapper.writeValueAsString(aList);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to save Step ExecutionStatus List to JSON: " + aList, e);
            }

            JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
            							.setText(ConfigurationConstants.ZEPHYR_STEP_EXECUTION_STATUSES,execStatusesStringObject );

		}
		
       JiraUtil.buildStepExecutionStatusMap();
       return null;
	}

	/**
     * Identifies the plugin that will be upgraded.
     */
	@Override
	public String getPluginKey() {
		return "com.thed.zephyr.je";
	}

}
