package com.thed.zephyr.je.plugin.navigation;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugin.navigation.DefaultPluggableFooter;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import com.thed.zephyr.util.ZephyrLicenseException;

public class EvalFooter extends DefaultPluggableFooter {

	@Override
	public String getFullFooterHtml(HttpServletRequest request) {
		if(shouldShowFooter(request))
			return super.getFullFooterHtml(request);
		else 
			return "";
	}

	@Override
	public String getSmallFooterHtml(HttpServletRequest request) {
		if(shouldShowFooter(request))
			return super.getSmallFooterHtml(request);
		else 
			return "";
	}

	public boolean shouldShowFooter(HttpServletRequest request) {
		ZephyrLicenseManager zLicManager = (ZephyrLicenseManager) ZephyrComponentAccessor.getInstance().getComponent("zephyr-je-Licensemanager");
		Boolean isLicenseEval = true;
		try {
			isLicenseEval = zLicManager.isEval();
		} catch (ZephyrLicenseException e) {
			e.printStackTrace();
		}
		if(!isLicenseEval){
			return false;
		}
		String url = request.getRequestURL().toString();	
		String queryParam = (String) request.getAttribute("webwork.view_uri");		//query params (for project tabs) - #selectedTab=com.thed.zephyr.je%3Apdb_cycle_panel_section
		//We are showing footer on all project tabs. Reason is that footer is not retrieved on tab switch. We need to add logic on client side to hide on tab click
		if(StringUtils.indexOfIgnoreCase(queryParam, "browseproject") > -1 || url.indexOf("ExecuteTest") > -1 || url.indexOf("ZephyrWelcome") > -1 || url.indexOf("enav") > -1 || url.indexOf("ZQLManageFilters") > -1){
			return true;
		}
		return false;
	}
}