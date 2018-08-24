package it.com.thed.zephyr.je.pageobjects.cycle;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.Init;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.pageobjects.elements.timeout.Timeouts;
import com.atlassian.webdriver.AtlassianWebDriver;

public class ZephyrCycleDialog {

	  private PageElement cycleVersionElement;
	  private PageElement cycleNameElement;
	  private PageElement cycleDescriptionElement;
	  private PageElement cycleBuildElement;
	  private PageElement cycleEnvironmentElement;
	  private PageElement cycleFromDateElement;
	  private PageElement cycleEndDateElement;

	  @Inject
	  private PageBinder pageBinder;

	  @Inject
	  private Timeouts timeouts;

	  @Inject
	  private PageElementFinder elementFinder;

	  @Inject
	  private AtlassianWebDriver driver;
	  	  
	  @Init
	  public void initialise()
	  {
		this.cycleVersionElement = this.elementFinder.find(By.id("cycle_version"), TimeoutType.AJAX_ACTION); 		  
		this.cycleNameElement = this.elementFinder.find(By.id("cycle_name"), TimeoutType.AJAX_ACTION);
	    this.cycleDescriptionElement = this.elementFinder.find(By.id("cycle_description"));
	    this.cycleBuildElement = this.elementFinder.find(By.id("cycle_build"));
	    this.cycleEnvironmentElement = this.elementFinder.find(By.id("cycle_environment"));
	    this.cycleFromDateElement = this.elementFinder.find(By.className("project-config-operations"));
	  }
	  
	  public String getCycleVersion(){
		  return this.cycleVersionElement.getValue();
	  }
	  
	  public String getCycleName(){
		  return this.cycleNameElement.getText();
	  }
	  
	  public String getCycleDescription(){
		  return this.cycleDescriptionElement.getText();
	  }
}