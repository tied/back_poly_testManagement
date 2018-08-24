package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DataCenterStatus")
public class DataCenterStatus {
	@XmlElement
    public boolean statusFlag;

	public boolean isStatusFlag() {
		return statusFlag;
	}

	public void setStatusFlag(boolean statusFlag) {
		this.statusFlag = statusFlag;
	}

	
	
	
		
}
