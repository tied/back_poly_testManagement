package it.com.thed.zephyr.je.pageobjects.issue;

import java.util.List;

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

public class ZephyrTestExecutionSetupDialog {

	/*
	 * This element will hold parent DIV for this dialog (id="zephyr-je-add-execute")
	 */
	//@ElementBy(id="zephyr-je-add-execute")
	//@ElementBy(class="aui-dialog-content-ready")
	private PageElement rootElement;
	
	private PageElement executeAdHocRadioButtonElement;
	private PageElement executeExistingCycleRadioButtonElement;
	private PageElement executeSubmitButton;

	private PageElement projectVersion;
	private PageElement testCycle;
	
	  @Inject
	  private AtlassianWebDriver driver;

	  @Inject
	  private PageBinder pageBinder;

	  @Inject
	  private Timeouts timeouts;

	  @Inject
	  private PageElementFinder elementFinder;
	
	@Init
	public void initialise()
	{
		this.rootElement = elementFinder.find(By.className("aui-dialog-content-ready"));
		this.executeAdHocRadioButtonElement = rootElement.find(By.id("zephyr-je-execute-adhoc"));
		this.executeExistingCycleRadioButtonElement = rootElement.find(By.id("zephyr-je-execute-existing"));
		this.executeSubmitButton = rootElement.find(By.cssSelector("a[title|='Execute Test']"));
		
		this.projectVersion = rootElement.find(By.id("project_version"));
		this.testCycle = rootElement.find(By.id("cycle_names"));
		
	}
	
	public void click_ExecuteAdHocRadioButton(){
		this.executeAdHocRadioButtonElement.click();
	}
	
	public void click_ExecuteExistingTestCycleRadioButton(){
		this.executeExistingCycleRadioButtonElement.click();
	}

	public void editVersion(String versionName) throws InterruptedException{
		projectVersion.javascript().mouse().mouseover();
		Poller.waitUntil("Tried to open the project version dropwdown, but couldn't find the trigger on the page", projectVersion.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
		//projectVersion.click();
		
		List<PageElement> optionElementList = projectVersion.findAll(By.tagName("option"));
		for(PageElement option:optionElementList)
		{
			if(option.getText().equals(versionName)){
				option.click();
				break;
			}
		}
	}

	public void editTestCycle(String cycleName) throws InterruptedException{
		testCycle.javascript().mouse().mouseover();
		Poller.waitUntil("Tried to open the test Cycle dropwdown, but couldn't find the trigger on the page", testCycle.timed().isVisible(), (Matcher<Boolean>) Matchers.is(Boolean.valueOf(true)), Poller.by(this.timeouts.timeoutFor(TimeoutType.AJAX_ACTION)));
		//testCycle.click();
		
		List<PageElement> optionElementList = testCycle.findAll(By.tagName("option"));
		for(PageElement option:optionElementList)
		{
			if(option.getText().equals(cycleName)){
				option.click();
				break;
			}
		}
	}
	
	public ZephyrTestExecutionPage submitTestExecutionSetupDialog(){
		
		this.executeSubmitButton.click();
		return this.pageBinder.bind(ZephyrTestExecutionPage.class, new Object[0]);
	}
}