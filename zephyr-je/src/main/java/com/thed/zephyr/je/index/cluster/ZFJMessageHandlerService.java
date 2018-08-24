/**
 * 
 */
package com.thed.zephyr.je.index.cluster;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.task.context.Contexts;
import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.util.collect.CollectionEnclosedIterable;
import com.atlassian.jira.util.collect.EnclosedIterable;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.thed.zephyr.je.index.ScheduleIdsScheduleIterable;
import com.thed.zephyr.je.index.ScheduleIndexManager;
import com.thed.zephyr.je.index.ScheduleIndexer;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageStatus;
import com.thed.zephyr.je.index.cluster.ZFJMessage.ZFJMessageType;
import com.thed.zephyr.je.model.ReindexJobProgress;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.util.ApplicationConstants;

import org.apache.log4j.Logger;
import org.apache.lucene.search.IndexSearcher;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;

/**
 * Polls the database on a regular interval to see if there are index related actions to perform
 *
 */
public class ZFJMessageHandlerService implements MessageHandler
{
    // Constants
    private static final int INITIAL_DELAY = 3;
    private static final int PERIOD = 3;
    private static final Logger log = Logger.getLogger(ZFJMessageHandlerService.class);

    // Fields
    private final ScheduleIndexManager scheduleIndexManager;
    private final ScheduleManager scheduleManager;
    private final ScheduleIndexer scheduleIndexer;
    private final ZFJClusterMessageStore clusterMessageStore;
    private final ScheduledExecutorService scheduler;
    private final NodeStateManager nodeStateManager;
    private final ScheduleResourceDelegate scheduleResourceDelegate;

       @Nullable
    private volatile ScheduledFuture<?> messageHandlerService;

    private final Runnable handler = new Runnable()
    {
        public void run()
        {
        	log.debug("********** HANDLE RECEIVE MESSAGE ***********************");
            handleReceivedMessages();
        }
    };

    public ZFJMessageHandlerService(final ZFJClusterMessageStore clusterMessageStore,
    		final NodeStateManager nodeStateManager,final ScheduleManager scheduleManager,final ScheduleIndexManager scheduleIndexManager,
    		final ScheduleIndexer scheduleIndexer, final ScheduleResourceDelegate scheduleResourceDelegate)
    {
        this.clusterMessageStore = clusterMessageStore;
        this.scheduleIndexManager = scheduleIndexManager;
        this.nodeStateManager=nodeStateManager;
        this.scheduleManager=scheduleManager;
        this.scheduleIndexer=scheduleIndexer;
        scheduler = Executors.newScheduledThreadPool(1, ThreadFactories.namedThreadFactory("ZFJClusterMessageHandlerServiceThread"));
        this.scheduleResourceDelegate = scheduleResourceDelegate;
    }

