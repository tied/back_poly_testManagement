package com.thed.zephyr.je.index;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import com.atlassian.instrumentation.Counter;
import com.atlassian.jira.instrumentation.Instrumentation;
import com.atlassian.jira.instrumentation.InstrumentationName;
import com.atlassian.jira.util.Closeable;
import com.atlassian.jira.util.CompositeCloseable;
import com.atlassian.jira.util.RuntimeIOException;

import org.apache.lucene.search.IndexSearcher;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Implements search over a single IndexReader, but remains open even if close() is called. This way
 * it can be shared by multiple objects that need to search the index without being aware of the
 * keep-the-index-open-until-it-changes logic.
 * <p>
 * Like the {@link DelegateSearcher} it extends, this class uses fragile extension, we need to check
 * the super-classes whenever we up Lucene to make sure we override everything correctly.
 */
public class DelayCloseSearcher extends DelegateSearcher implements DelayCloseable
{
    private final DelayCloseable.Helper helper;
    private final IndexSearcher searcher;
    private final AtomicBoolean isOpen = new AtomicBoolean(false);

    DelayCloseSearcher(@Nonnull final IndexSearcher searcher)
    {
        super(notNull("searcher", searcher));
        this.searcher = searcher;
        helper = new DelayCloseable.Helper(new SearcherCloser(searcher));
    }

    DelayCloseSearcher(@Nonnull final IndexSearcher searcher, @Nonnull final Closeable closeAction)
    {
        super(notNull("searcher", searcher));
        this.searcher = searcher;
        helper = new DelayCloseable.Helper(new CompositeCloseable(closeAction, new SearcherCloser(searcher)));
    }

    public void closeWhenDone()
    {
        helper.closeWhenDone();
    }

    public boolean isClosed()
    {
        return helper.isClosed();
    }

    public void open()
    {
        helper.open();
        if (isOpen.compareAndSet(false, true))
        {
            // We don't want the reader to close immediately the searcher does, so that we can reopen it later.
            // So we must incRef it here.
            searcher.getIndexReader().incRef();
            Counter searcherLuceneOpenInstrument = Instrumentation.pullCounter(InstrumentationName.SEARCHER_LUCENE_OPEN);
            searcherLuceneOpenInstrument.incrementAndGet();
        }
    }

    //
    // IndexSearcher overrides
    //

    @Override
    public void close()
    {
        helper.close();
    }

    /**
     * Simple {@link Closeable} adaptor for a Searcher.
     */
    private static class SearcherCloser implements Closeable
    {
        private final IndexSearcher searcher;
        private Counter searcherLuceneCloseInstument = Instrumentation.pullCounter(InstrumentationName.SEARCHER_LUCENE_CLOSE);

        SearcherCloser(final IndexSearcher searcher)
        {
            this.searcher = searcher;
        }

        public void close()
        {
            try
            {
                searcher.close();
                // Decrement the ref count on the reader.  This will automatically close it when the ref count goes to zero.
                searcher.getIndexReader().decRef();
                searcherLuceneCloseInstument.incrementAndGet();
            }
            catch (final IOException e)
            {
                throw new RuntimeIOException(e);
            }
        }
    }
}
