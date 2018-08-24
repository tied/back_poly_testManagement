package com.thed.zephyr.util;


import com.google.common.collect.ImmutableMap;
import com.thed.zephyr.je.vo.FieldMetadata;
import com.thed.zephyr.je.vo.ImportFieldConfig;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


public interface ApplicationConstants {

    int UNSCHEDULED_VERSION_ID = -1;
    String UNSCHEDULED_VERSION_ID_AS_STRING = "-1";
    int UNEXECUTED_STATUS = -1;
    int SYSTEM_EXECUTION_STATUS_TYPE = 0;
    int CUSTOM_EXECUTION_STATUS_TYPE = 1;

    int ACADEMIC = 0;
    int COMMERCIAL = 1;
    int COMMUNITY = 2;
    int DEMONSTRATION = 3;
    int DEVELOPER = 4;
    int NON_PROFIT = 5;
    int OPEN_SOURCE = 6;
    int PERSONAL = 7;
    int STARTER = 8;
    int HOSTED = 9;

    String TESTSTEPRESULT_TYPE = "TESTSTEPRESULT";
    String SCHEDULE_TYPE = "SCHEDULE";
    String TEST_STEP_TYPE = "TESTSTEP";
    //Following key is used in IssueEventListner to find out the changes made by user on type "issuetype".
    String ISSUE_TYPE_KEY = "issuetype";

    String ERROR_ID = "errorId";
    String ERROR_DESC = "errorDesc";
    String ERROR_DESC_HTML = "errorDescHtml";

    //ERROR Codes
    int ZEPHYR_INVALID_LICENSE = 10;
    int ZEPHYR_EVAL_EXPIRED = 11;
    int ZEPHYR_ACTIVATE_LICENSE = 12;
    int ZEPHYR_NOT_LICNESED = 13;
    int ZEPHYR_USER_LIMIT_REACHED = 14;
    int ZEPHRY_UPDATES_NOT_AVAILABLE = 15;
    int ZEPHYR_LICENSE_COMPATIBILITY_ERROR = 16;
    int ZEPHYR_LICENSE_PUBLICKEY_ERROR = 17;
    int ZEPHYR_LICENSE_PUBLICPRIVATE_KEY_GEN_ERROR = 18;
    int ZEPHYR_LICENSE_KEY_READ_ERROR = 19;
    int ZEPHYR_VERSION_READING_ERROR = 20;
    int ZEPHYR_VERSION_UPGRADE_ERROR = 21;
    //CODE for General Errors other than above.
    int ZEPHYR_UNCATEGORIZED_LICENSE_EXCEPTION = 444;

    String ZFJ_PLUGIN_KEY = "com.thed.zephyr.je";
    int AD_HOC_CYCLE_ID = -1;
    /*To make it work with Confluence Gadgets, we are keeping this as a fixed constant*/
    int ALL_CYCLES_ID = -2;
    String AD_HOC_CYCLE_ID_AS_STRING = "-1";
    String AD_HOC_CYCLE_NAME = "Ad hoc";
    String UNSCHEDULED_VERSION_NAME = "Unscheduled";
    String NULL_VALUE = "-1";
    int MAX_IN_QUERY = 998;
    String ENCRYPTED_STRING = "AO-7DEABF";
    String ACCESS_ALL = "ALL";

    //Constants for Audit/Change logs
    String NEW = "NEW";
    String OLD = "OLD";
    String NULL = "NULL";

    // Date Formats
    String ZFJ_DATE_FORMAT = "MM-dd-yyyy";
    String DATE_FORMAT_SHORT = "dd/MMM/yy";
    String DATE_TIME_FORMAT_SHORT = "dd/MMM/yy hh:mm a";
    //Constants for JIRA 6.4 Project Centric View
    String PROJECT_CENTRIC_VIEW_FEATURE_DISABLED_KEY = "com.atlassian.jira.projects.ProjectCentricNavigation.Disabled";
    String PROJECT_CENTRIC_VIEW_EXECUTION_REPORT = "testcase-execution-report";
    String PROJECT_CENTRIC_VIEW_BURNDOWN_REPORT = "test-burndown-report";
    String PROJECT_CENTRIC_VIEW_TOP_DEFECTS_REPORT = "test-topdefects-report";

