package com.thed.zephyr.je.attachment;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.UUID;

import net.java.ao.RawEntity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import webwork.config.Configuration;
import webwork.multipart.MultiPartRequestWrapper;

import com.atlassian.core.util.FileSize;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.LimitedOutputStream;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.dbc.Assertions;
import com.atlassian.jira.web.util.FileNameCharacterCheckerUtil;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.Teststep;

public class ZDefaultWebAttachmentManager implements ZWebAttachmentManager {
	    private static final Logger log = Logger.getLogger(ZDefaultWebAttachmentManager.class);

	    private final FileNameCharacterCheckerUtil fileNameCharacterCheckerUtil;
	    private final I18nHelper.BeanFactory beanFactory;
	    private final JiraAuthenticationContext authenticationContext;
	    private final ZTemporaryAttachmentsMonitorLocator locator;
	    private final AttachmentService service;
	    private final ProjectManager projectManager;
	    private final IssueManager issueManager;

	    
	    public ZDefaultWebAttachmentManager(final I18nHelper.BeanFactory beanFactory,
	            final JiraAuthenticationContext authenticationContext, ZTemporaryAttachmentsMonitorLocator locator,
	            final AttachmentService service,final ProjectManager projectManager,IssueManager issueManager)
	    {
	        this.beanFactory = beanFactory;
	        this.authenticationContext = authenticationContext;
	        this.locator = locator;
	        this.service = service;
	        this.projectManager=projectManager;
	        this.issueManager=issueManager;
	        this.fileNameCharacterCheckerUtil = new FileNameCharacterCheckerUtil();
	    }


	    @Override
		public TemporaryAttachment createTemporaryAttachment(final MultiPartRequestWrapper requestWrapper, final String fileParamName,
	            final Long entityId, final String entityType, final Project project) throws ZAttachmentException
	    {
	        if (entityType == null && entityType == null && project == null)
	        {
	            throw new IllegalArgumentException("'id' and 'project' cannot be null at the same time.");
	        }

	        //Is the mult-part request in a good state. This will also check the file size. We need to do this
	        //first becasue the file will be null if the attachment is oversized.
	        validateAttachmentIfExists(requestWrapper, fileParamName, false);

	        final File file = requestWrapper.getFile(fileParamName);
	        if (file == null) //there's not much we can do here.
	        {
	            log.warn(format("Could not create attachment. No file found in MultiPartRequestWrapper. File param name: %s. Request wrapper filenames: %s.", fileParamName, CollectionBuilder.list(requestWrapper.getFileNames())));
	            return null;
	        }

	        //check permissions.
	        assertCanAttach(entityId,entityType,project);

	        final String filename = requestWrapper.getFilesystemName(fileParamName);
	        final String contentType = requestWrapper.getContentType(fileParamName);

	        //create the temporary attachment file with the id prefixed to avoid name clashes when multiple people are
	        //uploading files.
	        final UniqueFile uniqueFile = createUniqueFile(file.getName());
	        createTemporaryAttachmentOnDisk(file, uniqueFile.getFile());
	        
	      //  final int entityId = rawEntity == null ? null : RawEntityFactory.getID(rawEntity);
	        final TemporaryAttachment temporaryAttachment = new TemporaryAttachment(uniqueFile.getId(), entityId,entityType, uniqueFile.getFile(), filename, contentType);
	        addToMonitor(temporaryAttachment);
	        return temporaryAttachment;
	    }

