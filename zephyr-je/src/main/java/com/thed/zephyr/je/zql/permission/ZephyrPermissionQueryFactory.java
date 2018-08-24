package com.thed.zephyr.je.zql.permission;

import org.apache.lucene.search.Query;

import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;

public interface ZephyrPermissionQueryFactory {
    /**
     * Generate a permission query for a zephyr specific permission.
     * 
     * @param searcher the user who is doing the searching
     * @param permissionKey the specific permission
     * @return a permission query for that user
     */
    Query getQuery(final ApplicationUser searcher, final ProjectPermissionKey permissionKey);
}
