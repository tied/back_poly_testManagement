package it.com.thed.zephyr.je.pagetests.issue;

import org.junit.Test;

import com.atlassian.jira.functest.framework.FuncTestCase;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.config.EnvironmentBasedProductInstance;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;

@WebTest({Category.WEBDRIVER_TEST, Category.PLUGINS, Category.ISSUES})
public class ZephyrViewIssuePageTest extends FuncTestCase{

	final JiraTestedProduct jira;
	
	public ZephyrViewIssuePageTest(){
		jira = new JiraTestedProduct(new EnvironmentBasedProductInstance());
	}
		
    @Test
    public void testAddCommentsToIssue(){
 	   ViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ViewIssuePage.class, "BC-2");
 	   //issuePage.comment().typeComment("This is first comment on this issue from View Issue Page").add();
   }
    
}
