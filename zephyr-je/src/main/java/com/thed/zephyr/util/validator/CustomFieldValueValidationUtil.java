package com.thed.zephyr.util.validator;

import com.thed.zephyr.util.ApplicationConstants;
import org.apache.commons.lang.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * This class will hold the common validation method for custom field values.
 */
public class CustomFieldValueValidationUtil {

    /**
     * Validate the entity type passed in the user request.
     * @param entityType
     * @return
     */
    public static boolean validateEntityType(String entityType) {
        if (!ApplicationConstants.ENTITY_TYPE.TESTSTEP.toString().equalsIgnoreCase(entityType) &&
                !ApplicationConstants.ENTITY_TYPE.EXECUTION.toString().equalsIgnoreCase(entityType)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     *
     * @param requestObject
     * @return
     */
    public static boolean isObjectNull(Object requestObject) {
        return Objects.isNull(requestObject);
    }

    /**
     *
     * @param customFieldType
     * @return
     */
    public static boolean validateCustomFieldType(String customFieldType) {
        return StringUtils.isNotBlank(customFieldType) &&
                !ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.containsKey(customFieldType);
    }

    /**
     *
     * @param customFieldType
     * @param dstCustomFieldType
     * @return
     */
    public static boolean validateCustomFieldTypeWithGivenCustomField(String customFieldType, String dstCustomFieldType) {
       return StringUtils.isNotBlank(customFieldType) &&
                !ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(customFieldType).equalsIgnoreCase(dstCustomFieldType);
    }

    public static boolean validateCustomFieldValueWithType(String customFieldType, String value) {

        if(StringUtils.isNotBlank(value) && !value.equalsIgnoreCase(StringUtils.EMPTY)) {
            if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType)
                    .equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {

                Pattern pattern = Pattern.compile(ApplicationConstants.DECIMAL_NUMBER_REGEXP);
                if(!pattern.matches(ApplicationConstants.DECIMAL_NUMBER_REGEXP,value)) {
                    return Boolean.TRUE;
                }

            }else if(ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType)
                    .equalsIgnoreCase(ApplicationConstants.DATE_TIME_VALUE) || ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType)
                    .equalsIgnoreCase(ApplicationConstants.DATE_VALUE)) {

                return !StringUtils.isNumeric(value);
            }
        }
        return Boolean.FALSE;
    }

    public static boolean validateNumberValueMaxLength(String customFieldType, String value) {

        if (ApplicationConstants.CUSTOM_FIELD_VALUE_TYPE_MAP.get(customFieldType)
                .equalsIgnoreCase(ApplicationConstants.NUMBER_VALUE)) {
            if(StringUtils.isNotBlank(value) && !value.equalsIgnoreCase(StringUtils.EMPTY)) {
                Double inputValue = Double.valueOf(value);

                if((inputValue > ApplicationConstants.MAX_VAL || inputValue < ApplicationConstants.MIN_VAL)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }


    /**
     *
     * @param entityType
     * @param dstEntityType
     * @return
     */
    public static boolean validateEntityTypeWithGivenCustomField(String entityType, String dstEntityType) {
        return StringUtils.isNotBlank(entityType) && !StringUtils.equalsIgnoreCase(dstEntityType, entityType);
    }

    /**
     *
     * @param stringValueToValidate
     * @return
     */
    public static boolean isStringBlank(String stringValueToValidate) {
        return StringUtils.isBlank(stringValueToValidate);
    }

    public static boolean validateCustomFieldValueIdWithGivenId(Long customFieldValueId, Long dstCustomFieldValueId) {
        return !customFieldValueId.equals(dstCustomFieldValueId);
    }

    public static boolean validateCustomFieldIdWithGivenId(Long customFieldId, Long dstCustomFieldId) {
        return !customFieldId.equals(dstCustomFieldId);
    }

    public static boolean validateEntityIdWithGivenId(Integer entityId, Integer dstEntityId) {
        return !entityId.equals(dstEntityId);
    }

    public static boolean validateTextValueMaxLength(String customFieldType, String value, Integer maxLength) {
        return StringUtils.isNotBlank(value) &&
                (ApplicationConstants.CUSTOM_FIELD_TYPE_MAP.get(customFieldType)
                        .equalsIgnoreCase("TEXT")) && StringUtils.length(value) > maxLength;
    }
}
