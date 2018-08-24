package com.thed.zephyr.je.attachment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.jira.user.ApplicationUser;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.AttachmentNotFoundException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.http.JiraHttpUtils;
import com.atlassian.jira.util.io.InputStreamConsumer;
import com.atlassian.jira.web.exception.WebExceptionChecker;
import com.atlassian.jira.web.servlet.InvalidAttachmentPathException;
import com.atlassian.seraph.util.RedirectUtils;

public abstract class AbstractViewFileServlet extends HttpServlet
{
    private static final Logger log = Logger.getLogger(ViewAttachmentServlet.class);

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            File file;
            try
            {
                file = getFileName(request,response);
            }
            catch (InvalidAttachmentPathException e)
            {
                response.sendError(400, "Invalid attachment path");
                return;
            }
            catch (AttachmentNotFoundException nfe)
            {
                send404(request, response);
                return;
            }

            streamFileData(request, response, file);
        }
        catch (Exception e)
        {
            if (WebExceptionChecker.canBeSafelyIgnored(e))
            {
                return;
            }
            log.error("Error serving file for path " + request.getPathInfo() + ": " + e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    private void send404(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendError(404, String.format("Attachment %s was not found", request.getPathInfo()));
    }

    private void redirectForSecurityBreach(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
    {
    	if (getUserName() != null)
    	{
    		RequestDispatcher rd = request.getRequestDispatcher("/secure/views/securitybreach.jsp");
    		JiraHttpUtils.setNoCacheHeaders(response);
    		rd.forward(request, response);
    	}
    	else
    	{
    		response.sendRedirect(RedirectUtils.getLoginUrl(request));
    	}
    }


    private void streamFileData(final HttpServletRequest request, final HttpServletResponse response, final File file)
    throws IOException, ServletException
    {
    	try
    	{
    		getInputStream(file, new InputStreamConsumer<Void>() {
    			@Override
    			public Void withInputStream(final InputStream is) throws IOException
    			{
    				// can only set headers after knowing that we have the file - otherwise we can't do response.sendError()
    				setResponseHeaders(request, response);

    				final OutputStream out = response.getOutputStream();
    				try
    				{
    					IOUtils.copy(is, out);
    				}
    				finally
    				{
    					IOUtils.closeQuietly(out);
    				}
    				return null;
    			}
    		});
    	}
    	catch (AttachmentNotFoundException e)
    	{
    		log.error("Error finding " + request.getPathInfo() + " : " + e.getMessage());
    		// the outcome of this will only be complete if nothing else has written to the OutputStream, so we must
    		// do this before setResponseHeaders() is called
    		send404(request, response);
    		return;
    	}
    	catch (FileNotFoundException e) // getInputStream will throw it when file is missing on disk
    	{
    		log.error("Error finding " + request.getPathInfo() + " : " + e.getMessage());
    		// the outcome of this will only be complete if nothing else has written to the OutputStream, so we must
    		// do this before setResponseHeaders() is called
    		send404(request, response);
    		return;
    	}
    	catch (IOException e)
    	{
    		// we suspect this to be a Broken Pipe exception, probably due to the user closing the connection by pressing
    		// the stop button in their browser, which we don't really care about logging
    		log.debug("Error serving content to client", e);

    	}

        catch (PermissionException e)
        {
            redirectForSecurityBreach(request, response);
            return;
        }
    }

    /**
     * Gets the attachment file (not the file name) that corresponds to the requested attachment.
     *
     * @param file the file
     * @return the File resource for the attachment.
     * @throws DataAccessException If there is a problem looking up the data to support the attachment.
     * @throws IOException if there is a problem getting the File.
     * @throws PermissionException if the user has insufficient permission to see the attachment.
     * @throws InvalidAttachmentPathException if the path to the attachment was invalid in some way.
     */
    protected abstract void getInputStream(File file, InputStreamConsumer<Void> consumer)
            throws InvalidAttachmentPathException, DataAccessException, IOException, PermissionException;

    abstract File getFileName(HttpServletRequest request, HttpServletResponse response) throws DataAccessException, PermissionException;
    
    /**
     * Sets the content type, content length and "Content-Disposition" header
     * of the response based on the values of the attachement found.
     *
     * @param request  HTTP request
     * @param response HTTP response
     */
    protected abstract void setResponseHeaders(HttpServletRequest request, HttpServletResponse response)
            throws InvalidAttachmentPathException, DataAccessException, IOException;

    /**
     * @return The logged-in user's name, or null (anonymous)
     */
    protected final String getUserName()
    {
		ApplicationUser user = getJiraAuthenticationContext().getLoggedInUser();
        return (user != null ? user.getName() : null);
    }

    protected JiraAuthenticationContext getJiraAuthenticationContext()
    {
        return ComponentAccessor.getJiraAuthenticationContext();
    }
}
    