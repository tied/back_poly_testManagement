package com.thed.zephyr.servlet;

import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.views.util.RssViewUtils;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.query.Query;
import com.atlassian.velocity.VelocityManager;
import com.google.common.cache.LoadingCache;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.helper.ScheduleSearchResourceHelper;
import com.thed.zephyr.je.service.*;
import com.thed.zephyr.je.vo.ZQLScheduleBean;
import com.thed.zephyr.je.zql.core.SearchService;
import com.thed.zephyr.je.zql.helper.SearchResult;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ZephyrComponentAccessor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An export  servlet used for bulk and other kind of export
 *  - take the filename and retrive the file,
 *  - clean up the file 
 *
 * This servlet can be reached at http://localhost:2990/jira/plugins/servlet/com.thed.zephyr.je/export
 */
public class ExportServlet extends HttpServlet
{
    protected final Logger log = Logger.getLogger(ExportServlet.class);
    private final JiraAuthenticationContext authContext;
    private final VelocityRequestContextFactory velocityRequestContextFactory;

    public ExportServlet(JiraAuthenticationContext authContext,VelocityRequestContextFactory velocityRequestContextFactory)
    {
        this.authContext = authContext;
        this.velocityRequestContextFactory=velocityRequestContextFactory;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        if (authContext.getLoggedInUser() == null) {
    		Map<String, String> map = new HashMap<String, String>();
    		response.setStatus(HttpURLConnection.HTTP_FORBIDDEN);
			map.put(ApplicationConstants.ERROR_ID, Status.FORBIDDEN.name());
			map.put(ApplicationConstants.ERROR_DESC, "Request Originated from Invalid source");
			map.put(ApplicationConstants.ERROR_DESC_HTML, "Request Originated from Invalid source");
		    JSONObject jsonResponse = new JSONObject(map);
		    response.setContentType("application/json");
		    response.getWriter().print(jsonResponse);
		    return;
        }
        String exportType = request.getParameter("exportType");
        if(StringUtils.equalsIgnoreCase(exportType, "rss")) {
	        String zqlQuery = (request.getParameter("zql") != null && request.getParameter("zql").length() > 0) ? request.getParameter("zql") : "";
	        if(zqlQuery != null) {
	        	SearchService searchService = (SearchService)ZephyrComponentAccessor.getInstance().getComponent("searchService");
	        	ScheduleSearchResourceHelper searchResourceHelper = new ScheduleSearchResourceHelper(authContext.getLoggedInUser(),searchService,
	        			(ExportService)ZephyrComponentAccessor.getInstance().getComponent("exportService"),
	        			ComponentAccessor.getIssueManager(),(CycleManager)ZephyrComponentAccessor.getInstance().getComponent("cycle-manager"),
	        			ComponentAccessor.getVersionManager(),(TeststepManager)ZephyrComponentAccessor.getInstance().getComponent("teststep-manager")
	        			,(StepResultManager)ZephyrComponentAccessor.getInstance().getComponent("stepresult-manager"),
                        (FolderManager) ZephyrComponentAccessor.getInstance().getComponent("folder-manager"),
						(ZephyrCustomFieldManager) ZephyrComponentAccessor.getInstance().getComponent("zephyrcf-manager"));
	    		ParseResult parseResult = searchResourceHelper.getNewSearchQuery(zqlQuery);
	            Query currentQuery=null;
	    		if (parseResult.isValid())
	            {
	    			currentQuery = parseResult.getQuery();
	            } else {
	            	if(parseResult.getErrors() != null && !parseResult.getErrors().getErrorMessages().isEmpty()) {
	            		return;
	            	}
	            }
	    		log.debug("Current Query="+currentQuery);
	        	// validates Atlassian query object and returns JSONObject with the error if any
	        	JSONObject jsonObject = searchResourceHelper.validateQuery(currentQuery);
		    	if(jsonObject != null) {
					return;
		    	} else {
		    		response.setContentType("application/rss+xml");
		    		response.setHeader("Content-Disposition", "inline");
			    	String responseValue = exportRSSFeed(searchService, searchResourceHelper, currentQuery,response);
			        final Writer writer = new BufferedWriter(response.getWriter());
			        if(writer != null) {
			        	writer.write(responseValue);
			        	writer.flush();
			        }
	        	} 
	        }
        } else {
			String fileName = request.getParameter("fileName");
			String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
			/**
			 * In windows, it is invalid for the file name to contain '*' and hence is not create the file.
			 * So converting the '*' in fileName to '_'.
			 */
			encodedFileName = encodedFileName.replaceAll("\\*", "_");
			final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
			File file = new File(tmpDir + File.separator + encodedFileName);
			InputStreamReader isr = null;
			PrintWriter pw = null;
			try {
				String extension = FilenameUtils.getExtension(file.getName());
				final String codedName = JiraUrlCodec.encode(fileName, true);
				String disposition = "attachment";
				if (isSafari(request)) {
					response.setHeader("Content-Disposition", disposition + "; filename=" + fileName);
				} else {
					response.setHeader("Content-Disposition", disposition + "; filename*=UTF-8''" + codedName + ";");
				}
				pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"), false);
				if(file.getName().startsWith(ApplicationConstants.INTEGRITY_CHECKER)) {
					response.setContentType("application/octet-stream;charset=UTF-8");
					FileInputStream fis = new FileInputStream(file);
					byte[] bufffer = new byte[4096];
					ServletOutputStream outputStream = response.getOutputStream();
			        while(fis.read(bufffer) != -1)    
			        	outputStream.write(bufffer); 
			        fis.close();
			        outputStream.flush();
			        outputStream.close();			        
				} else {					
					if (StringUtils.equalsIgnoreCase(extension, "html")) {
						isr = new InputStreamReader(new FileInputStream(file));
						response.setContentType("text/html;charset=UTF-8");
					} else {
						isr = new InputStreamReader(new FileInputStream(file), "UTF-8");
						if (StringUtils.equalsIgnoreCase(extension, "XML")
								|| StringUtils.equalsIgnoreCase(extension, "RSS")) {
							response.setContentType("text/xml;charset=UTF-8");
						} else if (StringUtils.equalsIgnoreCase(extension, "excel")) {
							response.setContentType("application/vnd.ms-excel; charset=UTF-8");
						} else if (StringUtils.equalsIgnoreCase(extension, "csv")) {
							response.setContentType("text/csv;charset=UTF-8");
						} else {
							response.setContentType("application/octet-stream;charset=UTF-8");
						}
					}
					IOUtils.copy(isr, pw);
					pw.flush();
				}				
				
			} catch (FileNotFoundException e) {

			} finally {
				close(isr);
				close(pw);
				if (file.exists()) {
					file.delete();
				}
			}
		}
	}
    
