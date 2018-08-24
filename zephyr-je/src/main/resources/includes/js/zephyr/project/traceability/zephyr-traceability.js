/**
 * Traceability Page
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Traceability == 'undefined') { ZEPHYR.Traceability = {}; }

ZEPHYR.Traceability.fixVersion;
ZEPHYR.Traceability.fixVersionId;
ZEPHYR.Traceability.issueType;
ZEPHYR.Traceability.issueTypeId;
ZEPHYR.Traceability.maxGridRecords = 20;
ZEPHYR.Traceability.selectedIssues = [];
ZEPHYR.Traceability.nonProjectCentricViewTabURL = 'selectedTab=com.thed.zephyr.je%3Apdb_traceability_panel_section';

ZEPHYR.Traceability.RequirementSelectionView = Backbone.View.extend({
	tagName : 'form',
	events : {
		'click .search-button'	: 	'searchClickHandler'
	},
	initialize : function(options){
		this.fixVersion = options.fixVersion;
		this.issueType = options.issueType;
		this.issueTypeId = options.issueTypeId;
		this.isDefToReqSelected = options.isDefToReqSelected;
		this.render();
	},
	render : function(){
		var traceabilityRequirementViewHTML = ZEPHYR.Templates.Project.Traceability.traceabilityRequirementView({
			isDefToReqSelected: ((this.isDefToReqSelected == 'true') ? true: false)
		});
		this.$el.html(traceabilityRequirementViewHTML);
		
		this.fetchVersions();
	    this.fetchIssueTypes(function(){
	    	jQuery('#requirement-section').removeClass('hide').addClass('show');
	    });
		ZEPHYR.Traceability.IssuesGridView = new ZEPHYR.Traceability.IssueListGridView();

	    jQuery('#traceabilityReport').on('click', function(ev){
	    	ev.preventDefault();
	    	
	    	if(AJS.$(ev.target).attr('disabled'))
	    		return;
	    	
	    	var params = {
	    		fixVersion : ZEPHYR.Traceability.fixVersion,
	    		fixVersionId : ZEPHYR.Traceability.fixVersionId,
	    		issueType : ZEPHYR.Traceability.issueType,
	    		issueTypeId : ZEPHYR.Traceability.issueTypeId,
	    		selectedPage : parseInt(jQuery('#selectedIndex').text()) || 1
	    	},
			selectedReportType = jQuery('input[name=traceabilityReportType]:checked').attr('id'),
			_href = ev.target.href;
	    	
	    	sessionStorage.setItem('zephyr-je-traceability-report-type', selectedReportType);
	    	sessionStorage.setItem('zephyr-je-traceability-report-parameters', JSON.stringify(params));
	    	window.location.href = _href;
	    	ZEPHYR.Traceability.Report.init(selectedReportType);
	    });
	    ZEPHYR.Traceability.Navigation.setLocation();

	    return this;
	},
	fetchVersions : function() {
		var projectId = AJS.$("#projId").val(),
			instance = this;
		
		/* JIRA's versions */
	    jQuery.ajax({
	        url: getRestURL() + "/util/versionBoard-list?projectId=" + projectId,
	        type : "get",
	        success : function(versions) {
	        	var versionsHTML = ZEPHYR.Templates.Project.Traceability.renderFixVersions({
            		versions: versions,
            		selectedVersionId: -1
            	});
            	AJS.$('#traceabilityVersions').html(versionsHTML);
            	if(AJS.$.auiSelect2) { // (AJS.$.fn && AJS.$.fn.auiSelect2) || 
            		AJS.$('#traceabilityVersions-dd').auiSelect2();
    				AJS.$('.select2-container').css({'width': '200px'});
    				AJS.$('.traceabilityVersions-dd span.select2-chosen').html(AJS.I18n.getText('zephyr-je.pdb.traceability.select.version.label'));
            	} else {
            		if(instance.fixVersion) {
            			if(instance.fixVersionId)
            				AJS.$('#traceabilityVersions-dd').find('option[value="' + that.fixVersionId + '"]').attr('selected', 'selected');
            			else
	            			AJS.$('#traceabilityVersions-dd').find('option[title="' + instance.fixVersion + '"]').attr('selected', 'selected');
            		}
            		var versionSelect = new AJS.SingleSelect({
	                        element: AJS.$('#traceabilityVersions-dd'),
	                        maxInlineResultsDisplayed: 15,
	                        maxWidth:400,
	                        submitInputVal: true,
	                        matchingStrategy: '(^|.*?(\\s*|\\(\))({0})(.*)' // Fix for ZFJ-1462
	                    });
            		if(!instance.fixVersion) {
            			AJS.$('#traceabilityVersions-dd-field').attr('placeholder', AJS.I18n.getText('zephyr-je.pdb.traceability.select.version.label'));
            			versionSelect.clear();
            		}
            		AJS.$('#traceabilityVersions-dd-field').unbind('keydown');
            		AJS.$('#traceabilityVersions-dd-field').bind('keydown', function(ev) {
            			var _keyCode = ev.keyCode || ev.which;
            			
            			if(_keyCode == 13)
            				ev.preventDefault();
            		});
            	}
	        }
	    });
	},
	fetchIssueTypes : function(showRequirementSection) {		
		var that = this,
			formatState = function(state) {
				if (!state.id) { 
					return state.text; 
				}
				var iconUrl = jQuery(state.element).data('iconurl'),
					$state = AJS.$('<span><img src="' + iconUrl + '" class="select-img-icon" /> ' + state.text + '</span>');
				
				return $state;
			};
			
		jQuery.ajax({
	        url: contextPath + "/rest/api/2/issuetype",
	        type : "get",
	        success : function(issueTypes) {
	        	var issueTypesHTML = ZEPHYR.Templates.Project.Traceability.renderIssueTypes({
        			issueTypes: issueTypes,
        			testIssueTypeId: AJS.$('#testIssueTypeId').val()
        		});
            	AJS.$('#issueTypes').html(issueTypesHTML);
            	if(AJS.$.auiSelect2) {
            		AJS.$('#issueTypes-dd').auiSelect2();
    				AJS.$('.select2-container').css({'width': '200px'});
    				AJS.$('.issueTypes-dd span.select2-chosen').html(AJS.I18n.getText('zephyr-je.pdb.traceability.select.issuetype.label'));
            	} else {
            		if(that.issueType) {
            			if(that.issueTypeId)
            				AJS.$('#issueTypes-dd').find('option[value="' + that.issueTypeId + '"]').attr('selected', 'selected');
            			else
            				AJS.$('#issueTypes-dd').find('option[title="' + that.issueType + '"]').attr('selected', 'selected');
            		}
            		var issueSelect = new AJS.SingleSelect({
	                        element: AJS.$('#issueTypes-dd'),
	                        maxInlineResultsDisplayed: 15,
	                        maxWidth:400,
	                        submitInputVal: true,
	                        matchingStrategy: '(^|.*?(\\s*|\\(\))({0})(.*)' // Fix for ZFJ-1462
	                    });
            		if(!that.issueType) {
	            		AJS.$('#issueTypes-dd-field').attr('placeholder', AJS.I18n.getText('zephyr-je.pdb.traceability.select.issuetype.label'));
						if (issueSelect.$container)
							issueSelect.clear();
            		}
            		AJS.$('#issueTypes-dd-field').unbind('keydown');
            		AJS.$('#issueTypes-dd-field').bind('keydown', function(ev) {
            			var _keyCode = ev.keyCode || ev.which;
            			
            			if(_keyCode == 13)
            				ev.preventDefault();
            		});
            	}
				showRequirementSection();
	        }
	    });
	},
	fetchIssues : function(jqlQuery, startAt, currentIndex, restoreIssueList) {
		var maxAllowed = ZEPHYR.Traceability.maxGridRecords,
			that = this;

    	ZEPHYR.Loading.showLoadingIndicator();
		jQuery.ajax({
	        url: contextPath + '/rest/api/2/search',
            type : 'GET',
            data: {
				jql : jqlQuery,
				startAt : startAt || 0,
				maxResults : maxAllowed,
				validateQuery : true
			},
            success: function(response) {
	        	ZEPHYR.Loading.hideLoadingIndicator();
            	if(response && response.issues && response.issues.length) {
            		ZEPHYR.Traceability.IssuesGridView.render(response, maxAllowed, currentIndex, startAt, restoreIssueList);	
            	}
            	else{
            		that.showErrorField(AJS.I18n.getText('zephyr-je.pdb.traceability.issues.results.none.found'));	
            	}
			},
            error: function(error){
	        	ZEPHYR.Loading.hideLoadingIndicator();
            	var errorJSON = JSON.parse(error.responseText);
				that.showErrorField(errorJSON.errorMessages);	
			}
        });
	},
	showErrorField : function(message){
		var errorField = ZEPHYR.Templates.Project.Traceability.showErrorField({message : message});
		jQuery('#issue-grid').html(errorField);
		if(jQuery('#navigateToReport').hasClass('show')){
			jQuery('#navigateToReport').removeClass('show').addClass('hide');
		}
	},
	searchClickHandler : function(){
		var projectId = AJS.$("#projId").val(),
			fixVersion = jQuery('#traceabilityVersions-dd').find('option:selected').text(),
			fixVersionId = jQuery('#traceabilityVersions-dd').find('option:selected').val(),
			issueType = jQuery('#issueTypes-dd').find('option:selected').text(),
			issueTypeId = jQuery('#issueTypes-dd').find('option:selected').val(),
			jqlQuery = 'project = "' + projectId + '" AND issuetype = "' + issueTypeId + '"',
			result = this.validateSelectedOptions();

		//for analytics
        if (za != undefined) {
            za.track({'event': ZephyrEvents.SEARCH_TRACEABILITY_REPORTS,
                    'eventType':'Click'
                },
                function (res) {
                    console.log('Analytics test: -> ',res);
                });
        }

		if(jQuery('.traceabilityVersions-dd span.select2-chosen').html() === AJS.I18n.getText('zephyr-je.pdb.traceability.select.version.label') || AJS.$('#traceabilityVersions-dd-field') == ''){
			fixVersion = null;
		}

		if(fixVersion){
			jqlQuery += ' AND fixVersion = "' + ZEPHYR.Traceability.escapeString(fixVersion) + '"';
		}
		ZEPHYR.Traceability.fixVersion = ZEPHYR.Traceability.escapeString(fixVersion);
		ZEPHYR.Traceability.fixVersionId = fixVersionId;
		ZEPHYR.Traceability.issueType = issueType;
		ZEPHYR.Traceability.issueTypeId = issueTypeId;
		ZEPHYR.Traceability.selectedIssues = [];
		sessionStorage.setItem('zephyr-je-traceability-selectedIssues', ZEPHYR.Traceability.selectedIssues.toString());

		jQuery('#traceabilityReport').attr('disabled', 'disabled');

		if(result){
			this.fetchIssues(jqlQuery, 0);
		}
		else{
			return false;
		}

	},
	validateSelectedOptions : function(){
		var message,
			versionVal = jQuery('div.traceabilityVersions-dd').find('span.select2-chosen').html() || AJS.$('#traceabilityVersions-dd-field').val(),
			issueTypeVal = jQuery('div.issueTypes-dd').find('span.select2-chosen').html() || AJS.$('#issueTypes-dd-field').val(),
			result = true,
			cxt = jQuery("#traceability-message-bar");
			
		if(issueTypeVal === AJS.I18n.getText('zephyr-je.pdb.traceability.select.issuetype.label') || issueTypeVal == ''){
			message = AJS.I18n.getText('zephyr-je.pdb.traceability.validate.select.issuetype.message');
			result = false;
		}
		
		if(!result){
			cxt.empty();
			AJS.messages.error(cxt, {
				title: AJS.I18n.getText('zephyr.je.submit.form.error.title'),
			    body: message,
			    closeable: true
			});

			setTimeout(function(){
				jQuery('#traceability-message-bar').html('');
			}, 5000);
		}

		return result;
	}
});

