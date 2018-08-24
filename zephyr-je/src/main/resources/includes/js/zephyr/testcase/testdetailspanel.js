JIRA.ViewIssueTabs.onTabReady(function(container) {
	if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
	if(typeof ZEPHYR.testDetails == 'undefined') ZEPHYR.testDetails = {};
		
    /**
     * For page refresh, we get many events, one of them has container as document.
     * For tabClick event, we can check if container has Zephyr Audit element.
     */
	if(AJS.$('#je-audit-tabpanel').length < 1){
		return;
	}

	var currentIssueId;
	try {
		if (JIRA.Issue.getIssueId()) {
			currentIssueId = JIRA.Issue.getIssueId();
		}
	}catch(e){}
	
	ZEPHYR.testDetails.TestAuditDetailsView = Backbone.View.extend({		
		initialize: function() {
			_.bindAll(this, 'getAuditDetails', 'applyData', 'displayAuditDetailsOnShowMore', 'refreshTestDetails');
		},

        displayAuditDetailsOnInit: function(currentIssueId) {
            this.offset = 0;
            this.maxRecords = 20;
            this.count = 20;
            this.currentIssueId = currentIssueId;
            var instance = this;

            var issueIdChanged = this.currentIssueId;//(this.auditIssue == undefined || this.auditIssue.id != currentIssueId);
            if(issueIdChanged) {
                this.auditIssue = {"id": currentIssueId};
                var data = {
                    maxRecords: this.maxRecords,
                    offset: 	this.offset,
                    issueId: 	currentIssueId
                };
                this.getAuditDetails(data, function(response) {
                    /* During fast scrolling, there may be multiple request get fired, lets make sure we only stick to the current Issue */
                    if(currentIssueId == JIRA.Issue.getIssueId()){
                        instance.auditIssue.response = response;
                        instance.applyData(response);
                    } else{
                        AJS.log("Ignoring this audit result.." + JIRA.Issue.getIssueId() + " " + currentIssueId);
                    }
                });
            } else {
            	try {
	                if(this.auditIssue && this.auditIssue.response)
	                    this.applyData(this.auditIssue.response);
            	} catch(e) {
            		console.log(e);
            	}
            }
        },
		
		getAuditDetails: function(data, callback) {
	        jQuery.ajax({
	            url: getRestURL() + "/audit?entityType=TESTSTEP",
	            data: data,
	            contentType: "application/json",
	            success: function(response) {
	                if(callback)
	                	callback(response);
	            }
	        });
		},
		
		/* Idempotent method, cant be called multiple times */
		applyData: function(response){
	        // On refresh and on IE9 the div#je-audit-issuetab-child element is not attached,
	        // so attaching the element to DOM if the tab is 'Test Details History'.
	        if(AJS.$('ul#issue-tabs li#je-audit-tabpanel.active').length == 1 && AJS.$('#je-audit-issuetab-child').length == 0) {
	            AJS.$('#issue_actions_container').html(ZEPHYR.TestDetails.addTestDetailsHistoryContainer());
	        }
	        // On refresh and on IE9 the div#je-audit-issuetab-child element is not attached,
	        // so attaching the element to DOM if the tab is 'All'.
	        if(AJS.$('ul#issue-tabs li#all-tabpanel.active').length == 1 && AJS.$('#je-audit-issuetab-child').length == 0) {
	            AJS.$('#issue_actions_container').append(ZEPHYR.TestDetails.addTestDetailsHistoryContainer());
	        }
	        AJS.$('#je-audit-issuetab-child').html(ZEPHYR.TestDetails.addTestDetailsHistory({
	            auditLogs: 		response.auditLogs,
	            contextPath:	contextPath
	        }));
	        AJS.$('#audit-history-show-more').bind('click', this.displayAuditDetailsOnShowMore);
	        AJS.$('#testdetails-refresh').bind('click', this.refreshTestDetails);
	        this.count = response.auditLogs.length;
	        if(this.count >= response.totalItemsCount) AJS.$('.audit-history-show-more-container').remove();
	    },
		
	    displayAuditDetailsOnShowMore: function(ev) {
	    		ev.preventDefault();
	    		var instance = this;
	    		this.offset = this.offset + this.maxRecords;
	    		var data = {
	    			maxRecords: this.maxRecords, 
	    			offset: 	this.offset, 
	    			issueId: 	JIRA.Issue.getIssueId()
	    		};
	    		this.getAuditDetails(data, function(response) {
	    			AJS.$('#testdetails-wrapper').append(ZEPHYR.TestDetails.addAuditHistory({
    					auditLogs: 		response.auditLogs,
    					displayHeader: 	false,
    					contextPath:	contextPath
    				}));
    				instance.count += response.auditLogs.length;
    				if(instance.count >= response.totalItemsCount) AJS.$('.audit-history-show-more-container').remove();
	    		})
	    },
	    
	    // Refresh the test details
	    refreshTestDetails: function(ev) {
	    	ev.preventDefault();
            this.resetData();
            currentIssueId = currentIssueId || JIRA.Issue.getIssueId() || null;
	    	this.displayAuditDetailsOnInit(currentIssueId);
	    },

        resetData: function(){
            this.auditIssue = null;
        }

	});
	
	if(!ZEPHYR.testDetails.testAuditDetailsView) {
        ZEPHYR.testDetails.testAuditDetailsView = new ZEPHYR.testDetails.TestAuditDetailsView({el: AJS.$('.issuePanelWrapper')});
	} else ZEPHYR.testDetails.testAuditDetailsView.delegateEvents();
    if(AJS.$('li#je-audit-tabpanel.active').length > 0 || AJS.$('li#all-tabpanel.active').length > 0) {
        //If container is available and has our audit panel, we should force refresh
        if(container && container.find('#je-audit-tabpanel').length > 0){
            ZEPHYR.testDetails.testAuditDetailsView.resetData();
        }
    	ZEPHYR.testDetails.testAuditDetailsView.displayAuditDetailsOnInit(currentIssueId);
    }
});