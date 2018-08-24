package com.thed.zephyr.je.index.cluster;

import java.util.List;

import com.atlassian.util.concurrent.ResettableLazyReference;
import com.thed.zephyr.je.index.cluster.Node.NodeState;
import org.apache.commons.lang3.StringUtils;

/**
 * We will work on removing the reference to this class if we decide to use 6.1 or above JIRA
 *
 */
public class NodeStateManagerImpl implements NodeStateManager {
    private final ZFJClusterNodeStore zfjClusterNodeStore;
    private final ClusterProperties clusterProperties;

    public NodeStateManagerImpl(final ZFJClusterNodeStore zfjClusterNodeStore,final ClusterProperties clusterProperties)
    {
        this.zfjClusterNodeStore=zfjClusterNodeStore;
        this.clusterProperties=clusterProperties;
    }

    /** This is a reference to the actual node where this cluster instance is running. */
    private ResettableLazyReference<Node> nodeRef = new ResettableLazyReference<Node>()
    {
        @Override
        protected Node create() throws Exception
        {
            return initializeNode();
        }
    };

    
    private Node initializeNode() {
        String nodeId = clusterProperties.getNodeId();
        if (StringUtils.isBlank(nodeId)) {
            return Node.NOT_CLUSTERED;
        }

        // try to get the Node from the database
        Node node = zfjClusterNodeStore.getNode(nodeId);
        if (node == null) {
            node = new Node(nodeId, NodeState.ACTIVE);
            return node;
        }
        return node;
    }
    
    /**
     * If there is a cluster.properties that appears to be valid
     * @return  true if clustered
     */
    @Override
    public boolean isClustered()
    {
        return getNode().isClustered();
    }
    
    @Override
    public Node getNode()
    {
        return nodeRef.get();
    }
    
    @Override
    public List<Node> getAllNodes()
    {
        return zfjClusterNodeStore.getAllNodes();
    }

    @Override
    public List<Node> getAllActiveNodes()
    {
        return zfjClusterNodeStore.getAllActiveNodes();
    }

    @Override
    public Node current() {
        return nodeRef.get();
    }
}
