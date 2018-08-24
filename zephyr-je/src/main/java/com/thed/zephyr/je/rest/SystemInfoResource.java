package com.thed.zephyr.je.rest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.BuildUtilsInfoImpl;
import com.atlassian.jira.util.JiraUtilsBean;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.system.SystemInfoUtils;
import com.atlassian.jira.util.system.SystemInfoUtilsImpl;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZFJLicenseDetails;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value = "SystemInfo Resource API(s)", description = "Following section describes the rest resources pertaining to SystemInfoResource")
@Path("systemInfo")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class SystemInfoResource {
	private final ZephyrLicenseManager zLicenseManager;
	private final JiraAuthenticationContext authContext;
	
	public SystemInfoResource(JiraAuthenticationContext authContext,ZephyrLicenseManager zLicenseManager) {
		this.zLicenseManager=zLicenseManager;
		this.authContext=authContext;
	}

	@ApiOperation(value = "Get System Information")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name="response", value = "{\"jira_db_build\":\"70111\",\"jira_app_server\":\"Apache Tomcat\",\"jira_db_type\":\"mysql\",\"licenseDescription\":\"Zephyr for JIRA - Test Management: Evaluation\",\"jira_version\":\"7.0.2\",\"customerId\":\"Evaluation license\",\"zfj_build\":\"32002584\",\"SEN\":\"SEN-L7506125\",\"zfj_version\":\"3.2.0\"}")})
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSystemInformation(){
		JSONObject jsonObject = new JSONObject();
		try {
			if (authContext.getLoggedInUser() == null) {
				jsonObject.put("error", authContext.getI18nHelper().getText("zephyr.common.logged.user.error"));
				return Response.status(Status.UNAUTHORIZED).entity(jsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
			}
		} catch (JSONException e) {
			return Response.status(Status.BAD_REQUEST).build();
		}
		
    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
    	if(!isJiraAdmin) {
			String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
			return JiraUtil.getPermissionDeniedErrorResponse(errorMessage);
    	}
    	
		BuildUtilsInfo buildUtils = new BuildUtilsInfoImpl();
		SystemInfoUtils utils = new SystemInfoUtilsImpl();

		Map<String,String> responseMap = new HashMap<String, String>();
		String customerId = "";
		String licenseDescription = "";
		String SEN = "";
		String installedZephyrJEInfo = JiraUtil.getPropertySet(ConfigurationConstants.ZEPHYR_ENTITY_NAME, ConfigurationConstants.ZEPHYR_ENTITY_ID)
		.getString(ConfigurationConstants.ZEPHYR_JE_CURRENT_VERSION);

		PluginLicense zephyrMarketplaceLicense = zLicenseManager.getZephyrMarketplaceLicense();
		if(zephyrMarketplaceLicense == null){
			ZephyrLicense zLicense = zLicenseManager.getLicense();
			ZFJLicenseDetails zLicenseDetails = zLicenseManager.getLicenseDetails(zLicense);
			licenseDescription = zLicenseDetails.getDescription();
			customerId = zLicense.getOrganisationId();
			SEN = zLicense.getSupportEntitlementNumber();
		}
		else{
			customerId = zephyrMarketplaceLicense.getOrganization().getName();
			licenseDescription = zephyrMarketplaceLicense.getDescription();
			SEN = zephyrMarketplaceLicense.getSupportEntitlementNumber().get();
		}
		
		responseMap.put("licenseDescription", licenseDescription);
		responseMap.put("customerId", customerId);
		responseMap.put("SEN", SEN);
		
		String installedZephyrJEBuild = null;
		String installedZephyrJEVersion = null;

		//For first time, we don't have version store. Hence assume it's 610 our base version which we released initially.
		if(installedZephyrJEInfo.equals("1.0.0")){
			installedZephyrJEBuild = "610";
			installedZephyrJEVersion = "1.0";
		}
		else{
			int indexOfVersion = installedZephyrJEInfo.lastIndexOf(".");
			installedZephyrJEBuild = installedZephyrJEInfo.substring(indexOfVersion + 1);
			installedZephyrJEVersion = installedZephyrJEInfo.substring(0,indexOfVersion);
		}
		

		responseMap.put("jira_version", buildUtils.getVersion());
		responseMap.put("jira_db_build", String.valueOf(buildUtils.getDatabaseBuildNumber()));
		responseMap.put("jira_db_type", utils.getDatabaseType());
		responseMap.put("jira_app_server", utils.getAppServer());
		responseMap.put("zfj_build", installedZephyrJEBuild);
		responseMap.put("zfj_version", installedZephyrJEVersion);
		return Response.ok(responseMap).build();
	}

}
