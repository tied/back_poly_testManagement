package com.thed.zephyr.je.config.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

public class ViewZephyrCustomField extends JiraWebActionSupport {

    private final ZephyrLicenseManager zLicenseManager;

    public ViewZephyrCustomField(ZephyrLicenseManager zLicenseManager){
        this.zLicenseManager = zLicenseManager;
    }

    @Override
    protected void doValidation() {
        super.doValidation();
    }

    @Override
    public String doDefault() throws Exception {
        ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
        if(!licenseVerificationResult.isValid())
            return getRedirect(licenseVerificationResult.getForwardURI().toString());

        return SUCCESS;
    }
}
