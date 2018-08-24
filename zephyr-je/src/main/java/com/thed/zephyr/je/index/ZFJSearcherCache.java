package com.thed.zephyr.je.index;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import com.atlassian.jira.util.Supplier;

public class ZFJSearcherCache {
    private static final ThreadLocal<ZFJSearcherCache> THREAD_LOCAL = new ThreadLocal<ZFJSearcherCache>();

    public static ZFJSearcherCache getThreadLocalCache()
    {
    	ZFJSearcherCache threadLocalSearcherCache = THREAD_LOCAL.get();
        if (threadLocalSearcherCache == null)
        {
            threadLocalSearcherCache = new ZFJSearcherCache();
            THREAD_LOCAL.set(threadLocalSearcherCache);
        }
        return threadLocalSearcherCache;
    }

    private IndexSearcher zfjIndexSearcher;
    
    IndexSearcher retrieveZFJIndexSearcher(final Supplier<IndexSearcher> searcherSupplier)
    {
        if (zfjIndexSearcher == null)
        {
        	zfjIndexSearcher = searcherSupplier.get();
        }
        return zfjIndexSearcher;
    }
    
    
    IndexReader retrieveZFJIndexReader(final Supplier<IndexSearcher> searcherSupplier)
    {
        return retrieveZFJIndexSearcher(searcherSupplier).getIndexReader();
    }

    /**
     * Close the schedule searchers.
     *
     * @throws java.io.IOException if there's a lucene exception accessing the disk
     */
    public void closeSearchers() throws IOException {
        try {
            closeSearcher(zfjIndexSearcher);
        }
        finally {
        	zfjIndexSearcher = null;
        }
    }

    private void closeSearcher(final IndexSearcher searcher) throws IOException {
        if (searcher != null) {
            searcher.close();
        }
    }
}
