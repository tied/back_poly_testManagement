package com.thed.zephyr.je.index.cluster;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * Represents the set of messages that can be sent in the execution cluster
 *
 */
public class ZFJMessage
{
    private final ZFJMessageType messageType;

    private ZFJMessage(ZFJMessageType messageType)
    {
        this.messageType = messageType;
    }

    public static ZFJMessage fromString(final String message)
    {
        return new ZFJMessage(ZFJMessageType.fromString(message));
    }

    public ZFJMessageType getMessageType()
    {
        return messageType;
    }

    @Override
    public String toString()
    {
        return serializeAsString();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        final ZFJMessage message = (ZFJMessage) o;
        return messageType == message.messageType;
    }

    @Override
    public int hashCode()
    {
        int result = messageType.hashCode();
        return result;
    }

    private String serializeAsString()
    {
        StringBuilder sb = new StringBuilder(messageType.getMessageType());
        return sb.toString();
    }

    public enum ZFJMessageType
    {
        INDEX_EXECUTION("Index Execution(s)"),
        DELETE_EXECUTION("Delete Execution(s)"),
        DELETE_CYCLE("Delete Cycle"),
        DELETE_FOLDER("Delete Folder"),
        DELETE_PROJECT("Delete Project"),
        REINDEX_BY_PROJECT("ReIndexByProject"),
        SYNC_INDEX_BY_PROJECT("SyncIndexByProject"),
    	RE_INDEX_ALL("ReIndexAll"),
        SYNC_INDEX_ALL("SyncIndex"),
    	DELETE_ISSUE("Delete Issue");


        private static Map<String, ZFJMessageType> messageTypeMap;

        private String messageType;

        private ZFJMessageType(String messageType)
        {
            this.messageType = messageType;
        }

        public String getMessageType()
        {
            return messageType;
        }

        private static ZFJMessageType fromString(String messageType)
        {
            if (messageTypeMap == null)
            {
                initialiseMessageMap();
            }
            return messageTypeMap.get(messageType);
        }

        private static void initialiseMessageMap()
        {
            messageTypeMap = Maps.newHashMap();
            for (ZFJMessageType messageType : values())
            {
                messageTypeMap.put(messageType.getMessageType(), messageType);
            }
        }
    }
    
    
    public enum ZFJMessageStatus
    {
        NEW("NEW"),
        WORK_IN_PROGRESS("WORK_IN_PROGRESS"),
    	PROCESSED("PROCESSED");

        private static Map<String, ZFJMessageStatus> messageStatusMap;

        private String messageStatus;

        private ZFJMessageStatus(String messageStatus)
        {
            this.messageStatus = messageStatus;
        }

        public String getMessageStatus()
        {
            return messageStatus;
        }

        private static ZFJMessageStatus fromString(String messageStatus)
        {
            if (messageStatusMap == null)
            {
                initialiseMessageMap();
            }
            return messageStatusMap.get(messageStatus);
        }

        private static void initialiseMessageMap()
        {
        	messageStatusMap = Maps.newHashMap();
            for (ZFJMessageStatus messageStatus : values())
            {
            	messageStatusMap.put(messageStatus.getMessageStatus(), messageStatus);
            }
        }
    }
}
