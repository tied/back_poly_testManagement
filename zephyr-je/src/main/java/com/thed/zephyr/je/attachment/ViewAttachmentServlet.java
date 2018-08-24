package com.thed.zephyr.je.attachment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.TeststepManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.AttachmentNotFoundException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.BrowserUtils;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.util.io.InputStreamConsumer;
import com.atlassian.jira.web.servlet.HttpResponseHeaders;
import com.atlassian.jira.web.servlet.InvalidAttachmentPathException;
import com.thed.zephyr.je.model.Attachment;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.ApplicationConstants;

public class ViewAttachmentServlet extends AbstractViewFileServlet
{
	private static final long serialVersionUID = -2228425587579186678L;
	private final MimeSniffingKit mimeSniffingKit;
    private final ZAttachmentManager zAttachmentManager;
    private final ProjectManager projectManager;
    private final ScheduleManager scheduleManager;
    private final StepResultManager stepResultManager;
    private final IssueManager issueManager;
    private final TeststepManager teststepManager;
    
    @Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
	}

	public ViewAttachmentServlet(final ZAttachmentManager zAttachmentManager,
			final MimeSniffingKit mimeSniffingKit,
			final ScheduleManager scheduleManager,
			final ProjectManager projectManager,
			final StepResultManager stepResultManager,
			final IssueManager issueManager, final TeststepManager teststepManager) {
		this.zAttachmentManager = zAttachmentManager;
		this.mimeSniffingKit = mimeSniffingKit;
		this.scheduleManager = scheduleManager;
		this.projectManager = projectManager;
		this.issueManager = issueManager;
		this.stepResultManager = stepResultManager;
		this.teststepManager = teststepManager;
	}

     
	ApplicationProperties getApplicationProperties() {
		return ComponentAccessor.getApplicationProperties();
	}


   /**
     * Returns the file of the attachment.
     *
     * @param request  HTTP request
     * @param response HTTP response
     * @return attachment file
     * @throws DataAccessException if attachment or user cannot be retrieved due to some kind of db access problem.
     * @throws PermissionException if user is denied permission to see the attachment
     */
	protected File getFileName(HttpServletRequest request, HttpServletResponse response) throws DataAccessException, PermissionException
    {
        Attachment attachment = getAttachment(request);

        if (attachment ==null || StringUtils.isBlank(getUserName()))
        {
            throw new PermissionException("You do not have permissions to view this issue");
        }
        Project project = null;
        if(StringUtils.equalsIgnoreCase(ApplicationConstants.SCHEDULE_TYPE, attachment.getType())) {
        	Schedule schedule = scheduleManager.getSchedule(attachment.getEntityId().intValue());
            project = projectManager.getProjectObj(schedule.getProjectId());
        } else if(StringUtils.equalsIgnoreCase(ApplicationConstants.TESTSTEPRESULT_TYPE, attachment.getType())) {
        	StepResult stepResult = stepResultManager.getStepResult(attachment.getEntityId().intValue());
            project = projectManager.getProjectObj(stepResult.getProjectId());
        } else if(StringUtils.equalsIgnoreCase(ApplicationConstants.TEST_STEP_TYPE, attachment.getType())) {
            Teststep teststep = teststepManager.getTeststep(attachment.getEntityId().intValue());
            project = issueManager.getIssueObject(teststep.getIssueId()).getProjectObject();
        }
        return AttachmentUtils.getAttachmentFile(attachment.getEntityId(),attachment.getType(),attachment,project);
    }
    
    /**
     * Gets Attachment Object
     * @param request
     * @return
     * @throws AttachmentNotFoundException
     */
    protected Attachment getAttachment(HttpServletRequest request) throws AttachmentNotFoundException {
        String id = request.getParameter("id");
        String fileName = request.getParameter("name");

        
        if(StringUtils.isBlank(id)) {
            throw new AttachmentNotFoundException("Invalid Input Request. Please correct the Input parameters and try again");
        }

        if(StringUtils.isBlank(fileName)) {
            throw new AttachmentNotFoundException("Invalid FileName in Input. Please correct the Input parameters and try again");
        }
        try {
			return zAttachmentManager.getAttachment(Long.valueOf(id));
		} catch (Exception e) {
			log("Error Retrieving Attachment:",e);
			throw new AttachmentNotFoundException(e);
		}	
	}

	/**
     * Looks up the attachment by reading the id from the query string.
     *
     * @param query eg. '/10000/foo.txt'
     * @return attachment found
     * @throws Exception 
     * @throws AttachmentNotFoundException 
     */
	protected Attachment getAttachment(String query)
			throws AttachmentNotFoundException {
		int x = query.indexOf('/', 1);
		final String idStr = query.substring(1, x);
		Long id;
		try {
			id = new Long(idStr);
		} catch (NumberFormatException e) {
			throw new AttachmentNotFoundException(idStr);
		}
		if (query.indexOf('/', x + 1) != -1) {
			// JRA-14580. only one slash is allowed to prevent infinite
			// recursion by web crawlers.
			throw new AttachmentNotFoundException(idStr);
		}

		try {
			return zAttachmentManager.getAttachment(id);
		} catch (Exception e) {
			log("Error Retrieving Attachment:", e);
			throw new AttachmentNotFoundException(e);
		}
	}

    /**
     * Sets the content type, content length and "Content-Disposition" header
     * of the response based on the values of the attachement found.
     *
     * @param request  HTTP request
     * @param response HTTP response
     */
    @Override
	protected void setResponseHeaders(HttpServletRequest request, HttpServletResponse response) throws AttachmentNotFoundException, IOException
    {
        Attachment attachment = getAttachment(request);
        response.setContentType(attachment.getMimetype());
        response.setContentLength(attachment.getFilesize().intValue());
        
        Project project = null;
        if(StringUtils.equalsIgnoreCase(ApplicationConstants.SCHEDULE_TYPE,attachment.getType())) {
        	Schedule schedule = scheduleManager.getSchedule(attachment.getEntityId().intValue());
        	project = projectManager.getProjectObj(schedule.getProjectId());
        } else if(StringUtils.equalsIgnoreCase(ApplicationConstants.TESTSTEPRESULT_TYPE,attachment.getType())) {
        	StepResult stepResult = stepResultManager.getStepResult(attachment.getEntityId().intValue());
        	Schedule schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
        	Issue issue = issueManager.getIssueObject(Long.valueOf(schedule.getIssueId()));
        	project = issue.getProjectObject();
        } else if(StringUtils.equalsIgnoreCase(ApplicationConstants.TEST_STEP_TYPE,attachment.getType())) {
            Teststep teststep = teststepManager.getTeststep(attachment.getEntityId().intValue());
            project = issueManager.getIssueObject(teststep.getIssueId()).getProjectObject();
	    }
        String userAgent = request.getHeader(BrowserUtils.USER_AGENT_HEADER);
        final ApplicationProperties ap = getApplicationProperties();
        String disposition = getContentDisposition(attachment, userAgent,project);

        final String codedName = JiraUrlCodec.encode(attachment.getFileName(), true);
        final String jiraEncoding = ap.getEncoding();
        // note the special *= syntax is used for embedding the encoding that the filename is in as per RFC 2231
        // http://www.faqs.org/rfcs/rfc2231.html
        response.setHeader("Content-Disposition", disposition + "; filename*=" + jiraEncoding + "''" + codedName + ";");

        HttpResponseHeaders.cachePrivatelyForAboutOneYear(response);
    }

	private String getContentDisposition(final Attachment attachment,
			final String userAgent, final Project project) throws IOException {
		return mimeSniffingKit.getContentDisposition(attachment, userAgent,
				project);
	}

	@Override
	protected void getInputStream(File file,
			InputStreamConsumer<Void> consumer)
			throws InvalidAttachmentPathException, DataAccessException,
			IOException, PermissionException {
        streamAttachmentContent(file, consumer);
	}

	
	private <T> T streamAttachmentContent(final File file, final InputStreamConsumer<T> consumer) throws IOException, PermissionException {
		InputStream inputStream = new FileInputStream(file);
		try {
			inputStream = new BufferedInputStream(inputStream);
			return consumer.withInputStream(inputStream);
		} finally {
			IOUtils.closeQuietly(inputStream);
		}
	}
}


