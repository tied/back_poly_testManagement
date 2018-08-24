package com.thed.zephyr.je.rest.delegate;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.thed.zephyr.je.rest.exception.RESTException;
import javax.ws.rs.core.Response;
public interface IssueImporterResourceDelegate {
   public Response importFiles(HttpServletRequest request) throws RESTException;
   public Map<String, String> extractIssueMapping(HttpServletRequest request) throws RESTException, Exception;
}
