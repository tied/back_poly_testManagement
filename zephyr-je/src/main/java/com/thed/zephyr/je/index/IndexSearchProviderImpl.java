package com.thed.zephyr.je.index;

import org.apache.lucene.search.IndexSearcher;


public class IndexSearchProviderImpl implements IndexSearchProvider {
	private final ScheduleIndexManager scheduleIndexManager;
	
	public IndexSearchProviderImpl(ScheduleIndexManager scheduleIndexManager) {
		this.scheduleIndexManager=scheduleIndexManager;
	}
	
	@Override
	public IndexSearcher getSearcher() {
		return scheduleIndexManager.getScheduleSearcher();
	}
}
