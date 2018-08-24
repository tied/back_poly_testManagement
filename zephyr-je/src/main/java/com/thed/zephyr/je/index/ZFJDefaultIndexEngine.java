package com.thed.zephyr.je.index;

import java.io.IOException;

import org.apache.lucene.index.IndexWriter;
import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.atlassian.jira.index.Configuration;
import com.atlassian.jira.index.Index.UpdateMode;
import com.atlassian.jira.util.Closeable;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.RuntimeIOException;
import com.atlassian.jira.util.Supplier;
import com.atlassian.util.concurrent.LazyReference;
import com.thed.zephyr.je.index.DelayCloseable.AlreadyClosedException;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

import net.jcip.annotations.ThreadSafe;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Thread-safe container that manages our current {@link IndexSearcher} and {@link Writer}.
 * <p>
 * Gets passed searcher and writer factories that create new instances of these when required.
 */
@ThreadSafe
public class ZFJDefaultIndexEngine implements ZFJDefaultIndex.Engine
{
    private static final Logger log = Logger.getLogger(ZFJDefaultIndexEngine.class);

    /**
     * How to perform an actual write to the writer.
     */
    static enum FlushPolicy
    {
        /**
         * Do not flush or close.
         */
        NONE()
        {
            @Override
            void commit(final WriterReference writer)
            {}
        },
        /**
         * Commit the writer's pending updates, do not close.
         */
        FLUSH()
        {
            @Override
            void commit(final WriterReference writer)
            {
                writer.commit();
            }
        },

        /**
         * Close the writer after performing the write.
         */
        CLOSE()
        {
            @Override
            synchronized void commit(final WriterReference writer)
            {
                writer.close();
            }
        };

        void perform(final com.thed.zephyr.je.index.Index.Operation operation, final WriterReference writer) throws IOException
        {
            try
            {
                operation.perform(writer.get(operation.mode()));
            }
            finally
            {
                commit(writer);
            }
        }

        abstract void commit(final WriterReference writer);
    }

    private final WriterReference writerReference;
    private final SearcherFactory searcherFactory;
    private final SearcherReference searcherReference;
    private final FlushPolicy writePolicy;
    private final Configuration configuration;

    /**
     * Production ctor.
     *
     * @param configuration the {@link Directory} and {@link Analyzer}
     * @param writePolicy when to flush writes
     */
    ZFJDefaultIndexEngine(final @Nonnull Configuration configuration, final @Nonnull FlushPolicy writePolicy)
    {
        this(new SearcherFactoryImpl(configuration), null, configuration, writePolicy);
    }

    /**
     * Main ctor.
     *
     * @param searcherFactory for creating {@link IndexSearcher searchers}
     * @param writerFactory for creating Writer instances of the correct mode
     * @param configuration the {@link Directory} and {@link Analyzer}
     * @param writePolicy when to flush writes
     */
    ZFJDefaultIndexEngine(final @Nonnull SearcherFactory searcherFactory, @Nullable final Function<UpdateMode, Writer> writerFactory, final @Nonnull Configuration configuration, final @Nonnull FlushPolicy writePolicy)
    {
        this.writePolicy = notNull("writePolicy", writePolicy);
        this.configuration = notNull("configuration", configuration);
        this.searcherFactory = notNull("searcherFactory", searcherFactory);
        this.searcherReference = new SearcherReference(searcherFactory);
        this.writerReference = new WriterReference(writerFactory == null ? new DefaultWriterFactory() : writerFactory);
    }

    /**
     * leak a {@link IndexSearcher}. Must get closed after usage.
     */
    @Nonnull
    public IndexSearcher getSearcher()
    {
        // mode is irrelevant to a Searcher
        return searcherReference.get(UpdateMode.INTERACTIVE);
    }

    public void clean()
    {
        close();
        try
        {
            IndexWriterConfig luceneConfig = new IndexWriterConfig(LuceneVersion.get(), configuration.getAnalyzer());
            luceneConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            new IndexWriter(configuration.getDirectory(), luceneConfig).close();
        }
        catch (final IOException e)
        {
            throw new RuntimeIOException(e);
        }
    }

    public void write(@Nonnull final com.thed.zephyr.je.index.Index.Operation operation) throws IOException
    {
        try
        {
            writePolicy.perform(operation, writerReference);
        }
        finally
        {
            searcherReference.close();
        }
    }

    public void close()
    {
        writerReference.close();
        searcherReference.close();
        searcherFactory.release();  // JRA-29587
    }

    /**
     * Thread-safe holder of the current Searcher
     */
    @ThreadSafe
    private class SearcherReference extends ReferenceHolder<DelayCloseSearcher>
    {
        private final SearcherFactory searcherSupplier;

        SearcherReference(@Nonnull final SearcherFactory searcherSupplier)
        {
            this.searcherSupplier = notNull("searcherSupplier", searcherSupplier);
        }

        @Override
        DelayCloseSearcher doCreate(final UpdateMode mode)
        {
            // To create a valid searcher, we need a valid writer.
            // Getting the writer reference, here ensures that.
            writerReference.get(mode);
            writePolicy.commit(writerReference);
            return new DelayCloseSearcher(searcherSupplier.get());
        }

