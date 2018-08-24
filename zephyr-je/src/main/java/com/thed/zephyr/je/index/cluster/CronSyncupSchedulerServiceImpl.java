package com.thed.zephyr.je.index.cluster;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.scheduler.cron.CronExpressionValidator;
import com.thed.zephyr.je.rest.delegate.ScheduleResourceDelegate;
import com.thed.zephyr.je.vo.RecoveryFormBean;
import com.thed.zephyr.util.ApplicationConstants;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;

public class CronSyncupSchedulerServiceImpl implements LifecycleAware, CronSyncupSchedulerService {

	protected final Logger log = Logger.getLogger(CronSyncupSchedulerServiceImpl.class);

	private static final String BACKUP_COLUMN_KEY = "backup-recovery";
	private final SchedulerService schedulerService;
	private final CronExpressionValidator cronExpressionValidator;
	private final ScheduleResourceDelegate scheduleResourceDelegate;

	private static final JobRunnerKey CRON_SERVICE = JobRunnerKey.of("cronService");

	public CronSyncupSchedulerServiceImpl(SchedulerService schedulerService,
			CronExpressionValidator cronExpressionValidator, ScheduleResourceDelegate scheduleResourceDelegate) {
		this.schedulerService = schedulerService;
		this.cronExpressionValidator = cronExpressionValidator;
		this.scheduleResourceDelegate = scheduleResourceDelegate;
	}

	private void registerJobRunner() {
		schedulerService.registerJobRunner(CRON_SERVICE, new CronSyncupRunnerImpl(scheduleResourceDelegate));
	}

	private void registerBackUpJobRunner() {
		schedulerService.registerJobRunner(CRON_SERVICE, new CronBackupIndexRunnerImpl(scheduleResourceDelegate));
	}

	@Override
	public RecoveryFormBean schduleWithExpression(String expression,String userId) throws IOException{
		log.debug("Start cron job to back up the index files.");
		RecoveryFormBean recoveryFormBean = new RecoveryFormBean();
		schedulerService.unscheduleJob(JobId.of(ConfigurationConstants.BACKUPINDEXFILES_JOB_NAME));
		if (cronExpressionValidator.isValid(expression)) {
			recoveryFormBean= validateCronExpression(expression);
			Schedule schedule = Schedule.forCronExpression(expression);
			JobConfig jobConfig = JobConfig.forJobRunnerKey(CRON_SERVICE).withSchedule(schedule)
					.withRunMode(RunMode.RUN_LOCALLY);

			JobId jobId = JobId.of(ConfigurationConstants.BACKUPINDEXFILES_JOB_NAME);
			
			try {
				recoveryFormBean.setServerTime(getServerTime());
				recoveryFormBean.setRootPath(getRecoveryPath());
				
				registerBackUpJobRunner();
				schedulerService.scheduleJob(jobId, jobConfig);
				JiraUtil.setBackupRecoveryByKey(BACKUP_COLUMN_KEY, getObjectMapper().writeValueAsString(recoveryFormBean));
				
				log.debug("User ["+userId+"] scheduleed cron job based on the cron expression ["+expression+"]");
				
				recoveryFormBean = getrecoveryForm();
			} catch (SchedulerServiceException e) {
				log.debug("Error occered while schedule back index files job"+e);
			}
		}else {
			recoveryFormBean.setServerTime(getServerTime());
			recoveryFormBean.setRootPath(getRecoveryPath());
			recoveryFormBean.setErrorMessage(ApplicationConstants.CRON_ERROR);
			recoveryFormBean.setCronString(expression);
			recoveryFormBean.setRecoveryEnabled(Boolean.TRUE.toString());
			JiraUtil.setBackupRecoveryByKey(BACKUP_COLUMN_KEY, getObjectMapper().writeValueAsString(recoveryFormBean));
			log.debug("User ["+userId+"] scheduleed cron job based on the cron expression ["+expression+"]");
			recoveryFormBean = getrecoveryForm();	
		}
		
		return recoveryFormBean;
	}

