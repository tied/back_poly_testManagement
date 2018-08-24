package com.thed.zephyr.je.config.action;

import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.favourites.FavouritesManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.portal.PortletConfigurationManager;
import com.atlassian.jira.user.preferences.UserPreferencesManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.util.OutlookDate;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.thed.zephyr.je.config.ZephyrJEDefaultConfiguration;
import com.thed.zephyr.je.config.license.PluginUtils;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.job.ZFJJobRunner;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZFJLicenseDetails;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class ZephyrLicenseAction extends JiraWebActionSupport{

	protected static final Logger log = Logger.getLogger(ZephyrLicenseAction.class);

	private final JiraLicenseService jiraLicenseService;
	private final ZephyrLicenseManager zLicenseManager;
	private final PortletConfigurationManager pConfigurationManager;
	private final FavouritesManager favouritesManager;
	private final UserPreferencesManager userPreferencesManager;
    private ZFJJobRunner zfjJobRunner;
	
	private String newLicenseKey;
	private String licenseDescription;
	private String licenseExpiryStatusMessage;
	private String maintenanceEndString;
	private String purchaseDateString;
	private int maximumNumberOfUsers;
	private String userEnteredLicenseString;
	
	private final PluginLicenseManager licenseManager;
	
	@SuppressWarnings("deprecation")
	private final DateTimeFormatterFactory dateTimeFormatterFactory;

	
	public ZephyrLicenseAction(final JiraLicenseService jiraLicenseService, 
								final ZephyrLicenseManager zLicenseManager,
								final DateTimeFormatterFactory dateTimeFormatterFactory,
								final PortletConfigurationManager pConfigurationManager,
								final FavouritesManager favouritesManager,
								final UserPreferencesManager userPreferencesManager,
								final PluginLicenseManager licenseManager,
                                final ZFJJobRunner zfjJobRunner){
		this.jiraLicenseService = jiraLicenseService;
		this.zLicenseManager = zLicenseManager;
		this.dateTimeFormatterFactory = dateTimeFormatterFactory;
		this.pConfigurationManager = pConfigurationManager;
		this.favouritesManager = favouritesManager;
		this.userPreferencesManager = userPreferencesManager;
		
		this.licenseManager = licenseManager;
        this.zfjJobRunner = zfjJobRunner;
	}
	
    public String doSuccess(){
        if(getLoggedInUser() == null || !hasGlobalPermission(GlobalPermissionKey.ADMINISTER)){
            return getRedirect("/secure/Dashboard.jspa");
        }

        try{
        	ZephyrLicense zephyrLicense = getLicense();
        	/*Zephyr license is null which means user has never entered it, hence return success*/
        	if(zephyrLicense == null){
        		return SUCCESS;
        	}
        	ZFJLicenseDetails zephyrLicenseDetails = getLicenseDetails(zephyrLicense);
        	//set Values for display.
        	setLicenseDisplayValues(zephyrLicenseDetails);
        	
        	zLicenseManager.doVerifyInternal(true);
        }
        catch (Exception e){
        	addError("Exception Message", e.getMessage());
        	log.error(e.getMessage());
        	
        	return ERROR;
        }
        return SUCCESS;
    }

    public String doUpdate(){
        try{
            if(!hasGlobalPermission(GlobalPermissionKey.ADMINISTER)){
                return getRedirect("/secure/Dashboard.jspa");
            }
            
            if(StringUtils.isBlank(newLicenseKey)){
            	//Let's remove license string from the database.
            	String licenseString = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).getString(ConfigurationConstants.ZEPHYR_LICENSE);
            	if(licenseString != null)
            		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID).remove(ConfigurationConstants.ZEPHYR_LICENSE);
            	
        		JiraUtil.setCurrentZephyrLicense(null);
        		return SUCCESS;
            }

            //First set ZephyrLicenseDetails object to the license that is already in the database.
        	ZephyrLicense zephyrLicense = getLicense();
        	ZFJLicenseDetails zephyrLicenseDetails = getLicenseDetails(zephyrLicense);
        	setLicenseDisplayValues(zephyrLicenseDetails);

        	//Now let's try to set the license. If this invalid license, exception will be thrown!
        	zLicenseManager.setLicense(newLicenseKey);
            
        	//Since we here now, it means license is valid one. Let's first do the initalization work.
            //Do remaining Zephyr JE initialization which requires plugin to be enabled and available.
            //Such as Zephyr Test step creation as it needs Custom Field Type and Searcher from our plugin!
    		
        	/*In case we need to init on demand, we can do it by populating this license. This also gets involked automatically when JIRA starts*/
        	synchronized(ZephyrJEDefaultConfiguration.initialized){
        		ZephyrJEDefaultConfiguration zephyrConfiguration = new ZephyrJEDefaultConfiguration(pConfigurationManager, userPreferencesManager);
        		zephyrConfiguration.postInit();
        	}
			
        	
    		//Set database version 
    		String zephyrJEVersion = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);
    		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
    		.setString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION, zephyrJEVersion );
    		
        	//Everything set correctly. So let's update licenseDetails to newly selected value from the database.
        	zephyrLicense = getLicense();
        	zephyrLicenseDetails = getLicenseDetails(zephyrLicense);
        	setLicenseDisplayValues(zephyrLicenseDetails);
        	/*
        	 * New License has been applied, lets perform version check
        	 */
            zfjJobRunner.execute(null);
        	
        	//Display error message to user if 
        }
        catch (Exception e){
        	addError("Exception Message", e.getMessage());
        	log.error(e.getMessage());

        	return ERROR;
        }
        return SUCCESS;
    }
     
    public ZFJLicenseDetails getLicenseDetails(ZephyrLicense zLicense){
    	ZFJLicenseDetails zephyrLicenseDetails = zLicenseManager.getLicenseDetails(zLicense);
    	return zephyrLicenseDetails;
    }
    
    public ZephyrLicense getLicense(){
    	ZephyrLicense zephyrLicense = zLicenseManager.getLicense();
    	return zephyrLicense;
    }

	@SuppressWarnings("deprecation")
	private void setLicenseDisplayValues(ZFJLicenseDetails zephyrLicenseDetails){
	   licenseDescription = zephyrLicenseDetails.getDescription() != null ? zephyrLicenseDetails.getDescription() : "";
	   licenseExpiryStatusMessage = zephyrLicenseDetails.getLicenseExpiryStatusMessage(this, ComponentAccessor.getJiraAuthenticationContext().getOutlookDate());
	   maintenanceEndString = zephyrLicenseDetails.getMaintenanceEndString(new OutlookDate(ComponentAccessor.getJiraAuthenticationContext().getLocale(), ComponentAccessor.getApplicationProperties(), ComponentAccessor.getI18nHelperFactory(), dateTimeFormatterFactory));
	   purchaseDateString = zephyrLicenseDetails.getPurchaseDate(new OutlookDate(ComponentAccessor.getJiraAuthenticationContext().getLocale(), ComponentAccessor.getApplicationProperties(), ComponentAccessor.getI18nHelperFactory(), dateTimeFormatterFactory));
	   maximumNumberOfUsers = zephyrLicenseDetails.getMaximumNumberOfUsers();
	   userEnteredLicenseString = zLicenseManager.getUserEnteredLicenseStringFromStore();
   }
   
    /**
     * @return the license description, or an empty string if there is none.
     */
    public String getLicenseDescription() {
    	return licenseDescription;
    }

    public String getLicenseExpiryStatusMessage(){
    	return licenseExpiryStatusMessage;
    }

    public String getMaintenanceEndString(){
    	return maintenanceEndString;
    }

    public String getPurchaseDateString(){
    	return purchaseDateString;
    }

    @Override
	public String getServerId(){
    	return jiraLicenseService.getServerId();
    }

    public int getMaximumNumberOfUsers(){
    	return maximumNumberOfUsers;
    }

    public String getNewLicenseKey(){
        return newLicenseKey;
    }

    public void setNewLicenseKey(String newLicenseKey){
        this.newLicenseKey = newLicenseKey.trim();
    }
    
    public boolean isUPMLicensePresent(){
    	boolean isUPMHandlingLicense = false;

		isUPMHandlingLicense = licenseManager.getLicense().isDefined();

		return isUPMHandlingLicense;
    }
    
    public boolean isUpmLicensingAware(){
    	return true;
    }
    
    /**
     * Retrieves appropriate UPM page license. Called from vm
     */
    public String getMPLicURL(){
    	if(isUpmLicensingAware()){
			return PluginUtils.getUPMManagePluginUri().toString();
    	}
    	
    	return insertContextPath("/plugins/servlet/" + ConfigurationConstants.PLUGIN_KEY + "/license");
    }
    
   public String getUserEnteredLicenseString(){
	   return userEnteredLicenseString;
   }
}