package com.thed.zephyr.util.optionalservice;

import com.thed.zephyr.zapi.component.Zapi;
import org.osgi.framework.BundleContext;

/**
 * A service factory to safely create {@link com.thed.zephyr.util.optionalservice.Zapi} instances if and only if {@link com.thed.zephyr.zapi.component.Zapi} can be found.
 */
public class ZapiServiceFactory extends OptionalService<Zapi>
{
    public ZapiServiceFactory(final BundleContext bundleContext)
    {
        super(bundleContext, Zapi.class);
    }

    public com.thed.zephyr.util.optionalservice.Zapi get()
    {
        // If this class is being loaded, that means that a sufficient version of Zapi is found.
        // It should be safe to call getService without worrying about the underlying service tracker
        // not finding the Zapi instance.
        Zapi zapi = getService();
        return new com.thed.zephyr.util.optionalservice.Zapi(zapi);
    }
}
