package com.thed.zephyr.je.attachment;

import static com.atlassian.core.util.WebRequestUtils.MACOSX;
import static com.atlassian.core.util.WebRequestUtils.WINDOWS;
import static com.atlassian.core.util.WebRequestUtils.getBrowserOperationSystem;
import static com.atlassian.jira.util.dbc.Assertions.notNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.util.UtilDateTime;

import webwork.action.ServletActionContext;

import com.atlassian.core.util.FileUtils;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.util.AttachmentPathManager;
import com.atlassian.jira.exception.AttachmentNotFoundException;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.usercompatibility.UserCompatibilityHelper;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.mime.MimeManager;
import com.atlassian.jira.web.ExecutingHttpRequest;
import com.atlassian.jira.web.bean.I18nBean;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.thed.zephyr.je.event.EntityType;
import com.thed.zephyr.je.event.EventType;
import com.thed.zephyr.je.event.ScheduleModifyEvent;
import com.thed.zephyr.je.event.StepResultModifyEvent;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ApplicationConstants;

public class ZAttachmentManagerImpl implements ZAttachmentManager
{
    private static final Logger log = Logger.getLogger(ZAttachmentManagerImpl.class);
    private final MimeManager mimeManager = ComponentAccessor.getComponent(MimeManager.class);
    private final ApplicationProperties applicationProperties;
    private final AttachmentPathManager attachmentPathManager = ComponentAccessor.getComponent(AttachmentPathManager.class);
    private final com.thed.zephyr.je.service.AttachmentManager attachmentManager;
    private final ProjectManager projectManager;
    private final I18nBean.BeanFactory i18nBeanFactory;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final EventPublisher eventPublisher;
    private final JiraAuthenticationContext authContext;
    private final StepResultManager stepResultManager;
    private final ScheduleManager scheduleManager;

    
    public ZAttachmentManagerImpl(final ApplicationProperties applicationProperties,
            final I18nHelper.BeanFactory i18nBeanFactory,com.thed.zephyr.je.service.AttachmentManager attachmentManager,ProjectManager projectManager,
            final JiraAuthenticationContext jiraAuthenticationContext, final EventPublisher eventPublisher, final JiraAuthenticationContext authContext,
            final StepResultManager stepResultManager, final ScheduleManager scheduleManager)
    {
        this.applicationProperties = applicationProperties;
        this.projectManager = projectManager;
        this.i18nBeanFactory = i18nBeanFactory;
        this.attachmentManager=attachmentManager;
        this.jiraAuthenticationContext=jiraAuthenticationContext;
        this.eventPublisher = eventPublisher;
        this.authContext = authContext;
        this.stepResultManager = stepResultManager;
        this.scheduleManager = scheduleManager;
    }

    @Override
    public Attachment getAttachment(Long id)
    {
        Attachment attachment = attachmentManager.getAttachment(id);
        if (attachment == null) throw new AttachmentNotFoundException(id);
        return attachment;
    }

    @Override
    public List<Attachment> getAttachments(Long entityId, String entityType)
    {
        return getStoredAttachments(entityId,entityType);
    }

    public List<Attachment> getStoredAttachments(Long entityId, String entityType)
    {
        Collection<Attachment> attachmentGvs = attachmentManager.getAttachmentsByEntityIdAndType(entityId.intValue(),entityType);
        List<Attachment> attachments = new ArrayList<Attachment>(attachmentGvs.size());
        return attachments;
    }

    private List<Attachment> getStoredAttachments(Long entityId, String entityType, Comparator<? super Attachment> comparator)
    {
        List<Attachment> attachments = getStoredAttachments(entityId,entityType);
        Collections.sort(attachments, comparator);
        return attachments;
    }

    @Override
    public List<Attachment> getAttachments(Long entityId, String entityType, Comparator<? super Attachment> comparator)
    {
        return getStoredAttachments(entityId,entityType, comparator);
    }

 /*   public Attachment createAttachmentCopySourceFile(final File file, final String filename, final String contentType, final String attachmentAuthor, final Long entityId, String entityType, final Map<String, Object> attachmentProperties, final Date createdTime)
            throws ZAttachmentException
    {
        if (file == null)
        {
            log.warn("Cannot create attachment without a file (filename=" + filename + ").");
            return null;
        }
        else if (filename == null)
        {
            // Perhaps we should just use the temporary filename instead of losing the attachment? These are all hacks anyway. We need to properly support multipart/{related,inline}
            log.warn("Cannot create attachment without a filename - inline content? See http://jira.atlassian.com/browse/JRA-10825 (file=" + file.getName() + ").");
            return null;
        }
        AttachmentUtils.checkValidAttachmentDirectory(schedule);

        //get sanitised version of the mimeType.
        String contentTypeFromFile = mimeManager.getSanitisedMimeType(contentType, filename);

       	Attachment attachment = createAttachment(schedule.getID(), attachmentAuthor, contentTypeFromFile, filename, file.length(), attachmentProperties, createdTime);

        // create attachment on disk
        createAttachmentOnDiskCopySourceFile(attachment, file);
        return attachment;
    }*/

