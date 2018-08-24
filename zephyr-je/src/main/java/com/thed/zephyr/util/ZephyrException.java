package com.thed.zephyr.util;

@SuppressWarnings("serial")
public class ZephyrException extends Exception
{
    private String id = "";

    public ZephyrException()
    {
        super();
    }

    public ZephyrException(Throwable th)
    {
        super(th);
    }

    public ZephyrException(String id, String msg)
    {
        super(msg);
        this.id = id;
    }

    public ZephyrException(String id, String msg, Throwable th)
    {
        super(msg, th);
        this.id = id;
    }
    
    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id;
    }
}
