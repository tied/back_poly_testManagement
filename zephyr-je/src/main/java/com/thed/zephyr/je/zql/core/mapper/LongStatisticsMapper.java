package com.thed.zephyr.je.zql.core.mapper;

import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.statistics.LongFieldStatisticsMapper;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.issue.statistics.util.LongComparator;
import org.apache.lucene.document.NumberTools;

import java.util.Comparator;

/**
 * Created by niravshah on 4/15/18.
 */
public class LongStatisticsMapper implements LuceneFieldSorter {
    private final String documentConstant;

    public LongStatisticsMapper(String documentConstant) {
        this.documentConstant = documentConstant;
    }

    @Override
    public String getDocumentConstant() {
        return this.documentConstant;
    }


    public Object getValueFromLuceneField(String documentValue) {
        if (FieldIndexer.NO_VALUE_INDEX_VALUE.equals(documentValue)) {
            return null;
        } else {
            return NumberTools.stringToLong(documentValue);
        }
    }


    public Comparator getComparator() {
        return LongComparator.COMPARATOR;
    }

    public boolean equals(Object o) {
        if(this == o) {
            return true;
        } else if(!(o instanceof LongStatisticsMapper)) {
            return false;
        } else {
            LongStatisticsMapper that = (LongStatisticsMapper)o;
            if(this.documentConstant != null) {
                if(!this.documentConstant.equals(that.documentConstant)) {
                    return false;
                }
            } else if(that.documentConstant != null) {
                return false;
            }

            return true;
        }
    }

    public int hashCode() {
        return this.getDocumentConstant() != null?this.getDocumentConstant().hashCode():0;
    }
}