    int JOB_STATUS_INPROGRESS = 0;
    int JOB_STATUS_COMPLETED = 1;
    int JOB_STATUS_FAILED = 2;
    int JOB_STATUS_STOPPED = 3;
    String JOB_STATUS_STOP_STRING = "STOP";
    String IN_PROGRESS_JOBS_MAP = "in_progress_job_map";
    String COMPLETED_JOBS_MAP = "completed_job_map";
    Long DEFAULT_EXPIRE_TIME_FOR_IN_PROGRESS_JOB_PROGRESS = 10L;
    Long DEFAULT_EXPIRE_TIME_FOR_COMPLETED_JOB_PROGRESS = 1L;
    int DEFAULT_COMPLETED_JOB_MAP_SIZE = 1000;
    int MAX_LIFE_TIME_FOR_JOBS_DURING_CLEAN_COMPLETED_JOB_MAP = 1200000;//in ms
    String CLEAN_JOB_PROGRESS_COMPLETED_MAP_LOCK_KEY = "clean_job_progress_completed_map_lock_key";

    String UPDATE_BULK_EXECUTION_STATUS_LOCK = "update_bulk_execution_status";

    String ADD_TESTS_TO_CYCLE_JOB_PROGRESS = "add_tests_to_cycle_job_progress";
    String UPDATE_BULK_EXECUTION_STATUS_JOB_PROGRESS = "update_bulk_execution_status_job_progress";
    String BULK_EXECUTION_ASSIGN_CF_JOB_PROGRESS = "bulk_execution_customfields_job_progress";
    String JOB_PROGRESS_TOKEN = "jobProgressToken";
    String BULK_EXECUTION_COPY_MOVE_JOB_PROGRESS = "bulk_execution_copy_move_job_progress";
    String BULK_EXECUTION_ASSIGN_USER_JOB_PROGRESS = "bulk_execution_assign_user_job_progress";
    String BULK_EXECUTIONS_DELETE_JOB_PROGRESS = "bulk_executions_delete_job_progress";
    String BULK_EXECUTION_ASSOCIATE_DEFECT_JOB_PROGRESS = "bulk_execution_associate_defect_job_progress";
    String CYCLE_DELETE_JOB_PROGRESS = "cycle_delete_job_progress";
    String CYCLE_ENTITY = "Cycle";
    String REINDEX_JOB_PROGRESS = "reindex_job_progress";
    String ERROR = "error";
    String EXECUTION = "execution";
    String MASKED_DATA = "XXXXX";

    String PROJECT_IDX = "projid";
    String VERSION_IDX = "version";
    String CYCLE_IDX = "CYCLE_ID";
    String FOLDER_IDX = "FOLDER_ID";
    String TOTAL_DEFECT_COUNT_IDX = "TOTAL_DEFECT_COUNT_IDX";
    String MOVE_EXECUTIONS_FROM_CYCLE_TO_FOLDER_JOB_PROGRESS = "move_executions_from_cycle_to_folder_job_progress";
    String TOTAL_FOLDERS = "totalFolders";
    String TOTAL_DEFECTS = "totalDefects";
    long AD_HOC_CYCLE_ID_LONG = -1L;
    long ADHOC_SYSTEM_FOLDER_ID = -2L;
    String ADHOC_SYSTEM_FOLDER_NAME = "System";
    String ADHOC_SYSTEM_FOLDER_DESCRIPTION = "System folder for adhoc cycle.";
    int MAX_LIMIT = 1000;

	String SUPPORTTOOL_LOCK = "supportToolLock";

    
    String ZFJ_SHARED_HOME_PATH = "/zfj";
    String ZIP_EXTENSION = ".zip";
    String REINDEX_ALL_ZIP_FILE_NAME = ZFJ_SHARED_HOME_PATH + "/reindexAll" + ZIP_EXTENSION;
    String REINDEX_BY_PROJECT_FILE_NAME = ZFJ_SHARED_HOME_PATH + "/reindexPrj" + ZIP_EXTENSION;
    String ZFJ_SUPPORTTOOL = "/supportTool";
    
