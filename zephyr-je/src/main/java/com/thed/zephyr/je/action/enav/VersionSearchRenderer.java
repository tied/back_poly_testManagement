package com.thed.zephyr.je.action.enav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.velocity.VelocityManager;

public class VersionSearchRenderer extends ZAbstractSearchRenderer{

	private Project selectedProject;
	private VersionManager versionManager;
	
	public VersionSearchRenderer(final VelocityRequestContextFactory velocityRequestContextFactory, final VelocityManager velocityManager, 
							final ApplicationProperties applicationProperties, Project project, VersionManager versionManager) {
		super(velocityRequestContextFactory, velocityManager, applicationProperties);
		this.selectedProject = project;
		this.versionManager = versionManager;
	}

	@Override
	public String getSearcherNameKey() {
		return "enav.version.searcher.name";
	}
	
	@Override
	public String getSearcherId() {
		return "vid";
	}
	
	@Override
	public String getTemplate() {
		return "version-searcher.vm";
	}

	@Override
	protected Map<String, Object> getVelocityParams(final ApplicationUser searcher, final FieldValuesHolder fieldValuesHolder, final Map displayParameters){
		final Map<String, Object> velocityParams = super.getVelocityParams(searcher, fieldValuesHolder, displayParameters);
		velocityParams.put("versions", getVersions());
		return velocityParams;
	}
	
	private Iterable<Version> getVersions(){
		List<Version> allVersions = new ArrayList<Version>();
		if(selectedProject != null){
			Collection<Version> unreleasedVersions = versionManager.getVersionsUnreleased(selectedProject.getId(), false);
			Collection<Version> releasedVersions = versionManager.getVersionsReleased(selectedProject.getId(), false);
			allVersions.addAll(unreleasedVersions);
			allVersions.addAll(releasedVersions);
		}
		return allVersions;
	}
	

}
