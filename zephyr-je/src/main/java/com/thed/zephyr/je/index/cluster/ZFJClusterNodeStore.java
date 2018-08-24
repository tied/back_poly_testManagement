package com.thed.zephyr.je.index.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.core.entity.EntityCondition;
import org.ofbiz.core.entity.GenericValue;

import com.atlassian.jira.ofbiz.FieldMap;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class ZFJClusterNodeStore
{
    private static final String ENTITY = "ClusterNode";
    private static final String NODE_ID = "nodeId";
    private static final String NODE_STATE = "nodeState";
    private static final String TIMESTAMP = "timestamp";
    private static final String IP = "ip";
    private static final String CACHE_LISTENER_PORT = "cacheListenerPort";

    private final OfBizDelegator ofBizDelegator;

    public ZFJClusterNodeStore(final OfBizDelegator ofBizDelegator)
    {
        this.ofBizDelegator = ofBizDelegator;
    }

    public Node getNode(String nodeId)
    {
        Node node = null;
        GenericValue gv = ofBizDelegator.findByPrimaryKey(ENTITY, getPkFields(nodeId));
        if (gv != null)
        {
            node = fromGv(gv);
        }
        return node;
    }

    public List<Node> getAllNodes()
    {
        return findNodes(null, null);
    }

    public List<Node> findNodes(final EntityCondition condition, List<String> orderBy)
    {
        return Lists.transform(ofBizDelegator.findByCondition(ENTITY, condition, null, orderBy), new Function<GenericValue, Node>()
        {
            @Override
            public Node apply(@Nullable final GenericValue gv)
            {
                return fromGv(gv);
            }
        });
    }

    public List<Node> getAllActiveNodes() {
        List<Node> activeNodes = new ArrayList<>();
        List<Node> nodes = getAllNodes();
        if(nodes != null) {
            nodes.stream().forEach(node -> {
                if (StringUtils.equalsIgnoreCase(node.getState().toString(), Node.NodeState.ACTIVE.toString()) ||
                        StringUtils.equalsIgnoreCase(node.getState().toString(), Node.NodeState.ACTIVATING.toString())) {
                    activeNodes.add(node);
                }
            });
        }
        return activeNodes;
    }

    private Node fromGv(@Nonnull final GenericValue gv) {
        final Long multicastPort = gv.getLong(CACHE_LISTENER_PORT);
        final String ip = gv.getString(IP);
        final Long timestamp = gv.getLong(TIMESTAMP);
        return new Node(gv.getString(NODE_ID),
                Node.NodeState.valueOf(gv.getString(NODE_STATE)),
                timestamp,
                ip,
                multicastPort);
    }

    private Map<String, Object> getPkFields(final String nodeId)
    {
        return new FieldMap(NODE_ID, nodeId);
    }
}