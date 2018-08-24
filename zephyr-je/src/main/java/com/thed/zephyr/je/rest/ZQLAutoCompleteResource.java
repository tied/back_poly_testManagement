package com.thed.zephyr.je.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.ValueGeneratingClauseHandler;
import com.atlassian.jira.jql.util.JqlStringSupport;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.DelimeterInserter;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.opensymphony.util.TextUtils;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import com.thed.zephyr.je.zql.core.SearchHandlerManager;
import com.thed.zephyr.util.ZephyrCacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * Rest end point for ZQL Autocomplete suggestions.
 *
 */
@Api(value = "Execution Filter Auto Complete Resource API(s)", description = "Following section describes rest resources (API's) pertaining to ExecutionFilteAutoCompleteResource")
@Path ("zql/autocomplete")
@Produces ({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@ResourceFilters(ZFJApiFilter.class)
public class ZQLAutoCompleteResource
{
    private final SearchHandlerManager searchHandlerManager;
    private final JiraAuthenticationContext authenticationContext;
    private final JqlStringSupport jqlStringSupport;

    public ZQLAutoCompleteResource(final JiraAuthenticationContext authenticationContext, final JqlStringSupport jqlStringSupport,
    		final SearchHandlerManager searchHandlerManager)
    {
        this.searchHandlerManager = searchHandlerManager;
        this.authenticationContext = authenticationContext;
        this.jqlStringSupport = jqlStringSupport;
    }

    @GET
    /**
     * This is the AJAX entry point to find issues given a query string.  The particluar instance of this call can "grey out" issues
     * by providing extra filterer parameters.
     *
     * @param fieldName         what the user has typed in to search for the field name
     * @param fieldValue        the portion of the field value that has already been provided by the user.
     * @return a Response containing a list of {@link com.atlassian.jira.rest.v1.jql.AutoCompleteResource.AutoCompleteResult}'s containing the matching autocomplete values
     */
    @ApiOperation(value = "Get ZQL Auto Complete Result", notes = "Get ZQL Auto Complete Result by Field Name, Field Value")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"results\":[{\"value\":\"Major\",\"displayName\":\"<b>M<\\/b>ajor\"},{\"value\":\"Minor\",\"displayName\":\"<b>M<\\/b>inor\"}]}")})
    public Response getZQLAutocomplete(@QueryParam ("fieldName") final String fieldName,
                                      @QueryParam("fieldValue") final String fieldValue)
    {
        final AutoCompleteResultsWrapper results = new AutoCompleteResultsWrapper();
        if (fieldValue != null)
        {
            getAutoCompleteResultsForField(fieldName, fieldValue, results);
        }
        return Response.ok(results).cacheControl(ZephyrCacheControl.never()).build();
    }

    private void getAutoCompleteResultsForField(String fieldName, String fieldValue, AutoCompleteResultsWrapper results)
    {
        final ApplicationUser searcher = authenticationContext.getLoggedInUser();
        final Collection<ClauseHandler> clauseHandlers = searchHandlerManager.getClauseHandler(searcher, fieldName);
        if (clauseHandlers != null && clauseHandlers.size() == 1)
        {
            ClauseHandler clauseHandler = clauseHandlers.iterator().next();

            if (clauseHandler instanceof ValueGeneratingClauseHandler)
            {
                final ClauseValuesGenerator clauseValuesGenerator =  ((ValueGeneratingClauseHandler) (clauseHandler))
                                                                            .getClauseValuesGenerator();
                generateResults(clauseValuesGenerator, searcher, fieldName, fieldValue, results);
            }
        }
    }


    @XmlRootElement
    public static class AutoCompleteResultsWrapper
    {
        @XmlElement
        private List<AutoCompleteResult> results = new ArrayList<AutoCompleteResult>();

        private AutoCompleteResultsWrapper() {}

        public void addResult(AutoCompleteResult result)
        {
            results.add(result);
        }
    }

    @XmlRootElement
    public static class AutoCompleteResult
    {
        @XmlElement
        private String value;
        @XmlElement
        private String displayName;

        private AutoCompleteResult() {}

        public AutoCompleteResult(String value, String displayName)
        {
            this.value = value;
            this.displayName = displayName;
        }
    }

    private String quoteZeroPaddedNumbers(final String value)
    {
        if (value == null)
        {
            return value;
        }
        try
        {
            final long longVal = Long.parseLong(value);
            // Great, its a number, see if it starts with a 0
            if (value.startsWith("0"))
            {
                return "\"" + value + "\"";
            }
            else
            {
                return value;
            }
        }
        catch(NumberFormatException nfe)
        {
            return value;
        }
    }

    private void generateResults(ClauseValuesGenerator clauseValuesGenerator, ApplicationUser searcher, String fieldName, String fieldValue, AutoCompleteResultsWrapper results)
    {
        DelimeterInserter delimeterInserter = new DelimeterInserter("<b>", "</b>");
        // Lets assume that anything that has one of the following one char before it, is a new word.
        delimeterInserter.setConsideredWhitespace("-_/\\,.+=&^%$#*@!~`'\":;<>(");

        final ClauseValuesGenerator.Results generatorResults = clauseValuesGenerator.getPossibleValues(searcher, fieldName, fieldValue, 15);

        final List<ClauseValuesGenerator.Result> list = generatorResults.getResults();
        for (ClauseValuesGenerator.Result result : list)
        {
            StringBuilder displayName = new StringBuilder();
            for (int i = 0; i < result.getDisplayNameParts().length; i++)
            {
                if (i != 0)
                {
                    displayName.append(" ");
                }
                String displayNamePart = result.getDisplayNameParts()[i];
                // Need to encode both BEFORE we let the delimeterInserter work so that we will correctly match the strings, even if they have
                // HTML escape characters in them.
                displayName.append(delimeterInserter.insert(TextUtils.htmlEncode(displayNamePart), new String[]{TextUtils.htmlEncode(fieldValue)}));
            }
            final String encodedValue = jqlStringSupport.encodeValue(result.getValue());
            // JRA-19142 - quote zero padded numbers for autocomplete suggestions
            results.addResult(new AutoCompleteResult(quoteZeroPaddedNumbers(encodedValue), displayName.toString()));
        }
    }
}
