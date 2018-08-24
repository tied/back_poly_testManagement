package com.thed.zephyr.je.permissions.service;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.roles.ProjectRole;

/**
 * Service responsible for
 * 1. Create global user group for QA
 * 2. Create global Role for QA
 * 3. Configure Role at Project level
 * 4. Add users to Role at Project level
 * 5. Assign QA Role to Zephyr Permissions at each Project level
 */
public interface PermissionConfigManager {

    void addAndConfigurePermissions();

    void removeAndCleanUpPermissions();

    Group createOrFetchGroup(String groupName);

    void addExistingGroupsTo(Group group);

    ProjectRole createOrFetchProjectRole(Group group);

    void addUsersToProjectRole(ProjectRole projectRole, Group zephyrQAUserGroup);

    void addRoleToPermissionSchemes(ProjectRole zephyrProjectRole);
}
