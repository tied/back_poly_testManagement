package com.thed.zephyr.je.action;

import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.security.random.DefaultSecureTokenGenerator;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.CodecUtils;
import com.thed.zephyr.util.JiraUtil;
import webwork.action.ActionContext;

/**
 * Created by mukul on 1/21/15.
 */
public class ZephyrEncKeyLoadAction extends AbstractIssueSelectAction {

    @Override
    protected String doExecute() throws Exception {
        final String crytoPart = DefaultSecureTokenGenerator.getInstance().generateToken();
        final String cryptedString = System.currentTimeMillis() + "|" + ApplicationConstants.ACCESS_ALL + "|" + crytoPart;
        CodecUtils codecUtils = new CodecUtils();
        String encryptedString = codecUtils.encrypt(cryptedString);
        String encKeyFld = System.getProperty("zephyr.header.field", ApplicationConstants.ENCRYPTED_STRING);
        ActionContext.getRequest().setAttribute("encKeyFld", encKeyFld);
        ActionContext.getRequest().setAttribute("encKeyVal", encryptedString);
        ActionContext.getRequest().setAttribute("showWF", JiraUtil.showWorkflow());

        return super.doExecute();
    }
}
