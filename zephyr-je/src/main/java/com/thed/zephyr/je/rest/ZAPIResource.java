package com.thed.zephyr.je.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.PluginAccessor;
import com.google.common.collect.ImmutableMap;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

@Api(value = "ZAPI Resource API(s)", description = "Following section describes the rest resources pertaining to ZAPIResource")
@Path ("moduleInfo")
@Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON})
@ResourceFilters(ZFJApiFilter.class)
public class ZAPIResource {
	protected final Logger log = Logger.getLogger(ZAPIResource.class);
	private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

	private final JiraAuthenticationContext authContext;
	private final PluginAccessor pluginAccessor;

	public ZAPIResource(final JiraAuthenticationContext authContext,
			final PluginAccessor pluginAccessor) {
		this.authContext=authContext;
		this.pluginAccessor=pluginAccessor;
	}

	@ApiOperation(value = "Get ZAPI Module Status")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name="response",value = "{\"status\": \"ENABLED\"}")})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response zapiModuleStatus() {
    	if(authContext.getLoggedInUser() == null) {
            return buildLoginErrorResponse();
    	}
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
            log.error(String.format(ERROR_LOG_MESSAGE,Status.FORBIDDEN.getStatusCode(), Status.FORBIDDEN,errorMessage));
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}

    	final I18nHelper i18nHelper = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
		try {
			return Response
					.status(Status.OK)
					.type(MediaType.APPLICATION_JSON)
					.entity(ImmutableMap
							.of("status",
									pluginAccessor
											.getPluginModule(
													ConfigurationConstants.ZAPI_PLUGIN_KEY)
											.getPlugin().getPluginState()
											.name())).build();
		} catch (Exception e) {
			return Response.status(Response.Status.NOT_ACCEPTABLE)
					.type(MediaType.APPLICATION_JSON)
					.entity(ImmutableMap.of("Error", i18nHelper.getText("zapi.plugin.unavailable"))).build();
		}
	}

    /**
     * Common logged in user error response.
     * @return
     */
    private Response buildLoginErrorResponse() {

        JSONObject jsonObject = new JSONObject();
        try {
			log.error(String.format(ERROR_LOG_MESSAGE,Status.UNAUTHORIZED.getStatusCode(), Response.Status.UNAUTHORIZED,authContext.getI18nHelper().getText("zephyr.common.logged.user.error")));
            jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
        } catch (JSONException e) {
            log.error("Error occurred during response object creation.",e);
            return Response.status(Status.BAD_REQUEST).build();
        }
        return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
    }
}
