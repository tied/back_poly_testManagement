package com.thed.zephyr.je.index;

import javax.annotation.Nonnull;

import com.atlassian.jira.index.Index.UpdateMode;
import com.atlassian.jira.util.RuntimeInterruptedException;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.atlassian.util.concurrent.SettableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Queueing {@link Index} implementation that takes all operations on the queue
 * and batches them to the underlying {@link Index delegate} on the task thread.
 * <p>
 * The created thread is interruptible and dies when interrupted, but will be
 * recreated if any new index jobs arrive. The initial task thread is not created
 * until the first indexing job arrives.
 */
public class ZFJQueueingIndex implements CloseableIndex
{
    private final BlockingQueue<FutureOperation> queue = new LinkedBlockingQueue<FutureOperation>();
    private final Task task = new Task(queue);
    private final AtomicSupplier<Thread> indexerThread = new AtomicSupplier<Thread>()
    {
        @Override
        protected Thread create()
        {
            final Thread thread = threadFactory.newThread(task);
            thread.start();
            return thread;
        }
    };

    private final CloseableIndex delegate;
    private final ThreadFactory threadFactory;

    ZFJQueueingIndex(@Nonnull final String name, @Nonnull final CloseableIndex delegate, final long maxQueueSize)
    {
        this(ThreadFactories.namedThreadFactory(notNull("name", name) + "-indexQueue"), delegate, maxQueueSize);
    }

    ZFJQueueingIndex(@Nonnull final ThreadFactory threadFactory, @Nonnull final CloseableIndex delegate, final long maxQueueSize)
    {
        this.threadFactory = notNull("threadFactory", threadFactory);
        this.delegate = notNull("delegate", delegate);
    }

    public Result perform(@Nonnull final Index.Operation operation)
    {
        final FutureOperation future = new FutureOperation(operation);
        queue.add(future);
        check();
        return new FutureResult(future);
    }

    public void close()
    {
        final Thread thread = indexerThread.get();
        try
        {
            while (thread.isAlive())
            {
                thread.interrupt();
                thread.join(100);
            }
        }
        catch (final InterruptedException e)
        {
            ///CLOVER:OFF
            throw new RuntimeInterruptedException(e);
            ///CLOVER:ON
        }
        finally
        {
            indexerThread.compareAndSetNull(thread);
            delegate.close();
        }
    }

    /**
     * Check that there is an indexer thread and it is running. In theory there is
     */
    private void check()
    {
        while (true)
        {
            final Thread thread = indexerThread.get();
            if (!thread.isAlive())
            {
                indexerThread.compareAndSetNull(thread);
            }
            else
            {
                return;
            }
        }
    }

    public class Task implements Runnable
    {
        private final BlockingQueue<FutureOperation> queue;

        Task(final BlockingQueue<FutureOperation> queue)
        {
            this.queue = notNull("queue", queue);
        }

        public void run()
        {
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    index();
                }
                catch (final InterruptedException e)
                {
                    break;
                }
            }
        }

        void index() throws InterruptedException
        {
            final List<FutureOperation> list = new ArrayList<FutureOperation>();
            list.add(queue.take());
            queue.drainTo(list);
            final CompositeOperation operation = new CompositeOperation(list);
            operation.set(delegate.perform(operation));
        }
    }

    /**
     * Class that is responsible for returning Result to the calling thread.
     * <p>
     * Calls to {@link FutureOperation#get()} will block until the reference is set
     */
    public static class FutureOperation extends SettableFuture<Index.Result>
    {
        private final com.thed.zephyr.je.index.Index.Operation operation;

        public FutureOperation(final com.thed.zephyr.je.index.Index.Operation operation)
        {
            this.operation = notNull("operation", operation);
        }

        public UpdateMode mode()
        {
            return operation.mode();
        }
    }

    public static class CompositeOperation extends Index.Operation
    {
        private final List<FutureOperation> operations;

        CompositeOperation(final List<FutureOperation> operations)
        {
            this.operations = Collections.unmodifiableList(operations);
        }

        public void set(final Result result)
        {
            for (final FutureOperation future : operations)
            {
                future.set(result);
            }
        }

        @Override
        public void perform(final Writer writer) throws IOException
        {
            for (final FutureOperation future : operations)
            {
                future.operation.perform(writer);
            }
        }

        @Override
        public UpdateMode mode()
        {
            //@TODO check size to simply return BATCH
            for (final FutureOperation future : operations)
            {
                if (future.mode() == UpdateMode.BATCH)
                {
                    return UpdateMode.BATCH;
                }
            }
            return UpdateMode.INTERACTIVE;
        }
    }
}
