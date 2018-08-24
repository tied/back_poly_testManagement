package com.thed.zephyr.je.index.cluster;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.database.DatabaseConfig;
import com.atlassian.jira.config.database.DatabaseConfigurationManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.ofbiz.FieldMap;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.thed.zephyr.je.config.license.PluginUtils;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageStatus;
import com.thed.zephyr.je.model.cluster.ExecClusterMessage;
import com.thed.zephyr.je.service.impl.BaseManagerImpl;
import net.java.ao.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Responsible for storing and retrieving cluster messages in the underlying database.
 *
 */
public class ZFJClusterMessageStore extends BaseManagerImpl {
    private static final Logger log = Logger.getLogger(ZFJClusterMessageStore.class);

    // Fields
    private final DateTimeFormatterFactory dateTimeFormatterFactory;

    public ZFJClusterMessageStore(final ActiveObjects ao, DateTimeFormatterFactory dateTimeFormatterFactory) {
        super(checkNotNull(ao));
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
    }

    /**
     * Stores the message in the underlying database.
     *
     * @param message - the message to store in the underlying table
     */
    public ExecClusterMessage storeMessage(@Nonnull final ZFJClusterMessage message) {
        return storeFieldMap(getFields(message));
    }

    /**
     * Creates a Execution ClusterMessage and stores it in the underlying database
     *
     * @param sourceNode           the ID of the source node (required)
     * @param destinationNode      the ID of the destination node (required)
     * @param message              the message to send (required)
     * @param affectedExecutionIds the executionIds to index (optional). Comma separated executionIds to avoid row bubbling up
     * @param status               the status of the message
     */
    public ZFJClusterMessage createMessage(
            @Nonnull final String sourceNode, @Nonnull final String destinationNode, @Nonnull final String message, String affectedExecutionIds, @Nonnull final String status) {
        DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
        final String formattedDate = formatter.format(new Date());
        final Date creationDate = convertToDate(formattedDate, formatter);
        Map<String, Object> fieldMap = new HashMap<>();
        fieldMap.put("SOURCE_NODE", sourceNode);
        fieldMap.put("DESTINATION_NODE", destinationNode);
        fieldMap.put("MESSAGE", message);
        fieldMap.put("STATUS", status);
        fieldMap.put("CREATION_TIME", creationDate);
        if (StringUtils.isNotBlank(affectedExecutionIds)) {
            fieldMap.put("AFFECTED_EXECUTION_ID", affectedExecutionIds);
        }
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder().putAll(fieldMap);
        ImmutableMap<String, Object> fields = builder.build();
        ExecClusterMessage gv = storeFieldMap(fields);
        return fromExecCluster(gv);
    }

