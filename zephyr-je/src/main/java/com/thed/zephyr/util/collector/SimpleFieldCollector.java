package com.thed.zephyr.util.collector;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;

public abstract class SimpleFieldCollector extends DocumentHitCollector {
	protected String fieldName;
	protected Object value;

	public SimpleFieldCollector(IndexSearcher searcher, String fieldName) {
		super(searcher);
		this.fieldName = fieldName;
	}

	public abstract void doCollect(Document d);

	@Override
	public void collect(Document d) {
		doCollect(d);
	}

	public String getFieldName() {
		return fieldName;
	}

	public Object getValue() {
		return value;
	}
}
