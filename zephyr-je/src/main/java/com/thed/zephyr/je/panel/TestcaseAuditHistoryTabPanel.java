package com.thed.zephyr.je.panel;

import com.atlassian.jira.plugin.issuetabpanel.*;
import com.thed.zephyr.je.action.TestcaseAuditPanelIssueAction;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/13/13
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestcaseAuditHistoryTabPanel extends AbstractIssueTabPanel2 {

    @Override
    public ShowPanelReply showPanel(ShowPanelRequest showPanelRequest) {
        return ShowPanelReply.create(StringUtils.equals(showPanelRequest.issue().getIssueTypeObject().getId(), JiraUtil.getTestcaseIssueTypeId()));
    }

    @Override
    public GetActionsReply getActions(GetActionsRequest getActionsRequest) {
        if (!getActionsRequest.isAsynchronous()){
            //return GetActionsReply.create(new AjaxTabPanelAction(request));
            return GetActionsReply.create();
        }
        return GetActionsReply.create(new TestcaseAuditPanelIssueAction(descriptor()));
    }
}
