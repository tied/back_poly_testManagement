package com.thed.zephyr.je.zql.core.generator;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.comparator.LocaleSensitiveVersionNameComparator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.*;
/**
 * FixVersion Custom Clause Value generator which includes showing the unscheduled version in the ZQL
 * @author niravshah
 *
 */
public class FixVersionClauseValueGenerator implements ClauseValuesGenerator {

    private final VersionManager versionManager;
    private final PermissionManager permissionManager;
    private final I18nHelper.BeanFactory beanFactory;

    public FixVersionClauseValueGenerator(final VersionManager versionManager, final PermissionManager permissionManager, I18nHelper.BeanFactory beanFactory)
    {
        this.versionManager = versionManager;
        this.permissionManager = permissionManager;
        this.beanFactory = beanFactory;
    }

    public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults)
    {
        final List<Version> versions = new ArrayList<Version>(versionManager.getAllVersions());

        Collections.sort(versions, new LocaleSensitiveVersionNameComparator(getLocale(searcher)));

        final Set<Result> versionValues = new LinkedHashSet<Result>();

        for (Version version : versions)
        {
            if (versionValues.size() == maxNumResults)
            {
                break;
            }
            final String lowerCaseVersionName = version.getName().toLowerCase();
            if (StringUtils.isBlank(valuePrefix) || lowerCaseVersionName.startsWith(valuePrefix.toLowerCase()))
            {
                final Project project = version.getProjectObject();
                if (project != null && permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, searcher))
                {
                    versionValues.add(new Result(version.getName()));
                }
            }
        }

        String unscheduledVersion = beanFactory.getInstance(searcher).getText("zephyr.je.version.unscheduled",
                ApplicationConstants.UNSCHEDULED_VERSION_NAME);
        if(StringUtils.isNotEmpty(unscheduledVersion) && StringUtils.startsWith(unscheduledVersion.toLowerCase(),valuePrefix.toLowerCase())) {
            versionValues.add(new Result(unscheduledVersion));
        }
        return new Results(new ArrayList<Result>(versionValues));
    }

    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}
