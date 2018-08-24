package com.thed.stanford.zauth.filter;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import com.atlassian.jira.user.util.UserManager;

public class ZAuthenticationFilterTest {

	private Mockery context = new Mockery();

	private ZAuthenticationFilter zauthenticationFilter = new ZAuthenticationFilter();
	private HttpServletRequest request;
	private HttpServletResponse response;
	private FilterChain chain;

	private JiraAuthenticationContext jiraAuthenticationContext;

	private UserManager userManager;

	@Before
	public void setUp() throws Exception {
		request = context.mock(HttpServletRequest.class);
	    response = context.mock(HttpServletResponse.class);
	    chain = context.mock(FilterChain.class);
	    
		jiraAuthenticationContext = context.mock(JiraAuthenticationContext.class);
		userManager = context.mock(UserManager.class);
	    new MockComponentWorker().addMock(JiraAuthenticationContext.class, jiraAuthenticationContext)
	    						.addMock(UserManager.class, userManager)
	    						.init();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDoFilter() throws IOException, ServletException {
		final ApplicationUser admin = new MockApplicationUser("admin");
		//assertNull(ComponentAccessor.getJiraAuthenticationContext().getUser());
		context.checking(new Expectations(){{
		      oneOf(request).getHeader("X-Forwarded-For");
		      will(returnValue(null));
		      oneOf(request).getHeader("Proxy-Client-IP");
		      will(returnValue(null));
		      oneOf(request).getHeader("WL-Proxy-Client-IP");
		      will(returnValue(null));
		      oneOf(request).getHeader("HTTP_CLIENT_IP");
		      will(returnValue(null));
		      oneOf(request).getHeader("HTTP_X_FORWARDED_FOR");
		      will(returnValue(null));
		      oneOf(request).getRemoteAddr();
		      will(returnValue("127.0.0.1"));
		      oneOf(userManager).getUserByName(null);
		      will(returnValue(admin));
		      oneOf(chain).doFilter(request, response);
		      exactly(2).of(jiraAuthenticationContext).getUser();
		      will(returnValue(admin));
		    }});
		zauthenticationFilter.doFilter(request, response, chain);
		assertNotNull(ComponentAccessor.getJiraAuthenticationContext().getUser());
		assertEquals(admin, ComponentAccessor.getJiraAuthenticationContext().getUser());
	}

	@Test
	public void testGetClientIpAddr() throws IOException, ServletException {
		//zauthenticationFilter.doFilter(request, response, null);
	}

}
