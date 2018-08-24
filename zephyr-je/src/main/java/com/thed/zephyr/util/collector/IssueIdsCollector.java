package com.thed.zephyr.util.collector;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;

import com.atlassian.jira.issue.index.DocumentConstants;

@NotThreadSafe
public class IssueIdsCollector extends SimpleFieldCollector
{
	public final static String NAME = DocumentConstants.ISSUE_ID;
	
    public IssueIdsCollector(IndexSearcher searcher){
    	this(NAME, searcher);
    }
    
    public IssueIdsCollector(final String fieldName, IndexSearcher searcher){
    	super(searcher, fieldName);
    	//Using set to avoid duplicate issues
    	value = new HashSet<Long>();
    }

    @Override
	@SuppressWarnings("unchecked")
    public void doCollect(Document d){
        ((Set<Long>)value).add(new Long(d.get(getFieldName())));
    }
}