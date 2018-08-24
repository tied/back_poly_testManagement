package com.thed.zephyr.je.service.impl;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.thed.zephyr.je.attachment.AttachmentUtils;
import com.thed.zephyr.je.service.FileImportService;
import com.thed.zephyr.je.vo.ImportJob;

/**
 * Generic class to get the files from multipart form request.
 * @author gayatri
 *
 */
public class FileImportServiceImpl implements FileImportService {
	protected final Logger log = Logger.getLogger(FileImportServiceImpl.class);

	public FileImportServiceImpl() {    	
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object processMultiPartRequest(HttpServletRequest request,
			List<File> filesList) throws Exception{
		log.debug("temp directory"+System.getProperty("java.io.tmpdir"));
		final File tmpDir = AttachmentUtils.getTemporaryAttachmentDirectory();
		Object formDataObject = null;
		String tempDir = System.getProperty("java.io.tmpdir");
		if (ServletFileUpload.isMultipartContent(request)) {
			FileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload fileUpload = new ServletFileUpload(factory);
			List<FileItem> items = fileUpload.parseRequest(request);
			if (items != null) {
				Iterator<FileItem> iter = items.iterator();
				while (iter.hasNext()) {
					final FileItem item = iter.next();
					// final String itemName = item.getName();
					final String fieldName = item.getFieldName();
					final String fieldValue = item.getString("UTF-8");
					if (item.isFormField()) {
						if (fieldName.equalsIgnoreCase("importjob")) {
							Gson gson = new Gson();
							formDataObject = gson.fromJson(fieldValue, ImportJob.class);
						} else if(fieldName.equalsIgnoreCase("fileType")) {
							formDataObject = fieldValue;
						}
					} else {
						log.debug("file temp path--->" + tempDir + File.separator + item.getName());
						String fileName = item.getName();
						if(fileName.indexOf(File.separator) > 0) {
							fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
						}
						fileName = new String(System.currentTimeMillis() + "_" + fileName);
						File file = new File(tmpDir, fileName);
						if(!file.exists()){
							item.write(file);
						}
						file.setWritable(true,false);
						filesList.add(file);
					}
				}
			}
		}
		return formDataObject;

	}

}
