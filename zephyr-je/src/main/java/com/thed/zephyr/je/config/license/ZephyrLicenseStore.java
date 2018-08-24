package com.thed.zephyr.je.config.license;

public interface ZephyrLicenseStore {

	public ZephyrLicense retrieve();
    public void store(final String licenseString);
    public String getUserEnteredLicenseString();

}
