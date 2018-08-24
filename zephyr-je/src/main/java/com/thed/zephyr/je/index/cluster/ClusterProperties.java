package com.thed.zephyr.je.index.cluster;

import javax.annotation.Nullable;

/**
 * This is used to lookup cluster properties from the underlying properties file.
 *
 */
public interface ClusterProperties
{
    /**
     *
     * @param key key for the property you want to look up
     * @return String value of the property, null if it does not exist
     */

    @Nullable
    String getProperty(String key);

    /**
     * Get the shared home for a clustered installation.
     * Will return null if no shared home is set.
     * @return
     */
    String getSharedHome();

    /**
     * Get the node id for a clustered installation.
     * Will return null if no node id is set.
     * @return
     */
    String getNodeId();
}
