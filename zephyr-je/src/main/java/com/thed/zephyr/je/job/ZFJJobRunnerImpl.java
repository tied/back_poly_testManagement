package com.thed.zephyr.je.job;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.system.SystemInfoUtils;
import com.atlassian.jira.util.system.SystemInfoUtilsImpl;
import com.atlassian.query.Query;
import com.atlassian.scheduler.compat.JobInfo;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.je.config.license.ZephyrLicense;
import com.thed.zephyr.je.config.license.ZephyrLicenseManager;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

public class ZFJJobRunnerImpl implements ZFJJobRunner {
	protected final Logger log = Logger.getLogger(ZFJJobRunnerImpl.class);

	@Override
	public void execute(JobInfo jobInfo) {
		if(!JiraUtil.getVersionCheck().getOrElse(true)){
            return ;
		}
		List<NameValuePair> pingParams = new ArrayList<NameValuePair>();
		BuildUtilsInfo buildUtilsInfo = ComponentAccessor.getComponentOfType(BuildUtilsInfo.class);
		pingParams.add(new NameValuePair("jiraVersion", buildUtilsInfo.getVersion() + "-" + buildUtilsInfo.getApplicationBuildNumber()));
		SystemInfoUtils utils = new SystemInfoUtilsImpl();
		try {
			StringBuffer dbVersion = new StringBuffer(utils.getDatabaseType()).append("-").append(utils.getDatabaseMetaData().getDatabaseProductVersion());
			pingParams.add(new NameValuePair("dbVersion", dbVersion.toString()));
		} catch (Exception ex) {
			log.error("Unable to determine DB Version " + ex.getMessage());
		}
		
		JiraUtil.populateLicParams(pingParams);
		
		//ZFJ Build
		pingParams.add(new NameValuePair("buildNumber", ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getVersion()));
		pingParams.add(new NameValuePair("upm", ComponentAccessor.getPluginAccessor().getPlugin("com.atlassian.upm.atlassian-universal-plugin-manager-plugin").getPluginInformation().getVersion()));
		StringBuffer checkSum = new StringBuffer();
		
		//Projects
		checkSum.append(StringUtils.leftPad(String.valueOf(ComponentAccessor.getProjectManager().getProjectObjects().size()), 4, '0'));
		try {
			//Tests
			log.debug("Testcase issue Type Id ="+JiraUtil.getTestcaseIssueTypeId());
			Query query = JqlQueryBuilder.newClauseBuilder().issueType(JiraUtil.getTestcaseIssueTypeId()).buildQuery();
			SearchProvider searchProvider = ComponentAccessor.getComponent(SearchProvider.class);
			String tests = String.valueOf(searchProvider.searchCountOverrideSecurity(query, null));
			checkSum.append(StringUtils.leftPad(tests, 6, '0'));
		} catch (SearchException e) {
			checkSum.append(StringUtils.leftPad("", 6, '-'));
		} catch (Exception e) {
			checkSum.append(StringUtils.leftPad("", 6, '-'));
		}
		//Executions
		ActiveObjects ao = ZephyrComponentAccessor.getInstance().getActiveObjects();
		if(ao != null){
			String executions = String.valueOf(ao.count(Schedule.class, net.java.ao.Query.select().where("EXECUTED_ON IS NOT NULL")));
			checkSum.append(StringUtils.leftPad(executions, 6, '0'));
		} else{
			checkSum.append(StringUtils.leftPad("", 6, '?'));
		}
		HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
		String url = System.getProperty("VERSION_CHECK_URL", "https://version.yourzephyr.com/zfj_version_check.php");
		client.getHostConfiguration().setHost(url);
		GetMethod getMethod = new GetMethod(url);

		try {
			//Users
			String users = String.valueOf(ComponentAccessor.getUserManager().getTotalUserCount());
			checkSum.append(StringUtils.leftPad(users, 5, '0'));
			pingParams.add(new NameValuePair("chksum", checkSum.toString()));

			client.getParams().setBooleanParameter("http.protocol.allow-circular-redirects", true);
			client.getHttpConnectionManager().getParams().setSoTimeout(17000);
			client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			getMethod.setQueryString(pingParams.toArray(new NameValuePair[0]));
			log.debug("Ping String " + getMethod.getQueryString());
        
			int code = client.executeMethod(getMethod);
			if (code == HttpURLConnection.HTTP_OK) {
				log.debug("version check completed " + getMethod.getResponseBodyAsString());
			} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
				log.error("Unable to perform version check. Response from Server: \n " + getMethod.getResponseBodyAsString());
			} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
				log.error("Unable to perform version check, Proxy authentication required. Response from Server: \n  " + getMethod.getResponseBodyAsString());
			} else {
				log.error("Unable to perform version check.  Response from Server: \n  " + getMethod.getResponseBodyAsString());
			}
		} catch (Exception e) {
			log.info("Unable to perform version check ");
		} finally {
			if(getMethod != null) {
				getMethod.releaseConnection();
			}
		}
	}
}
