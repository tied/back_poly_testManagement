package it.com.thed.zephyr.je.pageobjects.issue;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.utils.by.ByJquery;

public class ZephyrTeststep {

	  private String teststepId;
	  private PageElement teststepPageElement;
	  private PageElement stepCell;
	  private PageElement dataCell;
	  private PageElement resultCell;
	  private PageElement operationsCell;

	  @Inject
	  private PageBinder pageBinder;

	  @Inject
	  private Timeouts timeouts;

	  @Inject
	  private PageElementFinder elementFinder;

	  @Inject
	  private AtlassianWebDriver driver;
	  
	  public ZephyrTeststep(String stepId){
		  this.teststepId = stepId;
	  }
	  
	  @Init
	  public void initialise()
	  {
		this.teststepPageElement = this.elementFinder.find(By.id("step-" + this.teststepId + "-row"), TimeoutType.AJAX_ACTION);
	    this.stepCell = this.teststepPageElement.find(By.cssSelector("span[data-field-name|='step']"));
	    this.dataCell = this.teststepPageElement.find(By.cssSelector("span[data-field-name|='data']"));
	    this.resultCell = this.teststepPageElement.find(By.cssSelector("span[data-field-name|='result']"));
	    this.operationsCell = this.teststepPageElement.find(By.className("project-config-operations"));
	  }
	  
	  public String getStep(){
		  return this.stepCell.getText();
	  }
	  
	  public String getData(){
		  return this.dataCell.getText();
	  }
	  
	  public String getResult(){
		  return this.resultCell.getText();
	  }

		public ZephyrTeststepEditForm edit(final String fieldName)
		{
		    teststepPageElement.find(ByJquery.$(".jira-restfultable-editable[data-field-name=" + fieldName + "]")).click();
		    return pageBinder.bind(ZephyrTeststepEditForm.class, By.id(teststepPageElement.getAttribute("id")));
		}
	  
	  public void clickDeleteButton(){
		  
		    this.driver.executeScript("jQuery('#" + this.teststepPageElement.getAttribute("id") + "').addClass('jira-restfultable-active')", new Object[0]);
		    PageElement deleteButton = this.teststepPageElement.find(getDeleteButtonLocator());

		    deleteButton.javascript().mouse().mouseover();
		    Poller.waitUntil("Tried to click the delete button for Teststep, but couldn't find the trigger on the page", deleteButton.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));

		    deleteButton.click();
	  }
	  
	  private static By getDeleteButtonLocator()
	  {
	    return By.className("project-config-operations-trigger");
	  }
 
}