    @Override
    public Attachment createAttachment(Long entityId, String entityType,  ApplicationUser author, String mimetype, String filename, Long filesize, String comment, Map<String, Object> attachmentProperties, Date createdTime,Project project) throws Exception
    {
        return createAttachment(entityId, entityType,(author != null ? UserCompatibilityHelper.getKeyForUser(author.getDirectoryUser()) : null), mimetype, filename, filesize, comment, attachmentProperties, createdTime);
    }

    private Attachment createAttachment(Long entityId, String entityType, String authorName, String mimetype, String filename, Long filesize, String comment, 
    		Map<String, Object> attachmentProperties, Date createdTime) throws Exception
    {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("ENTITY_ID", entityId);
        fields.put("TYPE", entityType);
        fields.put("AUTHOR", authorName);
        fields.put("MIMETYPE", mimetype);
        fields.put("FILE_NAME", filename);
        fields.put("FILESIZE", filesize);
        fields.put("COMMENT", comment);
        fields.put("DATE_CREATED", createdTime);

        Attachment attachment = attachmentManager.saveAttachment(fields);
        return attachment;
    }

    @Override
    public void deleteAttachment(Attachment attachment) throws Exception
    {
        try
        {
        	Project project = projectManager.getProjectObj(new Long(attachment.getID()));
            if (deleteAttachmentFile(attachment,project)) {
                attachmentManager.removeAttachment(attachment);
            }
            else {
                throw new Exception("Could not delete attachment file");
            }
        }
        catch (Exception e)
        {
            log.error("Unable to delete attachment.", e);
            throw e;
        }
    }

    @Override
    public void delete(JiraServiceContext jiraServiceContext, Long attachmentId) {
        ErrorCollection errorCollection = jiraServiceContext.getErrorCollection();

        Attachment attachment = getAndVerifyAttachment(attachmentId, errorCollection);

        if (errorCollection.hasAnyErrors())
        {
            return;
        }


        //attempt to delete the attachment from disk and related metadata from database
        try
        {
            attachmentManager.removeAttachment(attachment);
        }
        catch (Exception e)
        {
            errorCollection.addErrorMessage(getText("attachment.service.error.delete.attachment.failed", attachmentId.toString()));
            return;
        }
  	
    }

    
    private Attachment getAndVerifyAttachment(Long attachmentId, ErrorCollection errorCollection)
    {
        if (attachmentId == null)
        {
            errorCollection.addErrorMessage(getText("attachment.service.error.null.attachment.id"));
            return null;
        }

        Attachment attachment = attachmentManager.getAttachment(attachmentId);

        if (attachment == null)
        {
            errorCollection.addErrorMessage(getText("attachment.service.error.null.attachment", attachmentId.toString()));
        }
        return attachment;
    }
    
    @Override
	public void deleteAttachmentDirectory(Long entityId, String entityType,Project project) throws Exception
    {
        if (entityId != null && entityType != null && attachmentsAllowedAndDirectoryIsSet())
        {
            File attachmentDir = AttachmentUtils.getAttachmentDirectory(entityId,entityType,project);
            if (!attachmentDir.isDirectory()) throw new Exception("Attachment path '"+attachmentDir+"' is not a directory");
            if (!attachmentDir.canWrite()) throw new Exception("Can't write to attachment directory '"+attachmentDir+"'");

            // Remove the /thumbs/ subdirectory if required.
            final File thumbnailDirectory = new File(attachmentDir, AttachmentUtils.THUMBS_SUBDIR);
            if (thumbnailDirectory.exists())
            {
                // We want to delete it.
                if (thumbnailDirectory.listFiles().length == 0)
                {
                    boolean deleted = thumbnailDirectory.delete();
                    if (!deleted)
                    {
                        log.error("Unable to delete the issue attachment thumbnail directory '" + thumbnailDirectory + "'.");
                    }
                }
                else
                {
                    for (File file : thumbnailDirectory.listFiles())
                    {
                        log.debug("file = " + file);
                    }
                    log.error("Unable to delete the issue attachment thumbnail directory '" + thumbnailDirectory + "' because it is not empty.");
                }
            }

            if (attachmentDir.listFiles().length == 0)
            {
                if (!attachmentDir.delete())
                {
                    log.error("Unable to delete the issue attachment directory '" + attachmentDir + "'.");
                }
            }
            else
            {
                log.error("Unable to delete the issue attachment directory '" + attachmentDir + "' because it is not empty.");
            }
        }
    }

