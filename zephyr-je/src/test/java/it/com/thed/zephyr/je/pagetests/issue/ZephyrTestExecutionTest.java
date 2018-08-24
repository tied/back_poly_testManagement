package it.com.thed.zephyr.je.pagetests.issue;

import it.com.thed.zephyr.je.pageobjects.issue.ZephyrTestExecutionPage;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrTestExecutionRow;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrTestExecutionSetupDialog;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrViewIssuePage;
import it.com.thed.zephyr.je.pagetests.BaseWebTest;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;

/**
 * Reference: TestVersionConfig available in package com.atlassian.jira.webtest.webdriver.tests.projectconfig
 * Source Location: jira-project/jira-selenium-tests/src			
 */

@WebTest ( { Category.WEBDRIVER_TEST, Category.PLUGINS, Category.PROJECTS, Category.IGNITE  })
public class ZephyrTestExecutionTest extends BaseWebTest{

	private static final String VERSION_1 = "version-1";

	private static final String CYCLE_1 = "cycle-1";
	

	private static final String STATUS_PASS = "PASS";
	private static final String STATUS_FAIL = "FAIL";
	private static final String STATUS_WIP = "WIP";
	private static final String STATUS_BLOCKED = "BLOCKED";
	private static final String STATUS_UNEXECUTED = "UNEXECUTED";
	
	private static final String DEFECTS_VERIFICATION_LIST = "BC-3";
	
	private static final String EXECUTED_BY_ADMIN = "A.D.Mean";
		
	@Test
	public void executeTestForUnscheduledVersionAndAdhocCycleAsFailTest() throws InterruptedException{
		ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
		ZephyrTestExecutionSetupDialog executionSetupDialog = issuePage.clickExecuteButton();
		executionSetupDialog.click_ExecuteExistingTestCycleRadioButton();

		ZephyrTestExecutionPage testExecutionPage = executionSetupDialog.submitTestExecutionSetupDialog();
		testExecutionPage.editExecutionStatus(STATUS_FAIL);
		testExecutionPage.clickSaveButton();
		assertEquals(testExecutionPage.getExecutionStatus(),STATUS_FAIL );
 	}

	@Test
	public void executeTestForUnscheduledVersionAndAdhocCycleAsPassTest() throws InterruptedException{
		ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
		ZephyrTestExecutionSetupDialog executionSetupDialog = issuePage.clickExecuteButton();
		executionSetupDialog.click_ExecuteExistingTestCycleRadioButton();

		ZephyrTestExecutionPage testExecutionPage = executionSetupDialog.submitTestExecutionSetupDialog();
		testExecutionPage.editExecutionStatus(STATUS_PASS);
		testExecutionPage.clickSaveButton();
		assertEquals(testExecutionPage.getExecutionStatus(),STATUS_PASS );
 	}

	@Test
	public void executeTestForVersion1AndCycle1AsPassTest() throws InterruptedException{
		ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
		ZephyrTestExecutionSetupDialog executionSetupDialog = issuePage.clickExecuteButton();
		executionSetupDialog.click_ExecuteExistingTestCycleRadioButton();

		executionSetupDialog.editVersion(VERSION_1);
		executionSetupDialog.editTestCycle(CYCLE_1);
		
		ZephyrTestExecutionPage testExecutionPage = executionSetupDialog.submitTestExecutionSetupDialog();
		testExecutionPage.editExecutionStatus(STATUS_PASS);
		
		testExecutionPage.addDefects();
		
		testExecutionPage.clickSaveButton();
		assertEquals(testExecutionPage.getExecutionStatus(),STATUS_PASS );
		
		issuePage = testExecutionPage.returnTestIssuePage();
		List<ZephyrTestExecutionRow> testExecutions = issuePage.getTestExecutions();
		assertNotNull(testExecutions);
		
		//Verify that one of the test execution is above with status as pass and with all defects added.
		boolean isTestExecutedCorrectly = false;
		
		for(ZephyrTestExecutionRow testExecutionRow: testExecutions){
			
			if(StringUtils.equals(testExecutionRow.getVersion(), VERSION_1)
					&& StringUtils.equals(testExecutionRow.getCycle(), CYCLE_1)
					&& StringUtils.equals(testExecutionRow.getStatus(), STATUS_PASS)
					&& StringUtils.equals(testExecutionRow.getDefects(), DEFECTS_VERIFICATION_LIST)
					&& StringUtils.equals(testExecutionRow.getExecby(), EXECUTED_BY_ADMIN))
				isTestExecutedCorrectly = true;
		}
		
		assertTrue(isTestExecutedCorrectly);
 	}
}