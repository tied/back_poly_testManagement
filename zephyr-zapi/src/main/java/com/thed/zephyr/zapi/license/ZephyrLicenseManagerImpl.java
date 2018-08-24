package com.thed.zephyr.zapi.license;

import com.atlassian.jira.application.ApplicationKeys;
import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.license.JiraLicenseManager;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.zapi.util.ApplicationConstants;
import com.thed.zephyr.zapi.util.ZephyrLicenseException;
import org.apache.log4j.Logger;


public class ZephyrLicenseManagerImpl implements ZephyrLicenseManager {

    private static final Logger log = Logger.getLogger(ZephyrLicenseManagerImpl.class);

    private final JiraLicenseService jiraLicenseService;
    private final I18nHelper i18nHelper;
    private final PluginLicenseManager licenseManager;
    private final JiraLicenseManager jiraLicenseManager;


    public ZephyrLicenseManagerImpl(final JiraLicenseService jiraLicenseService, PluginLicenseManager licenseManager,
                                    JiraLicenseManager jiraLicenseManager) {
        this.licenseManager = licenseManager;
        this.jiraLicenseManager = jiraLicenseManager;
        this.i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        this.jiraLicenseService = jiraLicenseService;
    }


    @Override
    public void verify() throws ZephyrLicenseException {
        if (isMPLicenseStored()) {
            doVerifyWithUPM();
        }
    }

    /**
     * @return
     */
    private Boolean isMPLicenseStored() {
        //Check and see if a license is currently stored.
        //This accessor method can be used whether or not a licensing-aware UPM is present.
        if (licenseManager.getLicense().isDefined())
            return true;
        else
            return false;
    }

    //https://developer.atlassian.com/display/UPM/License+Validation+Rules
    private void doVerifyWithUPM() throws ZephyrLicenseException {
        PluginLicense pluginLicense;
        try {
            pluginLicense = licenseManager.getLicense().get();
        } catch (Exception e) {
            log.fatal("Error in querying UPM ", e);
            throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_NOT_LICNESED, i18nHelper.getText("zephyr.license.not.licensed"));
        }
        //Check and see if the stored license has an error. If not, it is currently valid.
        if (!pluginLicense.isValid()) {
            switch (pluginLicense.getError().get()) {
                case EXPIRED:
                    throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_EVAL_EXPIRED, i18nHelper.getText("zephyr.license.eval.expired"));
                case USER_MISMATCH:
                    checkUserLimitCompatibility(pluginLicense.isUnlimitedEdition(), pluginLicense.getEdition().get(), getJIRALicenseDetails());
                    break;
                case VERSION_MISMATCH:
                    //verifyPluginVersionAndMaintenanceDate(pluginLicense.getMaintenanceExpiryDate().get().toDate());
                    break;
                case TYPE_MISMATCH:
                    throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, i18nHelper.getText("zephyr.marketplace.license.mismatch.error", pluginLicense.getPluginName(), pluginLicense.getLicenseType().name()));
            }

            throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_ACTIVATE_LICENSE, i18nHelper.getText("zephyr.license.activate"));
        }

    }

    private LicenseDetails getJIRALicenseDetails() {
        return jiraLicenseManager.getLicense(ApplicationKeys.SOFTWARE).getOrElse(jiraLicenseManager.getLicense(ApplicationKeys.CORE).getOrNull());
    }

    public PluginLicense getZephyrMarketplaceLicense() {
        if (licenseManager.getLicense().isDefined())
            return licenseManager.getLicense().get();

        return null;
    }

    private void checkUserLimitCompatibility(Boolean isUnlimited, Integer maximumNumberOfUsers, LicenseDetails jiraLicenseDetails) throws ZephyrLicenseException {
        if (isUnlimited) return;

        if (jiraLicenseDetails.isUnlimitedNumberOfUsers() && (maximumNumberOfUsers < 500))
            throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, limitCompatibilityMessage(isUnlimited, maximumNumberOfUsers, jiraLicenseDetails));

        if (jiraLicenseDetails.getJiraLicense().getMaximumNumberOfUsers() - 1 > maximumNumberOfUsers)
            throw new ZephyrLicenseException(ApplicationConstants.ZEPHYR_LICENSE_COMPATIBILITY_ERROR, limitCompatibilityMessage(isUnlimited, maximumNumberOfUsers, jiraLicenseDetails));
    }

    private String limitCompatibilityMessage(Boolean isUnlimited, Integer maximumNumberOfUsers, LicenseDetails jiraLicenseDetails) {
        String zephyrLimit = isUnlimited ? "Unlimited" : String.valueOf(maximumNumberOfUsers);
        String jiraLimit = jiraLicenseDetails.isUnlimitedNumberOfUsers() ? "Unlimited" : String.valueOf(jiraLicenseDetails.getJiraLicense().getMaximumNumberOfUsers());

        return ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.license.limit.compatibilitymessage", zephyrLimit, jiraLimit);
    }


}