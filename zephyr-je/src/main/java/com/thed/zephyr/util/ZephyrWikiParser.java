package com.thed.zephyr.util;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.renderer.wiki.WikiRendererFactory;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.v2.RenderMode;

/**
 * Wiki Parser
 */
public class ZephyrWikiParser {
	public static final ZephyrWikiParser WIKIPARSER = new ZephyrWikiParser();
	private WikiRendererFactory wikiRenderer;

	public ZephyrWikiParser() {
    	wikiRenderer = new WikiRendererFactory();
	}
	
	/**
	 * Convert wiki to HTML
	 */
	public String convertWikiToHTML(String markup, Issue issue) {
		String baseURL = ComponentAccessor.getWebResourceUrlProvider().getBaseUrl();
		RenderContext context = new RenderContext();
		if(issue != null)
			context.addParam("jira.issue", issue);
		context.setBaseUrl(baseURL);
		context.pushRenderMode(RenderMode.ALL_WITH_NO_MACRO_ERRORS);
		String html = wikiRenderer.getWikiRenderer().convertWikiToXHtml(context, markup);
		return html;
	}

	/**
	 * Convert wiki to Text
	 */
	public String convertWikiToText(String markup) {
		RenderContext context = new RenderContext();
		String baseURL = ComponentAccessor.getWebResourceUrlProvider().getBaseUrl();
		context.setBaseUrl(baseURL);
		
		if(null != markup) {
			String imageMarkupPattern = "!([^\\s]+)!"; // Fix for ZFJ-1507
			markup = markup.replaceAll(imageMarkupPattern, "$1");
		}
		
		// Convert wiki to Text
		String wikiToText = wikiRenderer.getWikiRenderer().convertWikiToText(context, markup);
		
		wikiToText = wikiToText.replaceAll("\n", ""); // Fix for ZFJ-1551, replace new line to the "", for csv format
		return wikiToText;
	}
}
