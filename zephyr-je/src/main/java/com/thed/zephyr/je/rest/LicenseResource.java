package com.thed.zephyr.je.rest;


import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.web.util.OutlookDateManager;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.util.ZFJLicenseDetails;

@Api(value = "License Resource API(s)", description = "Following section describes the rest resources (API's) pertaining to Zephyr for JIRA LicenseResource")
@Path("license")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class LicenseResource {

	private final ZephyrLicenseManager zLicenseManager;
	private final OutlookDateManager outlookDateManager;
	private final DateTimeFormatterFactory dateTimeFormatterFactory;
	
	public LicenseResource(JiraAuthenticationContext authContext,
							ZephyrLicenseManager zLicenseManager,
							OutlookDateManager outlookDateManager,
							DateTimeFormatterFactory dateTimeFormatterFactory){
		this.zLicenseManager = zLicenseManager;
		this.outlookDateManager = outlookDateManager;
		this.dateTimeFormatterFactory = dateTimeFormatterFactory;
	}
	@ApiOperation(value = "Get License Status Information", notes = "Get License Status Inforation")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"expDateFormatted\":\"13/Apr/16 10:00 AM\",\"isEval\":\"true\",\"customerId\":\"Evaluation license\",\"licenseInformation\":\"Zephyr for JIRA - Test Management: Evaluation<br/><small>Support available until <strong>(13/Apr/16 10:00 AM)<\\/strong>. <\\/small>\",\"SEN\":\"SEN-L7506125\",\"version\":\"3.2.0\",\"expDate\":1460556000000}")})
	@GET
	public Response getLicenseStatus(){
		Map<String, Object> messageMap = new HashMap<String, Object>();
		PluginLicense zephyrMarketplaceLicense = zLicenseManager.getZephyrMarketplaceLicense();
		if(zephyrMarketplaceLicense == null){
			ZephyrLicense zLicense = zLicenseManager.getLicense();
			ZFJLicenseDetails zLicenseDetails = zLicenseManager.getLicenseDetails(zLicense);
			String licenseDescription = zLicenseDetails.getDescription();
			String licenseExpiryStatusMessage = zLicenseDetails.getLicenseExpiryStatusMessage(ComponentAccessor.getJiraAuthenticationContext().getI18nHelper(), outlookDateManager.getOutlookDate(new Locale("en")));
	
			String customerId = zLicense.getOrganisationId();
			
			messageMap.put("licenseInformation", licenseDescription + licenseExpiryStatusMessage);
			messageMap.put("customerId", customerId);
            messageMap.put("SEN", zLicense.getSupportEntitlementNumber());
			messageMap.put("isEval", String.valueOf(zLicense.isEvaluation()));
			messageMap.put("expDate", zLicense.getExpiryDate().getTime());
			DateTime expDateTime = new DateTime(zLicense.isEvaluation() ? zLicense.getExpiryDate() : zLicense.getMaintenanceExpiryDate());
			messageMap.put("expDateFormatted", dateTimeFormatterFactory.formatter().forLoggedInUser().format(expDateTime.toDate()));
		}
		else{
			
			String customerId = zephyrMarketplaceLicense.getOrganization().getName();
			String licenseDescription = zephyrMarketplaceLicense.getDescription();
			String expDateString = dateTimeFormatterFactory.formatter().forLoggedInUser().format(zephyrMarketplaceLicense.getMaintenanceExpiryDate().get().toDate());
			String licenseExpiryStatusMessage = "<br/><small>"+ ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.support.available.until", "(" +expDateString+ ")") +"</small>";
			messageMap.put("licenseInformation", licenseDescription + licenseExpiryStatusMessage);
			messageMap.put("customerId", customerId);
            messageMap.put("SEN", zephyrMarketplaceLicense.getSupportEntitlementNumber().get());
			messageMap.put("isEval", String.valueOf(zephyrMarketplaceLicense.isEvaluation()));
			messageMap.put("expDate", zephyrMarketplaceLicense.getMaintenanceExpiryDate().get().getMillis());
			messageMap.put("expDateFormatted", expDateString);
		}
        messageMap.put("version", ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_PRODUCT_VERSION));
		return Response.ok(messageMap).build();
	}
}