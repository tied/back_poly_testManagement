package com.thed.zephyr.je.config.action;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.JiraUtil;

/**
 * @author manjunath
 *
 */
public class ZephyrDatacenter extends JiraWebActionSupport{

	private final ZephyrLicenseManager zLicenseManager;
	
	public ZephyrDatacenter(ZephyrLicenseManager zLicenseManager) {
		this.zLicenseManager = zLicenseManager;
	}

	@Override
	public String doDefault() throws Exception {
		ZephyrLicenseVerificationResult licenseVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
		if(!licenseVerificationResult.isValid())
			return getRedirect(licenseVerificationResult.getForwardURI().toString());		
		return SUCCESS;
	}
}
