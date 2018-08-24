package com.thed.jira.zauth.admin;

import java.util.Set;

import com.thed.jira.zauth.utils.JIRAUtil;
import org.apache.commons.lang.StringUtils;

import webwork.action.ActionContext;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class ZAuthConfiguration extends JiraWebActionSupport {

	@Override
	public String doDefault() throws Exception {
		JIRAUtil.getWhiteList();
		return SUCCESS;
	}
	
	public String doEditConfig() throws Exception{
		String action = ActionContext.getRequest().getParameter("zEdit");
		if(StringUtils.equalsIgnoreCase(action, "Remove")){
			return removeIpAddress();
		}else if(StringUtils.equalsIgnoreCase(action, "Add")){
			return addIpAddress();
		}else{
			return SUCCESS;
		}
	}
	
	private String addIpAddress() throws Exception {
		String ipAddress = ActionContext.getRequest().getParameter("ipToAdd");
		JIRAUtil.addToWhiteList(ipAddress);
		return SUCCESS;
	}
	
	private String removeIpAddress() throws Exception {
		String ipAddress = ActionContext.getRequest().getParameter("zauthSelectedIP");
		JIRAUtil.removeFromWhiteList(ipAddress);
		return SUCCESS;
	}
	
	public int toMB(Integer bytes){
		return bytes >> 20;
	}
	
	public Set<String> getWhiteList(){
		return JIRAUtil.getWhiteList();
	}
}
