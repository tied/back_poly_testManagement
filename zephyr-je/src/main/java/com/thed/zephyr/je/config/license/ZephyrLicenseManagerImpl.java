package com.thed.zephyr.je.config.license;

import com.atlassian.extras.api.LicenseType;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.application.ApplicationKeys;
import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.license.JiraLicenseManager;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.util.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.net.URI;
import java.util.Date;


public class ZephyrLicenseManagerImpl implements ZephyrLicenseManager{

    private static final Logger log = Logger.getLogger(ZephyrLicenseManagerImpl.class);

    private final ZephyrLicenseStore licenseStore;
    private final JiraLicenseService jiraLicenseService;
    private final JiraLicenseManager jiraLicenseManager;
    private final BuildProperties buildProperties;
    private final JiraAuthenticationContext authContext;
    private final I18nHelper i18nHelper;
    private final PluginLicenseManager licenseManager;
    private ApplicationProperties applicationProperties;
    
    private UserUtil userUtil;
    

	public ZephyrLicenseManagerImpl (final JiraLicenseService jiraLicenseService, 
									final JiraLicenseManager jiraLicenseManager,
									final ZephyrLicenseStore licenseStore,
									final BuildProperties buildProperties,
									final JiraAuthenticationContext authContext, 
									final PluginLicenseManager licenseManager,
									final ApplicationProperties applicationProperties){
		this.jiraLicenseService = jiraLicenseService;
		this.jiraLicenseManager = jiraLicenseManager;
		this.licenseStore = licenseStore;
		this.buildProperties = buildProperties;
		this.authContext = authContext;
		this.i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		this.licenseManager = licenseManager;
		this.applicationProperties = applicationProperties;
	}
	
	@Override
	public ZephyrLicense getLicense(){
		ZephyrLicense currLicense = JiraUtil.getCurrentZephyrLicense();
		
		if(currLicense == null){
			currLicense = getLicenseFromStore();
		}
		
		JiraUtil.setCurrentZephyrLicense(currLicense);
	    return currLicense;
	}

	@Override
	public ZephyrLicense getLicenseFromStore(){
		return licenseStore.retrieve();
	}

	public String getUserEnteredLicenseStringFromStore(){
		return licenseStore.getUserEnteredLicenseString();
	}
	
	@Override
	public ZFJLicenseDetails getLicenseDetails(ZephyrLicense inputZLicense){
		
		try{
			DateTime buildDate = buildProperties.getBuildDate();
			ZFJLicenseDetails zfjLicenseDetails = inputZLicense == null ? NullLicenseDetails.NULL_LICENSE_DETAILS : new ZephyrLicenseDetails(inputZLicense, buildDate);
			return zfjLicenseDetails;
		}
		catch(Exception e){
			log.error(e);
			return NullLicenseDetails.NULL_LICENSE_DETAILS;
		}
	}
	
	@Override
	public void setLicense(final String licenseString) throws ZephyrLicenseException{
		//Validate if License Key is not tempered and then create license Object if everything looks ok!
		LicenseVerifier licenseVerifier = new LicenseVerifier(authContext);
		ZephyrLicense newZephyrLicense = licenseVerifier.validateLicense( licenseString);
	    ZFJLicenseDetails newZephyrLicenseDetails = getLicenseDetails(newZephyrLicense);
	    
	    verify(newZephyrLicense, newZephyrLicenseDetails, getJIRALicenseDetails());
		JiraUtil.setCurrentZephyrLicense(newZephyrLicense);
	    
	    licenseStore.store(licenseString);
	}

	private LicenseDetails getJIRALicenseDetails(){
		return jiraLicenseManager.getLicense(ApplicationKeys.SOFTWARE).getOrElse(jiraLicenseManager.getLicense(ApplicationKeys.CORE).getOrNull());
	}

    @Override
	public void verifyOnStartup() throws ZephyrLicenseException{
    	// Dont verify the user count at start up - if using Crowd this one might not be up [GHS-1730]
    	//
    	doVerifyInternal(false);
    }
    
    @Override
    public URI getRedirectionUrl(){
    	if(isMPLicenseStored()){
			return PluginUtils.getUPMManagePluginUri();
    	}else{
    		return URI.create(applicationProperties.getBaseUrl() + "/secure/admin/ZLicense.jspa").normalize();
    	}
    }
    
    @Override
	public void verify() throws ZephyrLicenseException{
        if (isMPLicenseStored()){
        	doVerifyWithUPM();
        }else{
        	doVerifyInternal(true);
        }
    }
    
