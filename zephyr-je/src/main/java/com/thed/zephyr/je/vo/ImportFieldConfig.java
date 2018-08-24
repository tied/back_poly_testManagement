package com.thed.zephyr.je.vo;

import java.util.List;

import com.thed.zephyr.je.vo.ImportFieldSchema;

public class ImportFieldConfig {

	/*-----------------------------------------------------------------------
	 * ATTRIBUTES
	 *---------------------------------------------------------------------*/

	/* primary key */
	private String id ;
	
	/* Entity to which this custom field belongs.
	 * Entity name should be same as Java class name. It should be in proper case.
	 * E.g. "Requirement" and not "requirement" 
	 * */
	//private String entityName;

	/*system field or custom field */
	private Boolean systemField;
	
	/* data-type: int, long, string, etc.
	 * Explicit foreign key is not added to FieldTypeMetaData.id table */
	//private String fieldTypeMetadata ;
	private ImportFieldSchema fieldSchema ;
	
	/* field name, all lower-case, starts with alphabetic character. Holds value of "name" attribute
	 * <property column="zcf_myCustomField" name="myCustomField" not-null="false" type="java.lang.String"/> */
	//private String fieldName ;

	/* column name, all lower-case, starts with alphabetic character. Holds value of "column" attribute
	 * <property column="zcf_myCustomField" name="myCustomField" not-null="false" type="java.lang.String"/> */
	//private String columnName ;
	
	/* Descriptive field name */
	private String displayName ;

	/* Long description */
	//private String description ;
	
	/* value is mandatory at client */
	private Boolean mandatory ;
	
	/* value is searchable */
	//private Boolean searchable ;
	
	/* value is importable */
	//private Boolean importable ;
	
	/* value is exportable */
	//private Boolean exportable ;
	
	/* key-value mappings if datatype is LOV. 
	 * This value is saved in Preference table. 
	 * Preference.name is entity.fieldname.LOV, e.g.: requirement.zcf_1001.LOV*/
//	@Column(name="lovValue", length = 255)
//	@Transient
//	private String lovValue ;

	/* length of column if of type String */
	private Integer length;

    private List<String> allowedValues; //id and name will be colon seperated

    public ImportFieldConfig() {
    	super();
    }
	public ImportFieldConfig(String id, String entityName, boolean systemField,
			ImportFieldSchema fieldSchema, String displayName, boolean mandatory, int length, List<String> allowedValues) {
		super();
		this.id = id;
		this.systemField = systemField;
		this.fieldSchema = fieldSchema;
		this.displayName = displayName;
		this.mandatory = mandatory;
		this.length = length;
        this.allowedValues = allowedValues;
	}
	
	/*-----------------------------------------------------------------------
	 * OVERRIDE
	 *---------------------------------------------------------------------*/

	

	/**
	 * Merges (i..e. copies) attributes from input entity to itself.
	 * 
	 * @param updatedFieldConfig
	 */
	public void merge(ImportFieldConfig updatedFieldConfig) {
		this.setId(updatedFieldConfig.getId());
		this.setSystemField(updatedFieldConfig.getSystemField());
		this.setDisplayName(updatedFieldConfig.getDisplayName());
		this.setMandatory(updatedFieldConfig.getMandatory());
	}
	
	/*-----------------------------------------------------------------------
	 * GETTER/SETTER
	 *---------------------------------------------------------------------*/

	

	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	
	public Boolean getSystemField() {
		return systemField;
	}


	public void setSystemField(Boolean systemField) {
		this.systemField = systemField;
	}

	public String getDisplayName() {
		return displayName;
	}


	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Boolean getMandatory() {
		return mandatory;
	}


	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

    public List<String> getAllowedValues() {
        return allowedValues;
    }
	public ImportFieldSchema getFieldSchema() {
		return fieldSchema;
	}
	public void setFieldSchema(ImportFieldSchema fieldSchema) {
		this.fieldSchema = fieldSchema;
	}
	public void setAllowedValues(List<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	
}