    String ZIC_TOTAL_EXECUTION_COUNT_DB = "zicTotalExecutionCountdb";
    String ZIC_TOTAL_EXECUTION_COUNT = "zicTotalExecutionCount";
    String ZIC_TOTAL_CYCLE_COUNT = "zicTotalCycleCount";
    String ZIC_EXECUTION_COUNT_BY_CYCLE = "zicExecutionCountByCycle";
    String ZIC_EXECUTION_COUNT_BY_FOLDER = "zicExecutionCountByFolder";
    String ZIC_ISSUE_COUNT_BY_PROJECT = "zicIssueCountByProject";
    String ZIC_TESTSTEP_RESULT_COUNT_BY_EXECUTION = "zicTeststepResultCountByExecution";
    String ZIC_TESTSTEP_COUNT_BY_ISSUE = "zicTeststepCountByIssue";
    String TAB_ACTIVE = "tabActive";
    
    String COPY_TESTSTEPS_FROM_SOURCE_TO_DESTINATION = "copy_teststeps_from_source_to_destination";
    Object PROJECT_ID = "PROJECT_ID";
    String TESTCASES = "testcases";
    String PROJECTS = "projects";
    String VERSIONS = "versions";
    String CYCLES = "cycles";
    String DEFECTS = "defects";
    String ATTACHMENTS = "attachments";
    String EXECUTIONS = "executions";
    String VERSION_ID = "VERSION_ID";
    String FOLDER_ENTITY = "Folder";
    String FOLDER_CLONE_JOB_PROGRESS = "folder_clone_job_progress";
    Integer REINDEX_BATCH_SIZE = 50000;
    Integer MAX_PROJECT_ID = 50;
    String PROJECT_ID_IDX = "PROJECT_ID";
    String ISSUE_ID_IDX = "ISSUE_ID";

    String CYCLE_ID_ENTITY = "cycleId";
    String FOLDER_ID_ENTITY = "folderId";
    String CREATE_CYCLE = "Create Cycle";
    String ADD_FOLDER = "Add Folder";
    String ADD_TEST_TO_CYCLE = "Add Test to Cycle";
    String UPDATE_EXECUTION_STATUS = "Update Execution Status";

    // Importer related constants start
    int EXTRA_ROWS_IN_END = 2;
    String IMPORT_JOB_NEW = "11001";
    String IMPORT_JOB_NORMALIZATION_IN_PROGRESS = "11002";
    String IMPORT_JOB_NORMALIZATION_SUCCESS = "11003";
    String IMPORT_JOB_NORMALIZATION_FAILED = "11004";
    String IMPORT_JOB_IMPORT_IN_PROGRESS = "11005";
    String IMPORT_JOB_IMPORT_FAILED = "11006";
    String IMPORT_JOB_IMPORT_SUCCESS = "11007";
    String IMPORT_JOB_IMPORT_PARTIAL_SUCCESS = "11008";

    HashMap<String, ImportFieldConfig> excelFieldConfigs = new LinkedHashMap<String, ImportFieldConfig>();
    HashMap<String, ImportFieldConfig> xmlFieldConfigs = new LinkedHashMap<String, ImportFieldConfig>();
    HashMap<String, FieldMetadata> fieldTypeMetadataMap = new LinkedHashMap<String, FieldMetadata>();
    String ISSUE_KEY_REGEX = "((?<!([A-Z]{1,10})-?)[A-Z]+-\\d+)";
    String TESTSTEP_ORDER = "stepOrder";
    String TESTSTEP_ACTION = "stepAction";
    String TESTSTEP_DATA = "stepData";
    String TESTSTEP_EXPECTED_RESULTS = "stepExpectedResults";
    String TESTSTEP_CREATED_BY = "stepCreatedBy";

