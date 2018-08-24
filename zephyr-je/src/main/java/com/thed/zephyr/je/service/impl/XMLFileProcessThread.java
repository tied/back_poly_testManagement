package com.thed.zephyr.je.service.impl;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.thed.zephyr.je.operation.JobProgress;
import com.thed.zephyr.je.service.IssueImporterService;
import com.thed.zephyr.je.service.JobProgressService;
import com.thed.zephyr.je.vo.*;
import com.thed.zephyr.je.vo.ImportFile.ColumnMetadata;
import com.thed.zephyr.je.vo.TestCase.Response;
import com.thed.zephyr.util.ExcelFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static com.thed.zephyr.util.ApplicationConstants.IMPORT_JOB_NORMALIZATION_FAILED;

public class XMLFileProcessThread implements Callable<ImportJobStatus> {

	private ImportJob importJob;
	private File file;
	private ApplicationUser user;
	private IssueImporterService issueImporterService;
	private JobProgressService jobProgressService;
	private JiraAuthenticationContext authContext;
	private static final Logger log = LoggerFactory.getLogger(XMLFileProcessThread.class);
	private String jobProgressToken;
	public XMLFileProcessThread(JiraAuthenticationContext authContext,
								IssueImporterService issueImporterService,
								final JobProgressService jobProgressService,
								final String jobProgressToken) {
		this.authContext = authContext;
		this.issueImporterService = issueImporterService;
		this.jobProgressService = jobProgressService;
		this.jobProgressToken = jobProgressToken;
	}

	public ImportJob getImportJob() {
		return importJob;
	}

