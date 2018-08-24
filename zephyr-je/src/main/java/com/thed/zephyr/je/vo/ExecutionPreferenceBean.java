package com.thed.zephyr.je.vo;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ExecutionPreference")
public class ExecutionPreferenceBean {

    @XmlElement
    public Boolean testStep = Boolean.TRUE;

    @XmlElement
    public Boolean testdata = Boolean.TRUE;

    @XmlElement
    public Boolean expectedResult = Boolean.TRUE;

    @XmlElement
    public Boolean stepAttachment = Boolean.TRUE;

    @XmlElement
    public Boolean status = Boolean.TRUE;

    @XmlElement
    public Boolean comment = Boolean.TRUE;

    @XmlElement
    public Boolean attachments = Boolean.TRUE;

    @XmlElement
    public Boolean defects = Boolean.TRUE;

    public Boolean getTestStep() {
        return testStep;
    }

    public void setTestStep(Boolean testStep) {
        this.testStep = testStep;
    }

    public Boolean getTestdata() {
        return testdata;
    }

    public void setTestdata(Boolean testdata) {
        this.testdata = testdata;
    }

    public Boolean getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(Boolean expectedResult) {
        this.expectedResult = expectedResult;
    }

    public Boolean getStepAttachment() {
        return stepAttachment;
    }

    public void setStepAttachment(Boolean stepAttachment) {
        this.stepAttachment = stepAttachment;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Boolean getComment() {
        return comment;
    }

    public void setComment(Boolean comment) {
        this.comment = comment;
    }

    public Boolean getAttachments() {
        return attachments;
    }

    public void setAttachments(Boolean attachments) {
        this.attachments = attachments;
    }

    public Boolean getDefects() {
        return defects;
    }

    public void setDefects(Boolean defects) {
        this.defects = defects;
    }

    @Override
    public String toString() {
        return "ExecutionPreferenceBean{" +
                "testStep=" + testStep +
                ", testdata=" + testdata +
                ", expectedResult=" + expectedResult +
                ", stepAttachment=" + stepAttachment +
                ", status=" + status +
                ", comment=" + comment +
                ", attachments=" + attachments +
                ", defects=" + defects +
                '}';
    }
}