    /**
     * returns true if license stored is eval, whether expired or not
     * @throws ZephyrLicenseException
     */
    public boolean isEval() throws ZephyrLicenseException{
    	if (isMPLicenseStored()){
			return licenseManager.getLicense().get().isEvaluation();
		}else{
    		if(getLicense() == null) {
    			return false;
    		}
    		return getLicense().isEvaluation();
    	}
    }

	/**
	 * 
	 * @return
	 */
	private Boolean isMPLicenseStored() {
		//Check and see if a license is currently stored.
        //This accessor method can be used whether or not a licensing-aware UPM is present.
		return licenseManager.getLicense().isDefined();
	}

	//https://developer.atlassian.com/display/UPM/License+Validation+Rules
    private void doVerifyWithUPM() throws ZephyrLicenseException{
    	PluginLicense pluginLicense;
		pluginLicense = licenseManager.getLicense().get();
		//Check and see if the stored license has an error. If not, it is currently valid.
        if (!pluginLicense.isValid()){
        	switch(pluginLicense.getError().get()){
        		case EXPIRED:
        			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_EVAL_EXPIRED, i18nHelper.getText("zephyr.license.eval.expired") );
        		case USER_MISMATCH:
        			checkUserLimitCompatibility(pluginLicense.isUnlimitedNumberOfUsers(), pluginLicense.getMaximumNumberOfUsers().get(), getJIRALicenseDetails());
        			break;
        		case VERSION_MISMATCH:
        			verifyPluginVersionAndMaintenanceDate(pluginLicense.getMaintenanceExpiryDate().get().toDate());
        			break;
        		case TYPE_MISMATCH:
        			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, i18nHelper.getText("zephyr.marketplace.license.mismatch.error", pluginLicense.getPluginName(), pluginLicense.getLicenseType().name()));
        	}
        	
        	throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_ACTIVATE_LICENSE,i18nHelper.getText("zephyr.license.activate") );
        }

    }

    public void doVerifyInternal(boolean checkUserLimit) throws ZephyrLicenseException{
    	
    	ZephyrLicense zLicense = getLicense();
	    if(zLicense == null)
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_ACTIVATE_LICENSE,i18nHelper.getText("zephyr.license.activate") );

	    ZFJLicenseDetails zephyrLicenseDetails = getLicenseDetails(zLicense);
	    
    	if(!zephyrLicenseDetails.isLicenseSet())
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_NOT_LICNESED, i18nHelper.getText("zephyr.license.not.licensed"));

//    	if(checkUserLimit && getUserUtil().hasExceededUserLimit())
//			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_USER_LIMIT_REACHED, i18nHelper.getText("zephyr.license.jira.user.limit.reached"));

    	LicenseDetails jiraLicenseDetails = getJIRALicenseDetails();

    	//verifyBuildDate(zephyrLicenseDetails);
        verify(zLicense, zephyrLicenseDetails, jiraLicenseDetails);
        
        if(!JiraUtil.isDevMode() && zLicense.isMaintenanceExpired())
        	verifyPluginVersionAndMaintenanceDate(zLicense.getMaintenanceExpiryDate());
    }

    private void verifyPluginVersionAndMaintenanceDate(Date maintenanceExpirationDate) throws ZephyrLicenseException{
    	String installedZephyrJEInfo = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
				.getString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);

    	
    	Plugin plugin = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY);
    	String newZephyrJEVersion = plugin.getPluginInformation().getVersion();
    	
		DateTime pluginBuildDate = PluginUtils.getPluginBuildDate(plugin);
    	
    	if(pluginBuildDate.isAfter(maintenanceExpirationDate.getTime())){
    		String upgradeErrorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.version.upgrade.error", newZephyrJEVersion);
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_VERSION_UPGRADE_ERROR, upgradeErrorMessage);
    	}
    }
    