	    @Override
	    public TemporaryAttachment createTemporaryAttachment(InputStream stream, String fileName, String contentType,
	            long size, Long entityId,String entityType, Project project) throws ZAttachmentException
	    {
	        Assertions.notBlank("fileName", fileName);
	        Assertions.notBlank("contentType", contentType);
	        Assertions.notNull("stream", stream);
	        
	        if (entityId == null && entityType == null && project == null)
	        {
	            throw new IllegalArgumentException("'Entity' and 'project' cannot be null .");
	        }
	        if (size < 0)
	        {
	            throw new IllegalArgumentException("size must be >= 0.");
	        }

	        if (size == 0)
	        {
	            throw new ZAttachmentException(getI18n().getText("attachfile.error.file.zero", fileName));
	        }
	        else
	        {
	            long maxAttachmentSize = getMaxAttachmentSize();
	            if (size > maxAttachmentSize)
	            {
	                throw new ZAttachmentException(getI18n().getText("attachfile.error.file.large", fileName, FileSize.format(maxAttachmentSize)));
	            }
	        }
	        assertCanAttach(entityId,entityType, project);
	        assertFileNameIsValid(fileName);
	        final UniqueFile uniqueFile = createUniqueFile(fileName);
	        LimitedOutputStream limitedOutput = null;
	        try
	        {
	            FileOutputStream fos = new FileOutputStream(uniqueFile.getFile());
	            limitedOutput = wrapOutputStream(fos, size);

	            IOUtils.copy(stream, limitedOutput);

	            //We want the close here. If we get an error flusing to the disk then we really need to know.
	            limitedOutput.close();

	            //This can only happen when the stream is too small. If the stream is too big we will get a
	            //TooBigIOException which is caught below.
	            if (limitedOutput.getCurrentLength() != size)
	            {
	                deleteFileIfExists(uniqueFile.getFile());
	                String text;
	                if (limitedOutput.getCurrentLength() == 0)
	                {
	                    text = getI18n().getText("attachfile.error.io.bad.size.zero", fileName);
	                }
	                else
	                {
	                    text = getI18n().getText("attachfile.error.io.bad.size", fileName,
	                            String.valueOf(limitedOutput.getCurrentLength()), String.valueOf(size));
	                }
	                throw new ZAttachmentException(text);
	            }

	            final TemporaryAttachment temporaryAttachment = new TemporaryAttachment(uniqueFile.getId(), entityId, entityType, uniqueFile.getFile(), fileName, contentType);
	            addToMonitor(temporaryAttachment);
	            return temporaryAttachment;
	        }
	        catch (IOException e)
	        {
	            IOUtils.closeQuietly(limitedOutput);
	            deleteFileIfExists(uniqueFile.getFile());

	            if (e instanceof LimitedOutputStream.TooBigIOException)
	            {
	                LimitedOutputStream.TooBigIOException tooBigIOException = (LimitedOutputStream.TooBigIOException) e;
	                throw new ZAttachmentException(getI18n().getText("attachfile.error.file.large", fileName, FileSize.format(tooBigIOException.getNextSize())));
	            }
	            else
	            {
	                //JRADEV-5540: This is probably caused by some kind of client i/o error (e.g. disconnect). Not much point of logging it as we send
	                // back an error reason anyways.
	                log.debug("I/O error occured while attaching file.", e);
	                throw new ZAttachmentException(getI18n().getText("attachfile.error.io.error", fileName, e.getMessage()), e);
	            }
	        }
	    }

	    @Override
		public boolean validateAttachmentIfExists(final MultiPartRequestWrapper requestWrapper, final String fileParamName, final boolean required)
	            throws ZAttachmentException
	    {
	        final File file = requestWrapper.getFile(fileParamName);
	        final String filename = requestWrapper.getFilesystemName(fileParamName);

	        return assertAttachmentIfExists(file, filename, required, requestWrapper.getContentLength());
	    }

	    private static void deleteFileIfExists(final File file)
	    {
	        if (file.exists() && !file.delete())
	        {
	            log.warn("Unable to delete file '" + file + "'.");
	        }
	    }

	    void addToMonitor(TemporaryAttachment temporaryAttachment) throws ZAttachmentException
	    {
	        final ZTemporaryAttachmentsMonitor attachmentsMonitor = locator.get(true);
	        if (attachmentsMonitor != null)
	        {
	            attachmentsMonitor.add(temporaryAttachment);
	        }
	        else
	        {
	            deleteFileIfExists(temporaryAttachment.getFile());
	            throw new ZAttachmentException(getI18n().getText("attachfile.error.session.error", temporaryAttachment.getFilename()));
	        }
	    }

	    private void createTemporaryAttachmentOnDisk(File file, File targetFile) throws ZAttachmentException
	    {
	        try
	        {
	            FileUtils.moveFile(file, targetFile);
	        }
	        catch (IOException e)
	        {
	            final String message = getI18n().getText("attachfile.error.move", file.getAbsolutePath(), targetFile.getAbsolutePath(), e);
	            log.error(message, e);
	            throw new ZAttachmentException(message);
	        }
	    }

	    LimitedOutputStream wrapOutputStream(OutputStream fos, long size)
	    {
	        return new LimitedOutputStream(new BufferedOutputStream(fos), size);
	    }

	    //Some characters are invalid on certain filesystems, JRA-5864, JRA-5595, JRA-6141
	    void assertFileNameIsValid(final String filename) throws ZAttachmentException
	    {
	        if (StringUtils.isBlank(filename))
	        {
	            throw new ZAttachmentException(getI18n().getText("attachfile.error.no.name"));
	        }

	        final String invalidChar = fileNameCharacterCheckerUtil.assertFileNameDoesNotContainInvalidChars(filename);
	        if (invalidChar != null)
	        {
	            throw new ZAttachmentException(getI18n().getText("attachfile.error.invalidcharacter", filename, invalidChar));
	        }
	    }

