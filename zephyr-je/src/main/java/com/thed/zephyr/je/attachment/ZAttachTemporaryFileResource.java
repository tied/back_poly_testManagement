package com.thed.zephyr.je.attachment;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.security.xsrf.XsrfInvocationChecker;
import com.atlassian.jira.security.xsrf.XsrfTokenGenerator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.action.AttachFileAction;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.permissions.model.PermissionType;
import com.thed.zephyr.je.permissions.service.ZephyrPermissionManager;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ZephyrCacheControl;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

@Path ("attachTemporaryFile")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class ZAttachTemporaryFileResource
{
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    protected final Logger log = Logger.getLogger(ZAttachTemporaryFileResource.class);

    private final JiraAuthenticationContext authContext;
    private final ZWebAttachmentManager zWebAttachmentManager;
    private final ScheduleManager scheduleManager;
    private final TeststepManager teststepManager;
    private final ProjectService projectService;
    private final XsrfInvocationChecker xsrfChecker;
    private final XsrfTokenGenerator xsrfGenerator;
    private final ZAttachmentHelper zAttachmentHelper;
    private final ZephyrPermissionManager zephyrPermissionManager;

    public ZAttachTemporaryFileResource(JiraAuthenticationContext authContext,
            ZWebAttachmentManager zWebAttachmentManager, ScheduleManager scheduleManager, ProjectService projectService,
            XsrfInvocationChecker xsrfChecker, XsrfTokenGenerator xsrfGenerator,ZAttachmentHelper zAttachmentHelper,TeststepManager teststepManager,
            ZephyrPermissionManager zephyrPermissionManager)
    {
        this.authContext = authContext;
        this.zWebAttachmentManager = zWebAttachmentManager;
        this.scheduleManager = scheduleManager;
        this.projectService = projectService;
        this.xsrfChecker = xsrfChecker;
        this.xsrfGenerator = xsrfGenerator;
        this.zAttachmentHelper = zAttachmentHelper;
        this.teststepManager=teststepManager;
        this.zephyrPermissionManager=zephyrPermissionManager;
    }

    @POST
    @Consumes (MediaType.WILDCARD)
    public Response addTemporaryAttachment(@QueryParam ("filename") String filename,
            @QueryParam ("projectId") Long projectId, @QueryParam ("entityId") Long entityId,@QueryParam ("entityType") String entityType,
            @QueryParam("size") Long size, @Context HttpServletRequest request)
    {

        ZAttachmentHelper.ValidationResult validationResult = zAttachmentHelper.validate(request, filename, size);

        if (!validationResult.isValid())
        {
            if (validationResult.getErrorType() == ZAttachmentHelper.ValidationError.FILENAME_BLANK)
            {
                return Response.status(Response.Status.BAD_REQUEST).cacheControl(ZephyrCacheControl.never()).build();
            }

            if (validationResult.getErrorType() == ZAttachmentHelper.ValidationError.XSRF_TOKEN_INVALID)
            {
                return createTokenError(xsrfGenerator.generateToken(request));
            }
            else if (validationResult.getErrorType() == ZAttachmentHelper.ValidationError.ATTACHMENT_IO_SIZE)
            {
                String message = authContext.getI18nHelper().getText("attachfile.error.io.size", filename);
                return createError(Response.Status.BAD_REQUEST, message);
            }
            else if (validationResult.getErrorType() == ZAttachmentHelper.ValidationError.ATTACHMENT_IO_UNKNOWN)
            {
                String message = authContext.getI18nHelper().getText("attachfile.error.io.error", filename, validationResult.getErrorMessage());
                return createError(Response.Status.INTERNAL_SERVER_ERROR, message);
            }
            else if (validationResult.getErrorType() == ZAttachmentHelper.ValidationError.ATTACHMENT_TO_LARGE)
            {
                return createError(Response.Status.BAD_REQUEST, validationResult.getErrorMessage());
            }
        }

        if (entityId == null && projectId == null)
        {
            return Response.status(Response.Status.BAD_REQUEST).cacheControl(ZephyrCacheControl.never()).build();
        }
        final ApplicationUser user = authContext.getLoggedInUser();
        Project project = getProject(user, projectId);

        boolean hasZephyrPermission =  verifyBulkEditPermissions(project.getId(),authContext.getLoggedInUser());
        if(!hasZephyrPermission) {
        	return getPermissionDeniedErrorResponse();
        }
        
        InputStream inputStream = validationResult.getInputStream();

        try
        {
            final TemporaryAttachment attach = zWebAttachmentManager.createTemporaryAttachment(validationResult.getInputStream(), filename,
                    validationResult.getContentType(), validationResult.getSize(), entityId,entityType, project);
            return Response.status(Response.Status.CREATED)
                    .entity(new GoodResult(attach.getId(), filename)).cacheControl(ZephyrCacheControl.never()).build();
        }
        catch (ZAttachmentException e)
        {
            return createError(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
        finally
        {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private Project getProject(ApplicationUser user, Long id)
    {
        ProjectService.GetProjectResult projectResult = projectService.getProjectById(user, id);
        if (projectResult.isValid())
        {
            return projectResult.getProject();
        }
        else
        {
            return throwFourOhFour(projectResult.getErrorCollection());
        }
    }

    private static Response createError(Response.Status status, com.atlassian.jira.util.ErrorCollection collection)
    {
        String message = getFirstElement(collection.getErrorMessages());
        if (message == null)
        {
            message = getFirstElement(collection.getErrors().values());
        }
        return createError(status, message);
    }

    private static Response createError(Response.Status status, String message)
    {
        return Response.status(status).cacheControl(ZephyrCacheControl.never()).entity(new BadResult(message)).build();
    }

    private Response createTokenError(String newToken)
    {
        String message = authContext.getI18nHelper().getText("attachfile.xsrf.try.again");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .cacheControl(ZephyrCacheControl.never()).entity(new BadResult(message, newToken)).build();
    }

    private <T> T throwFourOhFour(com.atlassian.jira.util.ErrorCollection errorCollection)
    {
        throw new WebApplicationException(createError(Response.Status.NOT_FOUND, errorCollection));
    }

    private static <T> T getFirstElement(Collection<? extends T> values)
    {
        if (!values.isEmpty())
        {
            return values.iterator().next();
        }
        else
        {
            return null;
        }
    }

    @XmlRootElement
    public static class GoodResult
    {
        @XmlElement
        private String name;

        @XmlElement
        private String id;

        @SuppressWarnings ( { "unused" })
        private GoodResult() {}

        GoodResult(long id, String name)
        {
            this.id = String.valueOf(id);
            this.name = name;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            GoodResult that = (GoodResult) o;

            if (id != null ? !id.equals(that.id) : that.id != null) { return false; }
            if (name != null ? !name.equals(that.name) : that.name != null) { return false; }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
    }

    @XmlRootElement
    public static class BadResult
    {
        @XmlElement
        private String errorMessage;

        @XmlElement
        private String token;

        @SuppressWarnings ( { "unused" })
        private BadResult() {}

        BadResult(String msg)
        {
            this(msg, null);
        }

        BadResult(String msg, String token)
        {
            this.errorMessage = msg;
            this.token = token;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            BadResult badResult = (BadResult) o;

            if (errorMessage != null ? !errorMessage.equals(badResult.errorMessage) : badResult.errorMessage != null)
            { return false; }
            if (token != null ? !token.equals(badResult.token) : badResult.token != null) { return false; }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = errorMessage != null ? errorMessage.hashCode() : 0;
            result = 31 * result + (token != null ? token.hashCode() : 0);
            return result;
        }
    }
    
    
    /**
     * Below Section is for adding another EntityType needed for Attachment.
     */
    private Schedule getSchedule(User user, Long id)
    {
        Schedule result = scheduleManager.getSchedule(id.intValue());
        if (result != null)
        {
            return result;
        }
        else
        {
        	//ErrorCollection collection = new ErrorCollection();
        	//collection.addErrorMessage("Bad Request");
            throw new WebApplicationException(createError(Response.Status.NOT_FOUND, "Bad Request"));
        }
    }

    
    private Teststep getTeststep(User user, Long id)
    {
        Teststep result = teststepManager.getTeststep(id.intValue());
        if (result != null)
        {
            return result;
        }
        else
        {
        	//ErrorCollection collection = new ErrorCollection();
        	//collection.addErrorMessage("Bad Request");
            throw new WebApplicationException(createError(Response.Status.NOT_FOUND, "Bad Request"));
        }
    }
    
	/**
	 * @return 
	 */
	private Response getPermissionDeniedErrorResponse() {
        String errorMessage = authContext.getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
        return Response.status(Response.Status.FORBIDDEN)
                .cacheControl(ZephyrCacheControl.never()).entity(new BadResult(errorMessage)).build();
	}
	
	/**
	 * Verifies Bulk Permission
	 * @param projectId
	 * @param user
	 * @return
	 */
	private boolean verifyBulkEditPermissions(Long projectId,ApplicationUser user) {
		//Check ZephyrPermission and update response to include execution per project permissions
		ProjectPermissionKey cyclePermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_BROWSE_CYCLE.toString());
		ProjectPermissionKey executionPermissionKey = new ProjectPermissionKey(PermissionType.ZEPHYR_EDIT_EXECUTION.toString());
		Collection<ProjectPermissionKey> projectPermissionKeys = new ArrayList<ProjectPermissionKey>();
		projectPermissionKeys.add(executionPermissionKey);
		projectPermissionKeys.add(cyclePermissionKey);
		boolean loggedInUserHasZephyrPermission = zephyrPermissionManager.validateUserPermissions(projectPermissionKeys, null, user ,projectId);
		return loggedInUserHasZephyrPermission;
	}
}