//    private void verifyBuildDate(LicenseDetails zLicenseDetails) throws ZephyrLicenseException
//    {
//        if(!zLicenseDetails.isMaintenanceValidForBuildDate(new Date(buildProperties.getBuildDate().getMillis())))
//			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_ACTIVATE_LICENSE, "You cannot update to Zephyr version " + buildProperties.getVersion() + ". Updates are no longer available.");
//	}
    
    public PluginLicense getZephyrMarketplaceLicense(){

		if(licenseManager.getLicense().isDefined())
            return licenseManager.getLicense().get();

		return null;
    }
    
    private void verify(ZephyrLicense zephyrLicense, ZFJLicenseDetails zephyrLicenseDetails, LicenseDetails jiraLicenseDetails) throws ZephyrLicenseException{
    	checkLicenseCompatibility(zephyrLicense, zephyrLicenseDetails, jiraLicenseDetails);
    	checkExpiryDate(zephyrLicenseDetails);
    }

	private void checkExpiryDate(ZFJLicenseDetails zephyrLicenceDetails) throws ZephyrLicenseException{
		if(zephyrLicenceDetails.isEvaluation() && zephyrLicenceDetails.isExpired())
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_EVAL_EXPIRED, i18nHelper.getText("zephyr.license.eval.expired") );
	}
    
	private void checkLicenseCompatibility(ZephyrLicense zLicense, ZFJLicenseDetails zephyrLicenseDetails, LicenseDetails jiraLicenseDetails) throws ZephyrLicenseException{
    	// Let's not worry about license compatibility. We care only about having valid Zephyr License.
//    	if (!zLicense.getLicenseType().equals(getJiraLicenseType(jiraLicenseDetails)))
//    	{
//			throw new LicenseException(typeCompabilityMessage(zLicense, zephyrLicenseDetails, jiraLicenseDetails));
//    	}
    	
    	checkUserLimitCompatibility(zephyrLicenseDetails.isUnlimitedNumberOfUsers(), zephyrLicenseDetails.getMaximumNumberOfUsers(), getJIRALicenseDetails());
    }
	
	private LicenseType getJiraLicenseType(LicenseDetails jiraLicenseDetails){

		/*
		 * LicenseType do not have "Evaluation" type. This is because "Evaluation" license is full-fledged (i.e. Commercial unlimited users) 30 days license.
		 * After this evaluation period ends, user cannot use JIRA! 
		 * Hence for evaluation licenses, description will say as "Evaluation" but user limit will show as "Unlimited". 
		 */
		
		if(jiraLicenseDetails.isCommercial())
			return LicenseType.COMMERCIAL;

		if(jiraLicenseDetails.isCommunity())
			return LicenseType.COMMUNITY;
		
		if(jiraLicenseDetails.isDemonstration())
			return LicenseType.DEMONSTRATION;
		
		if(jiraLicenseDetails.isDeveloper())
			return LicenseType.DEVELOPER;

		if(jiraLicenseDetails.isNonProfit())
			return LicenseType.NON_PROFIT;
		
		if(jiraLicenseDetails.isOpenSource())
			return LicenseType.OPEN_SOURCE;
		
		if(jiraLicenseDetails.isPersonalLicense())
			return LicenseType.PERSONAL;
		
		if(jiraLicenseDetails.isStarter())
			return LicenseType.STARTER;

		//There are two more conditions HOSTED and ACADEMIC for which LicenseDetails doesn't expose any methods to verify.
		//Hence if all above fails, default will be TESTING license.
		return LicenseType.TESTING;
	}
	
    private void checkUserLimitCompatibility(Boolean isUnlimited, Integer maximumNumberOfUsers, LicenseDetails jiraLicenseDetails) throws ZephyrLicenseException{
    	if(isUnlimited) return;
    	
    	if(jiraLicenseDetails.isUnlimitedNumberOfUsers() && (maximumNumberOfUsers < 500))
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, limitCompatibilityMessage(isUnlimited, maximumNumberOfUsers, jiraLicenseDetails));
    	
    	if(jiraLicenseDetails.getJiraLicense().getMaximumNumberOfUsers() - 1 > maximumNumberOfUsers)
			throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, limitCompatibilityMessage(isUnlimited, maximumNumberOfUsers, jiraLicenseDetails));
	}
    
	private String limitCompatibilityMessage(Boolean isUnlimited, Integer maximumNumberOfUsers, LicenseDetails jiraLicenseDetails){
		String zephyrLimit = isUnlimited ? "Unlimited" : String.valueOf(maximumNumberOfUsers);
		String jiraLimit = jiraLicenseDetails.getJiraLicense().isUnlimitedNumberOfUsers() ? "Unlimited" : String.valueOf(jiraLicenseDetails.getJiraLicense().getMaximumNumberOfUsers());

		return ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.license.limit.compatibilitymessage",zephyrLimit, jiraLimit);
	}
	
	private String typeCompatibilityMessage(ZephyrLicense zLicense, LicenseDetails zephyrLicenseDetails, LicenseDetails jiraLicenseDetails){
		String zephyrType = zephyrLicenseDetails.isEvaluation() ? "EVALUATION" : zLicense.getLicenseType().name();
		String jiraType = jiraLicenseDetails.isEvaluation() ? "EVALUATION" : getJiraLicenseType(jiraLicenseDetails).name();
		return ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.license.type.compatibilitymessage",zephyrType, jiraType);
	}

	private UserUtil getUserUtil(){
		if(userUtil != null) return userUtil;
		userUtil = ComponentManager.getComponentInstanceOfType(UserUtil.class);
		return userUtil;
	}

	@Override
	public String getServerId(){
    	return jiraLicenseService.getServerId();
    }
	
}