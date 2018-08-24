package com.thed.zephyr.je.zql.core.mapper;

import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.statistics.ValueStatisticMapper;
import com.thed.zephyr.je.model.CustomFieldOption;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.log4j.Logger;

@SuppressWarnings("rawtypes")
public class CustomFieldListStatisticsMapper implements LuceneFieldSorter
{
    protected final Logger log = Logger.getLogger(CustomFieldListStatisticsMapper.class);
    private String documentConstant;

	public CustomFieldListStatisticsMapper(String documentConstant) {
		this.documentConstant=documentConstant;
	}
	
    public String getDocumentConstant()
    {
        return documentConstant;
    }

    public Object getValueFromLuceneField(String documentValue) {
        return documentValue;
    }

    public Comparator getComparator()
    {
        return ListValueComparator.INSTANCE;
    }

    public int hashCode()
    {
        return documentConstant.hashCode();
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        // Ensure that we test that the other object is an instance of the same class! As we could have a
        // totally different sorter also sort on the Issue Key constant (so comparing constants is not enought).
        // For example, the MultiIssueKeySearcher in the JIRA toolkit also sorts on Issue Key 
        if (obj == null || getClass() != obj.getClass())
        {
            return false;
        }

        CustomFieldListStatisticsMapper that = (CustomFieldListStatisticsMapper) obj;

        return documentConstant.equals(that.getDocumentConstant());
    }
}
