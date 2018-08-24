package com.thed.zephyr.je.vo;

import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
@XmlRootElement(name = "ImportPreference")
public class ImportPreference {
	@XmlElement
	private String id;
	@XmlElement
	private String fileType;
	@XmlElement
	private Set<ImportFieldMapping> mappingSet;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getFileType() {
		return fileType;
	}
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}
	public Set<ImportFieldMapping> getMappingSet() {
		return mappingSet;
	}
	public void setMappingSet(Set<ImportFieldMapping> mappingSet) {
		this.mappingSet = mappingSet;
	}
	@Override
	public String toString() {
		return "ImportMappingPreference [id=" + id + ", fileType=" + fileType
				+ ", mappingSet=" + mappingSet + "]";
	}


}
