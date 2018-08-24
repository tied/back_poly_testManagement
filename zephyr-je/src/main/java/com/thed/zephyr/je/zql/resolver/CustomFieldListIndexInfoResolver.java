package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.CustomFieldOption;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for labels.
 *
 */
public class CustomFieldListIndexInfoResolver implements IndexInfoResolver<CustomFieldOption>
{
   private final NameResolver<CustomFieldOption> customFieldListResolver;

    public CustomFieldListIndexInfoResolver(NameResolver<CustomFieldOption> customFieldListResolver){
        this.customFieldListResolver=customFieldListResolver;
    }

    public List<String> getIndexedValues(final String rawValue) {
        notNull("rawValue", rawValue);
        final String[] customFields = StringUtils.split(rawValue, ",");
        final List<String> customFieldList = (customFields != null) ?
                Lists.<String>newArrayListWithCapacity(customFields.length) :
                Lists.<String>newArrayList();

        if(customFields != null) {
            for (String customField : customFields) {
                List<String> idsFromName = customFieldListResolver.getIdsFromName(customField);
                customFieldList.addAll(idsFromName);
            }
            return customFieldList;
        }
        return Collections.emptyList();
    }



    public List<String> getIndexedValues(final Long rawValue) {
        notNull("rawValue", rawValue);
       return this.getIndexedValues(String.valueOf(rawValue));
    }

    public String getIndexedValue(final CustomFieldOption customFieldOption) {
           return String.valueOf(customFieldOption.getCustomField().getID());
    }
}
