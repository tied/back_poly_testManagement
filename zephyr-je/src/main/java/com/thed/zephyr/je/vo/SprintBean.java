package com.thed.zephyr.je.vo;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.*;
import org.joda.time.DateTime;

@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "id",
        "self",
        "state",
        "name",
        "startDate",
        "endDate",
        "completeDate",
        "originBoardId"
})
public class SprintBean {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("self")
    private String self;
    @JsonProperty("state")
    private State state;
    @JsonProperty("name")
    private String name;
    @JsonProperty("startDate")
    private DateTime startDate;
    @JsonProperty("endDate")
    private DateTime endDate;
    @JsonProperty("completeDate")
    private DateTime completeDate;
    @JsonProperty("originBoardId")
    private Integer originBoardId;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The id
     */
    @JsonProperty("id")
    public Long getId() {
        return id;
    }

    /**
     *
     * @param id
     * The id
     */
    @JsonProperty("id")
    public void setId(Long id) {
        this.id = id;
    }

    /**
     *
     * @return
     * The self
     */
    @JsonProperty("self")
    public String getSelf() {
        return self;
    }

    /**
     *
     * @param self
     * The self
     */
    @JsonProperty("self")
    public void setSelf(String self) {
        this.self = self;
    }

    /**
     *
     * @return
     * The state
     */
    @JsonProperty("state")
    public String getState() {
        return state.name();
    }

    /**
     *
     * @param state
     * The state
     */
    @JsonProperty("state")
    public void setState(String state) {
        this.state = State.valueOf(state);
    }

    /**
     *
     * @param state
     * The state
     */
    @JsonProperty("state")
    public void setState(State state) {
        this.state = state;
    }

    /**
     *
     * @return
     * The name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The startDate
     */
    @JsonProperty("startDate")
    public DateTime getStartDate() {
        return startDate;
    }

    /**
     *
     * @param startDate
     * The startDate
     */
    @JsonProperty("startDate")
    public void setStartDate(DateTime startDate) {
        this.startDate = startDate;
    }

    /**
     *
     * @return
     * The endDate
     */
    @JsonProperty("endDate")
    public DateTime getEndDate() {
        return endDate;
    }

    /**
     *
     * @param endDate
     * The endDate
     */
    @JsonProperty("endDate")
    public void setEndDate(DateTime endDate) {
        this.endDate = endDate;
    }

    /**
     *
     * @return
     * The completeDate
     */
    @JsonProperty("completeDate")
    public DateTime getCompleteDate() {
        return completeDate;
    }

    /**
     *
     * @param completeDate
     * The completeDate
     */
    @JsonProperty("completeDate")
    public void setCompleteDate(DateTime completeDate) {
        this.completeDate = completeDate;
    }

    /**
     *
     * @return
     * The originBoardId
     */
    @JsonProperty("originBoardId")
    public Integer getOriginBoardId() {
        return originBoardId;
    }

    /**
     *
     * @param originBoardId
     * The originBoardId
     */
    @JsonProperty("originBoardId")
    public void setOriginBoardId(Integer originBoardId) {
        this.originBoardId = originBoardId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public enum State{
        FUTURE,
        ACTIVE,
        CLOSED;
        private State() {
        }
    }
}