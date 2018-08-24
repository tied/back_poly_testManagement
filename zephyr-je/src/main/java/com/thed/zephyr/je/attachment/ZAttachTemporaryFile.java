package com.thed.zephyr.je.attachment;

import java.util.Iterator;

import net.java.ao.RawEntity;

import webwork.action.ServletActionContext;
import webwork.multipart.MultiPartRequestWrapper;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.json.JSONEscaper;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;

/**
 * Used by inline-file-attach.js to upload temporary attachments that can then be converted to real attachments
 * lateron.
 *
 * @since 4.2
 */

public class ZAttachTemporaryFile extends AbstractIssueSelectAction
{
    public static final String TEMP_FILENAME = "tempFilename";

    protected final transient ZWebAttachmentManager zWebAttachmentManager;
    protected final transient ApplicationProperties applicationProperties;
    private boolean create = false;
    private Long projectId;
    private Long entityId;
    private String entityType;
    private Long id;

    private TemporaryAttachment temporaryAttachment;
    private ScheduleManager scheduleManager;
    private TeststepManager teststepManager;

    
    public ZAttachTemporaryFile(final SubTaskManager subTaskManager, final ZWebAttachmentManager zWebAttachmentManager,
            final ApplicationProperties applicationProperties,final ScheduleManager scheduleManager,final TeststepManager teststepManager)
    {
        super(subTaskManager);
        this.zWebAttachmentManager = zWebAttachmentManager;
        this.applicationProperties = applicationProperties;
        this.scheduleManager=scheduleManager;
        this.teststepManager=teststepManager;
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        try
        {
        	this.temporaryAttachment = zWebAttachmentManager.createTemporaryAttachment(getMultipart(), TEMP_FILENAME, entityId,entityType, getProjectObject());
        }
        catch (final ZAttachmentException e)
        {
            addErrorMessage(e.getMessage());
        }
        return "temp_file_json";
    }


	public TemporaryAttachment getTemporaryAttachment()
    {
        return temporaryAttachment;
    }

    public boolean isCreate()
    {
        return create;
    }

    public void setCreate(final boolean create)
    {
        this.create = create;
    }

    protected MultiPartRequestWrapper getMultipart()
    {
        return ServletActionContext.getMultiPartRequest();
    }

    public String encode(final String text)
    {
        return JSONEscaper.escape(text);
    }

    public Project getProjectObject()
    {
        if(projectId != null)
        {
            return getProjectManager().getProjectObj(projectId);
        }
        return null;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(final Long projectId)
    {
        this.projectId = projectId;
    }

    public String getErrorMessage()
    {
        if(!getErrorMessages().isEmpty())
        {
            final StringBuilder errorMsgs = new StringBuilder();
            final Iterator errorIterator = getErrorMessages().iterator();
            while (errorIterator.hasNext())
            {
                final String error = (String) errorIterator.next();
                errorMsgs.append(error);
                if(errorIterator.hasNext())
                {
                    errorMsgs.append(", ");
                }
            }

            return errorMsgs.toString();
        }
        return "";
    }

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getEntityId() {
		return entityId;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}
}
