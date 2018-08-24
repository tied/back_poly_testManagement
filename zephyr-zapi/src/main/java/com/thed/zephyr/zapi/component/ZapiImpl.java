package com.thed.zephyr.zapi.component;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.thed.zephyr.zapi.license.ZephyrLicenseManager;
import com.thed.zephyr.zapi.util.ConfigurationConstants;
import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: mukul
 * Date: 12/30/13
 * Time: 12:06 PM
 */

public class ZapiImpl implements Zapi {

    protected final Logger log = Logger.getLogger(ZapiImpl.class);

    private final ZephyrLicenseManager zephyrLicenseManager;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;

    public ZapiImpl(ZephyrLicenseManager zephyrLicenseManager, DateTimeFormatterFactory dateTimeFormatterFactory) {
        this.zephyrLicenseManager = zephyrLicenseManager;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
    }

    @Override
    public JSONObject getZapiConfig() {
        log.info("Reading Zapi plugin license information");
        //Add License Validation licId
        JSONObject json = new JSONObject();
        try {
            PluginLicense license = zephyrLicenseManager.getZephyrMarketplaceLicense();
            if(license != null){
                json.put("licValid", license.isValid());
                json.put("SEN", license.getSupportEntitlementNumber().get());
                String expDateString = dateTimeFormatterFactory.formatter().forLoggedInUser().format(license.getMaintenanceExpiryDate().get().toDate());
                String licenseExpiryStatusMessage = "<br/><small> Support available until (<strong>"+expDateString+"</strong>).</small>";
                json.put("licenseInformation", license.getDescription() + licenseExpiryStatusMessage);
                json.put("licMaintenanceExpired", license.isMaintenanceExpired());
                json.put("licMaintenanceExpirationDate", license.getMaintenanceExpiryDate().get().toString());
                json.put("version", ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_PRODUCT_VERSION));
            }else{
                json.put("licValid", false);
                json.put("SEN", "");
                json.put("licenseInformation", "No license found");
                json.put("licMaintenanceExpired", false);
                json.put("version", ComponentAccessor.getPluginAccessor().getPlugin(ConfigurationConstants.PLUGIN_KEY).getPluginInformation().getParameters().get(ConfigurationConstants.ZEPHYR_JE_PRODUCT_VERSION));
            }
        } catch(JSONException e) {
            log.error("Error reading Zapi plugin license information", e);
        }
        log.info("Zapi plugin license information returned");
        return json;
    }
}