	private String exportRSSFeed(SearchService searchService,
			ScheduleSearchResourceHelper searchResourceHelper,
			Query currentQuery, HttpServletResponse response) {
		SearchResult searchResults = null;
		List<ZQLScheduleBean> schedules = new ArrayList<ZQLScheduleBean>();

		try {
			searchResults = searchService.search(authContext.getLoggedInUser(), currentQuery,0,true,null,false);
			if(searchResults != null && (searchResults.getDocuments() != null || searchResults.getDocuments().size() > 0)) {
				LoadingCache<Integer,String> cycleCache = searchResourceHelper.getNewCycleCache();
				List<Integer> issuePermission = new ArrayList<>();

				for(Document document : searchResults.getDocuments()) {
					ZQLScheduleBean schedule = searchResourceHelper.documentsToJSON(document, cycleCache, "teststeps", issuePermission);
					if(schedule.getId() != null) {
						schedules.add(schedule);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(schedules.size() > 0) {
	        Map<String, Object> bodyParams = JiraVelocityUtils.getDefaultVelocityParams(authContext);
	        bodyParams.put("zqlQuery", currentQuery.getQueryString());
	        bodyParams.put("schedules", schedules);
	        bodyParams.put("i18n", authContext.getI18nHelper());
	        bodyParams.put("rssLocale", RssViewUtils.getRssLocale(authContext.getLocale()));
	        final String baseUrl = velocityRequestContextFactory.getJiraVelocityRequestContext().getCanonicalBaseUrl();
	        bodyParams.put("baseUrl", baseUrl);
	        
	        VelocityManager velocityManager = ComponentAccessor.getVelocityManager();
	        final String responseValue = velocityManager.getEncodedBody("templates/zephyr/schedule/export/","searchrequest-rss.vm","UTF-8", bodyParams);
	        return responseValue;
		}
		return null;
	}

    
	private void close(Closeable close) {
		if (close != null) {
			try {
				close.close();
			} catch (IOException e) {
                log.debug("Error closing streams after serving content", e);
			}
		}
	}

    private Boolean isSafari(HttpServletRequest request){
        String  browserDetails  =   request.getHeader("User-Agent");
        if(browserDetails != null){
            return (org.apache.commons.lang.StringUtils.containsIgnoreCase(browserDetails, "safari") && !org.apache.commons.lang.StringUtils.containsIgnoreCase(browserDetails, "chrome"));
        }
        return false;
        //UserAgentUtil.UserAgent agent = new UserAgentUtilImpl().getUserAgentInfo(browserDetails);
        //return agent.getBrowser().getBrowserFamily() == UserAgentUtil.BrowserFamily.SAFARI;
    }
}
