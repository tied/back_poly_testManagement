package com.thed.zephyr.je.reports;

import com.atlassian.configurable.ValuesGenerator;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.I18nBean;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.*;

/**
 * Created by mukul on 1/14/15.
 */
public class ZephyrVersionValuesGenerator implements ValuesGenerator {
    private static final Logger log = Logger.getLogger(ZephyrVersionValuesGenerator.class);

    public ZephyrVersionValuesGenerator() {
    }

    @Override
    public Map getValues(Map params) {
    	ComponentAccessor.getWebResourceManager().requireResource("com.thed.zephyr.je:zephyr-reports-resources");
        GenericValue projectGV = (GenericValue) params.get("project");
        ApplicationUser remoteUser = (ApplicationUser) params.get("User");
        try {
            VersionManager versionManager = ComponentAccessor.getVersionManager();
            I18nBean i18n = new I18nBean(remoteUser);

            Collection unreleasedVersions = versionManager.getVersionsUnreleased(projectGV.getLong("id"), false);
            OrderedMap unreleased = ListOrderedMap.decorate(new HashMap(unreleasedVersions.size()));
            unreleased.put(new Long(-2L), i18n.getText("common.filters.unreleasedversions"));
            unreleased.put(new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID), "- " + i18n.getText("zephyr.je.version.unscheduled"));

            if (!unreleasedVersions.isEmpty()) {
                Iterator unreleasedItr = unreleasedVersions.iterator();
                while (unreleasedItr.hasNext()) {
                    Version released = (Version) unreleasedItr.next();
                    unreleased.put(released.getId(), "- " + released.getName());
                }
            }

            OrderedMap released1 = ListOrderedMap.decorate(new HashMap(unreleasedVersions.size()));
            ArrayList releasedVersions = new ArrayList(versionManager.getVersionsReleased(projectGV.getLong("id"), false));
            if (!releasedVersions.isEmpty()) {
                released1.put(new Long(-3L), i18n.getText("common.filters.releasedversions"));
                Collections.reverse(releasedVersions);
                Iterator releasedVersionsItr = releasedVersions.iterator();

                while (releasedVersionsItr.hasNext()) {
                    Version versions = (Version) releasedVersionsItr.next();
                    released1.put(versions.getId(), "- " + versions.getName());
                }
            }

            int size1 = unreleased.size() + released1.size();
            OrderedMap versions1 = ListOrderedMap.decorate(new HashMap(size1));
            versions1.putAll(unreleased);
            versions1.putAll(released1);
            return versions1;
        } catch (Exception e) {
            log.error("Could not retrieve versions for the project: " + (projectGV != null ? projectGV.getString("id") : "Project is null."), e);
            return null;
        }
    }
}
