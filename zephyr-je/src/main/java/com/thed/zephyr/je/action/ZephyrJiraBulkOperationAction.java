package com.thed.zephyr.je.action;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import webwork.action.ActionContext;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bulkedit.operation.BulkEditAction;
import com.atlassian.jira.bulkedit.operation.ProgressAwareBulkOperation;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.jira.web.action.issue.bulkedit.AbstractBulkOperationDetailsAction;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.jira.web.bean.BulkEditBeanSessionHelper;
import com.thed.zephyr.je.operation.BulkAddTestBean;
import com.thed.zephyr.je.operation.ZephyrAddTestBulkOperation;

@SuppressWarnings("serial")
public class ZephyrJiraBulkOperationAction extends AbstractBulkOperationDetailsAction implements InitializingBean, DisposableBean {
    protected final Logger log = Logger.getLogger(ZephyrJiraBulkOperationAction.class);

	
	private Integer versionId;
	private Integer cycleId;
	private Integer projId;
    private Integer folderId;

	public ZephyrJiraBulkOperationAction(SearchService jiraSearchService,
			BulkEditBeanSessionHelper bulkEditBeanSessionHelper,
			TaskManager taskManager) {
		super(jiraSearchService, bulkEditBeanSessionHelper, taskManager, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper());
	}
	

    public boolean isHasAvailableActions() throws Exception {
        return getZephyrBulkOperation().canPerform(getBulkEditBean(), getLoggedInUser());
    }

    public String getOperationDetailsActionName() {
        return getZephyrBulkOperation().getOperationName() + "Details.jspa";
    }

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ComponentAccessor.getBulkOperationManager().addProgressAwareBulkOperation(ZephyrAddTestBulkOperation.NAME_KEY, ZephyrAddTestBulkOperation.class);
	}


	public String doDetails() throws Exception {
        BulkEditBean bulkEditBean = getBulkEditBean();
        // Check that we have a BulkEditBean
        if (bulkEditBean == null) {
            // If we do not have BulkEditBean, send the user to the first step of the wizard
            return redirectToStart();
        }

        bulkEditBean.clearAvailablePreviousSteps();
        bulkEditBean.addAvailablePreviousStep(1);
        bulkEditBean.addAvailablePreviousStep(2);
        bulkEditBean.setCurrentStep(3);
        List<Project> projects = ComponentAccessor.getProjectManager().getProjects();
        ActionContext.getRequest().setAttribute("projects",projects);
        List<Long> projectList = new ArrayList<Long>();
        for(Issue issue : bulkEditBean.getSelectedIssues()) {
        	if(!projectList.contains(issue.getProjectObject().getId())) {
        		projectList.add(issue.getProjectObject().getId());
        	}
        }
        ActionContext.getRequest().setAttribute("issueCount",bulkEditBean.getSelectedIssues() != null ? bulkEditBean.getSelectedIssues().size() : 0);
        ActionContext.getRequest().setAttribute("projectCount",projectList != null && projectList.size() > 0 ? projectList.size() : 0);
        return INPUT;
	}

	public String doDetailsValidation() throws Exception {
	    // Check that we have a BulkEditBean -
        // Note: if the user is accessing JIRA from a URL not identical to baseURL, the redirect will cause them to lose their session,
        // and getBulkEditBean() will return null here (JT)
        if (getBulkEditBean() == null) {
            // If we do not have BulkEditBean, send the user to the first step of the wizard
            return redirectToStart();
        }

        final BulkEditBean bulkEditBean = getBulkEditBean();
        bulkEditBean.clearAvailablePreviousSteps();
        bulkEditBean.addAvailablePreviousStep(1);
        bulkEditBean.addAvailablePreviousStep(2);
        bulkEditBean.addAvailablePreviousStep(3);
        bulkEditBean.setCurrentStep(4);
        return INPUT;
	}

	@SuppressWarnings("deprecation")
	public String doPerform() throws Exception {	
		if (getBulkEditBean() == null) {
            return redirectToStart();
        }
		doPerformValidation();
		final String taskName = getText("jira.bulk.operation.progress.taskname.addtests",
				getRootBulkEditBean() != null ? getRootBulkEditBean().getSelectedIssues().size() : 0);
		BulkEditBean bulkEditBean = getBulkEditBean();
		BulkAddTestBean bulk = new BulkAddTestBean(ComponentAccessor.getIssueManager());
		bulk.initSelectedIssues(bulkEditBean.getSelectedIssues());
		bulk.setProjectId(projId);
		bulk.setVersionId(versionId);
		bulk.setCycleId(cycleId);
		bulk.setFolderId(folderId);
		return submitBulkOperationTask(bulk, getZephyrBulkOperation(), taskName);
	}
	
	
    private void doPerformValidation() {
    	JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
    	try {
            // Ensure the user has the global BULK CHANGE permission
        	GlobalPermissionManager permissionManager = ComponentAccessor.getGlobalPermissionManager();
            if (!permissionManager.hasPermission(GlobalPermissionKey.BULK_CHANGE,authenticationContext.getLoggedInUser())) {
                addErrorMessage(authenticationContext.getI18nHelper().getText("bulk.change.no.ProjectPermissions", String.valueOf(getBulkEditBean().getSelectedIssues().size())));
                return;
            }

            // Ensure the user can perform the operation
            if (!getZephyrBulkOperation().canPerform(getBulkEditBean(), getLoggedInUser())) {
                addErrorMessage(authenticationContext.getI18nHelper().getText("bulk.edit.cannotperform.error",
                        String.valueOf(getBulkEditBean().getSelectedIssues().size())));
                return;
            }
        }
        catch (Exception e) {
            log.error("Error occurred while testing operation.", e);
            addErrorMessage(authenticationContext.getI18nHelper().getText("bulk.canperform.error"));
            return;
        }

//        try {
//            for (BulkEditAction bulkEditAction : getBulkEditBean().getActions().values()) {
//                if (!bulkEditAction.isAvailable(getBulkEditBean())) {
//                    addErrorMessage(authenticationContext.getI18nHelper().getText("bulk.edit.perform.invalid.action",
//                            String.valueOf(getBulkEditBean().getSelectedIssues().size())));
//                }
//            }
//        }
//        catch (Exception e) {
//            log.error("Error occurred validating available update operations.", e);
//            addErrorMessage(authenticationContext.getI18nHelper().getText("bulk.canperform.error"));
//        }
    }


	public ProgressAwareBulkOperation getZephyrBulkOperation() {
		return ComponentAccessor.getBulkOperationManager().
				getProgressAwareOperation(ZephyrAddTestBulkOperation.NAME_KEY);
	}
	
	public void setCycleId(Integer cycleId) {
		this.cycleId = cycleId;
	}
	
	public void setVersionId(Integer versionId) {
		this.versionId = versionId;
	}
	public void setProjId(Integer projId) {
		this.projId = projId;
	}
	
	
	public Integer getVersionId() {
		return versionId;
	}

	public Integer getCycleId() {
		return cycleId;
	}

	public Integer getProjId() {
		return projId;
	}

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }
}
