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
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.service.FolderManager;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * Class generates the folder names for the clause.
 * 
 * @author manjunath
 *
 */
public class FolderNameClauseValueGenerator implements ClauseValuesGenerator {

	private final FolderManager folderManager;
	private final PermissionManager permissionManager;
    private final I18nHelper.BeanFactory beanFactory;

    public FolderNameClauseValueGenerator(FolderManager folderManager, PermissionManager permissionManager, I18nHelper.BeanFactory beanFactory) {
    	this.folderManager = folderManager;
    	this.permissionManager = permissionManager;
    	this.beanFactory = beanFactory;
    }

    @SuppressWarnings("unchecked")
	public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {
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
        List<Folder> folders = new ArrayList<>();

        partitionedList.forEach(
                partition -> {
                    folders.addAll(folderManager.getFoldersByProjectId(partition, "NAME", valuePrefix));
                }
        );

    	List<String> values = new ArrayList<String>();
    	for(Folder folder : folders) {
    		values.add(folder.getName());
    	}
        Collections.sort(values, new LocaleSensitiveStringComparator(getLocale(searcher)));

        final Set<Result> folderNameValues = new LinkedHashSet<Result>();
        for (String value : values)
        {  
        	folderNameValues.add(new Result(value));
        }
        return new Results(new ArrayList<ClauseValuesGenerator.Result>(folderNameValues));
    }
	
    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}

