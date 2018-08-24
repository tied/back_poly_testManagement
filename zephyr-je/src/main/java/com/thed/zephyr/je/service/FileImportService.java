package com.thed.zephyr.je.service;

import java.io.File;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.thed.zephyr.je.vo.ImportJob;

public interface FileImportService {
	public Object processMultiPartRequest(HttpServletRequest request, List<File> filesList) throws Exception;
	
}
