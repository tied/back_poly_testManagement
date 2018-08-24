package com.thed.zephyr.je.plugin.navigation;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.plugin.navigation.DefaultPluggableTopNavigation;
import com.atlassian.security.random.DefaultSecureTokenGenerator;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.CodecUtils;
import com.thed.zephyr.util.JiraUtil;

public class ZFJPluggableTopNavigation extends DefaultPluggableTopNavigation {
	@Override
	public String getHtml(HttpServletRequest request) {
        final String crytoPart = DefaultSecureTokenGenerator.getInstance().generateToken();
        final String cryptedString = System.currentTimeMillis() + "|" + ApplicationConstants.ACCESS_ALL + "|" + crytoPart;
        CodecUtils codecUtils = new CodecUtils();
		String encryptedString = codecUtils.encrypt(cryptedString);
		String encKeyFld = System.getProperty("zephyr.header.field", ApplicationConstants.ENCRYPTED_STRING);
		request.setAttribute("encKeyFld", encKeyFld);
		request.setAttribute("encKeyVal", encryptedString);
        request.setAttribute("showWF", JiraUtil.showWorkflow());

		//request.setAttribute("zapi", o)
		return super.getHtml(request);
	}
}