    String JIRA_FIELD_SUMMARY = "summary";
    String JIRA_FIELD_EXTERNAL_ID = "externalid";
    String JIRA_FIELD_LABELS = "labels";
    String JIRA_FIELD_COMMENTS = "comments";
    String JIRA_FIELD_FIX_VERSIONS = "fixVersions";
    String JIRA_FIELD_AFFECTED_VERSIONS = "versions";
    String JIRA_FIELD_COMPONENTS = "components";
    String JIRA_FIELD_PRIORITY = "priority";
    String JIRA_FIELD_ASSIGNEE = "assignee";
    String JIRA_FIELD_DESCRIPTION = "description";
    String JIRA_FIELD_ISSUE_KEY = "issueKey";
    String JIRA_FIELD_DUE_DATE = "dueDate";
    String JIRA_FIELD_PARENT = "parent";

    String IMPORT_CREATE_ISSUES_JOB_PROGRESS = "import_create_issues_job_progress";
    String EXCEL_DISCRIMINATOR_BY_EMPTY_ROW = "By Empty Row";
    String EXCEL_DISCRIMINATOR_BY_SHEET = "By Sheet";
    String EXCEL_DISCRIMINATOR_BY_ID_CHANGE = "By ID Change";
    String EXCEL_DISCRIMINATOR_BY_TESTNAME_CHANGE = "By Testcase Name Change";

    String IMPORT_JOB_PROCESS_SUCCESS_STATUS = "Successful";
    String IMPORT_JOB_PROCESS_FAIL_STATUS = "Failed";

    int IMPORT_JOB_THREAD_POOL_SIZE = 10;
    int IMPORTER_MAX_UPLOAD_FILE_SIZE = 5;//MB
    String ALL_SHEET_REGEX = ".*";
    // Importer related constants end
    String DATABASE_COUNT = "dbCount";
    String INDEX_COUNT = "indexCount";
    String CURRENT_NODE_ID = "nodeId";
    String CURRENT_NODE_IP = "nodeIp";

    enum ENTITY_TYPE {
        TESTSTEP, EXECUTION
    }

    Integer EXECUTION_CUSTOM_FIELD_GLOBAL_LEVEL = new Integer(20);
    Integer TEST_STEP_CUSTOM_FIELD_GLOBAL_LEVEL = new Integer(5);

    String STRING_VALUE = "STRING_VALUE";
    String NUMBER_VALUE = "NUMBER_VALUE";
    String DATE_VALUE = "DATE_VALUE";
    String DATE_TIME_VALUE = "DATE_TIME_VALUE";
    String LARGE_VALUE = "LARGE_VALUE";
    String LIST_VALUE = "LIST_VALUE";

    String RADIO_BUTTON = "RADIO_BUTTON";
    String CHECKBOX = "CHECKBOX";
    String SINGLE_SELECT = "SINGLE_SELECT";
    String MULTI_SELECT = "MULTI_SELECT";

    /**
     * This map will hold the mapping of custom field value type to database column type.
     */
    Map<String, String> CUSTOM_FIELD_VALUE_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("RADIO_BUTTON", LIST_VALUE)
            .put("CHECKBOX", LIST_VALUE)
            .put("SINGLE_SELECT", LIST_VALUE)
            .put("MULTI_SELECT", LIST_VALUE)
            .put("NUMBER", NUMBER_VALUE)
            .put("DATE", DATE_VALUE)
            .put("DATE_TIME", DATE_TIME_VALUE)
            .put("TEXT", STRING_VALUE)
            .put("LARGE_TEXT", LARGE_VALUE)
            .build();


    Map<String, String> CUSTOM_FIELD_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("RADIO_BUTTON", RADIO_BUTTON)
            .put("CHECKBOX", CHECKBOX)
            .put("SINGLE_SELECT", SINGLE_SELECT)
            .put("MULTI_SELECT", MULTI_SELECT)
            .put("NUMBER", "NUMBER")
            .put("DATE", "DATE")
            .put("DATE_TIME", "DATE_TIME")
            .put("TEXT", "TEXT")
            .put("LARGE_TEXT", "LARGE_TEXT")
            .build();

