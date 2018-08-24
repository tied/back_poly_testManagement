package com.thed.zephyr.je.index.cluster;

import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageStatus;

/**
 * Represents a message sent from a node
 *
 */
public class ZFJClusterMessage
{
    private final Long id;
    private final String sourceNode;
    private final String destinationNode;
    private final ZFJMessage message;
    private final String affectedExecutionId;
    private final ZFJMessageStatus messageStatus;

    public ZFJClusterMessage(final Long id, final String sourceNode,
    		final String destinationNode, final ZFJMessage message,String affectedExecutionId,ZFJMessageStatus messageStatus)
    {
        this.id = id;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.message = message;
        this.affectedExecutionId=affectedExecutionId;
        this.messageStatus=messageStatus;
    }
    
    public ZFJClusterMessage(final Long id, final String sourceNode,
    		final String destinationNode, final ZFJMessage message,ZFJMessageStatus messageStatus)
    {
        this.id = id;
        this.sourceNode = sourceNode;
        this.destinationNode = destinationNode;
        this.message = message;
        this.affectedExecutionId=null;
        this.messageStatus=messageStatus;
    }

    public Long getId()
    {
        return id;
    }

    public String getSourceNode()
    {
        return sourceNode;
    }

    public String getDestinationNode()
    {
        return destinationNode;
    }

    public ZFJMessage getMessage()
    {
        return message;
    }

	/**
	 * @return the affectedExecutionId
	 */
	public String getAffectedExecutionId() {
		return affectedExecutionId;
	}

	/**
	 * @return the messageStatus
	 */
	public ZFJMessageStatus getMessageStatus() {
		return messageStatus;
	}
}