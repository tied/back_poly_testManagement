package com.thed.zephyr.je.zql.core.generator;

import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CustomFieldStringClauseValueGenerator implements ClauseValuesGenerator {

	private final PermissionManager permissionManager;
	private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final I18nHelper.BeanFactory beanFactory;
    private static final Integer MAX_NUM_RESULTS = 30;

    public CustomFieldStringClauseValueGenerator(PermissionManager permissionManager,ZephyrCustomFieldManager zephyrCustomFieldManager,
                                                 I18nHelper.BeanFactory beanFactory) {
    	this.permissionManager = permissionManager;
    	this.zephyrCustomFieldManager = zephyrCustomFieldManager;
    	this.beanFactory = beanFactory;
    }

    public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {
        final Collection<Project> visibleProjects = permissionManager.getProjects(ProjectPermissions.BROWSE_PROJECTS, searcher);

        Integer customFieldId = null;
        if(StringUtils.isNotBlank(jqlClauseName)) {
            CustomField customField  = zephyrCustomFieldManager.getCustomFieldByName(jqlClauseName);
            if(Objects.nonNull(customField)) {
                customFieldId = customField.getID();
            }
        }
        Set<Result> stringCustomFieldValues = new LinkedHashSet<Result>();
        CustomFieldOption[] customFieldOptions = zephyrCustomFieldManager.getCustomFieldOptions(customFieldId);
        List<String> values = new ArrayList<>();
        if(customFieldOptions != null) {
            for (CustomFieldOption customFieldOption : customFieldOptions) {
                if(customFieldOption.getCustomField().getProjectId() != null) {
                    visibleProjects.stream().forEach(project -> {
                        if (project.getId().longValue() == customFieldOption.getCustomField().getProjectId().longValue()) {
                            values.add(customFieldOption.getOptionValue());
                        }
                    });
                } else {
                    values.add(customFieldOption.getOptionValue());
                }
            }
            Collections.sort(values, new LocaleSensitiveStringComparator(getLocale(searcher)));
            for (String value : values) {
                if (stringCustomFieldValues.size() > MAX_NUM_RESULTS) {
                    break;
                }
                stringCustomFieldValues.add(new Result(value));
            }
        }
        return new Results(new ArrayList<>(stringCustomFieldValues));
    }

    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}