ZEPHYR.Traceability.IssueListGridView = Backbone.View.extend({
	tagName : 'div',
	initialize : function() {
		_.bindAll(this, 'updateSelectionCount',
			'selectAllIssues',
			'selectIssuesOnGrid',
			'selectIssues');
	},
    
    getLinks: function(totalCount, currentOffset, maxResultAllowed) {
    	var linksNew = [];
    	linksNew = ZEPHYR.generatePageNumbers(totalCount, currentOffset, maxResultAllowed);	
    	return linksNew;
    },
	render : function(issuesList, maxAllowed, currentIndex, startAt, restoreIssueList){
		currentIndex = currentIndex || 1;
		var linksNew = this.getLinks(issuesList.total, currentIndex, maxAllowed);
		var issuesListHtml = ZEPHYR.Templates.Project.Traceability.renderIssuesGrid({
    		issuesList : issuesList,
    		maxAllowed : maxAllowed,
    		currentIndex : currentIndex || 1,
    		linksNew: linksNew
    	}),
    	that = this, 
    	countEnd = startAt + maxAllowed,
    	totalRecords;

    	jQuery('#issue-grid').html(issuesListHtml);
    	jQuery('.issues-pagination').on('click', that.executeSearch);
    	jQuery('.results-count-start').text(startAt + 1);

    	totalRecords = parseInt(jQuery('.results-count-total').html());
    	countEnd = ((countEnd > totalRecords) || (totalRecords <= maxAllowed)) ? totalRecords : countEnd;
    	jQuery('.results-count-end').text(countEnd);

    	jQuery('.select-issues').on('click', that.selectIssues);
    	jQuery('#selectAllID').on('click', function(ev){
    		that.selectAllIssues(ev);
    	});
    	jQuery('.refresh-table').on('click', function(ev){
    		that.refreshGrid(ev);
    	});

    	that.showGenerateTraceabilityButton();

    	if(restoreIssueList && typeof restoreIssueList === 'function'){
    		restoreIssueList();
    	}
    	else{
    		that.selectIssuesOnGrid(ZEPHYR.Traceability.selectedIssues);
    	}
    	return this;
	},
	showGenerateTraceabilityButton : function(){
		jQuery('#navigateToReport').removeClass('hide').addClass('show');
	},
	refreshGrid : function(ev){
		ev.preventDefault();
		this.executeSearch(ev, true);
		jQuery('#traceabilityReport').attr('disabled', 'disabled');
		ZEPHYR.Traceability.selectedIssues = [];
		sessionStorage.setItem('zephyr-je-traceability-selectedIssues', ZEPHYR.Traceability.selectedIssues.toString());
	},
	updateIssueArray : function(allIssues, selectedIssuesArray){
		
		for(var i = 0; i < allIssues.length; i++){

			var $issue = jQuery(allIssues[i]),
				$issueRow = $issue.parents('tr.issuerow'),
				$issueId = $issueRow.data('issueid'),
				index = selectedIssuesArray.indexOf($issueId);

			if(index > -1){
				selectedIssuesArray.splice(index, 1);
			}
		}

		return selectedIssuesArray;
	},
	selectAllIssues : function(ev){
		var allIssues = jQuery('.select-issues'),
			selectedIssuesArray = ZEPHYR.Traceability.selectedIssues,
			that = this;
		if(!allIssues.length){
			return;
		}
		if(jQuery(ev.target).hasClass('selected')){
			allIssues.removeAttr('checked');
			allIssues.removeClass('selected');
			jQuery(ev.target).removeClass('selected');
			ZEPHYR.Traceability.selectedIssues = that.updateIssueArray(allIssues, selectedIssuesArray);
		}
		else{
			jQuery(ev.target).addClass('selected');
			allIssues.addClass('selected');
			allIssues.attr('checked', true);
			var selectedIssuesArray = ZEPHYR.Traceability.selectedIssues;

			for(var i = 0; i < allIssues.length; i++){
				var $selectedIssue = jQuery(allIssues[i]),
					$selectedIssueRow = $selectedIssue.parents('tr.issuerow'),
					$selectedIssueId = $selectedIssueRow.data('issueid');
				
				if(selectedIssuesArray.indexOf($selectedIssueId) > -1){
					continue;
				}

				selectedIssuesArray.push($selectedIssueId);
			}
			ZEPHYR.Traceability.selectedIssues = selectedIssuesArray;
		}
		if(selectedIssuesArray.length){
			jQuery('#traceabilityReport').removeAttr('disabled');
		}
		else{
			jQuery('#traceabilityReport').attr('disabled', 'disabled');
		}
		
		// Update the selection Count
		this.updateSelectionCount(selectedIssuesArray.length);
		sessionStorage.setItem('zephyr-je-traceability-selectedIssues', ZEPHYR.Traceability.selectedIssues.toString());
	},
	selectIssues : function(ev){

		var $selectedIssue = jQuery(ev.target),
			$selectedIssueRow = $selectedIssue.parents('tr.issuerow'),
			$selectedIssueId = $selectedIssueRow.data('issueid'),
			selectedIssuesArray = ZEPHYR.Traceability.selectedIssues,
			selectedIndex;

			if($selectedIssue.hasClass('selected')){
				$selectedIssue.removeClass('selected');
				selectedIndex = selectedIssuesArray.indexOf($selectedIssueId);
				if(selectedIndex > -1){
					selectedIssuesArray.splice(selectedIndex,1);
				}
			}
			else{
				$selectedIssue.addClass('selected');
				ZEPHYR.Traceability.selectedIssues.push($selectedIssueId);
			}
			
			if(selectedIssuesArray.length){
				jQuery('#traceabilityReport').removeAttr('disabled');
			}
			else{
				jQuery('#traceabilityReport').attr('disabled', 'disabled');
			}
			// Update the selection Count
			this.updateSelectionCount(selectedIssuesArray.length);
			ZEPHYR.Traceability.selectedIssues = selectedIssuesArray;
			sessionStorage.setItem('zephyr-je-traceability-selectedIssues', ZEPHYR.Traceability.selectedIssues.toString());
			
	},
	executeSearch : function(ev, isRefreshed){
		
		ev.preventDefault();
    	
    	var startAt = parseInt(jQuery(ev.target).data('pageid')),
    		currentIndex = parseInt(jQuery(ev.target).data('currentindex')),
    		projectId = AJS.$("#projId").val(),
    		fixVersion = ZEPHYR.Traceability.fixVersion || jQuery('#traceabilityVersions-dd').find('option:selected').text(),
    		issueType = ZEPHYR.Traceability.issueType || jQuery('#issueTypes-dd').find('option:selected').text(),
    		issueTypeId = ZEPHYR.Traceability.issueTypeId || jQuery('#issueTypes-dd').find('option:selected').val(),
    		jqlQuery = 'project = "' + projectId + '" AND issuetype = "' + issueTypeId + '"';
    	
    	if(jQuery('.traceabilityVersions-dd span.select2-chosen').html() === AJS.I18n.getText('zephyr-je.pdb.traceability.select.version.label')){
			fixVersion = null;
		}

    	if(fixVersion){
			jqlQuery += ' AND fixVersion = "' + ZEPHYR.Traceability.escapeString(fixVersion) + '"';
		}
    	if(!currentIndex){
    		currentIndex = jQuery(ev.target).hasClass('next') ? (parseInt(jQuery('#selectedIndex').text()) + 1) : (parseInt(jQuery('#selectedIndex').text()) - 1);
    	}

    	if(isRefreshed){
    		startAt = 0;
    		currentIndex = 1;
    	}

    	ZEPHYR.Traceability.RequirementsView.fetchIssues(jqlQuery, startAt, currentIndex);
	},
	selectIssuesOnGrid : function(selectedIssuesArray){

		var allIssues = jQuery('.select-issues'),
			selectedIssueCount = 0;
		
		if(!allIssues.length || !selectedIssuesArray.length){
			return;
		}

		for(var i = 0; i < allIssues.length; i++){

			var $issue = jQuery(allIssues[i]),
				$issueRow = $issue.parents('tr.issuerow'),
				$issueId = $issueRow.data('issueid');

			if(selectedIssuesArray.indexOf($issueId) > -1){
				$issue.attr('checked', true);
				$issue.addClass('selected');
				selectedIssueCount++;
			}
		}
		if(selectedIssueCount === allIssues.length){
			jQuery('#selectAllID').attr('checked', true);
			jQuery('#selectAllID').addClass('selected');
		}
		// Update the selection Count
		this.updateSelectionCount(selectedIssuesArray.length);
		jQuery('#traceabilityReport').removeAttr('disabled');
	},
	updateSelectionCount: function(count) {
		if(count == 0) {
    		AJS.$('#selection-count-id').html('');
    	} else {
    		var updateCountHTML = '<small><label id="count-check-id">' + AJS.I18n.getText('enav.bulk.select.label', count) + '</label></small>';
    		AJS.$('#selection-count-id').html(updateCountHTML);
    	}
	}
});

