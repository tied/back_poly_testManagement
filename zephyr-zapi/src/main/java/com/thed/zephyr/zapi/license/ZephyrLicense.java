package com.thed.zephyr.zapi.license;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.atlassian.extras.api.LicenseType;
import com.atlassian.jira.web.util.OutlookDate;
import com.thed.zephyr.zapi.util.ApplicationConstants;
import com.thed.zephyr.zapi.util.ZephyrLicenseException;

/**
 * A set of methods which describe the state of the currently installed license for Zephyr Plugin.
 *
 */
public class ZephyrLicense
{

	private String licenseString;
	
	private String licenseEdition;
	private LicenseType licenseType;
	private String description;
	private String licenseId;
	private Date purchaseDate;
	private Date maintenanceExpiryDate;
	private Date licenseExpiryDate;
	//
	private boolean isEval = false;
	private boolean isUnlimitedUsersAllowed = false;
	private int maximumNumberOfUsers;
	//
	private String organisation;
	private String organisationId;
	private String organisationEmail;
	
	private boolean isSupportEntitled;
	private String supportEntitlementNumber;

	private boolean isLicenseSet = false;
	
	protected static final Logger log = Logger.getLogger(ZephyrLicense.class);
	
/*
 * Test Strings to create ZephyrLicenses.
 * licenseEdition#licenseType#licenseDesc#licenseId#purchaseDate#maintenanceExpiryDate#licenseExpiryDate#isEval#isUnlimited#maxNoOfUsers#organisation#organisationId#organisationEmail	
 */
	public ZephyrLicense(String licenseString) throws ZephyrLicenseException {
        this.licenseString = notNull("licenseString", licenseString);
 
        log.info("Setting license details...");
        
        String[] list = licenseString.split("#");

    	int exceptionSwitch = 0;
		String message = "Error - Invalid Data";
    	String tempPurchaseDateString = list[4];
    	String tempMaintenanceExpiryDateString = list[5];
    	String tempLicenseExpiryDateString = list[6];

    	try{
	        licenseEdition = list[0];
	        
	    	int licenseTypeValue = Integer.parseInt(list[1]);
	    	switch(licenseTypeValue){
	    		case ApplicationConstants.ACADEMIC:
	    			licenseType = LicenseType.ACADEMIC;
	    			break;
	    			
	    		case ApplicationConstants.COMMERCIAL:
	    			licenseType = LicenseType.COMMERCIAL;
	    			break;
	    			
	    		case ApplicationConstants.COMMUNITY:
	    			licenseType = LicenseType.COMMUNITY;
	    			break;
	    			
	    		case ApplicationConstants.DEMONSTRATION:
	    			licenseType = LicenseType.DEMONSTRATION;
	    			break;
	    			
	    		case ApplicationConstants.DEVELOPER:
	    			licenseType = LicenseType.DEVELOPER;
	    			break;
	    			
	    		case ApplicationConstants.HOSTED:
	    			licenseType = LicenseType.HOSTED;
	    			break;
	    			
	    		case ApplicationConstants.NON_PROFIT:
	    			licenseType = LicenseType.NON_PROFIT;
	    			break;
	    			
	    		case ApplicationConstants.OPEN_SOURCE:
	    			licenseType = LicenseType.OPEN_SOURCE;
	    			break;
	
	    		case ApplicationConstants.PERSONAL:
	    			licenseType = LicenseType.PERSONAL;
	    			break;
	    			
	    		case ApplicationConstants.STARTER:
	    			licenseType = LicenseType.STARTER;
	    			break;
	    	}
	        
	    	description = list[2];
	    	licenseId = list[3];
	    	
    		DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
    		
    		exceptionSwitch = 1;
    		purchaseDate = df.parse(tempPurchaseDateString);
    		
    		exceptionSwitch = 2;
    		maintenanceExpiryDate = df.parse(tempMaintenanceExpiryDateString);
    		
    		exceptionSwitch = 3;
    		licenseExpiryDate = df.parse(tempLicenseExpiryDateString);
    		
        	String evaluationType = list[7];
        	if(evaluationType.equals("1")){
        		isEval = true;
        	}
        	
        	String unlimitedUsersFlag = list[8];
        	if(unlimitedUsersFlag.equals("1")){
        		isUnlimitedUsersAllowed = true;
        		maximumNumberOfUsers = 9999;
        	}
        	else{
        		exceptionSwitch = 4;
            	maximumNumberOfUsers = Integer.parseInt(list[9]);
        	}
        	

        	organisation = list[10];
        	organisationId = list[11];
        	organisationEmail = list[12];
    	}
    	catch(ParseException pe){
    		
    		switch(exceptionSwitch){
	    		case 1:
	    			message = "Error - Invalid Purchase Date - " + tempPurchaseDateString;
	    			break;
    			
    			case 2:
    				message = "Error - Invalid Maintenance Date - " + tempMaintenanceExpiryDateString;
    				break;
    			
    			case 3:
    				message = "Error - Invalid License Expiry Date - " + tempLicenseExpiryDateString;
    				break;
    			
    			default:
    				message = "Error with one of the Dates input in LicenseKey. Please verify.";
    				break;
    		}
    		
    		throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_INVALID_LICENSE,message);
    	}
    	catch(NumberFormatException nfe){
    		switch(exceptionSwitch){
    			case 4:
    				message = "Error - Invalid Number for MaximumNumberOfUsers - " + maximumNumberOfUsers; 
    		}
    		
    		throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_INVALID_LICENSE,message);
    	}
    	catch(Exception e){
    		throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_UNCATEGORIZED_LICENSE_EXCEPTION,message);
    	}

    	isLicenseSet = true;
	}
	
    /**
     * Tells whether a license has been set or not for this JIRA instance.
     *
     * @return {@code true} if a license has been set, {@code false} otherwise.
     */
    public boolean isLicenseSet(){
    	return isLicenseSet;
    }

    /**
     * Gets the version of the current license, 0 if not set. This is the version of encoder/decoder that was used with
     * this license.
     *
     * @return the version of the current license, 0 if not set.
     */
    public int getLicenseVersion(){
    	return 0;
    }

    /**
     * Tells whether the current license authorise an unlimited number of users.
     *
     * @return {@code true} if the license authorise an unlimited number of users, {@code false} otherwise.
     * @see #getMaximumNumberOfUsers()
     */
    public boolean isUnlimitedNumberOfUsers() {
		return isUnlimitedUsersAllowed;
	}

    public Date getMaintenanceExpiryDate(){
    	return maintenanceExpiryDate;
    }

    public Date getExpiryDate(){
    	return licenseExpiryDate;
    }
    
    /**
     * Gets the maximum number of users allowed by the current license
     *
     * @return the maximum number of user allowed by the license, -1 if unlimited
     * @see #isUnlimitedNumberOfUsers()
     */
    public int getMaximumNumberOfUsers() {
		return maximumNumberOfUsers;
	}

    /**
     * @return the description of the current license
     */
    public String getDescription() {
    	return description;
	}

    /**
     * @return the Partner name inside the current license or null if its not set
     */
    public String getPartnerName() {
		return null;
	}

    /**
     * Gets a nicely formatted purchase date for the current license
     *
     * @param outlookDate the date formatter
     * @return a formatted purchased date.
     */
    @SuppressWarnings("deprecation")
	public String getPurchaseDate(OutlookDate outlookDate) {
		return outlookDate.format(purchaseDate);
	}

    public LicenseType getLicenseType(){
    	return licenseType;
    }

    public boolean isEvaluation(){
    	return isEval;
    }
    
    /**
     * Gets the organisation this license belongs to
     *
     * @return the organisation this license belongs to
     */
    public String getOrganisation() {
		return organisation;
	}

    /**
     * @return the encoded license string that was was decode to produce the current lincence.  This will return null if
     *         it is not set
     */
    public String getLicenseString() {
		return licenseString;
	}

    /**
     * Gets the SEN from the license
     *
     * @return the SEN from the license
     */
    public String getSupportEntitlementNumber() {
		return supportEntitlementNumber;
	}

    /**
     * @return true for licenses that are entitled to support and updates of JIRA. This includes everything which is not
     *         a Personal License or Non-Commercial-Non-Renewable.
     */
    public boolean isEntitledToSupport(){
    	return isSupportEntitled;
    }

	public String getLicenseEdition() {
		return licenseEdition;
	}

	public String getLicenseId() {
		return licenseId;
	}

	public String getOrganisationId() {
		return organisationId;
	}

	public String getOrganisationEmail() {
		return organisationEmail;
	}

    public boolean isMaintenanceExpired(){
       	final Date expiry = getMaintenanceExpiryDate();
    	return  (expiry != null) && (new Date().getTime() - expiry.getTime() > 0L);
    }
	
}