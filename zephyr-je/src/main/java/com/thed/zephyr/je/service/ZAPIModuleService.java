package com.thed.zephyr.je.service;

public interface ZAPIModuleService {
	/**
	 * Enables ZAPI Module accessible rest service. Needs the Module Key to be passed in.
	 * @param completeKey
	 * @return
	 */
	boolean enableZAPIModule(String completeKey);
	
	/**
	 * Disables ZAPI Module accessible rest service. Needs the Module Key to be passed in.
	 * @param completeKey
	 * @return
	 */
	boolean disableZAPIModule(String completeKey);
}