/**
 * Check if the selectedIssues is defined, else get the values from session in case of refresh 
 */
ZEPHYR.Traceability.getSelectedIssueIdsFromSession = function() {
	if(!ZEPHYR.Traceability.selectedIssues.length) {
		var selectedItems = sessionStorage.getItem('zephyr-je-traceability-selectedIssues');
		if(selectedItems)
			selectedItems = selectedItems.split(',');
		else
			selectedItems = [];
		ZEPHYR.Traceability.selectedIssues = selectedItems;
	}
}

ZEPHYR.Traceability.Navigation = {
	setLocation: function() {
		var projectKey = AJS.$("#projKey").val(),
            urlSuffix = 'type=report&offset=0',
            reportLoc = '';
        
        if(AJS.$('#isProjectCentricViewEnabled').val()) {
        	reportLoc = '#traceability-tab&' + urlSuffix;
        } else {
        	var _location = window.location.hash;
    		if(_location.indexOf('#selectedTab=') > -1)
    			reportLoc = '#' + ZEPHYR.Traceability.nonProjectCentricViewTabURL + '&' + urlSuffix;
    		else
    			reportLoc = '#' +  urlSuffix;
        }

        AJS.$('#traceabilityReport').attr('href', reportLoc);

	},
	updateURLIfNonProjectCentricView: function() {
		if(!AJS.$('#isProjectCentricViewEnabled').val()) {
			var _location = window.location.href;
    		if(_location.indexOf('?selectedTab=') > -1) {
    			var _href = sessionStorage.getItem('zephyr-je-traceability-report-href');
    			if(_href) {
    				sessionStorage.removeItem('zephyr-je-traceability-report-href');
    				window.location.href = _href;
    			}
    		}
		}
	},
	renderTraceabilityView : function(){		
		var chartType,
			that = this;

		ZEPHYR.Traceability.Navigation.updateURLIfNonProjectCentricView();
		var location = window.location.href;
		if(location.indexOf('type=report') > -1) {
			var offset = (location.indexOf('offset=') > -1) ? ZephyrURL('#offset') : 0;
    		chartType =  sessionStorage.getItem('zephyr-je-traceability-report-type');
    		that.loadCharts(chartType, offset);
    	} else {
    		var isRestoreRequired = sessionStorage.getItem('zephyr-je-traceability-isReqRestore');
    		var isDefToReqSelected = sessionStorage.getItem('zephyr-je-traceability-isDefToReq');
    		if(isRestoreRequired === "true"){
    			that.reloadState(isDefToReqSelected);
    		}
    		else{
    			ZEPHYR.Traceability.RequirementsView = new ZEPHYR.Traceability.RequirementSelectionView({
			    	el : '#traceability-container',
			    	isDefToReqSelected: false
			    });
    		}
    		that.attachTraceabilityDescription();
	    }
	},
	attachTraceabilityDescription: function() {
		jQuery('.header-section-secondary').show();
    	// Render the message
		AJS.$('#requirement-message').html(ZEPHYR.Templates.Project.Traceability.requirementDescription());
	},
	loadCharts : function(chartType, offset){
		ZEPHYR.Traceability.Navigation.setParamsFromSession();
		ZEPHYR.Traceability.Report.init(chartType, offset);
	},
	setParamsFromSession: function() {		
		var params = JSON.parse(sessionStorage.getItem('zephyr-je-traceability-report-parameters'));

		if(!params){
			return false;
		}

		ZEPHYR.Traceability.fixVersion = params.fixVersion;
		ZEPHYR.Traceability.fixVersionId = parseInt(params.fixVersionId);
		ZEPHYR.Traceability.issueType = params.issueType;
		ZEPHYR.Traceability.issueTypeId = params.issueTypeId;
		return params;
	},
	reloadState : function(isDefToReqSelected){
		var params = ZEPHYR.Traceability.Navigation.setParamsFromSession();
		
		if(!params){
			return;
		}

		var projectId = AJS.$("#projId").val(),
			_issueType = params.issueTypeId || params.issueType,
			jqlQuery = 'project = "' + projectId + '" AND issuetype = "' + _issueType + '"';
			startAt = (params.selectedPage - 1) * ZEPHYR.Traceability.maxGridRecords,
			currentIndex = params.selectedPage;
		
		if(params.fixVersion){
			jqlQuery += ' AND fixVersion = "' + params.fixVersion + '"';
		}
		ZEPHYR.Traceability.RequirementsView = new ZEPHYR.Traceability.RequirementSelectionView({
	    	el : '#traceability-container',
	    	fixVersion: params.fixVersion,
	    	issueType: params.issueType,
	    	issueTypeId: params.issueTypeId,
	    	isDefToReqSelected: isDefToReqSelected
	    });
		this.attachTraceabilityDescription();
		ZEPHYR.Traceability.RequirementsView.fetchIssues(jqlQuery, startAt, currentIndex, function(){
			var issuesStringList = sessionStorage.getItem('zephyr-je-traceability-selectedIssues').split(','),
				selectedIssuesList = [];
			
			for(var i = 0; i < issuesStringList.length; i++){
				selectedIssuesList.push(parseInt(issuesStringList[i]));
			}

			ZEPHYR.Traceability.IssuesGridView.selectIssuesOnGrid(selectedIssuesList);
			ZEPHYR.Traceability.selectedIssues = selectedIssuesList;

			function selectDropDownOption($selectElem, selectedText){
				var options = $selectElem.find('option');
				options.removeAttr('selected');
				for(var i = 0; i < options.length; i++){
					var option = AJS.$(options[i]);
					if(ZEPHYR.Traceability.escapeString(option.text()) === selectedText){
						option.attr('selected', 'selected');
					}
				}
			}

			if(params && !params.fixVersion){
				AJS.$('.traceabilityVersions-dd span.select2-chosen').html(AJS.I18n.getText('zephyr-je.pdb.traceability.select.version.label'));
			}
			else{
				AJS.$('.traceabilityVersions-dd span.select2-chosen').html(params.fixVersion);
				selectDropDownOption(AJS.$('.traceabilityVersions-dd'), params.fixVersion);
			}

			AJS.$('.issueTypes-dd span.select2-chosen').html(params.issueType);
			selectDropDownOption(AJS.$('.issueTypes-dd'), params.issueType);
		});
		
		sessionStorage.removeItem('zephyr-je-traceability-isReqRestore');
		sessionStorage.removeItem('zephyr-je-traceability-isDefToReq');
	}
}

