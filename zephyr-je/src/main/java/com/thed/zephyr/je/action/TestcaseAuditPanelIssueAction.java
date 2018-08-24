package com.thed.zephyr.je.action;

import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueAction;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;

import java.util.Date;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/16/13
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestcaseAuditPanelIssueAction extends AbstractIssueAction{

    public TestcaseAuditPanelIssueAction(IssueTabPanelModuleDescriptor descriptor){
        super(descriptor);
    }

    @Override
    public Date getTimePerformed() {
        return new Date();
    }

    @Override
    protected void populateVelocityParams(Map map) {
        map.put("panelContent","Testcase details Audit Issue tab panel content from velocity context.");
    }
}
