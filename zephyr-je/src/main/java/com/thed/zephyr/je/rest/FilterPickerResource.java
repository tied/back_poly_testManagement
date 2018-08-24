package com.thed.zephyr.je.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchRequestManager;
import com.atlassian.jira.rest.v1.util.CacheControl;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.util.I18nHelper;
import com.thed.zephyr.util.ZephyrCacheControl;

@Api(value = "FilterPicker Resource API(s)", description = "Following section describes the rest resources pertaining to FilterPickerResource")
@Path("picker")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class FilterPickerResource {
    private static final Logger log = Logger.getLogger(FilterPickerResource.class);

    private final JiraAuthenticationContext authContext;
    private final SearchRequestManager searchRequestManager;
    private static final String DELIMS = "-_/\\,+=&^%$#*@!~`'\":;<> ";

    
    public FilterPickerResource(JiraAuthenticationContext authContext, I18nHelper.BeanFactory i18nBeanFactory,
    		SearchRequestManager searchRequestManager){
		this.authContext = authContext;
		this.searchRequestManager=searchRequestManager;
    }


	@ApiOperation(value = "Get Search For Filter")
	@ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
			@ApiImplicitParam(name = "response", value = "{\"options\":[{\"id\":\"10001\",\"label\":\"f1\",\"value\":\"f1\",\"type\":\"option\",\"icon\":\"\"}]}")})
	@Path ("filters")
    @GET
    public Response searchForFilters(@QueryParam ("query") String jqlQuery){
    	if(jqlQuery == null){
    		jqlQuery = "";
    	}
        return Response.ok(getFilters(jqlQuery)).cacheControl(ZephyrCacheControl.never()).build();
    }

    public FilterPickerResponse getFilters(String jqlQuery){
    	final StringBuilder newQueryBuilder = new StringBuilder();
    	final StringTokenizer tokenizer = new StringTokenizer(jqlQuery, DELIMS);
    	while (tokenizer.hasMoreElements()) {
    		final String token = tokenizer.nextToken();
    		if (StringUtils.isNotBlank(token)) {
    			newQueryBuilder.append("+").append(token).append(" ");
    		}
    	}

    	final String newQuery = newQueryBuilder.toString().trim();

    	final SharedEntitySearchParametersBuilder builder = new SharedEntitySearchParametersBuilder();
    	builder.setName(StringUtils.isBlank(jqlQuery) ? null : newQuery);
    	builder.setDescription(StringUtils.isBlank(jqlQuery) ? null : newQuery);

    	// we are using OR searching at the moment. This may change in the future
    	// As we are using wildcards, set mode to wildcard
    	builder.setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD);
    	// what are we sorting on
    	builder.setSortColumn(SharedEntityColumn.NAME, true);

    	builder.setEntitySearchContext(SharedEntitySearchContext.USE);

    	final SharedEntitySearchResult<SearchRequest> searchResults = searchRequestManager.search(builder.toSearchParameters(), authContext.getLoggedInUser(), 0, 15);
    	final List<SearchRequest> list = searchResults.getResults();

    	final FilterPickerResponse response = new FilterPickerResponse("filt-1","Filter Search","optgroup","0");


    	for (SearchRequest searchRequest : list)
    	{
    		final FilterPickerOptions option = new FilterPickerOptions(String.valueOf(searchRequest.getId()), 
    				searchRequest.getName(), searchRequest.getName(), "option","");
    		response.addOptions(option);
    	}

        return response;
    }
 
  
    @XmlRootElement
    public static class FilterPickerResponse
    {
    	@XmlElement
        private String id;
    	@XmlElement
        private String label;
        @XmlElement
        private String type;
        @XmlElement
        private String weight;
        @XmlElement
        private List<FilterPickerOptions> options = null;

        @SuppressWarnings({"unused"})
        private FilterPickerResponse() {}

		public FilterPickerResponse(String id,String label, String type, String weight) {
			this.id=id;
			this.label = label;
			this.type = type;
			this.weight = weight;
		}

        
		public FilterPickerResponse(String label, String type, String weight,
				List<FilterPickerOptions> options) {
			this.label = label;
			this.type = type;
			this.weight = weight;
			this.options = options;
		}

		public void addOptions(FilterPickerOptions option) {
			if (options == null) {
				options = new ArrayList<FilterPickerOptions>();
			}
			options.add(option);
		}

    }

    @XmlRootElement
    public static class FilterPickerOptions
    {
        @XmlElement
        private String id;
    	@XmlElement
        private String label;
    	@XmlElement
        private String value;
    	@XmlElement
        private String type;
		@XmlElement
        private String icon;

        @SuppressWarnings({"unused"})
        private FilterPickerOptions() {}

        public FilterPickerOptions(String id, String label,String value, String type,
				String icon) {
			this.id = id;
			this.label = label;
			this.value=value;
			this.type = type;
			this.icon = icon;
		}
    }
}