	@Override
	public String schduleWithDateTimeInterval(Date date, long intervalInMillis) {
		log.debug("Start cron job to back up the index files.");
		schedulerService.unscheduleJob(JobId.of(ConfigurationConstants.BACKUPINDEXFILES_JOB_NAME));
		JobId jobId = JobId.of(ConfigurationConstants.BACKUPINDEXFILES_JOB_NAME);
		JobConfig jobConfig = JobConfig.forJobRunnerKey(CRON_SERVICE)
				.withSchedule(Schedule.forInterval(intervalInMillis, date)).withRunMode(RunMode.RUN_LOCALLY);
		try {
			registerBackUpJobRunner();
			schedulerService.scheduleJob(jobId, jobConfig);
		} catch (SchedulerServiceException e) {
			log.debug("Error occered while schedule back index files job"+e);
		}
		log.debug("cron job scheduleed based on the cron date ["+date+"] time intervalInMillis ["+intervalInMillis+"]");
		return ApplicationConstants.SUCCESS;
	}

	@Override
	public RecoveryFormBean unSchduleCronJob(String userId) throws IOException {
		log.debug("User ["+userId+"] unSchduled the Cron Job");
		RecoveryFormBean recoveryFormBean = new RecoveryFormBean();
		recoveryFormBean.setRootPath(getRecoveryPath());
		recoveryFormBean.setServerTime(getServerTime());
		recoveryFormBean.setCronString(ApplicationConstants.EMPTY);
		recoveryFormBean.setRecoveryEnabled(Boolean.FALSE.toString());
		JiraUtil.setBackupRecoveryByKey(BACKUP_COLUMN_KEY, getObjectMapper().writeValueAsString(recoveryFormBean));
		schedulerService.unscheduleJob(JobId.of(ConfigurationConstants.BACKUPINDEXFILES_JOB_NAME));
		log.debug("unscheduled the back up the index files");
		recoveryFormBean = getrecoveryForm();
		return recoveryFormBean;
	}

	@Override
	public void onStart() {
		// Daily 10:30 PM syncup job do the syncup index files
		log.debug("unscheduled the daily syncup Job.");
		schedulerService.unscheduleJob(JobId.of(ConfigurationConstants.NEWNODE_SYNC_INDEX_JOB_NAME));
		String expression = "0 0 22 * * ? *";
		if (cronExpressionValidator.isValid(expression)) {
			Schedule schedule = Schedule.forCronExpression(expression);
			JobConfig jobConfig = JobConfig.forJobRunnerKey(CRON_SERVICE).withSchedule(schedule)
					.withRunMode(RunMode.RUN_LOCALLY);

			JobId jobId = JobId.of(ConfigurationConstants.NEWNODE_SYNC_INDEX_JOB_NAME);
			try {
				log.debug("scheduled the daily syncup Job every day night at 10 O clock.");
				registerJobRunner();
				schedulerService.scheduleJob(jobId, jobConfig);
				log.debug("Daily syncup job scheduleed based on the expression ["+expression+"]");
			} catch (SchedulerServiceException e) {
				log.debug("Error occered while schedule syncup job "+e);
			}
		}
	}

	@Override
	public RecoveryFormBean getrecoveryForm() throws IOException {
		RecoveryFormBean recoveryFormBean = new RecoveryFormBean();
		
		String backupRecoveryResponse = JiraUtil.getBackupRecoveryByKey(BACKUP_COLUMN_KEY);
		
		if(StringUtils.isNotBlank(backupRecoveryResponse)) {
			recoveryFormBean =getObjectMapper().readValue(backupRecoveryResponse,RecoveryFormBean.class);
		}
		recoveryFormBean.setServerTime(getServerTime());
		recoveryFormBean.setRootPath(getRecoveryPath());
		return recoveryFormBean;
	}
	
	 private ObjectMapper getObjectMapper() {
	        ObjectMapper mapper = new ObjectMapper();
	        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
	                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
	                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
	                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
	                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
	        return mapper;
	    } 
	 
	 private String getRecoveryPath() throws IOException {
		 return scheduleResourceDelegate.getRecoveryBackUpPath();
	 }
	 
