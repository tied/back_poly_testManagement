package com.thed.zephyr.je.model;

import net.java.ao.Entity;

import java.util.Map;

public interface AOEntityHolder<T extends Entity> {
	public Map<String, Object> getProperties();
	/**
	 * Can be used to delay the look up or creation of entity till its really needed
	 */
	public T fetchOrCreate();

    /**
     * create entity.
     */
    public T create();
}
