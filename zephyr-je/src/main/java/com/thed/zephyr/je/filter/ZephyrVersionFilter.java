package com.thed.zephyr.je.filter;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.ScheduleManager;

public class ZephyrVersionFilter implements Filter
{
    private static final Logger log = Logger.getLogger(ZephyrVersionFilter.class);
    private FilterConfig config;
    private final ScheduleManager scheduleManager;
    private final CycleManager cycleManager;
    private final VersionManager versionManager;
    
    public ZephyrVersionFilter(final ScheduleManager scheduleManager,final CycleManager cycleManager,final VersionManager versionManager) {
    	this.scheduleManager=scheduleManager;
    	this.cycleManager=cycleManager;
    	this.versionManager=versionManager;
    }
    
	@Override
	public void init(FilterConfig config) throws ServletException {
	       this.config = config;
	}

	@Override
	public void doFilter(ServletRequest request,
			ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if ((!(request instanceof HttpServletRequest)) || (!(response instanceof HttpServletResponse))) {
	       chain.doFilter(request, response);
	       log.info("Ignoring non-HTTP requests. Sorry dont know what to do with such requests.");
	       return;
	    }
		
	    HttpServletRequest req = (HttpServletRequest)request;
	    String url = req.getRequestURI();
	    
	    //Intercept only if it's Delete Version REST API.
	    if(StringUtils.equals(HttpMethod.DELETE, req.getMethod())){
		    Map<String, String[]> map = request.getParameterMap();
		    boolean isSwap=false;
		    Long target=null;

            String ipAddress = req.getRemoteAddr();
            log.debug("URL for Event Trigger: " + url + " from ip:"+ipAddress);

            for(Map.Entry<String, String[]> entry : map.entrySet()){
                if(StringUtils.equalsIgnoreCase(entry.getKey(), "affects")) {
                    for(String value : entry.getValue()) {
                        if(StringUtils.equalsIgnoreCase(value,"swap")) {
                            isSwap=true;
                        }
                    }
               }
               if(StringUtils.equalsIgnoreCase(entry.getKey(), "fix")) {
                    for(String value : entry.getValue()) {
                        if(StringUtils.equalsIgnoreCase(value,"swap")) {
                            isSwap=true;
                        }
                    }
               }
               if(StringUtils.equalsIgnoreCase(entry.getKey(),"moveAffectedIssuesTo")) {
                    for(String value : entry.getValue()) {
                        target = Long.valueOf(value);
                    }
               }
               if(target == null){
                   if(StringUtils.equalsIgnoreCase(entry.getKey(),"moveFixIssuesTo")) {
                        for(String value : entry.getValue()) {
                            target = Long.valueOf(value);
                        }
                   }
               }
           }
		   	    
		   String versionUrl = StringUtils.substringBefore(url, "?");
		   Version version = null;
		   if(StringUtils.isNotBlank(versionUrl)) {
			   String versionId = StringUtils.substringAfterLast(versionUrl, "/");
			   version = versionManager.getVersion(Long.valueOf(versionId));
		   }

		   //Let container continue to do the next intended action.
	       chain.doFilter(request, response);
	       
	       //Now clean up zephyr data
	       if(version != null) {
		       if(isSwap) {
		        	//Move all schedules with the deleted version to version i in target. 
		   	   		Integer noOfCycles = cycleManager.swapVersion(version.getId(), target, version.getProjectObject().getId());
		   	    	log.debug("No. Of Cycles swapped:"+noOfCycles);
		       }else{
                   if(version.getId() != null) {
                       //Delete Version without changing
                       int noOfCycles = cycleManager.swapVersion(version.getId(), new Long(ApplicationConstants.UNSCHEDULED_VERSION_ID), version.getProjectObject().getId());
                       log.debug("No. Of Cycles swapped to Unscheduled Version:"+noOfCycles);
                   }
               }
	       }
	    }else{
		    chain.doFilter(request, response);
	    }
	}

	@Override
	public void destroy() {
	       this.config = null;
	}
}