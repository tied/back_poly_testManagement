package com.thed.zephyr.zapi.util;



public class ConfigurationConstants {
	

	/**
	 * There is a way you can access information form PluginKey from com.atlassian.plugin.ModuleDescriptor	
	 * Need to find out how to get reference to this class. 
	 * Reference: http://forums.atlassian.com/thread.jspa?messageID=257334087
	 */
	public static final String PLUGIN_KEY = "com.thed.zephyr.zapi";
	
	/**
	 * Entity name that is used as key to store configuration data (e.g. issueId, customFieldId etc)
	 */
	public static final String ZEPHYR_ENTITY_NAME = "ZAPIPlugin";
	public static final Long ZEPHYR_ENTITY_ID = new Long(1l);
    public static final String ZEPHYR_LICENSE = "ZAPILicense";
    public static final String ZEPHYR_JE_PRODUCT_VERSION = "zephyr.je.product.version";
}
