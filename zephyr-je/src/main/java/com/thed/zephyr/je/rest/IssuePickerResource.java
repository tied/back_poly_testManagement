package com.thed.zephyr.je.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import com.sun.jersey.spi.container.ResourceFilters;
import com.thed.zephyr.je.rest.filter.ZFJApiFilter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.issue.search.IssuePickerResults;
import com.atlassian.jira.bc.issue.search.IssuePickerSearchService;
import com.atlassian.jira.bc.issue.search.IssuePickerSearchService.IssuePickerParameters;
import com.atlassian.jira.bc.issue.search.LuceneCurrentSearchIssuePickerSearchProvider;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.DelimeterInserter;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.util.profiling.UtilTimerStack;
import com.opensymphony.util.TextUtils;
import com.thed.zephyr.util.ConfigurationConstants;
import com.thed.zephyr.util.JiraUtil;
import com.thed.zephyr.util.ZephyrCacheControl;

@Api(value = "IssuePicker Resource API(s)", description = "Following section describes the rest resources pertaining to IssuePickerResource")
@Path("issues")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@ResourceFilters(ZFJApiFilter.class)
public class IssuePickerResource {
    private static final Logger log = Logger.getLogger(IssuePickerResource.class);

    private final JiraAuthenticationContext authContext;
    private final IssueManager issueManager;
    private final ApplicationProperties applicationProperties;
    private final ProjectManager projectManager;
    private static final String RUNNING_ISSUE_PICKER_SEARCH = "Running issue-picker search: ";
	public static LuceneCurrentSearchIssuePickerSearchProvider searchProvider = ComponentManager.getComponentInstanceOfType(LuceneCurrentSearchIssuePickerSearchProvider.class);
    private final IssueTypeSchemeManager issueTypeSchemeManager;


    public IssuePickerResource(JiraAuthenticationContext authContext, I18nHelper.BeanFactory i18nBeanFactory,
            IssuePickerSearchService service, IssueManager issueManager,
            ApplicationProperties applicationProperties, ProjectManager projectManager, IssueTypeSchemeManager issueTypeSchemeManager){
		this.authContext = authContext;
		this.issueManager = issueManager;
		this.applicationProperties = applicationProperties;
		this.projectManager = projectManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
    }


