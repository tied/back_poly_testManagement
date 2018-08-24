package com.thed.zephyr.zapi.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.thed.zephyr.zapi.component.Zapi;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.atlassian.jira.rest.api.http.CacheControl.never;

@Path("zapi")
@AnonymousAllowed
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class ZAPIResource {
    protected final Logger log = Logger.getLogger(ZAPIResource.class);

	private final JiraAuthenticationContext authContext;
    private final Zapi zapiComponent;

    public ZAPIResource(JiraAuthenticationContext authContext, Zapi zapiComponent) {
        this.authContext=authContext;
        this.zapiComponent=zapiComponent;
    }

	/**
	 * Gets all schedules available for given Issue Id
	 */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
   	public Response isLicenseValid() {
    	/* Open for everyone */    	
//    	if(authContext.getLoggedInUser() == null) {
//    		return Response.status(Status.FORBIDDEN).build();
//    	}

        JSONObject json = zapiComponent.getZapiConfig();
        return Response.ok(json.toString()).cacheControl(never()).build();
    }
}
