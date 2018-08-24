package com.thed.zephyr.je.config.license;

import java.util.Collection;
import java.util.Date;

import javax.annotation.Nullable;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.util.OutlookDate;
import com.thed.zephyr.util.ZFJLicenseDetails;

public class NullLicenseDetails implements ZFJLicenseDetails
{

/*
 * - The type NullLicenseDetails must implement the inherited abstract method 
	 LicenseDetails.getLicenseExpiryStatusMessage(User)
	- The type NullLicenseDetails must implement the inherited abstract method LicenseDetails.getLicenseStatusMessage(User, 
	 String)
	- The type NullLicenseDetails must implement the inherited abstract method LicenseDetails.getSupportRequestMessage(User)	
 */
    static final ZFJLicenseDetails NULL_LICENSE_DETAILS = new NullLicenseDetails();

    private NullLicenseDetails()
    {}

    @Override
	public String getSupportRequestMessage(final I18nHelper i18n, final OutlookDate outlookDate)
    {
        return null;
    }

    /**
     * Return the HTML message for support request denial or send e-mail success page.
     *
     * @param user a User object for the calling user
     * @return the support request message
     */
    @Override
	public String getSupportRequestMessage(User user) {
		return null;
	}
    
    @Override
	public String getMaintenanceEndString(final OutlookDate outlookDate)
    {
        return null;
    }

	@Override
	public boolean isStarter() 
	{
		return false;
	}

    @Override
	public boolean isUnlimitedNumberOfUsers()
    {
        return false;
    }

    @Override
	public int getMaximumNumberOfUsers()
    {
        return 0;
    }

    @Override
	public boolean isLicenseSet()
    {
        return false;
    }

    @Override
	public int getLicenseVersion()
    {
        return 0;
    }

    @Override
	public String getDescription()
    {
        return "";
    }

    @Override
	public String getPartnerName()
    {
        return null;
    }

    @Override
	public boolean isExpired()
    {
        return false;
    }

    @Override
	public String getPurchaseDate(final OutlookDate outlookDate)
    {
        return "";
    }

    @Override
	public boolean isEvaluation()
    {
        return false;
    }

    @Override
	public boolean isCommercial()
    {
        return false;
    }

    @Override
	public boolean isPersonalLicense()
    {
        return false;
    }

    @Override
	public boolean isCommunity()
    {
        return false;
    }

    @Override
	public boolean isOpenSource()
    {
        return false;
    }

    @Override
	public boolean isNonProfit()
    {
        return false;
    }

    @Override
	public boolean isDemonstration()
    {
        return false;
    }

    @Override
	public boolean isDeveloper()
    {
        return false;
    }

    @Override
	public String getOrganisation()
    {
        return "<Unknown>";
    }

    @Override
	public boolean isEntitledToSupport()
    {
        return false;
    }

    @Override
	public boolean isLicenseAlmostExpired()
    {
        return false;
    }

    @Override
	public boolean hasLicenseTooOldForBuildConfirmationBeenDone()
    {
        return false;
    }

    @Override
	public String getLicenseString()
    {
        return "";
    }

    @Override
	public boolean isMaintenanceValidForBuildDate(final Date currentBuildDate)
    {
        return false;
    }

    @Override
	public String getSupportEntitlementNumber()
    {
        return null;
    }

    @Override
	public String getLicenseStatusMessage(final I18nHelper i18n, final OutlookDate outlookDate, final String delimiter)
    {
        return null;
    }

    /**
     * Return the HTML message that describes the current status of the license.
     *
     * @param user the user for whom the message should be i18n'ed
     * @param delimiter the line delimiter for the message
     * @return the status message
     */
    @Override
	public String getLicenseStatusMessage(@Nullable User user, String delimiter) {
		return null;
	}
    
    @Override
	public String getLicenseExpiryStatusMessage(final I18nHelper i18n, final OutlookDate outlookDate)
    {
        return null;
    }

    /**
     * Return the HTML message that briefly describes the expiry status of the license. Intended for use with the Admin
     * Portlet.
     *
     * @param user the user for whom the message should be i18n'ed
     * @return the status message, null for normal license outside of support period
     */
    @Override
	public String getLicenseExpiryStatusMessage(@Nullable User user){
    	return null;
    }
 
    @Override
	public String getBriefMaintenanceStatusMessage(final I18nHelper i18n)
    {
        return null;
    }

	public boolean isOnDemand() {
		// TODO Auto-generated method stub
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