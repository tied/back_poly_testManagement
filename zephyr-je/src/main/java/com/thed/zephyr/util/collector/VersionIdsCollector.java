package com.thed.zephyr.util.collector;

import com.atlassian.jira.issue.statistics.StatisticsMapper;
import com.thed.zephyr.je.config.model.ExecutionStatus;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.util.*;

@NotThreadSafe
public class VersionIdsCollector extends Collector {
    protected final Logger log = Logger.getLogger(VersionIdsCollector.class);
    private final Set<Integer> result;
    private IndexSearcher indexSearcher;
    private int docBase = 0;
	private final List<String> fields = Arrays.asList(new String[]{ApplicationConstants.VERSION_ID});
	private FieldSelector fieldSelector;

	@SuppressWarnings("rawtypes")
	public VersionIdsCollector(Set<Integer> result, IndexSearcher indexSearcher)
    {
        this.result = result;
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

    /**
     * Collect DocIds
     */
	public void collect(int i) {
		final Document scheduleDocument = getDocument(docBase + i);
		adjustMapForValues(scheduleDocument, result);
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

	private void adjustMapForValues(Document scheduleDocument, Set<Integer> map) {

		if(scheduleDocument.get("VERSION_ID") != null) {
			Integer versionId = new Integer(scheduleDocument.get("VERSION_ID"));
			map.add(versionId);
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