ZEPHYR.Traceability.escapeString = function (str) {
	str = str.replace(/\\/g, '\\\\');
	str = str.replace(/"/g, '\\"');
	return str;
}

ZEPHYR.Traceability.init = function() {
	var _location = window.location.href,
		isProjectCentricViewEnabled = AJS.$('#isProjectCentricViewEnabled').val(),
		activeTab = AJS.$("input#zephyr-proj-tab").val();
	
	if((isProjectCentricViewEnabled && _location.indexOf('traceability-tab') > -1) || (!isProjectCentricViewEnabled && activeTab == 'traceability-tab')){
		if(ZEPHYR.Traceability.RequirementsView)
			ZEPHYR.Traceability.RequirementsView.remove();
		window.addEventListener("beforeunload", function(e) {
	    	var _loc = window.location.href;
	    	
	    	if(!isProjectCentricViewEnabled && _loc.indexOf('type=report') > -1 && _loc.indexOf('?selectedTab') > -1 && (document.activeElement && document.activeElement.id.indexOf('zephyr-je.topnav.tests.test.plan.traceability') == -1)) {
	    		sessionStorage.setItem('zephyr-je-traceability-report-href', ZEPHYR.Traceability.Report.reportPageURL);
	    	} else if(sessionStorage.getItem('zephyr-je-traceability-report-href'))
				sessionStorage.removeItem('zephyr-je-traceability-report-href');
	    });
		if(!isProjectCentricViewEnabled) {
			// Clear the offset(#) in the URL on selection of other tabs in non project centric view if in report page
			AJS.$('.browse-tab').bind('click', function(ev) {
				var _href = ev.target.href;
				var _loc = window.location.href;
				if(_loc.indexOf('type=report') > -1 && (_loc.indexOf('?' + ZEPHYR.Traceability.nonProjectCentricViewTabURL) > -1 || _loc.indexOf('?selectedTab=com.thed.zephyr.je:pdb_traceability_panel_section') > -1)) {
					sessionStorage.removeItem('zephyr-je-traceability-report-href');
					window.location.href = '#';					
				}
			});
		}
		ZEPHYR.Traceability.Navigation.renderTraceabilityView();
	} else
		return;
}

var isLoadedInIframe = function() {
	try {
		return (window !== window.parent);
	} catch(e) {
		return false;
	}
}

var InitPageContent = function(initCallback) {
	if(isLoadedInIframe()) {
		AJS.$(window).load(function(){
			initCallback();
		});
	} else {
		AJS.$(document).ready(function(){
			initCallback();
		});
	}
}

InitPageContent(function(){
	JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, panel, reason){
		if(AJS.$('#traceability-container').find('#report-section').length || AJS.$('#traceability-container').find('#requirement-section').length)
			return;
		ZEPHYR.Traceability.init();
    });
    ZEPHYR.Traceability.init();
});