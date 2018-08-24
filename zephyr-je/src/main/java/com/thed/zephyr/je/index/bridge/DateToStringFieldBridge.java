package com.thed.zephyr.je.index.bridge;


import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.atlassian.jira.util.LuceneUtils;


public class DateToStringFieldBridge implements JEFieldBridge {

	@Override
	public void set(String fieldName,Object value, Document document) {
		Date date = (Date)value;
		if(date == null) {
			return;
		}
		String dateCreated = "DATE_CREATED";
		if(StringUtils.isNotBlank(fieldName)) {
		} else {
			fieldName = dateCreated;
		}
		//Date truncatedDate = DateUtils.truncate(date, Calendar.DATE); // commented to fix ZFJ-1253
		document.add(new Field(fieldName, LuceneUtils.dateToString(date), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
	}

}
