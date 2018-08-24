package com.thed.zephyr.je.index;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.atlassian.jira.index.Configuration;
import com.atlassian.jira.util.RuntimeIOException;
import com.thed.zephyr.je.index.Index.Manager;
import com.thed.zephyr.util.UnZipDir;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class ZFJDefaultManager implements Manager
{
    private final Configuration configuration;
    private final ZFJDefaultIndex.Engine actor;
    private final CloseableIndex index;

    ZFJDefaultManager(final @Nonnull Configuration configuration, 
    		final @Nonnull ZFJDefaultIndex.Engine actor, final @Nonnull CloseableIndex index)
    {
        this.configuration = notNull("configuration", configuration);
        this.actor = notNull("actor", actor);
        this.index = notNull("index", index);
    }

    public Index getIndex()
    {
        return index;
    }

    public int getNumDocs()
    {
        return getSearcher().getIndexReader().numDocs();
    }

    public IndexSearcher getSearcher()
    {
        return actor.getSearcher();
    }

    public void deleteIndexDirectory()
    {
        actor.clean();
    }

    public void close()
    {
        index.close();
    }

    public boolean isIndexCreated()
    {
        try
        {
            return IndexReader.indexExists(configuration.getDirectory());
        }
        catch (final IOException e)
        {
            ///CLOVER:OFF
            throw new RuntimeIOException(e);
            ///CLOVER:ON
        }
    }
    
    public void updateIndexWithNewFilesFromZip(String zipFilePath) throws IOException {
    	Directory destDirectory = configuration.getDirectory();
    	for(String name : destDirectory.listAll()) {
			try {
				destDirectory.deleteFile(name);
			} catch(Exception ex) {
			}
		}
        UnZipDir unZipDir = new UnZipDir(zipFilePath, destDirectory);
        unZipDir.unzip();
    }
    
    public int removeDuplicateSchedules(int dbCount) {
    	IndexReader indexReader = null;
        try {
        	indexReader = IndexReader.open(configuration.getDirectory(), false);
            int numDocs = indexReader.numDocs();
            Set<String> scheduleSet = new HashSet<>();
            if(numDocs > dbCount) {
        	   int cnt = 0;
        	   for(int i = numDocs - 1; i >= 0; i--) {
        		   String itemId = indexReader.document(i).get("schedule_id");
        	       if(!scheduleSet.contains(itemId)) {
        	    	   scheduleSet.add(itemId);        	    	   
        	       } else {
        	    	   cnt++;
        	    	   indexReader.deleteDocument(i);
        	       }
        	   }
        	   return cnt;
           }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(indexReader != null)
                	indexReader.close();
            } catch (Exception e) {
            }
        }
        return 0;
    }
}
