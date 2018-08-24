package it.com.thed.zephyr.je.pageobjects.issue;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import com.atlassian.jira.pageobjects.pages.AbstractJiraPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.google.common.base.Preconditions;

public class ZephyrViewIssuePage extends AbstractJiraPage
{
  private static final String URI_TEMPLATE = "/browse/%s";
  private final String issueKey;
  private final String uri;

  @Inject
  private PageBinder pageBinder;
  
  @Inject
  private Timeouts timeouts;

  @ElementBy(id="key-val")
  private PageElement issueHeaderLink;

  @ElementBy(id="project-config-panel-versions")
  private PageElement teststepsPage;

  @ElementBy(id="ztestSchedulesTable")
  private PageElement testExecutionsPage;

  @ElementBy(id="zephyr-je-add-execute")
  private PageElement executionButton;
  
  @ElementBy(id="link-issue")
  private PageElement linkButton;
  
  private ZephyrTeststepEditForm zTeststepEditForm;
  
  public ZephyrViewIssuePage(String issueKey)
  {
    this.issueKey = Preconditions.checkNotNull(issueKey);
    this.uri = String.format(URI_TEMPLATE, new Object[] { issueKey });
  }

  @Init
  public void initComponents()
  {
    this.zTeststepEditForm = this.pageBinder.bind(ZephyrTeststepEditForm.class, new Object[] { By.className("jira-restfultable-editrow") });
  }

  public List<ZephyrTeststep> getTeststeps()
  {
    List<ZephyrTeststep> teststeps = new ArrayList<ZephyrTeststep>();

    if (this.teststepsPage.find(By.className("jira-restfultable-no-entires")).timed().isPresent().by(this.timeouts.timeoutFor(TimeoutType.DIALOG_LOAD)).booleanValue())
    {
      return teststeps;
    }
    
    List<PageElement> teststepsElements = this.teststepsPage.findAll(By.className("project-config-version"));

    for (PageElement teststepElement : teststepsElements)
    {
      teststeps.add(this.pageBinder.bind(ZephyrTeststep.class, new Object[]{teststepElement.getAttribute("data-id")}));
    }

    return teststeps;
  }
  
  public ZephyrTeststep getMostRecentlyAddedTeststep(){
	  List<ZephyrTeststep> teststeps = getTeststeps();
	  
	  int size = teststeps.size();
	  if(size > 0)
		  return teststeps.get(size - 1);
		  
	return null;   
  }

  public List<ZephyrTestExecutionRow> getTestExecutions()
  {
	List<ZephyrTestExecutionRow> testExecutions = new ArrayList<ZephyrTestExecutionRow>();
    if (this.testExecutionsPage.find(By.className("zephyr-test-executions-no-entries")).timed().isPresent().by(this.timeouts.timeoutFor(TimeoutType.DIALOG_LOAD)).booleanValue())
    {
      return testExecutions;
    }
    
    List<PageElement> testExecutionElements = this.testExecutionsPage.findAll(By.className("zephyr-test-execution-entry"));
    for (PageElement testExecutionElement : testExecutionElements)
    {
    	testExecutions.add(this.pageBinder.bind(ZephyrTestExecutionRow.class, new Object[]{testExecutionElement}));
    }

    return testExecutions;
  }

  public ZephyrTestExecutionSetupDialog clickExecuteButton(){
      executionButton.javascript().mouse().mouseover();

      Poller.waitUntil("Tried to click \"Execute\" button....", executionButton.timed().isVisible(), (Matcher<Boolean>) Matchers.is(new Boolean(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
	  executionButton.click();
	  
	  return this.pageBinder.bind(ZephyrTestExecutionSetupDialog.class, new Object[0]);
  }

  public ZephyrIssueLinkDialogPage clickLinkButton(){
      linkButton.javascript().mouse().mouseover();

      Poller.waitUntil("Tried to click \"Link\" button....", linkButton.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
	  linkButton.click();
	  
	  return this.pageBinder.bind(ZephyrIssueLinkDialogPage.class, new Object[0]);
  }
  
  public ZephyrTeststepEditForm getTeststepEditForm(){
	 return this.zTeststepEditForm;
  }

  @Override
public TimedCondition isAt()
  {
    return this.issueHeaderLink.timed().hasText(this.issueKey);
  }

  @Override
public String getUrl()
  {
    return this.uri;
  }
  
  public String getIssueKey()
  {
    return this.issueKey;
  }
}