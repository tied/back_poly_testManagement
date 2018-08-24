package com.thed.zephyr.util;

import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

/**
 * Holds the Zephyr build properties.
 * This class is an adaption of JIRA's com.atlassian.jira.util.BuildUtils class
 */

@Service
public class BuildPropertiesImpl implements BuildProperties
{
    private static final String UNPARSED_DATE = "2011-07-28T14:53:49.461+1000";
    private static final String VERSION = "5.7.1-SNAPSHOT";
    private static final String CHANGE_SET = "7df0b4a76a2e";
    private static final String CHANGE_SET_DATE = "2011-07-28 11:51 +1000";

	private static final DateTime PARSED_DATE;

    static
    {
    	PARSED_DATE = new DateTime(UNPARSED_DATE);
    }

    @Override
	public String getVersion()
    {
        return VERSION;
    }

    @Override
	public String getChangeSet()
    {
        return CHANGE_SET;
    }
    
    @Override
	public String getChangeSetDate()
    {
        return CHANGE_SET_DATE;
    }

    @Override
	public DateTime getBuildDate()
    {
        return PARSED_DATE;
    }
}