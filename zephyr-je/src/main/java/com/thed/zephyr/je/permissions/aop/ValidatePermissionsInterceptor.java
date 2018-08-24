package com.thed.zephyr.je.permissions.aop;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.springframework.util.StopWatch;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;

/**
 * ValidatePermissionsInterceptor to validate Zephyr custom permissions on rest resources
 */

public class ValidatePermissionsInterceptor implements MethodInterceptor {

    protected final Logger log = Logger.getLogger(ValidatePermissionsInterceptor.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final StopWatch stopWatch = new StopWatch(invocation.getMethod().getName());
        stopWatch.start("Zephyr Custom Permissions validation");

        try {
            if (invocation.getMethod().isAnnotationPresent(ValidatePermissions.class)) {
                log.debug("##### START METHOD {} : " + invocation.getMethod().getName());
                PermissionType[] permissionTypes = invocation.getMethod().getAnnotation(ValidatePermissions.class).permissionType();
                return invokeAfterValidatePermissions(invocation, permissionTypes);
            } else
                return invocation.proceed();
        } catch (Throwable t) {
            log.error("##### Error validating zephyr custom permissions for : " + invocation.getMethod().getName() + t.getMessage());
            String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permission.validation.error", t.getMessage());
            return Response.serverError().entity(errorMessage).build();
        } finally {
            stopWatch.stop();
            log.debug("##### END METHOD {} : " + invocation.getMethod().getName());
        }
    }


    private Object invokeAfterValidatePermissions(MethodInvocation invocation, PermissionType[] permissionTypes) throws Throwable {

        // if no PermissionType mentioned in Annotation, let go validation
        if (null == permissionTypes || permissionTypes.length == 0)
            return invocation.proceed();

        // initialize areValidPermissions default to true
        Boolean areValidPermissions = true;

        ApplicationUser loggedInUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        log.debug("Logged In User:"+ loggedInUser != null ? loggedInUser.getUsername() : "Logged In User returned null");
        Project project = JiraUtil.getProjectThreadLocal();
        if(null != invocation && null != invocation.getMethod()) {
            log.debug("Method Invoked :" + invocation.getMethod().getName() + " ==> Project :" + project);
        }

        // for each permission type mentioned in Annotation, check for validity
        for (PermissionType permissionType : permissionTypes) {
            areValidPermissions &= hasAnonymousPermission(loggedInUser,project) && verifyPermissions(permissionType, loggedInUser,project);

            // if 'areValidPermissions == false', return a permission validation error message
            if (!areValidPermissions) {
                String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
                JSONObject errorJSON = new JSONObject();
                // build error map
                errorJSON.put("PERM_DENIED", errorMessage);
                return buildResponseErrorResponse(errorJSON);
            }
        }

        //else proceed with default method invocation.
        return invocation.proceed();
    }

    /**
     * Checks to see if Anonymous Permission exists ( JIRA 6.4)
     *
     * @param user
     * @param project
     * @return boolean
     */
    private boolean hasAnonymousPermission(ApplicationUser user, Project project) {
        boolean hasAdministerRights = ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.ADMINISTER_PROJECTS, project, user);
        boolean hasBrowseRights = ComponentAccessor.getPermissionManager().hasPermission(ProjectPermissions.BROWSE_PROJECTS, project, user);

        if (!hasAdministerRights && !hasBrowseRights) {
            return false;
        }
        return hasAdministerRights || hasBrowseRights;
    }
    
    
	public Boolean verifyPermissions(PermissionType permissionType,ApplicationUser loggedInUser, Project project) {
		ZephyrPermissionManager zephyrPermissionManager = (ZephyrPermissionManager)ZephyrComponentAccessor.getInstance().getComponent("zephyrPermissionManager");
        ProjectPermissionKey projectPermissionKey = new ProjectPermissionKey(permissionType.toString());
		return zephyrPermissionManager.validateUserPermission(projectPermissionKey, project, loggedInUser,project.getId());
	}

    /**
     * Build Error Map
     *
     * @param errorMap
     * @return
     */
    private Response buildResponseErrorResponse(JSONObject errorJsonObject) {
        Response.ResponseBuilder builder = Response.status(Response.Status.FORBIDDEN);
        builder.type(MediaType.APPLICATION_JSON);
        builder.entity(errorJsonObject.toString());
        return builder.build();
    }
}
