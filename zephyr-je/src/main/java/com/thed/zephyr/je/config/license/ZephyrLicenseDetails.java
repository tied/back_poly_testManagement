package com.thed.zephyr.je.config.license;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.Collection;
import java.util.Date;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

import com.atlassian.core.util.DateUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.extras.api.LicenseType;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.util.OutlookDate;
import com.thed.zephyr.util.ZFJLicenseDetails;

public class ZephyrLicenseDetails implements ZFJLicenseDetails
{
    private final ZephyrLicense license;
    private final String licenseString;
    private final DateTime buildDate;

    ZephyrLicenseDetails(final ZephyrLicense license, final DateTime buildDate)
    {
        this.license = notNull("license", license);
        this.licenseString = notNull("licenseString", license.getLicenseString());
        this.buildDate = notNull("buildDate", buildDate);
    }

    public boolean isEntitledToSupport()
    {
        return !(isNonCommercialNonRenewable() || isPersonalLicense());
    }

	public boolean isExpired(){
    	final Date expiry = getLicenseExpiry();
    	return  (expiry != null) && (new Date().getTime() - expiry.getTime() > 0L);
    }
    
	public boolean isLicenseAlmostExpired()
    {
        if (isEvaluation() || isNewBuildWithOldLicense())
        {
            final Date expiry = getLicenseExpiry();
            return ((expiry != null) && (expiry.getTime() - new Date().getTime() < 7L * DateUtils.DAY_MILLIS));
        }
        
        return false;
    }

	public String getPurchaseDate(final OutlookDate outlookDate)
    {
        return license.getPurchaseDate(outlookDate);
    }

	public boolean isStarter() 
	{
        return LicenseType.STARTER.equals(license.getLicenseType());
	}

	public boolean isEvaluation()
    {
        return license.isEvaluation();
    }

	public boolean isCommercial()
    {
        return LicenseType.COMMERCIAL.equals(license.getLicenseType());
    }

	public boolean isCommunity()
    {
        return LicenseType.COMMUNITY.equals(license.getLicenseType());
    }

	public boolean isOpenSource()
    {
        return LicenseType.OPEN_SOURCE.equals(license.getLicenseType());
    }

	public boolean isPersonalLicense()
    {
        return LicenseType.PERSONAL.equals(license.getLicenseType());
    }

	public boolean isNonProfit()
    {
        return LicenseType.NON_PROFIT.equals(license.getLicenseType());
    }

	public boolean isDemonstration()
    {
        return LicenseType.DEMONSTRATION.equals(license.getLicenseType());
    }

	public boolean isDeveloper()
    {
        return LicenseType.DEVELOPER.equals(license.getLicenseType());
    }

    private boolean isNonProfitLicense()
    {
        return LicenseType.NON_PROFIT.equals(license.getLicenseType());
    }

    private boolean isAcademic()
    {
        return LicenseType.ACADEMIC.equals(license.getLicenseType());
    }
    
    private boolean isDemonstrationLicense()
    {
        return LicenseType.DEMONSTRATION.equals(license.getLicenseType());
    }

    private boolean isNonCommercialNonRenewable()
    {
        return isNonProfitLicense() || isDemonstrationLicense() || LicenseType.TESTING.equals(license.getLicenseType());
    }

    @Override
	public String getOrganisation()
    {
        return license.getOrganisation() == null ? "<Unknown>" : license.getOrganisation();
    }

    @Override
	public boolean hasLicenseTooOldForBuildConfirmationBeenDone()
    {
        return true;
    }

    @Override
	public String getLicenseString()
    {
        return licenseString;
    }

    @Override
	public boolean isMaintenanceValidForBuildDate(final Date currentBuildDate)
    {
        return license.getMaintenanceExpiryDate() == null || license.getMaintenanceExpiryDate().compareTo(currentBuildDate) >= 0;
    }

	public String getSupportEntitlementNumber()
    {
        return license.getSupportEntitlementNumber();
    }

    /**
     * Return the HTML message for support request denial or send e-mail success page.
     *
     * @param user a User object for the calling user
     * @return the support request message
     */
	public String getSupportRequestMessage(User user) {
		return "";
	}
    
	public String getSupportRequestMessage(final I18nHelper i18n, final OutlookDate outlookDate)
    {
        return "";
    }

	public String getMaintenanceEndString(final OutlookDate outlookDate)
    {
        Date end;
        if (isEvaluation()){
            end = getLicenseExpiry();
        }
        else{
            end = getMaintenanceExpiryDate();
        }
        
        return outlookDate.formatDMY(end);
    }

	public boolean isUnlimitedNumberOfUsers(){
        return license.isUnlimitedNumberOfUsers();
    }

	public int getMaximumNumberOfUsers(){
        return license.getMaximumNumberOfUsers();
    }

	public boolean isLicenseSet(){
        return true;
    }

