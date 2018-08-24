package com.thed.zephyr.je.index.cluster;

import java.util.List;


/**
 * Manage the state of nodes in the Cluster.
 *
 */
public interface NodeStateManager
{
    /**
     * Returns the current JIRA node.
     *
     * @return a non-null instance; call {@link com.thed.zephyr.je.index.cluster.Node#isClustered()}
     * to see if it's part of a cluster
     */
    Node getNode();
    
    /**
     * If there is a cluster.properties that appears to be valid
     * @return  true if clustered
     */
    boolean isClustered();
    
    /**
     * Returns all clustered nodes in database
     * @return
     */
    List<Node> getAllNodes();


    /**
     * Returns all clustered active nodes in database
     * @return
     */
    List<Node> getAllActiveNodes();


    Node current();
}
