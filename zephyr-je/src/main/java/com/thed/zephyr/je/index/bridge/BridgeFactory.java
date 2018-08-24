package com.thed.zephyr.je.index.bridge;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.converters.DoubleConverterImpl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ZephyrComponentAccessor;

public class BridgeFactory {
	
	@SuppressWarnings("rawtypes")
	public static JEFieldBridge extractType(Class impl) throws Exception {
		JEFieldBridge bridge = null;
		if (impl != null) {
			try {
				ScheduleManager scheduleManager = getScheduleManager();
				StepResultManager stepResultManager = getStepResultManager();
				CustomFieldValueResourceDelegate customFieldValueResourceDelegate = getCustomFieldResourceDelegate();
				ZephyrCustomFieldManager zephyrCustomFieldManager = getZephyrCustomFieldManager();

				if (ExternalEntityFieldBridge.class.isAssignableFrom( impl ) ) {
					bridge = new ExternalEntityFieldBridge(ComponentAccessor.getIssueManager(),scheduleManager,stepResultManager);
				} else if (DateToStringFieldBridge.class.isAssignableFrom( impl ) ) {
					bridge = new DateToStringFieldBridge();
				} else if (CustomFieldEntityFieldBridge.class.isAssignableFrom(impl)) {
					bridge =  new CustomFieldEntityFieldBridge(customFieldValueResourceDelegate,zephyrCustomFieldManager, new DoubleConverterImpl(ComponentAccessor.getJiraAuthenticationContext()));
				}
				return bridge;
			}
			catch (Exception e) {
				throw new Exception( "Unable to instantiate FieldBridge for " + impl.getName(), e );
			}
		}
		return null;
	}

	private static ScheduleManager getScheduleManager() {
        return (ScheduleManager)ZephyrComponentAccessor.getInstance().getComponent("schedule-manager");
    }
	
	private static StepResultManager getStepResultManager() {
        return (StepResultManager)ZephyrComponentAccessor.getInstance().getComponent("stepresult-manager");
    }

	private static ZephyrCustomFieldManager getZephyrCustomFieldManager() {
		return (ZephyrCustomFieldManager)ZephyrComponentAccessor.getInstance().getComponent("zephyrcf-manager");
	}

	private static CustomFieldValueResourceDelegate getCustomFieldResourceDelegate() {
		return (CustomFieldValueResourceDelegate) ZephyrComponentAccessor.getInstance().getComponent("customFieldValueResourceDelegate");
	}
}
