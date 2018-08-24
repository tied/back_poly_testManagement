package com.thed.zephyr.je.zql.core.mapper;


import com.atlassian.jira.issue.search.LuceneFieldSorter;

import java.util.Comparator;

public class FolderStatisticsMapper implements LuceneFieldSorter {

    private String documentConstant;

    public FolderStatisticsMapper(String documentConstant) {
        this.documentConstant = documentConstant;
    }

    @Override
    public String getDocumentConstant() {
        return documentConstant;
    }

    @Override
    public Object getValueFromLuceneField(String documentValue) {
        return documentValue;
    }

    @Override
    public Comparator getComparator() {
        return NameComparator.INSTANCE;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        FolderStatisticsMapper that = (FolderStatisticsMapper) object;

        return documentConstant != null ? documentConstant.equals(that.documentConstant) : that.documentConstant == null;
    }

    @Override
    public int hashCode() {
        return documentConstant != null ? documentConstant.hashCode() : 0;
    }
}
