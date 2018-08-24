package com.thed.zephyr.je.action.enav;

import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.velocity.VelocityManager;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;

public class CycleSearchRenderer extends ZAbstractSearchRenderer{

	private Project selectedProject;
	private CycleManager cycleManager;
	
	public CycleSearchRenderer(final VelocityRequestContextFactory velocityRequestContextFactory, final VelocityManager velocityManager, 
							final ApplicationProperties applicationProperties, Project project, CycleManager cycleManager) {
		super(velocityRequestContextFactory, velocityManager, applicationProperties);
		this.selectedProject = project;
		this.cycleManager = cycleManager;
	}

	@Override
	public String getSearcherId() {
		return "cid";
	}
	
	@Override
	public String getSearcherNameKey() {
		return "enav.cycle.searcher.name";
	}
	
	@Override
	public String getTemplate() {
		return "cycle-searcher.vm";
	}

	@Override
	protected Map<String, Object> getVelocityParams(final ApplicationUser searcher, final FieldValuesHolder fieldValuesHolder, final Map displayParameters){
		final Map<String, Object> velocityParams = super.getVelocityParams(searcher, fieldValuesHolder, displayParameters);
		velocityParams.put("cycles", getCycles());
		return velocityParams;
	}
	
	private List<Cycle> getCycles(){
		List<Cycle> allCycles = cycleManager.getCyclesByVersion(CycleManager.ANY, selectedProject.getId(),null);
		return allCycles;
	}
}