    /**
     * Retrieves the messages destined for the given node.  Returns messages
     * with either the specific node id, or destined for all, but they must
     * have been sent from another node - no listening to your own messages.
     *
     * @return a list
     */
    public List<ZFJClusterMessage> getMessages(final Node destinationNode, ZFJMessage.ZFJMessageType messageType) {
        log.debug("ZFJClusterMessage getMessages=" + destinationNode.getNodeId());

        if (!destinationNode.isClustered()) {
            return Collections.emptyList();
        }
        final String destinationNodeId = destinationNode.getNodeId();
        if (ao != null) {
            DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).getDatabaseConfiguration();
            String whereClause = new String();
            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres"))
                whereClause = " \"DESTINATION_NODE\" = ? and \"CLAIMED_NODE\" IS NULL and \"STATUS\" = 'NEW' and \"MESSAGE\" = ?";
            else
                whereClause = " DESTINATION_NODE = ? and CLAIMED_NODE IS NULL and STATUS='NEW' AND MESSAGE = ?";

            Query query = Query.select().where(whereClause, destinationNodeId,messageType.getMessageType());

            int totalMessage = ao.count(ExecClusterMessage.class, query);
            log.debug("Total Count of Records retrieved:" + totalMessage);
            int count = 0;
            int limit = 10000;
            List<ZFJClusterMessage> totalObjects = new ArrayList<>();
            while (count < totalMessage) {
                if ((totalMessage - count) < limit) {
                    limit = totalMessage - count;
                }
                ExecClusterMessage[] execClusterMessages = ao.find(ExecClusterMessage.class, Query.select().where(whereClause, destinationNodeId,messageType.getMessageType()).order("CREATION_TIME ASC").limit(limit));
                List<ZFJClusterMessage> zfjClusterMessages = Lists.transform(Arrays.asList(execClusterMessages), new Function<ExecClusterMessage, ZFJClusterMessage>() {
                    @Override
                    public ZFJClusterMessage apply(@Nullable final ExecClusterMessage input) {
                        return fromExecCluster(input);
                    }
                });
                totalObjects.addAll(zfjClusterMessages);
                count = count + execClusterMessages.length; //count = 30002,
                updateToWorkInProgress(execClusterMessages);
            }
            return totalObjects;
        }
        return new ArrayList<ZFJClusterMessage>(0);
    }
	
	public List<ZFJClusterMessage> getMessages(final Node destinationNode) {
        log.debug("ZFJClusterMessage getMessages=" + destinationNode.getNodeId());
        if (!destinationNode.isClustered()) {
            return Collections.emptyList();
        }
        final String destinationNodeId = destinationNode.getNodeId();
        if (ao != null) {
            DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).getDatabaseConfiguration();
            DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
            String formattedDate = formatter.format(new Date());
            Date messageTms = convertToDate(formattedDate, formatter);
            String whereClause = new String();
            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres"))
                whereClause = " \"DESTINATION_NODE\" = ? and \"CLAIMED_NODE\" IS NULL and \"STATUS\" = 'NEW'";
            else
                whereClause = " DESTINATION_NODE = ? and CLAIMED_NODE IS NULL and STATUS='NEW'";

            final int COUNT_LIMIT = 500;
            if(!checkAnyReindexSyncIsHappening(dbConfig.getDatabaseType(), destinationNodeId)) {//logic to avoid sync mismatch in case reindex all or reindex by project is happening.
            	Query query = Query.select().where(whereClause, destinationNodeId).order(" CREATION_TIME ASC "); //using create time to pick messages in order wise.
                query.setLimit(COUNT_LIMIT); //Fetching only 500 messages so that sync will happen very quickly
                ExecClusterMessage[] execClusterMessages = ao.find(ExecClusterMessage.class, query);
                if(execClusterMessages != null && execClusterMessages.length > 0) {
                	List<ZFJClusterMessage> zfjClusterMessages = Stream.of(execClusterMessages).map(execClusterMessage ->  {
                    	ZFJClusterMessage convertedClusterMessage = fromExecCluster(execClusterMessage);
                    	execClusterMessage.setClaimedNode(execClusterMessage.getDestinationNode());
                        execClusterMessage.setMessageTime(messageTms);
                        execClusterMessage.setStatus(ZFJMessageStatus.WORK_IN_PROGRESS.getMessageStatus());
                        execClusterMessage.save();
                    	return convertedClusterMessage;
                    }).collect(Collectors.toList());
                    return zfjClusterMessages;
                }
            }            
        }
        return new ArrayList<ZFJClusterMessage>(0);
    }
	
	private boolean checkAnyReindexSyncIsHappening(String dbType, String destinationNode) {
		String whereClause = new String();
		if (StringUtils.startsWithIgnoreCase(dbType, "postgres"))
            whereClause = " \"DESTINATION_NODE\" = ? and \"STATUS\" = 'WORK_IN_PROGRESS' and \"MESSAGE\" IN ('ReIndexAll', 'ReIndexByProject', 'SyncIndexByProject', 'SyncIndex')";
        else
            whereClause = " DESTINATION_NODE = ? and STATUS='WORK_IN_PROGRESS' and MESSAGE IN ('ReIndexAll', 'ReIndexByProject', 'SyncIndexByProject', 'SyncIndex')";
		Query query = Query.select().where(whereClause, destinationNode);
		ExecClusterMessage[] execClusterMessages = ao.find(ExecClusterMessage.class, query);
        if(execClusterMessages != null && execClusterMessages.length > 0) {
        	return true;
        }
		return false;
	}

    /**
     * Updates the Message to WIP once it reads of the queue
     * @param execClusterMessages
     */
    private void updateToWorkInProgress(ExecClusterMessage[] execClusterMessages) {
        Arrays.stream(execClusterMessages).forEach(execClusterMessage -> {
            DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
            String formattedDate = formatter.format(new Date());
            Date messageTms = convertToDate(formattedDate, formatter);
            execClusterMessage.setClaimedNode(execClusterMessage.getDestinationNode());
            execClusterMessage.setMessageTime(messageTms);
            execClusterMessage.setStatus(ZFJMessageStatus.WORK_IN_PROGRESS.getMessageStatus());
            execClusterMessage.save();
        });
    }

    /**
     * Delete message from DB
     * Do we need to delete the message from DB?
     */
    public void deleteMessage() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        log.info("Deleting Message Before Creation Date:"+cal.getTime());
        if (ao != null) {
            DatabaseConfig dbConfig = ((DatabaseConfigurationManager) ComponentAccessor.getComponent(DatabaseConfigurationManager.class)).getDatabaseConfiguration();

            if (StringUtils.startsWithIgnoreCase(dbConfig.getDatabaseType(), "postgres")) {
                ao.deleteWithSQL(ExecClusterMessage.class, "\"STATUS\" IN ('PROCESSED','WORK_IN_PROGRESS') AND \"MESSAGE_TIME\" < ? ", cal.getTime());
                ao.deleteWithSQL(ExecClusterMessage.class, "\"CREATION_TIME\" < ? ", cal.getTime());
            } else {
                ao.deleteWithSQL(ExecClusterMessage.class, "STATUS IN ('PROCESSED','WORK_IN_PROGRESS') AND MESSAGE_TIME < ? ", cal.getTime());
                ao.deleteWithSQL(ExecClusterMessage.class, "CREATION_TIME < ? ", cal.getTime());
            }
        }
    }

    /**
     * Update message with timestamp
     * @param clusterMessage
     */
    public void updateMessage(ZFJClusterMessage clusterMessage) {
        ExecClusterMessage execClusterMessage = getFromClusterMessage(clusterMessage);
        if (execClusterMessage != null) {
            execClusterMessage.setClaimedNode(clusterMessage.getDestinationNode());
            DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
            String formattedDate = formatter.format(new Date());
            Date messageTms = convertToDate(formattedDate, formatter);
            execClusterMessage.setMessageTime(messageTms);
            execClusterMessage.setStatus(ZFJMessageStatus.PROCESSED.getMessageStatus());
            execClusterMessage.save();
        }
    }


    /**
     * Update message with timestamp
     * @param clusterMessage
     */
    public void updateWIPMessage(ZFJClusterMessage clusterMessage) {
        ExecClusterMessage execClusterMessage = getFromClusterMessage(clusterMessage);
        if (execClusterMessage != null) {
            execClusterMessage.setClaimedNode(clusterMessage.getDestinationNode());
            DateTimeFormatter formatter = dateTimeFormatterFactory.formatter().forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
            String formattedDate = formatter.format(new Date());
            Date messageTms = convertToDate(formattedDate, formatter);
            execClusterMessage.setMessageTime(messageTms);
            execClusterMessage.setStatus(ZFJMessageStatus.WORK_IN_PROGRESS.getMessageStatus());
            execClusterMessage.save();
        }
    }

    /**
     * Create the AO object from the MessageCluster wrapper
     * @param clusterMessage
     * @return
     */
    private ExecClusterMessage getFromClusterMessage(ZFJClusterMessage clusterMessage) {
        ExecClusterMessage[] ExecClusterMessages = ao.find(ExecClusterMessage.class, Query.select().where("ID = ?", clusterMessage.getId()));
        if (ExecClusterMessages != null && ExecClusterMessages.length > 0) {
            return ExecClusterMessages[0];
        }
        return null;
    }

    /**
     * Retrieves Work IN Process messages less than 5 days old. This will ensure that we are cleaning up WIP data.
     * We will have a scheduler to perform a cleanup on messages either daily or weekly.
     * @param destinationNodeId
     * @return
     */
    private ExecClusterMessage[] getWIPMessage(String destinationNodeId) {
        Calendar cal = GregorianCalendar.getInstance();
        cal.set(Calendar.DATE, -2);
        List<Object> params = new ArrayList<Object>();
        params.add(destinationNodeId);
        params.add(cal.getTime());
        ExecClusterMessage[] ExecClusterMessages = ao.find(ExecClusterMessage.class, Query.select().where(" DESTINATION_NODE = ? and CLAIMED_NODE IS NULL and STATUS = 'WORK_IN_PROGRESS' and MESSAGE_TIME < ?", params.toArray()));
        if (ExecClusterMessages != null && ExecClusterMessages.length > 0) {
            return ExecClusterMessages;
        }
        return null;
    }

    private ZFJClusterMessage fromExecCluster(@Nonnull final ExecClusterMessage clusterMessage) {
        final ZFJMessage message = ZFJMessage.fromString(clusterMessage.getMessage());
        return new ZFJClusterMessage(Long.valueOf(clusterMessage.getID()), clusterMessage.getSourceNode(),
                clusterMessage.getDestinationNode(), message, clusterMessage.getAffectedExecutionId(), ZFJMessageStatus.valueOf(clusterMessage.getStatus()));
    }

    /**
     * Create message in the database.
     * @param fields
     * @return
     */
    private ExecClusterMessage storeFieldMap(@Nonnull Map<String, Object> fields) {
        return ao.create(ExecClusterMessage.class, fields);
    }

    /**
     * Get database fields for ExecClusterMessage
     * @param message
     * @return
     */
    private Map<String, Object> getFields(final ZFJClusterMessage message) {
        return new FieldMap("ID", message.getId())
                .add("SOURCE_NODE", message.getSourceNode())
                .add("DESTINATION_NODE", message.getDestinationNode())
                .add("MESSAGE", message.getMessage().getMessageType().getMessageType())
                .add("STATUS", message.getMessageStatus().getMessageStatus())
                .add("AFFECTED_EXECUTION_ID", message.getAffectedExecutionId());
    }

    /**
     * Format the date to timestamp
     * @param dateString
     * @param formatter
     * @return
     */
    private Date convertToDate(String dateString, DateTimeFormatter formatter) {
        if (!StringUtils.isBlank(dateString)) {
            Date date = formatter.parse(dateString);
            return date;
        }
        return null;
    }
}
