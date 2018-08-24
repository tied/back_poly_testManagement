package com.thed.zephyr.je.zql.resolver;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.thed.zephyr.util.ApplicationConstants;

public class FixVersionResolver implements NameResolver<Version> {
    private final VersionManager versionManager;

    public FixVersionResolver(final VersionManager versionManager)
    {
        this.versionManager = versionManager;
    }

    public List<String> getIdsFromName(final String name)
    {
        notNull("name", name);
        Collection<Version> versions = versionManager.getVersionsByName(name);
        Function<Version, String> function = new Function<Version, String>()
        {
            public String get(final Version input)
            {
                return input.getId().toString();
            }
        };
        List<String> versionList = CollectionUtil.transform(versions, function);
        List<String> allVersions = new ArrayList<String>(versionList);
        if(StringUtils.equalsIgnoreCase(name, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.je.version.unscheduled"))) {
        	allVersions.add(String.valueOf(ApplicationConstants.UNSCHEDULED_VERSION_ID));
        }
        return allVersions;
    }

    public boolean nameExists(final String name)
    {
        notNull("name", name);
        Collection<Version> versions = versionManager.getVersionsByName(name);
        return !versions.isEmpty();
    }

    public boolean idExists(final Long id)
    {
        notNull("id", id);
        if(get(id) != null) {
            return true;
        } else if (id == ApplicationConstants.UNSCHEDULED_VERSION_ID) {
            return true;
        }
        return false;
    }

    public Version get(final Long id)
    {
        return versionManager.getVersion(id);
    }

    ///CLOVER:OFF
    public Collection<Version> getAll()
    {
        return versionManager.getAllVersions();
    }
}
