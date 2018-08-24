package com.thed.zephyr.je.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "testStepBeanWrapper")
public class TeststepBeanWrapper {

    @XmlElement
    public List<TeststepBean> stepBeanCollection;

    @XmlElement
    public TeststepBean prevTestStepBean;

    @XmlElement
    public TeststepBean nextTestStepBean;

    @XmlElement
    public TeststepBean firstElementOnNextPage;

    @XmlElement
    public Boolean isLastElementOnPage;

    public List<TeststepBean> getStepBeanCollection() {
        return stepBeanCollection;
    }

    public void setStepBeanCollection(List<TeststepBean> stepBeanCollection) {
        this.stepBeanCollection = stepBeanCollection;
    }

    public TeststepBean getPrevTestStepBean() {
        return prevTestStepBean;
    }

    public void setPrevTestStepBean(TeststepBean prevTestStepBean) {
        this.prevTestStepBean = prevTestStepBean;
    }

    public TeststepBean getNextTestStepBean() {
        return nextTestStepBean;
    }

    public void setNextTestStepBean(TeststepBean nextTestStepBean) {
        this.nextTestStepBean = nextTestStepBean;
    }

    public TeststepBean getFirstElementOnNextPage() {
        return firstElementOnNextPage;
    }

    public void setFirstElementOnNextPage(TeststepBean firstElementOnNextPage) {
        this.firstElementOnNextPage = firstElementOnNextPage;
    }

    public Boolean getLastElementOnPage() {
        return isLastElementOnPage;
    }

    public void setLastElementOnPage(Boolean lastElementOnPage) {
        isLastElementOnPage = lastElementOnPage;
    }
}
