package com.thed.zephyr.je.action.enav;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.searchers.renderer.AbstractSearchRenderer;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.collect.CompositeMap;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.velocity.VelocityManager;

public abstract class ZAbstractSearchRenderer implements ZSearchRenderer {
	private static final Logger log = Logger.getLogger(AbstractSearchRenderer.class);
	private static final String SEARCHER_TEMPLATE_DIRECTORY_PATH = "templates/enav/";
	protected VelocityManager velocityManager;
	protected ApplicationProperties applicationProperties;
	protected VelocityRequestContextFactory velocityRequestContextFactory;

	public ZAbstractSearchRenderer(final VelocityRequestContextFactory velocityRequestContextFactory, final VelocityManager velocityManager, final ApplicationProperties applicationProperties) {
		this.velocityRequestContextFactory = velocityRequestContextFactory;
		this.velocityManager = velocityManager;
		this.applicationProperties = applicationProperties;
		init();
	}

	protected void init() {

	}

	@Override
	public String getHTML(final ApplicationUser searcher, final FieldValuesHolder fieldValuesHolder, final Map displayParameters) {
		Map<String, Object> velocityParams = getVelocityParams(searcher, fieldValuesHolder, displayParameters);
		return renderTemplate(getTemplate(), velocityParams);
	}
	
	/**
	 * 
	 * @param searcher
	 * @param fieldValuesHolder
	 * @param displayParameters
	 * @return
	 */
	protected Map<String, Object> getVelocityParams(final ApplicationUser searcher, final FieldValuesHolder fieldValuesHolder, final Map displayParameters){
		final JiraAuthenticationContext jiraAuthenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        final Map<String, Object> velocityParams = new HashMap<String, Object>(20);
        velocityParams.put("baseurl", velocityRequestContextFactory.getJiraVelocityRequestContext().getBaseUrl());
        velocityParams.put("auiparams", new HashMap<String, Object>());
        velocityParams.put("displayParameters", displayParameters);
        velocityParams.put("i18n", getI18n(searcher));
        velocityParams.put("searcherId", getSearcherId());
        velocityParams.put("searcherName", getSearcherNameKey());
        return CompositeMap.of(velocityParams, JiraVelocityUtils.getDefaultVelocityParams(jiraAuthenticationContext));
	}

	/**
	 * 
	 * @param template
	 * @param velocityParams
	 * @return
	 */
	protected String renderTemplate(final String template,final Map velocityParams) {
		try {
			return velocityManager.getEncodedBody(SEARCHER_TEMPLATE_DIRECTORY_PATH, template, applicationProperties.getEncoding(), velocityParams);
		} catch (final VelocityException e) {
			log.error("Error occurred while rendering velocity template for '" + SEARCHER_TEMPLATE_DIRECTORY_PATH + "/" + template + "'.", e);
		}

		return "";
	}
	
	 protected I18nHelper getI18n(final ApplicationUser searcher){
        return ComponentAccessor.getComponent(I18nHelper.BeanFactory.class).getInstance(searcher);
    }
}
