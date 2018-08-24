package it.com.thed.zephyr.je.pagetests;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.pageobjects.PageBinder;
import com.google.inject.Inject;
import com.thed.zephyr.je.pageobjects.config.SingleJiraWebTestRunner;

import org.junit.runner.RunWith;

@RunWith(SingleJiraWebTestRunner.class)
public abstract class BaseWebTest
{
  @Inject protected JiraTestedProduct jira;
  @Inject protected PageBinder pageBinder;
}
