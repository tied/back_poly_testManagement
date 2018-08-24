package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @since v2.5
 */
@XmlRootElement(name = "ExecutionDefect")
public class ExecutionDefectBean {
	@XmlElement
	private Integer defectId;
	
	@XmlElement
	private String defectKey;

	@XmlElement
	private String defectSummary;
	
	@XmlElement
	private String defectStatus;

	@XmlElement
	private String defectResolutionId;

	public Integer getDefectId() {
		return defectId;
	}

	public void setDefectId(Integer defectId) {
		this.defectId = defectId;
	}




	@Override
	public String toString() {
		return "ExecutionDefectBean [defectId=" + defectId + ", defectKey="
				+ defectKey + ", defectSummary=" + defectSummary
				+ ", defectStatus=" + defectStatus + "]";
	}

	public String getDefectStatus() {
		return defectStatus;
	}

	public void setDefectStatus(String defectStatus) {
		this.defectStatus = defectStatus;
	}

	public String getDefectKey() {
		return defectKey;
	}

	public void setDefectKey(String defectKey) {
		this.defectKey = defectKey;
	}

	public String getDefectSummary() {
		return defectSummary;
	}

	public void setDefectSummary(String defectSummary) {
		this.defectSummary = defectSummary;
	}

	public String getDefectResolutionId() {
		return defectResolutionId;
	}

	public void setDefectResolutionId(String defectResolutionId) {
		this.defectResolutionId = defectResolutionId;
	}
}
