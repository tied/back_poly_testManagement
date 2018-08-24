package com.thed.zephyr.je.index;

import com.atlassian.jira.index.Configuration;
import com.thed.zephyr.je.index.ZFJDefaultIndexEngine.FlushPolicy;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

import javax.annotation.Nonnull;

/**
 * Static factory class for creating {@link Index} and {@link Manager}
 * instances.
 *
 */
public class ZFJIndexes
{
    /**
     * Creates an index where the index operations are placed on a queue and the
     * actual work is done on a background thread. Any {@link Result} may be
     * waited on to make sure that subsequent searchers will see the result of
     * that update, but you can timeout on that without losing the update.
     *
     * @param name used to name the background thread.
     * @param config that holds the {@link Directory} and {@link Analyzer} used
     *            for indexing and searching.
     * @param maxQueueSize
     * @return a {@link Manager} that has an index configured for queued
     *         operations.
     */
    public static Index.Manager createQueuedIndexManager(final @Nonnull String name, final @Nonnull Configuration config, final long maxQueueSize)
    {
        // writePolicy is that the IndexWriter is committed after every write
        final ZFJDefaultIndexEngine engine = new ZFJDefaultIndexEngine(config, FlushPolicy.FLUSH);
        return new ZFJDefaultManager(config, engine, new ZFJQueueingIndex(name, new ZFJDefaultIndex(engine), maxQueueSize));
    }

    /**
     * Creates an index where the index operation work is done in the calling
     * thread. Any {@link Result} may be waited on but it will always be a
     * non-blocking operation as it will be complete already. There is no way to
     * timeout these operations.
     * <p>
     * The Index write policy is that flushes will only occur if a Searcher is
     * requested, when the IndexWriter decides to according to its internal
     * buffering policy, or when the index is closed.
     *
     * @param config that holds the {@link Directory} and {@link Analyzer} used
     *            for indexing and searching.
     * @return a {@link Manager} that has an index configured for direct
     *         operations.
     */
    public static Index.Manager createSimpleIndexManager(final @Nonnull Configuration config)
    {
        final ZFJDefaultIndexEngine engine = new ZFJDefaultIndexEngine(config, FlushPolicy.NONE);
        return new ZFJDefaultManager(config, engine, new ZFJDefaultIndex(engine));
    }

    /** do not ctor */
    // /CLOVER:OFF
    private ZFJIndexes()
    {
        throw new AssertionError("cannot instantiate");
    }
    // /CLOVER:ON
}

