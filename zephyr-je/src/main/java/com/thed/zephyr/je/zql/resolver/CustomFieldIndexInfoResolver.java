package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomField;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for labels.
 *
 */
public class CustomFieldIndexInfoResolver implements IndexInfoResolver<CustomField>
{
     private final NameResolver<CustomField> customFieldResolver;

    public CustomFieldIndexInfoResolver(NameResolver<CustomField> customFieldResolver){
        this.customFieldResolver=customFieldResolver;
    }

    public List<String> getIndexedValues(final String rawValue)
    {
        notNull("rawValue", rawValue);
        final String[] customFields = StringUtils.split(rawValue, LabelsSystemField.SEPARATOR_CHAR);
        final List<String> customFieldList = (customFields != null) ?
                Lists.<String>newArrayListWithCapacity(customFields.length) :
                Lists.<String>newArrayList();

        if(customFields != null) {
            for (String customField : customFields) {
                customFieldList.add(customField);
            }
            return customFieldList;
        }
        return Collections.emptyList();
    }



    public List<String> getIndexedValues(final Long rawValue)
    {
        notNull("rawValue", rawValue);
       return this.getIndexedValues(String.valueOf(rawValue));
    }

    public String getIndexedValue(final CustomField customField)
    {
           return customField.getName();
    }
}
