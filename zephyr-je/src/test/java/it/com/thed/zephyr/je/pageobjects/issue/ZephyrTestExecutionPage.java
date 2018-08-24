package it.com.thed.zephyr.je.pageobjects.issue;

import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.MultiSelectElement;
import com.atlassian.pageobjects.elements.Option;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;

public class ZephyrTestExecutionPage {

	PageElement rootElement;

	@ElementBy(id = "zephyr-je-block-save")
	PageElement saveButton;

	@ElementBy(id = "comment")
	PageElement comment;

	PageElement hiddenScheduleId;
	PageElement editExecutionStatusLink;
	PageElement executionStatus;
	PageElement statusDropdownElement;
	PageElement scheduleDefects;

	@ElementBy(id="zephyr-je-block-back-to-test")
	PageElement returnToTestLink;
	
	@ElementBy(id="zephyr-je-defectskey")
	MultiSelectElement defectsMultiSelect;
	
	@Inject
	private Timeouts timeouts;

	@Inject
	private PageElementFinder elementFinder;
	
	@Inject
	private PageBinder pageBinder;

	@ElementBy(className="title")
	PageElement successMessage; 

	@Init
	public void initialise()
	{
		this.rootElement = elementFinder.find(By.id("page"));
		this.hiddenScheduleId = elementFinder.find(By.id("zScheduleId"));
		
		String scheduleId = hiddenScheduleId.getAttribute("value"); 
		executionStatus = elementFinder.find(By.id("current-execution-status-dd-schedule-" + scheduleId));
		editExecutionStatusLink = elementFinder.find(By.id("executionStatus-labels-schedule-"+ scheduleId));

		statusDropdownElement = elementFinder.find(By.id("exec_status-schedule-" + scheduleId));
		scheduleDefects = elementFinder.find(By.id("zephyrJEdefectskey-schedule-" + scheduleId + "-textarea"));
		
	}
	
	public ZephyrViewIssuePage returnTestIssuePage(){
	
		returnToTestLink.click();
		return this.pageBinder.bind(ZephyrViewIssuePage.class, "BC-2");
	}
	
	public void clickSaveButton() {
		saveButton.javascript().mouse().mouseover();

		Poller.waitUntil(
				"Tried to open the version operations cog, but couldn't find the trigger on the page",
				saveButton.timed().isVisible(),
				(Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)),
				Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
		
		saveButton.click();
	}

	public void editExecutionStatus(String status) throws InterruptedException{
		
		editExecutionStatusLink.javascript().mouse().mouseover();
		Poller.waitUntil("Tried to open the execution status dropwdown, but couldn't find the trigger on the page", executionStatus.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
		editExecutionStatusLink.click();
		
		List<PageElement> optionElementList = statusDropdownElement.findAll(By.tagName("option"));
		for(PageElement option:optionElementList)
		{
			if(option.getText().equals(status)){
				option.click();
				break;
			}
		}
		
	}
	
	public void addDefects(){
		scheduleDefects.type("BC");

		PageElement listElement = this.elementFinder.find(By.className("aui-list-item-li-bc-3---bc-3"));
		Poller.waitUntil("Waiting for matching defect list to be visible.", listElement.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
		listElement.click();
	}
	
	public String getExecutionStatus(){
		return executionStatus.getText();
	}
	
//	 public class ExecutionStatusDropdown{
//		  
//		  @ElementBy(id="exec_status")
//		  PageElement dropdownRootElement;
//		  
//		  By rootElementId;
//
//		  @Inject
//		  private PageElementFinder elementFinder;
//
//		  @Inject
//		  private PageBinder binder;
//		  
//		  @Init
//		  public void initialise(){
//		  }
//		  
//		  public void click(String status)
//		  {
//		    this.dropdownRootElement.find(By.linkText(status)).click();
//		  }
//	  }
}