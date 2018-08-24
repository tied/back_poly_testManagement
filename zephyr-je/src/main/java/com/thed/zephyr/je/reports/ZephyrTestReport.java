package com.thed.zephyr.je.reports;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.plugin.report.impl.AbstractReport;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.google.common.collect.Maps;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mukul on 1/15/15.`
 */
public class ZephyrTestReport extends AbstractReport {
    private static final Logger log = Logger.getLogger(ZephyrTestReport.class);
    private final VersionManager versionManager;

    public ZephyrTestReport(final VersionManager versionManager) {
        this.versionManager = versionManager;
    }

    @Override
    public String generateReportHtml(ProjectActionSupport action, Map reqParams) throws Exception {
        try {
            ApplicationUser remoteUser = action.getLoggedInApplicationUser();
            //Project selectedProject = action.getSelectedProject();
            String versionIdString = (String) reqParams.get("versionId");
            Version version = null;
            if (!versionIdString.equals("-1")) {
                Long versionIdLong = new Long(versionIdString);
                version = this.versionManager.getVersion(versionIdLong);
            }
            TextUtils textUtils = new TextUtils();
            String reportKey = (String) reqParams.get("reportKey");

            String reportForTitle = getDescriptor().getI18nBean().getText("je.gadget.testcase.report.for.title", "Testcase Report for");
            if (reportKey.contains(ApplicationConstants.PROJECT_CENTRIC_VIEW_EXECUTION_REPORT))
                reportForTitle = getDescriptor().getI18nBean().getText("je.gadget.testcase.execution.report.for.title", "Testcase Execution Report for");
            else if (reportKey.contains(ApplicationConstants.PROJECT_CENTRIC_VIEW_BURNDOWN_REPORT))
                reportForTitle = getDescriptor().getI18nBean().getText("je.gadget.testcase.burndown.report.for.title", "Test Execution Burndown for");
            else if (reportKey.contains(ApplicationConstants.PROJECT_CENTRIC_VIEW_TOP_DEFECTS_REPORT))
                reportForTitle = getDescriptor().getI18nBean().getText("je.gadget.testcase.topdefects.report.for.title", "Top Defects Report for");


            HashMap velocityParams = Maps.newHashMap();
            velocityParams.put("reportKey", reportKey);
            velocityParams.put("remoteUser", remoteUser);
            velocityParams.put("action", action);
            velocityParams.put("version", version);
            velocityParams.put("textUtils", textUtils);
            velocityParams.put("versionIdString", versionIdString);
            velocityParams.put("reportKey", reportKey);
            velocityParams.put("groupFld", reqParams.get("groupFld"));
            velocityParams.put("selectedProjectId", reqParams.get("selectedProjectId"));
            velocityParams.put("sprintId", reqParams.get("sprintId"));
            velocityParams.put("cycle", reqParams.get("cycle"));
            velocityParams.put("defectsCount", reqParams.get("defectsCount"));
            velocityParams.put("reportForTitle", reportForTitle);
            velocityParams.put("webResourceManager", ComponentAccessor.getWebResourceManager());

            String statuses = "";

            if (reqParams.get("pickStatus") instanceof String) {
                statuses = (String) reqParams.get("pickStatus");
            } else if (reqParams.get("pickStatus") instanceof String[]) {
                String[] pickStatuses = (String[]) reqParams.get("pickStatus");
                for (int i = 0; i < pickStatuses.length; i++) {
                    statuses += pickStatuses[i] + "|";
                }
                statuses = statuses.substring(0, statuses.length() - 1);
            }

            velocityParams.put("pickStatus", statuses);

            return descriptor.getHtml("view", velocityParams);
        } catch (Exception e) {
            log.error(e, e);
            return null;
        }
    }
}
