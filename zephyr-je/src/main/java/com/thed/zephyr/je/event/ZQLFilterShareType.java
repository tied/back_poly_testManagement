package com.thed.zephyr.je.event;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public enum ZQLFilterShareType {
	GLOBAL(1, "global"), PRIVATE(2, "private");
	final static Map<Integer, ZQLFilterShareType> allValues = ImmutableMap.of(GLOBAL.getShareTypeIntVal(), GLOBAL, PRIVATE.getShareTypeIntVal(), PRIVATE);

	private String shareType = null;
	private Integer shareTypeIntVal = -1;
	ZQLFilterShareType(Integer shareTypeIntVal, String shareType) {
		this.shareTypeIntVal = shareTypeIntVal;
		this.shareType = shareType;
	}

	public String getShareType() {
		return shareType;
	}
	
	public Integer getShareTypeIntVal() {
		return shareTypeIntVal;
	}
	
	/*Using ordinal could be error prone*/
	public static ZQLFilterShareType getZQLFilterShareType(int shareTypeId){
		return allValues.get(shareTypeId);
	}
}
