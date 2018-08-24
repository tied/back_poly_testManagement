package com.thed.zephyr.util;

import org.joda.time.DateTime;

/**
 * Provides access to GreenHopper build properties
 */
public interface BuildProperties {
	
    /**
     * Get the version of this build
     */
    public String getVersion();
    
    /**
     * Get the build date.
     */
    public DateTime getBuildDate();
    
    /**
     * Get the change set this build is made of.
     * Note that a + after the changeset indicates locally changed sources!
     * @return the changeset id
     */
    public String getChangeSet();
    
    /**
     * Get the commit date of the change set
     */
    public String getChangeSetDate();
}
