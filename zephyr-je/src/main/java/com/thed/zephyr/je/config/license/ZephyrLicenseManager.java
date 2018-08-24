package com.thed.zephyr.je.config.license;

import com.atlassian.jira.plugin.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.util.ZFJLicenseDetails;
import com.thed.zephyr.util.ZephyrLicenseException;

import java.net.URI;

public interface ZephyrLicenseManager extends PluginLicenseManager
{
    ZephyrLicense getLicense();

    ZephyrLicense getLicenseFromStore();
    
    String getUserEnteredLicenseStringFromStore();

    ZFJLicenseDetails getLicenseDetails(ZephyrLicense zephyrLicense);

    void verifyOnStartup() throws ZephyrLicenseException;

    void verify() throws ZephyrLicenseException;
    
    void doVerifyInternal(boolean checkUserLimit) throws ZephyrLicenseException;
    
    String getServerId();

	URI getRedirectionUrl();
    
    PluginLicense getZephyrMarketplaceLicense();

	boolean isEval() throws ZephyrLicenseException;
}
