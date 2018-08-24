package com.thed.zephyr.je.zql.model;

import net.java.ao.Entity;
import net.java.ao.Preload;

@Preload
public interface ZQLSharePermissions extends Entity{

	public String getShareType();
	public void setShareType(String shareType);
	
	public String getParam1();
	public void setParam1(String param1);
	
	public String getParam2();
	public void setParam2(String param2);
	
    public ZQLFilter getZQLFilter();
    public void setZQLFilter(ZQLFilter zqlFilter);
}
