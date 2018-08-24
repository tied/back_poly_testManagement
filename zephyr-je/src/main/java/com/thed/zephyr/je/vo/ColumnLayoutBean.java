package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "columnPosition")
public class ColumnLayoutBean {

	@XmlElement
	private Integer id; 
	@XmlElement
	private String userName;
	@XmlElement
	private Integer executionFilterId;
	@XmlElement
	private List<ColumnItemLayoutBean> columnItemBean;

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
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the columnItemBean
	 */
	public List<ColumnItemLayoutBean> getColumnItemBean() {
		return columnItemBean;
	}
	/**
	 * @param columnItemBean the columnItemBean to set
	 */
	public void setColumnItemBean(List<ColumnItemLayoutBean> columnItemBean) {
		this.columnItemBean = columnItemBean;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ColumnLayoutBean ["
				+ (id != null ? "id=" + id + ", " : "")
				+ (userName != null ? "userName=" + userName + ", " : "")
				+ (executionFilterId != null ? "executionFilterId=" + executionFilterId + ", "
						: "")
				+ (columnItemBean != null ? "columnItemBean=" + columnItemBean
						: "") + "]";
	}
	/**
	 * @return the executionFilterId
	 */
	public Integer getExecutionFilterId() {
		return executionFilterId;
	}
	/**
	 * @param executionFilterId the executionFilterId to set
	 */
	public void setExecutionFilterId(Integer executionFilterId) {
		this.executionFilterId = executionFilterId;
	}

}
