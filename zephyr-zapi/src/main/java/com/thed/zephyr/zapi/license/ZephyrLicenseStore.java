package com.thed.zephyr.zapi.license;

public interface ZephyrLicenseStore {

	public ZephyrLicense retrieve();
    public String getUserEnteredLicenseString();
}
