package com.thed.zephyr.util.optionalservice;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/30/13
 * Time: 12:00 PM
 */

/**
 * Accessor interface to safely get a {@link Zapi} instance if one is available.
 */
public interface ServiceAccessor {
    Zapi getZapi();
    GHSprintService getSprintService();
}