	    //package level protected for tests.
	    boolean assertAttachmentIfExists(final File file, final String fileName, final boolean required, final int contentLength)
	            throws ZAttachmentException
	    {
	        final boolean exists = exists(file, fileName, contentLength);
	        if (!exists)
	        {
	            if (required)
	            {
	                throw new ZAttachmentException(getI18n().getText("attachfile.error.filerequired"));
	            }
	        }
	        else
	        {
	            assertFileNameIsValid(fileName);
	        }

	        return exists;
	    }

	    private boolean exists(final File file, final String fileName, final int contentLength) throws ZAttachmentException
	    {
	        if (file == null)
	        {
	            if (fileName != null)
	            {
	                final long attachmentSize = getMaxAttachmentSize();
	                //now that we're only uploading a single file, the contentLength of the request is a pretty good
	                //indicator if the attachment was too large. If it's bigger than the max size allowed we
	                //return an error specific to this.
	                if (contentLength > attachmentSize)
	                {
	                    throw new ZAttachmentException(getI18n().getText("attachfile.error.file.large", fileName, FileSize.format(attachmentSize)));
	                }
	                else
	                {
	                    throw new ZAttachmentException(getI18n().getText("attachfile.error.file.zero", fileName));
	                }
	            }
	            else
	            {
	                return false;
	            }
	        }
	        else if (file.length() == 0)
	        {
	            throw new ZAttachmentException(getI18n().getText("attachfile.error.file.zero", fileName));
	        }
	        return true;
	    }

	    ///CLOVER:OFF
	    private long getUUID()
	    {
	        return Math.abs(UUID.randomUUID().getLeastSignificantBits());
	    }

	    UniqueFile createUniqueFile(String fileName)
	    {
	        final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
	        long uniqueId;
	        File tempAttachmentFile;
	        do
	        {
	            //if the file already exists, choose a new UUID to avoid clashes!
	            uniqueId = getUUID();
	            tempAttachmentFile = new File(tmpDir, uniqueId + "_" + fileName);
	        }
	        while (tempAttachmentFile.exists());

	        return new UniqueFile(tempAttachmentFile, uniqueId);
	    }

	    long getMaxAttachmentSize()
	    {
	        return Long.parseLong(Configuration.getString(APKeys.JIRA_ATTACHMENT_SIZE));
	    }

	    I18nHelper getI18n()
	    {
	        return beanFactory.getInstance(authenticationContext.getLocale());
	    }

	    
	    /** Attachment Directory Check needs to be added here **/
	    void assertAttachmentDirectoryExists(Long entityId, String entityType,Project project) throws ZAttachmentException
	    {
	        AttachmentUtils.checkValidAttachmentDirectory(entityId,entityType,project);
	    }
	    

	    void assertTemporaryDirectoryExists() throws ZAttachmentException
	    {
	        AttachmentUtils.checkValidTemporaryAttachmentDirectory();
	    }

	    ///CLOVER:ON

	    void assertCanAttach(Long entityId,String entityType, Project project) throws ZAttachmentException
	    {
	        if (entityId == null || entityType == null) {
	            //creating a new issue so when attaching temp files we don't yet have an issue yet to check permissions
	            //against, so we check the project
	            JiraServiceContext context = createServiceContext();
	            service.canCreateAttachments(context, project);
	            throwForFirstError(context.getErrorCollection());

	            //This throws an exception on failure.
	            assertTemporaryDirectoryExists();
	        } else {
	            JiraServiceContext context = createServiceContext();
	            //service.canCreateTemporaryAttachments(context, schedule);
	            throwForFirstError(context.getErrorCollection());

	            // check that we can write to the attachment directory
            	assertAttachmentDirectoryExists(entityId,entityType,project);
	        }
	    }

	    private JiraServiceContext createServiceContext()
	    {
	        return new JiraServiceContextImpl(authenticationContext.getLoggedInUser(),
	                new SimpleErrorCollection(), authenticationContext.getI18nHelper());
	    }

	    private static void throwForFirstError(ErrorCollection collection) throws ZAttachmentException
	    {
	        if (collection.hasAnyErrors())
	        {
	            String message = getFirstElement(collection.getErrorMessages());
	            if (message == null)
	            {
	                message = getFirstElement(collection.getErrors().values());
	            }

	            throw new ZAttachmentException(message);
	        }
	    }

	    private static <T> T getFirstElement(Collection<? extends T> values)
	    {
	        if (!values.isEmpty())
	        {
	            return values.iterator().next();
	        }
	        else
	        {
	            return null;
	        }
	    }

	    static class UniqueFile
	    {
	        private final File file;
	        private final long id;

	        UniqueFile(File file, long id)
	        {
	            this.file = file;
	            this.id = id;
	        }

	        public File getFile()
	        {
	            return file;
	        }

	        public long getId()
	        {
	            return id;
	        }
	    }
	}
