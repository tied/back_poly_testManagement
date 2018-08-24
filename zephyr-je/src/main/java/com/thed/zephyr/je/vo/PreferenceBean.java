package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;

/**
 * Created by dubey on 27-07-2017.
 */
@XmlRootElement(name = "PreferenceBean")
public class PreferenceBean {


    @XmlElement
    private Map<String,Map<String,String>> preferences;

    @XmlElement
    private String paginationWidth;

    public Map<String, Map<String, String>> getPreferences() {
        return preferences;
    }

    public void setPreferences(Map<String, Map<String, String>> preferences) {
        this.preferences = preferences;
    }

    public String getPaginationWidth() {
        return paginationWidth;
    }

    public void setPaginationWidth(String paginationWidth) {
        this.paginationWidth = paginationWidth;
    }
}
