package com.thed.zephyr.je.service;

import com.thed.zephyr.je.vo.ImportJob;
import org.apache.poi.ss.usermodel.Sheet;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ExcelFileValidationService {
	
	public boolean isValidRequiredMapping(ImportJob importJob);

	public boolean isValidRequiredXMLMapping(ImportJob importJob);

	public boolean isValidData(ImportJob importJob, File file, Map<String, String> fileMapping, List<Sheet> sheets) throws Exception;

	public boolean isValidMapping(String cellMapping);
	
}
