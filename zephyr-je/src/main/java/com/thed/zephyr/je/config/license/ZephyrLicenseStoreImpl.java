package com.thed.zephyr.je.config.license;

import org.apache.log4j.Logger;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;


public class ZephyrLicenseStoreImpl implements ZephyrLicenseStore
{
    private static final Logger log = Logger.getLogger(ZephyrLicenseStoreImpl.class);

    private final JiraAuthenticationContext authContext;
    
    public ZephyrLicenseStoreImpl(final JiraAuthenticationContext authContext){
    	this.authContext = authContext;
    }
    
    @Override
	public ZephyrLicense retrieve()
    {
    	ZephyrLicense zephyrLicense = null;
		
					
		try{
	    	String crypticLicense = deCrypt(JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, 1l).getText(ConfigurationConstants.ZEPHYR_LICENSE));
	    	if(crypticLicense == null)
				return null;
	    	
			//Validate if License Key is not tempered and then create license Object if everything looks ok!
			LicenseVerifier licenseVerifier = new LicenseVerifier(authContext);
			zephyrLicense = licenseVerifier.validateLicense( crypticLicense);
			
		}
		catch (Exception e)
		{
			//This should not happen because licensekey is entered after validation!.
			log.error(e);
			return null;
		}   
		
		return zephyrLicense;
    }

    @Override
	public void store(final String licenseString)
    {
		JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, 1l).setText(ConfigurationConstants.ZEPHYR_LICENSE, encrypt(licenseString));
    }

    public String getUserEnteredLicenseString(){
    	String crypticLicense = deCrypt(JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, 1l).getText(ConfigurationConstants.ZEPHYR_LICENSE));
    	if(crypticLicense == null)
			return null;
    	
    	return crypticLicense;
    }
    
    private String encrypt(String licence)
    {
        String criptedLicence = licence.replaceAll("r", "&");
        criptedLicence = criptedLicence.replaceAll("b", "\\!");
        return criptedLicence;
    }
    
    private String deCrypt(String criptedLicence)
    {
        if(criptedLicence == null) return null;
        
        String licence = criptedLicence.replaceAll("&", "r");
        licence = licence.replaceAll("\\!", "b");
        return licence;
    }
    
    
}