    /**
     * This is the AJAX entry point to find issues given a query string.  The particluar instance of this call can "grey out" issues
     * by providing extra filterer parameters.
     *
     * @param query             what the user type in to search on
     * @param currentJQL        the JQL of the current Search.
     * @param currentIssueKey   the current issue or null
     * @param currentProjectId  the current project id or null
     * @param showSubTasks      set to false if sub tasks should be greyed out
     * @param showSubTaskParent set to false to have parent issue greyed out
     * @return a Response containing a list of {@link com.atlassian.jira.rest.v1.issues.IssuePickerResource.IssueSection} containing matching issues
     */
    @ApiOperation(value = "Get Issues for Test", notes = "Get Issues Data for Test by Query")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "{\"sections\":[{\"label\":\"History Search\",\"sub\":\"Showing 19 of 51 matching issues\",\"id\":\"hs\",\"issues\":[{\"key\":\"SONY-2036\",\"keyHtml\":\"SONY-2036\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"rtyhjk\",\"summaryText\":\"rtyhjk\"},{\"key\":\"SONY-2035\",\"keyHtml\":\"SONY-2035\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"Test\",\"summaryText\":\"Test\"},{\"key\":\"SONY-1831\",\"keyHtml\":\"SONY-1831\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"SONY Project\",\"summaryText\":\"SONY Project\"},{\"key\":\"SONY-2019\",\"keyHtml\":\"SONY-2019\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"xcv\",\"summaryText\":\"xcv\"},{\"key\":\"NOK-37\",\"keyHtml\":\"NOK-37\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"dfghjk\",\"summaryText\":\"dfghjk\"},{\"key\":\"SONY-1975\",\"keyHtml\":\"SONY-1975\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"SONY Project\",\"summaryText\":\"SONY Project\"},{\"key\":\"SONY-2034\",\"keyHtml\":\"SONY-2034\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"test\",\"summaryText\":\"test\"},{\"key\":\"TEST-1\",\"keyHtml\":\"TEST-1\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"test\",\"summaryText\":\"test\"},{\"key\":\"TEST-4\",\"keyHtml\":\"TEST-4\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"dfgb\",\"summaryText\":\"dfgb\"},{\"key\":\"SONY-1983\",\"keyHtml\":\"SONY-1983\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"AZXCVGBHN\",\"summaryText\":\"AZXCVGBHN\"},{\"key\":\"SONY-2023\",\"keyHtml\":\"SONY-2023\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"test\",\"summaryText\":\"test\"},{\"key\":\"SONY-2025\",\"keyHtml\":\"SONY-2025\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"test\",\"summaryText\":\"test\"},{\"key\":\"SONY-2032\",\"keyHtml\":\"SONY-2032\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"sdfg\",\"summaryText\":\"sdfg\"},{\"key\":\"SONY-2033\",\"keyHtml\":\"SONY-2033\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"asdfgh\",\"summaryText\":\"asdfgh\"},{\"key\":\"SONY-1\",\"keyHtml\":\"SONY-1\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"SONY Project-1\",\"summaryText\":\"SONY Project-1\"},{\"key\":\"SONY-1018\",\"keyHtml\":\"SONY-1018\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"SONY Project\",\"summaryText\":\"SONY Project\"},{\"key\":\"SONY-2028\",\"keyHtml\":\"SONY-2028\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"sdfghj\",\"summaryText\":\"sdfghj\"},{\"key\":\"SONY-2027\",\"keyHtml\":\"SONY-2027\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"fghj\",\"summaryText\":\"fghj\"},{\"key\":\"SONY-2026\",\"keyHtml\":\"SONY-2026\",\"img\":\"/download/resources/com.thed.zephyr.je/images/icons/ico_zephyr_issuetype.png\",\"summary\":\"CLONE - test\",\"summaryText\":\"CLONE - test\"}]}]}")})
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)    
    public Response getIssues(@QueryParam("query") final String query,
                                      @QueryParam("currentJQL") final String currentJQL,
                                      @QueryParam("currentIssueKey") final String currentIssueKey,
                                      @QueryParam("currentProjectId") final String currentProjectId,
                                      @QueryParam("showSubTasks") final boolean showSubTasks,
                                      @QueryParam("showSubTaskParent") final boolean showSubTaskParent)
    {

        String typeId = JiraUtil.getTestcaseIssueTypeId();
        String jql = "type="+typeId;
        return Response.ok(getIssuesForTest(query, jql, currentIssueKey, currentProjectId, showSubTasks, showSubTaskParent)).cacheControl(ZephyrCacheControl.never()).build();
    }

    /**
     * Returns default IssueType ID for a given project. Inteded for internal consumption only
     * @param projectId projectId or projectKey
     * @return issueTypeId
     * @throws 500 if projectId is invalid
     */

    @ApiOperation(value = "Get Default Issue Type", notes = "Get Default Issue Type by Project Id")
    @ApiImplicitParams({@ApiImplicitParam(name = "request", value = "{ }"),
            @ApiImplicitParam(name = "response", value = "10001")})
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("default")
    public Response getDefaultIssueType(@QueryParam("project") final String projectId){
        if (TextUtils.stringSet(projectId)){
            Project project = null;
            if(projectId.matches("-?\\d+(\\.\\d+)?"))
                project = projectManager.getProjectObj((new Long(projectId)));
            else
                project = projectManager.getProjectObjByKey(projectId);
            IssueType issueType = issueTypeSchemeManager.getDefaultIssueType(project);
            if(issueType != null)
                return Response.ok(issueType.getId()).cacheControl(ZephyrCacheControl.never()).build();
        }
        return Response.ok("").cacheControl(ZephyrCacheControl.never()).build();
    }


    /**
     * This is the AJAX entry point to find issues given a query string.  The particluar instance of this call can "grey out" issues
     * by providing extra filterer parameters.
     *
     * @param query             what the user type in to search on
     * @param currentJQL        the JQL of the current Search.
     * @param currentIssueKey   the current issue or null
     * @param currentProjectId  the current project id or null
     * @param showSubTasks      set to false if sub tasks should be greyed out
     * @param showSubTaskParent set to false to have parent issue greyed out
     * @return A list of {@link com.atlassian.jira.rest.v1.issues.IssuePickerResource.IssueSection} containing matching issues
     */
    private IssuePickerResultsWrapper getIssuesForTest(String query, String currentJQL, String currentIssueKey, String currentProjectId, boolean showSubTasks, boolean showSubTaskParent) {
        final JiraServiceContext context = getContext();
        final IssuePickerResultsWrapper results = new IssuePickerResultsWrapper();

        Issue currentIssue = null;
        if (TextUtils.stringSet(currentIssueKey)){
            currentIssue = issueManager.getIssueObject(currentIssueKey);
        }

        int limit = getLimit();
        Project project = null;

        if (TextUtils.stringSet(currentProjectId)){
            project = projectManager.getProjectObj((new Long(currentProjectId)));
        }

        final IssuePickerSearchService.IssuePickerParameters pickerParameters = new IssuePickerSearchService.IssuePickerParameters(query, currentJQL, currentIssue, project, showSubTasks, showSubTaskParent, limit);
        Collection<IssuePickerResults> pickerResults = getResults(context, pickerParameters);
        final IssuePickerSearchService pickerService = ComponentAccessor.getComponentOfType(IssuePickerSearchService.class);
        if(pickerService != null) {
        	pickerResults = pickerService.getResults(context, pickerParameters);	
        } else{
        	pickerResults = getResults(context, pickerParameters);
        }
        for (IssuePickerResults pickerResult : pickerResults){
        	final Collection<Issue> issues = pickerResult.getIssues();
            final String labelKey = pickerResult.getLabel();
            final String id = pickerResult.getId();
            final String label = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText(labelKey);
            if (!issues.isEmpty()){
                final IssueSection section = new IssueSection(id, label, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("jira.ajax.autocomplete.showing.x.of.y", Integer.toString(issues.size()), Integer.toString(pickerResult.getTotalIssues())), null);
                results.addSection(section);

                for (Issue issue : issues){
                    String typeId = JiraUtil.getTestcaseIssueTypeId();
                	//Only ADD it to the target list if the issue is of type test
                    if(StringUtils.equalsIgnoreCase(issue.getIssueTypeObject().getId(),typeId)) {
                		section.addIssue(getIssue(issue, pickerResult));
                	}
                }

            }
            else{
                final IssueSection section = new IssueSection(id, label, null, ComponentAccessor.getJiraAuthenticationContext().getI18nHelper().getText("jira.ajax.autocomplete.no.matching.issues"));
                results.addSection(section);
            }
        }

        return results;
    }

    private Collection<IssuePickerResults> getResults(
			JiraServiceContext context, IssuePickerParameters issuePickerParams) {
        final String timer = RUNNING_ISSUE_PICKER_SEARCH + issuePickerParams.getQuery();
        UtilTimerStack.push(timer);
        try
        {
            final Collection<IssuePickerResults> results = new ArrayList<IssuePickerResults>();
            int issuesRemaining = issuePickerParams.getLimit();
            if (searchProvider.handlesParameters(context.getLoggedInUser(), issuePickerParams))
            {
                issuesRemaining--; // remove one item for heading

                final IssuePickerResults result = searchProvider.getResults(context, issuePickerParams, issuesRemaining);

                int size = result.getIssues().size();
                if (size == 0)
                {
                    size = 1; // if no items returned - add 1 for the no items row
                }
                issuesRemaining -= size;

                results.add(result);
                if (issuesRemaining <= 0)
                {
                    return results;
                }
            }
            return results;
        }
        finally
        {
            UtilTimerStack.pop(timer);
        }
    }


	// get the number of items to display.
    private int getLimit()
    {
        //Default limit to 20
        int limit = 20;

        try
        {
            limit = Integer.valueOf(applicationProperties.getDefaultBackedString(APKeys.JIRA_AJAX_AUTOCOMPLETE_LIMIT));
        }
        catch (NumberFormatException nfe)
        {
            log.error("jira.ajax.autocomplete.limit does not exist or is an invalid number in jira-application.properties.", nfe);
        }

        return limit;
    }

    /*
    * We use direct html instead of velocity to ensure the AJAX lookup is as fast as possible
    */
    private IssuePickerIssue getIssue(Issue issue, IssuePickerResults result)
    {
        DelimeterInserter delimeterInserter = new DelimeterInserter("<b>", "</b>");
        // Lets assume that anything that has one of the following one char before it, is a new word.
        delimeterInserter.setConsideredWhitespace("-_/\\,.+=&^%$#*@!~`'\":;<>");

        final String[] keysTerms = result.getKeyTerms().toArray(new String[result.getKeyTerms().size()]);
        final String[] summaryTerms = result.getSummaryTerms().toArray(new String[result.getSummaryTerms().size()]);

        final String issueKey = delimeterInserter.insert(TextUtils.htmlEncode(issue.getKey()), keysTerms);
        final String issueSummary = delimeterInserter.insert(TextUtils.htmlEncode(issue.getSummary()), summaryTerms);

        return new IssuePickerIssue(issue.getKey(), issueKey, getIconURI(issue.getIssueTypeObject()), issueSummary, issue.getSummary());
    }

    private String getIconURI(IssueConstant issueConstant)
    {
        //mainly here for unit tests.
        if (issueConstant == null)
        {
            return "";
        }

        return issueConstant.getIconUrl();
    }

    // protected for unit testing
    protected JiraServiceContext getContext()
    {
        final ApplicationUser user = authContext.getLoggedInUser();
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        return new JiraServiceContextImpl(user, errorCollection);
    }

    @XmlRootElement
    public static class IssuePickerResultsWrapper
    {
        @XmlElement
        private List<IssueSection> sections = null;

        @SuppressWarnings({"UnusedDeclaration", "unused"})
        private IssuePickerResultsWrapper() {}

        public IssuePickerResultsWrapper(List<IssueSection> sections)
        {
            this.sections = sections;
        }

        public void addSection(IssueSection section)
        {
            if (sections == null)
            {
                sections = new ArrayList<IssueSection>();
            }
            sections.add(section);
        }
    }

    @XmlRootElement
    public static class IssueSection
    {
        @XmlElement
        private String label;
        @XmlElement
        private String sub;
        @XmlElement
        private String id;
        @XmlElement
        private String msg;
        @XmlElement
        private List<IssuePickerIssue> issues = null;

        @SuppressWarnings({"unused"})
        private IssueSection() {}

        public IssueSection(String id, String label, String sub, String msg, List<IssuePickerIssue> issues)
        {
            this.label = label;
            this.sub = sub;
            this.id = id;
            this.issues = issues;
            this.msg = msg;
        }

        public IssueSection(String id, String label, String sub, String msg)
        {
            this.label = label;
            this.sub = sub;
            this.id = id;
            this.msg = msg;
        }

        public void addIssue(IssuePickerIssue issue)
        {
            if (issues == null)
            {
                issues = new ArrayList<IssuePickerIssue>();
            }
            issues.add(issue);
        }
    }

    @XmlRootElement
    public static class IssuePickerIssue
    {
        @XmlElement
        private String key;
        @XmlElement
        private String keyHtml;
        @XmlElement
        private String img;
        @XmlElement
        private String summary;
        @XmlElement
        private String summaryText;

        @SuppressWarnings({"UnusedDeclaration", "unused"})
        private IssuePickerIssue() {}

        public IssuePickerIssue(String key, String keyHtml, String img, String summary, String summaryText)
        {
            this.key = key;
            this.keyHtml = keyHtml;
            this.img = img;
            this.summary = summary;
            this.summaryText = summaryText;
        }
    }
   
}
