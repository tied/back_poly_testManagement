package com.thed.zephyr.je.service.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueManager;
import com.thed.zephyr.je.event.TeststepModifyEvent;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.util.JiraUtil;
import net.java.ao.Query;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.StepResultModifyEvent;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.service.AttachmentManager;
import com.thed.zephyr.util.ApplicationConstants;

import javax.ws.rs.core.Response;


public class AttachmentManagerImpl implements AttachmentManager {
	private static final Logger log = LoggerFactory.getLogger(AttachmentManagerImpl.class);

	
	private final ActiveObjects ao;
	private final ProjectManager projectManager;
	private final IssueManager issueManager;
    private final EventPublisher eventPublisher;
    private final JiraAuthenticationContext authContext;
	
	public AttachmentManagerImpl(ActiveObjects ao,ProjectManager projectManager, final EventPublisher eventPublisher, final JiraAuthenticationContext authContext,
                                IssueManager issueManager) {
		this.ao = checkNotNull(ao);
		this.projectManager=projectManager;
		this.eventPublisher = eventPublisher;
		this.authContext = authContext;
		this.issueManager = issueManager;
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.AttachmentManager#getAttachments(com.thed.jira.plugin.model.Attachment)
	 */
	@Override
	public List<Attachment> getAttachments(Attachment attachment) {
		Attachment [] attachments =  ao.find(Attachment.class);
		return Arrays.asList(attachments);
	}

	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.AttachmentManager#getAttachment(int id)
	 */
	@Override
	public Attachment getAttachment(Long id) {
		Attachment[] attachments =  ao.find(Attachment.class, Query.select().where("ID = ?", id));
		if(attachments != null && attachments.length > 0){
			return attachments[0];
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see com.thed.jira.plugin.service.AttachmentManager#getAttachmentsByEntityIdAndType(Integer id,String type)
	 */
	@Override
	public List<Attachment> getAttachmentsByEntityIdAndType(Integer entityId,String entityType) {
		Attachment[] attachments = ao.find(Attachment.class, Query.select().where("ENTITY_ID = ? and TYPE = ?", entityId,entityType)); 
		if(attachments != null && attachments.length > 0) {
			return Arrays.asList(attachments);
		}
		return new ArrayList<Attachment>();
	}
	
	@Override
	public Attachment saveAttachment(Map<String, Object> attachmentProperties) {
		return ao.create(Attachment.class, attachmentProperties);
	}
	
	@Override
	public Response removeAttachment(Attachment attachment) {
		Project project = null;
		StepResult result = null;
		Schedule schedule = null;
		Integer issueId = null;
        Teststep teststep = null;
		if(attachment != null){
			if(ApplicationConstants.TESTSTEPRESULT_TYPE.equals(attachment.getType())){
				result = ao.get(StepResult.class, attachment.getEntityId().intValue());
				project = projectManager.getProjectObj(result.getProjectId());
				schedule = ao.get(Schedule.class, result.getScheduleId());
				issueId = schedule.getIssueId();
			}
			else if(ApplicationConstants.SCHEDULE_TYPE.equals(attachment.getType())){
				schedule = ao.get(Schedule.class, attachment.getEntityId().intValue());
				project = projectManager.getProjectObj(schedule.getProjectId());
				issueId = schedule.getIssueId();
			}else if(StringUtils.equalsIgnoreCase(ApplicationConstants.TEST_STEP_TYPE,attachment.getType())) {
				teststep = ao.get(Teststep.class,attachment.getEntityId().intValue());
				issueId = teststep.getIssueId().intValue();
				project = issueManager.getIssueObject(teststep.getIssueId()).getProjectObject();
			}

			if(issueId != null) {
				boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(issueId),null,authContext.getLoggedInUser());
				if (!hasViewIssuePermission) {
					String errorMessage = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("schedule.project.permission.error", "Attachment for EntityType " + attachment.getType(), String.valueOf(project.getId()));
                    log.error("[Error] [Error code:"+ Response.Status.NOT_ACCEPTABLE.getStatusCode()+" "+Response.Status.NOT_ACCEPTABLE+" Error Message :"+errorMessage);
					return JiraUtil.getPermissionDeniedErrorResponse("Insufficient Issue permissions." + errorMessage);
				}
			}

			deleteAttachmentFiles(new Attachment[]{attachment}, attachment.getType(), project);
			ao.delete(attachment);
			// tracking attachment remove event for change logs.
        	Table<String, String, Object> changePropertyTable =  HashBasedTable.create();
    		changePropertyTable.put("ATTACHMENT", ApplicationConstants.OLD, attachment.getFileName());
    		changePropertyTable.put("ATTACHMENT", ApplicationConstants.NEW, ApplicationConstants.NULL);				
			if(ApplicationConstants.TESTSTEPRESULT_TYPE.equals(attachment.getType())){
				eventPublisher.publish(new StepResultModifyEvent(result, changePropertyTable, EventType.STEPRESULT_ATTACHMENT_DELETED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
			}
			else if(ApplicationConstants.SCHEDULE_TYPE.equals(attachment.getType())){
				eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ATTACHMENT_DELETED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
			}else if (ApplicationConstants.TEST_STEP_TYPE.equalsIgnoreCase(attachment.getType()) && null != teststep) {
				eventPublisher.publish(new TeststepModifyEvent(teststep, changePropertyTable, EventType.TESTSTEP_UPDATED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
			}
		}
		return null;
	}
	
	@Override
	public void removeAttachmentsInBulk(String entityType, List<Integer> entityIds,Project project) {
		if(entityIds == null || entityIds.size() < 1)
			return;
		int fromIndex = 0;
		int toIndex = 0;
		boolean stopSublist=false;
		while(!stopSublist) {
			toIndex = entityIds.size() > fromIndex && 
					entityIds.size() > (fromIndex + ApplicationConstants.MAX_IN_QUERY) ? 
							(fromIndex + ApplicationConstants.MAX_IN_QUERY) : entityIds.size();
			String ques = StringUtils.repeat("?", ",", toIndex - fromIndex);
			List<Integer> subList = entityIds.subList(fromIndex, toIndex);
			Attachment[] attachments = ao.find(Attachment.class, Query.select().where("ENTITY_ID IN ("+ ques +") AND TYPE = '" + entityType + "'", subList.toArray()));
			if(deleteAttachmentFiles(attachments, entityType, project)) {
				if(attachments != null && attachments.length > 0)
					ao.delete(attachments);
			}
			fromIndex += ApplicationConstants.MAX_IN_QUERY;
			if(toIndex == entityIds.size()) {
				stopSublist = true;
			}
		}
	}
	
	private boolean deleteAttachmentFiles(Attachment[] attachments, String entityType, Project project) {
    	for(Attachment attachment : attachments) {
	        File attachmentFile = AttachmentUtils.getAttachmentFile(attachment, project);
	        File thumbnailFile = AttachmentUtils.getThumbnailFile(attachment.getEntityId(),entityType, attachment, project);
        	File fileDir = AttachmentUtils.getAttachmentDirectory(attachment.getEntityId(), entityType, project);

	        org.apache.commons.io.FileUtils.deleteQuietly(AttachmentUtils.getLegacyThumbnailFile(attachment,project));
	        try {
		        if (attachmentFile.exists() && thumbnailFile.exists()) {
		            attachmentFile.delete();
		            thumbnailFile.delete();
		        	fileDir.delete();
		        } else if (attachmentFile.exists()) {
		        	attachmentFile.delete();
		        	fileDir.delete();
		        } else {
		        	log.warn("Trying to delete non-existent attachment: [" + attachmentFile.getAbsolutePath() + "] ..ignoring");
		        }
	        } catch(Exception e) {
	        	log.warn("Error Trying to delete attachment : [" + attachmentFile.getAbsolutePath() + "] ..");
	        }
    	}
    	return true;
    }

	@Override
	public List<Attachment> getAttachmentsByCriteria(String searchExpression, int maxAllowedRecord) {
		// TODO Auto-generated method stub
		return null;
	}

}
