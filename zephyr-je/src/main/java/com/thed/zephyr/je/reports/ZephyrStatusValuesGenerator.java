package com.thed.zephyr.je.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.I18nBean;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ZephyrStatusValuesGenerator implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(ZephyrStatusValuesGenerator.class);

    public ZephyrStatusValuesGenerator() {
    }

    @Override
    public Map getValues(Map params) {
        GenericValue projectGV = (GenericValue) params.get("project");
        ApplicationUser remoteUser = (ApplicationUser) params.get("User");
        try {
            I18nBean i18n = new I18nBean(remoteUser);
            Collection<Status> issueStatus = JiraUtil.getIssueStatusesForProject(projectGV.getLong("id"));
            OrderedMap statuses = ListOrderedMap.decorate(new HashMap(issueStatus.size()));
            for (Status status : issueStatus) {
                statuses.put(status.getId(), status.getNameTranslation(i18n));
            }
            return statuses;
        } catch (Exception e) {
            log.error("Could not retrieve statuses for the project: " + (projectGV != null ? projectGV.getString("id") : "Project is null."), e);
            return null;
        }
    }
}
