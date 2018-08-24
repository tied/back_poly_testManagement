package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "columnPosition")
public class ColumnItemLayoutBean {

	@XmlElement
	private Integer id; 
	@XmlElement
	private String filterIdentifier;
	@XmlElement
	private Integer orderId;
	@XmlElement
	private boolean visible;
	@XmlElement
	private Integer customFieldId;

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	/**
	 * @return the filterIdentifier
	 */
	public String getFilterIdentifier() {
		return filterIdentifier;
	}
	/**
	 * @param filterIdentifier the filterIdentifier to set
	 */
	public void setFilterIdentifier(String filterIdentifier) {
		this.filterIdentifier = filterIdentifier;
	}
	/**
	 * @return the orderId
	 */
	public Integer getOrderId() {
		return orderId;
	}
	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ColumnItemLayoutBean [id=" + id + ", filterIdentifier="
				+ filterIdentifier + ", orderId=" + orderId + "]";
	}
	/**
	 * @return the visibility
	 */
	public boolean isVisible() {
		return visible;
	}
	/**
	 * @param visible the visibility to set
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

    public Integer getCustomFieldId() {
        return customFieldId;
    }

    public void setCustomFieldId(Integer customFieldId) {
        this.customFieldId = customFieldId;
    }
}
