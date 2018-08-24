package com.thed.zephyr.je.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.system.SystemInfoUtils;
import com.atlassian.jira.util.system.SystemInfoUtilsImpl;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.model.feedback.FeedbackRequest;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.ZephyrCacheControl;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Objects;

/**
 * Created by niravshah on 12/13/17.
 */

@Path("feedback")
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class FeedbackResource {
    private static final String FEEDBACK_ENTITY_TYPE = "FEEDBACK";
    private static final String ERROR_LOG_MESSAGE = "[Error] [Error code: %s %s Error Message : %s]";

    protected final Logger log = Logger.getLogger(FeedbackResource.class);
    private JiraAuthenticationContext authContext;
    private ZephyrLicenseManager zLicenseManager;

    public FeedbackResource(ZephyrLicenseManager zLicenseManager,
                            JiraAuthenticationContext authContext) {
        this.zLicenseManager = zLicenseManager;
        this.authContext = authContext;
    }


    @POST
    public Response sendFeedback(FeedbackRequest feedbackRequest) throws Exception {
        JSONObject pingParams = new JSONObject();

        BuildUtilsInfo buildUtilsInfo = ComponentAccessor.getComponentOfType(BuildUtilsInfo.class);
        pingParams.put("jiraVersion", buildUtilsInfo.getVersion() + "-" + buildUtilsInfo.getApplicationBuildNumber());
        SystemInfoUtils utils = new SystemInfoUtilsImpl();
        try {
            StringBuffer dbVersion = new StringBuffer(utils.getDatabaseType()).append("-").append(utils.getDatabaseMetaData().getDatabaseProductVersion());
            pingParams.put("dbVersion", dbVersion.toString());
        } catch (Exception ex) {
            log.error("Unable to determine DB Version " + ex.getMessage());
        }

        //JiraUtil.populateLicParams(pingParams);
        ZephyrLicenseManager licManager = (ZephyrLicenseManager) ZephyrComponentAccessor.getInstance().getComponent("zephyr-je-Licensemanager");
        PluginLicense pluginLic = licManager.getZephyrMarketplaceLicense();
        if(pluginLic != null){
            pingParams.put("custId", pluginLic.getOrganization().getName());
            pingParams.put("licenseId", pluginLic.getSupportEntitlementNumber().getOrElse(pluginLic.getServerId()));
            pingParams.put("licSrc", "ZFJ");
            pingParams.put("licType", pluginLic.getLicenseType().name());
        } else {
            ZephyrLicense lic = licManager.getLicense();
            if(lic != null){
                pingParams.put("custId", lic.getOrganisationId());
                pingParams.put("licenseId", lic.getLicenseId());
                pingParams.put("licSrc", "ZEP");
                pingParams.put("licType", lic.getLicenseType().name());
            }else{
                log.fatal(" ZEPHYR FOR JIRA License is not installed");
            }
        }

        pingParams.put("userName", feedbackRequest.getUserName());
        pingParams.put("summary",feedbackRequest.getSummary());
        pingParams.put("description",feedbackRequest.getDescription());
        pingParams.put("component",feedbackRequest.getComponent());
        if(!feedbackRequest.isSendAnonymous()) {
            pingParams.put("email", feedbackRequest.getEmail());
        }


        //ZFJ Build
        pingParams.put("buildNumber", ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getVersion());
        HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
        client.getParams().setBooleanParameter("http.protocol.allow-circular-redirects", true);
        client.getHttpConnectionManager().getParams().setSoTimeout(17000);
        client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
        String url = System.getProperty("ZFJ_FEEDBACK_URL", "https://version.yourzephyr.com/feedback.php");
        client.getHostConfiguration().setHost(url);

        PostMethod postMethod = new PostMethod(url);
        try {
            StringRequestEntity requestEntity = new StringRequestEntity(
                    pingParams.toString(),"application/json", "UTF-8");
            postMethod.setRequestEntity(requestEntity);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //postMethod.setRequestBody(pingParams.toArray(new NameValuePair[pingParams.size()]));
        log.debug("Ping String " + pingParams.toString());

        try {
            int code = client.executeMethod(postMethod);
            if (code == HttpURLConnection.HTTP_OK) {
                log.debug("version check completed " + postMethod.getResponseBodyAsString());
            } else if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                log.error("Unable to perform version check. Response from Server: \n " + postMethod.getResponseBodyAsString());
            } else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
                log.error("Unable to perform feedback post, Proxy authentication required. Response from Server: \n  " + postMethod.getResponseBodyAsString());
            } else {
                log.error("Unable to perform feedback post.  Response from Server: \n  " + postMethod.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.fatal("Unable to perform version check ", e);
        } finally{
            if(postMethod != null){
                postMethod.releaseConnection();
            }
        }
        return Response.ok().cacheControl(ZephyrCacheControl.never()).build();
    }

}
