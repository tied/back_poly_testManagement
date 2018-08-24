package com.thed.zephyr.je.zql.core.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;

public class CycleBuildClauseValueGenerator implements ClauseValuesGenerator {

	private final CycleManager cycleManager;
	private final PermissionManager permissionManager;

    public CycleBuildClauseValueGenerator(CycleManager cycleManager,PermissionManager permissionManager)
    {
    	this.cycleManager=cycleManager;
    	this.permissionManager=permissionManager;
    }

    @SuppressWarnings("unchecked")
	public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults)
    {
        final Collection<Project> projects = permissionManager.getProjects(ProjectPermissions.BROWSE_PROJECTS, searcher);
        final Collection<Integer> collection = CollectionUtils.collect(projects, new Transformer() {
            @Override
			public Object transform(final Object input) {
                if (input == null) {
                    return null;
                }
                
                final Project project = (Project)input;
                return project.getId();
            }
        });
        List<List<Integer>> partitionedList = Lists.partition(new ArrayList(collection), 1000);
        List<Cycle> cycles = new ArrayList<>();

        partitionedList.forEach(
                partition -> {
                    cycles.addAll(cycleManager.getCyclesByProjectId(partition,"BUILD",valuePrefix));
                }
        );

        List<String> values = new ArrayList<String>();

    	for(Cycle cycle : cycles) {
    		values.add(cycle.getBuild());
    	}
    	
        final Set<Result> cycleBuildValues = new LinkedHashSet<Result>();
        for (String value : values)
        {  
        	cycleBuildValues.add(new Result(value));
        }
        return new Results(new ArrayList<ClauseValuesGenerator.Result>(cycleBuildValues));
    }
	
}
