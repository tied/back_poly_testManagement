package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;
import com.atlassian.jira.util.LuceneUtils;

@SuppressWarnings("deprecation")
@NotThreadSafe
public class IssueObjectCollector extends DocumentHitCollector 
{
    protected final Logger log = Logger.getLogger(IssueObjectCollector.class);
	private Map<String,Object> issueDetails;
    private IndexSearcher indexSearcher;
    private int docBase = 0;
	Map<String, HashSet<String>> result = new HashMap<>();


    public IssueObjectCollector(Map<String,Object> issueDetails, IndexSearcher searcher){
    	super(searcher);
        this.indexSearcher=searcher;
		this.issueDetails=issueDetails;
    }

	public void collect(Document document){
    	issueDetails.put("SUMMARY", document.get("summary"));
    	issueDetails.put("STATUS", document.get("status"));
    	issueDetails.put("ISSUE_ID", document.get("issue_id"));
    	issueDetails.put("CREATED_DATE", DateUtils.truncate(LuceneUtils.stringToDate(document.get("created")), Calendar.DATE));
    	doCollect(issueDetails);
	}
	
	private void doCollect( Map<String, Object> issueDetails){
		
		String dateStr = issueDetails.get("CREATED_DATE").toString();
	    if(!result.containsKey(dateStr)) {
	       	result.put(dateStr, new HashSet<String>());
	    }
	    result.get(dateStr).add((String) issueDetails.get("ISSUE_ID"));

	}
	
	public Map<String, HashSet<String>> getIssueObject(){
		return  result;
	}
	
}