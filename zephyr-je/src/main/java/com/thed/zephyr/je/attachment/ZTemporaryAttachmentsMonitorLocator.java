package com.thed.zephyr.je.attachment;


/**
 * Locator to abstract how we obtain the TemporaryAttachmentsMonitor.  Implementations should store one of these
 * monitors per user.
 *
 */
public interface ZTemporaryAttachmentsMonitorLocator
{
    /**
     * Returns the current temporary attachmentsMonitor.  Creates a new one if specified when none exists yet.
     *
     * @param create Should this call create a new monitor if none exists yet
     * @return The current monitor or null.
     */
    ZTemporaryAttachmentsMonitor get(boolean create);
}
