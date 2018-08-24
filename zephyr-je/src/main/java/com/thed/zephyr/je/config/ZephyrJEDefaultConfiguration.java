package com.thed.zephyr.je.config;

import com.atlassian.gadgets.dashboard.Color;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfigScheme;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenTab;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.portal.PortalPage;
import com.atlassian.jira.portal.PortalPageManager;
import com.atlassian.jira.portal.PortletConfigurationManager;
import com.atlassian.jira.sharing.SharedEntity;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.conditions.IsProjectPresentAndUserLoggedInCondition;
import com.thed.zephyr.je.config.customfield.CustomFieldException;
import com.thed.zephyr.je.config.customfield.CustomFieldMetadata;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import net.jcip.annotations.GuardedBy;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

//Reference Class: com.pyxis.greenhopper.jira.configurations.ScrumDefaultConfiguration.java
public class ZephyrJEDefaultConfiguration {

	protected static final Logger log = Logger.getLogger(ZephyrJEDefaultConfiguration.class);
	public static final String ID = "ZEPHYR_ID";
	public static Boolean initialized = false;

	protected final I18nHelper i18n;
	private PortletConfigurationManager pConfigurationManager;
	private UserPreferencesManager userPreferencesManager;

	public ZephyrJEDefaultConfiguration( final PortletConfigurationManager pConfigurationManager,
						final UserPreferencesManager userPreferencesManager) {

		this.pConfigurationManager = pConfigurationManager;
		this.userPreferencesManager = userPreferencesManager;
		i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
	}

	public String getId() {
		return ID;
	}

	public void init() {
		log.debug("JZDefaultConfiguration - init");

		//By default, set version check to true
        updateVersionCheckFirstTime();
        //Set database version
		String zephyrJEVersion = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);
		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
					.setString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION, zephyrJEVersion );
	}

    /**
	 * This function verifies if all configuration parameters are set properly or not.
	 * If not, it will reset back to default values.
	 * Other configuration reInitialization should be done from Admin Configuration screen.
	 * @param pluginVersionInfo
	 */
	public void reInitalize(String pluginVersionInfo){
        updateVersionCheckFirstTime();
        String dbVersionInfo = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
										.getString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);

		if(!StringUtils.equals(dbVersionInfo, pluginVersionInfo)){
			JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
			.setString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION, pluginVersionInfo );
		}
			
	}
	
	/**
	 * Gets called from <code>IsProjectPresentAndUserLoggedInCondition</code> in lifeCycle method onStart
	 * @see IsProjectPresentAndUserLoggedInCondition
	 */
	@GuardedBy("initialized")
	public void postInit(){
        updateVersionCheckFirstTime();
		String zephyrDashboardId = JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
				   .getString(ConfigurationConstants.ZEPHYR_DASHBOARD_KEY);

		if(zephyrDashboardId == null){
			log.debug("Create Dashboard for first time");
			createZephyrDashboard();
		}
		
		String testCaseTypeId = getZephyrIssueTypeId();
		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, 1l).setString(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY,
						testCaseTypeId);

		createZephyrCustomField(testCaseTypeId);
		
		//Building statuses after plugin enabled in ZephyrJELauncher.afterPropertiesSet() doesn't help.
		//The reason is zephyr JE specific i18n messages are not available though plugin is enabled!
		//Hence let's do this action also here after user enters valid license!
		buildZephyrExecutionStatuses();

		/* May use it in future */
