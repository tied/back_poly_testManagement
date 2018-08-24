package com.thed.zephyr.util.collector;

import com.atlassian.jira.issue.index.DocumentConstants;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

@NotThreadSafe
public class ProjectIdsCollector extends SimpleFieldCollector
{
	public final static String NAME = DocumentConstants.PROJECT_ID;
    private Set<Long> projects;


    public ProjectIdsCollector(IndexSearcher searcher){
    	this(NAME, searcher);
    }

    public ProjectIdsCollector(final String fieldName, IndexSearcher searcher){
    	super(searcher, fieldName);
    	value = new HashSet<Long>();
    }

    @Override
	@SuppressWarnings("unchecked")
    public void doCollect(Document d){
        Long projectId = new Long(d.get(getFieldName()));
        ((Set<Long>)value).add(projectId);
    }


}