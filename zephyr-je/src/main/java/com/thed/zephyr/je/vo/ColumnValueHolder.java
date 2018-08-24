package com.thed.zephyr.je.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ColumnValueHolder {
	/* column value */
	private String value;

	/* FieldConfig details */
	private ImportFieldConfig fieldConfig;

	/* holds index number of excel row */
	private List<Integer> truncateRowIndex = new ArrayList<Integer>();

	private boolean truncateInfoRequired;

	/* if it is a list type custom field, holds preference values */
	private Map<String, String> preferenceMap;

	private String mappedField;
	
	public ColumnValueHolder() {
		super();
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public ImportFieldConfig getFieldConfig() {
		return fieldConfig;
	}

	public void setFieldConfig(ImportFieldConfig fieldConfig) {
		this.fieldConfig = fieldConfig;
	}

	public void addTruncatedRowIndex(int index) {
		truncateRowIndex.add(index);
	}

	public List<Integer> getTruncateRowIndex() {
		return truncateRowIndex;
	}

	public boolean isTruncateInfoRequired() {
		return truncateInfoRequired;
	}

	public void setTruncateInfoRequired(boolean truncateInfoRequired) {
		this.truncateInfoRequired = truncateInfoRequired;
	}

	public Map<String, String> getPreferenceMap() {
		return preferenceMap;
	}

	public void setPreferenceMap(Map<String, String> preferenceMap) {
		this.preferenceMap = preferenceMap;
	}

	public void setMappedField(String mappedField) {
		this.mappedField = mappedField;
		
	}

	public String getMappedField() {
		return mappedField;
	}

}