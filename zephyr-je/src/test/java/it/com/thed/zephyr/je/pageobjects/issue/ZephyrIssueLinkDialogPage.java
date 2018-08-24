package it.com.thed.zephyr.je.pageobjects.issue;

import java.util.List;

import javax.inject.Inject;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.timeout.Timeouts;

public class ZephyrIssueLinkDialogPage {

	@ElementBy(id="linkKey-textarea")
	PageElement issuesTextarea;

	@ElementBy(id="issue-link-submit")
	PageElement issueLinkSubmit;
	
	@Inject
	private Timeouts timeouts;

	@Inject
	private PageElementFinder elementFinder;
	
	@Inject
	private PageBinder pageBinder;
	
	public void addDefects(){
		issuesTextarea.type("TEST");
		
		PageElement historyOptGroup = elementFinder.find(By.cssSelector("optgroup[label=\"History Search\"]"));
		List<PageElement> options = historyOptGroup.findAll(By.tagName("option"));
		for(PageElement option: options){
			System.out.println("Option Value -" + option.getValue() + " Text is - "+ option.getText() );
			if(option.getValue().equals("TEST-2")){
				option.click();
				break;
			}
		}
	}

	public void clickLinkButton(){
	    issueLinkSubmit.javascript().mouse().mouseover();
	    issueLinkSubmit.click();
	}	
}