    @Override
    @Nullable
    public List<ZFJClusterMessage> sendMessage(final ZFJMessage message,EnclosedIterable<Schedule> schedules, List<String> projectIds, boolean skipCurrentNode)
    {
        final List<ZFJClusterMessage> clusterMessages = new ArrayList<ZFJClusterMessage>();
        if (getCurrentNode().isClustered()) {
    		//Zip and compress the indexed schedule folder to copy and extract to other nodes.
            try { //This should happen only in case of datacenter scenario.
            	if(ZFJMessageType.RE_INDEX_ALL.getMessageType().equals(message.getMessageType().getMessageType())
            			|| ZFJMessageType.SYNC_INDEX_ALL.getMessageType().equals(message.getMessageType().getMessageType())) {
            		scheduleIndexManager.zipScheduleDirectory(ApplicationConstants.REINDEX_ALL_ZIP_FILE_NAME);
            	} else if(ZFJMessageType.REINDEX_BY_PROJECT.getMessageType().equals(message.getMessageType().getMessageType()) 
            			|| ZFJMessageType.SYNC_INDEX_BY_PROJECT.getMessageType().equals(message.getMessageType().getMessageType())) {
            		scheduleIndexManager.zipScheduleDirectory(ApplicationConstants.REINDEX_BY_PROJECT_FILE_NAME);
            	}            	
			} catch (Exception e) {
				log.error("Error occurred while zipping the schedule index folder -> ", e);
				throw new RuntimeException(e.getMessage(), e);
			}
            final String sourceId = getCurrentNode().getNodeId();
            for(final Node node : nodeStateManager.getAllActiveNodes()) {
                switch (message.getMessageType())  {
                case INDEX_EXECUTION:
	            	if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
	            		final List<String> affectedExecutionIds = new ArrayList<String>();
	                    schedules.foreach(new Consumer<Schedule>() {
                            @Override
                            public void consume(@Nonnull Schedule schedule)
                            {
                            	affectedExecutionIds.add(String.valueOf(schedule.getID()));
                            }
	                    });
	            		if(affectedExecutionIds.size() > 0) {
	                    	 final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(), message.toString(),StringUtils.join(affectedExecutionIds,","),ZFJMessageStatus.NEW.getMessageStatus());
	                    	 clusterMessages.add(clusterMessage);
	            		}
            		}
	            	break;
				case RE_INDEX_ALL:
					if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId())) {
                    	 final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(), message.toString(),null,ZFJMessageStatus.NEW.getMessageStatus());
                    	 clusterMessages.add(clusterMessage);
            		}
					break;
                case SYNC_INDEX_ALL:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId())) {
                        final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(), message.toString(),null,ZFJMessageStatus.NEW.getMessageStatus());
                        clusterMessages.add(clusterMessage);
                    }
                    break;
                case REINDEX_BY_PROJECT:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId())) {
                        log.info("REINDEX_BY_PROJECT");
                        final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(), message.toString(),StringUtils.join(projectIds,","),ZFJMessageStatus.NEW.getMessageStatus());
                        clusterMessages.add(clusterMessage);
                    }
                    break;
                case SYNC_INDEX_BY_PROJECT:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId())) {
                        log.info("REINDEX_BY_PROJECT");
                        final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(), message.toString(),StringUtils.join(projectIds,","),ZFJMessageStatus.NEW.getMessageStatus());
                        clusterMessages.add(clusterMessage);
                    }
                    break;
				default:
					break;
            	}
            }
        }
        return clusterMessages;
    }

    @Override
    @Nullable
    public List<ZFJClusterMessage> sendDeletionMessage(final ZFJMessage message, EnclosedIterable<String> affectedExecutionIds, boolean skipCurrentNode)
    {
        final List<ZFJClusterMessage> clusterMessages = new ArrayList<>();
        if (getCurrentNode().isClustered())
        {
            final String sourceId = getCurrentNode().getNodeId();
            for(final Node node : nodeStateManager.getAllActiveNodes()) {
                switch (message.getMessageType())  {
                case DELETE_EXECUTION:
	            	if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
	            		if(affectedExecutionIds != null && affectedExecutionIds.size() > 0) {
	                    	 final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(),
	                    			 message.toString(),StringUtils.join(EnclosedIterable.Functions.toList(affectedExecutionIds),","),ZFJMessageStatus.NEW.getMessageStatus());
	                    	 clusterMessages.add(clusterMessage);
	            		}
            		}
	            	break;
                case DELETE_CYCLE:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
                        if(affectedExecutionIds != null && affectedExecutionIds.size() > 0) {
                            final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(),
                                    message.toString(),StringUtils.join(EnclosedIterable.Functions.toList(affectedExecutionIds),","),ZFJMessageStatus.NEW.getMessageStatus());
                            clusterMessages.add(clusterMessage);
                        }
                    }
                    break;
                case DELETE_FOLDER:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
                        if(affectedExecutionIds != null && affectedExecutionIds.size() > 0) {
                            final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(),
                                    message.toString(),StringUtils.join(EnclosedIterable.Functions.toList(affectedExecutionIds),","),ZFJMessageStatus.NEW.getMessageStatus());
                            clusterMessages.add(clusterMessage);
                        }
                    }
                    break;
                case DELETE_PROJECT:
                        if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
                            if(affectedExecutionIds != null && affectedExecutionIds.size() > 0) {
                                final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(),
                                        message.toString(),StringUtils.join(EnclosedIterable.Functions.toList(affectedExecutionIds),","),ZFJMessageStatus.NEW.getMessageStatus());
                                clusterMessages.add(clusterMessage);
                            }
                        }
                        break;
                case DELETE_ISSUE:
                    if(!StringUtils.equalsIgnoreCase(node.getNodeId(),getCurrentNode().getNodeId()) || !skipCurrentNode) {
                        if(affectedExecutionIds != null && affectedExecutionIds.size() > 0) {
                            final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(sourceId, node.getNodeId(),
                                    message.toString(),StringUtils.join(EnclosedIterable.Functions.toList(affectedExecutionIds),","),ZFJMessageStatus.NEW.getMessageStatus());
                            clusterMessages.add(clusterMessage);
                        }
                    }
                    break;
				default:
					break;
            	}
            }
        }
        return clusterMessages;
    }

    @Override
    public List<ZFJClusterMessage> receiveMessages(ZFJMessage.ZFJMessageType messageType)
    {
        return clusterMessageStore.getMessages(getCurrentNode(),messageType);
    }
	
	@Override
    public List<ZFJClusterMessage> receiveMessages()
    {
        return clusterMessageStore.getMessages(getCurrentNode());
    }

    @Override
    public void start()
    {
    	log.debug("********** Starting Services on Node:" + getCurrentNode().getNodeId() + " ***********************");
        messageHandlerService = scheduler.scheduleAtFixedRate(handler, INITIAL_DELAY, PERIOD, SECONDS);
    }

    @Override
    public void stop()
    {
        if (messageHandlerService != null)
        {
            messageHandlerService.cancel(false);
        }
        scheduler.shutdown();
    }

    private Node getCurrentNode()
    {
        return nodeStateManager.getNode();
    }

    @Override
    public void handleReceivedMessages() {
        String jobProgressToken = "";
        try {
            clusterMessageStore.deleteMessage();
            List<ZFJClusterMessage> messages = receiveMessages();
            long startTime = System.currentTimeMillis();
            log.debug("Total Messages retrieved from Indexing :"+ messages.size());
            messages.stream().forEach(message -> {
                switch (message.getMessage().getMessageType())  {
                    case INDEX_EXECUTION:
                        if(message.getAffectedExecutionId() != null ) {
                            String[] executionIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            List<Long> indexExecutionSchedules = Stream.of(executionIds).filter(executionId -> StringUtils.isNotBlank(executionId)).map(executionId -> Long.valueOf(executionId)).collect(Collectors.toList());
                            updateIndexExecution(indexExecutionSchedules, jobProgressToken, startTime);
                            clusterMessageStore.updateMessage(message);
                        }
                        break;
                    case DELETE_EXECUTION:
                        if(message.getAffectedExecutionId() != null && StringUtils.isNotBlank(message.getAffectedExecutionId())) {
                            String[] executionIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            String term = "schedule_id";
                            deleteByTerm(message, executionIds,term);
                        }
                        break;
                    case DELETE_CYCLE:
                        if(message.getAffectedExecutionId() != null && StringUtils.isNotBlank(message.getAffectedExecutionId())) {
                            String[] cycleIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            deleteByTerm(message, cycleIds, ApplicationConstants.CYCLE_IDX);
                        }
                        break;
                    case DELETE_FOLDER:
                        if(message.getAffectedExecutionId() != null && StringUtils.isNotBlank(message.getAffectedExecutionId())) {
                            String[] folderIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            deleteByTerm(message, folderIds, ApplicationConstants.FOLDER_IDX);
                        }
                        break;
                    case DELETE_ISSUE:
                        if(message.getAffectedExecutionId() != null && StringUtils.isNotBlank(message.getAffectedExecutionId())) {
                            String[] projectIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            deleteByTerm(message, projectIds, ApplicationConstants.ISSUE_ID_IDX);
                        }
                        break;
                    case DELETE_PROJECT:
                        if(message.getAffectedExecutionId() != null && StringUtils.isNotBlank(message.getAffectedExecutionId())) {
                            String[] projectIds = StringUtils.split(message.getAffectedExecutionId(), ",");
                            deleteByTerm(message, projectIds, ApplicationConstants.PROJECT_ID_IDX);
                        }
                        break;
                    case RE_INDEX_ALL:
                    case SYNC_INDEX_ALL:
						try {
							scheduleIndexManager.deleteScheduleIndexes();
							reindexOrSyncIndexAll(message, startTime);
						} catch (Exception e) {
                        	e.printStackTrace();
                            log.error("reindexAll or syncIndexAll:", e);
                        }
						break;
                    case REINDEX_BY_PROJECT:
                    case SYNC_INDEX_BY_PROJECT:
                    	try {
                    		scheduleIndexManager.deleteScheduleIndexes();
                    		reindexByProjectOrSyncByProject(message, startTime);
						} catch(Exception e) {
                            e.printStackTrace();
                            log.error("reindexByProject or syncIndexByProject:", e);
                        }
                        break;
                    default:
                        break;
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            log.error("There was a problem handling a cluster message", e);
        }
    }
    
    private void reindexOrSyncIndexAll(ZFJClusterMessage message, long startTime) {
    	CompletableFuture.runAsync(() -> {
			try {
				log.info("Start of ReIndexing all on Node:" + getCurrentNode().getNodeId());
				ReindexJobProgress reindexJobProgress = null;	                    		                    	
            	List<ReindexJobProgress> reindexJobProgressList = scheduleManager.getReindexJobProgress(ZFJMessageType.RE_INDEX_ALL.toString(), OptionalLong.empty());
            	if(reindexJobProgressList.size() > 0) {
            		reindexJobProgress = reindexJobProgressList.get(0);
            	}
				scheduleIndexManager.unZipScheduleDirectory(ApplicationConstants.REINDEX_ALL_ZIP_FILE_NAME);
				if(reindexJobProgress != null) {                   			
            		reindexJobProgress.setCompletedNodeCount(reindexJobProgress.getCompletedNodeCount() + 1);
					reindexJobProgress.save();
					//applying the changes if anything is missed out while taking snapshot.
					checkAndSyncIndexData(reindexJobProgress.getDateIndexed());
				}
				log.info("End of ReIndexing all on Node:" + getCurrentNode().getNodeId());
			} catch (Exception e) {
                String error = "Error performing Reindexing";
                log.error("reindexAll(): " + error, e);
            } finally {
                long endTime = System.currentTimeMillis();
                clusterMessageStore.updateMessage(message);
                log.info("*********Time to unzip reindexall zip file for node - " + getCurrentNode().getNodeId() + " : " + (endTime-startTime) + " - msec");
            }
		});
    }
    
    private void checkAndSyncIndexData(Date indexedDate) throws Exception {
    	Date currentDate = Calendar.getInstance().getTime();
		int totalSchedule = scheduleManager.getScheduleCount(java.util.Optional.of(currentDate), java.util.OptionalLong.empty(), java.util.Optional.of(Boolean.FALSE));
		int indexedCount = getIndexedCount();
		if(totalSchedule != indexedCount) {
			Integer limit = ApplicationConstants.REINDEX_BATCH_SIZE;
			Integer offset = 0;
			//Apply all the newly added schedules into index
			Integer newScheduleCount = scheduleManager.getScheduleCount(java.util.Optional.of(indexedDate), java.util.OptionalLong.empty(), java.util.Optional.of(Boolean.TRUE));
			if(newScheduleCount > 0) {
    			do {                                
    				List<Long> schedulesArr = scheduleManager.getScheduleIdsByPagination(java.util.Optional.of(indexedDate), java.util.OptionalLong.empty(), offset, limit, java.util.Optional.of(Boolean.TRUE));                                  		
                    scheduleIndexManager.reIndexScheduleWithOutMessage(new ScheduleIdsScheduleIterable(schedulesArr, scheduleManager, new ArrayList<>()), Contexts.nullContext(), null);
                    offset += limit;
                } while(offset <= newScheduleCount);
    		}
			//Apply modified and deleted schedules into index based cycle, folder, issue and project.
			scheduleResourceDelegate.applyChangesToIndexFromDate(indexedDate.getTime(), OptionalLong.empty());
			//Check and remove any duplicate schedules indexed in lucene.
			scheduleIndexManager.removeDuplicateSchedules(totalSchedule);
		}
    }
    
    private void reindexByProjectOrSyncByProject(ZFJClusterMessage message, long startTime) {
    	CompletableFuture.runAsync(() -> {
			try {
				log.info("Start of ReIndexing by projects on Node:" + getCurrentNode().getNodeId());
				List<Long> projectIdList = new ArrayList<>();
				if(message.getAffectedExecutionId() != null ) {
					String[] projectIds = StringUtils.split(message.getAffectedExecutionId(), ",");
					if (projectIds != null && projectIds.length > 0) {
						projectIdList = Stream.of(projectIds).map(projectId -> Long.valueOf(projectId)).collect(Collectors.toList());
                    }
					String placeholderCommaList = Joiner.on(",").join(Iterables.transform(projectIdList, Functions.constant("?")));
					ReindexJobProgress reindexJobProgress = null;
                	scheduleIndexManager.deleteScheduleIndexes();
                	List<ReindexJobProgress> reindexJobProgressList = scheduleManager.getReindexJobProgress(ZFJMessageType.REINDEX_BY_PROJECT.toString(), projectIdList, placeholderCommaList);
                	if(reindexJobProgressList.size() > 0) {
                		reindexJobProgress = reindexJobProgressList.get(0);
                	}
					scheduleIndexManager.unZipScheduleDirectory(ApplicationConstants.REINDEX_BY_PROJECT_FILE_NAME);
					if(reindexJobProgress != null) {
						for(ReindexJobProgress reindexJobProgressL : reindexJobProgressList) {
							reindexJobProgressL.setCompletedNodeCount(reindexJobProgress.getCompletedNodeCount() + 1);
							reindexJobProgressL.save();
						}
						//applying the changes if anything is missed out while taking snapshot.
						checkAndSyncIndexData(reindexJobProgress.getDateIndexed());
					}
				}							
				log.info("End of ReIndexing by projects on Node:" + getCurrentNode().getNodeId());
			} catch (Exception e) {
                String error = "Error Performing Reindexing";
                log.error("reindexByProject(): " + message.getAffectedExecutionId() + " : error ->"+ error, e);
            } finally {
                long endTime = System.currentTimeMillis();
                clusterMessageStore.updateMessage(message);
                log.info("*********Time to unzip reindexByProject zip file for node - " + getCurrentNode().getNodeId() + " : " + (endTime-startTime) + " - msec");
            }
		});
    }

    private void deleteByTerm(ZFJClusterMessage message, String[] executionIds,String term) {
        EnclosedIterable<String> scheduleIdIterables = CollectionEnclosedIterable.copy(Arrays.asList(executionIds));
        scheduleIndexer.deleteBatchIndexByTerm(scheduleIdIterables, term, Contexts.nullContext());
        clusterMessageStore.updateMessage(message);
    }
	
	private void updateIndexExecution(List<Long> indexExecutionSchedules, String jobProgressToken, long startTime) {
    	 if(indexExecutionSchedules.size() > 0) {
             log.info("Total Executions to be Indexed::" + indexExecutionSchedules.size());
             int count = 0;
             int limit = 999;
             while (count < indexExecutionSchedules.size()) {
                 if ((indexExecutionSchedules.size() - count) < limit) {
                     limit = indexExecutionSchedules.size() - count;
                 }
                 List<Long> subList = indexExecutionSchedules.subList(count, count + limit);
                 Collection<Schedule> schedules = scheduleManager.getSchedulesInBatch(subList);
                 //Incase the schedules are deleted, it will always keep looping in as there is no guarantee of the ordering. In that case, we will loop it forward
                 if(schedules == null || schedules.size() == 0) {
                     count = count + subList.size();
                 } else {
                     EnclosedIterable<Schedule> scheduleIterable = CollectionEnclosedIterable.copy(schedules);
                     scheduleIndexer.reIndexSchedules(scheduleIterable, Contexts.nullContext(), jobProgressToken);
                     count = count + schedules.size();
                 }
             }
             //need to revisit this when there are more than 2 nodes - for now deleting is fine
             long endTime = System.currentTimeMillis();
             log.debug("Total Time for Indexing:" + (endTime - startTime) + " - msec");
         }
    }
	
	 private int getIndexedCount() {
	        final IndexSearcher scheduleSearcher = scheduleIndexer.getScheduleIndexSearcher();
	        try {
	        	return scheduleSearcher.getIndexReader().numDocs();
	        } catch(Exception e) {
	        	log.warn("Error while getting indexed count in hander service class",e);
	        } finally {
	        	try {
					scheduleSearcher.close();
				} catch (IOException e) {
		        	log.warn("Error closing searcher",e);
				}
	        }
	        return 0;
	    }

	@Override
	public ZFJClusterMessage addCurrentNodeindexMessage(String message, String status) {
		if (getCurrentNode().isClustered()) {
			final ZFJClusterMessage clusterMessage =  clusterMessageStore.createMessage(getCurrentNode().getNodeId(), getCurrentNode().getNodeId(), message.toString(), null, status);
	   	 	return clusterMessage;
        }
		return null;
	}
	
	@Override
	public void updateMessageForCurrentNode(ZFJClusterMessage zfjClusterMessage)  {
		clusterMessageStore.updateMessage(zfjClusterMessage);
	}

}

