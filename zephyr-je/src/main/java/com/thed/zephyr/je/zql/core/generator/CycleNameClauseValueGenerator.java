package com.thed.zephyr.je.zql.core.generator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.I18nHelper;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.util.ApplicationConstants;

public class CycleNameClauseValueGenerator implements ClauseValuesGenerator {

	private final CycleManager cycleManager;
	private final PermissionManager permissionManager;
    private final I18nHelper.BeanFactory beanFactory;

    public CycleNameClauseValueGenerator(CycleManager cycleManager,
    		PermissionManager permissionManager,I18nHelper.BeanFactory beanFactory)
    {
    	this.cycleManager=cycleManager;
    	this.permissionManager=permissionManager;
    	this.beanFactory=beanFactory;
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
         List<List<Integer>> partitioned = Lists.partition(new ArrayList(collection), 1000);
         List<Cycle> cycles = new ArrayList<Cycle>();
         for (List<Integer> list : partitioned) {
        	 cycles.addAll(cycleManager.getCyclesByProjectId(list,"NAME",valuePrefix));
		}
       
    	List<String> values = new ArrayList<String>();
        if(StringUtils.startsWith(ApplicationConstants.AD_HOC_CYCLE_NAME,valuePrefix)) {
        	values.add(ApplicationConstants.AD_HOC_CYCLE_NAME);
        }
    	for(Cycle cycle : cycles) {
    		values.add(cycle.getName());
    	}
        Collections.sort(values, new LocaleSensitiveStringComparator(getLocale(searcher)));

        final Set<Result> cycleNameValues = new LinkedHashSet<Result>();
        for (String value : values)
        {  
        	cycleNameValues.add(new Result(value));
        }
        return new Results(new ArrayList<ClauseValuesGenerator.Result>(cycleNameValues));
    }
	
    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}
