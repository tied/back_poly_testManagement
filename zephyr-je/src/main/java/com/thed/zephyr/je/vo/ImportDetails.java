package com.thed.zephyr.je.vo;

import java.util.HashSet;
import java.util.Set;

import com.thed.zephyr.util.ExcelDiscriminator;

@SuppressWarnings("serial")
public class ImportDetails implements java.io.Serializable{
	private Integer startingRowNumber;

	private String discriminator;
	
	private String sheetFilter;
	
	private boolean importAllSheetsFlag;

	private Set<ImportFieldMapping> fieldMappingSet = new HashSet<ImportFieldMapping>();

	public ImportDetails(){
		super();
	}
	
	public ImportDetails(Integer startingRowNumber, String discriminator, String sheetFilter, boolean importAllSheetsFlag, Set<ImportFieldMapping> fieldMappingSet) {
		super();
		this.startingRowNumber = startingRowNumber;
		this.discriminator = discriminator;
		this.fieldMappingSet = fieldMappingSet;
		this.sheetFilter = sheetFilter;
		this.importAllSheetsFlag = importAllSheetsFlag;
	}

	public Integer getStartingRowNumber() {
		return startingRowNumber;
	}

	public void setStartingRowNumber(Integer startingRowNumber) {
		this.startingRowNumber = startingRowNumber;
	}

	public String getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(String discriminator) {
		this.discriminator = discriminator;
	}
	
	public String getSheetFilter() {
		return sheetFilter;
	}

	public void setSheetFilter(String sheetFilter) {
		this.sheetFilter = sheetFilter;
	}

	public boolean isImportAllSheetsFlag() {
		return importAllSheetsFlag;
	}

	public void setImportAllSheetsFlag(boolean importAllSheetsFlag) {
		this.importAllSheetsFlag = importAllSheetsFlag;
	}

	public Set<ImportFieldMapping> getFieldMappingSet() {
		return fieldMappingSet;
	}

	public void setFieldMappingSet(Set<ImportFieldMapping> fieldMappingSet) {
		this.fieldMappingSet = fieldMappingSet;
	}
}
