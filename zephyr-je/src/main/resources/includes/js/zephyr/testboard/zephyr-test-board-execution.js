/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }
ZEPHYR.Agile.TestBoard.EXECUTION_MAX_RESULT_ALLOWED = 10;

// Executions
ZEPHYR.Agile.TestBoard.Execution = Backbone.Model.extend();
ZEPHYR.Agile.TestBoard.ExecutionCollection = Backbone.Collection.extend({
	model: ZEPHYR.Agile.TestBoard.Execution,
 	url: function() {
 		if(ZEPHYR.Agile.TestBoard.data.page == ZEPHYR.Agile.TestBoard.page.sprint) {
 			return getRestURL() + "/zql/executeSearch";
 		} else if(ZEPHYR.Agile.TestBoard.data.page == ZEPHYR.Agile.TestBoard.page.issues) {
 			return getRestURL() + "/execution/executionsByIssue";
 		}
 	},
 	parse: function(resp, xhr) {
		this.offset = resp.offset;
		this.currentIndex = resp.currentIndex;
		this.maxResultAllowed = resp.maxResultAllowed;
		this.linksNew = resp.linksNew;
		this.totalCount = resp.totalCount;
		this.executionStatus = resp.executionStatuses;
 		return resp.executions;
 	}
});

//{call ZEPHYR.Agile.TestBoard.getCycleZQLQuery data="[$cycle.versionName, $cycle.name]" /}
ZEPHYR.Agile.TestBoard.getCycleZQLQuery = function(params, output) {
	var fixVersion = params[0],
		cycleName = params[1],
		projectKey = params[2],
		zql = 'project = "' + ZEPHYR.Agile.TestBoard.escapeString(projectKey) + '" AND fixVersion = "' + ZEPHYR.Agile.TestBoard.escapeString(fixVersion) + '" AND cycleName in ("' 
			+ ZEPHYR.Agile.TestBoard.escapeString(cycleName) +'")';
	
	return appendSoyOutputOnCall(encodeURIComponent(zql), output);
}

/**
 * Execution pagination view
 */
ZEPHYR.Agile.TestBoard.ExecutionPaginationView = Backbone.View.extend({
    tagName:'div',
    events:{
    	"click [id^='zql-pagination-']" : 'executePaginatedSearch',
    	"click [id^='refreshZQLId']"	: "refreshZQLView"
    },
    initialize:function (options) {
    	this.eventId = (options.eventId) ? '_' + options.eventId: '';
        this.model.bind("reset", this.render, this);
    }, 
    render:function () {
    	if(this.model.length > 0) {
    		var addZQLPaginationFooterHTML = ZEPHYR.Templates.TestBoard.addZQLPaginationFooterHTML({
	    		totalCount: this.model.totalCount,
	    		currentIndex: this.model.currentIndex,
	    		maxAllowed: this.model.maxResultAllowed,
	    		linksNew: this.model.linksNew
	    	})
    		this.$el.html(addZQLPaginationFooterHTML);
    	} else {
			this.$el.html('');
		}
		return this;
    },
    executePaginatedSearch : function(ev) {
    	ev.preventDefault();
    	this.offset = eval(ev.target.attributes['page-id'].value);
    	this.currentOffset = (this.offset/ZEPHYR.Agile.TestBoard.EXECUTION_MAX_RESULT_ALLOWED) + 1;
    	ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET + this.eventId, this.offset);
    },
    
    refreshZQLView: function(ev) {
    	ev.preventDefault();
    	ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.EXECUTIONS_GET + this.eventId, this.offset);
    }
});