	public void setImportJob(ImportJob importJob) {
		this.importJob = importJob;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public ApplicationUser getUser() {
		return user;
	}

	public void setUser(ApplicationUser user) {
		this.user = user;
	}

	@Override
	public ImportJobStatus call() throws Exception {
		ImportJobStatus jobStatus = new ImportJobStatus();
		int issuesCount = importXMLFile(file, importJob, user);
		if(issuesCount > 0) {
			jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.successful"));
			//jobStatus.setFileName(file.getName());
			jobStatus.setIssuesCount(String.valueOf(issuesCount));
		} else {
			jobStatus.setStatus(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.init.failed"));
			//jobStatus.setFileName(file.getName());
			jobStatus.setErrorMsg(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.failed.msg"));
		}
		String fileName = file.getName();
		jobStatus.setFileName(fileName.substring(fileName.indexOf("_") + 1));
		return jobStatus;
	}

	private int importXMLFile(File file, ImportJob importJob, ApplicationUser user){
		ImportDetails importDetails = importJob.getImportDetails();
		Set<ImportFieldMapping> mappingSet = importDetails.getFieldMappingSet();
		Map<String, ColumnMetadata> columnsMetadataMap = issueImporterService.populateColumnMetadata(importJob.getFieldConfigMap(), mappingSet);
		int result = parseAndCreateIssue(importJob, file, columnsMetadataMap, user);
		try {
			if(null != file && file.exists())
				file.delete();
		} catch (Exception e) {
			log.warn("Exception while deleting the file", e);
		}
		return result;
	}

    /**
     * Parse the xml and create issue for each entry in the xml.
     * @param importJob
     * @param inputFile
     * @param columnMetadataMap
     * @param user
     * @return
     */
	private int parseAndCreateIssue(ImportJob importJob, File inputFile, Map<String ,ColumnMetadata> columnMetadataMap, ApplicationUser user){
		Long createOn = new Date().getTime();
		int issuesCount = 0;
		//Get the column mapping separate for custom fields and teststeps
		//Depending on the values, create separate objects and finally populate the issue of type Test
		Map<String, String> xmlValuesMap = null;
		List<Map<String, Object>> testSteps = new ArrayList<>();
		Stack<String> stepStack = new Stack<>();
		boolean isCustomField = false;
		Set<String> elements = new HashSet<>();
		XMLEventReader xmlEventReader = null;
		FileReader fileReader = null;
		try{
			Map<String, Object> stepData = null;
			XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			fileReader = new FileReader(inputFile);
			//xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(inputFile));
			xmlEventReader = xmlInputFactory.createXMLEventReader(fileReader);
			StringBuilder element = new StringBuilder("");
			int customfieldElementCount = -1;
			String customFieldKey = null;
			//			String customFieldValue = null;
			while(xmlEventReader.hasNext()){
				XMLEvent xmlEvent = xmlEventReader.nextEvent();
				if (xmlEvent.isStartElement()){
					StartElement startElement = xmlEvent.asStartElement();
					String elementName = startElement.getName().toString();
					/*if(StringUtils.endsWith(element, "custom_field") && startElement.getName().equals("name")){
                    	//elementName = startElement.
                    }*/
					//Incase the element is testcase, its a start of a new testcase
					if(elementName.equals("testcase")){
						//createTestcase(xmlValuesMap, testSteps);
						xmlValuesMap = new HashMap<String, String>();
					} else if(elementName.equals("step")){
						//Incase the element is step, its start of a new teststep
						if(stepStack.size() == 0){
							stepStack.push(elementName);
							stepData = new HashMap<>();
						}
					} else if(elementName.equals("custom_field")){
						isCustomField = true;
						customfieldElementCount = 1;
					} else if(isCustomField){
						elementName="";
					}
					if(element.length() > 0 //&& elementName.length() > 0
							){
						element.append("/");
					}
					if(elementName.length() > 0){
						element//.append("/")
						.append(elementName);
					}
					//elements.add(element.toString());
					@SuppressWarnings("rawtypes")
					Iterator attributeIterator = startElement.getAttributes();
					if(xmlValuesMap != null){
						while(attributeIterator.hasNext()){
							Attribute attribute = (Attribute)attributeIterator.next();
							//elements.add(element + "." + attribute.getName());
							//element.append("." + attribute.getName());
							xmlValuesMap.put(element + "." + attribute.getName(), attribute.getValue());
							elements.add(element.toString() + "." + attribute.getName());
						}
					}
				}
				/*if(xmlEvent.isProcessingInstruction()){
                	ProcessingInstruction pi = (ProcessingInstruction) xmlEvent;
                	if(StringUtils.startsWith(pi.getData(), "customfield")){
                		elements.add(element + "/" + pi.getData());
                		element.delete(0, element.length());
                	}
                }*/
				if(xmlEvent.isCharacters()){
					javax.xml.stream.events.Characters pi = (javax.xml.stream.events.Characters) xmlEvent;
					if(!StringUtils.isEmpty(pi.getData().trim()) && xmlValuesMap != null){
						if(!stepStack.isEmpty()){
							//This is a step element
							stepData.put(element.toString(), pi.getData());
							elements.add(element.toString());
						}else if(isCustomField){
							//This is a custom field element
							if(customfieldElementCount == 1){
								customFieldKey = element.toString() + pi.getData();
							}else if (customfieldElementCount == 2){
								xmlValuesMap.put(customFieldKey, pi.getData());
								elements.add(customFieldKey);
								isCustomField = false;	
							}
							++customfieldElementCount;
						}else {
							xmlValuesMap.put(element.toString(), pi.getData());
							elements.add(element.toString());
							//This is a issue field element
						}
					}
					/*if(StringUtils.startsWith(pi.getData(), "customfield") || 
                			(element.lastIndexOf("/") > 0 && StringUtils.endsWith(element.substring(0, element.lastIndexOf("/")), "custom_field"))){
                		//elements.add(element.substring(0, element.lastIndexOf("/")) + "/" + pi.getData());
                		//element.delete(0, element.length());
                		element = new StringBuilder(element.substring(0, element.lastIndexOf("/")) + "/" + pi.getData());
                		isCustomField = true;
                	}*/
					//xmlValueMap.put(element.toString(), pi.getData());
				}
				if(xmlEvent.isEndElement()){
					EndElement endElement = xmlEvent.asEndElement();
					String elementName = endElement.getName().toString();
					//elements.add(element.toString());
					if(elementName.equals("testcase")){
						boolean result = createIssue(xmlValuesMap, testSteps, importJob, columnMetadataMap, createOn, user);
						if(result){
							issuesCount++;
							JobProgress jobProgress = jobProgressService.addCompletedSteps(jobProgressToken, issuesCount);
							if(jobProgress == null || jobProgress.getCanceledJob()){
								log.warn("Importer process stopped as the user has requested to stop:"+ inputFile.getName());
								throw new Exception(authContext.getI18nHelper().getText("zephyr-je.pdb.importer.job.process.stopped"));
							}
							testSteps.clear();
						}
						xmlValuesMap = null;
					}else if(elementName.equals("step")){
						if(stepStack.size() > 0){
							stepStack.pop();
							testSteps.add(stepData);
							//stepData = null;
						}
					}/*else if(elementName.equals("custom_field")){
                    	isCustomField = false;
                    }*/
					if(element.indexOf("/") > 0){
						element.delete(element.lastIndexOf("/"), element.length());
					}else {
						element.delete(0, element.length());
					}
				}
				jobProgressService.addCompletedSteps(jobProgressToken,1);
			}
		}catch(Exception e){
			log.warn("Exception while getting elements from XML file:" + inputFile.getName() , e);
		}finally{
            try {
                if(null != xmlEventReader) {
                    log.debug("attempt to close xml event reader.");
                    xmlEventReader.close();
                }
                if(null != fileReader) {
                    log.debug("attempt to close file reader");
                    fileReader.close();
                }
            } catch (XMLStreamException | IOException  e) {
                log.error("Exception while closing XmlEventReader for the file:"+ inputFile.getName(), e);
                ExcelFileUtil.addJobHistoryAndUpdateStatus(importJob, IMPORT_JOB_NORMALIZATION_FAILED, e.getMessage());
            }
		}
		return issuesCount;
	}

    /**
     * Create Issue by using the parse data
     * @param xmlValuesMap
     * @param testSteps
     * @param importJob
     * @param columnsMetadataMap
     * @param createdOn
     * @param user
     * @return
     */
	private boolean createIssue(Map<String, String> xmlValuesMap, List<Map<String, Object>> testSteps,
			ImportJob importJob, Map<String, ColumnMetadata> columnsMetadataMap, Long createdOn, ApplicationUser user) {
		boolean issueCreated = false;
		String issueKey = null;
		testSteps = mapXMLDataToFieldConfig(xmlValuesMap, columnsMetadataMap, testSteps);
		TestCase testcase = issueImporterService.populateTestCase(importJob, user, columnsMetadataMap, -1);
		try {
			if(StringUtils.isEmpty(testcase.getIssueKey())){
				Response response = issueImporterService.createTestCase(testcase, user);
				issueKey = response.getIssueKey();
				issueCreated = true;
			}else{
				issueKey = testcase.getIssueKey();
			}
		}catch(Exception e){
			log.error("Exception while creating issue:", e);
		}
		if(issueKey != null){
			issueImporterService.createTestSteps(importJob, issueKey, testSteps);
		}
		return issueCreated;
	}

	/**
	 * Populate/Map the rawdata from XML elements into the ColumnMetadata 
	 * so that the application populates the data in the correct attributes.
	 * @param xmlValuesMap
	 * @param columnsMetadataMap
	 */
	private List<Map<String, Object>> mapXMLDataToFieldConfig(Map<String, String> xmlValuesMap, Map<String, ColumnMetadata> columnsMetadataMap, List<Map<String, Object>> testSteps){
		Map<String, ColumnMetadata> stepsMetadata = new HashMap<>();
		for(String key : columnsMetadataMap.keySet()){
			ColumnMetadata metadata = columnsMetadataMap.get(key);
			//String jiraField = metadata.getFieldConfig().getId();
			String mappedField = metadata.getMappedField();
			String value = xmlValuesMap.get(mappedField);
			metadata.setValue(value);
			if(metadata.getFieldConfig().getId().startsWith("step") && StringUtils.isNotEmpty(metadata.getMappedField())){
				stepsMetadata.put(metadata.getMappedField(), metadata);
			}
		}
		List<Map<String, Object>> updatedTestSteps = new ArrayList<>();
		//For all testSteps, we update the key to the mapped column
		Map<String, Object> updatedTestStep = null;
		for(Map<String, Object> testStep : testSteps){
			updatedTestStep = new HashMap<>();
			for(String str : testStep.keySet()){
				if(stepsMetadata.containsKey(str)){
					updatedTestStep.put(stepsMetadata.get(str).getFieldConfig().getId(), testStep.get(str));
				}
			}
			if(updatedTestStep.size() > 0){
				updatedTestSteps.add(updatedTestStep);
			}
		}
		return updatedTestSteps;
	}


}