	private String getServerTime() {
		long yourmilliseconds = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
		Date resultdate = new Date(yourmilliseconds);
		TimeZone tz = TimeZone.getDefault();
		tz.getDisplayName();
		return sdf.format(resultdate)+" - "+tz.getDisplayName();
	}
	
	
	private static RecoveryFormBean validateCronExpression(String expression) {
		RecoveryFormBean bean = new RecoveryFormBean();
		List<String> list = new ArrayList<>();
		StringTokenizer expChar = new StringTokenizer(expression, " ");
		
		while (expChar.hasMoreTokens()) {
			list.add(expChar. nextToken().toString());
		}
		//System.out.println("list "+list);
		
		if(list.size()==6) {
			String secs = list.get(0);
			String mins = list.get(1);
			String hrs = list.get(2);
			String days = list.get(3);
			String months = list.get(4);
			String weekDay = list.get(5);
			if(secs.equals("0") && days.equals("*") && months.equals("*") && weekDay.equals("?")) {
				/***** daily expression ***/ 
				//System.out.println("Expression : Daily");
				if(hrs.contains("-") && hrs.contains("/")) {
					// 0 0 1-17/3 * * ?   or       0 0 1-17/2 * * ?    or     0 0 1-17/1 * * ?    scenarios
					String hourly[]= hrs.split("/");
					String hours = hourly[0];
					String intervalHrs = hourly[1];
					
					
					if(getStringToInt(intervalHrs)>=1 && getStringToInt(intervalHrs)<=3) {
						String fromToHrs[]= hours.split("-");
						
						/*if(getStringToInt(fromToHrs[0])>getStringToInt(fromToHrs[1])) {
							System.out.println("Exception : You must select a from time that is before the to time");
							bean.setErrorMessage("Invalid Cron Expression");
							 setAdvancedRecoveryBeanAsEmpty(bean);
						}else*/ if(getStringToInt(fromToHrs[0])<=getStringToInt(fromToHrs[1])){
							if(NumberUtils.isNumber(mins)) {
								if(getStringToInt(mins)==0) {
									bean.setCronString(expression);
									 bean.setMode("daily");
									 	if(getStringToInt(fromToHrs[0])>12) {
												bean.setFromHours(get24Hrs(fromToHrs[0]));
												bean.setFromMeridian(ApplicationConstants.PM);
											}else {
												bean.setFromHours(fromToHrs[0].equals("0")?get24Hrs(fromToHrs[0]):fromToHrs[0]);
												bean.setFromMeridian(ApplicationConstants.AM);
											}
										 if(getStringToInt(fromToHrs[1])>12) {
												bean.setToHours(get24Hrs(fromToHrs[1]));
												bean.setToMeridian(ApplicationConstants.PM);
											}else {
												bean.setToHours(fromToHrs[1].equals("0")?get24Hrs(fromToHrs[1]):fromToHrs[1]);
												bean.setToMeridian(ApplicationConstants.AM);
											}
										 bean.setInterval(String.valueOf(getStringToInt(intervalHrs)*60));
											bean.setDay(ApplicationConstants.EMPTY);
											bean.setDaysOfMonth(ApplicationConstants.EMPTY);
											bean.setMonthDay(ApplicationConstants.EMPTY);
											bean.setOnceHours(ApplicationConstants.EMPTY);
											bean.setOnceMeridian(ApplicationConstants.EMPTY);
											bean.setOnceMinutes(ApplicationConstants.EMPTY);
											bean.setRecoveryEnabled(Boolean.TRUE.toString());
											bean.setWeek(ApplicationConstants.EMPTY);
											bean.setWeekdays(new ArrayList<>());
											bean.setErrorMessage(ApplicationConstants.EMPTY);
								
								}else {
									setRecoveryBeanAsAdvanced(expression,bean);
								}
							}else {
								setRecoveryBeanAsAdvanced(expression,bean);
							}
						}
					}else {
						setRecoveryBeanAsAdvanced(expression,bean);
					}
				}else if (hrs.contains("-")) {
					//  0 0/15 1-18 * * ?       and       0 0/30 0-12 * * ?   	scenarios
					 String fromToHrs[]= hrs.split("-");
					/* if(getStringToInt(fromToHrs[0])>getStringToInt(fromToHrs[1])) {
						 System.out.println("Exception : You must select a from time that is before the to time");
						 bean.setErrorMessage("Invalid Cron Expression");
						 setAdvancedRecoveryBeanAsEmpty(bean);
						 
					}else*/ if(getStringToInt(fromToHrs[0])<=getStringToInt(fromToHrs[1])){
						if(mins.contains("/")) {
							bean.setCronString(expression);
							 bean.setMode("daily");
							 if(getStringToInt(fromToHrs[0])>12) {
										bean.setFromHours(get24Hrs(fromToHrs[0]));
										bean.setFromMeridian(ApplicationConstants.PM);
									}else {
										bean.setFromHours(fromToHrs[0].equals("0")?get24Hrs(fromToHrs[0]):fromToHrs[0]);
										bean.setFromMeridian(ApplicationConstants.AM);
									}
								 if(getStringToInt(fromToHrs[1])>12) {
										bean.setToHours(get24Hrs(fromToHrs[1]));
										bean.setToMeridian(ApplicationConstants.PM);
									}else {
										bean.setToHours(fromToHrs[1].equals("0")?get24Hrs(fromToHrs[1]):fromToHrs[1]);
										bean.setToMeridian(ApplicationConstants.AM);
									}
								 	String minutes[] = mins.split("/"); 
									bean.setInterval(minutes[1]);	
								 	bean.setDay(ApplicationConstants.EMPTY);
									bean.setDaysOfMonth(ApplicationConstants.EMPTY);
									bean.setMonthDay(ApplicationConstants.EMPTY);
									bean.setOnceHours(ApplicationConstants.EMPTY);
									bean.setOnceMeridian(ApplicationConstants.EMPTY);
									bean.setOnceMinutes(ApplicationConstants.EMPTY);
									bean.setRecoveryEnabled(Boolean.TRUE.toString());
									bean.setWeek(ApplicationConstants.EMPTY);
									bean.setWeekdays(new ArrayList<>());
									bean.setErrorMessage(ApplicationConstants.EMPTY);
						}else {
							setRecoveryBeanAsAdvanced(expression,bean);
						}
						 
					}
					 	
						
				}else if(NumberUtils.isNumber(hrs)){
					// 0 0 15 * * ?       and       0 45 15 * * ?   	scenarios  
					
					if(NumberUtils.isNumber(mins) && getStringToInt(mins)==0) {
						bean.setCronString(expression);
						bean.setMode("daily");
						if(getStringToInt(hrs)>12) {
							bean.setOnceHours(get24Hrs(hrs));
							bean.setOnceMeridian(ApplicationConstants.PM);
						}else {
							bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
							bean.setOnceMeridian(ApplicationConstants.AM);
						}
					
						
						bean.setOnceMinutes(mins);
						bean.setDay(ApplicationConstants.EMPTY);
						bean.setDaysOfMonth(ApplicationConstants.EMPTY);
						bean.setFromHours(ApplicationConstants.EMPTY);
						bean.setFromMeridian(ApplicationConstants.EMPTY);
						bean.setInterval(ApplicationConstants.EMPTY);
						bean.setMonthDay(ApplicationConstants.EMPTY);
						bean.setRecoveryEnabled(Boolean.TRUE.toString());
						bean.setToHours(ApplicationConstants.EMPTY);
						bean.setToMeridian(ApplicationConstants.EMPTY);
						bean.setWeek(ApplicationConstants.EMPTY);
						bean.setWeekdays(new ArrayList<>());
						bean.setErrorMessage(ApplicationConstants.EMPTY);
					}else if(NumberUtils.isNumber(mins) &&getStringToInt(mins)%5 ==0 && (getStringToInt(mins) >0 && getStringToInt(mins) <=55)) {
						bean.setCronString(expression);
						bean.setMode("daily");
						if(getStringToInt(hrs)>=12) {
							bean.setOnceHours(get24Hrs(hrs));
							bean.setOnceMeridian(ApplicationConstants.PM);
						}else {
							bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
							bean.setOnceMeridian(ApplicationConstants.AM);
						}
						bean.setOnceMinutes(mins);
						bean.setDay(ApplicationConstants.EMPTY);
						bean.setDaysOfMonth(ApplicationConstants.EMPTY);
						bean.setFromHours(ApplicationConstants.EMPTY);
						bean.setFromMeridian(ApplicationConstants.EMPTY);
						bean.setInterval(ApplicationConstants.EMPTY);
						bean.setMonthDay(ApplicationConstants.EMPTY);
						bean.setRecoveryEnabled(Boolean.TRUE.toString());
						bean.setToHours(ApplicationConstants.EMPTY);
						bean.setToMeridian(ApplicationConstants.EMPTY);
						bean.setWeek(ApplicationConstants.EMPTY);
						bean.setWeekdays(new ArrayList<>());
						bean.setErrorMessage(ApplicationConstants.EMPTY);
					}else {
						 // minutes not multiple by 5   0 41 15 * * ?   	scenarios 
						setRecoveryBeanAsAdvanced(expression,bean);
					}
				}else {
					setRecoveryBeanAsAdvanced(expression,bean);
				}
			}else if ((secs.equals("0") && months.equals("*") && weekDay.equals("?")) || (secs.equals("0") &&  days.equals("?") && months.equals("*") && (weekDay.contains("#") || (weekDay.contains("L")) ))) {
				/***** daysOfMonth expression ***/
				//System.out.println("Expression : Days per Month");
				if(secs.equals("0") && months.equals("*") && weekDay.equals("?")) {
					//0 45 21 L * ?     or 0 0 1 1 * ?	  or   0 45 10 18 * ?
					if(NumberUtils.isNumber(mins) && NumberUtils.isNumber(hrs) && (NumberUtils.isNumber(days)|| NumberUtils.isNumber(days))) {
						if(getStringToInt(mins)%5 ==0 && (getStringToInt(mins) >=0 && getStringToInt(mins) <=55)) {
							bean.setCronString(expression);
							bean.setMode("dayOfMonth");
							if(getStringToInt(hrs)>=12) {
								bean.setOnceHours(get24Hrs(hrs));
								bean.setOnceMeridian(ApplicationConstants.PM);
							}else {
								bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
								bean.setOnceMeridian(ApplicationConstants.AM);
							}
							bean.setOnceMinutes(mins);
							bean.setDay(ApplicationConstants.EMPTY);
							bean.setDaysOfMonth("dayOfMonth");
							bean.setFromHours(ApplicationConstants.EMPTY);
							bean.setFromMeridian(ApplicationConstants.EMPTY);
							bean.setInterval(ApplicationConstants.EMPTY);
							bean.setMonthDay(days);
							bean.setRecoveryEnabled(Boolean.TRUE.toString());
							bean.setToHours(ApplicationConstants.EMPTY);
							bean.setToMeridian(ApplicationConstants.EMPTY);
							bean.setWeek(ApplicationConstants.EMPTY);
							bean.setWeekdays(new ArrayList<>());
							bean.setErrorMessage(ApplicationConstants.EMPTY);
						}else {
							setRecoveryBeanAsAdvanced(expression, bean);
						}
					}else {
						setRecoveryBeanAsAdvanced(expression, bean);
					}
					
					
				}else if(secs.equals("0") &&  days.equals("?") && months.equals("*") && (weekDay.contains("#") || weekDay.contains("L"))) {
					
					if(NumberUtils.isNumber(mins) && NumberUtils.isNumber(hrs)) {
						if(getStringToInt(mins)%5 ==0 && (getStringToInt(mins) >=0 && getStringToInt(mins) <=55)) {
							if(weekDay.contains("#")) {
								String[] monthOfWeekday = weekDay.split("#");
								if((getStringToInt(monthOfWeekday[0])>0 && getStringToInt(monthOfWeekday[0])<=7) && (getStringToInt(monthOfWeekday[1])>0 && getStringToInt(monthOfWeekday[1])<=4)) {
									bean.setCronString(expression);
									bean.setMode("dayOfMonth");
									if(getStringToInt(hrs)>=12) {
										bean.setOnceHours(get24Hrs(hrs));
										bean.setOnceMeridian(ApplicationConstants.PM);
									}else {
										bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
										bean.setOnceMeridian(ApplicationConstants.AM);
									}
									bean.setOnceMinutes(mins);
									bean.setDay(monthOfWeekday[1]);
									bean.setDaysOfMonth("dayOfMonth");
									bean.setFromHours(ApplicationConstants.EMPTY);
									bean.setFromMeridian(ApplicationConstants.EMPTY);
									bean.setInterval(ApplicationConstants.EMPTY);
									bean.setMonthDay(ApplicationConstants.EMPTY);
									bean.setRecoveryEnabled(Boolean.TRUE.toString());
									bean.setToHours(ApplicationConstants.EMPTY);
									bean.setToMeridian(ApplicationConstants.EMPTY);
									bean.setWeek(monthOfWeekday[0]);
									bean.setWeekdays(new ArrayList<>());
									bean.setErrorMessage(ApplicationConstants.EMPTY);
								}else {
									setRecoveryBeanAsAdvanced(expression, bean);
								}
							}else if(weekDay.contains("L")) {
								char[] array = weekDay.toCharArray();
								if(array.length==1 && String.valueOf(array[0]).equals("L")) {
									setRecoveryBeanAsAdvanced(expression, bean);
								}else {
									if((getStringToInt(String.valueOf(array[0]))>0 && getStringToInt(String.valueOf(array[0]))<=7) && String.valueOf(array[1]).equals("L")) {
										bean.setCronString(expression);
										bean.setMode("dayOfMonth");
										if(getStringToInt(hrs)>=12) {
											bean.setOnceHours(get24Hrs(hrs));
											bean.setOnceMeridian(ApplicationConstants.PM);
										}else {
											bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
											bean.setOnceMeridian(ApplicationConstants.AM);
										}
										bean.setOnceMinutes(mins);
										bean.setDay(String.valueOf(array[1]));
										bean.setDaysOfMonth("dayOfMonth");
										bean.setFromHours(ApplicationConstants.EMPTY);
										bean.setFromMeridian(ApplicationConstants.EMPTY);
										bean.setInterval(ApplicationConstants.EMPTY);
										bean.setMonthDay(ApplicationConstants.EMPTY);
										bean.setRecoveryEnabled(Boolean.TRUE.toString());
										bean.setToHours(ApplicationConstants.EMPTY);
										bean.setToMeridian(ApplicationConstants.EMPTY);
										bean.setWeek(String.valueOf(array[0]));
										bean.setWeekdays(new ArrayList<>());
										bean.setErrorMessage(ApplicationConstants.EMPTY);
									}else {
										setRecoveryBeanAsAdvanced(expression, bean);
									}
								}
							}
						}else {
							setRecoveryBeanAsAdvanced(expression, bean);
						}
					}else {
						setRecoveryBeanAsAdvanced(expression, bean);
					}
				}else {
					setRecoveryBeanAsAdvanced(expression,bean);
				}
				
			}else if (secs.equals("0") && days.equals("?") && months.equals("*")) {
				/***** daysOfWeek expression ***/
				//System.out.println("Expression : daysOfWeek");
				if(hrs.contains("-") && hrs.contains("/")) {
					// 0 0 1-17/3 * * ?   or       0 0 1-17/2 * * ?    or     0 0 1-17/1 * * ?    scenarios
					String hourly[]= hrs.split("/");
					String hours = hourly[0];
					String intervalHrs = hourly[1];
					
					if(getStringToInt(intervalHrs)>=1 && getStringToInt(intervalHrs)<=3) {
						String fromToHrs[]= hours.split("-");
						
						/*if(getStringToInt(fromToHrs[0])>getStringToInt(fromToHrs[1])) {
							System.out.println("Exception : You must select a from time that is before the to time");
							bean.setErrorMessage("Invalid Cron Expression");
							 setAdvancedRecoveryBeanAsEmpty(bean);
						}else*/ if(getStringToInt(fromToHrs[0])<=getStringToInt(fromToHrs[1])){
							if(!mins.contains("/") && !mins.contains("*")) {
								if(getStringToInt(mins)==0) {
									bean.setCronString(expression);
									 bean.setMode("daysOfWeek");
									 	if(getStringToInt(fromToHrs[0])>12) {
												bean.setFromHours(get24Hrs(fromToHrs[0]));
												bean.setFromMeridian(ApplicationConstants.PM);
											}else {
												bean.setFromHours(fromToHrs[0].equals("0")?get24Hrs(fromToHrs[0]):fromToHrs[0]);
												bean.setFromMeridian(ApplicationConstants.AM);
											}
										 if(getStringToInt(fromToHrs[1])>12) {
												bean.setToHours(get24Hrs(fromToHrs[1]));
												bean.setToMeridian(ApplicationConstants.PM);
											}else {
												bean.setToHours(fromToHrs[1].equals("0")?get24Hrs(fromToHrs[1]):fromToHrs[1]);
												bean.setToMeridian(ApplicationConstants.AM);
											}
										 bean.setInterval(String.valueOf(getStringToInt(intervalHrs)*60));
											bean.setDay(ApplicationConstants.EMPTY);
											bean.setDaysOfMonth(ApplicationConstants.EMPTY);
											bean.setMonthDay(ApplicationConstants.EMPTY);
											bean.setOnceHours(ApplicationConstants.EMPTY);
											bean.setOnceMeridian(ApplicationConstants.EMPTY);
											bean.setOnceMinutes(ApplicationConstants.EMPTY);
											bean.setRecoveryEnabled(Boolean.TRUE.toString());
											bean.setWeek(ApplicationConstants.EMPTY);
											bean.setWeekdays(getWeekDays(weekDay));
								}else {
									setRecoveryBeanAsAdvanced(expression,bean);
								}
							}else {
								setRecoveryBeanAsAdvanced(expression,bean);
							}
						}
					}else {
						setRecoveryBeanAsAdvanced(expression,bean);
					}
				}else if (hrs.contains("-")) {
					//  0 0/15 1-18 * * ?       and       0 0/30 0-12 * * ?   	scenarios
					 String fromToHrs[]= hrs.split("-");
					/* if(getStringToInt(fromToHrs[0])>getStringToInt(fromToHrs[1])) {
						 System.out.println("Exception : You must select a from time that is before the to time");
						 bean.setErrorMessage("Invalid Cron Expression");
						 setAdvancedRecoveryBeanAsEmpty(bean);
						 
					}else */if(getStringToInt(fromToHrs[0])<=getStringToInt(fromToHrs[1])){
						if(mins.contains("/")) {
							bean.setCronString(expression);
							 bean.setMode("daysOfWeek");
							 if(getStringToInt(fromToHrs[0])>12) {
										bean.setFromHours(get24Hrs(fromToHrs[0]));
										bean.setFromMeridian(ApplicationConstants.PM);
									}else {
										bean.setFromHours(fromToHrs[0].equals("0")?get24Hrs(fromToHrs[0]):fromToHrs[0]);
										bean.setFromMeridian(ApplicationConstants.AM);
									}
								 if(getStringToInt(fromToHrs[1])>12) {
										bean.setToHours(get24Hrs(fromToHrs[1]));
										bean.setToMeridian(ApplicationConstants.PM);
									}else {
										bean.setToHours(fromToHrs[1].equals("0")?get24Hrs(fromToHrs[1]):fromToHrs[1]);
										bean.setToMeridian(ApplicationConstants.AM);
									}
								 	String minutes[] = mins.split("/"); 
									bean.setInterval(minutes[1]);	
								 	bean.setDay(ApplicationConstants.EMPTY);
									bean.setDaysOfMonth(ApplicationConstants.EMPTY);
									bean.setMonthDay(ApplicationConstants.EMPTY);
									bean.setOnceHours(ApplicationConstants.EMPTY);
									bean.setOnceMeridian(ApplicationConstants.EMPTY);
									bean.setOnceMinutes(ApplicationConstants.EMPTY);
									bean.setRecoveryEnabled(Boolean.TRUE.toString());
									bean.setWeek(ApplicationConstants.EMPTY);
									bean.setWeekdays(getWeekDays(weekDay));
						}else {
							setRecoveryBeanAsAdvanced(expression,bean);
						}
					}
				}else if(NumberUtils.isNumber(hrs)){
					// 0 0 15 * * ?       and       0 45 15 * * ?   	scenarios  
					if(getStringToInt(mins)==0) {
						bean.setCronString(expression);
						bean.setMode("daysOfWeek");
						if(getStringToInt(hrs)>12) {
							bean.setOnceHours(get24Hrs(hrs));
							bean.setOnceMeridian(ApplicationConstants.PM);
						}else {
							bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
							bean.setOnceMeridian(ApplicationConstants.AM);
						}
						bean.setOnceMinutes(mins);
						bean.setDay(ApplicationConstants.EMPTY);
						bean.setDaysOfMonth(ApplicationConstants.EMPTY);
						bean.setFromHours(ApplicationConstants.EMPTY);
						bean.setFromMeridian(ApplicationConstants.EMPTY);
						bean.setInterval(ApplicationConstants.EMPTY);
						bean.setMonthDay(ApplicationConstants.EMPTY);
						bean.setRecoveryEnabled(Boolean.TRUE.toString());
						bean.setToHours(ApplicationConstants.EMPTY);
						bean.setToMeridian(ApplicationConstants.EMPTY);
						bean.setWeek(ApplicationConstants.EMPTY);
						bean.setWeekdays(getWeekDays(weekDay));
					}else if(getStringToInt(mins)%5 ==0 && (getStringToInt(mins) >0 && getStringToInt(mins) <=55)) {
						bean.setCronString(expression);
						bean.setMode("daysOfWeek");
						if(getStringToInt(hrs)>=12) {
							bean.setOnceHours(get24Hrs(hrs));
							bean.setOnceMeridian(ApplicationConstants.PM);
						}else {
							bean.setOnceHours(hrs.equals("0")?get24Hrs(hrs):hrs);
							bean.setOnceMeridian(ApplicationConstants.AM);
						}
						bean.setOnceMinutes(mins);
						bean.setDay(ApplicationConstants.EMPTY);
						bean.setDaysOfMonth(ApplicationConstants.EMPTY);
						bean.setFromHours(ApplicationConstants.EMPTY);
						bean.setFromMeridian(ApplicationConstants.EMPTY);
						bean.setInterval(ApplicationConstants.EMPTY);
						bean.setMonthDay(ApplicationConstants.EMPTY);
						bean.setRecoveryEnabled(Boolean.TRUE.toString());
						bean.setToHours(ApplicationConstants.EMPTY);
						bean.setToMeridian(ApplicationConstants.EMPTY);
						bean.setWeek(ApplicationConstants.EMPTY);
						bean.setWeekdays(getWeekDays(weekDay));
					}else {
						 // minutes not multiple by 5   0 41 15 * * ?   	scenarios 
						setRecoveryBeanAsAdvanced(expression,bean);
					}
				}else {
					setRecoveryBeanAsAdvanced(expression,bean);
				}
			}else {
				setRecoveryBeanAsAdvanced(expression,bean);
			}
		}else {
			setRecoveryBeanAsAdvanced(expression,bean);
		}
		
		return bean;
	}
	
	
	private static String get24Hrs(String key){
		Map<String, String> map = new HashMap<>();
		map.put("12", "12");
		map.put("13", "1");
		map.put("14", "2");
		map.put("15", "3");
		map.put("16", "4");
		map.put("17", "5");
		map.put("18", "6");
		map.put("19", "7");
		map.put("20", "8");
		map.put("21", "9");
		map.put("22", "10");
		map.put("23", "11");
		map.put("0", "12");
		return map.get(key);
		
	}
	
