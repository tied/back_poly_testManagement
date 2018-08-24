package com.thed.zephyr.je.service.impl;

import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.thed.zephyr.je.service.MyManager;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by mukul on 7/30/15.
 */

@Path("myrest")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
public class MyManagerImpl {

    private final MyManager myManagerDelegate;

    public MyManagerImpl(MyManager myManager) {
        myManagerDelegate = myManager;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void foo() {
        myManagerDelegate.foo();
    }
}
