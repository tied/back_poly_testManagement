package com.thed.zephyr.util.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.NotThreadSafe;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Scorer;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;

@NotThreadSafe
public class ScheduleDefectHitCollector extends Collector {
    protected final Logger log = Logger.getLogger(ScheduleDefectHitCollector.class);

	private final Map<Integer,Integer> result;
	private final Map<Integer,List<String>> testIds;
	private IndexSearcher indexSearcher;
    private int docBase = 0;

	
	public ScheduleDefectHitCollector(Map<Integer,Integer> result,Map<Integer,List<String>> testIds,IndexSearcher searcher) {
        this.indexSearcher=searcher;
		this.result=result;
		this.testIds=testIds;
	}

	public void collect(int i) {
		final Document scheduleDocument = getDocument(docBase + i);
		adjustMapForValues(scheduleDocument,result);
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
	private void adjustMapForValues(Document doc,Map<Integer, Integer> result2) {
		Fieldable[] fields = doc.getFieldables("SCHEDULE_DEFECT_ID");
		
		for(Fieldable field : fields) {
			String defect = field.stringValue();
			if(StringUtils.isNotBlank(defect) && !StringUtils.equalsIgnoreCase(defect, "-1")) {
				Integer defectId = Integer.valueOf(defect);
				if (result.containsKey(defectId)) {
					Integer cnt = result.get(defectId);
					result.put(defectId, cnt+1);
				} else {
					result.put(defectId, new Integer(1));
				}
				
				// Getting IssueIds for IssueNavigator to redirect to,
				String issueId = doc.get("ISSUE_ID");
				if(testIds.containsKey(defectId)) {
					List<String> issueList = testIds.get(defectId);
					if(!issueList.contains(issueId)) {
						issueList.add(issueId);
						testIds.put(defectId, issueList);
					}
				} else {
					List<String> issueList = new ArrayList<String>();
					issueList.add(issueId);
					testIds.put(defectId, issueList);
				}
			}
		}
	}
}
