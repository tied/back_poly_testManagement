package com.thed.zephyr.je.index;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.indexers.impl.FieldIndexerUtil;
import net.java.ao.Common;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumberTools;

import com.atlassian.jira.issue.index.DocumentConstants;
import com.thed.zephyr.je.index.bridge.BridgeFactory;
import com.thed.zephyr.je.index.bridge.ClassBridge;
import com.thed.zephyr.je.index.bridge.FieldBridge;
import com.thed.zephyr.je.model.Cycle;
import com.thed.zephyr.je.model.Folder;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.util.ApplicationConstants;


public class ScheduleDocument {

    private static final Logger log = Logger.getLogger(ScheduleDocument.class);
	private static final String SORT_CYCLE_NAME_INDEX_LABEL = "sort_cycle";
	private static final String SORT_FOLDER_NAME_INDEX_LABEL = "sort_folder";

    public static <T> Document getDocument(final Schedule schedule)
    {
        final Document doc = new Document();
        addSchedule(doc, schedule);
        return doc;
    }
    
	private static void addSchedule(Document doc, Schedule schedule) {
		boolean isClassBridgeAnnotation = schedule.getEntityType().isAnnotationPresent(ClassBridge.class);
		//If Class Level Annotation, index External Entity
		if(isClassBridgeAnnotation) {
			ClassBridge classBridgeAnnotation =schedule.getEntityType().getAnnotation(ClassBridge.class);
			indexClassBridge(classBridgeAnnotation,schedule,doc);
			indexCustomFieldClassBridge(classBridgeAnnotation,schedule,doc);
		}
		
		for (Method method : schedule.getEntityType().getMethods()) {
			Searchable indexAnnotation = Common.getAnnotationDelegate(schedule.getEntityManager().getFieldNameConverter(), method).getAnnotation(Searchable.class);
			try {
				String attribute = schedule.getEntityManager().getFieldNameConverter().getName(method);
				if(StringUtils.equalsIgnoreCase(attribute,"ID")) {
					Object idValue = method.invoke(schedule);
					indexLongAsKeyword(doc, "schedule_id", Long.valueOf(idValue.toString()));
				}
				if (indexAnnotation != null) {
					if (Common.isAccessor(method)) {

						log.debug("Indexing Entity Type : " + schedule.getEntityType() +" and Attribute:"+attribute);
	
						Object value;
						value = method.invoke(schedule);
						
						//Check for FieldBridge Annotation
						com.thed.zephyr.je.index.bridge.FieldBridge fieldBridgeAnnotation = 
							Common.getAnnotationDelegate(schedule.getEntityManager().getFieldNameConverter(), method).getAnnotation(com.thed.zephyr.je.index.bridge.FieldBridge.class);
						//If Field Bridge Present, Index field in a separate way
						if(fieldBridgeAnnotation != null) {
							indexFieldBridge(fieldBridgeAnnotation,attribute,value,doc);
						} else {
							if(method.getReturnType().isAssignableFrom(Cycle.class)) {
								Cycle value1=(Cycle)value;
								String newvalue = value1 != null ? String.valueOf(value1.getID()) : ApplicationConstants.NULL_VALUE;
								indexKeyword(doc, attribute, newvalue);
								String newCycleNameValue = value1 != null ? value1.getName() : ApplicationConstants.AD_HOC_CYCLE_NAME;
								//doc.add(new Field("cycle", newCycleNameValue, Field.Store.YES, Field.Index.ANALYZED));
								doc.add(new Field("cycle", newCycleNameValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
								doc.add(new Field(SORT_CYCLE_NAME_INDEX_LABEL, newCycleNameValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
								indexKeyword(doc, "SPRINT_ID", value1 != null ? String.valueOf(value1.getSprintId()) : ApplicationConstants.NULL_VALUE);
							} else if(method.getReturnType().isAssignableFrom(Folder.class)) {
								Folder value1=(Folder)value;
								String newvalue = value1 != null ? String.valueOf(value1.getID()) : ApplicationConstants.NULL_VALUE;
								indexKeyword(doc, attribute, newvalue);
								String newFolderNameValue = value1 != null ? value1.getName() : ApplicationConstants.NULL_VALUE;
								//doc.add(new Field("folder", newFolderNameValue, Field.Store.YES, Field.Index.ANALYZED));
								doc.add(new Field("folder", newFolderNameValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
								doc.add(new Field(SORT_FOLDER_NAME_INDEX_LABEL, newFolderNameValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
							} else {
								if (StringUtils.equalsIgnoreCase(attribute,"ESTIMATED_TIME")) {
									indexLongAsPaddedKeywordWithDefault(doc, attribute, schedule.getEstimatedTime(), ApplicationConstants.NULL_VALUE);
								} else if (StringUtils.equalsIgnoreCase(attribute,"LOGGED_TIME")) {
									indexLongAsPaddedKeywordWithDefault(doc, attribute, schedule.getLoggedTime(), ApplicationConstants.NULL_VALUE);
								} else {
									String newValue = value != null ? value.toString() : ApplicationConstants.NULL_VALUE;
									indexKeyword(doc, attribute, newValue);
								}
							}
						}
					}
				}				
			} catch (IllegalArgumentException e) {
				log.error("Error Indexing : IllegalArgumentException :",e );
			} catch (IllegalAccessException e) {
				log.error("Error Indexing : IllegalAccessException :",e );
			} catch (InvocationTargetException e) {
				log.error("Error Indexing : InvocationTargetException :",e );
			} catch (Exception e) {
				log.error("Error Indexing : Exception :",e );
			}
		}
	}


    /**
     * Extracts the ClassBridge Type and Indexes the Data through that Bridge
     * @param classBridgeAnnotation
     * @param value
     * @param document
     */
    private static void indexClassBridge(ClassBridge classBridgeAnnotation,
			Object value, Document document) {
    	if(classBridgeAnnotation != null && classBridgeAnnotation.impl()[0] != null) {
    		try {
				BridgeFactory.extractType(classBridgeAnnotation.impl()[0]).set(null,value, document);
    		} catch (Exception e) {
				log.warn("Failed to Index data from Bridge:",e);
			}
    	}
	}

    
	/**
	 * Extracts the FieldBridge Type and Indexes the Data through that Bridge
	 * @param fieldBridgeAnnotation
	 * @param value
	 * @param fieldName 
	 * @param document
	 */
    private static void indexFieldBridge(FieldBridge fieldBridgeAnnotation,
			String fieldName, Object value, Document document) {
    	if(fieldBridgeAnnotation != null && fieldBridgeAnnotation.impl() != null) {
    		try {
				BridgeFactory.extractType(fieldBridgeAnnotation.impl()).set(fieldName,value, document);
    		} catch (Exception e) {
				log.warn("Failed to Index data from Bridge:",e);
			}
    	}
	}

    /**
     *
     * @param classBridgeAnnotation
     * @param value
     * @param document
     */
    private static void indexCustomFieldClassBridge(ClassBridge classBridgeAnnotation,
                                         Object value, Document document) {
        if(classBridgeAnnotation != null && classBridgeAnnotation.impl().length > 1 && classBridgeAnnotation.impl()[1] != null) {
            try {
                BridgeFactory.extractType(classBridgeAnnotation.impl()[1]).set(null,value, document);
            } catch (Exception e) {
                log.warn("Failed to Index custom field data from Bridge:",e);
            }
        }
    }
    
	/**
     * Index a single keyword field
     * @param doc the document to add the field to.
     * @param indexField the document field name to user.
     * @param fieldValue the value to index. This value will NOT be folded before adding it to the document.
     */
    private static void indexKeyword(final Document doc, final String indexField, final String fieldValue)
    {
        if (StringUtils.isNotBlank(fieldValue))
        {
        	log.debug("Indexing Field :" + indexField + " With Value :" + fieldValue);
        	if(StringUtils.equalsIgnoreCase(indexField, "PROJECT_ID")) {
                doc.add(new Field(DocumentConstants.PROJECT_ID, fieldValue, Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
                doc.add(new Field(DocumentConstants.ISSUE_SECURITY_LEVEL, "-1", Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
        	}
            doc.add(new Field(indexField, fieldValue, Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

	private static void indexLongAsKeyword(final Document doc, final String indexField, final Long fieldValue) {
        if (fieldValue != null)
        {
            doc.add(new Field(indexField, fieldValue.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            doc.add(new Field(DocumentConstants.ISSUE_KEY_NUM_PART_RANGE, NumberTools.longToString(fieldValue), Field.Store.NO,
                    Field.Index.NOT_ANALYZED_NO_NORMS));

        }
    }

	private static void indexLongAsPaddedKeywordWithDefault(Document doc, String indexField, Long aLong, String defaultValue) {
		String value = aLong != null ? NumberTools.longToString(aLong.longValue()):null;
		indexKeywordWithDefault(doc, indexField, value, defaultValue);
	}

	private static void indexKeywordWithDefault(Document doc, String indexField, String fieldValue, String defaultValue) {
		doc.add(getField(indexField, fieldValue, defaultValue, true));
	}

	private static Field getField(String indexField, String fieldValue, String defaultValue, boolean searchable) {
		String value = StringUtils.isNotBlank(fieldValue)?fieldValue:defaultValue;
		Field.Index index = searchable? Field.Index.NOT_ANALYZED_NO_NORMS: Field.Index.NO;
		return new Field(indexField, value, Field.Store.YES, index);
	}
}
