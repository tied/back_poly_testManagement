package it.com.thed.zephyr.je.pagetests.issue;

import it.com.thed.zephyr.je.pageobjects.issue.ZephyrIssueLinkDialogPage;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrViewIssuePage;
import it.com.thed.zephyr.je.pagetests.BaseWebTest;

import org.junit.Test;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;

@WebTest ( { Category.WEBDRIVER_TEST, Category.PLUGINS, Category.PROJECTS, Category.IGNITE  })
public class CreateIssueLinkTest extends BaseWebTest{

	@Test
	public void createIssueLinkToThisIssue() throws InterruptedException{
		ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
		ZephyrIssueLinkDialogPage linkDialog = issuePage.clickLinkButton();
		linkDialog.addDefects();
		linkDialog.clickLinkButton();
	}
	

}
