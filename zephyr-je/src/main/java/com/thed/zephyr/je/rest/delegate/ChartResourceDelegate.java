/**
 * 
 */
package com.thed.zephyr.je.rest.delegate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author niravshah
 *
 */
public interface ChartResourceDelegate {
	
	Response generateTestsCreatedData(HttpServletRequest request,
            final String projectKey,final String days, final String periodName);

	Map<String, Integer> getStats(HttpServletRequest request);
}
