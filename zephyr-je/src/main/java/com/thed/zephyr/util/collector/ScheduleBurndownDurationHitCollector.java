package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import com.atlassian.jira.util.LuceneUtils;

/**
 * Thread Unsafe class
 * @author smangal
 *
 */
public class ScheduleBurndownDurationHitCollector extends Collector {
	
	/**
	 * Temporary variable to store intermediate data 
	 */
	private final Map<String, Object> tempData;
	
	private final IndexSearcher indexSearcher;
	private int docBase = 0;
	private FieldSelector fieldSelector;
	private Boolean isUnexecutedScheduleExists = false;
	
	private final List<String> fields = Arrays.asList(new String[]{"DATE_CREATED", "EXECUTED_ON", "schedule_id"});
	protected final Logger log = Logger.getLogger(ScheduleBurndownDurationHitCollector.class);
	
	private Boolean collectedCalled = false;
	

	public ScheduleBurndownDurationHitCollector(IndexSearcher indexSearcher) {
		super();
		this.tempData = new HashMap<String, Object>();
		this.indexSearcher=indexSearcher;
		this.fieldSelector = new FieldSelector() {
			@Override
			public FieldSelectorResult accept(String fieldName) {
				if(fields.contains(fieldName))
					return FieldSelectorResult.LOAD;
				return FieldSelectorResult.NO_LOAD;
			}
		};
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void collect(int doc) throws IOException {
		collectedCalled = true;
		final Document scheduleDocument = getDocument(docBase + doc);
		log.debug("doc id - "+ doc + "Scheule Id - " + scheduleDocument.get("schedule_id") + " creation date - "+ scheduleDocument.get(fields.get(0)) + " Execution Date - " + scheduleDocument.get(fields.get(1)));
		
		extractMinDate(scheduleDocument.get(fields.get(0)), fields.get(0));
		String executedOnDate = scheduleDocument.get(fields.get(1));
		extractMinDate(executedOnDate, fields.get(1));
		
		//Execution Date is null, hence this schedule is unexecuted
		if(executedOnDate == null){
			isUnexecutedScheduleExists = true;
		}
		if(!isUnexecutedScheduleExists){
			extractMaxDate(executedOnDate, "MAX_"+fields.get(1));
		}
	}
	
	/**
	 * 
	 * @return map containing START_DATE and END_DATE
	 * @throws RuntimeException if called before collect is invoked  
	 */
	public Map<String, Object> getDates(){
		if(!collectedCalled)
			throw new RuntimeException("No Data is available, Make sure you call search parser before calling this function");
		Map<String, Object> miscData = new HashMap<String, Object>();
		Date minExecutionDate = (Date) tempData.get(fields.get(1));
		if(minExecutionDate == null){
			miscData.put("START_DATE", tempData.get(fields.get(0)));
		}else{
			miscData.put("START_DATE", minExecutionDate);
		}
		
		if(isUnexecutedScheduleExists != null && isUnexecutedScheduleExists){
			miscData.put("END_DATE", new Date());
		}else{
			miscData.put("END_DATE", tempData.get("MAX_"+fields.get(1)));
		}
		return miscData;
	}

	private void extractMaxDate(String docDateString, String fieldName) {
		if(docDateString != null){
			Date docDate =  DateUtils.truncate(LuceneUtils.stringToDate(docDateString), Calendar.DATE);
			Date currDate = (Date) tempData.get(fieldName);
			if(currDate == null || (docDate.getTime() > currDate.getTime()) ){
				tempData.put(fieldName, docDate);
			}else{
				tempData.put(fieldName, currDate);
			}
		}
	}

	private void extractMinDate(String docDateString, String fieldName) {
		if(docDateString != null){
			Date docDate = DateUtils.truncate(LuceneUtils.stringToDate(docDateString), Calendar.DATE);
			Date currDate = (Date) tempData.get(fieldName);
			if(currDate == null || (docDate.getTime() < currDate.getTime())){
				tempData.put(fieldName, docDate);
			}
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase)
			throws IOException {
		this.docBase = docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
	
	 /**
     * Get Index Document by DocID
     * @param docId
     * @param fieldSelector2 
     * @return
     */
	private Document getDocument(final int docId) {
        try {
            return indexSearcher.doc(docId, fieldSelector);
        }
        catch (IOException e) {
        	log.warn("Error Retrieving Index:", e);
            return null;
        }
    }

}
