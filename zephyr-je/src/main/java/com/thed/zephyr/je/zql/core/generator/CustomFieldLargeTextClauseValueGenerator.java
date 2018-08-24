package com.thed.zephyr.je.zql.core.generator;


import com.atlassian.jira.issue.comparator.LocaleSensitiveStringComparator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.ExecutionCf;
import com.thed.zephyr.je.service.CustomFieldValueManager;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CustomFieldLargeTextClauseValueGenerator implements ClauseValuesGenerator {
    private final CustomFieldValueManager customFieldValueManager;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final I18nHelper.BeanFactory beanFactory;
    private static final Integer MAX_NUM_RESULTS = 30;

    public CustomFieldLargeTextClauseValueGenerator(CustomFieldValueManager customFieldValueManager,ZephyrCustomFieldManager zephyrCustomFieldManager,
                                                 I18nHelper.BeanFactory beanFactory) {
        this.customFieldValueManager = customFieldValueManager;
        this.zephyrCustomFieldManager = zephyrCustomFieldManager;
        this.beanFactory = beanFactory;
    }

    public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {

        Integer customFieldId = null;
        List<ExecutionCf> executionsCfList;
        if(StringUtils.isNotBlank(jqlClauseName)) {
            CustomField customField  = zephyrCustomFieldManager.getCustomFieldByName(jqlClauseName);
            if(Objects.nonNull(customField)) {
                customFieldId = customField.getID();
            }
        }

        executionsCfList = customFieldValueManager.checkCustomFieldValueExistByTypeAndKey(ApplicationConstants.LARGE_VALUE, valuePrefix,customFieldId);
        List<String> values = new ArrayList<>();
        for(ExecutionCf executionCf : executionsCfList) {
            values.add(executionCf.getLargeValue());
        }
        Collections.sort(values, new LocaleSensitiveStringComparator(getLocale(searcher)));

        final Set<Result> largeTextCustomFieldValues = new LinkedHashSet<>();
        for (String value : values)
        {
            if(largeTextCustomFieldValues.size() > MAX_NUM_RESULTS) {
                break;
            }
            largeTextCustomFieldValues.add(new Result(value));
        }
        return new Results(new ArrayList<ClauseValuesGenerator.Result>(largeTextCustomFieldValues));
    }

    Locale getLocale(final ApplicationUser searcher) {
        return beanFactory.getInstance(searcher).getLocale();
    }
}
