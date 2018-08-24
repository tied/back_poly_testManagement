package com.thed.zephyr.util.optionalservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/30/13
 * Time: 12:11 PM
 */

public class ServiceAccessorImpl implements ServiceAccessor {

    private static final Logger log = LoggerFactory.getLogger(ServiceAccessorImpl.class);

    private final ApplicationContext applicationContext;
    private Zapi zapi;
    private GHSprintService sprintService;

    public ServiceAccessorImpl(ApplicationContext applicationContext) {
        this.applicationContext = checkNotNull(applicationContext, "applicationContext");
    }

    /*** Zapi ***/
    @Override
    public synchronized Zapi getZapi() {
        if (zapi == null) {
            initZapi();
        }
        return zapi;
    }

    private void initZapi() {
        try {
            Class<?> zapiServiceFactoryClass = getZapiComponentServiceFactoryClass();
            if (zapiServiceFactoryClass != null) {
                zapi = ((ZapiServiceFactory) applicationContext.getAutowireCapableBeanFactory().
                        createBean(zapiServiceFactoryClass, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false)).get();
            }
        } catch (Exception e) {
            log.debug("Could not create Zapi", e);
        }
    }

    /**
     * If the necessary Zapi class is found on the classpath, Zapi exists.
     * In that case return the {@link ZapiServiceFactory} class, otherwise return null.
     */
    private Class<?> getZapiComponentServiceFactoryClass() {
        try {
            // Check to see if the class we depend on (from Zapi) is available
            getClass().getClassLoader().loadClass("com.thed.zephyr.zapi.component.Zapi");
            // If we get to this point in the code, Zapi is available. Let's load the service factory
            return getClass().getClassLoader().loadClass("com.thed.zephyr.util.optionalservice.ZapiServiceFactory");
        } catch (Exception e) {
            log.info("The necessary Zapi class is unavailable.");
            return null;
        }
    }

    /*** SprintService ***/
    @Override
    public GHSprintService getSprintService() {
        if(sprintService == null){
            initSprintService();
        }
        return sprintService;
    }

    private void initSprintService() {
        try {
            Class<?> sprintServiceServiceFactoryClass = getSprintServiceComponentServiceFactoryClass();
            if (sprintServiceServiceFactoryClass != null) {
                sprintService = ((GHSprintServiceFactory) applicationContext.getAutowireCapableBeanFactory().
                        createBean(sprintServiceServiceFactoryClass, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false)).get();
            }
        } catch (Exception e) {
            log.debug("Could not create SpringService", e);
        }
    }

    private Class<?> getSprintServiceComponentServiceFactoryClass() {
        try {
            // Check to see if the class we depend on (from Zapi) is available
            getClass().getClassLoader().loadClass("com.atlassian.greenhopper.service.sprint.SprintService");
            // If we get to this point in the code, Zapi is available. Let's load the service factory
            return getClass().getClassLoader().loadClass("com.thed.zephyr.util.optionalservice.GHSprintServiceFactory");
        } catch (Exception e) {
            log.warn("The necessary SpringService class is unavailable.");
            return null;
        }
    }
}
