package com.thed.zephyr.je.index.cluster;

import java.io.IOException;
import java.util.Date;

import com.thed.zephyr.je.vo.RecoveryFormBean;

public interface CronSyncupSchedulerService {
	
	RecoveryFormBean schduleWithExpression(String expression,String userId) throws IOException;
	
	String schduleWithDateTimeInterval(Date date,long intervalInMillis);
	
	RecoveryFormBean unSchduleCronJob(String userId) throws IOException;
	
	public RecoveryFormBean getrecoveryForm() throws IOException;

}
