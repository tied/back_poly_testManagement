package com.thed.zephyr.je.index.cluster;

/**
 * Service to clean Exec Cluster messages
 *
 */
public interface ExecClusterIndexCleanService
{
    /**
     * deletes Messages
     */
    void deleteMessages();

}