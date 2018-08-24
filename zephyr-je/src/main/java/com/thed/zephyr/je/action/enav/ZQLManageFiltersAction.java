package com.thed.zephyr.je.action.enav;

import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.velocity.VelocityManager;

public class ZQLManageFiltersAction extends JiraWebActionSupport {
	
	/**
	 * Serial version UUID
	 */
	private static final long serialVersionUID = -3392865551313869350L;
	
	VelocityRequestContextFactory velocityRequestContextFactory;
	private VelocityManager velocityManager;

	/**
	 * 
	 */
	public ZQLManageFiltersAction(
			VelocityRequestContextFactory velocityRequestContextFactory,
			final VelocityManager velocityManager) {
		this.velocityRequestContextFactory = velocityRequestContextFactory;
		this.velocityManager = velocityManager;
	}

	@Override
	public String doDefault() throws Exception{
		return super.doDefault();
	}
	
	@Override
	public String doExecute() throws Exception{
		return super.doExecute();
	}	
}
