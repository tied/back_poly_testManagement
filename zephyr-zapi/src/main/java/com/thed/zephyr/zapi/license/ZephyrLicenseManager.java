package com.thed.zephyr.zapi.license;

import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.zapi.util.ZephyrLicenseException;

public interface ZephyrLicenseManager {
    void verify() throws ZephyrLicenseException;

    PluginLicense getZephyrMarketplaceLicense();
}
