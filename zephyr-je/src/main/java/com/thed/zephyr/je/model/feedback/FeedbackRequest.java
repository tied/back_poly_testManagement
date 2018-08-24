package com.thed.zephyr.je.model.feedback;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by Masud on 1/4/18.
 */
@XmlRootElement(name = "FeedbackRequest")
public class FeedbackRequest {

    @XmlElement
    private String userName;

    @XmlElement
    private String summary;

    @XmlElement
    private String description;

    @XmlElement
    private String component;

    @XmlElement
    private String email;

    @XmlElement
    private boolean sendAnonymous;

    public FeedbackRequest() {
    }

    public FeedbackRequest(String userName, String summary, String description, String component, String email, boolean sendAnonymous) {
        this.userName = userName;
        this.summary = summary;
        this.description = description;
        this.component = component;
        this.email = email;
        this.sendAnonymous = sendAnonymous;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isSendAnonymous() {
        return sendAnonymous;
    }

    public void setSendAnonymous(boolean sendAnonymous) {
        this.sendAnonymous = sendAnonymous;
    }
}