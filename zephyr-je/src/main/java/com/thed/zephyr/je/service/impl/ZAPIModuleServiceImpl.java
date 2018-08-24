/**
 * 
 */
package com.thed.zephyr.je.service.impl;

import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.thed.zephyr.je.service.ZAPIModuleService;

/**
 * @author niravshah
 *
 */
public class ZAPIModuleServiceImpl implements ZAPIModuleService {
    protected final Logger log = Logger.getLogger(ZAPIModuleServiceImpl.class);
	//private final String restKey = "com.thed.zephyr.je:zephyr-je-zapi-rest";

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.service.ZAPIModuleService#enableZAPIModule(java.lang.String)
	 */
	@Override
	public boolean enableZAPIModule(String completeKey) {
		try {
			ComponentAccessor.getPluginController().enablePluginModule(completeKey);
			return true;
		} catch(Exception e) {
			log.error("Error enabling ZAPI Rest Resource",e);
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see com.thed.zephyr.je.service.ZAPIModuleService#disableZAPIModule(java.lang.String)
	 */
	@Override
	public boolean disableZAPIModule(String completeKey) {
		try {
			ComponentAccessor.getPluginController().disablePluginModule(completeKey);
			return true;
		} catch(Exception e) {
			log.error("Error disabling ZAPI Rest Resource",e);
			return false;
		}
	}

}
