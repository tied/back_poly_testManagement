package com.thed.zephyr.je.vo;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExecutionStatusCountBean {

    @XmlElement
    private String statusName;
    @XmlElement
    private Integer statusCount;
    @XmlElement
    private String statusColor;

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public Integer getStatusCount() {
        return statusCount;
    }

    public void setStatusCount(Integer statusCount) {
        this.statusCount = statusCount;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
    }

    @Override
    public String toString() {
        return "{" +
                "statusName='" + statusName + '\'' +
                ", statusCount=" + statusCount +
                ", statusColor='" + statusColor + '\'' +
                '}';
    }
}
