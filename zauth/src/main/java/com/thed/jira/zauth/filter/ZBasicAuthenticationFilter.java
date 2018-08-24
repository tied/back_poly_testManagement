package com.thed.jira.zauth.filter;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thed.jira.zauth.utils.JIRAUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.seraph.auth.DefaultAuthenticator;
import com.atlassian.seraph.filter.HttpAuthFilter;
import com.atlassian.seraph.util.SecurityUtils;
import com.google.common.collect.Iterables;

public class ZBasicAuthenticationFilter extends HttpAuthFilter{
	
	private static final Logger log = Logger.getLogger(ZBasicAuthenticationFilter.class);
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))) {
	       chain.doFilter(request, response);
	       if(log.isInfoEnabled())
	    	   log.info("Ignoring non-HTTP requests. Sorry dont know what to do with such requests.");
	       return;
	    }
		String remoteAddr = getClientIpAddr((HttpServletRequest)request);
		
		if(log.isInfoEnabled())
			log.info(remoteAddr);
		if(!Iterables.contains(JIRAUtil.getWhiteList(), remoteAddr)){
			if(log.isInfoEnabled())
				log.info("Requesting server is not in white list, ignoring it...");
			chain.doFilter(request, response);
		    return;
		}
		
	    try {
	    	final SecurityUtils.UserPassCredentials creds = getUserCreds((HttpServletRequest)request);
	    	String userName = "" ;
			if(creds != null && StringUtils.isNotBlank(creds.getUsername())){
				userName = creds.getUsername();
			}else{
				userName = request.getParameter("os_username");
			}
	    	
			final String finalUserName = userName;
	    	if(StringUtils.isNotBlank(finalUserName)){
	    		final ApplicationUser user = ComponentAccessor.getUserManager().getUserByName(userName);
				ComponentAccessor.getJiraAuthenticationContext().setLoggedInUser(user);
				request.setAttribute(OS_AUTHSTATUS_KEY, LOGIN_SUCCESS);
				SecurityUtils.disableSeraphFiltering(request);
				Principal principal = new Principal() {
					@Override
					public String getName() {
						return finalUserName;
					}
				};
				((HttpServletRequest)request).getSession().setAttribute(DefaultAuthenticator.LOGGED_IN_KEY, principal);
				getAuthenticationContext().setUser(principal);
				if(log.isInfoEnabled())
					log.info("User authenticated ");
	    	}
		} catch (Exception e) {
			log.fatal("", e);
		} 
		chain.doFilter(request, response);
		
		if(log.isInfoEnabled())
			log.info("DONE " + response);
	}
	
	/**
	 * extracts senders IP Address from request
	 * @param request
	 * @return
	 */
	public String getClientIpAddr(HttpServletRequest request) {  
        String ip = request.getHeader("X-Forwarded-For");  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {  
            ip = request.getRemoteAddr();  
        }  
        return ip;  
    }
	
	/**
	 * 
	 * @param request
	 * @return
	 */
	public SecurityUtils.UserPassCredentials getUserCreds(HttpServletRequest request) {
		String auth = request.getHeader("Authorization");
		if (SecurityUtils.isBasicAuthorizationHeader(auth)) {
			return SecurityUtils.decodeBasicAuthorizationCredentials(auth);
		}
		return null;
	}
}
