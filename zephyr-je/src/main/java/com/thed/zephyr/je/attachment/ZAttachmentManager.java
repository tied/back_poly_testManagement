package com.thed.zephyr.je.attachment;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.user.ApplicationUser;
import net.java.ao.RawEntity;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.exception.AttachmentNotFoundException;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.project.Project;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;

/**
 * Manages all attachment related tasks in JIRA, which involves retrieving an attachment,
 * creating an attachment and deleting an attachment.
 */
public interface ZAttachmentManager {
	  /**
     * Get a single attachment by its ID.
     *
     * @param id the Attachment ID
     * @return the Attachment can never be null as an exception is thrown if an attachment with the passed id does not
     *  exist.
     * @throws Exception if there is a problem accessing the database.
     * @throws AttachmentNotFoundException thrown if an attachment with the passed id does not exist.
     */
    Attachment getAttachment(Long id) throws Exception, AttachmentNotFoundException;

    /**
     * Get a list of all attachments for a certain issue.
     *
     * @param issue the Issue
     * @return a list of {@link Attachment} objects
     * @throws Exception if there is a problem accessing the database.
     */
    List<Attachment> getAttachments(Long entityId, String entityType) throws Exception;

    /**
     * Get a list of all attachments for a certain issue, sorted according to the specified comparator.
     *
     * @param schedule the schedule
     * @param comparator used for sorting
     * @return a list of {@link Attachment} objects
     * @throws Exception if there is a problem accessing the database.
     */
    List<Attachment> getAttachments(Long entityId, String entityType, Comparator<? super Attachment> comparator) throws Exception;

    /**
     * Create an attachment both on disk, and in the database by copying the provided file instead of moving it.
     *
     * @param file                 A file on a locally accessible filesystem, this will be copied, not moved.
     * @param filename             The desired filename for this attachment.  This may be different to the filename on disk (for example with temp files used in file uploads)
     * @param contentType          The desired contentType.  Implementations of this interface can choose to override this value as appropriate
     * @param attachmentAuthor     The username of the user who created this attachment, this is not validated so it must be a valid username
     * @param issue                The id of the issue that this attachment is attached to
     * @param attachmentProperties Attachment properties (a Map of String -> Object properties).  These are optional,
     *                             and are used to populate a PropertySet on the Attachment ({@link com.atlassian.jira.issue.attachment.Attachment#getProperties()}.  Pass null to set no properties
     * @param createdTime          when the attachment was created
     *
     * @return the Attachment
     * @throws com.atlassian.jira.web.util.ZAttachmentException if any errors occur.
     */
   // Attachment createAttachmentCopySourceFile(File file, String filename, String contentType, String attachmentAuthor, Long entityId, String entityType, Map<String, Object> attachmentProperties, Date createdTime,Project project) throws ZAttachmentException;

    /**
     * Create an attachment both on disk, and in the database.
     *
     * @param file                 A file on a locally accessible filesystem
     * @param filename             The desired filename for this attachment.  This may be different to the filename on disk (for example with temp files used in file uploads)
     * @param contentType          The desired contentType.  Implementations of this interface can choose to override this value as appropriate
     * @param remoteUser           The use who created this attachment
     * @param entityId             The schedule or step that this attachment is attached to
     * @param entityType           schedule or step
     * @param comment              comment associated to the attachment
     * @param attachmentProperties Attachment properties (a Map of String -> Object properties).  These are optional,
     *                             and are used to populate a PropertySet on the Attachment ({@link com.atlassian.jira.issue.attachment.Attachment#getProperties()}.  Pass null to set no properties
     * @param createdTime the created time
     * @return A {@link ChangeItemBean} with all the changes to the issue.
     *
     * @throws com.thed.zephyr.je.attachment.ZAttachmentException if an error occurs while attempting to copy the file
     * @throws org.ofbiz.core.entity.GenericEntityException if there is an error in creating the DB record for the attachment
     */
    ChangeItemBean createAttachment(File file, String filename, String contentType, ApplicationUser remoteUser, Long entityId, String entityType, String comment, Map<String, Object> attachmentProperties, Date createdTime,Project project) throws ZAttachmentException, Exception;

