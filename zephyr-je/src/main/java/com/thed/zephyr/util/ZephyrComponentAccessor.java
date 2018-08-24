package com.thed.zephyr.util;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.atlassian.activeobjects.external.ActiveObjects;

public class ZephyrComponentAccessor implements ApplicationContextAware{
	
	private static ZephyrComponentAccessor instance;
	
	private ApplicationContext applicationContext;

	private ActiveObjects activeObjects;
	
	
	public ZephyrComponentAccessor(ActiveObjects ao) {
		super();
		this.activeObjects = ao;
		instance = this;
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	public Object getComponent(String key){
		if(isSetup())
			return applicationContext.getBean(key);
		return null;
	}


	public static Boolean isSetup(){
		return instance != null;
	}

	public static ZephyrComponentAccessor getInstance() {
		return instance;
	}

	public ActiveObjects getActiveObjects() {
		return activeObjects;
	}
	
}
