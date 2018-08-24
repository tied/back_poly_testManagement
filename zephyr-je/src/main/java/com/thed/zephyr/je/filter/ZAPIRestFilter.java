package com.thed.zephyr.je.filter;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.Plugin;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.thed.zephyr.je.config.license.PluginUtils;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.je.service.ZAPIModuleService;
import com.thed.zephyr.util.*;
import com.thed.zephyr.util.optionalservice.ServiceAccessor;
import com.thed.zephyr.util.optionalservice.Zapi;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class ZAPIRestFilter implements Filter{
    private static final Logger log = Logger.getLogger(ZAPIRestFilter.class);

    private FilterConfig config;
	private final ZephyrLicenseManager zLicenseManager;
    private final JiraAuthenticationContext authContext;
    private final ServiceAccessor optionalServiceAccessor;

	private final I18nHelper i18nHelper;
	private LoadingCache<String, Optional<ZAPILicense>> zapiStatusCache;

    public ZAPIRestFilter(ZephyrLicenseManager zLicenseManager, JiraAuthenticationContext authContext, ServiceAccessor optionalServiceAccessor){
    	this.authContext=authContext;
		this.zLicenseManager = zLicenseManager;
    	this.i18nHelper = authContext.getI18nHelper();
        this.optionalServiceAccessor = optionalServiceAccessor;
    }
    
	@Override
	public void init(FilterConfig config) throws ServletException {
		this.config = config;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		log.debug("Inside ZAPI REST Filter...");
		
		if(zapiStatusCache == null){
			zapiStatusCache = CacheBuilder.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build(new CacheLoader<String, Optional<ZAPILicense>>() {
				public Optional<ZAPILicense> load(String key) {
					return fetchZAPILicenseStatus();
				}
			});
		}
		
		if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))) {
		   log.info("Ignoring non-HTTP requests. Sorry dont know what to do with such requests.");
	       chain.doFilter(request, response);
	       return;
	    }
		
	    HttpServletRequest req = (HttpServletRequest)request;
	    HttpServletResponse res = (HttpServletResponse)response;
	    String url = req.getRequestURI();
	    log.debug("REST API URI - " +  url);
	    
	    //Check for Admin API 
	    if(StringUtils.contains(url, "/license") || StringUtils.contains(url, "/audit") ) {
	    	boolean isJiraAdmin = ComponentAccessor.getGlobalPermissionManager().hasPermission(GlobalPermissionKey.ADMINISTER,authContext.getLoggedInUser());
	    	if(!isJiraAdmin) {
				JSONObject jsonResponse = new JSONObject();
				try {
		    		String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("zephyr.plugin.permissions.invalid.error");
		    		jsonResponse.put("PERM_DENIED", errorMessage);
				    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
		    		res.setContentType("application/json");
				    res.getWriter().print(jsonResponse.toString());
					res.getWriter().flush();
				} catch(JSONException e) {
					log.error("Error creating JSON Response",e);
				}
	    		return;
	    	}
	    }
	    
	    if(authContext.getLoggedInUser() == null) {
		    @SuppressWarnings("rawtypes")
			Map map = new HashMap();
		    map.put(ApplicationConstants.ERROR_ID, "ERROR");
		    map.put(ApplicationConstants.ERROR_DESC, authContext.getI18nHelper().getText("zapi.login.error"));
		    JSONObject jsonResponse = new JSONObject(map);
		    res.setContentType("application/json");
		    res.getWriter().print(jsonResponse.toString());
			res.getWriter().flush();
    		return;
	    }
	    //Do License Validation.
		ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);

	    //license is invalid , do not process request. Instead display the License Error Message.
	    if( !licVerificationResult.isValid()){
		    res.setStatus(HttpURLConnection.HTTP_BAD_METHOD);
		    @SuppressWarnings("rawtypes")
			Map map = new HashMap();
		    map.put(ApplicationConstants.ERROR_ID, licVerificationResult.getException().getExceptionId());
		    map.put(ApplicationConstants.ERROR_DESC, licVerificationResult.getErrorMessage());
		    map.put(ApplicationConstants.ERROR_DESC_HTML, licVerificationResult.getGeneralMessage());
		    JSONObject jsonResponse = new JSONObject(map);
		    res.setContentType(MediaType.APPLICATION_JSON);
		    res.getWriter().print(jsonResponse.toString());
			res.getWriter().flush();
		    return;
	    }

	  //ZAPI Plugin Module Check
		Boolean zapiPluginValid = false;
		Optional<ZAPILicense> zapiLic = null;
		try {
            // checking ZAPI license via REST call
            zapiLic = zapiStatusCache.get("result");
            zapiPluginValid = isZapiValid(zapiLic);
            // checking ZAPI license using optional import component
            if(!zapiPluginValid) {
                Zapi zapi = optionalServiceAccessor.getZapi();
                zapiPluginValid = isZapiValid(zapi != null ? Optional.of(ZAPILicense.fromJson(zapi.getZapiConfig())) : zapiLic);
            }
		} catch (Exception e) {
			log.fatal("Unable to determine ZAPI status", e);
		} 
		ZAPIModuleService zapiModuleService = (ZAPIModuleService)ZephyrComponentAccessor.getInstance().getComponent("zapiModuleService");

		if(!zapiPluginValid) {
			zapiModuleService.disableZAPIModule(ConfigurationConstants.ZAPI_PLUGIN_KEY);
			zapiStatusCache.invalidateAll();	//If ZAPI status cant be determined, we will remove the cached value, so that it can be refetched on next request. 
		} else {
            boolean zapiModuleEnabled = ComponentAccessor.getPluginAccessor().isPluginModuleEnabled(ConfigurationConstants.ZAPI_PLUGIN_KEY);
            if(!zapiModuleEnabled)
			    zapiModuleService.enableZAPIModule(ConfigurationConstants.ZAPI_PLUGIN_KEY);
		}

		//boolean zapiModuleEnabled = ComponentAccessor.getPluginAccessor().isPluginModuleEnabled(ConfigurationConstants.ZAPI_PLUGIN_KEY);
	    
	    //If Module is disabled, do not process request. Instead display the License Error Message.
        if( !zapiPluginValid){
            Integer httpStatus = HttpURLConnection.HTTP_NOT_FOUND;

            String msg = i18nHelper.getText("zapi.plugin.unavailable");
            if (zapiLic.isPresent() && !zapiLic.get().licValid) {
                msg = zapiLic.get().errorMsg;
                httpStatus = HttpURLConnection.HTTP_BAD_METHOD;
            }
            Map<String, Object> map = new HashMap();
            map.put(ApplicationConstants.ERROR_ID, httpStatus);
            map.put(ApplicationConstants.ERROR_DESC, msg);
            map.put(ApplicationConstants.ERROR_DESC_HTML, msg);

            res.setStatus(httpStatus);
            JSONObject jsonResponse = new JSONObject(map);
            res.setContentType(MediaType.APPLICATION_JSON);
            res.getWriter().print(jsonResponse.toString());
            res.getWriter().flush();
            return;
        }
	    
	   //Let container continue to do the next intended action.
	   chain.doFilter(request, response);
	   log.debug("Exit ZAPI REST Filter...");
	}
	
	private Boolean isZapiValid(Optional<ZAPILicense> zapiLic) throws ZephyrLicenseException {
		if(!zapiLic.isPresent()){
			log.warn("Unable to determine ZAPI status");
			return false;
		}
		if(!zapiLic.get().licValid){
			log.warn("ZAPI license is invalid");
			zapiLic.get().errorMsg = i18nHelper.getText("zapi.plugin.lic.invalid");
			return false;
		}
		if(zapiLic.get().licMaintenanceExpired){
			Plugin plugin = ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY);
			DateTime zfjreleaseDate = PluginUtils.getPluginBuildDate(plugin);
			if(zfjreleaseDate.isAfter(zapiLic.get().licMaintenanceExpirationDate)){
				log.warn("Current ZFJ version is after ZAPI maintenance expiration date, ZAPI usage is not allowed and ZAPI license need to be renewed");
				zapiLic.get().errorMsg = i18nHelper.getText("zapi.plugin.maintenance.expired");
				return false;
			}else{
				return true;
			}
		}
		return true;
	}

	/**
	 * Checks if ZAPI License is valid
	 * @return
	 */
	private Optional<ZAPILicense>   fetchZAPILicenseStatus() {
		try {
			ApplicationProperties applicationProperties = (ApplicationProperties)ZephyrComponentAccessor.getInstance().getComponent("applicationProperties");
	        String restUrl = applicationProperties.getBaseUrl() + "/rest/zfjapi/latest/zapi";
			Client client = Client.create();
	        if(StringUtils.startsWithIgnoreCase(restUrl, "https")){
	        	initSSL();
	        }
	        WebResource webResource = client.resource(restUrl);
			ClientResponse response = webResource.accept("application/json").get(ClientResponse.class);
	        return decodeLicenseStatusFromResponse(response.getEntity(String.class));
		} catch(Exception e) {
			log.error("Error retrieving license information. Disable module", e);
		}
		return Optional.absent();
	}

	 
	 private void initSSL() {
		// Create a trust manager that does not validate certificate chains
		 TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
			    public X509Certificate[] getAcceptedIssuers(){return null;}
			    public void checkClientTrusted(X509Certificate[] certs, String authType){}
			    public void checkServerTrusted(X509Certificate[] certs, String authType){}
			}};

		 // Install the all-trusting trust manager
		 try {
		     SSLContext sc = SSLContext.getInstance("TLS");
		     sc.init(null, trustAllCerts, new SecureRandom());
		     HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		 } catch (Exception e) {}
		
	}

	/**
	  * gets the JSON response from the response String
	  * @param response
	  * @return
	  * @throws JSONException
	  */
	 private Optional<ZAPILicense> decodeLicenseStatusFromResponse(@Nullable final String response) throws JSONException {
		 if (response != null){
			 final JSONObject json = new JSONObject(response);
			 return Optional.of(ZAPILicense.fromJson(json));
		 }
		 return null;
	 }
	 
	 private static class ZAPILicense{
		 public Boolean licValid;
		 public Boolean licMaintenanceExpired;
		 public DateTime licMaintenanceExpirationDate;
		 public String errorMsg;
		 
		 public ZAPILicense(Boolean licValid, Boolean licMaintenanceExpired, DateTime licMaintenanceExpirationDate) {
			super();
			this.licValid = licValid;
			this.licMaintenanceExpired = licMaintenanceExpired;
			this.licMaintenanceExpirationDate = licMaintenanceExpirationDate;
		}

         public static ZAPILicense fromJson(JSONObject json) throws JSONException{
             return new ZAPILicense(json.getBoolean("licValid"), json.getBoolean("licMaintenanceExpired"),
                     new DateTime(json.has("licMaintenanceExpirationDate") ? json.getString("licMaintenanceExpirationDate") : null));
         }
	 }
	
	@Override
	public void destroy() {
	       this.config = null;
	}
}