    /**
     * Same as the {@link #createAttachment(java.io.File, String, String, User, org.ofbiz.core.entity.GenericValue, java.util.Map, java.util.Date)} method, except it
     * submits no attachmentProperties and uses now() for the created time.
     *
     * @param file        A file on a locally accessible filesystem
     * @param filename    The desired filename for this attachment.  This may be different to the filename on disk (for example with temp files used in file uploads)
     * @param contentType The desired contentType.  Implementations of this interface can choose to override this value as appropriate
     * @param remoteUser  The use who created this attachment
     * @param issue       The issue that this attachment is attached to
     * @return A {@link ChangeItemBean} with all the changes to the issue.
     *
     * @throws com.thed.zephyr.je.attachment.ZAttachmentException if an error occurs while attempting to copy the file
     * @throws org.ofbiz.core.entity.GenericEntityException if there is an error in creating the DB record for the attachment
     */
    ChangeItemBean createAttachment(File file, String filename, String contentType, ApplicationUser remoteUser, Long entityId, String entityType, String comment, Project project) throws ZAttachmentException, Exception;

    /**
     * Create an attachment in the database.  Note that this does not create it on disk, nor does it create a change item.
     *
     * @param issue                the issue that this attachment is attached to
     * @param author               The user who created this attachment
     * @param mimetype             mimetype
     * @param filename             The desired filename for this attachment.
     * @param filesize             filesize
     * @param comment              attachment's comment
     * @param attachmentProperties Attachment properties (a Map of String -> Object properties).
     * @param createdTime          when the attachment was created
     *
     * @return the Attachment
     * @throws org.ofbiz.core.entity.GenericEntityException if there is an error in creating the DB record for the attachment
     */
    Attachment createAttachment(Long entityId, String entityType, ApplicationUser author, String mimetype, String filename, Long filesize, String comment, Map<String, Object> attachmentProperties, Date createdTime,Project project) throws Exception;

    /**
     * Delete an attachment from the database and from disk.
     *
     * @param attachment the Attachment
     * @throws Exception if the attachment cannot be removed from the disk
     */
    void deleteAttachment(Attachment attachment) throws Exception;

    void delete(JiraServiceContext jiraServiceContext, Long attachmentId) throws Exception;

    /**
     * Delete the attachment directory from disk if the directory is empty.
     *
     * @param issue the issue whose attachment directory we wish to delete.
     * @throws Exception if the directory can not be removed or is not empty.
     */
    void deleteAttachmentDirectory(Long entityId, String entityType,Project project) throws Exception;

    /**
     * Determine if attachments have been enabled in JIRA and if the attachments directory exists.
     * @return true if enabled, false otherwise
     */
    boolean attachmentsEnabled();

    /**
     * Determine if screenshot applet has been enabled in JIRA.
     * @return true if enabled, false otherwise
     */
    boolean isScreenshotAppletEnabled();

    /**
     * Determine if the screenshot applet is supported by the user's operating system.
     *
     * Note. This always returns true now as we support screenshots on all our supported platforms
     *
     * @return true if applet is supported by the user's OS, false otherwise
     */
    boolean isScreenshotAppletSupportedByOS();

    /**
     * Converts a set of provided temporary attachments to real attachments attached to an issue.  This method will
     * also clean up any temporary attachments still linked to the issue via the TemporaryAttachmentsMonitor.
     *
     * @param user The user performing the action
     * @param entityId The execution or step id attachments should be linked to
     * @param entityType execution or step execution
     * @param comment The attachment comment
     * @param selectedAttachments The temporary attachment ids to convert as selected by the user
     * @param temporaryAttachmentsMonitor TemporaryAttachmentsMonitor containing information about all temporary attachments
     * @param currentProject 
     * @return A list of ChangeItemBeans for any attachments that got created
     * @throws ZAttachmentException If there were problems with the Attachment itself
     * @throws GenericEntityException if there is an error in creating the DB record for the attachment
     */
    List<ChangeItemBean> convertTemporaryAttachments(final ApplicationUser user, final Long entityId, final String entityType, final String comment, final List<Long> selectedAttachments,
            final ZTemporaryAttachmentsMonitor temporaryAttachmentsMonitor,Project currentProject) throws Exception;
}
