package com.thed.zephyr.util;

import com.thed.zephyr.je.config.customfield.CustomFieldMetadata;


public class ConfigurationConstants {
	

	/**
	 * There is a way you can access information form PluginKey from com.atlassian.plugin.ModuleDescriptor	
	 * Need to find out how to get reference to this class. 
	 * Reference: http://forums.atlassian.com/thread.jspa?messageID=257334087
	 */
	public static final String PLUGIN_KEY = "com.thed.zephyr.je";
	
	/**
	 * Entity name that is used as key to store configuration data (e.g. issueId, customFieldId etc)
	 */
	public static final String ZEPHYR_ENTITY_NAME = "ZephyrPlugin";
	public static final Long ZEPHYR_ENTITY_ID = new Long(1l);

	public static final String ZEPHYR_ISSUETYPE_KEY = "zephyr.issuetype.testcase";
	public static final String ZEPHYR_ISSUETYPE_KEY_DESCRIPTION = "zephyr.issuetype.testcase.desc";
	public static final String ZEPHYR_DISPLAY_WORKFLOW_SETTINGS = "zephyr.display.workflow.settings";
	public static final String ZEPHYR_DISPLAY_VERSION_SETTINGS = "zephyr.display.version.settings";
	public static final String ZEPHYR_DISPLAY_PERMISSION_SETTINGS = "zephyr.display.permission.settings";
	public static final String ZEPHYR_DISPLAY_LOG_LEVEL_SETTINGS = "zephyr.display.loglevel.settings";
	public static final String ZEPHYR_PERMISSION_GROUP_CREATE_SETTINGS = "zephyr.permission.group.create.settings";
	public static final String ZEPHYR_LINK_ISSUE_TO_TESTCASE = "zephyr.link.issue.to.testcase";
	public static final String ZEPHYR_LINK_TESTCASE_TO_ISSUE = "zephyr.link.testcase.to.issue";
	public static final String ZEPHYR_LOG_LEVEL_INFO = "zephyr.loglevel.info";
	public static final String ZEPHYR_LOG_MAX_SIZE = "zephyr.log.max.size";
	public static final String ZEPHYR_LOG_MAX_BACKUP = "zephyr.log.max.backup";
	public static final String ZEPHYR_LOG_DEFAULT_MAX_SIZE = "20";
	public static final String ZEPHYR_LOG_DEFAULT_MAX_BACKUP = "5";
	public static final String MEGABYTE = "MB";
	public static final String ZEPHYR_REMOTE_LINK_ISSUE_TO_EXECUTION = "zephyr.remote.link.issue.to.execution";
	public static final String ZEPHYR_ENABLE_ISSUE_SECURITY = "zephyr.enable.issue.security";
	public static final String ZEPHYR_DISABLE_LABELS_TEST_SUMMARY_FILTER = "zephyr.disable.testsummary.labels.filter";
	public static final String ZEPHYR_DISABLE_ALL_TEST_SUMMARY_FILTER = "zephyr.disable.testsummary.all.filter";
	public static final String ZEPHYR_DISABLE_PROJECT_TESTMENU = "zephyr.disable.project.testmenu";
	public static final String ZEPHYR_DISABLE_PROJECT_EXEC_WORKFLOW = "zephyr.disable.project.execworkflow";

	public static final String ZEPHYR_LINK_TESTCASE_STEP_TO_ISSUE = "zephyr.link.testcase.step.to.issue";
	public static final String ZEPHYR_REMOTE_LINK_ISSUE_TO_STEP_EXECUTION = "zephyr.remote.link.issue.to.step.execution";
    public static final String ZEPHYR_SHOW_CREATE_ZEPHYR_TEST_ON_ISSUE_TYPES = "zephyr.show.create.zephyr.test.on.issue.types";

	public static final String ZEPHYR_ISSUE_TYPE_TEST_PROJECT_CREATE = "zephyr.issuetype.test.on.project.create";
	public static final String ZEPHYR_JE_CURRENT_VERSION = "zephyr.je.current.version";
    public static final String ZEPHYR_JE_PRODUCT_VERSION = "zephyr.je.product.version";

