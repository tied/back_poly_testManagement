package com.thed.zephyr.je.filter;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thed.zephyr.je.event.EventType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.event.api.EventPublisher;
import com.thed.zephyr.je.event.SingleScheduleEvent;
import com.thed.zephyr.je.service.CycleManager;
import com.thed.zephyr.je.service.ScheduleManager;

public class ZephyrProjectFilter implements Filter
{
    private static final Logger log = Logger.getLogger(ZephyrProjectFilter.class);
    private FilterConfig config;
    static final Pattern EVENT_PATTERN = Pattern.compile("^.*/([^\\./]+)\\.jspa.*$");
    private final ScheduleManager scheduleManager;
    private final CycleManager cycleManager;
    private final EventPublisher eventPublisher;
    
    public ZephyrProjectFilter(final ScheduleManager scheduleManager,final CycleManager cycleManager, final EventPublisher eventPublisher) {
    	this.scheduleManager=scheduleManager;
    	this.cycleManager=cycleManager;
    	this.eventPublisher = eventPublisher;
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
	    HttpServletResponse res = (HttpServletResponse)response;

	    String url = req.getRequestURI();
	    url = url.split("\\?")[0];
	    Matcher matcher = EVENT_PATTERN.matcher(url);
	    if (!(matcher.matches())) {
	    	log.debug("Pattern '" + EVENT_PATTERN + "' not matched. No event detected.");
	    }
	    
	    String urlPattern = matcher.group(1);
	    log.debug("URL " + urlPattern + " detected.");

	    String ipAddress = req.getRemoteAddr();

	    log.debug("URL for Event Trigger: " + url);
        
	    Map<String, String[]> map = request.getParameterMap();
	    boolean confirmDelete = false;
	    String projectIdFromRequest = null;
	    for (Entry<String, String[]> entry : map.entrySet()) {
	        String name = entry.getKey();
	        String[] values = entry.getValue();
	        if(StringUtils.equalsIgnoreCase(name, "confirm")) {
	        	for(String value : values) {
	        		if(StringUtils.equalsIgnoreCase(value, "true")) {
	        			confirmDelete = true;
	        		}
	        	}
	        }else if(StringUtils.equalsIgnoreCase(name, "pid")) {
                if(Objects.nonNull(values) && values.length > 0) {
                        projectIdFromRequest = values[0];
                }
            }
	        log.debug(name + ": " + Arrays.toString(values));
	    }

	    if(StringUtils.isNotBlank(projectIdFromRequest)) {
            //Long projectId = new Long( request.getParameter("pid") );
            Long projectId = Long.parseLong(projectIdFromRequest);
            if(confirmDelete) {
                Integer cycles = cycleManager.removeBulkCycle(projectId,null);
                log.debug("Total Cycles Deleted:"+cycles);

            }
        }else {
	        //marking confirm delete flag as false since project id is null.
            log.error("received project id as null from the request.");
            confirmDelete = Boolean.FALSE;
        }

	    //Let container continue to do the next intended action.
        chain.doFilter(request, response);
        
        //Once container does the real action, then we should do the Zephyr data clean up.
//        issue = ComponentAccessor.getIssueManager().getIssueObject(issueId);
//        if (issue != null){
//        	System.out.println("Issue Type - " + issue.getIssueTypeObject().getName());
//        }
//        else
//        {
//        	System.out.println("Issue no longer exists. Let's do the Zephyr Data cleanup for this issue.");
//        }
		
        //deleteIndexBased on Project
        if(confirmDelete) {		
	    	Map<String,Object> param = new HashMap<String,Object>();
			param.put("ENTITY_TYPE", "PROJECT_ID");
			List<String> entities = new ArrayList<String>();
			entities.add(projectIdFromRequest);
			param.put("ENTITY_VALUE", entities);
			try {
				eventPublisher.publish(new SingleScheduleEvent(null,param, EventType.PROJECT_DELETED));
			} catch(Exception e) {
				log.error("Error Deleting Index:");
			}    
        }
       
	}

	@Override
	public void destroy() {
	       this.config = null;
	}
}