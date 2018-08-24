package com.thed.zephyr.je.filter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.config.license.ZephyrLicenseVerificationResult;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.CodecUtils;
import com.thed.zephyr.util.JiraUtil;

public class ZephyrRestFilter implements Filter{
    private static final Logger log = Logger.getLogger(ZephyrRestFilter.class);

    private FilterConfig config;
    private final ZephyrLicenseManager zLicenseManager;
    private final I18nHelper i18nHelper;
    
    public ZephyrRestFilter(ZephyrLicenseManager zLicenseManager,
    						JiraAuthenticationContext authContext){
    	this.zLicenseManager = zLicenseManager;
    	this.i18nHelper = authContext.getI18nHelper();
    }
    
	@Override
	public void init(FilterConfig config) throws ServletException {
	       this.config = config;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest request,
			ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		log.debug("Inside Zephyr REST Filter...");
		
		if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))) {
		   log.info("Ignoring non-HTTP requests. Sorry dont know what to do with such requests.");
	       chain.doFilter(request, response);
	       return;
	    }
		
	    HttpServletRequest req = (HttpServletRequest)request;
	    HttpServletResponse res = (HttpServletResponse)response;
	    String url = req.getRequestURI();
	    log.debug("REST API URI - " +  url);
	    
	    //Do License Validation.
	    ZephyrLicenseVerificationResult licVerificationResult = JiraUtil.performLicenseValidation(zLicenseManager);
	    
	    //license is invalid , do not process request. Instead display the License Error Message.
	    if( !licVerificationResult.isValid()){
		    res.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
		    @SuppressWarnings("rawtypes")
			Map map = new HashMap();
		    map.put(ApplicationConstants.ERROR_ID, licVerificationResult.getException().getExceptionId());
		    map.put(ApplicationConstants.ERROR_DESC, licVerificationResult.getErrorMessage());
		    map.put(ApplicationConstants.ERROR_DESC_HTML, licVerificationResult.getGeneralMessage());
		    JSONObject jsonResponse = new JSONObject(map);
		    res.setContentType("application/json");
		    res.getWriter().print(jsonResponse.toString());
			res.getWriter().flush();
		    return;
	    }

	    
	    //Check if request came in from Browser and has eKey in it
    	String userAgent = req.getHeader("User-Agent");
    	String encryptedKey = (String)req.getHeader(ApplicationConstants.ENCRYPTED_STRING);
    	if(StringUtils.isEmpty(userAgent) || StringUtils.isEmpty(encryptedKey)) {
		    createJSONErrorResponse(res);
		    return;
    	}else if(StringUtils.contains(userAgent, "ZFJImporter") && (StringUtils.contains(req.getRequestURI(), "/teststep") || StringUtils.contains(req.getRequestURI(), "/util"))){
			//Do nothing, lets just pass thro'
		} else {
            CodecUtils codecUtils = new CodecUtils();
        	final String decryptedKey = codecUtils.decrypt(encryptedKey);
    		String[] splitTokens = StringUtils.split(decryptedKey, '|');
    		if(splitTokens.length != 3) {
    		    createJSONErrorResponse(res);
    		    return;
    		} else {
    			String date = splitTokens[0];
				Date date1 = new Date(Long.valueOf(date));
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DATE, - 7);
				if(date1.before(cal.getTime())) {
				    createJSONErrorResponse(res);
				    return;
				}
    		}
    	}

	   //Let container continue to do the next intended action.
	   chain.doFilter(request, response);
	   log.debug("Exit Zephyr REST Filter...");
	}

	private void createJSONErrorResponse(HttpServletResponse res) throws IOException {
		res.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
		Map<String, String> map = new HashMap<String, String>();
		map.put(ApplicationConstants.ERROR_ID, Status.FORBIDDEN.name());
		map.put(ApplicationConstants.ERROR_DESC, "Request Originated from Invalid source");
		map.put(ApplicationConstants.ERROR_DESC_HTML, "Request Originated from Untrusted source. If you are trying to access Zephyr API, please make sure to install ZAPI and use zapi url");
		JSONObject jsonResponse = new JSONObject(map);
		res.setContentType("application/json");
		res.getWriter().print(jsonResponse.toString());
		res.getWriter().flush();
	}
	
	@Override
	public void destroy() {
	       this.config = null;
	}
}
