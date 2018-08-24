package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import com.atlassian.jira.issue.search.parameters.lucene.sort.JiraLuceneFieldFinder;
import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.JiraUtil;

@NotThreadSafe
public class ExecutionsByDurationCollector extends Collector {
    protected final Logger log = Logger.getLogger(ExecutionsByDurationCollector.class);
	private StatisticsMapper statisticsMapper;
    private final Map result;
    private final Map<Object, Map<String,Object>> statusByDuration;
    private Collection<String>[] docToTerms;
    private IndexSearcher indexSearcher;
    private int docBase = 0;

    @SuppressWarnings("rawtypes")
	public ExecutionsByDurationCollector(StatisticsMapper statisticsMapper, Map result, 
			IndexSearcher indexSearcher,Map<Object, Map<String,Object>> statusByDuration)
    {
        this.result = result;
        this.statisticsMapper = statisticsMapper;
        this.indexSearcher=indexSearcher;
        this.statusByDuration=statusByDuration;
        try
        {
            docToTerms = JiraLuceneFieldFinder.getInstance().getMatches(indexSearcher.getIndexReader(), statisticsMapper.getDocumentConstant());
        }
        catch (IOException e){
            log.info("Error Retrieving DocTerms.Ignoring",e);
        }
    }

    /**
     * Collect DocIds
     */
	public void collect(int i) {
		final Document scheduleDocument = getDocument(docBase + i);
		adjustMapForValues(scheduleDocument, result, docToTerms[docBase + i],
				statusByDuration);
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
		// Do nothing
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

	private void adjustMapForValues(Document scheduleDocument,
			Map<Object, Integer> map, Collection<String> terms,
			Map<Object, Map<String, Object>> statusByDurationMap) {
		if (terms == null) {
			return;
		}
		for (String term : terms) {
			Object object = statisticsMapper.getValueFromLuceneField(term);
			Integer count = map.get(object);

			if (count == null) {
				count = 0;
			}
			map.put(object, count + 1);
			String statusName = scheduleDocument.get("STATUS");

			if (!statusByDurationMap.containsKey(object)) {
				for (ExecutionStatus executionStatus : JiraUtil.getExecutionStatuses().values()) {
					populateDefault(statusByDurationMap, object,executionStatus);// 1-0,2-0,3-0
				}
			}

			for (ExecutionStatus executionStatus : JiraUtil.getExecutionStatuses().values()) {
				if (StringUtils.equalsIgnoreCase(statusName, executionStatus.getId().toString())) {

					// Populate statusDurationMap and use total from the above
					if (statusByDurationMap.containsKey(object)) {
						Map<String, Object> statusMap = statusByDurationMap.get(object);
						Integer cnt = (Integer) statusMap.get(executionStatus.getId().toString());
						cnt = cnt == null ? 0 : cnt;
						if(StringUtils.equalsIgnoreCase(executionStatus.getId().toString(), "-1")) {
							Integer statCount = statusMap.get("unexecuted") != null ? (Integer)statusMap.get("unexecuted") : new Integer(0);
							statusMap.put("unexecuted",statCount + 1);
						} else {
							Integer statCount = statusMap.get("executed") != null ? (Integer)statusMap.get("executed") : new Integer(0);
							statusMap.put("executed",statCount + 1);
						}
						statusMap.put(executionStatus.getId().toString(), cnt + 1);
						statusByDurationMap.put(object, statusMap);
					}
				}
			}
		}
	}
	
    
    /**
     * Defaults The values to execStatusId:0 and than as and how the hits are encountered, the count will be changed
     * @param statusByDurationMap
     * @param object
     * @param executionStatus
     */
    private void populateDefault(
			Map<Object, Map<String, Object>> statusByDurationMap, Object object, ExecutionStatus executionStatus) {
    	Map<String,Object> statusMap = statusByDurationMap.get(object);
    	if(statusMap == null) {
    		statusMap =  new HashMap<String, Object>();
			statusMap.put("executed",new Integer(0));
			statusMap.put("unexecuted",new Integer(0));
    	}
		statusMap.put(executionStatus.getId().toString(),new Integer(0));
		statusByDurationMap.put(object,statusMap);
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
