package com.thed.zephyr.je.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.model.StepResult;
import com.thed.zephyr.je.model.Teststep;
import com.thed.zephyr.je.service.StepResultManager;
import com.thed.zephyr.util.JiraUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import webwork.action.ActionContext;
import webwork.action.ServletActionContext;
import webwork.config.Configuration;
import webwork.multipart.MultiPartRequestWrapper;

import com.atlassian.core.util.FileSize;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CommentVisibility;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.Function;
import com.atlassian.jira.util.collect.CollectionUtil;
import com.atlassian.jira.util.json.JSONEscaper;
import com.atlassian.jira.web.action.issue.AbstractIssueSelectAction;
import com.atlassian.jira.web.action.issue.AttachTemporaryFile;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.je.attachment.TemporaryAttachment;
import com.thed.zephyr.je.attachment.ZAttachmentException;
import com.thed.zephyr.je.attachment.ZAttachmentManager;
import com.thed.zephyr.je.attachment.ZTemporaryAttachmentsMonitor;
import com.thed.zephyr.je.attachment.ZTemporaryAttachmentsMonitorLocator;
import com.thed.zephyr.je.service.ScheduleManager;
import com.thed.zephyr.je.service.TeststepManager;
import com.thed.zephyr.util.ApplicationConstants;

public class AttachFileAction extends  AbstractIssueSelectAction {
    protected final Logger log = Logger.getLogger(AttachFileAction.class);

	private final ZTemporaryAttachmentsMonitorLocator zTemporaryAttachmentsMonitorLocator;
    private ZAttachmentManager zAttachmentManager;
	private ScheduleManager scheduleManager;
	private StepResultManager stepResultManager;

    private long maxSize = Long.MIN_VALUE;
    private String[] filetoconvert;
    private static final String FILETOCONVERT = "filetoconvert";

	public static final String TEMP_FILENAME = "tempFilename";
    private Long projectId;
    private String commentLevel;
    private String comment;
    protected final CommentService commentService;
    private CommentVisibility commentVisibility;
    private final UserUtil userUtil;
    protected final ProjectRoleManager projectRoleManager;
    private final TeststepManager teststepManager;

	
    public AttachFileAction(final ProjectRoleManager projectRoleManager, 
            final CommentService commentService, 
            final UserUtil userUtil,ScheduleManager scheduleManager,final ZTemporaryAttachmentsMonitorLocator zTemporaryAttachmentsMonitorLocator,
            ZAttachmentManager zAttachmentManager,TeststepManager teststepManager, StepResultManager stepResultManager) {
        this.zAttachmentManager = zAttachmentManager;
        this.zTemporaryAttachmentsMonitorLocator = zTemporaryAttachmentsMonitorLocator;
        this.commentService=commentService;
        this.userUtil=userUtil;
        this.projectRoleManager=projectRoleManager;
        this.stepResultManager=stepResultManager;
        this.scheduleManager=scheduleManager;
        this.teststepManager = teststepManager;
    }

    @Override
    public String doDefault() throws Exception
    {
    	String entityType = ActionContext.getRequest().getParameter("entityType");
		ActionContext.getRequest().setAttribute("entityType", entityType);

		String entityId = ActionContext.getRequest().getParameter("entityId");
		ActionContext.getRequest().setAttribute("entityId", entityId);

        MutableIssue issue = null;
		if(StringUtils.isNotBlank(ActionContext.getRequest().getParameter("id"))) {
            issue = getIssueManager().getIssueObject(Long.valueOf(ActionContext.getRequest().getParameter("id")));
            ActionContext.getRequest().setAttribute("issue", issue);
        }

		Project currentProject = super.getSelectedProjectObject();
		
		if(issue != null){
			currentProject = issue.getProjectObject();
			if(!currentProject.getId().equals(issue.getProjectObject().getId()))
				super.setSelectedProject(currentProject);
		}
    	ActionContext.getRequest().setAttribute("projectId",currentProject.getId());
    	ActionContext.getRequest().setAttribute("projectKey",currentProject.getKey());

    	if(entityId != null && entityType.equals(ApplicationConstants.SCHEDULE_TYPE)){
    		clearTemporaryAttachmentsForEntity(entityId);
    	} else if(entityId != null && entityType.equals(ApplicationConstants.TESTSTEPRESULT_TYPE)){
    		clearTemporaryAttachmentsForEntity(entityId);
    	} else if(StringUtils.isNotBlank(entityId) && entityType.equals(ApplicationConstants.TEST_STEP_TYPE)){
            clearTemporaryAttachmentsForEntity(entityId);
        }
        return INPUT;
    }
    
