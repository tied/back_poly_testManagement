package com.thed.zephyr.je.index.cluster;

import javax.annotation.Nullable;
import static com.atlassian.jira.util.dbc.Assertions.notBlank;
import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static com.google.common.base.Objects.equal;
import com.google.common.base.Objects;

/**
 * Represents a node in the cluster
 *
 */
public class Node
{
    /**
     * Special node that is used when JIRA is not configured for clustering.
     */
    public static Node NOT_CLUSTERED = new Node();

    public enum NodeState {
        ACTIVE,
        PASSIVE,
        ACTIVATING,
        PASSIVATING,
        OFFLINE;
    }

    private final NodeState state;
    private final String nodeId;
    /**
     * This timestamp will reflect the last time the node changed its state.
     */
    private final Long timestamp;

    /**
     * This is the ip address of the node
     */
    private final String ip;

    /**
     * Port that the node will use to listen for changes
     */
    private final Long cacheListenerPort;

    public Node(final String nodeId, final NodeState state)
    {
        this.nodeId = nodeId;
        this.state = state;
        this.timestamp = null;
        this.ip = null;
        this.cacheListenerPort = null;
    }

    /**
     * Creates a representation of a node.
     *
     * @param nodeId            the node's ID, which must be neither {@code null} nor blank
     * @param state             the node's initial state, which must not be {@code null}
     * @param cacheListenerPort the port the node will listen for new changes
     * @throws IllegalArgumentException if the {@code nodeId} or {@code state} is not allowed
     */
    public Node(final String nodeId, final NodeState state, final Long timestamp, final String ip, final Long cacheListenerPort) {
        this.nodeId = notBlank("nodeId", nodeId);
        this.state = notNull("state", state);
        this.timestamp = timestamp;
        this.ip = ip;
        this.cacheListenerPort = cacheListenerPort;
    }

    // Special private constructor for the NOT_CLUSTERED instance's use, only
    private Node() {
        this.nodeId = null;
        this.state = NodeState.ACTIVE;
        this.timestamp = null;
        this.ip = null;
        this.cacheListenerPort = null;
    }

    public NodeState getState() {
        if (isClustered()) {
            return state;
        }
        return NodeState.ACTIVE;
    }

    @Nullable
    public String getNodeId()
    {
        return nodeId;
    }

    public boolean isClustered()
    {
        return nodeId != null;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getIp() {
        return ip;
    }

    public Long getCacheListenerPort() {
        return cacheListenerPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Node node = (Node) o;

        return equal(ip, node.ip)
                && equal(cacheListenerPort, node.cacheListenerPort)
                && equal(nodeId, node.nodeId)
                && equal(state, node.state)
                && equal(timestamp, node.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodeId, state, timestamp, ip, cacheListenerPort);
    }
}