    public static final String ZEPHYR_CF_TESTSTEP_KEY = "zephyr.customfield.teststep";
    public static final String ZEPHYR_DASHBOARD_KEY = "zephyr.dashboard";

    public static final CustomFieldMetadata CF_TESTSTEP_METADATA = new CustomFieldMetadata("zephyr.customfield.teststep.name", 
																	"zephyr.customfield.teststep.desc",
																	PLUGIN_KEY + ":zephyr-je-customfield-teststep", 
																	PLUGIN_KEY + ":zephyr-je-customfield-teststep-searcher");

    public static final String ZEPHYR_EXECUTION_STATUSES = "zephyr.testcase.execution.statuses";
    public static final String ZEPHYR_STEP_EXECUTION_STATUSES = "zephyr.testcase.step.execution.statuses";
    public static final String ZEPHYR_ISSUE_LINK_RELATION = "zephyr.testcase.issue.link.relation";

	public static final String ZEPHYR_REQ_TO_TEST_LINK_RELATION = "zephyr.requirement.testcase.link.relation";
    public static final String ZEPHYR_REQ_TO_TEST_LINK_OLD_RELATION = "zephyr.requirement.testcase.link.old.relation";
	public static final String ZEPHYR_REQ_TO_TEST_LINK_NAME = "Test";
	public static final String ZEPHYR_REQ_TO_TEST_LINK_OUTWARD = "Is Tested By";
	public static final String ZEPHYR_REQ_TO_TEST_LINK_INWARD = "Tests";

    public static final String ZEPHYR_LICENSE = "ZephyrLicense";

	public static final String JOB_NAME = "Zephyr for JIRA background job";
	public static final String DELETION_SYNC_INDEX_JOB_NAME = "ZFJ:DeletionSyncIndex";
	public static final String INDEX_SYNC_INDEX_JOB_NAME = "ZFJ:SyncIndex";

    public static final String ZEPHYR_ZQL_RESULT_MAX_ON_PAGE = "zephyr.zql.search.result.max";

	public static final String ZAPI_PLUGIN_KEY = "com.thed.zephyr.je:zephyr-je-zapi-rest";
    public static final String ZEPHYR_REST_FILTER_MODULE_KEY = "com.thed.zephyr.je:zephyr-rest-filter";

	// DarkFeature constants
	public static final String ZEPHYR_DARK_FEATURE_PREFIX = "zephyr.lab.feature";
    public static final String ZEPHYR_PROJECT_PERMISSION_FEATURE_KEY = "zephyr.lab.feature.project.permissions";

	// Zephyr Custom Permission Constants
	public static final String ZEPHYR_PROJECT_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_PROJECT_PERMISSIONS";
	public static final String ZEPHYR_CYCLE_CREATE_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_CREATE_CYCLE";
	public static final String ZEPHYR_CYCLE_EDIT_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_EDIT_CYCLE";
	public static final String ZEPHYR_CYCLE_DELETE_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_DELETE_CYCLE";
	public static final String ZEPHYR_CYCLE_VIEW_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_VIEW_CYCLE";
	public static final String ZEPHYR_EXECUTION_CREATE_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_CREATE_EXECUTION";
	public static final String ZEPHYR_EXECUTION_EDIT_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_EDIT_EXECUTION";
	public static final String ZEPHYR_EXECUTION_DELETE_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_DELETE_EXECUTION";
	public static final String ZEPHYR_EXECUTION_VIEW_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_VIEW_EXECUTION";
	public static final String ZEPHYR_EXECUTION_COMMENT_PERMISSION_MODULE_KEY = "com.thed.zephyr.je:ZEPHYR_COMMENT_EXECUTION";
    public static final String ZEPHYR_UPDATE_EXECUTION_EXECUTED_ON = "com.thed.zephyr.je:ZEPHYR_UPDATE_EXECUTION_EXECUTED_ON";
    
    public static final String NEWNODE_SYNC_INDEX_JOB_NAME = "ZFJ:NewNodeSyncIndex";

	public static final String ZEPHYR_ANALYTICS_SETTINGS = "zephyr.analytics.settings";
    public static final String BACKUPINDEXFILES_JOB_NAME = "ZFJ:BackUpIndexFiles"; 
}