    public String getMaxSize(Boolean display){
    	String maxSize = Configuration.getString(APKeys.JIRA_ATTACHMENT_SIZE);
    	if(display)
    		return FileUtils.byteCountToDisplaySize(Long.parseLong(maxSize));
    	return maxSize;
    }

    @Override
    protected void doValidation()
    {
        try {
            //attachmentService.canCreateAttachments(getJiraServiceContext(), getIssueObject());
        	super.doValidation(); 
        } catch (final Exception ex) {
            // Do nothing as error is added above
            return;
        }
        
        String entityId = ActionContext.getRequest().getParameter("entityId");
        String entityType = ActionContext.getRequest().getParameter("entityType");
        String projectIdString = ActionContext.getRequest().getParameter("projectId");
        Project currentProject = determineCurrentProject(projectIdString);
		
        // check that we can write to the attachment directory
        try {
            if(StringUtils.isBlank(entityId) && StringUtils.equalsIgnoreCase(entityType,ApplicationConstants.TEST_STEP_TYPE)) {
                log.info("Not creating the directory for the first time.");
            } else {
                log.info("creating the directory for the first time.");
                com.thed.zephyr.je.attachment.AttachmentUtils.checkValidAttachmentDirectory(Long.valueOf(entityId), entityType, currentProject);
            }
        } catch (ZAttachmentException e) {
            addErrorMessage(e.getMessage());
        }

		Schedule schedule = null;
        Integer issueId = null;
		if(entityType.equals(ApplicationConstants.TESTSTEPRESULT_TYPE)){
			// get the StepResult by given Id
			StepResult stepResult = stepResultManager.getStepResult(Integer.valueOf(entityId));
			if(stepResult != null) {
				schedule = scheduleManager.getSchedule(stepResult.getScheduleId());
				issueId = schedule.getIssueId();
			}
		} else if(entityType.equals(ApplicationConstants.SCHEDULE_TYPE)){
			// get the Schedule by given Id
			schedule = scheduleManager.getSchedule(Integer.valueOf(entityId));
			if(schedule != null) {
				issueId = schedule.getIssueId();
			}
		} else if(entityType.equalsIgnoreCase(ApplicationConstants.TEST_STEP_TYPE)) {
		    if(StringUtils.isNotBlank(entityId)) {
                Teststep testStep = teststepManager.getTeststep(Integer.valueOf(entityId));
                if(null != testStep)
                    issueId = testStep.getIssueId().intValue();
            }
		}

		if(issueId != null) {
			boolean hasViewIssuePermission = JiraUtil.hasIssueViewPermission(Long.valueOf(issueId),null,getLoggedInUser());
			if (!hasViewIssuePermission) {
				String errorMessage = getText("schedule.project.permission.error", "Attachment for EntityType  " + entityType, String.valueOf(projectId));
				addError(AttachTemporaryFile.TEMP_FILENAME, "Insufficient Issue permissions." + errorMessage);
			}
		}


		final List<Long> filesToConvert = getTemporaryFileIdsToConvert();
        if(filesToConvert.isEmpty()) {
            addError(AttachTemporaryFile.TEMP_FILENAME, getText("attachfile.error.filerequired"));
        }
        else {
            for (final Long tempAttachmentId : filesToConvert) {
                final TemporaryAttachment temporaryAttachment = getTemporaryAttachment(tempAttachmentId);
                if(temporaryAttachment == null || !temporaryAttachment.getFile().exists()) {
                    //Display these errors under the upload box as the checkbox will have gone away to were bits go
                    // when they die.
                    addError(AttachTemporaryFile.TEMP_FILENAME, getText("attachment.temporary.id.session.time.out"));
                    break;
                }
            }
        }
    }

      
    @Override
    //@RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        final Collection<ChangeItemBean> changeItemBeans = new ArrayList<ChangeItemBean>();
        final List<Long> fileIdsToConvert = getTemporaryFileIdsToConvert();

