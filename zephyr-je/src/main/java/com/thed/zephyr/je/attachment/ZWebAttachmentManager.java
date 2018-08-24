package com.thed.zephyr.je.attachment;

import java.io.InputStream;

import net.java.ao.RawEntity;

import webwork.multipart.MultiPartRequestWrapper;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.util.AttachmentException;
import com.thed.zephyr.je.model.Schedule;

/**
 * Manager for issue attachments.
 */
public interface ZWebAttachmentManager
{

    /**
     * Creates a temporary attachment on disk.  These attachment generally only live for the duration of a user's session
     * and will also be deleted on exit of the JVM. This method will not create a link to the issue yet, but simply
     * copy the attachment to a temp directory in the attachments folder and store all the relevant details in the
     * returned {@link com.atlassian.jira.issue.attachment.TemporaryAttachment} object
     *
     * @param requestWrapper the wrapper containing getFile() and getFilesystemName() describing the attachment
     * @param fileParamName ame of form parameter specifying filename (in requestWrapper).
     * @param issue The issue that this temporary attachment is for.  Can be null when creating a new issue
     * @param project The project where the attachment is to be placed. This is used to do security checks when creating an issue and
     *  there is no issue to run a check on. Will be ignored when issue is not null.
     * @return A {@link com.atlassian.jira.issue.attachment.TemporaryAttachment} containing details about where the temp attachment was created
     * @throws AttachmentException if there was an error saving the temporary attachment.
     */
    TemporaryAttachment createTemporaryAttachment(final MultiPartRequestWrapper requestWrapper, final String fileParamName,
    		final Long entityId, final String entityType, final Project project) throws ZAttachmentException;

    /**
     * Creates a temporary attachment on disk.  These attachment generally only live for the duration of a user's session
     * and will also be deleted on exit of the JVM. This method will not create a link to the issue yet, but simply
     * copy the attachment to a temp directory in the attachments folder and store all the relevant details in the
     * returned {@link com.atlassian.jira.issue.attachment.TemporaryAttachment} object
     *
     * @param stream the input stream for the attachment.
     * @param fileName the name of the attachment.
     * @param contentType the content type of the passed stream.
     * @param size the size of the passed stream.
     * @param issue The issue that this temporary attachment is for.  Can be null when creating a new issue.
     * @param project The project where the attachment is to be placed. This is used to do security checks when creating an issue and
     *  there is no issue to run a check on. Will be ignored when issue is not null.
     * @return A {@link com.atlassian.jira.issue.attachment.TemporaryAttachment} containing details about where the temp attachment was created
     * @throws AttachmentException if there was an error saving the temporary attachment.
     */
    TemporaryAttachment createTemporaryAttachment(final InputStream stream, final String fileName,
            final String contentType, long size, final Long entityId, final String entityType, final Project project) throws ZAttachmentException;

    /**
     * Determine whether an attachment exists and is valid (i.e. non-zero and contains no invalid characters)
     * @param requestWrapper the wrapper containing getFile() and getFilesystemName() describing the attachment
     * @param fileParamName the parameter in the wrapper to use to find attachment info
     * @param required whether having an valid and existent attachment is mandatory
     * @return whether the attachment is valid and exists
     * @throws AttachmentException if the attachment is zero-length, contains invalid characters, or simply doesn't exist
     * when required
     */
    boolean validateAttachmentIfExists(MultiPartRequestWrapper requestWrapper, String fileParamName, boolean required) throws ZAttachmentException;
}