	private static int getStringToInt(String value) {
		return Integer.parseInt(value);
	}
	
	private static RecoveryFormBean setRecoveryBeanAsAdvanced(String expression,RecoveryFormBean bean) {
		bean.setCronString(expression);
		bean.setMode("advanced");
		bean.setDay(ApplicationConstants.EMPTY);
		bean.setDaysOfMonth(ApplicationConstants.EMPTY);
		bean.setFromHours(ApplicationConstants.EMPTY);
		bean.setFromMeridian(ApplicationConstants.EMPTY);
		bean.setInterval(ApplicationConstants.EMPTY);
		bean.setMonthDay(ApplicationConstants.EMPTY);
		bean.setOnceHours(ApplicationConstants.EMPTY);
		bean.setOnceMeridian(ApplicationConstants.EMPTY);
		bean.setOnceMinutes(ApplicationConstants.EMPTY);
		bean.setRecoveryEnabled(Boolean.TRUE.toString());
		bean.setToHours(ApplicationConstants.EMPTY);
		bean.setToMeridian(ApplicationConstants.EMPTY);
		bean.setWeek(ApplicationConstants.EMPTY);
		bean.setWeekdays(new ArrayList<>());
		bean.setErrorMessage(ApplicationConstants.EMPTY);
		return bean;
	}
	
	
	private static List<String> getWeekDays(String value){
		List<String> weekDays = new ArrayList<String>(Arrays.asList(value.split(",")));
		List<String> weekDaysList = new ArrayList<>();
		for (String day : weekDays) {
			weekDaysList.add("'"+day+"'");
		}
		return weekDaysList;
	}
	 

}
