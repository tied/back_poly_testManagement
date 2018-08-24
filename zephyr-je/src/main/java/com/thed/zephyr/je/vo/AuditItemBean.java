package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;

public class AuditItemBean {

	@XmlElement
	private int id;
	
	@XmlElement
	private String field;
	
	@XmlElement
	private String oldValue;
	
	@XmlElement
	private String newValue;

	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getField() {
		return field;
	}
	public void setField(String field) {
		this.field = field;
	}
	public String getOldValue() {
		return oldValue;
	}
	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}
	public String getNewValue() {
		return newValue;
	}
	public void setNewValue(String newValue) {
		this.newValue = newValue;
	}
}
