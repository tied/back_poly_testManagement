package com.thed.zephyr.zapi.util;

public class ApplicationConstants {
	
	public static final int UNSCHEDULED_VERSION_ID = -1;
	public static final int UNEXECUTED_STATUS = -1;
	public static final int SYSTEM_EXECUTION_STATUS_TYPE = 0;
	public static final int CUSTOM_EXECUTION_STATUS_TYPE = 1;

	public static final int ACADEMIC = 0;
	public static final int COMMERCIAL = 1;
	public static final int COMMUNITY = 2;
	public static final int DEMONSTRATION = 3;
	public static final int DEVELOPER = 4;
	public static final int NON_PROFIT = 5;
	public static final int OPEN_SOURCE = 6;
	public static final int PERSONAL = 7;
	public static final int STARTER = 8;
	public static final int HOSTED = 9;
	
	public static final String TESTSTEPRESULT_TYPE = "TESTSTEPRESULT";
	public static final String SCHEDULE_TYPE = "SCHEDULE";
	//Following key is used in IssueEventListner to find out the changes made by user on type "issuetype".
	public static final String ISSUE_TYPE_KEY="issuetype";
	
	public static final String ERROR_ID = "errorId";
	public static final String ERROR_DESC = "errorDesc";
	public static final String ERROR_DESC_HTML = "errorDescHtml";
	
	//ERROR Codes
	public static final int ZEPHYR_INVALID_LICENSE=10;
	public static final int ZEPHYR_EVAL_EXPIRED = 11;
	public static final int ZEPHYR_ACTIVATE_LICENSE = 12;
	public static final int ZEPHYR_NOT_LICNESED = 13;
	public static final int ZEPHYR_USER_LIMIT_REACHED = 14;
	public static final int ZEPHRY_UPDATES_NOT_AVAILABLE = 15;
	public static final int ZEPHYR_LICENSE_COMPATIBILITY_ERROR = 16;
	public static final int ZEPHYR_LICENSE_PUBLICKEY_ERROR = 17;
	public static final int ZEPHYR_LICENSE_PUBLICPRIVATE_KEY_GEN_ERROR= 18;
	public static final int ZEPHYR_LICENSE_KEY_READ_ERROR = 19;
	public static final int ZEPHYR_VERSION_READING_ERROR = 20;
	public static final int ZEPHYR_VERSION_UPGRADE_ERROR = 21;
	//CODE for General Errors other than above.
	public static final int ZEPHYR_UNCATEGORIZED_LICENSE_EXCEPTION=444;
	
	public static final String ZFJ_PLUGIN_KEY="com.thed.zephyr.je";
	public static final int AD_HOC_CYCLE_ID = -1;
	
	public static final String NULL_VALUE = "-1";
	public static final int MAX_IN_QUERY = 998;
	public static final String ENCRYPTED_STRING= "X-ENC"; 
}
