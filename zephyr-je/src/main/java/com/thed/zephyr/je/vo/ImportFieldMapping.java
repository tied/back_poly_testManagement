package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;
@XmlRootElement(name = "ImportFieldMapping")
public class ImportFieldMapping {
	@XmlElement
	private String zephyrField;
	@XmlElement
	private String mappedField;

	public ImportFieldMapping(){
		super();
	}
	
	public ImportFieldMapping(String zephyrField, String mappedField) {
		super();
		this.zephyrField = zephyrField;
		this.mappedField = mappedField;
	}

	public boolean equals(Object o) {
		if (o != null && o instanceof ImportFieldMapping) {
			ImportFieldMapping that = (ImportFieldMapping)o;
			//			return this.zephyrField.equals(that.zephyrField) && this.mappedField.equals(that.mappedField);
			return (this.zephyrField==that.zephyrField) && (this.mappedField==that.mappedField);
		} else {
			return false;
		}
	}
	public int hashCode() {
		return zephyrField.hashCode() +  (mappedField == null ? 13 : mappedField.hashCode());
	}


	/**
	 * @return the mappedField
	 */
	public String getMappedField() {
		return mappedField;
	}

	/**
	 * @param mappedField the mappedField to set
	 */
	public void setMappedField(String mappedField) {
		if (StringUtils.isNotBlank(mappedField)) {
			this.mappedField = mappedField;
		} else {
			this.mappedField = null;
		}
	}

	/**
	 * @return the zephyrField
	 */
	public String getZephyrField() {
		return zephyrField;
	}

	/**
	 * @param zephyrField the zephyrField to set
	 */
	public void setZephyrField(String zephyrField) {
		this.zephyrField = zephyrField;
	}

	@Override
	public String toString() {
		return "ImportFieldMapping [zephyrField=" + zephyrField
				+ ", mappedField=" + mappedField + "]";
	}
	
	

}
