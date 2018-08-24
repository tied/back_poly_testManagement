package com.thed.zephyr.je.rest;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.util.ApplicationConstants;

public class SupportDownloadRequst implements Callable<String> {
	
	protected final Logger log = Logger.getLogger(SupportDownloadRequst.class);
	
	public static final HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
	private String ip;
	private int port;
	private String cookie;
	private String headerValue;
	private JSONObject pingParams;
	public SupportDownloadRequst(String ip, int port,String cookie,String headerValue,JSONObject pingParams) {
		this.ip= ip;
		this.port= port;
		this.cookie= cookie;
		this.headerValue= headerValue;
		this.pingParams=pingParams;
	}
	
	@Override
	public String call() throws Exception {
		String fileName = null;

		client.getParams().setBooleanParameter("http.protocol.allow-circular-redirects", true);
		client.getHttpConnectionManager().getParams().setSoTimeout(17000);
		client.getHttpConnectionManager().getParams().setConnectionTimeout(30000);

		BuildUtilsInfo buildUtilsInfo = ComponentAccessor.getComponentOfType(BuildUtilsInfo.class);
		Integer jiraVersion = Integer.valueOf(buildUtilsInfo.getVersion().replaceAll("\\.", ""));
		
		if (jiraVersion >= ApplicationConstants.TROUBLESHOOT_JIRA_VERSION) {

			log.debug("Cookie :[" + cookie+"]");
			log.debug("headerValue :["  + headerValue+"]");
			log.debug("ip Address :["  + ip+"]");
			log.debug("port  :["  + port+"]");
			String url = System.getProperty("ZFJ_SUPPOERT_URL",
					"http://" + ip + ":" + port + "/rest/troubleshooting/latest/support-zip/local");
			client.getHostConfiguration().setHost(url);

			PostMethod postMethod = new PostMethod(url);
			final GetMethod method = new GetMethod();
			try {
				postMethod.setRequestHeader("Cookie", cookie);
				postMethod.setRequestHeader("AO-7DEABF", headerValue);
				postMethod.setRequestHeader("X-Atlassian-Token", "no-check");
				StringRequestEntity requestEntity = new StringRequestEntity(pingParams.toString(), "application/json",
						"UTF-8");
				postMethod.setRequestEntity(requestEntity);
			} catch (UnsupportedEncodingException e) {
				log.error("UnsupportedEncodingException :"+e);
			}
			log.debug("Ping String " + pingParams.toString());

			try {
				int code = client.executeMethod(postMethod);
				if (code == HttpURLConnection.HTTP_OK) {
					JSONObject jsonObject = new JSONObject();
					log.debug("version check completed " + postMethod.getResponseBodyAsString());

					jsonObject = new JSONObject(postMethod.getResponseBodyAsString());
					if (!Objects.isNull(jsonObject)) {
						String taskId = jsonObject.getString("taskId");
						// Retrieve content:
						String geturl = System.getProperty("ZFJ_GETSUPPOERT_URL", "http://" + ip + ":" + port
								+ "/rest/troubleshooting/latest/support-zip/status/task/" + taskId);
						method.setPath(geturl.toString());
						method.setRequestHeader("Cookie", cookie);
						method.setRequestHeader("AO-7DEABF", headerValue);
						method.setRequestHeader("X-Atlassian-Token", "no-check");
						
						
						boolean check = true;
	                    while(check) {
	                    	try {
	                    		final int status = client.executeMethod(method);
	                    		log.debug("status :: "+ status);
	                    		if (status == HttpURLConnection.HTTP_OK) {
	                    			if(null != method && StringUtils.isNotBlank(method.getResponseBodyAsString())) {
	                    				//log.debug("responseBody :: "+ method.getResponseBodyAsString());
	                        	    	JSONObject json = new JSONObject(method.getResponseBodyAsString());
	                        	    	int progressPercentage = json.getInt("progressPercentage");
	                        	    	//log.debug("job percentage : "+ progressPercentage);
	                        	    	//if(!Objects.isNull(json.getString("fileName"))) {
	                        	    		//log.debug("job percentage : "+ progressPercentage);
	                        	    		if(Objects.nonNull(json) && StringUtils.isNotBlank(json.getString("fileName"))) {
	                        	    			check = false;
	                            	    		fileName =  json.getString("fileName");
	                        	    		}else {
	                        	    			//log.debug("file response is null after 100%");
	                        	    			check= Boolean.FALSE;
	                        	    		}
	                        	    		
	                        	    	//}
	                    			}else {
	                    				log.debug("retrieved empty/null response from server");
	                    			}
	                    			
	                    	    }else if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN){
	                    	    	check = false;
	                    	    	log.error("Unable to perform version check. Response from Server: \n  " +  method.getResponseBodyAsString());
	                    	    } else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
	                    	    	check = false;
	                				log.error(
	                						"Unable to perform feedback post, Proxy authentication required. Response from Server: \n  "
	                								+ method.getResponseBodyAsString());
	                			} else {
	                				check = false;
	                				log.error("Unable to perform feedback post.  Response from Server: \n  "
	                						+ method.getResponseBodyAsString());
	                			}
	                    		
								} catch (NullPointerException | JSONException j) {
									check = true;
								}
	                    	
	                    }	
					}
				} else if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
					log.error("Unable to perform version check. Response from Server: \n "
							+ postMethod.getResponseBodyAsString());
				} else if (code == HttpURLConnection.HTTP_PROXY_AUTH) {
					log.error(
							"Unable to perform feedback post, Proxy authentication required. Response from Server: \n  "
									+ postMethod.getResponseBodyAsString());
				} else {
					log.error("Unable to perform feedback post.  Response from Server: \n  "
							+ postMethod.getResponseBodyAsString());
				}
			} catch (Exception e) {
				log.error("Unable to perform version check "+e);
			} finally {
				if (postMethod != null) {
					postMethod.releaseConnection();
				}
				if (method != null) {
					method.releaseConnection();
				}
			}

		}else{
			fileName = "redirectToJiraSupport";
		}
				return fileName;
	}

}
