package com.thed.zephyr.je.index;

import org.apache.lucene.search.IndexSearcher;

public interface IndexSearchProvider {
	public IndexSearcher getSearcher();
}