        @Override
        DelayCloseSearcher open(final DelayCloseSearcher searcher)
        {
            searcher.open();
            return searcher;
        }

        @Override
        void doClose(final DelayCloseSearcher searcher)
        {
            searcher.closeWhenDone();
        }
    }

    /**
     * Thread-safe holder of the current Writer
     */
    @ThreadSafe
    private static class WriterReference extends ReferenceHolder<Writer>
    {
        private final Function<UpdateMode, Writer> writerFactory;

        WriterReference(@Nonnull final Function<UpdateMode, Writer> writerFactory)
        {
            this.writerFactory = notNull("writerFactory", writerFactory);
        }

        public void commit()
        {
            if (!isNull())
            {
                get().commit();
            }
        }

        @Override
        Writer doCreate(final UpdateMode mode)
        {
            return writerFactory.get(mode);
        }

        @Override
        void doClose(final Writer writer)
        {
            writer.close();
        }

        @Override
        Writer open(final Writer writer)
        {
            return writer;
        }

    }

    private class DefaultWriterFactory implements Function<UpdateMode, Writer>
    {
        @Override
        public Writer get(UpdateMode mode)
        {
            // be default, create a writer wrapper that has access to this engine's searcher
            return new WriterWrapper(configuration, mode, new Supplier<IndexSearcher>() {
                @Override
                public IndexSearcher get()
                {
                    return getSearcher();
                }
            });
        }
    }

    static abstract class ReferenceHolder<T> implements Function<UpdateMode, T>, Closeable
    {
        private final AtomicReference<LazyReference<T>> reference = new AtomicReference<LazyReference<T>>();

        public final void close()
        {
            final Reference<T> supplier = reference.getAndSet(null);
            if (supplier != null)
            {
                try
                {
                    doClose(supplier.get());
                }
                catch (final RuntimeException ignore)
                {}
            }
        }

        abstract void doClose(T element);

        public final T get(final UpdateMode mode)
        {
            while (true)
            {
                Reference<T> ref = reference.get();
                while (ref == null)
                {
                    reference.compareAndSet(null, new LazyReference<T>()
                    {
                        @Override
                        protected T create()
                        {
                            return doCreate(mode);
                        }
                    });
                    ref = reference.get();
                }
                try
                {
                    return open(ref.get());
                }
                catch (final AlreadyClosedException ignore)
                {}
                // in the rare case of a race condition, try again
            }
        }

        abstract T doCreate(UpdateMode mode);

        abstract T open(T element);

        final boolean isNull()
        {
            return reference.get() == null;
        }

        final T get()
        {
            final LazyReference<T> lazyReference = reference.get();
            return (lazyReference == null) ? null : lazyReference.get();
        }

        final void setNull()
        {
            reference.set(null);
        }
    }

    static interface SearcherFactory extends Supplier<IndexSearcher>
    {
        void release();
    }

    static class SearcherFactoryImpl implements SearcherFactory
    {
        private final Configuration configuration;
        /* This is already held in the thread safe SearcherReference. */
        private volatile IndexReader oldReader = null;

        SearcherFactoryImpl(final Configuration configuration)
        {
            this.configuration = notNull("configuration", configuration);
        }

        public IndexSearcher get()
        {
            try
            {
                IndexReader reader;
                if (oldReader != null)
                {
                    try
                    {
                        reader = oldReader.reopen(true);
                        // If we actually get a new reader, we must close the old one
                        if (reader != oldReader)
                        {
                            // This will really close only when the ref count goes to zero.
                            try
                            {
                                oldReader.close();
                            }
                            catch (org.apache.lucene.store.AlreadyClosedException ignore)
                            {
                                log.debug("Tried to close an already closed reader.");
                            }
                        }
                    }
                    catch (org.apache.lucene.store.AlreadyClosedException ignore)
                    {
                        // JRADEV-7825: Really this shouldn't happen unless someone closes the reader from outside all
                        // the inscrutable code in this class (and its friends) but
                        // don't worry, we will just open a new one in that case.
                        log.warn("Tried to reopen the IndexReader, but it threw AlreadyClosedException. Opening a fresh IndexReader.");
                        reader = IndexReader.open(configuration.getDirectory(), true);
                    }
                }
                else
                {
                    reader = IndexReader.open(configuration.getDirectory(), true);
                }
                oldReader = reader;
                return new IndexSearcher(reader);
            }
            catch (final IOException e)
            {
                ///CLOVER:OFF
                throw new RuntimeIOException(e);
                ///CLOVER:ON
            }
        }

        public void release()
        {
            final IndexReader reader = oldReader;
            if (reader != null)
            {
                try
                {
                    reader.close();
                    oldReader = null;
                }
                catch (org.apache.lucene.store.AlreadyClosedException ignore)
                {
                    // Ignore
                }
                catch (IOException e)
                {
                    ///CLOVER:OFF
                    throw new RuntimeException(e);
                    ///CLOVER:ON
                }
            }
        }
    }
}
