package com.thed.zephyr.je.rest.exception;

import java.util.Arrays;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.json.JSONObject;
import com.thed.zephyr.util.ZephyrCacheControl;

public class RESTException extends WebApplicationException {

    public RESTException(final Response.Status status, final String... errorMessages){
    	ErrorCollection error = new SimpleErrorCollection();
    	error.addErrorMessages(Arrays.asList(errorMessages));
    	createResponse(status.getStatusCode(), error);
    }
    
	/**
     * Creates a new RESTException for the given issue, with a collection of errors.
     *
     * @param status the HTTP status of this error (401, 403, etc)
     * @param errors an ErrorCollection containing the errors
     */
    public RESTException(final Response.Status status, final ErrorCollection errors){
        super(createResponse(status.getStatusCode(), errors));
    }

    /**
     * Creates a new RESTException for the given issue and allows to nest an exception.
     *
     * @param status the HTTP status of this error (401, 403, etc)
     * @param cause the nested exception that will be logged by the ExceptionInterceptor, before returning the response to the user.
     */
    public RESTException(final Response.Status status, final Throwable cause){
        super(cause, status);
    }
    
    public RESTException(final Response.Status status, final Map<String, String> errorMap ){
    	super(createResponse(status,errorMap));
    	/*
    	 * Alternative approach is to convert map into errorCollection 
    	 * com.atlassian.jira.util.ErrorCollection errCol = new SimpleErrorCollection();
    		errCol.addErrors(errorMap);
    	 * */
    }

    public RESTException(Status status, JSONObject errorJsonObject) {
    	super(createResponse(status, errorJsonObject));
	}

    private static Response createResponse(Status status, @Nonnull JSONObject errorJsonObject) {
    	return Response.status(status).entity(errorJsonObject.toString()).cacheControl(ZephyrCacheControl.never()).build();
	}

	/**
     * Creates a new RESTException for the given issue, with a map of errors.
     *
     * @param status the HTTP status of this error (401, 403, etc)
     * @param errors an map containing the errors
     */
    private static Response createResponse(Status status, Map<String, String> errorMap) {
    	return Response.status(status).entity(errorMap).build();
	}
    

    /**
     * Creates a new HTTP response with the given status, returning the errors in the provided ErrorCollection.
     *
     * @param status the HTTP status to use for this response
     * @param errors an ErrorCollection containing errors
     * @return a Response
     */
    private static Response createResponse(final int status, final ErrorCollection errors)
    {
        // the issue key is not used yet, but should make it into the entity in the future...
        return Response.status(status).entity(errors).cacheControl(ZephyrCacheControl.never()).build();
    }
}