	public int getLicenseVersion(){
        return license.getLicenseVersion();
    }

	public String getDescription(){
        return license.getDescription();
    }

	public String getPartnerName(){
        return license.getPartnerName() == null ? null : license.getPartnerName();
    }

    /**
     * Return the HTML message that briefly describes the expiry status of the license. Intended for use with the Admin
     * Portlet.
     *
     * @param user the user for whom the message should be i18n'ed
     * @return the status message, null for normal license outside of support period
     */
	public String getLicenseExpiryStatusMessage(@Nullable User user){
    	return "Dear " + user.getDisplayName() + " Zephyr JE License has expired, please apply for renewal. Thank you!";
    }

    
	public String getLicenseExpiryStatusMessage(final I18nHelper i18n, final OutlookDate outlookDate){
        final String msg;
        if (isEvaluation()){
            if (isExpired()){
                msg = i18n.getText("zephyr.license.expired") + " " + i18n.getText("zephyr.license.buy.message");
            }
            else{
                msg = i18n.getText("zephyr.license.expiresin", getTimeUntilExpiry(i18n), outlookDate.formatDMY(getLicenseExpiry()));
            }
        }
        else if (isMaintenanceExpired()){
            if (isEntitledToSupport()){
                msg = i18n.getText("zephyr.support.expired.since", outlookDate.formatDMY(getMaintenanceExpiryDate()) ) + " " +i18n.getText("zephyr.license.buy.message");;
            }
            else{
                msg = i18n.getText("zephyr.upgrades.expired.since", outlookDate.formatDMY(getMaintenanceExpiryDate()) ) + " " +i18n.getText("zephyr.license.buy.message");;
            }
        }
        else if (!isMaintenanceExpired()){
            if (isEntitledToSupport()){
                msg = i18n.getText("zephyr.support.available.until", outlookDate.formatDMY(getMaintenanceExpiryDate()));
            }
            else{
                msg = i18n.getText("zephyr.upgrades.available.until", outlookDate.formatDMY(getMaintenanceExpiryDate()));
            }
        }
        else{
            return null;
        }
        return "<br><small>(" + msg + ")</small>";
    }

	public String getBriefMaintenanceStatusMessage(final I18nHelper i18n){
        String msg;
        if (!isEntitledToSupport()){
            msg = i18n.getText("admin.license.maintenance.status.unsupported");
        }
        else{
            msg = i18n.getText("admin.license.maintenance.status.supported.valid");
            // if eval or new build old license, check license expiry
            if ((isEvaluation() || isNewBuildWithOldLicense())){
                if (isExpired()){
                    msg = i18n.getText("admin.license.maintenance.status.supported.expired");
                }
            }
            // otherwise (regular license), check maintenance end date
            else if (isMaintenanceExpired()){
                msg = i18n.getText("admin.license.maintenance.status.supported.expired");
            }
        }
        return msg;
    }

    /**
     * Return the HTML message that describes the current status of the license.
     *
     * @param user the user for whom the message should be i18n'ed
     * @param delimiter the line delimiter for the message
     * @return the status message
     */
	public String getLicenseStatusMessage(@Nullable User user, String delimiter) {
		return null;
	}

	public String getLicenseStatusMessage(I18nHelper i18n, OutlookDate outlookDate, String delimiter) 
	{
		return "";
	}

    
    private Date getLicenseExpiry(){
        if (isEvaluation()){
            return license.getExpiryDate();
        }
        else if (isNewBuildWithOldLicense()){
            return license.getMaintenanceExpiryDate();
        }
        
        return null;
    }

    private boolean isNewBuildWithOldLicense(){
        return license.getMaintenanceExpiryDate().compareTo(new Date(buildDate.getMillis())) < 0 && hasLicenseTooOldForBuildConfirmationBeenDone();
    }

    private String getTimeUntilExpiry(final I18nHelper i18n){
        return DateUtils.dateDifference(new Date().getTime(), getLicenseExpiry().getTime(), 2, i18n.getDefaultResourceBundle());
    }

    private boolean isMaintenanceExpired(){
       	final Date expiry = getMaintenanceExpiryDate();
    	return  (expiry != null) && (new Date().getTime() - expiry.getTime() > 0L);
    }

    private Date getMaintenanceExpiryDate(){
        return license.getMaintenanceExpiryDate();
    }
    
	public String getLicenseEdition() {
		return license.getLicenseEdition();
	}

	public String getLicenseId() {
		return license.getLicenseId();
	}

	public String getOrganisationId() {
		return license.getOrganisationId();
	}

	public String getOrganisationEmail() {
		return license.getOrganisationEmail();
	}

	public boolean isOnDemand() {
		return false;
	}

	@Override
	public LicenseStatusMessage getLicenseStatusMessage(I18nHelper i18n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<LicenseContact> getContacts() {
		// TODO Auto-generated method stub
		return null;
	}
}