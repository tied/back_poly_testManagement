package com.thed.zephyr.je.zql.permission.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.ofbiz.core.entity.GenericEntityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.PermissionSchemeEntry;
import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.jira.permission.PermissionTypeManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.security.type.SecurityType;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.zql.permission.ZephyrPermissionQueryFactory;
/**
 * @author niravshah
 *
 */
public class ZephyrPermissionQueryFactoryImpl implements
		ZephyrPermissionQueryFactory {

    private static final Logger log = LoggerFactory.getLogger(ZephyrPermissionQueryFactoryImpl.class);

    private final PermissionManager permissionManager;
    private final PermissionSchemeManager permissionSchemeManager;
    private final PermissionTypeManager permissionTypeManager;
    private final ZephyrPermissionManager zephyrPermisisonManager;

    public ZephyrPermissionQueryFactoryImpl(final PermissionTypeManager permissionTypeManager,ZephyrPermissionManager zephyrPermisisonManager) {
        this.permissionManager = ComponentAccessor.getPermissionManager();
        this.permissionSchemeManager = ComponentAccessor.getPermissionSchemeManager();
        this.permissionTypeManager = permissionTypeManager;
        this.zephyrPermisisonManager=zephyrPermisisonManager;
    }

    @Override
    public Query getQuery(final ApplicationUser searcher, final ProjectPermissionKey permissionKey) {
        try {
            final BooleanQuery query = new BooleanQuery();
            final Collection<Project> projects = permissionManager.getProjects(ProjectPermissions.BROWSE_PROJECTS, searcher);
            final Set<Query> projectQueries = new LinkedHashSet<Query>();
            final BooleanQuery permissionQuery = new BooleanQuery();
            
            for(Project project : projects) {
	            final boolean hasPermission = zephyrPermisisonManager.validateUserPermission(permissionKey, project, searcher,project.getId());
	            if (hasPermission) {
	                collectProjectTerms(project, searcher, projectQueries, permissionKey);
	            }
            }
            for (final Query projectQuery : projectQueries) {
                permissionQuery.add(projectQuery, BooleanClause.Occur.SHOULD);
            }
            // add them to the permission query
            if (!permissionQuery.clauses().isEmpty()) {
                query.add(permissionQuery, BooleanClause.Occur.MUST);
            }
            return query;
        } catch (final GenericEntityException e) {
            log.error("Error constructing query: " + e, e);
            return null;
        }
    }

     /**
     * Loops around the permission schemes for the current project and adds a query for the SecurityType if there is one
     * in scheme.
     *
     * @param project The project for which we need to construct the query
     * @param searcher The user conducting the search
     * @param queries The collection of queries already generated for projects
     * @throws org.ofbiz.core.entity.GenericEntityException If there's a problem retrieving permissions.
     */
    private void collectProjectTerms(final Project project, final ApplicationUser searcherUser, final Set<Query> queries, 
    		final ProjectPermissionKey permissionId) throws GenericEntityException {
        final Long schemeId = permissionSchemeManager.getSchemeIdFor(project);
        final Collection<PermissionSchemeEntry> entities = permissionSchemeManager.getPermissionSchemeEntries(schemeId, permissionId);
        for (final PermissionSchemeEntry schemeEntry : entities) {
            final SecurityType securityType = permissionTypeManager.getSecurityType(schemeEntry.getType());
            if (securityType != null) {
                try {
                    if (userHasPermissionForProjectAndSecurityType(searcherUser, project, schemeEntry.getParameter(), securityType)) {
                        final Query tempQuery = securityType.getQuery(searcherUser, project, schemeEntry.getParameter());
                        if (tempQuery != null) {
                            queries.add(tempQuery);
                        }
                    }
                }
                catch (final Exception e) {
                    log.debug("Could not add query for security type:" + securityType.getDisplayName(), e);
                }
            } else {
                log.debug("Could not find security type:" + schemeEntry.getType());
            }
        }
    }

    /**
     * Tests if the specified user has permission for the specified security type in the specified project given the
     * context of the permission scheme entity.
     *
     * @param searcher the user; may be null if user is anonymous
     * @param project the project
     * @param parameter the permission parameter (group name etc)
     * @param securityType the security type
     * @return true if the user has permission; false otherwise
     */
    private boolean userHasPermissionForProjectAndSecurityType(final ApplicationUser searcher, final Project project, 
    		final String parameter, final SecurityType securityType) {
        boolean hasPermission;
        if (searcher == null) {
            hasPermission = securityType.hasPermission(project, parameter);
        } else {
            hasPermission = securityType.hasPermission(project, parameter, searcher, false);
        }
        return hasPermission;
    }
}
