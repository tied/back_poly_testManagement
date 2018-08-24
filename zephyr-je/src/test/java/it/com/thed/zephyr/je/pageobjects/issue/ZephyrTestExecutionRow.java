package it.com.thed.zephyr.je.pageobjects.issue;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.atlassian.webdriver.AtlassianWebDriver;

public class ZephyrTestExecutionRow {
	
	  private PageElement testExecutionRowPageElement;
	  private PageElement versionCell;
	  private PageElement cycleCell;
	  private PageElement statusCell;
	  private PageElement defectsCell;
	  private PageElement execbyCell;
	  private PageElement execonCell;

	  @Inject
	  private PageBinder pageBinder;
	  
	  @Inject
	  private Timeouts timeouts;

	  @Inject
	  private PageElementFinder elementFinder;

	  @Inject
	  private AtlassianWebDriver driver;
	  
	  public ZephyrTestExecutionRow(PageElement testExecutionRowPageElement){
		  this.testExecutionRowPageElement = testExecutionRowPageElement;
	  }
	  
	  @Init
	  public void initialise()
	  {
	    this.versionCell = this.testExecutionRowPageElement.find(By.className("zephyr-test-execution-entry-version"));
	    this.cycleCell = this.testExecutionRowPageElement.find(By.className("zephyr-test-execution-entry-cycle"));
	    this.statusCell = this.testExecutionRowPageElement.find(By.className("exec-status-container"));
	    this.defectsCell = this.testExecutionRowPageElement.find(By.className("zephyr-test-execution-entry-defect"));
	    this.execbyCell = this.testExecutionRowPageElement.find(By.className("zephyr-test-execution-entry-execby"));
	    this.execonCell = this.testExecutionRowPageElement.find(By.className("zephyr-test-execution-entry-execon"));
	  }
	  
	  public String getVersion(){
		  return this.versionCell.getText();
	  }
	  
	  public String getCycle(){
		  return this.cycleCell.getText();
	  }

	  public String getStatus(){
		  return this.statusCell.getText();
	  }

	  public String getDefects(){
		  return this.defectsCell.getText();
	  }
	  
	  public String getExecby(){
		  return this.execbyCell.getText();
	  }

	  public String getExecon(){
		  return this.execonCell.getText();
	  }
}
