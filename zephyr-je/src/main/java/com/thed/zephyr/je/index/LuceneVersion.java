package com.thed.zephyr.je.index;

import org.apache.lucene.util.Version;

public class LuceneVersion
{
    private static Version value = Version.LUCENE_30;

    /**
     * Gets the value used by JIRA when it interacts with Apache Lucene classes.
     * @return A Version instance.
     */
    public static Version get()
    {
        return value;
    }
}