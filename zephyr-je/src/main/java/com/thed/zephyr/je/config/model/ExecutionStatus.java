package com.thed.zephyr.je.config.model;

import java.io.Serializable;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

public class ExecutionStatus implements Serializable{

    @JsonProperty private final String name;
    @JsonProperty private final Integer id;
    @JsonProperty private final String description;
    @JsonProperty private final String color;
    //Determines if status is of type System or custom.
    //Only custom type statuses can be edit/deleted!
    // 0 = System Execution status
    // 1 = Custom Execution status
    @JsonProperty private final Integer type;
    
    @JsonCreator
    public ExecutionStatus(@JsonProperty("id") Integer id,
    				  @JsonProperty("name") String name,
                      @JsonProperty("description") String description,
                      @JsonProperty("color") String color,
                      @JsonProperty("type") Integer type)
    {
        this.id = checkNotNull(id, "id");
        this.name = checkNotNull(name, "name");
        this.description = description;
        this.color = checkNotNull(color, "color");
        this.type = checkNotNull(type, "type");
    }
    
	public Integer getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Integer getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public String getColor() {
		return color;
	}
	
	public Map<String, Object> toMap(){
		return ImmutableMap.<String, Object>of("id", this.getId(), "color", this.getColor(), "description", this.getDescription(), "name", this.getName().toUpperCase());
	}
}