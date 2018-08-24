package it.com.thed.zephyr.je.pagetests.issue;

import com.thed.zephyr.util.ApplicationConstants;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrTeststep;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrTeststepEditForm;
import it.com.thed.zephyr.je.pageobjects.issue.ZephyrViewIssuePage;
import it.com.thed.zephyr.je.pagetests.BaseWebTest;

import java.text.SimpleDateFormat;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
/**
 * Reference: TestVersionConfig available in package com.atlassian.jira.webtest.webdriver.tests.projectconfig
 * Source Location: jira-project/jira-selenium-tests/src			
 */

@WebTest ( { Category.WEBDRIVER_TEST, Category.PLUGINS, Category.PROJECTS, Category.IGNITE  })
//@Restore ("xml/versionspanel.xml")
public class ZephyrTeststepTest extends BaseWebTest
{
    private static final String EXPECTED_OPERATION = "Expected [%s] to have [%s]";
    private static final String EXPECTED_NO_OPERATION = "Expected [%s] NOT to have [%s]";
    private static final String EXPECTED_FIELD_POPULATED = "Expected [%s] field to still be populated";
    private static final String STEP_INPUT_1 = "New Step 1";
    private static final String STEP_INPUT_2 = "New Step 2";
    private static final String STEP_INPUT_3 = "New Step 3";
    private static final String STEP_EDIT_INPUT = "step edit";
    private static final String DATA_INPUT = "Data In DATA Teststep";
    private static final String DATA_EDIT_INPUT = "Data Edit";
    private static final String RESULT_INPUT = "results in Result Teststep";
    private static final String RESULT_EDIT_INPUT = "Results Edit";

    private static final String EDIT_OPERATION = "Edit Operation";
    private static final String ARCHIVE_OPERATION = "Archive Operation";
    private static final String RELEASE_OPERATION = "Release Operation";
    private static final String DELETE_OPERATION = "Delete Operation";
    private static final String UNARCHIVE_OPERATION = "Unarchive Operation";
    private static final String UNRELEASE_OPERATION = "Unrelease Operation";
    private static final String THAT_NAME_IS_ALREADY_USED = "A version with this name already exists in this project.";
    private static final String EXPECTED_ERROR_S = "Expected error [%s]";
    private static final String INVALID_DATE = "Please enter the date in the following format: d/MMM/yy";
    private static final String NEW_VERSION_6 = "New Version 6";
    private static final String DATE_VAL = "23/Mar/50";
    private static final String CHICK = "CHICK";
    private static final String STEP = "step";
    private static final String DATA = "data";
    private static final String RESULT = "result";

    private static final String NO_NAME_ERROR = "You must specify a valid version name";
    private static final String VERSION_NAME_TOO_LONG = "Description is too long. Please enter a description shorter than 255 characters.";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_SHORT);

    private ZephyrTeststepEditForm zTeststepEditForm;
    
    @Test
    public void stepCreationTest() throws InterruptedException
    {
  	   ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
  	   zTeststepEditForm = issuePage.getTeststepEditForm()
                .fill(STEP_INPUT_1, DATA_INPUT, RESULT_INPUT)
                .submit();

  	   //To Verify we need to get the last added value!
  	   ZephyrTeststep teststep = issuePage.getMostRecentlyAddedTeststep();

  	   assertNotNull(teststep);
       assertTrue(String.format(EXPECTED_FIELD_POPULATED, STEP), teststep.getStep().equals(STEP_INPUT_1));
       assertTrue(String.format(EXPECTED_FIELD_POPULATED, DATA), teststep.getData().equals(DATA_INPUT));
       assertTrue(String.format(EXPECTED_FIELD_POPULATED, RESULT), teststep.getResult().equals(RESULT_INPUT));
    }
    
    @Test
    public void stepDeletionTest() throws InterruptedException
    {
	   ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");
	   
	   //Get total number of Teststeps.
	   List<ZephyrTeststep> steps = issuePage.getTeststeps();
	   int beforeCount = steps.size();
	   
	   zTeststepEditForm = issuePage.getTeststepEditForm()
	             .fill(STEP_INPUT_1 + "DEL", DATA_INPUT + "DEL", RESULT_INPUT + "DEL")
	             .submit();
	
	   //To Verify we need to get the last added value!
	   ZephyrTeststep teststep = issuePage.getMostRecentlyAddedTeststep();
	
		assertNotNull(teststep);
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, STEP), teststep.getStep().equals(STEP_INPUT_1 + "DEL"));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, DATA), teststep.getData().equals(DATA_INPUT + "DEL"));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, RESULT), teststep.getResult().equals(RESULT_INPUT + "DEL"));

		teststep.clickDeleteButton();
		
		steps = issuePage.getTeststeps();
		int afterCount = steps.size();
		
		//Count of Total number of steps available should be same before and after the operation (teststep addtion and then deletion)!
		assertEquals(beforeCount, afterCount, 0);
    }
    
    @Test
    public void stepEditTest() throws InterruptedException
    {
 	   ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");

 	   //Get total number of Teststeps currently associated
 	   List<ZephyrTeststep> steps = issuePage.getTeststeps();
 	   int count = steps.size();
 	   
 	   if(count == 0){
 		   //create a new teststep here OR make sure this step always get TestIssue with a at least one teststep associated to it.
 	   }
 	   
 	   ZephyrTeststep step = steps.get(0);
 	   step.edit("data")
 	   		.fill(STEP_EDIT_INPUT, DATA_EDIT_INPUT, RESULT_EDIT_INPUT)
 	   		.submit();
 	   
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, STEP), step.getStep().equals(STEP_EDIT_INPUT));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, DATA), step.getData().equals(DATA_EDIT_INPUT));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, RESULT), step.getResult().equals(RESULT_EDIT_INPUT));
    }

    @Test
    public void editStepAndCancelAndVerifyNoChangesToTeststepTest() throws InterruptedException
    {
 	   ZephyrViewIssuePage issuePage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrViewIssuePage.class, "BC-2");

 	   //Get total number of Teststeps currently associated
 	   List<ZephyrTeststep> steps = issuePage.getTeststeps();
 	   int count = steps.size();
 	   
 	   if(count == 0){
 		   //create a new teststep here OR make sure this step always get TestIssue with a at least one teststep associated to it.
 	   }
 	   
 	   ZephyrTeststep step = steps.get(0);
 	   String stepValueBeforeEdit = step.getStep();
 	   String dataValueBeforeEdit = step.getData();
 	   String resultValueBeforeEdit = step.getResult();
 	   
 	   step.edit("data")
 	   		.fill(STEP_EDIT_INPUT, DATA_EDIT_INPUT, RESULT_EDIT_INPUT)
 	   		.cancel();
 	   
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, STEP), step.getStep().equals(stepValueBeforeEdit));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, DATA), step.getData().equals(dataValueBeforeEdit));
		assertTrue(String.format(EXPECTED_FIELD_POPULATED, RESULT), step.getResult().equals(resultValueBeforeEdit));
    }

}
