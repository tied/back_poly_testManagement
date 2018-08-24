package com.thed.zephyr.je.zql.core.mapper;

import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.thed.zephyr.je.zql.core.SystemSearchConstant;

import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class CycleStatisticsMapper implements LuceneFieldSorter
{
	private String documentConstant;

	public CycleStatisticsMapper(String documentConstant) {
		this.documentConstant=documentConstant;
	}
	
    public String getDocumentConstant()
    {
        return documentConstant;
    }

    public Object getValueFromLuceneField(String documentValue)
    {
        return documentValue;
    }

    public Comparator getComparator()
    {
        return NameComparator.INSTANCE;
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

        CycleStatisticsMapper that = (CycleStatisticsMapper) obj;

        return documentConstant.equals(that.getDocumentConstant());
    }
}
