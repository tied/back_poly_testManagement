package it.com.thed.zephyr.je.pagetests.admin;

import static org.junit.Assert.assertEquals;
import it.com.thed.zephyr.je.pagetests.BaseWebTest;

import org.junit.Before;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.thed.zephyr.je.pageobjects.admin.ZephyrLicenseConfigPage;

@WebTest({Category.WEBDRIVER_TEST, Category.PLUGINS, Category.ADMINISTRATION})
//@RestoreOnce("xml/JiraTestData.zip")

public class ZephyrLicenseConfigPageTest extends BaseWebTest
{
  private static final String LICENSE_NO_ERROR_MESSAGE="";

   @Before
   public final void setUp()
   {

   }

  //@Test
  public void testSetLicense(){
	  ZephyrLicenseConfigPage zephyrLicenseConfigPage = jira.gotoLoginPage().loginAsSysAdmin(ZephyrLicenseConfigPage.class);
	  zephyrLicenseConfigPage.setLicense("NewLicenseKey")
						.submit();
	  
	  assertEquals(LICENSE_NO_ERROR_MESSAGE, zephyrLicenseConfigPage.getErrorMessage());
  }
}