        String entityId = ActionContext.getRequest().getParameter("entityId");
        String entityType = ActionContext.getRequest().getParameter("entityType");
        String projectIdString = ActionContext.getRequest().getParameter("projectId");
        String comment = ActionContext.getRequest().getParameter("comment");

        Project currentProject = determineCurrentProject(projectIdString);

        
        if(entityId != null && entityType != null) {
			try {
	            final ZTemporaryAttachmentsMonitor temporaryAttachmentsMonitor = zTemporaryAttachmentsMonitorLocator.get(false);
	            if(temporaryAttachmentsMonitor != null) {
	                changeItemBeans.addAll(zAttachmentManager.convertTemporaryAttachments(getLoggedInUser(), Long.valueOf(entityId), entityType, comment,fileIdsToConvert, temporaryAttachmentsMonitor,currentProject));
	            } else {
	                addError(FILETOCONVERT, getText("attachment.temporary.session.time.out"));
	                return ERROR;
	            }
	        } catch (final Exception e) {
	        	log.fatal("", e);
	            addError(FILETOCONVERT, e.getMessage());
	            return ERROR;
	        }
	        clearTemporaryAttachmentsForEntity(entityId);
        }
        if (isInlineDialogMode()) {
            return returnComplete();
        }

        return getRedirect(redirectToAttachments());
    }

	/**
	 * @param projectIdString
	 * @return
	 */
	private Project determineCurrentProject(String projectIdString) {
		Project currentProject = super.getSelectedProjectObject();
        if(projectIdString != null){
        	setProjectId(new Long(projectIdString));
        	if(currentProject == null || !getProjectId().equals(currentProject.getId())){
        		currentProject = ComponentAccessor.getProjectManager().getProjectObj(getProjectId());
        	}
        }
		return currentProject;
	}

    private void clearTemporaryAttachmentsForEntity(String entityId) throws Exception
    {
        log.info("zTemporaryAttachmentsMonitorLocator : "+zTemporaryAttachmentsMonitorLocator);
        ZTemporaryAttachmentsMonitor locator = zTemporaryAttachmentsMonitorLocator.get(true);
        log.info("entity id ::" +entityId);
        if(StringUtils.isNotBlank(entityId)) {
            if(null == locator) {
                log.info("locator is null ::" +locator);
                locator = new ZTemporaryAttachmentsMonitor();
                log.info("locator is not null now::" +locator);
                locator.clearEntriesForEntity(Long.valueOf(entityId));
            }else {
                log.info("locator is not null ::" +locator);
                locator.clearEntriesForEntity(Long.valueOf(entityId));
            }
           // zTemporaryAttachmentsMonitorLocator.get(true).clearEntriesForEntity(Long.valueOf(entityId));
        }

    }

    
    public String getTargetUrl()
    {
        return isInlineDialogMode() ? redirectToIssue() : redirectToAttachments();
    }

    private String redirectToAttachments()
    {
        return "ManageAttachments.jspa?id=" + getIssue().getLong("id");
    }

    private String redirectToIssue()
    {
        return getViewUrl();
    }


    public String[] getFiletoconvert()
    {
        return filetoconvert;
    }

    public void setFiletoconvert(final String[] filetoconvert)
    {
        this.filetoconvert = filetoconvert;
    }

    public boolean isFileToConvertChecked(final Long tempFileId)
    {
        final List<Long> fileIds = getTemporaryFileIdsToConvert();
        return fileIds.contains(tempFileId);
    }

    public long getMaxSize()
    {
        if (maxSize != Long.MIN_VALUE) {
            return maxSize;
        }

        try {
            maxSize = Long.parseLong(getApplicationProperties().getString(APKeys.JIRA_ATTACHMENT_SIZE));
        } catch (NumberFormatException e) {
            maxSize = -1;
        }
        return maxSize;
    }

    public String getMaxSizePretty()
    {
        final long maxSize = getMaxSize();
        if (maxSize > 0) {
            return FileSize.format(maxSize);
        }
        else {
            return "Unknown?";
        }
    }

    private List<Long> getTemporaryFileIdsToConvert()
    {
        final String[] strings = getFiletoconvert();
        if(strings == null)
        {
            return Collections.emptyList();
        }
        final List<String> fileIdStrings = Arrays.asList(strings);
        return CollectionUtil.transform(fileIdStrings, new Function<String, Long>()
        {
            @Override
			public Long get(final String input)
            {
                return Long.parseLong(input);
            }
        });
    }


    public List<TemporaryAttachment> getTemporaryAttachments() throws Exception
    {
        final ZTemporaryAttachmentsMonitor temporaryAttachmentsMonitor = zTemporaryAttachmentsMonitorLocator.get(true);
        return new ArrayList<TemporaryAttachment>(temporaryAttachmentsMonitor.getByEntityId(getId()));
    }

    private TemporaryAttachment getTemporaryAttachment(final Long temporaryAttachmentId)
    {
        final ZTemporaryAttachmentsMonitor temporaryAttachmentsMonitor = zTemporaryAttachmentsMonitorLocator.get(false);
        if (temporaryAttachmentsMonitor != null)
        {
            return temporaryAttachmentsMonitor.getById(temporaryAttachmentId);
        }
        return null;
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
    
	    public String getSelectedLevelName()
	    {
	        if(getCommentLevel() == null)
	        {
	            return getText("security.level.viewable.by.all");
	        }

	        final Collection<ProjectRole> roleLevels = getRoleLevels();
	        for (ProjectRole roleLevel : roleLevels)
	        {
	            if(getCommentLevel().equals("role:" + roleLevel.getId().toString()))
	            {
	                return getText("security.level.restricted.to", TextUtils.htmlEncode(roleLevel.getName()));
	            }
	        }

	        final Collection<String> groupLevels = getGroupLevels();
	        for (String groupLevel : groupLevels)
	        {
	            if(getCommentLevel().equals("group:" + groupLevel))
	            {
	                return getText("security.level.restricted.to", TextUtils.htmlEncode(groupLevel));
	            }
	        }
	        return getText("security.level.viewable.by.all");
	    }
	    
	    public String getCommentLevel()
	    {
	        return commentLevel;
	    }

	    public void setCommentLevel(String commentLevel)
	    {
	        this.commentLevel = commentLevel;
	    }

	    public void setComment(String comment)
	    {
	        this.comment = comment;
	    }

	    public Collection getGroupLevels()
	    {
	        Collection groups;
	        if (getLoggedInUser() == null || !commentService.isGroupVisiblityEnabled())
	        {
	            groups = Collections.EMPTY_LIST;
	        }
	        else
	        {
	            groups = userUtil.getGroupNamesForUser(getLoggedInUser().getName());
	        }
	        return groups;
	    }

	    public Collection<ProjectRole> getRoleLevels()
	    {
	        Collection<ProjectRole> roleLevels = Collections.emptyList();;
	        if (commentService.isProjectRoleVisiblityEnabled())
	        {
	            if(StringUtils.isNotBlank(ActionContext.getRequest().getParameter("id"))) {
                    MutableIssue issue = getIssueManager().getIssueObject(Long.valueOf(ActionContext.getRequest().getParameter("id")));
                    if(issue != null) {
                        roleLevels = projectRoleManager.getProjectRoles(getLoggedInUser(), issue.getProjectObject());
                    }
                }
	        }
	        return roleLevels;
	    }

	    protected CommentVisibility getCommentVisibility()
	    {
	        if (commentVisibility == null)
	        {
	            commentVisibility = new CommentVisibility(commentLevel);
	        }
	        return commentVisibility;
	    }

	    public boolean isLevelSelected(String visibilityLevel)
	    {
	        return getCommentLevel() != null && getCommentLevel().equals(visibilityLevel);
	    }

		public String getComment() {
			return comment;
		}

		public CommentService getCommentService() {
			return commentService;
		}
}
