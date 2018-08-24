package com.thed.zephyr.je.index.bridge;


import com.atlassian.jira.datetime.LocalDate;
import com.atlassian.jira.datetime.LocalDateFactory;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
import com.atlassian.jira.issue.customfields.statistics.SelectStatisticsMapper;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.util.LuceneUtils;
import com.thed.zephyr.je.model.CustomField;
import com.thed.zephyr.je.model.Schedule;
import com.thed.zephyr.je.rest.CustomFieldValueResource;
import com.thed.zephyr.je.rest.delegate.CustomFieldValueResourceDelegate;
import com.thed.zephyr.je.service.ZephyrCustomFieldManager;
import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.util.NumericUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CustomFieldEntityFieldBridge implements JEFieldBridge {
    protected final Logger log = Logger.getLogger(CustomFieldEntityFieldBridge.class);
    private final CustomFieldValueResourceDelegate customFieldValueResourceDelegate;
    private final ZephyrCustomFieldManager zephyrCustomFieldManager;
    private final DoubleConverter doubleConverter;

    public CustomFieldEntityFieldBridge(CustomFieldValueResourceDelegate customFieldValueResourceDelegate, ZephyrCustomFieldManager zephyrCustomFieldManager, DoubleConverter doubleConverter) {
        this.customFieldValueResourceDelegate = customFieldValueResourceDelegate;
        this.zephyrCustomFieldManager=zephyrCustomFieldManager;
        this.doubleConverter=doubleConverter;
    }

    @Override
    public void set(String fieldName, Object value, Document document) {
        indexCustomFieldData(value, document);
    }

    private void indexCustomFieldData(Object value, Document document) {
        Schedule schedule = (Schedule) value;

        if(Objects.isNull(schedule)) {
            return;
        }

        List<CustomField> customFields = zephyrCustomFieldManager.getAllCustomFieldsByEntityTypeForProject(schedule.getProjectId(),ApplicationConstants.ENTITY_TYPE.EXECUTION.name());
        Map<String, CustomFieldValueResource.CustomFieldValueResponse> customFieldValues =
                customFieldValueResourceDelegate.getCustomFieldValuesByEntityId(schedule.getID(), ApplicationConstants.ENTITY_TYPE.EXECUTION.name(), null);

        Set<CustomField> cfBridge = new HashSet<>();
        customFields.stream().forEach(customField -> {
            if(MapUtils.isNotEmpty(customFieldValues)) {
                customFieldValues.forEach(
                    (customFieldValueId,customFieldValue) -> {
                        if(customField.getID() == customFieldValue.getCustomFieldId().intValue()) {
                            indexCustomFieldData(document, customFieldValue);
                            cfBridge.add(customField);
                        }
                    });
            }
            if(cfBridge == null || cfBridge.size() == 0) {
                indexDefaultData(document, customField);
            } else {
                cfBridge.remove(customField);
            }
        });
    }

    private void indexDefaultData(Document document, CustomField customField) {
        if(ApplicationConstants.DATE_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType())) ||
                ApplicationConstants.DATE_TIME_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()))) {
            document.add(new Field(String.valueOf(customField.getID()), LuceneUtils.dateToString(null), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            document.add(new Field(DocumentConstants.LUCENE_SORTFIELD_PREFIX  + String.valueOf(customField.getID()), LuceneUtils.dateToString(null), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else if(ApplicationConstants.NUMBER_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()))) {
            final String encoded = NumericUtils.doubleToPrefixCoded(Double.MAX_VALUE);
            document.add(new Field(DocumentConstants.LUCENE_SORTFIELD_PREFIX + String.valueOf(customField.getID()), encoded, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else if(ApplicationConstants.STRING_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()))) {
            document.add(new Field(String.valueOf(customField.getID()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else if(ApplicationConstants.LARGE_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()))) {
            document.add(new Field(String.valueOf(customField.getID()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else if(ApplicationConstants.LIST_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customField.getCustomFieldType()))) {
            document.add(new Field(String.valueOf(customField.getID()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else {
            document.add(new Field( String.valueOf(customField.getID()), ApplicationConstants.NULL_VALUE, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    private void indexCustomFieldData(Document document, CustomFieldValueResource.CustomFieldValueResponse customFieldValue) {
        if(ApplicationConstants.DATE_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldValue.getCustomFieldType()))
                || ApplicationConstants.DATE_TIME_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldValue.getCustomFieldType()))) {
            if(StringUtils.isNotBlank(customFieldValue.getValue())) {
                Date dateValue = new Date(Long.parseLong(customFieldValue.getValue()));
                if(StringUtils.equalsIgnoreCase(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldValue.getCustomFieldType()),ApplicationConstants.DATE_VALUE)) {
                    LocalDate localDate = LocalDateFactory.from(dateValue);
                    if(dateValue != null) {
                        document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), LuceneUtils.localDateToString(localDate), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                        document.add(new Field(DocumentConstants.LUCENE_SORTFIELD_PREFIX + String.valueOf(customFieldValue.getCustomFieldId()), LuceneUtils.localDateToString(localDate), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                    }
                } else {
                    document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), LuceneUtils.dateToString(dateValue), Field.Store.YES,Field.Index.NOT_ANALYZED_NO_NORMS));
                    document.add(new Field(DocumentConstants.LUCENE_SORTFIELD_PREFIX + String.valueOf(customFieldValue.getCustomFieldId()), LuceneUtils.dateToString(dateValue), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                }
            } else {
                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
        } else if(ApplicationConstants.NUMBER_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldValue.getCustomFieldType()))) {
            if(StringUtils.isNotBlank(customFieldValue.getValue())) {
                final String stringValue = doubleConverter.getStringForLucene(Double.valueOf(customFieldValue.getValue()));
                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), stringValue, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
            final String encoded = NumericUtils.doubleToPrefixCoded(StringUtils.isNotBlank(customFieldValue.getValue()) ? Double.valueOf(customFieldValue.getValue()) : Double.MAX_VALUE);
            document.add(new Field(DocumentConstants.LUCENE_SORTFIELD_PREFIX + String.valueOf(customFieldValue.getCustomFieldId()), encoded, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
        } else if(ApplicationConstants.LIST_VALUE.equals(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldValue.getCustomFieldType()))){
            if(StringUtils.isNotBlank(customFieldValue.getSelectedOptions())) {
                if(StringUtils.equalsIgnoreCase(customFieldValue.getCustomFieldType(), ApplicationConstants.MULTI_SELECT) || StringUtils.equalsIgnoreCase(customFieldValue.getCustomFieldType(), ApplicationConstants.CHECKBOX)) {
                    String[] customFieldOptions = customFieldValue.getSelectedOptions().split(",");
                    List<String> customFieldOptionList = Arrays.asList(customFieldOptions);
                    if(customFieldOptionList != null && customFieldOptionList.size() > 0) {
                        customFieldOptionList.stream().forEach(customFieldOption -> {
                            if (customFieldOption != null) {
                                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), customFieldOption, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()) + SelectStatisticsMapper.RAW_VALUE_SUFFIX, customFieldOption, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                            }
                        });
                    }
                } else {
                    document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), customFieldValue.getSelectedOptions(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                    document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()) + SelectStatisticsMapper.RAW_VALUE_SUFFIX, customFieldValue.getSelectedOptions(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
                }
            } else {
                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
        } else {
            if(StringUtils.isNotBlank(customFieldValue.getValue())) {
                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), customFieldValue.getValue(), Field.Store.YES, Field.Index.ANALYZED));
                final String customText = getValueForSorting(customFieldValue.getValue());
                if (StringUtils.isNotBlank(customText)) {
                    document.add(new Field("sort_"+String.valueOf(customFieldValue.getCustomFieldId()), customText, Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                }
            } else {
                document.add(new Field(String.valueOf(customFieldValue.getCustomFieldId()), ApplicationConstants.NULL_VALUE, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
            }
        }
    }

    private String getValueForSorting(final String fieldValue) {
        final String trimmed = (fieldValue == null) ? null : fieldValue.trim();
        if (!StringUtils.isBlank(trimmed)) {
            return trimmed;
        }
        else {
            return String.valueOf('\ufffd');
        }
    }
}
