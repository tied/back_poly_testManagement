package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimePeriod;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.atlassian.jira.util.LuceneUtils;
import com.thed.zephyr.util.ApplicationConstants;

@NotThreadSafe
public class UnexecutedSchedulesCollector extends Collector {
    protected final Logger log = Logger.getLogger(UnexecutedSchedulesCollector.class);
    
    private final Map result;
	private StatisticsMapper<TimePeriod> statisticsMapper;
	
    private IndexSearcher indexSearcher;
    private int docBase = 0;
	private Collection<String> fields;
	private FieldSelector fieldSelector;

	private TimePeriod startDateTimePeriod;
	private TimePeriod endDateTimePeriod;

	private final Map countMap;

    @SuppressWarnings("rawtypes")
	public UnexecutedSchedulesCollector(//Collection<String> flds, 
										Map result, 
										IndexSearcher indexSearcher,
										StatisticsMapper<TimePeriod> statisticsMapper,
										Date startDate,
										Date endDate,
										Map countMap)
    {
        this.result = result;
        this.indexSearcher=indexSearcher;
        this.statisticsMapper = statisticsMapper;
        this.startDateTimePeriod = statisticsMapper.getValueFromLuceneField(LuceneUtils.dateToString(startDate));
        if(endDate == null){
        	endDate = new Date();
        }
        this.endDateTimePeriod = statisticsMapper.getValueFromLuceneField(LuceneUtils.dateToString(endDate));

        TimePeriod tp = startDateTimePeriod;
        while(tp.getStart().getTime() <= endDateTimePeriod.getStart().getTime()){
        	result.put(tp, 0);
        	tp = ((RegularTimePeriod)tp).next();
        }
        
        this.countMap = countMap;
        
        /*this.fieldSelector = new FieldSelector() {
			
			@Override
			public FieldSelectorResult accept(String fieldName) {
				if(fields.contains(fieldName))
					return FieldSelectorResult.LOAD;
				return FieldSelectorResult.NO_LOAD;
			}
		};*/
    }

    /**
     * Collect DocIds
     */
	public void collect(int i) {
		final Document scheduleDocument = getDocument(docBase + i);
		adjustMapForValues(scheduleDocument, result, countMap);
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		// Do nothing
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.docBase = docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
	
	private void adjustMapForValues(Document scheduleDocument, Map<TimePeriod, Integer> bucketMap, Map<String, Integer> countMap){
		String executedOnDateStr = scheduleDocument.get("EXECUTED_ON");
		String creationDateStr = scheduleDocument.get("DATE_CREATED");
		TimePeriod executedOnDateKey = null;
		TimePeriod creationDateKey;
		//Boolean isExecuted = !StringUtils.equals(executedOnDateStr, ApplicationConstants.NULL_VALUE);
		if(executedOnDateStr != null){
			executedOnDateKey = statisticsMapper.getValueFromLuceneField(executedOnDateStr);
			Integer execCount = countMap.get("ExecutionCount");
			countMap.put("ExecutionCount", ++execCount);
		}
		
		Integer creationCount = countMap.get("CreationCount");
		countMap.put("CreationCount", ++creationCount);
		creationDateKey = statisticsMapper.getValueFromLuceneField(creationDateStr);
		
		TimePeriod countBeginTimePeriod = startDateTimePeriod.getStart().getTime() > creationDateKey.getStart().getTime() ? startDateTimePeriod : creationDateKey;
		TimePeriod countEndTimePeriod = (executedOnDateKey != null) && (executedOnDateKey.getStart().getTime() <= endDateTimePeriod.getStart().getTime()) ? ((RegularTimePeriod)executedOnDateKey).previous() : endDateTimePeriod;
		log.debug("Creation - " + creationDateStr + " Execution - " + executedOnDateKey + " ,StartDate - " + startDateTimePeriod + " ,End Date - " + endDateTimePeriod);
		while(countBeginTimePeriod.getStart().getTime() <= countEndTimePeriod.getStart().getTime()){
			log.debug("CountPeriod - " + countBeginTimePeriod);
			Integer count = bucketMap.get(countBeginTimePeriod);
			if (count != null) 
				bucketMap.put(countBeginTimePeriod, ++count);
			else
				bucketMap.put(countBeginTimePeriod, 1);
			countBeginTimePeriod = ((RegularTimePeriod)countBeginTimePeriod).next();
		}
	}

	/*
	private void adjustMapForValues(Document scheduleDocument, Map<Date,Integer> bucketMap){
		
		//For each schedule we need to have creation date and execution date so that we can decide on buckets in which we need to add this schedule count.
		
		Date executionDate = getDateFromString(scheduleDocument.get("EXECUTED_ON"), dateFormat, endDate);

		//Creation date will never be null. Hence we don't need to send graph plotting for start date.
		Date creationDate = getDateFromString(scheduleDocument.get("DATE_CREATED"), dateFormat, null);
		
		//if schedule is created before the date range over which we are going to plot the graph, then we will add it to buckets starting from graph creation date (startDate)
		//Hence between graph plotting start date and schedule creation date, we will pick the date which is latest.
		Date fromDate = getLaterDate(startDate, creationDate);

		//if schedule is executed after the date range over which we are going to plot the graph, then we will add it to buckets till the graph display date (endDate)
		//Hence between endDate and schedule execution date, we will pick the date which is earliest.
		Date toDate = getEarlierDate(endDate, executionDate);

		log.debug("Schedule ID - " + scheduleDocument.get("schedule_id") + " Creation " + creationDate + " Execution "+ executionDate);
		log.debug("From Date " + fromDate + " To Date " + toDate);
		log.debug();
		
		
		while( fromDate.before(toDate) || (fromDate.equals(toDate))){
			addtoBucket(bucketMap, fromDate);
			cal.setTime(fromDate);
			cal.add(Calendar.DATE, 1);
			fromDate = cal.getTime();
		}
	}
	*/
	
	public void addtoBucket(Map<Date, Integer> bucketMapper, Date dateObj){
		Integer count = bucketMapper.get(dateObj);
		if (count != null) 
			bucketMapper.put(dateObj, ++count);
		else
			bucketMapper.put(dateObj, 1);
	}
	
	//It allows us to pick the date which is greater between given two dates.
	public Date getLaterDate(Date startDate, Date creationDate){
		if(startDate.after(creationDate))
			return startDate;
		else
			return creationDate;
	}
	
	//Allows us to pick the date which is earlier between given two dates.
	//Note that we want to count all dates till which schedule is in un-executed state.
	public Date getEarlierDate(Date endDate, Date executionDate){
		if(executionDate != null){
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(executionDate);

			//Schedule is in un-executed state till one day before it's actual execution date.
			cal.add(Calendar.DATE, -1);
			executionDate = cal.getTime();
			
			if(endDate.after(executionDate))
				return executionDate;
			else
				return endDate;
					
		}
	   return endDate;
	}
	
	//
	public Date getDateFromString(String inputDateString, String format, Date alternateDate){
		DateFormat dateFormat = new SimpleDateFormat(format);
		Date date = null;
		
		if(inputDateString == null || (inputDateString.length() < 8))
			return alternateDate;
		else
			inputDateString = inputDateString.substring(0, 8);
		
		try{
			date = dateFormat.parse(inputDateString);
		}
		catch(java.text.ParseException pe)
		{
			date = alternateDate;
		}
		
		return date;
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
        	log.warn("Error Retrieving Index:",e);
            return null;
        }
    }
}
