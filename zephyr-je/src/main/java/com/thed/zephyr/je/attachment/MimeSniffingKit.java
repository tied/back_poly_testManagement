package com.thed.zephyr.je.attachment;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.util.IOUtil;
import com.thed.zephyr.je.model.Attachment;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class can sniff a file according to the current JIRA settings and determine jow an atatchment should be handled
 *
 * @since v4.1
 */
public class MimeSniffingKit
{
    public static final String CONTENT_DISPOSITION_ATTACHMENT = "attachment";
    public static final String CONTENT_DISPOSITION_INLINE = "inline";

    private static final Logger log = Logger.getLogger(MimeSniffingKit.class);
    private final ApplicationProperties applicationProperties;
    private HostileAttachmentsHelper attachmentHelper;

    public MimeSniffingKit(ApplicationProperties applicationProperties)
    {
        this.applicationProperties = applicationProperties;
        this.attachmentHelper = new HostileAttachmentsHelper();
    }

    /**
     * This will suggest a content disposition type (inline or attachment) for the given {@link
     * com.atlassian.jira.issue.attachment.Attachment}, respecting the settings in JIRA and taking IE badness into
     * account.
     *
     * @param attachment the Attachment in play
     * @param userAgent  the browser in play
     *
     * @return either INLINE or ATTACHMENT ready for a Content-Disposition header
     *
     * @throws IOException if stuff goes wrong
     */
    public String getContentDisposition(Attachment attachment, String userAgent, Project project) throws IOException
    {
        final File attachmentFile = getFileForAttachment(attachment,project);
        return getContentDisposition(attachment.getFileName(), attachment.getMimetype(), userAgent, attachmentFile);

    }

    /**
     * This will suggest a content disposition type (inline or attachment) for the given file, respecting the settings
     * in JIRA and taking IE badness into account.
     *
     * @param fileName        the name of the file
     * @param mimeContentType the exisiting content type
     * @param userAgent       the browser in play
     * @param file            the File to be checked
     *
     * @return either INLINE or ATTACHMENT ready for a Content-Disposition header
     *
     * @throws IOException if stuff goes wrong
     */
    public String getContentDisposition(String fileName, String mimeContentType, String userAgent, File file)
            throws IOException
    {
        String mimeSniffingPolicy = getMimeSniffingPolicy();
        boolean forceDownload = false;
        if (mimeSniffingPolicy.equalsIgnoreCase(APKeys.MIME_SNIFFING_OWNED))
        {
            log.debug("Mime sniffing policy is insecure, attachment will always be displayed inline");
        }
        if (!mimeSniffingPolicy.equalsIgnoreCase(APKeys.MIME_SNIFFING_OWNED) && isExecutableContent(fileName, mimeContentType))
        {
            // only in owned mode we allow inline html
            forceDownload = true;
            log.debug("Attachment \"" + fileName + "\" (" + mimeContentType + ")" + " presents as executable content, forcing download.");

        }
        else if (mimeSniffingPolicy.equalsIgnoreCase(APKeys.MIME_SNIFFING_PARANOID))
        {
            forceDownload = true;
        }
        return forceDownload ? CONTENT_DISPOSITION_ATTACHMENT : CONTENT_DISPOSITION_INLINE;
    }

    File getFileForAttachment(final Attachment attachment, Project project)
    {
        return AttachmentUtils.getAttachmentFile(attachment, project);
    }

    /**
     * Gets the bytes out of the given attachment.
     *
     * @param attachmentFile the file.
     * @param numBytes       the maximum number of bytes to return.
     *
     * @return the first numBytes bytes (or all if there are less in the file)
     *
     * @throws IOException if the attachment file cannot be read.
     */
    byte[] getLeadingFileBytes(File attachmentFile, int numBytes) throws IOException
    {
        FileInputStream stream = null;
        try
        {
            stream = new FileInputStream(attachmentFile);
            return IOUtil.getLeadingBytes(stream, numBytes);
        }
        finally
        {
            IOUtil.shutdownStream(stream);
        }
    }


    boolean isExecutableContent(String name, String contentType)
    {
        return attachmentHelper.isExecutableFileExtension(name) || attachmentHelper.isExecutableContentType(contentType);
    }


    private String getMimeSniffingPolicy()
    {
        String mimeSniffingPolicy = applicationProperties.getDefaultBackedString(APKeys.JIRA_OPTION_IE_MIME_SNIFFING);
        if (mimeSniffingPolicy == null)
        {
            mimeSniffingPolicy = APKeys.MIME_SNIFFING_WORKAROUND; // hard-coded default
            log.warn("Missing MIME sniffing policy application property " + APKeys.JIRA_OPTION_IE_MIME_SNIFFING
                    + " ! Defaulting to " + APKeys.MIME_SNIFFING_WORKAROUND);
        }
        if (!(APKeys.MIME_SNIFFING_OWNED.equalsIgnoreCase(mimeSniffingPolicy)
                || APKeys.MIME_SNIFFING_PARANOID.equalsIgnoreCase(mimeSniffingPolicy)
                || APKeys.MIME_SNIFFING_WORKAROUND.equalsIgnoreCase(mimeSniffingPolicy)))
        {
            log.warn("MIME sniffing policy application property is invalid: " + mimeSniffingPolicy
                    + " ! Defaulting to " + APKeys.MIME_SNIFFING_WORKAROUND);
            mimeSniffingPolicy = APKeys.MIME_SNIFFING_WORKAROUND; // hard-coded default
        }
        return mimeSniffingPolicy;
    }

}