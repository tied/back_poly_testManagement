package com.thed.zephyr.je.index.cluster;

import java.util.List;

import com.atlassian.jira.util.collect.EnclosedIterable;
import com.thed.zephyr.je.model.Schedule;

/**
 * Synchronously send and receive messages
 *
 */
public interface MessageHandler
{
	/**
	 * Sends any index/reIndex message to the queue for clustered Node to index.
	 * @param message
	 * @param schedules
	 * @return
	 */
    List<ZFJClusterMessage> sendMessage(ZFJMessage message,EnclosedIterable<Schedule> schedules, List<String> projectIds, boolean skipCurrentNode);
   
    /**
	 * Sends any deIndex(deletion) message to the queue for clustered Node to delete index.
     * @param message
     * @param scheduleIds
     * @param skipCurrentNode
     * @return
     */
    List<ZFJClusterMessage> sendDeletionMessage(ZFJMessage message, EnclosedIterable<String> scheduleIds, boolean skipCurrentNode);
    
    /**
     * Receives message for Nodes to perform indexing based on the Message type and destinationNode
     * @return
     */
    List<ZFJClusterMessage> receiveMessages(ZFJMessage.ZFJMessageType messageType);


    /**
     * Handles Incoming Messages
     */
    void handleReceivedMessages();

    
    /**
     * Starts the scheduler for Nodes to start polling for index messages.
     */
    void start();
    
    /**
     * Stops the index scheduler on undeploy event.
     */
    void stop();
	
	 /**
     * Receives message for Nodes to perform indexing. Fetches only 500 messages using order by creation time in ascending order.
     * @return
     */
    List<ZFJClusterMessage> receiveMessages();
    
    ZFJClusterMessage addCurrentNodeindexMessage(String message, String status);
    
    void updateMessageForCurrentNode(ZFJClusterMessage zfjClusterMessage);
}