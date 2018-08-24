package com.thed.zephyr.je.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.delegate.IssueImporterResourceDelegate;
import com.thed.zephyr.je.rest.exception.RESTException;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

@Api(value = "TestCase File Import Resource API(s)", description = "Following section describes the rest resources pertaining to IssueImporterResouece")
@Path("importer")
@AnonymousAllowed
@ResourceFilters(ZFJApiFilter.class)
public class IssueImporterResource {
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private final IssueImporterResourceDelegate issueImportResourceDelegate;
	private final JiraAuthenticationContext authContext;

	protected final Logger log = Logger.getLogger(IssueImporterResource.class);
	public IssueImporterResource(JiraAuthenticationContext authContext, IssueImporterResourceDelegate issueImportResourceDelegate) {
		super();
		this.authContext = authContext;
		this.issueImportResourceDelegate = issueImportResourceDelegate;
	}

	@ApiOperation(value = "Import test cases into Jira from Excel File/s for a project", notes = "Import project test cases into Jira ")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/issueImport")
	@POST
	public Response importFiles(@Context HttpServletRequest request) {
		Response authRes = handleAuthRes();
		if (authRes != null) return authRes;
		return issueImportResourceDelegate.importFiles(request);
	}

	@ApiOperation(value = "Import test cases into Jira from Excel File/s for a project", notes = "Import project test cases into Jira ")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("/fieldMapping")
	@POST
	public Response extractIssueMapping(@Context HttpServletRequest request) {
		Response authRes = handleAuthRes();
		if (authRes != null) return authRes;

		Map<String, String> mapping = null;
		try {
			mapping = issueImportResourceDelegate.extractIssueMapping(request);
		} catch (Exception e) {
			return JiraUtil.buildErrorResponse(Response.Status.BAD_REQUEST, null, e.getMessage(), null);
		}
		return Response.status(Status.OK).entity(mapping).build();
	}


	@ApiOperation(value = "Retrieves all issues types by project id", notes = "Retrieves all issue types")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/issueTypes/{projectKey}")
	@GET
	public Response getIssueTypes(@PathParam("projectKey") String projectKey) {
		Response authRes = handleAuthRes();
		if (authRes != null) return authRes;

		List<String> issueTypesList = null;
		if(StringUtils.isBlank(projectKey)) {
			log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.importer.validationError.project")));
			return Response.status(Status.BAD_REQUEST).entity(authContext.getI18nHelper().getText("zephyr.importer.validationError.project")).cacheControl(ZephyrCacheControl.never()).build();
		}
		Project project = ComponentAccessor.getProjectManager().getProjectObjByKey(projectKey);
		if(project != null) {
			Collection<IssueType> issueTypes = project.getIssueTypes();
			issueTypesList = new ArrayList<String>();
			for(IssueType issueType:issueTypes) {
				issueTypesList.add(issueType.getName());
			}
		}


		return Response.status(Status.OK).entity(issueTypesList).build();
	}
	
	@ApiOperation(value = "Retrieves all issues types by project id", notes = "Retrieves all issue types")
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/issueFields")
	@GET
	public Response getIssueFields(@QueryParam("projectKey") String projectKey, @QueryParam("issueKey") String issueKey) {
		Response authRes = handleAuthRes();
		if (authRes != null) return authRes;

		List<String> issueTypesList = null;
		if(StringUtils.isBlank(projectKey)) {
			log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.importer.validationError.project")));
			return Response.status(Status.BAD_REQUEST).entity(authContext.getI18nHelper().getText("zephyr.importer.validationError.project")).cacheControl(ZephyrCacheControl.never()).build();
		}
		if(StringUtils.isBlank(issueKey)) {
			log.error(String.format(ERROR_LOG_MESSAGE,Status.BAD_REQUEST.getStatusCode(),Status.BAD_REQUEST,authContext.getI18nHelper().getText("zephyr.importer.validationError.issue")));
			return Response.status(Status.BAD_REQUEST).entity(authContext.getI18nHelper().getText("zephyr.importer.validationError.issue")).cacheControl(ZephyrCacheControl.never()).build();
		}
		Project project = ComponentAccessor.getProjectManager().getProjectObjByKey(projectKey);
		
		if(project != null) {
			Collection<IssueType> issueTypes = project.getIssueTypes();
			issueTypesList = new ArrayList<String>();
			for(IssueType issueType:issueTypes) {
				
				if(issueType.getName().equalsIgnoreCase(issueKey)) {
					/*ComponentAccessor.getFieldConfigSchemeManager().get
					ComponentAccessor.getFieldManager().getVisibleFieldLayouts(arg0)
					ComponentAccessor.getFieldLayoutManager().getFieldLayout(project.getId(), issueType.getId()).
					ComponentAccessor.getFieldConfigSchemeManager().getConfigSchemesForField(ComponentAccessor.getFieldManager().getIssueTypeField())
					ComponentAccessor.getFieldManager().getIssueTypeField().getIssueConstants().
					ComponentAccessor.getFieldManager().getAvailableNavigableFields(authContext.getLoggedInUser())
					ComponentAccessor.getFieldConfigSchemeManager().getInvalidFieldConfigSchemesForIssueTypeRemoval(issueType);*/
					MutableIssue issue = ComponentAccessor.getComponentOfType(IssueFactory.class).getIssue();
					issue.setProjectId(project.getId());
					issue.setIssueTypeId(issueType.getId());
				   
				    IssueCreationHelperBean issueCreationHelperBean = ComponentAccessor.getComponentOfType(IssueCreationHelperBean.class);
				    List<OrderableField> availableFieldsList = issueCreationHelperBean.getFieldsForCreate(authContext.getLoggedInUser(), issue);
				    if(availableFieldsList != null && availableFieldsList.size() > 0) {
				    	FieldLayout layout = ComponentAccessor.getFieldLayoutManager().getFieldLayout(issue);
				    	//List<CustomField> coList = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issue).get(0).getCustomFieldType().get
				    	
				    	for(OrderableField field : availableFieldsList) {
				    		FieldLayoutItem item =layout.getFieldLayoutItem(field.getId());
				    	}
				    }
				    	
				   /* ComponentAccessor.getFieldConfigSchemeManager().
					Issue issue = ComponentAccessor.getIssueManager().getIssueByCurrentKey(issueKey);
					ComponentAccessor.getFieldConfigSchemeManager().getInvalidFieldConfigSchemesForIssueTypeRemoval(issueType)
					ComponentAccessor.getFieldConfigSchemeManager().getFieldConfigScheme(issueType)
					issueType.get
					Set<SearchableField> fieldsSet = ComponentAccessor.getFieldManager().getAllSearchableFields();*/
					break;
				}
			}
		}
		return Response.status(Status.OK).entity("").build();
	}


	/**
	 * Handle auth res
	 * @return
	 */
	private Response handleAuthRes() {
		final ApplicationUser user = authContext.getLoggedInUser();
		//Anonymous User pass thru
		boolean isPermissionEnabled = JiraUtil.getPermissionSchemeFlag();
		JSONObject jsonObject = new JSONObject();
		try {
			if (user == null && !JiraUtil.hasAnonymousPermission(user) && !isPermissionEnabled) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				log.error(String.format(ERROR_LOG_MESSAGE, Status.UNAUTHORIZED.getStatusCode(), Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			log.error("Error while authorizing.", e);
			return Response.status(Status.BAD_REQUEST).build();
		}
		return null;
	}
}
