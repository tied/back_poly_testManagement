package com.thed.zephyr.je.vo;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "RecoveryForm")
public class RecoveryFormBean {
	@XmlElement
    public String recoveryEnabled;
	@XmlElement
    public String mode;
	@XmlElement
    public String daysOfMonth;
	@XmlElement
    public String monthDay;
	@XmlElement
    public String week;
	@XmlElement
    public String day;
	@XmlElement
    public String interval;
	@XmlElement
    public String onceHours;
	@XmlElement
    public String onceMinutes;
	@XmlElement
    public String onceMeridian;
	@XmlElement
    public String fromHours;
	@XmlElement
    public String fromMeridian;
	@XmlElement
    public String toHours;
	@XmlElement
    public String toMeridian;
	@XmlElement
    public List<String> weekdays;
	@XmlElement
    public String cronString;
	@XmlElement
    public String rootPath;
	@XmlElement
    public String serverTime;
	@XmlElement
    public String errorMessage;
	@XmlElement
    public String status;
	
	
	public String getRecoveryEnabled() {
		return recoveryEnabled;
	}
	public void setRecoveryEnabled(String recoveryEnabled) {
		this.recoveryEnabled = recoveryEnabled;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getDaysOfMonth() {
		return daysOfMonth;
	}
	public void setDaysOfMonth(String daysOfMonth) {
		this.daysOfMonth = daysOfMonth;
	}
	public String getMonthDay() {
		return monthDay;
	}
	public void setMonthDay(String monthDay) {
		this.monthDay = monthDay;
	}
	public String getWeek() {
		return week;
	}
	public void setWeek(String week) {
		this.week = week;
	}
	public String getDay() {
		return day;
	}
	public void setDay(String day) {
		this.day = day;
	}
	public String getInterval() {
		return interval;
	}
	public void setInterval(String interval) {
		this.interval = interval;
	}
	public String getOnceHours() {
		return onceHours;
	}
	public void setOnceHours(String onceHours) {
		this.onceHours = onceHours;
	}
	public String getOnceMinutes() {
		return onceMinutes;
	}
	public void setOnceMinutes(String onceMinutes) {
		this.onceMinutes = onceMinutes;
	}
	public String getOnceMeridian() {
		return onceMeridian;
	}
	public void setOnceMeridian(String onceMeridian) {
		this.onceMeridian = onceMeridian;
	}
	public String getFromHours() {
		return fromHours;
	}
	public void setFromHours(String fromHours) {
		this.fromHours = fromHours;
	}
	public String getFromMeridian() {
		return fromMeridian;
	}
	public void setFromMeridian(String fromMeridian) {
		this.fromMeridian = fromMeridian;
	}
	public String getToHours() {
		return toHours;
	}
	public void setToHours(String toHours) {
		this.toHours = toHours;
	}
	public String getToMeridian() {
		return toMeridian;
	}
	public void setToMeridian(String toMeridian) {
		this.toMeridian = toMeridian;
	}
	public List<String> getWeekdays() {
		return weekdays;
	}
	public void setWeekdays(List<String> weekdays) {
		this.weekdays = weekdays;
	}
	public String getCronString() {
		return cronString;
	}
	public void setCronString(String cronString) {
		this.cronString = cronString;
	}
	public String getRootPath() {
		return rootPath;
	}
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}
	public String getServerTime() {
		return serverTime;
	}
	public void setServerTime(String serverTime) {
		this.serverTime = serverTime;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	
	
}
