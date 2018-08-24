package com.thed.zephyr.je.vo;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "TeststepPreference")
public class TeststepPreferenceBean {

    @XmlElement
    public Boolean isTestStepVisible = Boolean.TRUE;

    @XmlElement
    public Boolean isTestStepDataVisible = Boolean.TRUE;

    @XmlElement
    public Boolean isTestStepExpectedResultVisible = Boolean.TRUE;

    @XmlElement
    public Boolean isTestStepAttachmentVisible = Boolean.TRUE;

    public Boolean getTestStepVisible() {
        return isTestStepVisible;
    }

    public void setTestStepVisible(Boolean testStepVisible) {
        isTestStepVisible = testStepVisible;
    }

    public Boolean getTestStepDataVisible() {
        return isTestStepDataVisible;
    }

    public void setTestStepDataVisible(Boolean testStepDataVisible) {
        isTestStepDataVisible = testStepDataVisible;
    }

    public Boolean getTestStepExpectedResultVisible() {
        return isTestStepExpectedResultVisible;
    }

    public void setTestStepExpectedResultVisible(Boolean testStepExpectedResultVisible) {
        isTestStepExpectedResultVisible = testStepExpectedResultVisible;
    }

    public Boolean getTestStepAttachmentVisible() {
        return isTestStepAttachmentVisible;
    }

    public void setTestStepAttachmentVisible(Boolean testStepAttachmentVisible) {
        isTestStepAttachmentVisible = testStepAttachmentVisible;
    }

    @Override
    public String toString() {
        return "TeststepPreferenceBean{" +
                "isTestStepVisible=" + isTestStepVisible +
                ", isTestStepDataVisible=" + isTestStepDataVisible +
                ", isTestStepExpectedResultVisible=" + isTestStepExpectedResultVisible +
                ", isTestStepAttachmentVisible=" + isTestStepAttachmentVisible +
                '}';
    }
}
