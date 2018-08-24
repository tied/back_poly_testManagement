package com.thed.zephyr.je.service;

import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.je.vo.ZQLScheduleBean;

import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.util.Collection;
import java.util.List;


public interface ExportService {
	StreamingOutput generate(List<ZQLScheduleBean> schedules, String exportType);

	StreamingOutput generateCSV(List<ZQLScheduleBean> schedules);

	StreamingOutput exportCycleOrFolder(Integer cycleId, I18nHelper i18nHelper,
			Long projectId, Long versionId, Long startDate, Long endDate, String cycleName,
			String build, String env, Long folderId, String sortQuery);

	File createDefectRequirementReport(String exportType,Collection<Long> defectIds, Long versionId)  throws Exception;

	File createRequirementDefectReport(String exportType,Collection<Long> requirementIds, Long versionId) throws Exception;

}
