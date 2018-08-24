package com.thed.zephyr.je.vo;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CycleSummary")
public class CycleSummaryPreferenceBean {

    @XmlElement
    public Boolean status = Boolean.TRUE;

    @XmlElement
    public Boolean summary = Boolean.TRUE;

    @XmlElement
    public Boolean defect = Boolean.TRUE;

    @XmlElement
    public Boolean component = Boolean.TRUE;

    @XmlElement
    public Boolean label = Boolean.TRUE;

    @XmlElement
    public Boolean executedBy = Boolean.TRUE;

    @XmlElement
    public Boolean executedOn = Boolean.TRUE;

    @XmlElement
    public Boolean assignee = Boolean.TRUE;

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getSummary() {
        return summary;
    }

    public void setSummary(Boolean summary) {
        this.summary = summary;
    }

    public Boolean getDefect() {
        return defect;
    }

    public void setDefect(Boolean defect) {
        this.defect = defect;
    }

    public Boolean getComponent() {
        return component;
    }

    public void setComponent(Boolean component) {
        this.component = component;
    }

    public Boolean getLabel() {
        return label;
    }

    public void setLabel(Boolean label) {
        this.label = label;
    }

    public Boolean getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(Boolean executedBy) {
        this.executedBy = executedBy;
    }

    public Boolean getExecutedOn() {
        return executedOn;
    }

    public void setExecutedOn(Boolean executedOn) {
        this.executedOn = executedOn;
    }

    public Boolean getAssignee() {
        return assignee;
    }

    public void setAssignee(Boolean assignee) {
        this.assignee = assignee;
    }

    @Override
    public String toString() {
        return "CycleSummaryPreferenceBean{" +
                "status=" + status +
                ", summary=" + summary +
                ", defect=" + defect +
                ", component=" + component +
                ", label=" + label +
                ", executedBy=" + executedBy +
                ", executedOn=" + executedOn +
                ", assignee=" + assignee +
                '}';
    }
}
