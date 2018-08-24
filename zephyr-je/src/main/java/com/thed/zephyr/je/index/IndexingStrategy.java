package com.thed.zephyr.je.index;

import com.atlassian.jira.util.Closeable;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.Supplier;
import com.thed.zephyr.je.index.Index.Result;

public interface IndexingStrategy extends Function<Supplier<Result>, Result>, Closeable
{}