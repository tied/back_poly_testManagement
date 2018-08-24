package com.thed.zephyr.je.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.core.filters.AbstractHttpFilter;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.util.URLCodec;

public class ENavRedirectFilter extends AbstractHttpFilter {
    private final JiraAuthenticationContext authContext;


    public ENavRedirectFilter(final JiraAuthenticationContext authContext)
    {
        this.authContext = authContext;
    }

    @Override
    protected void doFilter(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException
    {
    	if(authContext.getLoggedInUser() == null) {
        	String uri = getFWDRequestURI(request);
        	//For popups, We should just send error and display the link instead of redirecting
        	response.sendRedirect(request.getContextPath() + "/login.jsp?permissionViolation=true&os_destination=" + uri);
        	return;
    	}

        StringBuilder redirectUrl = new StringBuilder(request.getContextPath() + "/secure/enav/#");
        // Check if the user was starting a new search, looking at a
        // filter, or looking at JQL. We want to redirect accordingly.
        final String filterId = request.getParameter("filterId");
        String zqlQuery = request.getParameter("query");
        String view = request.getParameter("view");
        String offset = request.getParameter("offset");

        if (filterId != null || zqlQuery != null || view != null || offset != null) {
        	redirectUrl.append("?");
        }
        
        if (filterId != null) {
            redirectUrl.append("filter=").append(JiraUrlCodec.encode(filterId));
        } else if (zqlQuery != null) {
            redirectUrl.append("query=").append(JiraUrlCodec.encode(zqlQuery));
        } 
        if (view != null) {
            redirectUrl.append("&view=").append(JiraUrlCodec.encode(view));
        } 
        if (offset != null) {
            redirectUrl.append("&offset=").append(JiraUrlCodec.encode(view));
        } 
        response.sendRedirect(redirectUrl.toString());
        return;
    }
    
	/**
	 * @throws UnsupportedEncodingException 
	 * @returns uri that can be appended to the login page 
	 */
	private String getFWDRequestURI(final HttpServletRequest request) throws UnsupportedEncodingException {
		String uri = request.getServletPath();
		if(request.getQueryString() != null ){
			uri += "?" + request.getQueryString();
		}
		return URLCodec.encode(uri);
	}
}