//		JiraUtil.checkAndCreateReqToTestTypeRelation(false);
		initialized = true;
	}

    /**
     * Creates versionDisplay property in DB and initialize it with true, if its not already been created
     */
    private void updateVersionCheckFirstTime() {
        if(!JiraUtil.getVersionCheck().isDefined()){
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
                    .setString(ConfigurationConstants.ZEPHYR_DISPLAY_VERSION_SETTINGS,"true" );
        }
    }

	public String getZephyrIssueTypeId() {
		return getIssueTypeId(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY, false, 1l);
	}

	private String getIssueTypeId(String key, boolean subTask, long position) {
		String typeId = JiraUtil.getTestcaseIssueTypeId();

		String name = i18n.getText(key);
		String testIssueTypeInJIRA = null;

		//Check if issue exists or not.
		for (IssueType issueType : JiraUtil.getAllIssueTypes()) {
			//log.error("IssueType Name - " + issueType.getName().toLowerCase());
			if(!issueType.isSubTask()
					&& (typeId != null && typeId.equals(issueType.getId())) ){
				
				testIssueTypeInJIRA = issueType.getId();
				log.debug("Issue type with same name, and id as stored in zephyr issuetype property found. Hence we are not creating new issue type.");
				break;
			}
			
//			if (issueType.getName().toLowerCase().indexOf(name.toLowerCase()) >= 0
//					&& (subTask && issueType.isSubTask() || !subTask && !issueType.isSubTask())) {
//				testIssueTypeInJIRA = issueType.getId();
//				//log.error("Found Issue Type with id - " + typeId + ", was looking for name " + name + " and current name is " + issueType.getName());
//				break;
//			}
		}
		
		if (( typeId == null) || (testIssueTypeInJIRA == null)) {
			log.debug("Zephyr IssueType is getting created...");
			typeId = createAndPersistIssueType(name, subTask, position);
		} 

		return typeId;
	}

	private String createAndPersistIssueType(String issueTypeName, boolean subTask, long position) {
		
		String newIssueTypeId = null;
		try{
			
			String issueTypeDescription = i18n.getText(ConfigurationConstants.ZEPHYR_ISSUETYPE_KEY_DESCRIPTION);
			String iconUrl = JiraUtil.buildIconURL("ico_zephyr_issuetype",".png");

			IssueType newIssueType = JiraUtil.getConstantsManager()
									.insertIssueType(issueTypeName, position, null,issueTypeDescription, iconUrl);
			log.info("Created IssueType - " + newIssueType.getName() + " Issue ID - " + newIssueType.getId());	
			newIssueTypeId = newIssueType.getId();

			IssueTypeSchemeManager issueTypeSchemeManager = JiraUtil.getIssueTypeSchemeManager();
			try{
					//As expected, rest of the setup like DefaultIssueTypeScheme creation and all, happens only after user enters valid license.
					//Hence if you are running our plugin first time using Atlassian PDK, adding our newly created IssueType to DefaultIssueTypeScheme (which is not yet exists) will fail.
					//So just catch the exception and log the error message.
					//Remember any operations related to scheme will get failed. For example calls like getAllSchemes() or getDefaultIssueTypeScheme() all are failed before Jira Setup Completion..
					//It would have been great if these methods returned null objects to avoid this way of Exception Catching!
				
					List<FieldConfigScheme> schemes = JiraUtil.getIssueTypeSchemeManager().getAllSchemes();
					for(FieldConfigScheme scheme : schemes){
						log.debug("Scheme name - " + scheme.getName());
					}
					
					if (!JiraUtil.isTypeAssociatedToDefaultScheme(newIssueTypeId)) {
							JiraUtil.getIssueTypeSchemeManager().addOptionToDefault(newIssueTypeId);
						}
					
			}catch(Exception e){
				//log.error("DefaultIssueTypeScheme doesn't exists. Probably this plugin is getting enabled before Jira Setup completion. Please do Jira Setup First and then again install this plugin ");
				e.printStackTrace();
			}
		}
		catch(CreateException e){
			//log.error("Error occured while creating new IssueType of type Test");
			e.printStackTrace();
		}
		
		//log.error("ISSUE TYPE GOT CREATED " + typeId);
		return newIssueTypeId;
	}

	/**
	 * 
	 * @param testCaseTypeId
	 * @return
	 */
	private CustomField createZephyrCustomField(String testCaseTypeId) {
		CustomField field = null;

		String id = JiraUtil.getPropertySet(
				ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getString(
				ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY);
		log.debug("Test step Custom Field ID is " + id);
		
		field = JiraUtil.getCustomFieldManager().getCustomFieldObject(id);
		log.debug("TESTSTEP Custom Field Object is " + field);
		
		if ((id == null) || (field == null)) {

			CustomFieldMetadata cfTeststepMetadata = ConfigurationConstants.CF_TESTSTEP_METADATA;
			field = createAndPersistField(
					cfTeststepMetadata.getFieldName(),
					cfTeststepMetadata.getFieldDescription(),
					cfTeststepMetadata.getFieldType(),
					cfTeststepMetadata.getFieldSearcher(), 
					//if CustomField is not associated to all Issue Types, then searcher will not show up in "Simple Mode" Issue Navigator.
					//Only way is make Custom Field available to all Issue Types rather than just Test Issue Type.
					//Since our Custom Field dont have view and edit vm associated, for all other issue types this custom field will be invisible!
					// "-1" denotes Any Issue Type i.e. this Custom Field will be associated to all issue types.
					new String[] { testCaseTypeId });
//					new String[] { "-1" });

			log.info("New Custom Field is created with ID -> " + field.getId());
			
			//Save the field creation info in Zephyr Configuration
			JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
					.setString(ConfigurationConstants.ZEPHYR_CF_TESTSTEP_KEY, field.getId());
			
			associateWithDefaultScreen(field);
		}

		return field;
	}

	// Reference Class
	// com.pyxis.greenhopper.jira.configurations.AbstractPersisted.java
	// protected IssueField createAndPersistField(String key, String type,
	// String searcher, String[] issueTypes, IssueFieldManager
	// issueFieldManager) {
	public CustomField createAndPersistField(String key, String desc,
			String type, String searcher, String[] issueTypes) {

		CustomField newCustomField = null;

		try {
			CustomFieldManager cfManager = JiraUtil.getCustomFieldManager();
			newCustomField = cfManager.createCustomField(i18n.getText(key),
														i18n.getText(desc), 
														cfManager.getCustomFieldType(type),
														null,
														//cfManager.getCustomFieldSearcher(searcher),
														CustomFieldUtils.buildJiraIssueContexts(true, null, null,JiraUtil.getTreeManager()), 
														CustomFieldUtils.buildIssueTypes(JiraUtil.getConstantsManager(),
														issueTypes));
		} catch (Exception e) {
			//log.error(e);
			throw new CustomFieldException(
					"Exception while trying to create a customField with the following parameters: "
							+ i18n.getText(key), e);
		}

		// return issueFieldManager.getField(fieldId);
		return newCustomField;
	}

	/**
	 * Associate the custom field with the default screen.
	 */
	public void associateWithDefaultScreen(CustomField customField) {
		notNull(customField, "The custom field to associate with the default screen cannot be null");

		// fetch the default screen
		FieldScreen defaultScreen = JiraUtil.getFieldScreenManager()
				.getFieldScreen(FieldScreen.DEFAULT_SCREEN_ID);

		// check whether the field has already been added to the screen
		// (regardless of what tab)
		if (!defaultScreen.containsField(customField.getId())) {
			//log.error("Associating Zephyr Customfiled to Default Screen for first time");
			// just add the field to the first tab
			// JIRA uses a List internally, so the tag position is simply the
			// index (which starts at 0)
			FieldScreenTab firstTab = defaultScreen.getTab(0);
			firstTab.addFieldScreenLayoutItem(customField.getId());
		}
	}

	protected String getPropertyKey(String key) {
		return key;
	}

	// --------------- Create Zephyr Dashboard changes -----------

	private PortalPage createNewPortalPage() {
		String title = i18n.getText("je.gadget.common.zephyr.dashboard.label");
		String desc = i18n.getText("je.gadget.common.zephyr.dashboard.desc");
		return PortalPage
				.name(title)
				.description(desc)
				.owner(JiraUtil.getRemoteUser().getName())
				.permissions(SharedEntity.SharePermissions.GLOBAL).build();
	}

	public void createZephyrDashboard() {
		PortalPageManager portalPageManager = JiraUtil.getPortalPageManager();

		PortalPage zephyrPage = createNewPortalPage();
		zephyrPage = portalPageManager.create(zephyrPage);
		log.debug("Dashboard with Id "+ zephyrPage.getId() + " is created");
		
		//Save the dashboard creation info in Zephyr Configuration
		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
				.setString(ConfigurationConstants.ZEPHYR_DASHBOARD_KEY, zephyrPage.getId().toString());

		//Now associate a gadget with this dashboard.
		addGadgetToDashboard(zephyrPage, "rest/gadgets/1.0/g/com.thed.zephyr.je:zephyr-je-gadget-testcase-creation/gadgets/testcase-creation.xml", com.atlassian.gadgets.dashboard.Color.color1, 0, 0);
		addGadgetToDashboard(zephyrPage, "rest/gadgets/1.0/g/com.thed.zephyr.je:zephyr-je-gadget-testcase-execution/gadgets/testcase-execution.xml", com.atlassian.gadgets.dashboard.Color.color3, 1, 0);
		
		//We should not call MarkDashboardFavourites() during Plugin Enable event.
		//Because during this time we don't have user object to which we will mark Dashboard as favourite.
		//Instead this call should be done from user event listener such as LoginEvent or so.
		/*
		 * markDashboardFavourite(systemPage, zephyrPage);
		 */
	}

	/**
	 * @param zephyrPage
	 * @param column TODO
	 * @param row TODO
	 * @return
	 */
	public URI addGadgetToDashboard(PortalPage zephyrPage, String gadgetURIStr, Color color, Integer column, Integer row) {
		URI gadgetURI = null;
		try {
			gadgetURI = new URI(gadgetURIStr);
		} catch (URISyntaxException urlE) {
			log.fatal("unable to add gadget to Zephyr Dashboard ", urlE);
		}

		pConfigurationManager.addGadget(zephyrPage.getId(), 
										column, row, gadgetURI, color, new HashMap<String, String>());
		return gadgetURI;
	}
	
	private void buildZephyrExecutionStatuses() {
		String defaultStatuses = JiraUtil.getPropertySet(
				ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getText(
				ConfigurationConstants.ZEPHYR_EXECUTION_STATUSES);
		
		//Second condition is mainly for development / testing purposes.
		//If string is empty array, then add the Executionstatues again.
		if(defaultStatuses == null || defaultStatuses.equals("[]")){
			//First create all default execution status json objects.
			List<ExecutionStatus> aList = new ArrayList<ExecutionStatus>();

			ExecutionStatus pass = new ExecutionStatus(1, i18n.getText("zephyr.je.admin.plugin.execstatus.pass.title"), i18n.getText("zephyr.je.admin.plugin.execstatus.pass.desc"), "#75B000",0);
			ExecutionStatus fail = new ExecutionStatus(2, i18n.getText("zephyr.je.admin.plugin.execstatus.fail.title"), i18n.getText("zephyr.je.admin.plugin.execstatus.fail.desc"),"#CC3300", 0);
			ExecutionStatus wip = new ExecutionStatus(3, i18n.getText("zephyr.je.admin.plugin.execstatus.wip.title"), i18n.getText("zephyr.je.admin.plugin.execstatus.wip.desc"), "#F2B000",0 );
			ExecutionStatus blocked = new ExecutionStatus(4, i18n.getText("zephyr.je.admin.plugin.execstatus.blocked.title"), i18n.getText("zephyr.je.admin.plugin.execstatus.blocked.desc"), "#6693B0", 0);
			ExecutionStatus unexecuted = new ExecutionStatus(-1, i18n.getText("zephyr.je.admin.plugin.execstatus.unexecuted.title"), i18n.getText("zephyr.je.admin.plugin.execstatus.unexecuted.desc"), "#A0A0A0", 0);

			aList.add(pass);
			aList.add(fail);
			aList.add(wip);
			aList.add(blocked);
			aList.add(unexecuted);
			
			ObjectMapper objMapper = new ObjectMapper();
			String execStatusesStringObject = null;
			
            try{
            	execStatusesStringObject = objMapper.writeValueAsString(aList);
            }
            catch (IOException e){
                throw new RuntimeException("Failed to save ExecutionStatus List to JSON: " + aList, e);
            }

            //log.error("Storing the statuses to JIRA.");
            JiraUtil.getPropertySet( ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
            							.setText(ConfigurationConstants.ZEPHYR_EXECUTION_STATUSES,execStatusesStringObject );

		}
		
       JiraUtil.buildExecutionStatusMap();
	}
}
