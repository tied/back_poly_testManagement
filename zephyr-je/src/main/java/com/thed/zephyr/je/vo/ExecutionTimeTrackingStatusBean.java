package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ExecutionTimeTrackingStatusBean {

    @XmlElement
    private Long totalExecutionEstimatedTime = new Long(0);
    @XmlElement
    private Long totalExecutionLoggedTime = new Long(0);
    @XmlElement
    private String totalExecutionEstimatedTimeDurationStr;
    @XmlElement
    private String totalExecutionLoggedTimeDurationStr;

    public Long getTotalExecutionEstimatedTime() {
        return totalExecutionEstimatedTime;
    }

    public void setTotalExecutionEstimatedTime(Long totalExecutionEstimatedTime) {
        this.totalExecutionEstimatedTime = totalExecutionEstimatedTime;
    }

    public Long getTotalExecutionLoggedTime() {
        return totalExecutionLoggedTime;
    }

    public void setTotalExecutionLoggedTime(Long totalExecutionLoggedTime) {
        this.totalExecutionLoggedTime = totalExecutionLoggedTime;
    }

    public String getTotalExecutionEstimatedTimeDurationStr() {
        return totalExecutionEstimatedTimeDurationStr;
    }

    public void setTotalExecutionEstimatedTimeDurationStr(String totalExecutionEstimatedTimeDurationStr) {
        this.totalExecutionEstimatedTimeDurationStr = totalExecutionEstimatedTimeDurationStr;
    }

    public String getTotalExecutionLoggedTimeDurationStr() {
        return totalExecutionLoggedTimeDurationStr;
    }

    public void setTotalExecutionLoggedTimeDurationStr(String totalExecutionLoggedTimeDurationStr) {
        this.totalExecutionLoggedTimeDurationStr = totalExecutionLoggedTimeDurationStr;
    }
}
