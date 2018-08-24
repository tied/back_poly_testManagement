package com.thed.zephyr.je.index;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.atlassian.util.concurrent.Timeout;
import com.thed.zephyr.je.index.Index.Result;

import org.apache.log4j.Logger;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Used to build a {@link Result} implementation that accumulates results from
 * other operations and awaits on them all.
 *
 * For operations that are complete it just aggregates their results.
 */

public class AccumulatingResultBuilder {
	private static final Logger log = Logger.getLogger(AccumulatingResultBuilder.class);
	private final Collection<Index.Result> inFlightResults = new LinkedBlockingQueue<Index.Result>();
	private int successesToDate = 0;
	private int failuresToDate = 0;
	private final Collection<Runnable> completionTasks = new LinkedList<Runnable>();

	public AccumulatingResultBuilder add(@Nonnull final Index.Result result)
	{
		notNull("result", result);
		if (result instanceof CompositeResult)
		{
			CompositeResult compositeResult = (CompositeResult) result;
			for (Result singleResult : compositeResult.getResults())
			{
				addInternal(singleResult);
			}
		}
		else
		{
			addInternal(result);
		}
		return this;
	}

	public AccumulatingResultBuilder addInternal(@Nonnull final Index.Result result)
	{
		checkCompleted();
		if (result.isDone())
		{
			collectResult(result);
		}
		else
		{
			inFlightResults.add(result);
		}
		return this;
	}

	public AccumulatingResultBuilder addCompletionTask(@Nonnull final Runnable runnable)
	{
		completionTasks.add(notNull("runnable", runnable));
		return this;
	}

	/**
	 * Keep the results list small, we don't want to waste too much ram with
	 * complete results.
	 */
	private void checkCompleted()
	{
		for (final Iterator<Index.Result> iterator = inFlightResults.iterator(); iterator.hasNext();)
		{
			final Index.Result result = iterator.next();
			if (result.isDone())
			{
				collectResult(result);
				iterator.remove();
			}
		}
	}

	private void collectResult(final Result result)
	{
		try
		{
			result.await();
			successesToDate++;
		}
		catch (RuntimeException e)
		{
			failuresToDate++;
			log.warn(e.getMessage());
		}
	}

	public Result toResult()
	{
		return new CompositeResult(inFlightResults, successesToDate, failuresToDate, completionTasks);
	}

	/**
	 * This class holds the actual result objects and aggregates them. Once a
	 * result has been awaited then it can be discarded.
	 */
	static class CompositeResult implements Result
	{
		private final Collection<Index.Result> results;
		private final Queue<Runnable> completionTasks;
		private int successes;
		private int failures;

		CompositeResult(final Collection<Result> inFlightResults, final int successes, final int failures, final Collection<Runnable> completionTasks)
		{
			this.successes = successes;
			this.failures = failures;
			this.results = new LinkedBlockingQueue<Index.Result>(inFlightResults);
			this.completionTasks = new LinkedList<Runnable>(completionTasks);
		}

		public void await()
		{
			for (final Iterator<Result> it = results.iterator(); it.hasNext();)
			{
				// all threads should await
				final Result result = it.next();
				try
				{
					result.await();
					successes++;
				}
				catch (RuntimeException e)
				{
					failures++;
					log.warn(e.getMessage());
				}
				// once run, they should be removed
				it.remove();
			}
			if (failures > 0)
			{
				throw new IndexingFailureException(failures);
			}
			complete();
		}

		private void complete()
		{
			while (!completionTasks.isEmpty())
			{
				// only one thread should run these tasks
				final Runnable task = completionTasks.poll();
				// /CLOVER:OFF
				if (task != null)
				{
					// /CLOVER:ON

					task.run();
				}
			}
		}

		public boolean await(final long time, final TimeUnit unit)
		{
			final Timeout timeout = Timeout.getNanosTimeout(time, unit);
			for (final Iterator<Result> it = results.iterator(); it.hasNext();)
			{
				// all threads should await
				final Result result = it.next();
				try
				{
					if (!result.await(timeout.getTime(), timeout.getUnit()))
					{
						return false;
					}
					successes++;
				}
				catch (RuntimeException e)
				{
					failures++;
					log.warn(e.getMessage());
				}
				// once run, they should be removed
				it.remove();
			}
			if (failures > 0)
			{
				throw new IndexingFailureException(failures);
			}
			complete();
			return true;
		}

		public boolean isDone()
		{
			for (final Index.Result result : results)
			{
				if (!result.isDone())
				{
					return false;
				}
			}
			return true;
		}

		Iterable<Result> getResults()
		{
			return results;
		}
	}
}
