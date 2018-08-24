package com.thed.zephyr.je.zql.resolver;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.jql.resolver.IndexInfoResolver;
import com.atlassian.jira.jql.resolver.NameResolver;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Index resolver that can find the index values for labels.
 *
 */
public class LabelIndexInfoResolver implements IndexInfoResolver<Label>
{
     private final NameResolver<Label> labelResolver;



    public LabelIndexInfoResolver(NameResolver<Label> labelResolver){
        this.labelResolver=labelResolver;
    }

    public List<String> getIndexedValues(final String rawValue)
    {
        notNull("rawValue", rawValue);
        final String[] labels = StringUtils.split(rawValue, LabelsSystemField.SEPARATOR_CHAR);
        final List<String> cleanLabels = (labels != null) ?
                Lists.<String>newArrayListWithCapacity(labels.length) :
                Lists.<String>newArrayList();

        if(labels != null)
        {
            for (String label : labels)
            {

                    cleanLabels.add(label);

            }
            return cleanLabels;
        }
        return Collections.emptyList();
    }



    public List<String> getIndexedValues(final Long rawValue)
    {
        notNull("rawValue", rawValue);
       return this.getIndexedValues(String.valueOf(rawValue));
    }

    public String getIndexedValue(final Label label)
    {
           return label.getLabel();
    }
}
