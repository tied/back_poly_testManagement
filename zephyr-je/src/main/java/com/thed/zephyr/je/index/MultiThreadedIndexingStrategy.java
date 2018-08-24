package com.thed.zephyr.je.index;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import com.atlassian.jira.index.MultiThreadedIndexingConfiguration;
import com.atlassian.jira.util.Supplier;
import com.atlassian.jira.util.concurrent.BoundedExecutor;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.thed.zephyr.je.index.Index.Result;

public class MultiThreadedIndexingStrategy implements IndexingStrategy
{
    private final BoundedExecutor executor;
    private final IndexingStrategy strategy;

    public MultiThreadedIndexingStrategy(@Nonnull final IndexingStrategy strategy, final MultiThreadedIndexingConfiguration config, String threadPrefix)
    {
        this.strategy = notNull("strategy", strategy);
        executor = new BoundedExecutor(Executors.newFixedThreadPool(config.noOfThreads(), ThreadFactories.namedThreadFactory(threadPrefix)),
            config.maximumQueueSize());
    }

    public Result get(final Supplier<Result> input)
    {
        return new FutureResult(executor.submit(new Callable<Index.Result>()
        {
            public Index.Result call() throws Exception
            {
                return strategy.get(input);
            }
        }));
    }

    public void close()
    {
        executor.shutdownAndWait();
    }
}