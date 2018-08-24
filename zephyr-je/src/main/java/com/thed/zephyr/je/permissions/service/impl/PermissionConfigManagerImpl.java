package com.thed.zephyr.je.permissions.service.impl;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.exception.embedded.InvalidGroupException;
import com.atlassian.fugue.Option;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.GlobalPermissionType;
import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.jira.permission.ProjectPermission;
import com.atlassian.jira.permission.ProjectPermissionCategory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.scheme.Scheme;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleImpl;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.component.multigrouppicker.GroupPickerWebComponent;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.PermissionConfigManager;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

import org.apache.commons.lang3.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/**
 * Service responsible for
 * 1. Create global user group for QA
 * 2. Create global Role for QA
 * 3. Configure Role at Project level
 * 4. Add users to Role at Project level
 * 5. Assign QA Role to Zephyr Permissions at each Project level
 */
public class PermissionConfigManagerImpl implements PermissionConfigManager {

    private static final Logger log = LoggerFactory.getLogger(PermissionConfigManagerImpl.class);

    private final GroupManager groupManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectRoleManager projectRoleManager;
    private final ProjectRoleService projectRoleService;
    private final PermissionManager permissionManager;
    private final PermissionSchemeManager permissionSchemeManager;

    private final String permissionType = "projectrole";

    public PermissionConfigManagerImpl(GroupManager groupManager, JiraAuthenticationContext jiraAuthenticationContext, ProjectRoleManager projectRoleManager,
                                       ProjectRoleService projectRoleService, PermissionManager permissionManager) {
        this.groupManager = groupManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectRoleManager = projectRoleManager;
        this.projectRoleService = projectRoleService;
        this.permissionSchemeManager = ComponentAccessor.getPermissionSchemeManager();
        this.permissionManager = permissionManager;
    }

    @Override
    public void addAndConfigurePermissions() {
        try {
            // Enable all Zephyr Permissions in JIRA system for all existing permission schemes
            if (!enableZephyrPermissions()) {
                log.error("Zephyr Permissions ain't added or configured in JIRA System. Please check logs !!!");
                return;
            }

            // Create a Global User Group for Zephyr QA
            String groupName = jiraAuthenticationContext.getI18nHelper().getText("zephyr.plugin.user.group.tester");
            log.debug("Fetch Zephyr Tester User Group with name: " + groupName);
            Group zfjGroup = groupManager.getGroup(groupName);
            if(zfjGroup == null) {
	            Group zephyrQAUserGroup = createOrFetchGroup(jiraAuthenticationContext.getI18nHelper().getText("zephyr.plugin.user.group.tester"));
	            if (null == zephyrQAUserGroup) {
	                log.error("Error creating Zephyr Tester User Group. Please check logs !!!");
	                return;
	            }
	
	            // add existing groups to this new group
	            addExistingGroupsTo(zephyrQAUserGroup);
	
	            // Create Global Zephyr Role and add zephyrQAUserGroup to this Role
	            ProjectRole projectRole = createOrFetchProjectRole(zephyrQAUserGroup);
	
	            // If Zephyr Tester Role create, add users to this role, also add this role to each project
	            if (null == projectRole) {
	                log.error("Error creating Zephyr Tester Project Role. Please check logs !!!");
	                return;
	            }
	            addUsersToProjectRole(projectRole, zephyrQAUserGroup);
	
	            addRoleToPermissionSchemes(projectRole);
	            
	            addJiraAdminRoleToZephyrGlobal(zephyrQAUserGroup);
            } else {
                log.debug("Zephyr Tester User Group " + groupName + " already exists. Skipping creating Zephyr Tester User Group Will check for User Role.");
	            // Create Global Zephyr Role and add zephyrQAUserGroup to this Role
	            ProjectRole projectRole = createOrFetchProjectRole(zfjGroup);
	
	            // If Zephyr Tester Role create, add users to this role, also add this role to each project
	            if (null == projectRole) {
	                log.error("Error creating Zephyr Tester Project Role. Please check logs !!!");
	                return;
	            }
	            addUsersToProjectRole(projectRole, zfjGroup);
	
	            addRoleToPermissionSchemes(projectRole);

	            addJiraAdminRoleToZephyrGlobal(zfjGroup);
            }
        } catch (Exception e) {
            log.error("Error adding and configuring Zephyr Permissions: ", e);
        }
    }

	@Override
    public void removeAndCleanUpPermissions() {
        try {
            JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
    		.setString(ConfigurationConstants.ZEPHYR_PERMISSION_GROUP_CREATE_SETTINGS, Boolean.FALSE.toString());
            if (!disableZephyrPermissions()) {
                log.error("Zephyr Permissions ain't removed or cleaned up in JIRA System. Please check logs !!!");
                return;
            }
        } catch (Exception e) {
            log.error("Error removing and cleaning up Zephyr Permissions: ", e);
        }
    }

