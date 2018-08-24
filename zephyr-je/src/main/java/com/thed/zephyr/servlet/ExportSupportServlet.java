package com.thed.zephyr.servlet;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.je.index.cluster.ClusterProperties;
import com.thed.zephyr.util.ApplicationConstants;

/**
 * An export  servlet used for bulk and other kind of export
 *  - take the filename and retrive the file,
 *  - clean up the file 
 *
 * This servlet can be reached at http://localhost:2990/jira/plugins/servlet/com.thed.zephyr.je/export
 */
public class ExportSupportServlet extends HttpServlet
{
    protected final Logger log = Logger.getLogger(ExportSupportServlet.class);
    private final JiraAuthenticationContext authContext;
    private final ClusterProperties clusterProperties;
    
    
    public ExportSupportServlet(JiraAuthenticationContext authContext,ClusterProperties clusterProperties) {
		this.authContext = authContext;
		this.clusterProperties = clusterProperties;
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
        	String fileName = request.getParameter("fileName");
	        String encodedFileName = URLEncoder.encode(fileName , "UTF-8");
	        /**
	         * In windows, it is invalid for the file name to contain '*' and hence is not create the file.
	         * So converting the '*' in fileName to '_'.
	         */
	        encodedFileName = encodedFileName.replaceAll("\\*", "_");
	        final File tmpDir = new File(clusterProperties.getSharedHome());
	        File file = new File(tmpDir+File.separator+"zfj"+File.separator+encodedFileName);
	        FileInputStream isr = null;
	        PrintWriter pw = null;
	        OutputStream responseOutputStream=null;
			try {
	            String extension = FilenameUtils.getExtension(file.getName());
	            final String codedName = JiraUrlCodec.encode(fileName, true);
	            String disposition = "attachment";
	            if(isSafari(request)){
	                response.setHeader("Content-Disposition", disposition + "; filename=" + fileName);
	            }else{
	                response.setHeader("Content-Disposition", disposition + "; filename*=UTF-8''" + codedName + ";");
	            }
	            response.setContentType("application/zip");
				isr = new FileInputStream(file);
				responseOutputStream = response.getOutputStream();
				IOUtils.copy(isr, responseOutputStream);
			} catch(FileNotFoundException e) {
				
			} finally {
	            close(isr);
	            close(responseOutputStream);
			    if(file.exists()) {
			    	file.delete();
			    }
			}
        //}
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
    }
}
