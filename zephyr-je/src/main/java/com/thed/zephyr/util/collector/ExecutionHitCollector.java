package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

@NotThreadSafe
@Deprecated //Not used
public class ExecutionHitCollector extends Collector {
    protected final Logger log = Logger.getLogger(ExecutionHitCollector.class);
	private Map<String,Object> scheduleDetails;
    private IndexSearcher indexSearcher;
    private Integer executionId;
    private int docBase = 0;
    private boolean nextExecutionFlag;
    private boolean prevExecutionFlag;
    
    /**
     * @param indexSearcher
     * @param maximumSize The maximum number of issue IDs / keys to collect (the stable search limit).
     * @param executionId The id of the selected execution (or {@code null}).
     */
    public ExecutionHitCollector(IndexSearcher scheduleSearcher,
    		Integer executionId,Map<String,Object> scheduleDetails) {
        this.indexSearcher = scheduleSearcher;
        this.executionId = executionId;
        this.scheduleDetails=scheduleDetails;
    }

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		// TODO Auto-generated method stub
	}



    public void collect(int i) throws IOException {
		final Document scheduleDocument = getDocument(docBase + i);
		String scheduleId = scheduleDocument.get("schedule_id");
		if(StringUtils.isNotBlank(scheduleId)) {
	    	if(nextExecutionFlag) {
				scheduleDetails.put("nextExecutionId", scheduleId);
	    		nextExecutionFlag = false;
	    	}
			//Mark the Prev and Next ScheduleId
	    	if(Integer.parseInt(scheduleId) == executionId.intValue()) {
				scheduleDetails.put(scheduleDocument.get("schedule_id"), scheduleDocument);
	    		nextExecutionFlag = true;
	    		prevExecutionFlag = true;
	    	}
	    	if(!prevExecutionFlag) {
				scheduleDetails.put("prevExecutionId", scheduleId);
	    	}
		}
	}

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        this.docBase = docBase;
    }



	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}
	
	public Map<String,Object> getScheduleDocument() {
		return scheduleDetails;
	}
	
    /**
     * Get Index Document by DocID
     * @param docId
     * @return
     */
	private Document getDocument(final int docId) {
        try {
            return indexSearcher.doc(docId);
        }
        catch (IOException e) {
        	log.warn("Error Retrieving Index:",e);
            return null;
        }
    }
}