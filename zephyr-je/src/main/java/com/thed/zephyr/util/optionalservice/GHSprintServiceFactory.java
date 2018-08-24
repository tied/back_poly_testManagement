package com.thed.zephyr.util.optionalservice;

import com.atlassian.greenhopper.service.sprint.SprintService;
import com.thed.zephyr.zapi.component.Zapi;
import org.osgi.framework.BundleContext;

/**
 * A service factory to safely create {@link com.thed.zephyr.util.optionalservice.Zapi} instances if and only if {@link Zapi} can be found.
 */
public class GHSprintServiceFactory extends OptionalService<SprintService>
{
    public GHSprintServiceFactory(final BundleContext bundleContext)
    {
        super(bundleContext, SprintService.class);
    }

    public GHSprintService get()
    {
        // If this class is being loaded, that means that a sufficient version of Zapi is found.
        // It should be safe to call getService without worrying about the underlying service tracker
        // not finding the Zapi instance.
        SprintService sprintService = getService();
        return new com.thed.zephyr.util.optionalservice.GHSprintService(sprintService);
    }
}