    private Boolean enableZephyrPermissions() {
        Boolean arePermissionsEnabled = false;
        try {
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_PROJECT_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_CREATE_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_EDIT_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_DELETE_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_VIEW_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_CREATE_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_EDIT_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_DELETE_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_VIEW_PERMISSION_MODULE_KEY);
            JiraUtil.enablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_COMMENT_PERMISSION_MODULE_KEY);
            arePermissionsEnabled = true;
        } catch (Exception e) {
            log.error("Error enabling ZEPHYR GLOBAL PERMISSION MODULE : ", e);
        }
        return arePermissionsEnabled;
    }

    private Boolean disableZephyrPermissions() {
        Boolean arePermissionsDisabled = false;
        try {
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_PROJECT_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_CREATE_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_EDIT_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_DELETE_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_CYCLE_VIEW_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_CREATE_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_EDIT_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_DELETE_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_VIEW_PERMISSION_MODULE_KEY);
            JiraUtil.disablePluginModuleByKey(ConfigurationConstants.ZEPHYR_EXECUTION_COMMENT_PERMISSION_MODULE_KEY);
            arePermissionsDisabled = true;
        } catch (Exception e) {
            log.error("Error disabling ZEPHYR GLOBAL PERMISSION MODULE : ", e);
        }
        return arePermissionsDisabled;
    }

    @Override
    public Group createOrFetchGroup(String groupName) {
        log.debug("Zephyr Tester User Group " + groupName + " doesn't exist. Creating new Zephyr Tester User Group.");
        Group group = null;
        try {
        	group = groupManager.createGroup(groupName);
        } catch (OperationNotPermittedException e) {
            log.error("Error creating Zephyr Tester User Group, OperationNotPermittedException: ", e);
        } catch (InvalidGroupException e) {
            log.error("Error creating Zephyr Tester User Group, InvalidGroupException: ", e);
        }
        return group;
    }

    @Override
    public void addExistingGroupsTo(Group zephyrQAUserGroup) {
        if (null != zephyrQAUserGroup) {
            log.debug("Adding existing user groups to Zephyr Tester User Group: " + zephyrQAUserGroup.getName());
            try {
                Collection<ApplicationUser> users = ComponentAccessor.getUserManager().getAllUsers();

                for (ApplicationUser user : JiraUtil.safe(users)) {
                    ComponentAccessor.getCrowdService().addUserToGroup(user.getDirectoryUser(), zephyrQAUserGroup);
                }
            } catch (Exception e) {
                log.error("Zephyr Tester User Group " + zephyrQAUserGroup.getName() + " doesn't exist. Adding other user groups to it will be skipped: ", e);
            }
        } else {
            log.error("Zephyr Tester User Group doesn't exist. Adding other user groups to it will be skipped.");
        }
    }

    @Override
    public ProjectRole createOrFetchProjectRole(Group zephyrQAUserGroup) {
        Long zephyrRoleId = 0L;

        ProjectRole projectRole = projectRoleService.getProjectRoleByName(jiraAuthenticationContext.getI18nHelper().getText("zephyr.plugin.global.role"),
                new SimpleErrorCollection());

        //If Zephyr Tester Project Role already exists
        if (null != projectRole) {
            log.debug("Zephyr Project Role already exists, skip to create new.");
            return projectRole;
        } else { // create new Zephyr Tester Project Role
            try {
                // Fetch all existing Roles and get the max ProjectRole Id
                Collection<ProjectRole> projectRoles = projectRoleService.getProjectRoles(new SimpleErrorCollection());

                Ordering<ProjectRole> projectRoleOrdering = new Ordering<ProjectRole>() {
                    @Override
                    public int compare(ProjectRole left, ProjectRole right) {
                        return Longs.compare(left.getId(), right.getId());
                    }
                };

                projectRole = projectRoleOrdering.max(JiraUtil.safe(projectRoles));
                zephyrRoleId = projectRole.getId() + 1;
            } catch (Exception e) {
                log.warn("Error fetching max ProjectRole Id from JIRA: ", e);
                zephyrRoleId += 1l;
            }

            // create new Zephyr Role
            ErrorCollection errorCollection = new SimpleErrorCollection();
            projectRole = projectRoleService.createProjectRole(new ProjectRoleImpl(zephyrRoleId, jiraAuthenticationContext.getI18nHelper().getText("zephyr.plugin.global.role"),
                    jiraAuthenticationContext.getI18nHelper().getText("zephyr.plugin.global.role.desc")), errorCollection);

            for (String name : errorCollection.getErrorMessages()) {
                log.error("Error creating Zephyr Tester Project Role: " + name);
            }
        }
        return projectRole;
    }
    
    // If Zephyr Tester Project Role created, add existing users to it and also add this Role to each existing Project
    @Override
    public void addUsersToProjectRole(ProjectRole projectRole, Group zephyrQAUserGroup) {

        // adding existing user group to Zephyr Project Role
        Collection actorsToAdd = GroupPickerWebComponent.getGroupNamesToAdd(zephyrQAUserGroup.getName());
        ErrorCollection errorCollection = new SimpleErrorCollection();
        projectRoleService.addDefaultActorsToProjectRole(actorsToAdd, projectRole, "atlassian-group-role-actor", errorCollection);

        // Logging any possible errors while fetching JIRA Project Roles
        for (String name : errorCollection.getErrorMessages()) {
            log.error("Error adding User Groups to Zephyr Tester Project Role: " + name);
        }

        // adding Zephyr Project Role to each existing project
        List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
        errorCollection = new SimpleErrorCollection();
        for (Project project : JiraUtil.safe(projects)) {
            projectRoleManager.updateProjectRoleActors(projectRoleManager.getProjectRoleActors(projectRole, project));
            projectRoleService.addActorsToProjectRole(actorsToAdd, projectRole, project, "atlassian-group-role-actor", errorCollection);
        }
        // Logging any possible errors while fetching JIRA Project Roles
        for (String name : errorCollection.getErrorMessages()) {
            log.error("Error adding Zephyr Tester Project Role to each Project: " + name);
        }
    }

    @Override
    public void addRoleToPermissionSchemes(ProjectRole zephyrProjectRole) {
        try {
            // Get all Projects from JIRA
            List<Project> projects = ComponentAccessor.getProjectManager().getProjectObjects();
            for (Project project : JiraUtil.safe(projects)) {

                // Fetch all PROJECT permissions and Filter out ZEPHYR permissions from this
                Collection<ProjectPermission> zephyrProjectPermissions = Collections2.filter(permissionManager.
                        getProjectPermissions(ProjectPermissionCategory.PROJECTS), new Predicate<ProjectPermission>() {
                    @Override
                    public boolean apply(ProjectPermission input) {
                        return StringUtils.startsWith(input.getKey(), "ZEPHYR_");
                    }
                });

                if (null == zephyrProjectPermissions || zephyrProjectPermissions.isEmpty()) {
                    log.error("Zephyr custom permissions not found. Adding role to permission will skip!!!");
                    return;
                }

                // For each permission scheme, add Zephyr Tester Project Role to Zephyr custom permissions
                List<Scheme> permissionSchemes = permissionSchemeManager.getSchemeObjects();
                for (Scheme scheme : JiraUtil.safe(permissionSchemes)) {
                    GenericValue schemeGenericValue = permissionSchemeManager.getScheme(scheme.getId());

                    for (ProjectPermission projectPermission : JiraUtil.safe(zephyrProjectPermissions)) {
                        ProjectPermissionKey projectPermissionKey = projectPermission.getProjectPermissionKey();
                        if (!this.permissionExists(schemeGenericValue, projectPermissionKey, permissionType, String.valueOf(zephyrProjectRole.getId()))) {
                            SchemeEntity schemeEntity = new SchemeEntity(permissionType, String.valueOf(zephyrProjectRole.getId()), projectPermissionKey);
                            permissionSchemeManager.createSchemeEntity(schemeGenericValue, schemeEntity);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error adding Zephyr Tester Project Role to Zephyr custom permissions: ", e);
        }
    }

    private boolean permissionExists(GenericValue schemeGenericValue, ProjectPermissionKey permissionKey, String type, String parameter) throws GenericEntityException {
        return !(permissionSchemeManager.getEntities(schemeGenericValue, permissionKey, type, parameter).isEmpty());
    }
    

    private void addJiraAdminRoleToZephyrGlobal(
			Group zephyrTesterGroup) {
    	Option<GlobalPermissionType> globalPermissionTypeOption =  ComponentAccessor.getGlobalPermissionManager().getGlobalPermission(PermissionType.ZEPHYR_TEST_MANAGEMENT_PERMISSION.toString());
		GlobalPermissionType globalPermissionType = globalPermissionTypeOption.getOrNull();
		if(globalPermissionType != null) {
			ComponentAccessor.getGlobalPermissionManager().addPermission(globalPermissionType, zephyrTesterGroup.getName());
		}
	}
}