    private boolean attachmentsAllowedAndDirectoryIsSet()
    {
        String attachmentDir = attachmentPathManager.getAttachmentPath();
        return applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS) && StringUtils.isNotBlank(attachmentDir);
    }

    @Override
    public boolean attachmentsEnabled()
    {
        boolean allowAttachments = applicationProperties.getOption(APKeys.JIRA_OPTION_ALLOWATTACHMENTS);
        boolean attachmentPathSet = StringUtils.isNotBlank(attachmentPathManager.getAttachmentPath());
        return allowAttachments && attachmentPathSet;
    }

    @Override
    public boolean isScreenshotAppletEnabled()
    {
        return applicationProperties.getOption(APKeys.JIRA_SCREENSHOTAPPLET_ENABLED);
    }

    protected boolean isScreenshotAppletEnabledForLinux()
    {
        return applicationProperties.getOption(APKeys.JIRA_SCREENSHOTAPPLET_LINUX_ENABLED);
    }

    @Override
    public boolean isScreenshotAppletSupportedByOS()
    {
        if (isScreenshotAppletEnabledForLinux())
        {
            // means all OS are supported so just return true.
            return true;
        }

        // Linux is still flakey
        int browserOS = getUsersOS();
        return (browserOS == WINDOWS || browserOS == MACOSX );
    }

    @Override
    public List<ChangeItemBean> convertTemporaryAttachments(final ApplicationUser user, final Long entityId, final String entityType,final String comment, final List<Long> selectedAttachments,
            final ZTemporaryAttachmentsMonitor temporaryAttachmentsMonitor, Project currentProject) throws Exception     {
        notNull("entityId", entityId);
        notNull("entityType", entityType);
        notNull("selectedAttachments", selectedAttachments);
        notNull("temporaryAttachmentsMonitor", temporaryAttachmentsMonitor);

        final List<ChangeItemBean> ret = new ArrayList<ChangeItemBean>();
        for (final Long selectedAttachment : selectedAttachments)
        {
            final com.thed.zephyr.je.attachment.TemporaryAttachment tempAttachment = temporaryAttachmentsMonitor.getById(selectedAttachment);
            final ChangeItemBean cib = createAttachment(tempAttachment.getFile(), tempAttachment.getFilename(), tempAttachment.getContentType(), user, entityId,entityType, comment, Collections.<String, Object>emptyMap(), UtilDateTime.nowTimestamp(),currentProject);
            if(cib != null)
            {
                ret.add(cib);
            }
            // Table to keep track of modified attachments of given StepResult for change logs
        	Table<String, String, Object> changePropertyTable =  HashBasedTable.create();
    		changePropertyTable.put("ATTACHMENT", ApplicationConstants.OLD, ApplicationConstants.NULL);
    		changePropertyTable.put("ATTACHMENT", ApplicationConstants.NEW, tempAttachment.getFilename());	            
            //if entityType == STEPRESULT, save change logs for attachment
            if(entityType.equals(ApplicationConstants.TESTSTEPRESULT_TYPE)){
	    		// get the StepResult by given Id
	    		StepResult stepResult = stepResultManager.getStepResult(entityId.intValue());
                Schedule schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
                schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                schedule.setModifiedDate(new Date());
                schedule.save();

				// publishing ScheduleModifyEvent for change logs
				eventPublisher.publish(new StepResultModifyEvent(stepResult, changePropertyTable, EventType.STEPRESULT_ATTACHMENT_ADDED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            } else if(entityType.equals(ApplicationConstants.SCHEDULE_TYPE)){
            	// get the Schedule by given Id
            	Schedule schedule = scheduleManager.getSchedule(entityId.intValue());
                schedule.setModifiedBy(UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext)));
                schedule.setModifiedDate(new Date());
                schedule.save();
				// publishing ScheduleModifyEvent for change logs
				eventPublisher.publish(new ScheduleModifyEvent(schedule, changePropertyTable, EventType.EXECUTION_ATTACHMENT_ADDED,
						UserCompatibilityHelper.getKeyForUser(JiraUtil.getLoggedInUser(authContext))));
            }
            // Add event for test step.

        }
        
        //finally clear any other remaining temp attachments for this issue
        temporaryAttachmentsMonitor.clearEntriesForEntity(entityId);
        return ret;
    }

    int getUsersOS()
    {
        HttpServletRequest servletRequest = ExecutingHttpRequest.get();
        if (servletRequest == null)
        {
            servletRequest = ServletActionContext.getRequest();

        }
        return getBrowserOperationSystem(servletRequest);

    }

    private static boolean deleteAttachmentFile(Attachment attachment,Project project) throws Exception
    {
        File attachmentFile = AttachmentUtils.getAttachmentFile(attachment,project);
        File thumbnailFile = AttachmentUtils.getThumbnailFile(attachment);

        org.apache.commons.io.FileUtils.deleteQuietly(AttachmentUtils.getLegacyThumbnailFile(attachment, project));

        if (attachmentFile.exists() && thumbnailFile.exists())
        {
            return attachmentFile.delete() && thumbnailFile.delete();
        }
        else if (attachmentFile.exists())
        {
            return attachmentFile.delete();
        }
        else
        {
            log.warn("Trying to delete non-existent attachment: [" + attachmentFile.getAbsolutePath() + "] ..ignoring");
            return true;
        }
    }

    /**
     * @param contentType          The desired contentType.  This may be modified if a better alternative is suggested by {@link MimeManager#getSanitisedMimeType(String, String)}
     * @param attachmentProperties String -> Object property map
     * @param currentProject 
     * @throws Exception 
     */
    @Override
	public ChangeItemBean createAttachment(File file, String filename, String contentType, ApplicationUser remoteUser, Long entityId, String entityType, String comment, Map<String, Object> attachmentProperties, Date createdTime, Project currentProject)
    throws Exception
    {
        if (file == null)
        {
            log.warn("Cannot create attachment without a file (filename="+filename+").");
            return null;
        }
        else if (filename == null)
        {
            // Perhaps we should just use the temporary filename instead of losing the attachment? These are all hacks anyway. We need to properly support multipart/{related,inline}
            log.warn("Cannot create attachment without a filename - inline content? See http://jira.atlassian.com/browse/JRA-10825 (file="+file.getName()+").");
            return null;
        }
        AttachmentUtils.checkValidAttachmentDirectory(entityId,entityType,currentProject);

        //get sanitised version of the mimeType.
        contentType = mimeManager.getSanitisedMimeType(contentType, filename);

        Attachment attachment = createAttachment(entityId,entityType,remoteUser, contentType, filename, new Long(file.length()), comment, attachmentProperties, createdTime,currentProject);

        // create attachment on disk
        createAttachmentOnDisk(attachment, file, remoteUser, currentProject);

        return new ChangeItemBean(ChangeItemBean.STATIC_FIELD, "Attachment", null, null, String.valueOf(attachment.getID()), filename);
    }

    @Override
    public ChangeItemBean createAttachment(File file, String filename, String contentType, ApplicationUser remoteUser, Long entityId, String entityType,String comment, Project project)
    throws ZAttachmentException, Exception
    {
        return createAttachment(file, filename, contentType, remoteUser, entityId,entityType, comment, Collections.<String, Object>emptyMap(), UtilDateTime.nowTimestamp(),project);
    }

    protected void createAttachmentOnDisk(Attachment attachment, File file, ApplicationUser user, Project currentProject) throws ZAttachmentException
    {
        File attachmentFile = AttachmentUtils.getAttachmentFile(attachment,currentProject);

        boolean renameSucceded = file.renameTo(attachmentFile);

        //java cannot rename files across partitions
        if (!renameSucceded)
        {
            // may be trying to move across different file systems (JRA-839), try the old copy and delete
            try
            {
                FileUtils.copyFile(file, attachmentFile);
                if (!file.delete())
                {
                    throw new ZAttachmentException(i18nBeanFactory.getInstance(user).
                            getText("attachfile.error.delete", file.getAbsolutePath()));
                }
            }
            catch (Exception e)
            {
                final String message =
                        i18nBeanFactory.getInstance(user).
                                getText("attachfile.error.move",
                                        Lists.newArrayList(file.getAbsolutePath(), attachmentFile.getAbsolutePath(), e));

                log.error(message, e);
                throw new ZAttachmentException(message);
            }
        }
    }

//    protected void createAttachmentOnDiskCopySourceFile(Attachment attachment, File file) throws ZAttachmentException
//    {
//        File attachmentFile = AttachmentUtils.getAttachmentFile(attachment);
//
//        try
//        {
//            FileUtils.copyFile(file, attachmentFile);
//        }
//        catch (IOException e)
//        {
//            log.error("Could not copy attachment from '" + file.getAbsolutePath() + "' to '" + attachmentFile.getAbsolutePath() + "'.", e);
//            throw new ZAttachmentException("Could not copy attachment from '" + file.getAbsolutePath() + "' to '" + attachmentFile.getAbsolutePath() + "'.");
//        }
//    }
    
    private String getText(String key)
    {
        return jiraAuthenticationContext.getI18nHelper().getText(key);
    }

    private String getText(String key, String param)
    {
        return jiraAuthenticationContext.getI18nHelper().getText(key, param);
    }

}
