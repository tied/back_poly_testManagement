package com.thed.zephyr.zapi.util;

import com.atlassian.license.LicenseException;

@SuppressWarnings("serial")
public class ZephyrLicenseException extends LicenseException
{
    private int exceptionId;

    public ZephyrLicenseException(Throwable th)
    {
        super(th);
    }

    public ZephyrLicenseException(int exceptionId, String msg)
    {
        super(msg);
        this.exceptionId = exceptionId;
    }

    public ZephyrLicenseException(int exceptionId, String msg, Throwable th)
    {
        super(msg, th);
        this.exceptionId = exceptionId;
    }
    
    public int getExceptionId()
    {
        return this.exceptionId;
    }
    
    public void setExceptionId(int exceptionId)
    {
        this.exceptionId = exceptionId;
    }
}