    String DECIMAL_NUMBER_REGEXP = "^-?[0-9.]*$";

    String MAX_ALLOWED_NUMBER_VAL = "100000000000000";
    String MIN_ALLOWED_NUMBER_VAL = "-100000000000000";

    //Fetch result max count
    Integer MAX_RESULT = 10;

    Double MAX_VAL = Double.valueOf(MAX_ALLOWED_NUMBER_VAL);
    Double MIN_VAL = Double.valueOf(MIN_ALLOWED_NUMBER_VAL);

    String FOLDER_LEVEL_ESTIMATED_TIME = "folderLevelEstimatedTime";
    String FOLDER_LEVEL_LOGGED_TIME = "folderLevelLoggedTime";
    String FOLDER_LEVEL_EXECUTIONS_LOGGED = "folderLevelExecutionsToBeLogged";
	
    String INDEX_BACKUP_FOLDER_NAME="/index_backup";
	String BACKUP_PROJECT_FILE_NAME = ZFJ_SHARED_HOME_PATH + INDEX_BACKUP_FOLDER_NAME + ZIP_EXTENSION;
	String SUCCESS = "success";
	String FAIL = "fail";
	
	String APPLICATION_LOG = "zfjApplicationLog";
	String DB_CLUSTER_CONFIG = "zfjdb_Cluster_Config";
	String TOMCAT_LOG = "zfjTomcatLog";
	String CATALINA = "catalina";
	String LOG = "log";
	String LOGS = "logs";
	String ON = "ON";
	String OFF = "OFF";
	String ZFJ = "zfj";
	String ZFJ_SUPPORT_TOOL = "ZFJ_SUPPORT_Tool_";
	String ZFJ_SUPPORT_CHECKLIST[] = new String[] {"zfjlogs","zfjdb","zfjshared","zfjtomcatlog"};
	String ZFJ_APP_LOG = "zephyr-jira.log";
	String DB_CONFIG = "dbconfig.xml";
	String CLUSTER_CONFIG = "cluster.properties";
	
	
	String JIRA = "JIRA";
	String ZFJVAL = "ZFJ";
	String INDEX_SNAPSHOT = "IndexSnapshot_";
	Integer TROUBLESHOOT_JIRA_VERSION = 743;
	

    String POSTGRES_DB = "postgres";
    String MSSQL_DB = "mssql";

    String TOTAL_COUNT = "totalCount";
    String DATA  = "data";


    String INTEGRITY_CHECKER = "IntegrityChecker";
    String IC_CYCLE_ID = "Cycle ID";
    String IC_FOLDER_ID = "Folder ID";
    String IC_PROJECT_ID = "Project ID";
    String IC_EXECUTION_ID = "Execution ID";
    String IC_ISSUE_ID = "Issue ID";
    String IC_EXECUTION_COUNT_AGAINST_DB_COUNT = "ExecutionCountAgainstDbCount";
    String IC_EXECUTION_COUNT_BY_CYCLE = "ExecutionCountByCycle";
    String EXECUTION_COUNT_BY_FOLDER = "ExecutionCountByFolder";
    String IC_ISSUE_COUNT_BY_PROJECT = "IssueCountByProject";
    String IC_TESTSTEP_COUNT_BY_EXECUTION = "TeststepCountByExecution";
    String IC_TESTSTEP_COUNT_BY_ISSUE = "TeststepCountByIssue";
    String IC_CYCLES_COUNT = "Cycles Count";
    String IC_CYCLE_COUNT = "CyclesCount";
    String IC_INDEXED_EXECUTION_COUNT = "Indexed Execution Count";
    String IC_EXECUTION_COUNT_FROM_DATABASE = "Execution Count from Database";
    String IC_STATUS = "Status";
    
    String NO_DATA = "No Data";

    String AM = "am";
	String PM = "pm";
	String EMPTY = "";
	String CRON_ERROR = "Invalid Cron Expression";

}
