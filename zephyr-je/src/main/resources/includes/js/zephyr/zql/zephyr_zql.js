ZEPHYR.ExecutionCycle = Backbone.Model.extend();
var executionColumnsInlineDialog;
var globaldetailViewContainer;
var globalzqlSearch;
var globalSchedule;

var stepSubmitButtonId = 'stepSubmitExecutionColumnsExecutionPage';
var closeButtonId = 'closeInlineDialogExecutionPage';
var allCustomFields;
var cycleObj = [];
var navigationObj = [];
ZEPHYR.ExecutionCycle.navObj = {
	offset: 0,
	maxResultAllowed: 10,
};
var allPagesPaginationPageWidth = {};
var atLastPagination= false;
var atFirstPagination = false;
var hideListView = true;

var stepsPagesUrlDetails = [];
var stepsTourXpathData = [];
var executionDetails = {};
var triggeringPaginationFrom = '';
var isReadyForPaginationChange = true;
var currentselectedExecutionId = '';

//This is to avoid repetative API calls, when left nav execution is clicked
var RIGHT_SECTION_TO_BE_UPDATED = 'right';
var updateSection = '';

ZEPHYR.ExecutionCycle.executionStepColumns = {};
ZEPHYR.ExecutionCycle.customFieldsOrder = [];

ZEPHYR.ExecutionCycle.testStep = {
	limit: 10,
	offset: 0,
	maxRecords: 0,
};

//Self executing function to set title of the page
(function(doc){
	doc.title = AJS.I18n.getText('enav.page.heading') +  doc.title;
})(document);

AJS.$(document).ready(function (event) {


	//  NEED TO HIT THE GET API HERE TO GET THE PAGINATION WIDTH
	// ZEPHYR.ExecutionCycle.navObj.maxResultAllowed = 10;

	if(!!document.URL.indexOf('gadget')) {
		var isRequiredElementLoaded = setInterval(function () {
			if (window.standalone) {
				if (AJS.$('.bulkAction-container')[0]) {
					zephyrZqlCallWalkthrough('walkThrough', false);
					clearInterval(isRequiredElementLoaded);
				}
			} else {
				clearInterval(isRequiredElementLoaded);
			}
		}, 500)

	}
});

function waitTillStandaloneLoad() {
	if (!!document.URL.indexOf('gadget')) {
		var isRequiredElementLoaded = setInterval(function () {
			if (AJS.$('.bulkAction-container')[0]) {
				zephyrZqlCallWalkthrough('walkThrough', false);
				clearInterval(isRequiredElementLoaded);
			}
		}, 500)

	}
}

function zephyrZqlCallWalkthrough(knowledgeTour, isNotFirstTime) {
	stepsTourXpathData = [
		{
			path: '//*[@id="custom-fidld-display-area"]/div/div/div/div[1]/h3',
			description: AJS.I18n.getText("walkthrough.testSummary.customField"),
			direction: 'down-right',
		}, {
			path: '//*[@id="readonly-comment-div"]',
			description: AJS.I18n.getText("walkthrough.zephyrZql.executions"),
			direction: 'top-left',
		}, {
			path: '//*[@id="attachmentexecutionmodule_heading"]/h3',
			description: AJS.I18n.getText("walkthrough.zephyrZql.attachments"),
			direction: 'down-right',
		}, {
			path: '//*[@id="teststepDetails_heading"]/h3',
			description: AJS.I18n.getText("walkthrough.zephyrZql.stepResult"),
			direction: 'down-right',
		}, {
			path: '//*[@id="testDetailGridExecutionPage"]//div[1]/div[2]/div/div[1]',
			element: AJS.$('.bulkAction-container')[0],
			description: AJS.I18n.getText("walkthrough.zephyrZql.stepResultColumnChooser"),
			direction: 'down-left',
		}, {
			path: '//*[@id="teststepDetails_heading"]/div/div[1]',
			description: AJS.I18n.getText("walkthrough.zephyrZql.stepResultFilter"),
			direction: 'top-left',
		}, {
			path: '//*[@id="execute-test-header-right"]',
			description: AJS.I18n.getText("walkthrough.executionScroll"),
			direction: 'down-left',
		}, {
			path: '//*[@id="execution-history-details"]/div[1]/h3',
			description: AJS.I18n.getText("walkthrough.zephyrZql.executionHistory"),
			direction: 'top-right',
		}, {
			path: '//*[@id="listViewBtn"]',
			description: AJS.I18n.getText("walkthrough.zephyrZql.listView"),
			direction: 'top-left',
		}, {
			path: '//*[@id="content"]/div[1]/div[2]/div[2]/header/div/h1/span',
			description: AJS.I18n.getText("walkthrough.zephyrZql.cycleSummaryPage"),
			direction: 'top-right',
		}, {
			path: '//*[@id="content"]/div[1]/div[2]/div[2]/header/div/h1/a',
			description: AJS.I18n.getText("walkthrough.zephyrZql.issueViewPage"),
			direction: 'down-right',
		}, {
			path: '//*[@id="return-to-search"]',
			description: AJS.I18n.getText("walkthrough.zephyrZql.searchTestExecutionPage"),
			direction: 'down-right',
		}
	];

	if (knowledgeTour == 'walkThrough') {
		stepsPagesUrlDetails = [
			{
				url: contextPath + '/secure/PlanTestCycle.jspa?projectKey=' + executionDetails.projectKey,
				title: AJS.I18n.getText('testCycle.summary.page.title'),
				toPage: 'cycleSummary',
			}
		];
		walkThroughCall(stepsTourXpathData, '#content ', 'executionPage', stepsPagesUrlDetails, isNotFirstTime, window.standalone);
	} else if (knowledgeTour == 'newPageLayout') {
		newPageLayout(stepsTourXpathData, '#content ', 'executionPage');
	}
}
AJS.$('#inline-dialog-execution-column-picker').live("click", function(e) {
   e.stopPropagation();
});

AJS.$('body').live("click", function(e){
    executionColumnsInlineDialog && executionColumnsInlineDialog.hide();
});

// function triggerView() {
//   var urlView = window.location.hash.split('&view=');
//   if(urlView && urlView[1] && urlView[1] === 'list') {
//     AJS.$('#listViewBtn').trigger('click');
//   }
// }

AJS.$('#' + stepSubmitButtonId).live('click', function() {
    var data = {};

    AJS.$('#inline-dialog-execution-column-picker li :checkbox').each(function() {
      if(this.checked) {
        ZEPHYR.ExecutionCycle.executionStepColumns[this.id].isVisible = "true";
      }	else {
        ZEPHYR.ExecutionCycle.executionStepColumns[this.id].isVisible = "false"
      }
    });

    data['preferences'] = ZEPHYR.ExecutionCycle.executionStepColumns;

    data = JSON.stringify(data);
    jQuery.ajax({
      url: getRestURL() + "/preference/setexecutioncustomization",
      type : "post",
      contentType :"application/json",
      data : data,
      dataType: "json",
      success : function(response) {
        window.zqlDetailView.attachDetailView(globaldetailViewContainer, globalzqlSearch);
        window.zqlDetailView.attachTestSteps();
        executionColumnsInlineDialog.hide();
      }
    });

    // window.executionDetailView.render();
    // window.teststepResultView.render();
});

function resizeTextarea() {
  if(!AJS.$('.jira-multi-select').find('textarea')[0] || !AJS.$("#editable-schedule-defects .representation ul li:last-child")[0]) {
    return;
  }
  var t = AJS.$('.jira-multi-select').find('textarea')[0];
  t.style.height = "1px";
  t.style.height = AJS.$('.jira-multi-select').find('.representation').height() + 10 +"px";
  t.style.paddingLeft = AJS.$(".representation ul li:last-child").offset().left + AJS.$(".representation ul li:last-child").width() - AJS.$(".representation ul").offset().left + 8 + "px";
  t.style.paddingTop = AJS.$(".representation ul li:last-child").offset().top - AJS.$(".representation ul").offset().top + 4 + "px";
}

AJS.$('#' + closeButtonId).live('click', function(){
    executionColumnsInlineDialog.hide();
});

AJS.$('#listViewBtn').live('click', function(ev, isRightSectionToBepdated){
	ev.stopPropagation();
	ev.stopImmediatePropagation();
	hideListView = false;
    AJS.$('.stl-exe-nav').removeClass('hide');
    AJS.$('#detailViewBtn').removeClass('active-view');
	AJS.$('#listViewBtn').addClass('active-view');
  toggleView('detail', 'list');
  resizeTextarea();
  var scrollTopPos = AJS.$('.execution-id.active').position().top + AJS.$('.execution-list-wrapper').scrollTop() - 95;
	var height = 0;
	if (AJS.$('#stl-exe-right-cycle-details-wrapper-container')) {
		height += AJS.$('#stl-exe-right-cycle-details-wrapper-container').height();
		height += parseFloat(AJS.$('#stl-exe-right-cycle-details-wrapper-container').css('padding-bottom'));
		height += parseFloat(AJS.$('#stl-exe-right-cycle-details-wrapper-container').css('padding-top'));
	}
	if (AJS.$('#stl-exe-right-detail-exec-view-wrapper-container')) {
		height += AJS.$('#stl-exe-right-detail-exec-view-wrapper-container').height();
		height += parseFloat(AJS.$('#stl-exe-right-detail-exec-view-wrapper-container').css('padding-bottom'));
		height += parseFloat(AJS.$('#stl-exe-right-detail-exec-view-wrapper-container').css('padding-top'));
	}

	if (AJS.$('#stl-exe-left-container')) {
		height -= height += parseFloat(AJS.$('#stl-exe-left-container').css('padding-top'));
		height -= height += parseFloat(AJS.$('#stl-exe-left-container').css('padding-bottom'));
		AJS.$('#stl-exe-left-container').css("heigt", height);
	}
	if (!isRightSectionToBepdated) {
	  scrollToElement(scrollTopPos);
	}
});

AJS.$('#detailViewBtn').live('click', function(){
	hideListView = true;
    AJS.$('.stl-exe-nav').addClass('hide');
    AJS.$('#detailViewBtn').addClass('active-view');
    AJS.$('#listViewBtn').removeClass('active-view');
    toggleView('list', 'detail');
    resizeTextarea();

});

function toggleView(arg1, arg2) {
	var query;
	if (window.location.hash.indexOf('view=') == -1) {
		if(window.location.hash.indexOf('?') == -1) {
			query = window.location.hash + '?view=' + arg2;
		} else {
			query = window.location.hash + '&view=' + arg2;
		}
	} else {
	 	query = window.location.hash.replace(arg1, arg2);
	}
	ZEPHYR.ZQL.router.navigate(query, {trigger: false, replace: true});
	window.viewType = arg2;
}

function scrollToElement(top) {
  AJS.$('.execution-list-wrapper').scrollTop(top);
}

var getCustomFields = function(projectId, completed) {
  jQuery.ajax({
    url: getRestURL() + "/customfield/entity?entityType=TESTSTEP&projectId="+projectId,
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
      getProjectCustomFields(response, projectId, completed);
    }
  });
}

var getProjectCustomFields = function(generalCustomField, projectId, completed) {
  jQuery.ajax({
    url: getRestURL() + "/customfield/byEntityTypeAndProject?entityType=TESTSTEP" + "&projectId=" + projectId,
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
      allCustomFields = generalCustomField.concat(response);
      setCustomFieldsObjects();
	  getExecutionCustomization(completed);
    }
  });
}

var getExecutionCustomization = function(completed) {
  jQuery.ajax({
    url: getRestURL() + "/preference/getexecutioncustomization",
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {

      Object.keys(response.preferences).forEach(function(key) {
        var obj = {
            "displayName": response.preferences[key].displayName,
            "isVisible": response.preferences[key].isVisible
        }
          ZEPHYR.ExecutionCycle.executionStepColumns[key] = obj;
      });
	allCustomFields.forEach(function(field) {
		if (!ZEPHYR.ExecutionCycle.executionStepColumns.hasOwnProperty(field.id)) {
			var obj = {
			"displayName": field.name,
			"isVisible": "false"
			}
			ZEPHYR.ExecutionCycle.executionStepColumns[field.id] = obj;
		}
	});
       if(completed)
        completed.call();
    }
  });
}

var setCustomFieldsObjects = function() {
  ZEPHYR.ExecutionCycle.customFieldsOrder = [];
  allCustomFields.forEach(function(field) {
    var orderObj = {
      "customfieldId": field.id,
      "customFieldType": field.fieldType,
      "customFieldName": field.name,
      "customDefaultValue": field.defaultValue
    }
    ZEPHYR.ExecutionCycle.customFieldsOrder.push(orderObj);
  });
}

var updateExecutionStepColumns = function(preference) {
  for(column in preference) {
  	if(ZEPHYR.ExecutionCycle.executionStepColumns[column]) {
    	ZEPHYR.ExecutionCycle.executionStepColumns[column].value = preference[column];
  	}
  }
}

ZEPHYR.ZQL.ZQLSearch = Backbone.Model.extend();
ZEPHYR.ZQL.ZQLSearchCollection = Backbone.Collection.extend({
    model:ZEPHYR.ZQL.ZQLSearch,
    url: function(){
    	return getRestURL() + "/zql/executeSearch/"
    },
    parse: function(resp, xhr){
    	return resp
    }
});

if(ZEPHYR.ZQL.ZQLFilters && !ZEPHYR.ZQL.ZQLFilters.LOGGED_IN_USER )
    ZEPHYR.ZQL.ZQLFilters.LOGGED_IN_USER = {};
ZEPHYR.ZQL.ZQLFilters = Backbone.Model.extend();
ZEPHYR.ZQL.ZQLFiltersCollection = Backbone.Collection.extend({
    model:ZEPHYR.ZQL.ZQLFilters,
    url: function(){
        var url = getApiBasepath() + "/zql/executionFilter/?byUser=true&fav=true&offset=-1&maxRecords=10";
        //return getRestURLForBlueprints() + "/zql/executionFilter/?byUser=true&fav=true&offset=-1&maxRecords=10"
        return url;
    },
    parse: function(resp, xhr){
    	return resp
    }
});

ZEPHYR.ZQL.ZQLColumn = Backbone.Model.extend();
ZEPHYR.ZQL.ZQLColumnCollection = Backbone.Collection.extend({
    model: ZEPHYR.ZQL.ZQLColumn,
    url: getRestURL() + "/znav/availableColumns/",
    parse: function(resp, xhr) {
    	// resp.customFields.map(function(field) {
		// 		field.id = (field.id).toString() + '_cf';
		// 	});
    	return resp
    }
});

var Zephyr = new function() {
	/**
	 * Refresh Attributes to CSS changes for ascending and descending class
	 */
	this.refreshAttributes = function(activeColPrefix,sortQueryPrefix) {
		var columnPrefixes = new Array("headerrow-id-exec-schedule", "headerrow-id-exec-cycle", "headerrow-id-exec-issue", "headerrow-id-exec-project","headerrow-id-exec-component",
				"headerrow-id-exec-priority","headerrow-id-exec-fixVersion","headerrow-id-exec-status","headerrow-id-exec-executedBy","headerrow-id-exec-executedOn","headerrow-id-exec-createdOn");
		for(var index in columnPrefixes){
			if(columnPrefixes[index] != activeColPrefix) {
				var colIdentifier = "#" + columnPrefixes[index];
				AJS.$(colIdentifier).attr("class","colHeaderLink sortable " + columnPrefixes[index]);
			}
		}
		var activeColumnJQueryElement = AJS.$("#" + activeColPrefix);
		var longStr = this.getSortString(activeColumnJQueryElement,true);
		activeColumnJQueryElement.removeClass();
		activeColumnJQueryElement.addClass("colHeaderLink active sortable " + longStr + " " + activeColPrefix);
        var shortStr = this.getSortString(activeColumnJQueryElement, false);
        activeColumnJQueryElement.attr("rel", sortQueryPrefix + ":"+ shortStr);
	},

	this.getSortString = function(changeKey,longForm) {
		if(changeKey != null && changeKey.attr('rel') != null) {
			var array = changeKey.attr('rel').split(':');
			if(array != null || array.length != 2) {
				 if(array[1] =="ASC") {
					 return longForm ? "ascending" : "DESC"
				 } else {
					 return longForm ? "descending" : "ASC"
				 }
			} else {
				return longForm ? "ascending" : "ASC"
			}
		}
	},


	this.transform=function() {

        //for analytics
        if (za != undefined) {
            za.track({'event':ZephyrEvents.SEARCH_TEST_EXECUTION,
                    'eventType':'Click'
			},
			function (res) {
				console.log('Analytics test: -> ',res);
			});
        }

		scheduleList=[];
		var offset=0;
		window.offset = 1;
		var queryParam = '';
		if (ZEPHYR.ZQL.maintain == true) {
			offset = ZEPHYR.ZQL.prevNextId.offset;
			queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=' + offset : '&view=' + window.viewType;
			queryParam += '&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId;
			var offsetPosition = null;
			var selectedIdPosition = null;
			if (window.location.hash.indexOf('&offset=' + offset) >= 0) {
				offsetPosition = window.location.hash.indexOf('&offset=' + offset);
			}
			if (window.location.hash.indexOf('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId) >= 0) {
				selectedIdPosition = window.location.hash.indexOf('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId);
			}

			if (offsetPosition && selectedIdPosition) {
				queryParam = queryParam.replace('&offset=' + offset, '');
				queryParam = queryParam.replace('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId, '');
				// queryParam = queryParam.slice(offsetPosition, ('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId).length);
				if (offsetPosition > selectedIdPosition) {
					queryParam += '&offset=' + offset + '&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId;
				} else {
					queryParam += '&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId + '&offset=' + offset;
				}
			}

		} else {
			queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=' + offset : '&view=' + window.viewType;
		}
    	ZEPHYR.ZQL.router.navigate("?query=" + encodeURIComponent(AJS.$('#zqltext').val()) + queryParam, {trigger:true})
	};

	this.navigateToFilters = function(filterId) {
		scheduleList=[];
		var offset=0;
		window.offset = 1;
		var queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=0' : '&view=' + window.viewType;
    	ZEPHYR.ZQL.router.navigate("?filter=" + filterId + queryParam, {trigger:true})
	};

	this.selectedSchedules = function() {
		var selectedScheduleItems = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
		AJS.$('#issuetable input[type=checkbox]:checked').each(function() {
			if(AJS.$(this).attr('name') != "selectAllID" && AJS.$.inArray(AJS.$(this).attr('name'), selectedScheduleItems) == -1) {
				ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.push(AJS.$(this).attr('name'));
	        	AJS.$('#'+AJS.$(this).attr('name')).attr('checked',true);
			}
		});
		scheduleList = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
		return ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
	}

	this.unSelectedSchedules = function() {
//		var selectedScheduleItems = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
		// var y = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
		// AJS.$('#issuetable input[type=checkbox]:checked').each(function() {
  //       	AJS.$(this).attr('checked',false);

		// 	var that = this;
		// 	y = jQuery.grep(y, function (value) {
		// 		return value != AJS.$(that)[0].getAttribute('name');
		// 	});
		// });
		scheduleList = [];
		if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0)
    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = scheduleList;
		return scheduleList;
	}

	this.updateSelectedExecutionsUI = function() {
		var selectedExecutions = Zephyr.selectedSchedules();
		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = [];
		// Set the Checkbox to check previous selected executionIds
		if(selectedExecutions != null && selectedExecutions.length > 0) {
	        AJS.$.each(selectedExecutions, function(index, execution) {
	        	ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.push(execution);
	        	AJS.$('#scheduleCheck-' + execution).attr('checked',true);
	        });
		}
	}

	//Listen for click on toggle checkbox
	AJS.$('#selectAllID').live('click',function(event) {
	    if(this.checked) {
	        AJS.$(':checkbox').each(function() {
	        	if(!this.disabled) {
                    this.checked = true;
                }
	        });
	        Zephyr.selectedSchedules();
			AJS.$("#enavBulkToolId").removeClass("disabled");
	    } else {
	        AJS.$(':checkbox').each(function() {
	            this.checked = false;
	    		var y = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
				var removeItem = AJS.$(this).attr('name');
	    		y = jQuery.grep(y, function(value) {
	    			  return value != removeItem;
	    		});
	    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = y;
	    		// Zephyr.unSelectedSchedules();
	    		var y = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
				AJS.$('#issuetable input[type=checkbox]:checked').each(function() {
		        	AJS.$(this).attr('checked',false);

					var that = this;
					y = jQuery.grep(y, function (value) {
						return value != AJS.$(that)[0].getAttribute('name');
					});
				});
				scheduleList = y;
				if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0)
		    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = scheduleList;
				// disable Tools link on unselect
				if (ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length == 0) {
					AJS.$("#enavBulkToolId").addClass("disabled");
					AJS.$("#toolOptions-dropdown").css("display","none");
				}
	        });
	    }
		globalDispatcher.trigger("schedulesCountRefresh",ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds);
	});

	AJS.$('#clearSelectionId').live('click',function(event) {
		event.preventDefault();
		Zephyr.unSelectedSchedules();
		globalDispatcher.trigger("schedulesCountRefresh",ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds);
        AJS.$(':checkbox').each(function() {
            this.checked = false;
    		var y = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
    		var removeItem = AJS.$(this).attr('name');
    		y = jQuery.grep(y, function(value) {
    			  return value != removeItem;
    		});
    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = y;
        });
	});
}

var ZephyrZQLFilters = new function(){
	this.init = function(){
		ZEPHYR.ZQL.ZQLFilters.data.searchResults = new ZEPHYR.ZQL.ZQLFiltersCollection()
		ZEPHYR.ZQL.ZQLFilters.data.searchResults.fetch({contentType:'application/json',success:function(){
			if(window.zqlFiltersView == null) {
				var zqlPredefinedFiltersView  = new ZEPHYR.ZQL.ZQLFilters.PredefinedSearchFilterView();
    			window.zqlFiltersView = new ZEPHYR.ZQL.ZQLFilters.SearchView({model:ZEPHYR.ZQL.ZQLFilters.data.searchResults});
        		AJS.$('#contentRelatedId').append(window.zqlFiltersView.render().el);
                JIRA.Dropdowns.bindNavigatorOptionsDds();
    		}
		}, error: function(response,jqXHR) {
        		showZQLError(jqXHR);
            }
		});
	}
	/*idempotent method: gets called multiple times. Init models*/
	this.findFiltersByUser = function(){
		//AJS.$('#executionFilterId').html("");
        ZEPHYR.ZQL.ZQLFilters.data.searchResults.fetch({contentType: 'application/json', reset: true})
	},
	this.createZQLFilterPicker = function(){
		AJS.$(document.body).find('.aui-field-filterpicker').each(function () {
	        new AJS.SingleSelect({
		           element: AJS.$("#searchZQLFilterName"),
	           itemAttrDisplayed: "label",
	           maxInlineResultsDisplayed: 15,
	           maxWidth:200,
	           showDropdownButton: false,
	           submitInputVal: true,
	           overlabel: AJS.I18n.getText("zql.filter.quick.srh.label"),
               errorMessage: AJS.I18n.getText("zql.filter.quick.srh.no.matching.filters"),
	           ajaxOptions: {
	        	   url:getRestURL() + "/zql/executionFilter/quickSearch",
	        	   query: true,
	        	   minQueryLength: 2,
	        	   formatResponse: function (response) {
	        		   var ret = [];
	        		   AJS.$(response).each(function() {
	        			   	// Setting the height of '40%' for the Search Execution Filter drop down.
	        			    AJS.$(document.body).find('div.ajs-layer.box-shadow').css('height', '40%');
	    	                var groupDescriptor = new AJS.GroupDescriptor();
	    	                groupDescriptor.addItem(new AJS.ItemDescriptor({
	   	                        value: this.filterName, // value of
	   	                        label: this.filterName, // title
	   	                        title: this.query + '&id=' + this.id,
	   	                        html: htmlEncode(this.filterName),
	   	                        highlighted: true
	    	                }));
	    	                ret.push(groupDescriptor);
	    	            });
	    	            return ret;
	        	   }
	           }
	        });
		});
	},

	this.toggleView = function() {
		// Toggle between detail and list view
		AJS.$('.layout-switcher-item a').click(function(ev) {
			ev.preventDefault();
			var currentTarget 	= ev.currentTarget || ev.srcElement,
				viewType 		= AJS.$(currentTarget).attr('id').split('-')[0],
				queryString;

			var queryParam = ZephyrZQLFilters.getViewQueryParamFromURL();

			// Set queryString based on the view option (detail/list)
			if(viewType == 'split'){
				queryString = queryParam + 'view=detail&offset=' + ZEPHYR.ZQL.pagination.offset;
				AJS.$('#split-view').addClass('active-view');
        hideListView = true;
    		AJS.$('#list-view').removeClass('active-view');
			}
			else if(viewType == 'list'){
				queryString = queryParam + 'view=list&offset=' + ZEPHYR.ZQL.pagination.offset;
    			AJS.$('#split-view').removeClass('active-view');
    			AJS.$('#list-view').addClass('active-view');
			}

			ZEPHYR.ZQL.router.navigate('?' + queryString, {trigger: true});
		});
	},

	// Get the query param from URL
	this.getViewQueryParamFromURL = function() {
		var queryParam 		= '';
		var zqlQuery 		= ZephyrURL('?query');
		var zqlFilter		= ZephyrURL('?filter');

		if(zqlQuery != null || zqlQuery != undefined) queryParam = 'query=' + encodeURIComponent(decodeURIComponent(zqlQuery)) + '&';
		else if(zqlFilter) queryParam = 'filter=' + zqlFilter + '&';

		return queryParam;
	},

	// return filterId
	this.getFilterId = function() {
		var filterId = ZEPHYR.ZQL.ZQLSearch.data.filterId || '';

		return filterId;
	}

	// set filterId
	this.setFilterId = function(filterId) {
		ZEPHYR.ZQL.ZQLSearch.data.filterId = filterId;
	}

	// set filterName
	this.setFilterName = function(filterName) {
		ZEPHYR.ZQL.ZQLSearch.data.filterName = filterName;
	}

	// set zql
	this.setZQL = function(zql) {
		ZEPHYR.ZQL.ZQLSearch.data.zql = zql;
	}

	this.updateDocumentTitle = function() {
		var title = AJS.$('title').length > 0 ? AJS.$('title')[0].text : '';
		// Note: AJS.$('title').html(title); will throw error in IE8 so using document.title to set the title.
		// If standalone display, 'Execute Test - $projectKey' as title else 'Execution Navigator - $projectKey'.
		if(window.standalone) {
			if(AJS.$('title:contains("' + AJS.I18n.getText("issue.schedule.execute.title") + '")').length == 0)
				document.title = title.replace(AJS.I18n.getText("enav.page.heading"), AJS.I18n.getText("issue.schedule.execute.title"));
		} else {
			if(AJS.$('title:contains("' + AJS.I18n.getText("enav.page.heading") + '")').length == 0)
				document.title = title.replace(AJS.I18n.getText("issue.schedule.execute.title"), AJS.I18n.getText("enav.page.heading"));
		}
		this.setPermissionMessageClass();
	}

	this.returnDefaultColumns = function() {
		var columnItemBean = [{"filterIdentifier": AJS.I18n.getText("enav.newcycle.name.label"), "orderId":0,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("cycle.reorder.executions.issue.label"), "orderId":1,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("execute.test.testsummary.label"), "orderId":2,"visible":true},
							  {"filterIdentifier": AJS.I18n.getText("issue.field.labels"), "orderId":3,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("enav.projectname.label"), "orderId":4,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("project.cycle.addTests.priority.label"), "orderId":5,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("je.gadget.common.component.label"), "orderId":6,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("je.gadget.common.version.label"), "orderId":7,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("execute.test.executionstatus.label"), "orderId":8,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("project.cycle.schedule.table.column.executedBy"), "orderId":9,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("project.cycle.schedule.table.column.executedOn"), "orderId":10,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("plugin.license.storage.admin.license.attribute.creationdate.title"), "orderId":11,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("enav.search.execution.defects"), "orderId":12,"visible":true},
                              {"filterIdentifier": AJS.I18n.getText("enav.newfolder.name.label"), "orderId":13,"visible":true},
							  {"filterIdentifier": AJS.I18n.getText("execute.test.workflow.estimated.time.label"), "orderId":14,"visible":false},
            				  {"filterIdentifier": AJS.I18n.getText("execute.test.workflow.logged.time.label"), "orderId":15,"visible":false}];
		return columnItemBean;
	},

    this.COLUMN_NAME_I18N_MAPPING = {"Cycle Name": AJS.I18n.getText("enav.newcycle.name.label"),
                                "Issue Key": AJS.I18n.getText("cycle.reorder.executions.issue.label"),
                                "Test Summary": AJS.I18n.getText("execute.test.testsummary.label"),
                                "Labels": AJS.I18n.getText("issue.field.labels"),
                                "Project Name": AJS.I18n.getText("enav.projectname.label"),
                                "Priority": AJS.I18n.getText("project.cycle.addTests.priority.label"),
                                "Component": AJS.I18n.getText("je.gadget.common.component.label"),
                                "Version": AJS.I18n.getText("je.gadget.common.version.label"),
                                "Execution Status": AJS.I18n.getText("execute.test.executionstatus.label"),
                                "Executed By": AJS.I18n.getText("project.cycle.schedule.table.column.executedBy"),
                                "Executed On": AJS.I18n.getText("project.cycle.schedule.table.column.executedOn"),
                                "Creation Date": AJS.I18n.getText("plugin.license.storage.admin.license.attribute.creationdate.title"),
                                "Execution Defect(s)": AJS.I18n.getText("enav.search.execution.defects"),
                                "Folder Name": AJS.I18n.getText("enav.newfolder.name.label"),
								"Estimated Time": AJS.I18n.getText("execute.test.workflow.estimated.time.label"),
								"Logged Time": AJS.I18n.getText("execute.test.workflow.logged.time.label")
                            },


    this.showZQLErrorOnNavigate = function(title, message) {
    	AJS.$('#content').html(ZEPHYR.ZQL.Search.enavStandaloneError({
			errorTitle:		title,
			errorBody:		message,
			contextPath:	contextPath
		})).show();
    	AJS.$('#footer').show();
    },

    this.updateFilterNameOnHeader = function(filterName) {
    	if(!filterName) {
    		filterName = AJS.I18n.getText("zql.search.label");
    	}
		AJS.$('#zqlHeader .search-title').html(htmlEncode(filterName)).attr('title', filterName);
    },
    this.setPermissionMessageClass = function() {
    	AJS.$('.zfj-permissions-active')
    		.removeClass('active')
    		.empty(); // Empty the permissions error message
    	if(window.viewType == 'detail') {
    		AJS.$('#zfj-permission-message-bar-execution-detail').addClass('active');
    	} else if(window.viewType == 'list') {
    		AJS.$('#zfj-permission-message-bar-execution-list').addClass('active');
    	}
    }
}

/*
 * View to handle dock/undock of filters sidebar in execution navigator
 * The sidebar can be in either of the three states, docked, undocked and detached [on hover of undocked sidebar].
 * TODO: Add animation on hide/show
 */
ZEPHYR.ZQL.ZQLFilters.SidebarPopoutView = Backbone.View.extend({
	el: '#contentRelatedId',

	initialize: function() {
		_.bindAll(this, 'undockSidebar', 'dockSidebar', 'openSidebarOnHover',
				'hideDetachedSidebar', 'updateOffsets', 'preventUndockSearchZQLFilter', 'allowUndockSearchZQLFilter',
				'allowUndockFavouriteFilters', 'allowDockFavouriteFilters');
		// On detached on click of 'Filters', currently on in use since we provided the mouseover option
		AJS.$('.zfj-sidebar-dock-button').bind('click', this.dockSidebar);
		this.$el.find('.zfj-sidebar-undock').bind('click', this.undockSidebar);
		AJS.$('#zfj-sidebar-menu-column').bind('mouseover', this.openSidebarOnHover);
		// Search Favourite filters
		AJS.$('#searchZQLFilterName-field, #searchZQLFilterName').bind('focusin', this.preventUndockSearchZQLFilter);
		AJS.$('#contentRelatedId').bind('mouseleave', this.allowUndockSearchZQLFilter);
		AJS.$('#zql-quick-srh-id').bind('click', this.allowUndockSearchZQLFilter);
		// Favourite Filters
		AJS.$('#contentRelatedId.zfj-ui-popout-detached a.filter-actions').live('click', this.allowUndockFavouriteFilters);
		AJS.$('.filter-operations .aui-list-item-link').live('click', this.allowDockFavouriteFilters);

		this.render();
		this.isDetached = false;
		this.isFilterKeyPress = false;
		this.isFilterDropDown = false;
	},

	preventUndockSearchZQLFilter: function(ev) {
		this.isFilterKeyPress = true;
	},

	allowDockFavouriteFilters: function(ev) {
		if(this.isFilterDropDown) {
			this.isFilterDropDown = false;
			this.hideSidebarOnMouseOut();
		}
	},

	allowUndockFavouriteFilters: function(ev) {
		if(ev.target && (AJS.$(ev.target).hasClass("filter-actions") || AJS.$(ev.target).parent().hasClass("filter-actions"))) {
			this.isFilterDropDown = true;
			globalDispatcher.on("zqlFilterDropDownHidden", this.allowDockFavouriteFilters);
		} else this.isFilterDropDown = false;
	},

	allowUndockSearchZQLFilter: function(ev) {
		if(ev.target && ev.target.id && ev.target.id == 'searchZQLFilterName-field')
			this.isFilterKeyPress = true;
		else this.isFilterKeyPress = false;
	},

	// Dock/undock the filters sidebar based on the previously stored localstorage value
	render: function() {
		var isDocked = this.isSidebarDocked();
		if(isDocked == true || isDocked == 'true') {
			AJS.$('#zfj-sidebar-menu-column').hide();
		} else if(isDocked == false || isDocked == 'false') {
			AJS.$('#contentRelatedId').hide();
			AJS.$('#zfj-sidebar-menu-column').show();
		}
		this.resizeDetailViewPanel();
	},

	// Whenever the sidebar is docked/undocked resize the detail view panel
	resizeDetailViewPanel: function() {
		// var width = AJS.$('#zqlResponse').parentsUntil('.navigator-group').width() - AJS.$('#list-results-panel').width();
  //   	AJS.$('#detail-panel-wrapper').width(width - 6);
	},

	// Updates the position and height of the sidebar.
    updateOffsets: function (ev) {
    	// Only if the sidebar is detached
        if (this.isDetached) {
            var placeholderTop 	= AJS.$('#zfj-sidebar-menu-column').offset().top;
            var scrollTop 		= jQuery(window).scrollTop();
            var offset			= Math.max(placeholderTop - scrollTop, 0);
            var height			= AJS.$(window).height();
            var props 			= {top: offset, height: height - offset};

            AJS.$('.zfj-ui-popout-detached').css(props);
            // Add the scrollbar for the Favourite Filters
            var filterPosition = AJS.$('#executionFilterId').position();
            if(filterPosition) {
                var filterHeight = height - filterPosition.top;
                if(filterHeight > 10)
                	AJS.$('#executionFilterId').css({height: (filterHeight - 20), overflow: 'auto'});
                else
                	AJS.$('#executionFilterId').removeAttr('style');
            }
        }
    },

	isSidebarDocked: function() {
		var isDocked = localStorage.getItem('executions.sidebar.docked');
		if(isDocked == null || isDocked == undefined || isDocked == '') {
			localStorage.setItem('executions.sidebar.docked', true);
			isDocked = true;
		}
		return isDocked;
	},

	undockSidebar: function(ev) {
		ev.preventDefault();
		AJS.$('#zfj-sidebar-menu-column').show();
		AJS.$('#contentRelatedId').hide();
		localStorage['executions.sidebar.docked'] = false;
		this.resizeDetailViewPanel();
	},

	dockSidebar: function(ev) {
		ev.preventDefault();
		AJS.$('#contentRelatedId').show();
		AJS.$('#zfj-sidebar-menu-column').hide();
		localStorage['executions.sidebar.docked'] = true;
		this.resizeDetailViewPanel();
	},

	// On hover of the undocked sidebar, display the detached filter sidebar
	openSidebarOnHover: function(ev) {
		ev.preventDefault();
		if(this.isDetached) return;
		this.isDetached = true;
		//AJS.$('#zfj-sidebar-menu-column').hide();

		AJS.$('.zfj-sidebar-dock').show();
		AJS.$('.zfj-sidebar-undock').hide();
		AJS.$('#contentRelatedId')
			.addClass('zfj-ui-popout-detached')
			.show()
			.addClass('zfj-ui-popout-expanded');

		AJS.$('.zfj-sidebar-dock').bind('click', this.hideDetachedSidebar);
		AJS.$('.zfj-ui-popout-detached').bind('mouseleave', this.hideDetachedSidebar);

		this.resizeDetailViewPanel();
		AJS.$(window).scroll(this.updateOffsets);
		AJS.$(window).trigger('scroll');
	},

	// Hide the detached slidebar, resize the detail view if present and unbind the scroll event.
	hideDetachedSidebar: function(ev) {
		ev.preventDefault();
		ev.stopImmediatePropagation();

		if((this.isFilterKeyPress || this.isFilterDropDown) && (AJS.$(ev.target).hasClass('zfj-sidebar-dock') || AJS.$(ev.target).parent().hasClass('zfj-sidebar-dock'))) {
			this.isFilterDropDown = false;
			this.isFilterKeyPress = false;
			AJS.$('.filter-operations').parent().remove();
			AJS.$('#executionFilterId').find('.filter-actions').removeClass('active');
			globalDispatcher.off('zqlFilterDropDownHidden');
		}
		this.isDetached = false;
		if(this.isFilterKeyPress || this.isFilterDropDown) return;

		AJS.$('.zfj-sidebar-undock').show();
		AJS.$('#zfj-sidebar-menu-column').hide();
		AJS.$('.zfj-sidebar-dock').unbind('click', this.hideDetachedSidebar);
		AJS.$('.zfj-ui-popout-detached').unbind('mouseleave', this.hideDetachedSidebar);

		// If the user clicked on the 'dock' [>>] button then dock the detached sidebar else hide it.
		if(ev.type == 'click') this.dockSidebarWhenDetached();
		else this.hideSidebarOnMouseOut();
		AJS.$('.zfj-sidebar-dock').hide();

		this.resizeDetailViewPanel();
		AJS.$(window).unbind("scroll", this.updateOffsets);
	},

	hideSidebarOnMouseOut: function() {
		// Order is important for IE10: ZFJ-1151.
		AJS.$('#zfj-sidebar-menu-column').show();
		AJS.$('#contentRelatedId')
			.removeClass('zfj-ui-popout-detached zfj-ui-popout-expanded')
			.hide();
		return;
	},

	dockSidebarWhenDetached: function() {
		AJS.$('#contentRelatedId')
			.removeClass('zfj-ui-popout-detached zfj-ui-popout-expanded');
		localStorage['executions.sidebar.docked'] = true;
		// Remove the scrollbar if present on 'Favourite Filters'
		AJS.$('#executionFilterId').removeAttr('style');
	}
});

ZEPHYR.ZQL.ZQLFilters.PredefinedSearchFilterView = Backbone.View.extend({
	el: '#predefinedFilters',
	events:{
    	"click .filter-link"	: "execFilter"
	},

    initialize: function () {
        this.template = function(){return (AJS.$('#predefinedFilters').html())};
    },
	render: function () {
		this.$el.html(this.template()); // this.$el is a jQuery wrapped el var
        return this;
	},
	execFilter : function(event){
		event.preventDefault();
    	var filterParam = AJS.$(event.currentTarget).attr("href");
    	Zephyr.unSelectedSchedules();
    	if(null != filterParam && filterParam.length > 0){
    		window.offset = 1;
    		var queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=' + window.offset : '&view=' + window.viewType;
    		ZEPHYR.ZQL.router.navigate(filterParam + queryParam, {trigger:true});
    	}
    }
});

ZEPHYR.ZQL.ZQLFilters.SearchView = Backbone.View.extend({
    tagName: 'ul',
    className: 'saved-filter filter-list favourite-filters',
    id:'executionFilterId',
    events:{
    	"click [id^='nav-area-zql-filter-']"	: "execFilter",
        "click .filter-actions": "showFilterActionsDropdown"
    },
    initialize:function () {
    	this.model.bind("change", this.render, this);
        this.model.bind("reset", this.render, this, this.execFilter, this);
    },
    render:function (eventName) {
    	// html template is ZEPHYR.ZQL.Search.showSavedFilters
    	AJS.$(this.el).html(ZEPHYR.ZQL.Search.showSavedFilters({filters:this.model.toJSON()}));
        var instance = this;
    	return this;
    },
    execFilter : function(event){
    	var filterParam = AJS.$(event.currentTarget).attr("href");
    	event.preventDefault();
    	Zephyr.unSelectedSchedules();
    	if(null != filterParam && filterParam.length > 0){
    		window.offset = 1;
    		var queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=' + window.offset : '&view=' + window.viewType;
    		ZEPHYR.ZQL.router.navigate(filterParam + queryParam, {trigger:true})
    	}

    },
    showFilterActionsDropdown: function(e) {
    	var filterDDOptions = {
            trigger: AJS.$(e.currentTarget),
            content: ZEPHYR.ZQL.Search.showFilterAction({filterId: e.currentTarget.id, filterName: AJS.$(e.currentTarget).text(), loggedInUser: ZEPHYR.ZQL.ZQLFilters.LOGGED_IN_USER, createdBy: AJS.$(e.currentTarget).data("owner")}),
            alignment: AJS.LEFT
        };
    	// JIRA 5.2 does not have WindowPositioning method
    	if(typeof AJS.InlineLayer.WindowPositioning != 'undefined') {
    		filterDDOptions['positioningController'] = new AJS.InlineLayer.WindowPositioning()			// Dynamically position the dropdown
    	}
    	var dropDown = new AJS.Dropdown(filterDDOptions);
		globalDispatcher.off('zqlFilterDropDownHidden');
    	dropDown.show();
    	AJS.$(dropDown).bind('hideLayer', function(){
    		AJS.$(e.currentTarget).next('.ajs-layer-placeholder').remove();
    		globalDispatcher.trigger('zqlFilterDropDownHidden');
    		// Unbind previously attached events
    		AJS.$(e.currentTarget).unbind('click');
    	});
    	e.preventDefault();
    }
});

ZEPHYR.ZQL.ZQLColumn.ListColumnInlineDialogView = Backbone.View.extend({
	events: {
		'keyup #user-column-sparkler-input'					: 'populateAutocomplete',
		'click li.check-list-item input[type=checkbox]'		: 'updateUIColumnStatus',
		'click #column-dialog-close'						: 'triggerCancelColumnSelector',
		'click #column-dialog-save'							: 'triggerSaveColumnSelector',
		'click #columns-restore-defaults'					: 'restoreDefaultColumns'
	},

	initialize: function() {
		this.model.bind("reset", this.render, this);
		this.zqlColumns = new ZEPHYR.ZQL.ZQLColumnCollection();
		this.zqlColumns.comparator = function(column) {
			return column.get('orderId');
		}
		this.autocompleteColumns = false;
	},

	triggerCancelColumnSelector: function(ev) {
		ev.preventDefault();
		this.options.dispatcher.trigger("hideColumnSelectorDialog");
	},

	triggerSaveColumnSelector: function(ev) {
		ev.preventDefault();
		this.options.dispatcher.trigger("saveColumnSelectorDialog");
	},

	restoreDefaultColumns: function(ev) {
		ev.preventDefault();
		var instance =  this;

		if(ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.id) {
			this.model.models[0].attributes.columnItemBean = ZephyrZQLFilters.returnDefaultColumns();
			this.options.dispatcher.trigger("saveColumnSelectorDialog");
		} else this.options.dispatcher.trigger("hideColumnSelectorDialog");
	},

	getSelectedColumns: function(columnLabel, count, selectType, visibility) {
		_.each(AJS.$('#column-items-' + selectType).find('li'), function(zqlColumn) {
			var columnName = AJS.$(zqlColumn).find('label').text();
			var column = this.zqlColumns.where({'filterIdentifier': columnName})[0];
			column.set('orderId', count);
			if(column.get('filterIdentifier') == columnLabel) column.set('visible', visibility);
			count++;
		}, this);
		return count;
	},

	getSelectedColumnsOnFilter: function(columnLabel, count, columnOrder) {
		_.each(this.zqlColumns.models, function(zqlColumn) {
			if(zqlColumn.get('filterIdentifier') == columnLabel) {
        zqlColumn.set({'orderId': columnOrder, 'visible': !zqlColumn.get('visible')});
      }
			else {
				zqlColumn.set('orderId', count);
				count++;
			}
		}, this);
		return count;
	},

	updateColumnStatus: function(columnLabel, countChanger) {
		//this.zqlColumns.reset(this.model.models[0].attributes.columnItemBean.concat(this.model.models[0].attributes.customFields));
	var selectedColumns = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean.filter(function(column){return column.visible}).length;
	if (countChanger) {
		countChanger.decrease ? selectedColumns-- : countChanger.increase ? selectedColumns++ : selectedColumns = selectedColumns
	}
    this.zqlColumns.reset(this.model.models[0].attributes.columnItemBean);

    var count = 0;

		if(!this.autocompleteColumns) {
			count = this.getSelectedColumns(columnLabel, count, 'selected', true);
			count = this.getSelectedColumns(columnLabel, count, 'unselected', false);
		} else {
			var parentEl = AJS.$(document.querySelectorAll('input[name="' + columnLabel + '"]')[0]).closest('ul');
			var selectType = AJS.$(parentEl).attr('id').split('-')[2];
			if (selectType == 'selected'){
		        //var columnOrder = (AJS.$(parentEl).find('li').length - 1);
		        var columnOrder = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean.filter(function(column) {return column.orderId}).length;
		            columnOrder += 1;
		        //console.log('data values', ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean, columnOrder);
		    } else if (selectType == 'unselected') {
        		var columnOrder = (this.zqlColumns.length - 1);
      		}
      //console.log('&&&', columnLabel, count, columnOrder);
			count = this.getSelectedColumnsOnFilter(columnLabel, count, columnOrder);
		}

		//var unSelectedColumnsCount = AJS.$(this.el).find('#column-items-selected li').length;
    //var unSelectedColumnsCount = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean.filter(function(column) {return column.orderId}).length;
		this.toggleColumnSort(selectedColumns, true);

		// Sort the columns based on the orderId
		this.zqlColumns.sort();
		var updatedColumns = this.zqlColumns.toJSON();
    this.model.models[0].attributes.columnItemBean = [];
    // this.model.models[0].attributes.customFields = [];
    var that = this;
    updatedColumns.forEach(function(column) {
      if(column.isCustomField) {
        that.model.models[0].attributes.customFields.push(column)
      }
      else {
        that.model.models[0].attributes.columnItemBean.push(column)
      }
    })
		zqlResultView.render();
		Zephyr.updateSelectedExecutionsUI();
	},

	toggleColumnSort: function(columnCount, isUpdate) {
		if(columnCount < 3) {
			AJS.$(this.el).find("#column-items-selected").sortable({ connectWith: "" });
			AJS.$(this.el).find("#column-items-selected .check-list-item input[type=checkbox]").prop("disabled", true);
		} else {
			if (isUpdate) {
				AJS.$(this.el).find("#column-items-selected").sortable({ connectWith: "#column-items-unselected" });
			}
			AJS.$(this.el).find("#column-items-selected .check-list-item input[type=checkbox]").prop("disabled", false);
		}
	},

	// Swap the columns between checked and unchecked section onClick of checkbox
	updateUIColumnStatus: function(ev) {
		var parentEl 	= AJS.$(ev.target).closest('ul'),
			selectType	= AJS.$(parentEl).attr('id').split('-')[2],
			targetEl 	= AJS.$(ev.target).closest('li'),
			columnLabel	= AJS.$(ev.target).closest('li').find('label').text();

		AJS.$(ev.target).closest('li').remove();
		var countChanger = {};
		if(selectType == 'selected') {
			AJS.$('#column-items-unselected').append(targetEl);
			countChanger['decrease']  = true;
		} else if(selectType == 'unselected') {
			AJS.$('#column-items-selected').append(targetEl);
			countChanger['increase']  = true;
		}
		this.updateColumnStatus(columnLabel,countChanger );
	},

	// Populate columns based on the search box item
	populateAutocomplete: function(ev) {
		var input = AJS.$('#user-column-sparkler-input').val();

		if(input) {
			var columnLabels = this.filteredColumnLabels(input);
      //console.log('columnLabels', columnLabels, ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean);
			AJS.$('#column-suggestions-all').html(ZEPHYR.ZQL.Search.addListColumnInlineDialogColumns({
				columns: 			columnLabels,
				isFilterColumns: 	true
			}));
			this.autocompleteColumns = true;
		} else {
			AJS.$('#column-suggestions-all').html(ZEPHYR.ZQL.Search.addListColumnInlineDialogColumns({
				columns: ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean,
				isFilterColumns: 	false
			}));
			this.autocompleteColumns = false;
			this.triggerSortableUI();
		}
	},

	filteredColumnLabels: function(input) {
		var reg = new RegExp(input.split('').join('\\w*').replace(/\W/, ""), 'i');
		var columnLabels = (ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean).filter(function(column) {
		    if (column.filterIdentifier.match(reg)) {
		    	return true;
		    }
		});
		return columnLabels;
	},

	// Make the column selectors sortable between selected and unselected section.
	triggerSortableUI: function() {
		var instance = this;

		AJS.$(this.el).find('#column-items-selected').sortable({
		      placeholder: 	"ui-state-highlight",
		      connectWith: 	"#column-items-unselected",
		      dropOnEmpty: 	true,
		      handle: 		".column-draghandler",
		      stop: function(ev, ui) {
		    	  var selectType = ui.item.closest('ul').attr('id').split('-')[2];
		    	  var columnLabel = ui.item.find('label').text();
		    	  var columnChanger = {};
		    	  if(selectType == 'unselected') {
		    		  ui.item.find('input[type=checkbox]').prop('checked', false);
		    		  columnChanger['decrease'] = true;
		    	  }
	    		  instance.updateColumnStatus(columnLabel,columnChanger);
		      }
	    });

		AJS.$(this.el).find('#column-items-unselected').sortable({
		      placeholder: 	"ui-state-highlight",
		      connectWith: 	"#column-items-selected",
		      dropOnEmpty: 	true,
		      handle: 		".column-draghandler",
		      stop: function(ev, ui) {
		    	  var selectType = ui.item.closest('ul').attr('id').split('-')[2];
		    	  var columnLabel = ui.item.find('label').text();
		    	  var columnChanger = {};
		    	  if(selectType == 'selected') {
		    		  ui.item.find('input[type=checkbox]').prop('checked', true);
		    		  columnChanger['increase'] = true;
		    	  }
	    		  instance.updateColumnStatus(columnLabel,columnChanger);
		      }
	    });
		AJS.$(this.el).find('#column-items-selected, #column-items-unselected').disableSelection();
		//var unSelectedColumnsCount = AJS.$(this.el).find('#column-items-selected li').length;
        //var unSelectedColumnsCount = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean.filter(function(column){return column.orderId}).length;
        var selectedColumns = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean.filter(function(column){return column.visible}).length;
		this.toggleColumnSort(selectedColumns, false);
	},

	render: function() {

		AJS.$(this.el).html(ZEPHYR.ZQL.Search.addListColumnInlineDialog({
			columns: ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean
		}));
		this.triggerSortableUI();

		return this;
	}
});

/*
 * ListColumnSelectorView handle rendering the column selectors
 */
ZEPHYR.ZQL.ZQLColumn.ListColumnSelectorView = Backbone.View.extend({
  template: _.template(ZEPHYR.ZQL.Search.addListColumnSelector()),

  events: {
		'click #column-selector'					: 'getColumnSelectorValues'
	},

	initialize: function() {
		this.isCancelEvent = false;
		this.options.dispatcher.on('hideColumnSelectorDialog', this.hideColumnSelectorView, this);
		this.options.dispatcher.on('saveColumnSelectorDialog', function() {
			window.inlineColumnDialog.hide();
		}, this);
		this.options.dispatcher.on('saveColumnSelector', this.saveColumnSelector, this);
	},

	saveColumnSelector: function() {
		var zqlColumnId = ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.id, restURL, restType;
    var data = JSON.parse(JSON.stringify(ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes));
    // data.customFields.forEach(function(column) {
    //   if(column.isCustomField) {
    //     delete column.isCustomField
    //   }

    //   if(column.orderId) {
    //     delete column.orderId
    //   }
    // })
		ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].set('executionFilterId', AJS.$('#zqltext').attr('filterId'));
		if(zqlColumnId) {
			restType = 'PUT';
			restURL = '/znav/updateColumnSelection/' + zqlColumnId;
		} else {
			restType = 'POST';
			restURL =  '/znav/createColumnSelection';
		}
		// data.customFields.map(function(field) {
		// 	field.id = field.id.replace("_cf", "");
		// 	field.id = Number(field.id);
		// });
		jQuery.ajax({
			url: getRestURL() +  restURL,
			type: restType,
			contentType:"application/json",
			dataType: "json",
			data: JSON.stringify(data),
			success: function(response) {
				// response.customFields.map(function(field) {
				// 	field.id = (field.id).toString() + '_cf';
				// });
				ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.reset(response);
				zqlResultView.render();
				Zephyr.updateSelectedExecutionsUI();
			},
			error: function(response) {
				showZQLError(response);
			}
		});
	},

	getColumnSelectorValues: function(ev) {
		ev.preventDefault();
		if(this.isTiggered) this.displayColumnDialog();
		window.inlineColumnDialog.show();
	},

	// Hide column selector inline dialog view
	hideColumnSelectorView: function() {
		this.isCancelEvent = true;
		window.inlineColumnDialog.hide();
		// TODO: Retrieve previous values instead of fetching from db
		ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.fetch({
			success: function() {
				zqlResultView.render();
				Zephyr.updateSelectedExecutionsUI();
			},
			error: function(response,jqXHR) {
	    		showZQLError(jqXHR);
			}
		});
	},

	// Attach the inline column dialog
	displayColumnDialog: function() {
		var instance = this;
		window.inlineColumnDialog = AJS.InlineDialog(AJS.$("#column-selector"), "column-selector",
		    function(content, trigger, showPopup) {
				window.listColumDialogView = new ZEPHYR.ZQL.ZQLColumn.ListColumnInlineDialogView({model: ZEPHYR.ZQL.ZQLColumn.data.zqlColumns, dispatcher: globalDispatcher});
				window.listColumDialogView.delegateEvents();
				content.css({"padding":"20px"}).html(window.listColumDialogView.render().el);
		        showPopup();
		        content.click(function(ev) {
		        	ev.stopImmediatePropagation();
		        });
		        // Lock the window scroll
		        // ZEPHYR.scrollLock('#column-suggestions-all');
		        return false;
		    }, {
		    	container: '#column-selector-inline-wrapper',
		    	hideDelay:  null,
		    	hideCallback: function() {
		    		if(!instance.isCancelEvent) instance.saveColumnSelector();
		    		else instance.isCancelEvent = false;
		    	},
		    	noBind: true,
          calculatePositions: function getPosition(popup, targetPosition, mousePosition, opts) {
            return {
              displayAbove: false,
              popupCss: {
                left: mousePosition.x + AJS.$("#column-selector").width() - 200 - 340,
                top: 75,
                right: mousePosition.y + 100
              },
              arrowCss: { left: 300, top: -7, right: 20 }
            };
          }
		    }
		);
		this.isTiggered = false;
	},

	render: function() {
		this.isTiggered = true;
    AJS.$(this.el).empty();
		AJS.$(this.el).append(this.template);
    this.delegateEvents();
		return this;
	}
});

//handles rename ZQL filter event
AJS.$("#[id^='zfj-filter-rename-id-']").live("click", function(e) {
	e.preventDefault();
	var filterName = AJS.$(e.currentTarget).data("value");
	var filterId = AJS.$(e.currentTarget).data("id");
	var dialog = new JIRA.FormDialog({
        id: "rename-zql-filter-id",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
			var htmlTemplate = AJS.$(ZEPHYR.ZQLFilter.Save.renameZQLFilter({filterName:AJS.$(e.currentTarget).data("value"), dlgTitle: AJS.I18n.getText('zql.filter.rename.dialog.title')}));
            callback(htmlTemplate);
        },
        submitHandler: function (e) {
        	parent.zqlHeaderView.renameZql(function() {
        		dialog.hide();
            }, filterId);
            e.preventDefault();
        }
    });
    dialog.show();
});

//handles remove ZQL filter event
AJS.$("#[id^='zfj-filter-delete-id-']").live("click", function(e) {
	e.preventDefault();
	var filterId = AJS.$(e.currentTarget).data("id");
    var filterName = AJS.$(e.currentTarget).data("value");
    var dialog = new JIRA.FormDialog({
        id: "remove-zql-filter-id",
        content: function (callback) {
            /*Short cut of creating view, move it to Backbone View and do it in render() */
            var htmlTemplate = AJS.$(ZEPHYR.ZQLFilter.Save.deleteZQLFilterConfirmationDialog({filterName:filterName,
                dlgTitle: AJS.I18n.getText('zql.filter.delete.dialog.title')}));
            callback(htmlTemplate);
        },
        submitHandler: function (e) {
            parent.zqlHeaderView.removeZql(function() {
                dialog.hide();
            }, filterId);
            e.preventDefault();
        }
    });
    dialog.show();
    //parent.zqlHeaderView.removeZql(filterId);
});

//handles marking ZQL filter unfavorite event
AJS.$("#[id^='zfj-filter-unfavorite-id-']").live("click", function(e) {
	e.preventDefault();
	var filterId = AJS.$(e.currentTarget).data("id");
	parent.zqlHeaderView.markZqlUnfavorite(filterId);
});

//handles COPY ZQL filter event
AJS.$("#[id^='zfj-filter-copy-id-']").live("click", function(e) {
	e.preventDefault();
	var filterName = AJS.$(e.currentTarget).data("value");
	var filterId = AJS.$(e.currentTarget).data("id");
	var dialog = new JIRA.FormDialog({
        id: "rename-zql-filter-id",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
			var htmlTemplate = AJS.$(ZEPHYR.ZQLFilter.Save.renameZQLFilter({filterName:AJS.I18n.getText('zql.filter.copy.field.label')+" "+AJS.$(e.currentTarget).data("value"),
				dlgTitle: AJS.I18n.getText('zql.filter.copy.dialog.title')}));
            callback(htmlTemplate);
        },
        submitHandler: function (e) {
        	parent.zqlHeaderView.copyZql(function() {
        		dialog.hide();
            }, filterId);
            e.preventDefault();
        }
    });
    dialog.show();
});

//Update Filter
//handles remove ZQL filter event
AJS.$("#[id^='zephyr-zql-update-filter']").live("click", function(e) {
	e.preventDefault();
	var filterId = AJS.$("#zqltext").attr('filterid');
	if(filterId != "" && AJS.$("#zqltext").val() != AJS.$('#zqltext').attr('data-query')) {
		parent.zqlHeaderView.updateZql(filterId);
	}
});

//Executes ZQL from Quick search result
AJS.$("#[id^='zql-quick-srh-id']").live("click", function(e) {
	e.preventDefault();
	var title = AJS.$('#searchZQLFilterName').find('option:selected').attr('title') || '';
	var filterQuery = title.split('&id=')[0];

	if(filterQuery && filterQuery != null && filterQuery.length > 0 ){
		var filterId = title.split('&id=')[1];
		AJS.$("#zqltext").clearInputs();
		AJS.$("#zqltext").val(filterQuery);
		AJS.$("#zqltext").attr('filterId', filterId);
		AJS.$("#zqltext").attr('data-query',filterQuery);
		ZephyrZQLFilters.setFilterName(AJS.$('#searchZQLFilterName-field').val());
		if(filterId)
			Zephyr.navigateToFilters(filterId);
		else
	        Zephyr.transform();
	}
});


ZEPHYR.ZQL.ZQLSearch.SearchDisplayView = Backbone.View.extend({
    tagName:'div',
    className: 'search-header-wrapper',

    events:{
    	"click [id^='refreshZQLId']"	: "refreshZQLView"
    },
    initialize:function () {
        this.model.bind("reset", this.render, this);
    },
    render: function() {
    	AJS.$(this.el).empty();
    	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0 &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length > 0) {
    		AJS.$(this.el).append(ZEPHYR.ZQL.Search.addDisplayDetail({
	    		totalCount:ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount,
	    		currentIndex:ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.currentIndex,
				maxAllowed:ZEPHYR.ZQL.pagination.maxResultAllowed})).find('#list-display-wrapper-right').eq(0).append(window.zqlListColumnSelectorView.render().el);
			    //AJS.$('#list-display-wrapper-right').append(window.zqlListColumnSelectorView.render().el);
			   window.zqlListColumnSelectorView.delegateEvents();
    		return this;
    	} else {
    		AJS.$(this.el).append();
    		return this;
    	}
    },
    refreshZQLView : function(event) {
    	event.preventDefault();
    	Zephyr.unSelectedSchedules();
		this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
    }
});


ZEPHYR.ZQL.ZQLSearch.SearchHeaderView = Backbone.View.extend({
    tagName: 'div',

    events:{
    	"click [id^='zephyr-zql-save-filter']" 	:      "saveZQLFilter"
    },

    initialize:function () {
        this.model.bind("reset", function() {
        	if(!window.standalone) this.render();
        }, this);
    },
    render:function (eventName) {
    	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0 &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes != null) {
    		AJS.$(this.el).html('');
	    	AJS.$(this.el).append(ZEPHYR.ZQL.Search.addZQLHeader({currentIndex:ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.currentIndex}));
		    if(AJS.$('#zqltext').val() == null || AJS.$('#zqltext').val() == '') {
		    	AJS.$(this.el).find('#zephyr-zql-save-filter').attr('disabled', 'disabled').removeClass('save-as-new-filter');
		    	this.undelegateEvents();
	    	}
		    this.updateHeader();
	    	return this;
    	} else {
    		AJS.$(this.el).append();
    		return this;
    	}
    },

    /*
     * Header will be updated to the filterName in three cases
     * i: Predefined filters
     * ii: Favourite Filters/Manage Execution Filters
     * iii: Search Execution Filters
     * In case of empty query or if the filterId is lost when page is refreshed with query in URL,
     * the Header will be displayed as 'Search'.
     */
	updateHeader: function() {
		var filterId 	= AJS.$('#zqltext').attr('filterid');
		var query 		= AJS.$('#zqltext').val();
		var filterName;

		if(filterId && query) {
			filterName = ZEPHYR.ZQL.ZQLSearch.data.filterName;
			if(!filterName) {
				var predefinedFilters = {
					'-1': AJS.I18n.getText("zql.filter.system.1"),
					'-2': AJS.I18n.getText("zql.filter.system.2"),
					'-3': AJS.I18n.getText("zql.filter.system.3"),
					'-4': AJS.I18n.getText("zql.filter.system.4"),
					'-5': AJS.I18n.getText("zql.filter.system.5"),
					'-6': AJS.I18n.getText("zql.filter.system.6")
				}
				if(predefinedFilters[filterId])
					filterName = predefinedFilters[filterId];
			}
		}
		ZephyrZQLFilters.updateFilterNameOnHeader(filterName);
	},

    saveZQLFilter : function(event) {
    	event.preventDefault();
	    var dialog = new JIRA.FormDialog({
	        id: "save-zql-filter-id",
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
				var htmlTemplate = AJS.$(ZEPHYR.ZQLFilter.Save.saveZQLFilter({method:"POST", fName:"", fDesc:"", isFav:"true", sharePerm:"1", dialogTitle: AJS.I18n.getText("zql.filter.save.dialog.title") }));
	            callback(htmlTemplate);
	        },
	        submitHandler: function (e) {
	        	parent.zqlHeaderView.validateZQL(function() {
	        		dialog.hide();
	            });
	        	e.preventDefault();
	        }
	    });
	    dialog.show();
    },

    validateZQL: function(completed) {
    	var zql = AJS.$("#zqltext").val();
    	var url = getApiBasepath() + "/zql/executeSearch/" ;

    	jQuery.ajax({
    		url: url,
            type: "GET",
            contentType: "application/json",
            dataType: "json",
            data: {zqlQuery:zql,offset:0,maxRecords:0,expand:"executionStatus"},
            success : function(response) {
            	parent.zqlHeaderView.saveZql();
            	if(completed)
					completed.call();
            },
            error: function(response) {
            	ZEPHYR.ZQL.ZQLSearch.data.searchResults.reset()
            	showZQLError(response);
            	if(completed)
					completed.call();
			}
        });
    },

	saveZql : function() {
		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter",
			type : "POST",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'query' : AJS.$("#zqltext").val(),
				  'filterName' : AJS.$('#filterName').val(),
		          'description' :  AJS.$('#filterDescription').val(),
		          'isFavorite' :  AJS.$('#isFavFilter').is(':checked'),
		          'sharePerm'  :  AJS.$('input[name=sharePerm]:radio:checked').val()
			}),
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				AJS.$('#zqltext').attr('filterid',response.id);
                ZephyrZQLFilters.findFiltersByUser();
				var filterId = response.id;
				var tempColumnFields = JSON.parse(JSON.stringify(ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean));
				for (var counter = 0; counter < tempColumnFields.length; counter += 1) {
					delete tempColumnFields[counter].id;
				}
        		var columnItems = {
        			'executionFilterId': filterId,
					'columnItemBean': tempColumnFields,
        		};
	    		AJS.$.ajax({
	    			url: getRestURL() +  '/znav/createColumnSelection',
	    			type: 'POST',
	    			contentType:"application/json",
	    			dataType: "json",
	    			data: JSON.stringify(columnItems),
	    			success: function(response) {
	    				var queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=0' : '&view=' + window.viewType;
    	        		ZEPHYR.ZQL.router.navigate("?filter=" + filterId + queryParam, {trigger: true});
	    			},
	    			error: function(response) {
	    				showZQLError(response);
	    			}
	    		});
			},
			error : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	updateZql : function (filterId){
		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/update",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'id' : parseInt(filterId),
				  'query': AJS.$("#zqltext").val()
			}),
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
				//disable the button
				AJS.$("#zephyr-zql-update-filter").attr('disabled','disabled');
			},
			error : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	renameZql : function (completed, filterId){
		var filterName 		= AJS.$('#filterName').val();
		var prevFilterName 	= AJS.$('#filterName').attr('data-filterName');

		// If there is no change in the filterName, just close the dialog
		if(filterName == prevFilterName) {
			if(completed)
				completed.call();
			return false;
		}

		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/rename",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'id' : filterId,
				  'filterName' : AJS.$('#filterName').val()
			}),
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
				if(completed)
					completed.call();
			},
			error : function(response,jqXHR) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				if(completed)
					completed.call();
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	copyZql : function (completed, filterId){
		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/copy",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'id' : filterId,
				  'filterName' : AJS.$('#filterName').val()
			}),
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
				if(completed)
					completed.call();
			},
			error : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				if(completed)
					completed.call();
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	removeZql : function (completed, filterId){
		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/"+filterId,
			type : "DELETE",
			contentType :"application/json",
			dataType: "json",
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
                if(completed)
                    completed.call();
			},
			error : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
                if(completed)
                    completed.call();
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	markZqlUnfavorite : function (filterId){
		jQuery.ajax({
			url: getRestURL() + "/zql/executionFilter/toggleFav",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify({
				  'isFavorite' : false,
				  'id' : filterId
			}),
			success : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
			},
			error : function(response) {
				parent.zqlHeaderView.showZQLSaveMsg(response);
				ZephyrZQLFilters.findFiltersByUser();
			}
		});
	},

	showZQLSaveMsg : function(response){
		var cxt = AJS.$("#zql-message-bar");
		cxt.empty();
		var title="";
		if(response && response.status && response.status != 200){
			title = "Error:";
			var msg = jQuery.parseJSON(response.responseText);
			var errorMessage = "";
			for(var propertyName in msg) {
				errorMessage += msg[propertyName];
				errorMessage += "\n";
			}
			AJS.messages.error(cxt, {
				title: title,
			    body: errorMessage,
			    closeable: true
			});
		} else {
			if(response.success) {
				AJS.messages.success(cxt, {
					title: title,
				    body: response.success,
				    closeable: true
				});
			}
			if(response.responseMessage) {
				AJS.messages.success(cxt, {
					title: title,
				    body: response.responseMessage,
				    closeable: true
				});
			}
		}
		setTimeout(function(){
			AJS.$(".aui-message").fadeOut(1000, function(){
				AJS.$(".aui-message").remove();
			});
		}, 2000);
	}

 });


// Detail execution view
var timeout;
ZEPHYR.ZQL.ZQLSearch.DetailExecutionView = Backbone.View.extend({
	className: 'aui-group split-view',
	currentExecutionIndex : 0,
	events: {
		'click .execution-name, .selectable-link' 	: 'triggerDisplayExecutionDetails',
		'click #previous-execution'	:	'previousExecutionShow',
		'click #next-execution'	:	'nextExecutionShow',
		'click .execution-id'                                        	: 'triggerDisplayExecutionDetails',
		'click #prev-page-execution'									: 'triggerPreviousPagination',
		'click #next-page-execution'									: 'triggerNextPagination',
		'click #return-to-search'										: 'returnToSearchUI',
		'click #view-full-screen'										: 'displayFullScreen',
		'click #next-page-execution-detail'                             : 'displayNextExecutions',
		'click #prev-page-execution-detail'                             : 'displayPrevExecutions',
		'click #nav-exe-dropdown-value'                                 : 'updateExecutions',
		'click #nav-exe-dropdown'                                       : 'showExecutionLimits',
		// 'focusout #nav-exe-dropdown'                                    : 'hideExecutionLimits',
		'change .aui-field-defectpickers'	: 'strikeOutDefectText',
		'focusout .aui-field-defectpickers'	:	'strikeOutDefectText',
		'keydown #stl-exe-left-container.focus' 						: 'traverseExecution',
		'click #stl-exe-left-container' 						: 'onClickLeftPanel',
		'click #stl-exe-right-container'									: 'onClickRightPanel',
		'click .change-pagination-width-function'									: 'changePaginationWidth',
		'click #pagination-dropdown-button'									: 'togglePaginationOptionDrodown',
		'click #pagination-go-to-previous-page'									: 'nextPage',
		'click #pagination-go-to-next-page'									: 'previousPage',
		'click #first-page-pagination': 'goToFirstPage',
		'click #last-page-pagination': 'goToLastPage',
	},

	initialize: function() {
		// re-render the view when the searchResults collection is reset.
		this.model.bind("reset", function() {
			if(window.viewType == 'detail') this.render();
		}, this);
		this.options.dispatcher.on('triggerPreviousExecution', function() {
			var targetEl = AJS.$('#previous-execution');
			if(targetEl.length == 0) targetEl = AJS.$('#prev-page-execution');
			targetEl.trigger('click');
		}, this);
		this.options.dispatcher.on('triggerNextExecution', function() {
			var targetEl = AJS.$('#next-execution');
			if(targetEl.length == 0) targetEl = AJS.$('#next-page-execution');
			targetEl.trigger('click');
		}, this);
		this.options.dispatcher.on('triggerFullScreen', function() {
			AJS.$('#view-full-screen').trigger('click');
		}, this);
		this.options.dispatcher.on('triggerPageNavigation', function() {
			if(this.pageEvent != '')
				this.triggerDisplayExecutionDetails(this.pageEvent);
		}, this);
		this.isPrevDetailView = false;
		this.pageEvent = '';
	},

	onClickLeftPanel : function(e) {
		e.currentTarget.classList.remove('focus');
		e.currentTarget.classList.add('focus');
		e.preventDefault();
	},

	onClickRightPanel : function(e) {
		var leftPanel = AJS.$('#stl-exe-left-container')
		if (leftPanel.length) {
			leftPanel[0].classList.remove('focus');
		}
	},

	traverseExecution : function(e) {
		if (e.keyCode == '38' || e.keyCode == '40') {
			e.preventDefault();
			var newCurrentIndex;
			if (e.keyCode == '38') {
				newCurrentIndex = this.currentExecutionIndex  > 0 ? this.currentExecutionIndex - 1 : 0;
			} else {
				newCurrentIndex = this.currentExecutionIndex  > -1 ? this.currentExecutionIndex + 1 : 0;
			}

			var elementCanBeClicked = AJS.$('.execution-name-wrapper.execution-id');
			var elementToBeClicked = elementCanBeClicked.length > newCurrentIndex ? elementCanBeClicked[newCurrentIndex] : undefined;

			if (elementToBeClicked) {
				this.currentExecutionIndex = newCurrentIndex;
				elementToBeClicked.click();
			}
		}
	},

	showExecutionLimits: function(){
		if( AJS.$("#nav-exe-dropdown .dropDown-container").css('display') == 'block'){
			AJS.$("#nav-exe-dropdown .dropDown-container").css("display", "none");
		} else{
			AJS.$("#nav-exe-dropdown .dropDown-container").css("display", "block");
		}
	},

	hideExecutionLimits: function(){
		AJS.$("#nav-exe-dropdown .dropDown-container").css("display", "none");
	},

	updateExecutions: function(ev){
		//  NEED TO HIT THE PUT/POST API HERE TO SET THE PAGINATION WIDTH

		var sendingData = allPagesPaginationPageWidth;
		sendingData.standaloneExecution = parseInt(ev.target.dataset.entries);

		if (allPagesPaginationPageWidth.fromPage == 'enav') {
			sendingData.searchTestExecution = parseInt(ev.target.dataset.entries);
		} else if (allPagesPaginationPageWidth.fromPage == 'planCycleSummaryactions') {
			sendingData.planCycleSummary = parseInt(ev.target.dataset.entries);
		}

		var that = this;

		jQuery.ajax({
			url: getRestURL() + "/preference/paginationWidthPreference",
			type: "PUT",
			contentType: "application/json",
			data: JSON.stringify(sendingData),
			dataType: "json",
			success: function (response) {
				var url = window.location.hash;
				var splitUrl = url.split('&');
				var newUrl = '';

				for (var counter = 0; counter < splitUrl.length; counter += 1) {
					if (splitUrl[counter].indexOf('pageWidth=') >= 0) {
						splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + parseInt(ev.target.dataset.entries);
					}
					if (splitUrl[counter].indexOf('offset=') >= 0) {
						splitUrl[counter] = splitUrl[counter].split('=')[0] + '=0';
					}
					if (splitUrl[counter].indexOf('view=') >= 0) {
						splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + window.viewType;
					}
				}
				newUrl = splitUrl[0];
				for (var counter = 1; counter < splitUrl.length; counter += 1) {
					newUrl += '&' + splitUrl[counter];
				}
				ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });


				ZEPHYR.ExecutionCycle.navObj.maxResultAllowed = parseInt(ev.target.dataset.entries);
				AJS.$("#nav-exe-dropdown .dropDown-select").html(ev.target.innerHTML);
				ZEPHYR.ExecutionCycle.navObj.offset = -1 * parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed);		//so that when the next execution is called, it resets it to zero by adding the equal number
				that.displayNextExecutions();
			}
		});
	},
	displayNextExecutions: function(){
		var selectedId;
		if(window.standalone) {
			var offset = parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed);
			ZEPHYR.ExecutionCycle.navObj.offset = offset;
			if(isNaN(offset)) offset = 0;
			window.viewType = 'detail';
			window.standalone = true;
			var zqlQuery = decodeURIComponent(ZephyrURL('?query'));
			ZephyrZQLFilters.setZQL(zqlQuery);
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({data:{zqlQuery:zqlQuery,offset:offset,
				maxRecords:ZEPHYR.ExecutionCycle.navObj.maxResultAllowed,expand:"executionStatus"},contentType:'application/json',  success:function(response) {
					cycleObj = response.models[0].attributes.executions;
					// cycleObj.sort(function(a,b) {return (a.id < b.id) ? 1 : ((b.id < a.id) ? -1 : 0);} );
					window.zqlDetailView.fetchPrevNextExecution(cycleObj[0].id);
					window.zqlDetailView.delegateEvents();
					atLastPagination = (response.models[0].attributes.totalCount <= parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed));
					atFirstPagination = ZEPHYR.ExecutionCycle.navObj.offset === 0;
					//window.zqlDetailView.render();

					var url = window.location.hash;
					var splitUrl = url.split('&');
					var newUrl = '';

					for (var counter = 0; counter < splitUrl.length; counter += 1) {
						if (splitUrl[counter].indexOf('offset=') >= 0) {
							splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + (parseInt(splitUrl[counter].split('=')[1]) + 1);
						}
						if (splitUrl[counter].indexOf('?') >= 0) {
							var innerSplit = splitUrl[counter].split('?');
							var deepInnerSplit = innerSplit[0].split('/')[0] + '/' + response.models[0].attributes.executions[0].id;

							splitUrl[counter] = deepInnerSplit + '?' + innerSplit[1];
						}
					}
					newUrl = splitUrl[0];
					for (var counter = 1; counter < splitUrl.length; counter += 1) {
						newUrl += '&' + splitUrl[counter];
					}

					ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
			}})
		} else {
			triggeringPaginationFrom = 'detail-next-page';
			ZEPHYR.ZQL.pagination.offset = parseInt(ZEPHYR.ZQL.pagination.offset) + parseInt(ZEPHYR.ZQL.pagination.maxResultAllowed);
			this.triggerEnavPagination("", ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		}
	},
	displayPrevExecutions: function(){
		var selectedId;
		if(window.standalone) {
			var toDisplayLast = arguments.length > 0 && arguments[0] !== undefined ? false : true;
			var offset = parseInt(ZEPHYR.ExecutionCycle.navObj.offset) - parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed);
			ZEPHYR.ExecutionCycle.navObj.offset = offset;
			if(isNaN(offset)) offset = 0;
			window.viewType = 'detail';
			window.standalone = true;
			var zqlQuery = decodeURIComponent(ZephyrURL('?query'));
			ZephyrZQLFilters.setZQL(zqlQuery);
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({
				data:{
					zqlQuery:zqlQuery,
					offset:offset,
					maxRecords:ZEPHYR.ExecutionCycle.navObj.maxResultAllowed,
					expand:"executionStatus"
				},
				contentType:'application/json',
				success:function(response) {
					cycleObj = response.models[0].attributes.executions;
					// cycleObj.sort(function(a,b) {return (a.id > b.id) ? 1 : ((b.id > a.id) ? -1 : 0);} );
					var offsetToDisplay = cycleObj.length - 1;
					window.zqlDetailView.fetchPrevNextExecution(cycleObj[toDisplayLast? offsetToDisplay: 0].id);
					window.zqlDetailView.delegateEvents();
					atLastPagination = (response.models[0].attributes.totalCount <= parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed));
					atFirstPagination = ZEPHYR.ExecutionCycle.navObj.offset === 0;
					//window.zqlDetailView.render();

					var url = window.location.hash;
					var splitUrl = url.split('&');
					var newUrl = '';

					for (var counter = 0; counter < splitUrl.length; counter += 1) {
						if (splitUrl[counter].indexOf('offset=') >= 0) {
							splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + (parseInt(splitUrl[counter].split('=')[1]) - 1);
						}
						if (splitUrl[counter].indexOf('?') >= 0) {
							var innerSplit = splitUrl[counter].split('?');
							var deepInnerSplit = innerSplit[0].split('/')[0] + '/' + response.models[0].attributes.executions[toDisplayLast? response.models[0].attributes.executions.length - 1 : 0].id;

							splitUrl[counter] = deepInnerSplit + '?' + innerSplit[1];
						}
					}
					newUrl = splitUrl[0];
					for (var counter = 1; counter < splitUrl.length; counter += 1) {
						newUrl += '&' + splitUrl[counter];
					}
					ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
				}
			})
		} else {
			triggeringPaginationFrom = 'details-previous-page';
			ZEPHYR.ZQL.pagination.offset -= parseInt(ZEPHYR.ZQL.pagination.maxResultAllowed);
			this.triggerEnavPagination("", ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		}

	},
	triggerEnavPagination : function(zqlQuery, offset, maxRecords, colPrefix, queryPrefix) {
		// ZEPHYR.ZQL.ZQLSearch.data.zql = zqlQuery;
		var selectedSchedules = [];
		var instance = this;
		//setting url
		var url = window.location.hash;
		var splitUrl = url.split('&');
		var newUrl = '';

		for (var counter = 0; counter < splitUrl.length; counter += 1) {
			if (splitUrl[counter].indexOf('offset=') >= 0) {
				splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + offset;
			}
			if (splitUrl[counter].indexOf('selectedId=') >= 0) {
				splitUrl[counter] = '';
			}
		}
		newUrl = splitUrl[0];
		for (var counter = 1; counter < splitUrl.length; counter += 1) {
			newUrl += (splitUrl[counter] !== '')? '&' + splitUrl[counter] : '';
		}
		ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
		//url set
		AJS.$("div.navigator-content").html("");
		ZEPHYR.Loading.showLoadingIndicator();
        ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({data:{zqlQuery:ZEPHYR.ZQL.ZQLSearch.data.zql,offset:offset,maxRecords:maxRecords,expand:"executionStatus"},contentType:'application/json', reset:true, success:function() {
					var totalPages = Math.ceil(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount / ZEPHYR.ZQL.pagination.maxResultAllowed);
					var currentIndex = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.currentIndex;
					if(currentIndex == totalPages) {
						atLastPagination = true;
					} else {
						atLastPagination = false;
					}
					if(currentIndex == 1) {
						atFirstPagination = true;
					} else {
						atFirstPagination = false;
					}

					ZEPHYR.ZQL.pagination.offset = offset;
			var cxt = AJS.$("#zql-message-bar");
			cxt.empty();
			if(ZEPHYR.ZQL.ZQLSearch.data.searchResults != null && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] != null &&
				ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
					ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length == 0) {
				AJS.$("div.navigator-content").addClass("empty-result");
				var message = AJS.I18n.getText('enav.results.none.found');
                AJS.$("div.navigator-content").html(ZEPHYR.ZQL.Search.noResults({message: message}));
			} else {
				if(ZEPHYR.ZQL.ZQLSearch.data.searchResults != null && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] != null &&
						ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
					selectedSchedules = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
				}

				if(selectedSchedules == null || selectedSchedules.length == 0) {
					if (typeof scheduleList != 'undefined') {
						selectedSchedules = scheduleList;
					}
				}
				//set the Checkbox to check previous selected executionIds
				if(selectedSchedules != null && selectedSchedules.length > 0) {
			        AJS.$.each(selectedSchedules, function(index,schedule) {
			        	ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.push(schedule);
			        	AJS.$('#scheduleCheck-'+schedule).attr('checked',true);
			        });
				}
				if(colPrefix != null || colPrefix != undefined) {
					Zephyr.refreshAttributes(colPrefix,queryPrefix);
				}
				JIRA.Dropdowns.bindNavigatorOptionsDds();
				globalDispatcher.trigger('triggerPageNavigation');
				globalDispatcher.trigger("schedulesCountRefresh",ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds);
			}
			// Disable save button for predefined filters
			if(AJS.$("#zqltext").attr("filterId") != "" && AJS.$("#zqltext").attr("filterId") > 0 &&
					AJS.$("#zqltext").val() != AJS.$('#zqltext').attr('data-query')) {
				AJS.$("#zephyr-zql-update-filter").removeAttr('disabled');
			} else {
				AJS.$("#zephyr-zql-update-filter").attr('disabled','disabled');
				AJS.$("#zephyr-zql-update-filter").off("click");
			}
			if(window.viewType == 'list')
				ZEPHYR.Loading.hideLoadingIndicator();
			AJS.$("#jqlerrormsg").removeClass("loading");
			AJS.$("#jqlerrormsg").addClass("jqlgood");
				// instance.toggleBulkToolBar();
		}, error: function(model, response) {
			ZEPHYR.Loading.hideLoadingIndicator();
			if(response.status == 401) {
				var statusText = response.statusText || AJS.I18n.getText('zephyr.common.forbidden.error.label');
				displayUnAuthErrorDialog(statusText);
			} else {
				instance.model.reset();
				AJS.$("#jqlerrormsg").removeClass("loading");
				AJS.$("#jqlerrormsg").addClass("jqlgood");
				showZQLError(response);
			}
	    }})
	},

	previousExecutionShow: function(ev) {
		this.currentExecutionIndex = ev.currentTarget.dataset['index'] ? Math.abs(ev.currentTarget.dataset['index'])  : 0;
    var self = this;
  	ev.preventDefault();
  	var currentTarget 			= ev.currentTarget || ev.srcElement,
  		selectedExecutionId		= AJS.$(currentTarget).attr('data-id'),
  		offset					= AJS.$(currentTarget).attr('data-offset') || ZephyrURL('?offset'),
  		queryString 			= '',
  		queryParam;
			if(selectedExecutionId === '0') {
				self.displayPrevExecutions();
				// self.fetchPrevNextExecution(cycleObj[cycleObj.length - 1].id);
			} else {
				self.triggerDisplayExecutionDetails(ev);
			}
	},

	nextExecutionShow: function(ev) {
		this.currentExecutionIndex = ev.currentTarget.dataset['index'] ? Math.abs(ev.currentTarget.dataset['index'])  : 0;
    var self = this;
  	ev.preventDefault();
  	var currentTarget 			= ev.currentTarget || ev.srcElement,
  		selectedExecutionId		= AJS.$(currentTarget).attr('data-id'),
  		offset					= AJS.$(currentTarget).attr('data-offset') || ZephyrURL('?offset'),
  		queryString 			= '',
  		queryParam;
			if(selectedExecutionId === '0') {
				self.displayNextExecutions();
			} else {
				self.triggerDisplayExecutionDetails(ev);
			}
	},

	triggerDisplayExecutionDetails: function(ev) {
	if (timeout) {
    	clearInterval(timeout);
	}
    var scrollToPos = AJS.$(ev.currentTarget).position().top + AJS.$('.execution-list-wrapper').scrollTop() - 95;
    this.currentExecutionIndex = ev.currentTarget.dataset['index'] ? Math.abs(ev.currentTarget.dataset['index'])  : 0;
    var self = this;
    var currentTarget 			= ev.currentTarget || ev.srcElement,
		selectedExecutionId		= currentTarget.dataset.id,
		offset,
		queryString 			= '',
		queryParam,
		pageWidth = allPagesPaginationPageWidth.searchTestExecution;
  		// ev.preventDefault();
    timeout = setTimeout(function() {

  		var queryParam = ZephyrZQLFilters.getViewQueryParamFromURL();
		if(window.standalone) {
			offset = currentTarget.dataset.offset || (ZEPHYR.ExecutionCycle.navObj.offset / ZEPHYR.ExecutionCycle.navObj.maxResultAllowed) + 1;
		} else {
			offset = ZEPHYR.ZQL.pagination.offset;
		}
  		queryString = '?' + queryParam + 'view=' + window.viewType + '&offset=' + offset + '&pageWidth=' + pageWidth + '&selectedId=' + selectedExecutionId;
			//to set a preceding / in query, added double / as single is trimmed off
  		if(window.standalone) {
  			queryString = '//' + selectedExecutionId + queryString;
  		}
      	window.offset = offset;
      	//This is to avoid repetative API calls, when left nav execution is clicked
      	if (window.standalone && currentselectedExecutionId != selectedExecutionId) {
      		currentselectedExecutionId = selectedExecutionId;
      		updateSection = RIGHT_SECTION_TO_BE_UPDATED;
      	}

  		ZEPHYR.ZQL.router.navigate(queryString, {trigger: false});
  		if(self.pageEvent == '' || window.standalone) {
  			self.fetchPrevNextExecution(selectedExecutionId, true , true);
  		} else {
  			self.pageEvent = '';
  		}

  		highlightDehighightExecution(selectedExecutionId);

    }, 350);

    // setTimeout(function() {
    //   scrollToElement(scrollToPos);
    // }, 1500);

	},

	triggerPreviousPagination: function(ev) {
		ev.stopImmediatePropagation();
		ev.preventDefault();
		var currentTarget 			= ev.currentTarget || ev.srcElement,
			offset					= AJS.$(currentTarget).attr('data-offset'),
			zqlOffset 				= (ZEPHYR.ZQL.pagination.offset - ZEPHYR.ZQL.pagination.maxResultAllowed);

		window.offset = window.offset - 1;
		ZEPHYR.Loading.showLoadingIndicator();
		this.options.dispatcher.trigger("searchZQLEvent", ZEPHYR.ZQL.ZQLSearch.data.zql, zqlOffset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		this.pageEvent = ev;
	},

	triggerNextPagination: function(ev) {
		ev.stopImmediatePropagation();
		ev.preventDefault();
		var currentTarget 			= ev.currentTarget || ev.srcElement,
			offset					= AJS.$(currentTarget).attr('data-offset'),
			zqlOffset 				= (ZEPHYR.ZQL.pagination.offset + ZEPHYR.ZQL.pagination.maxResultAllowed);

		ZEPHYR.Loading.showLoadingIndicator();
		this.options.dispatcher.trigger("searchZQLEvent", ZEPHYR.ZQL.ZQLSearch.data.zql, zqlOffset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		this.pageEvent = ev;
	},

	// If the executionId does not exist in zqlSerach list or if response is empty,
	// hide the loading indicator and call showZQLErrorOnNavigate() to display the error message.
    triggerZQLErrorOnNavigate: function(message) {
    	ZEPHYR.Loading.hideLoadingIndicator();

		if(AJS.$('#zql-message-bar:hidden').length > 0) {
			ZephyrZQLFilters.showZQLErrorOnNavigate(AJS.I18n.getText("execute.test.execution.navigator.error.label"), AJS.I18n.getText("execute.test.execution.navigator.error"));
		} else {
	    	AJS.messages.error(AJS.$('#zql-message-bar'), {
				title: 'Error',
				body:  AJS.I18n.getText("execute.test.execution.navigator.error"),
				closeable: false
			});
		}
    },

	returnToSearchUI: function(ev) {
		ev.preventDefault();
		var searchURL = AJS.$('#return-to-search').attr('href');

		this.isPrevDetailView = false;
		ZEPHYR.ZQL.router.navigate(searchURL, {trigger: true});
	},

	displayFullScreen: function(ev) {
		ev.preventDefault();
		var queryString = AJS.$(ev.target).attr('href');
		this.isPrevDetailView = true;
		ZEPHYR.ZQL.router.navigate(queryString.split('#')[1], {trigger: true});
	},

	//function listening on change of textbox for defects in standAlone page and strikeThrough
	//text if resolution is done. fetches the li elements and loops through them to check its inner html value
	//matches with defect key value and resolution is done.

	strikeOutDefectText: function(ev) {
		listOfElem = AJS.$('#editable-schedule-defects').find('.representation ul').find('li');
    if(!listOfElem.length || !globalSchedule.defects || (listOfElem.length !== globalSchedule.defects.length)){
        return;
    }
		for(var i = 0; i < listOfElem.length; i += 1) {
      var issueId = AJS.$(listOfElem[i]).find('.value-text a').html() || AJS.$(listOfElem[i]).find('.value-text').html();
      var defectObj = _.findWhere(globalSchedule.defects, {key: issueId});
			var title = defectObj.summary + ':' + defectObj.status;
      if(!AJS.$(listOfElem[i]).find('.value-text').hasClass('linkAppended')) {
			var issueText = AJS.$(listOfElem[i]).find('.value-text').html();
			AJS.$(listOfElem[i]).find('.value-item').on('click', function (event) {
				var url = AJS.$(event.currentTarget).find('a').attr('data-href');
				if (event.ctrlKey) {
					window.open(url, '_blank')
				} else {
					window.location.href = AJS.$(event.currentTarget).find('a').attr('data-href');
				}
			});
        	AJS.$(listOfElem[i]).attr('title', title);
				AJS.$(listOfElem[i]).find('.value-text').html('<a data-href="' + contextPath + '/browse/' + issueText + '">'+ issueText + '</a>');
				AJS.$(listOfElem[i]).find('.value-text').addClass('linkAppended');
			}

			if(defectObj && defectObj.resolution === 'Done') {
				AJS.$(listOfElem[i]).find('.value-text a').css('text-decoration', 'line-through');
			}
		}
	},

  	attachDetailView: function(detailViewContainer, zqlSearch, testChangeSelected, isRightSectionToBepdated) {
		testChangeSelected = testChangeSelected || false;
		ZEPHYR.Schedule.executionStatus = zqlSearch.get('status');
		executionDetails = zqlSearch.get('execution');
		ZEPHYR.Schedule.Execute.data = {
			issueId:		zqlSearch.get('execution').issueId,
			scheduleId:		zqlSearch.get('execution').id,	//This is redundant coz its used in templates. Need to remove
			projectId:		zqlSearch.get('execution').projectId
		};
		var issueSummary = zqlSearch.get('summary') || '';
		var issueDescription = zqlSearch.get('execution').issueDescription || '';
		var schedule = zqlSearch.get('execution');
		globalSchedule = schedule;	//saving schedule in global variable for use in strikeOutDefectText method
		// if(cycleObj.length == 0 && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount>0){
		if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount>0){
			cycleObj = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions;
			// cycleObj.sort(function(a,b) {return (a.id > b.id) ? 1 : ((b.id > a.id) ? -1 : 0);} );
		}
		// if(schedule.cycleName){
		//   schedule.cycleName = schedule.cycleName;
		//   cycleObj.name = schedule.cycleName;
		// 	  cycleObj.type = 'cycle';}
		// if(schedule.folderName){
		//   schedule.folderName = schedule.folderName;
		//   cycleObj.name = schedule.folderName;
		//   cycleObj.type = 'folder';
		// }

		var paginationObject = {};
		paginationObject.dbIndex = ZEPHYR.Schedule.Execute.data.scheduleId - 1;
		paginationObject.total = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount;
		// paginationObject.nextExecutionId = zqlSearch.get('nextExecutionId');
		// paginationObject.prevExecutionId = zqlSearch.get('prevExecutionId');
		paginationObject.nextExecutionId = 0;
		paginationObject.prevExecutionId = 0;


	    for(var i = 0; i<cycleObj.length;i++){
	      if(ZEPHYR.Schedule.Execute.data.scheduleId == cycleObj[i].id){
	        paginationObject.localIndex = i;
	        break;
	      }
	    }

		var totalCount 	= ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount;

		var queryString = ZephyrZQLFilters.getViewQueryParamFromURL();
		var viewType = 'list';
		if(this.isPrevDetailView) {
			viewType = 'detail';
		}
	    console.log('query string', queryString, AJS.$("#zqltext").val());
	    if(queryString) {
        queryString+= 'view=' + viewType + '&offset=' + ZEPHYR.ZQL.pagination.offset;
      } else {
        queryString='query=' + encodeURIComponent('issue = ' + executionDetails.issueKey)  + '&view=' + viewType + '&offset=' + ZEPHYR.ZQL.pagination.offset;
      }
		// for(var i=0; i<cycleObj.length; i++){
		// 	if(cycleObj[i].id === ZEPHYR.Schedule.Execute.data.scheduleId){
		// 		ZEPHYR.ExecutionCycle.navObj.offset = Math.floor(i / ZEPHYR.ExecutionCycle.navObj.maxResultAllowed);
		// 		// if(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed >= cycleObj.length){
		// 		// 	ZEPHYR.ExecutionCycle.navObj.offset = 0;
		// 		// } else{
		// 		// 	ZEPHYR.ExecutionCycle.navObj.offset = i;
		// 		// }
		// 		// if(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed > cycleObj.length){
		// 		// 	ZEPHYR.ExecutionCycle.navObj.maxResultAllowed = cycleObj.length;
		// 		// }
		// 		break;
		// 	}
		// }

		navigationObj = [];
		for(var i=0; i<cycleObj.length; i++){
			navigationObj.push(cycleObj[i]);
			if(cycleObj[i].id === ZEPHYR.Schedule.Execute.data.scheduleId){
				if (cycleObj[i+1]) {
					paginationObject.nextExecutionId = cycleObj[i+1].id;
				}
				if (cycleObj[i-1]) {
					paginationObject.prevExecutionId = cycleObj[i-1].id;
				}
				// break;
			}
		}
		navigationObj.total = cycleObj.length;
		ZEPHYR.Execution.currentSchedule = {
			'statusObj' : {
				currentExecutionStatus: zqlSearch.get('status')[zqlSearch.get('execution').executionStatus],
				executionStatuses: zqlSearch.get('status'),
			}
		}

		if(!window.standalone) {
			var totalPages = Math.ceil(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount / ZEPHYR.ZQL.pagination.maxResultAllowed);
			var currentIndex = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.currentIndex;
			if(currentIndex == totalPages) {
				atLastPagination = true;
			} else {
				atLastPagination = false;
			}
			if(currentIndex == 1) {
				atFirstPagination = true;
			} else {
				atFirstPagination = false;
			}
		}

		var scheduleIndex= cycleObj.indexOf(_.findWhere(cycleObj, {id: schedule.id}));

		var next = null;
		var previous = null;

		var previousId = 0;
		var nextId = 0;


		for (var counter = 0; counter < cycleObj.length; counter += 1) {
			if (ZEPHYR.ZQL.prevNextId.previousId == cycleObj[counter].id) {
				previous = cycleObj[counter];
				previousId = ZEPHYR.ZQL.prevNextId.previousId;
			}
			if (ZEPHYR.ZQL.prevNextId.nextId == cycleObj[counter].id) {
				next = cycleObj[counter];
				nextId = ZEPHYR.ZQL.prevNextId.nextId;
			}
		}


		var detailedViewHtml = ZEPHYR.Templates.Execution.detailedExecutionView({
			hideListView:           window.standalone ? hideListView : true,
			previous: 				previous,
			next: 					next,
			previousId: 			previousId,
			nextId: 				nextId,
			schedule:				schedule,
			executedByDisplay:		zqlSearch.get('execution').executedByDisplay,
			executedBy:				zqlSearch.get('execution').executedBy,
			currentExecutionStatus:	zqlSearch.get('status')[zqlSearch.get('execution').executionStatus],
			executionStatuses:		zqlSearch.get('status'),
			contextPath:			contextPath,
			projectKey:				zqlSearch.get('execution').projectKey,
			projectName:			zqlSearch.get('execution').projectName,
			projectAvatarId:		zqlSearch.get('execution').projectAvatarId,
			issueDescAsHtml:		issueDescription,
			queryString:            queryString,
		  	stepColumns:            ZEPHYR.ExecutionCycle.executionStepColumns,
		  	customFieldsOrder:      ZEPHYR.ExecutionCycle.customFieldsOrder,
			isExecutionPage : 		true,
			atLastPagination:		atLastPagination,
			atFirstPagination:		atFirstPagination,
		});

		var leftNavDetails = ZEPHYR.Templates.Execution.leftNavDetails({
			hideListView:           window.standalone ? hideListView : true,
			navigationObj:          navigationObj,
			navObj:                 ZEPHYR.ExecutionCycle.navObj,
			schedule:				schedule,
			contextPath:			contextPath,
			atLastPagination:	atLastPagination,
			atFirstPagination:	atFirstPagination,
		});

		var exeCont = detailViewContainer.find('.exe-cont');

		if (exeCont.length){
			if (isRightSectionToBepdated) {
				exeCont.find('.stl-exe-right').remove();
				exeCont.append(detailedViewHtml);
			} else {
				exeCont.html('');
				exeCont.append(leftNavDetails);
				exeCont.append(detailedViewHtml);
			}
		} else {
			detailViewContainer.html(ZEPHYR.Templates.Execution.executionWrapper({}));
			exeCont = detailViewContainer.find('.exe-cont');
			exeCont.append(leftNavDetails);
			exeCont.append(detailedViewHtml);
		}



    if(schedule.executionWorkflowStatus == 'COMPLETED') {
      setTimeout(function() {
        AJS.$('#editable-schedule-defects textarea').attr('disabled', true);
      }, 2000);
    }
    //setTimeout(function() {
      var observerUtility = new ObserverUtility();
	  document.getElementById('editable-schedule-defects') && observerUtility.observer.observe(document.getElementById('editable-schedule-defects'));
    //}, 0);
    var isFolder = schedule.folderName ? true : false;
		AJS.$("#cycle-name").html(ZEPHYR.Templates.Execution.nodeNameBar({
  		nodeName: isFolder ? schedule.folderName : schedule.cycleName,
  		nodeType: isFolder ? 'folder' : 'cycle',
      href: contextPath + '/secure/PlanTestCycle.jspa?projectKey=' + schedule.projectKey
		}));

		// AJS.$('#detailViewBtn').addClass('active-view');
		// AJS.$('#listViewBtn').removeClass('active-view');

		// if (testChangeSelected) {
		// 	AJS.$('#detailViewBtn').removeClass('active-view');
		// 	AJS.$('#listViewBtn').addClass('active-view');
		// }

		if(AJS.$('#estimatedTimeId').length) {
			AJS.progressBars.update("#estimatedTimeId", 1);
			AJS.progressBars.update("#loggedTimeId", 0.4);
		}
		var stepColumnCustomization = ZEPHYR.Templates.Execution.columnCustomisation({columns: ZEPHYR.ExecutionCycle.executionStepColumns, submitButtonId: stepSubmitButtonId, closeButtonId: closeButtonId });
		if(AJS.$("#inline-dialog-execution-column-picker").length > 0) {
			AJS.$("#inline-dialog-execution-column-picker").remove();
		}
		executionColumnsInlineDialog = AJS.InlineDialog(AJS.$("#step-columnCustomisation-inlineDialog-execution"), "execution-column-picker",
			function(content, trigger, showPopup) {
			    content.css({"padding":"10px 0 0", "max-height":"none"}).html(stepColumnCustomization);
			    showPopup();
			    return false;
			},
			{
			  width: 250,
			  closeOnTriggerClick: true,
			  persistent: true
			}
		);
		// Attach executionStatusView
		var editableFieldTrigger = AJS.$('#zexecute div.field-group.execution-status-container')[0];
		var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
			el: 			editableFieldTrigger,
			elBeforeEdit:	AJS.$(editableFieldTrigger).find('[id^=execution-field-current-status-schedule-]'),
			elOnEdit:		AJS.$(editableFieldTrigger).find('[id^=execution-field-select-schedule-]')
		});

		/* Initialize backbone model using form elements */
		ZEPHYR.Schedule.Execute.currentSchedule = new ZEPHYR.Issue.Schedule({
			id:					parseInt(zqlSearch.get('execution').id),
			executionStatus: 	AJS.$("select[id^=exec_status-schedule] option:selected").attr("value"),
			comment:			AJS.$('#schedule-comment')
		});
		ZEPHYR.Execution.History.init(zqlSearch.get('execution').issueId, zqlSearch.get('execution').id);
		ZEPHYR.Execution.CustomField.init(zqlSearch.get('execution'));
	},

	attachTestSteps: function(zqlSearch) {
		var instance = this;
		ZEPHYR.ExecutionCycle.schedule = zqlSearch.get('execution');
		ZEPHYR.Schedule.Execute.getAttachments(ZEPHYR.Schedule.Execute.currentSchedule.get("id"), "SCHEDULE");
		scheduleView = new ZEPHYR.Schedule.Execute.ScheduleView({model:ZEPHYR.Schedule.Execute.currentSchedule, el:AJS.$("#zexecute")});
		ZEPHYR.Schedule.Execution.data = {};
		ZEPHYR.Schedule.Execution.data.teststeps = new ZEPHYR.Issue.TestStepCollection();
   		ZEPHYR.Schedule.Execution.data.teststeps.fetch({success: function(teststeps, response) {
					ZEPHYR.Execution.testDetailsGrid.testStep = response.stepBeanCollection;
   			instance.fetchStepResults();
   		}});
	},

	// Display Execution details in the UI.
	displayExecutionDetails: function(selectedExecutionId, zqlSearch, executionJSON, isRightSectionToBepdated) {
		var instance				= this,
			executions				= this.model.models[0].attributes.executions,
			isExecIdInList			= false;

		// Remove focused class attached to the DOM
		AJS.$('.issue-list li').removeClass('focused');
		if(AJS.$('#zqltext').val().length == 0) AJS.$('#zqltext').val(ZEPHYR.ZQL.ZQLSearch.data.zql);

		// Attach Detail view
    globaldetailViewContainer = AJS.$('#detail-panel-wrapper');
    globalzqlSearch = zqlSearch;
    getCustomFields(executionJSON.projectId, function(){
	  instance.attachDetailView(AJS.$('#detail-panel-wrapper'), zqlSearch, isRightSectionToBepdated);
  		AJS.$('#execution-name-' + selectedExecutionId).addClass('focused');
  		AJS.$('#detail-view-pagination-bottom').html(window.zqlPaginationView.render().el);
  		window.zqlPaginationView.delegateEvents();

  		// Attach Test steps
  		instance.attachTestSteps(zqlSearch);
  		// TODO: Remove the looping, once I get the execution currentIndex.
  		// ----------------------------------------------------------------
  		// Based on the selected Cycle name, display the execution details
  		instance.attachAssigneeUI(executionJSON);
  		/*AJS.$.each(executions, function(i, execution) {
  			if(selectedExecutionId == execution.id) {
  			//	isExecIdInList = true;
  				// Attach Assignee UI
  		    	instance.attachAssigneeUI(executionJSON);
  		   		// instance.displayPrevNextNavigator(selectedExecutionId, (instance.model.models[0].attributes.offset + i), zqlSearch);
  		   	//	instance.getPrevNextValues(execution, i, selectedExecutionId);
  			}
  		});*/
  		if(executionJSON.isExecutionWorkflowEnabled && executionJSON.isTimeTrackingEnabled && executionJSON.cycleId != -1 && !executionJSON.isIssueEstimateNil) {
  			instance.displayExecutionWorkflow(executionJSON);
  		}
       	// AJS.$('.execute-test-header-right').remove();
  		/*if(!isExecIdInList) {
  			this.triggerZQLErrorOnNavigate();
  		}*/
    });
	},
		// Display Execution details in the UI.
	displayExecutionDetailsStandAlone: function (selectedExecutionId, zqlSearch, executionJSON, testChangeSelected, isRightSectionToBepdated) {
		var testChangeSelected = testChangeSelected || false;
		var instance				= this,
			executions				= this.model.models[0].attributes.executions,
			isExecIdInList			= false;

		// Remove focused class attached to the DOM
		AJS.$('.issue-list li').removeClass('focused');

		AJS.$('#content, #footer').show();
		instance.setElement(AJS.$('#content'));
		// Attach Detail view
    globaldetailViewContainer = AJS.$('#content');
    globalzqlSearch = zqlSearch;
    getCustomFields(executionJSON.projectId, function(){
	  instance.attachDetailView(AJS.$('#content'), zqlSearch, testChangeSelected, isRightSectionToBepdated);
      if(window.location.hash.indexOf('query') != -1) {
  			var zql = ZEPHYR.ZQL.ZQLSearch.data.zql;
  			if (!(AJS.$('#content > textarea').length)) {
  				AJS.$('#content').append('<textarea autocomplete="off" style="display: none; visibility: hidden;" name="zqltext" filterid="' + ZephyrZQLFilters.getFilterId() + '" id="zqltext"></textarea>');
  			}
  			AJS.$('#zqltext').attr('data-query', zql);
  			AJS.$('#zqltext').val(zql);
  		}

      instance.attachAssigneeUI(executionJSON);
  		// Attach Test steps
  		instance.attachTestSteps(zqlSearch);
      // TODO: Remove the looping, once I get the execution currentIndex
  		// ---------------------------------------------------------------
  		// If the execution id is present in the zqlSearch list then displaying the pagination else not.
  		AJS.$.each(executions, function(i, execution) {
              if(selectedExecutionId == execution.id) {
              	// Attach Assignee UI
  		    	//instance.attachAssigneeUI(executionJSON);
  				//isExecIdInList = true;
  		   		if(window.location.hash.indexOf('query') != -1 || window.location.hash.indexOf('filter') != -1) {
  					instance.displayPrevNextNavigator(selectedExecutionId, (instance.model.models[0].attributes.offset + i), zqlSearch);
  		   		}
  			}
  		});
  		if(executionJSON.isExecutionWorkflowEnabled && executionJSON.isTimeTrackingEnabled && executionJSON.cycleId != -1 && !executionJSON.isIssueEstimateNil) {
  			instance.displayExecutionWorkflow(executionJSON);
  		}
  		var options = {'className': 'standalone_execution_in_app', 'containerId': 'content'};
		/*var inAppMessage = new InAppMessage(options);
    	inAppMessage.createShowHideButton();*/
    });

		/*if(!isExecIdInList) {
			this.triggerZQLErrorOnNavigate();
		}*/
	},

	displayExecutionWorkflow: function(executionJSON) {
    	var executionWorkflow = new ExecutionWorkflow();
		    executionWorkflow.init({
    			buttonWrapperId: 'executionWorkflowWrapper',
    			progressWrapperId: 'executionWorkFlowProgressWrapper',
    			executionId: executionJSON.id,
    			estimatedTime: executionJSON.executionEstimatedTime,
    			estimatedTimePercentage: executionJSON.workflowLoggedTimedIncreasePercentage,
    			loggedTime: executionJSON.executionTimeLogged,
    			loggedTimePercentage: executionJSON.workflowCompletePercentage,
    			workflowStatus: executionJSON.executionWorkflowStatus
    		});
	},
	
	changePaginationWidth: function(event) {
		var that = this;

		var sendingData = allPagesPaginationPageWidth;
		sendingData.testGridWidth = parseInt(event.target.dataset.entries);

		jQuery.ajax({
			url: getRestURL() + "/preference/paginationWidthPreference",
			type: "PUT",
			contentType: "application/json",
			data: JSON.stringify(sendingData),
			dataType: "json",
			success: function (response) {
				allPagesPaginationPageWidth = response;
				ZEPHYR.ExecutionCycle.testStep.offset = 0;
				ZEPHYR.ExecutionCycle.testStep.limit = event.target.dataset.entries;
				that.fetchStepResults();
			}
		});
	},

	nextPage: function() {
		if (isReadyForPaginationChange) {
			isReadyForPaginationChange = false;
			this.resetPaginationFlag();
			ZEPHYR.ExecutionCycle.testStep.offset = parseInt(ZEPHYR.ExecutionCycle.testStep.offset) + parseInt(ZEPHYR.ExecutionCycle.testStep.limit);
			this.fetchStepResults();
		}
	},

	previousPage: function() {
		if (isReadyForPaginationChange) {
			isReadyForPaginationChange = false;
			this.resetPaginationFlag();
			ZEPHYR.ExecutionCycle.testStep.offset = parseInt(ZEPHYR.ExecutionCycle.testStep.offset) - parseInt(ZEPHYR.ExecutionCycle.testStep.limit);
			this.fetchStepResults();
		}
	},

	goToFirstPage: function() {
		if (isReadyForPaginationChange) {
			isReadyForPaginationChange = false;
			this.resetPaginationFlag();
			ZEPHYR.ExecutionCycle.testStep.offset = 0;
			this.fetchStepResults();
		}
	},

	resetPaginationFlag: function() {
		setTimeout(function () {
			isReadyForPaginationChange = true;
		}, 200);
	},

	goToLastPage: function() {
		if (isReadyForPaginationChange) {
			isReadyForPaginationChange = false;
			this.resetPaginationFlag();
			var lastPageLowerLimit;
			for (var counter = 0; counter < ZEPHYR.ExecutionCycle.testStep.maxRecords; counter += 1) {
				if (counter % ZEPHYR.ExecutionCycle.testStep.limit == 0) {
					lastPageLowerLimit = counter;
				}
			}
			ZEPHYR.ExecutionCycle.testStep.offset = lastPageLowerLimit;
			this.fetchStepResults();
		}
	},

	togglePaginationOptionDrodown: function() {
		if (AJS.$('#pagination-options-container').hasClass('hide')) {
			AJS.$('#pagination-options-container').removeClass('hide');
		} else {
			AJS.$('#pagination-options-container').addClass('hide');
		}
	},

	fetchStepResults: function() {
     	var stepResults = new ZEPHYR.Schedule.Execution.TestStepResultCollection()
		stepResults.fetch({data:{executionId: ZEPHYR.Schedule.Execute.currentSchedule.get('id'), expand: "executionStatus", offset: ZEPHYR.ExecutionCycle.testStep.offset, limit: ZEPHYR.ExecutionCycle.testStep.limit}, success:function(response){
			
			if (response.models.length > 0) {
				ZEPHYR.ExecutionCycle.testStep.maxRecords = response.models[0].attributes.stepResultsCount;
	
				var lastPageLowerLimit;
				for (var counter = 0; counter < parseInt(ZEPHYR.ExecutionCycle.testStep.maxRecords); counter += 1) {
					if (counter % parseInt(ZEPHYR.ExecutionCycle.testStep.limit) == 0) {
						lastPageLowerLimit = counter;
					}
				}
				var paginationComponentHtml = ZEPHYR.Templates.Execution.paginationComponent({
					limit: parseInt(ZEPHYR.ExecutionCycle.testStep.limit),
					offset: parseInt(ZEPHYR.ExecutionCycle.testStep.offset),
					maxRecords: parseInt(ZEPHYR.ExecutionCycle.testStep.maxRecords),
					lastPageLowerLimit: lastPageLowerLimit,
				});
				AJS.$('#pagination-outer-container').html(paginationComponentHtml);
			}
				ZEPHYR.Execution.testDetailsGrid.stepResults = [];

				stepResults.models.map(function(stepResult) {
					ZEPHYR.Execution.testDetailsGrid.stepResults.push(stepResult.attributes);
				});

     		window.teststepResultView = new ZEPHYR.Schedule.Execution.TeststepResultView({model:stepResults});
     		if(stepResults.models != null && stepResults.models.length > 0) {
     			ZEPHYR.Schedule.Execution.data.stepExecutionStatuses = new Backbone.Collection(stepResults.models[0].get('executionStatus'));
					ZEPHYR.Execution.testDetailsGrid.executionStatus = {};
					ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.map(function(status){
						ZEPHYR.Execution.testDetailsGrid.executionStatus[status.id] = status;
					});
     		}

     		// AJS.$('#teststepDetails div.mod-content table.aui').append(window.teststepResultView.render().el);
				if(ZEPHYR.Execution.testDetailsGrid.stepResults.length === 0) {
          ZEPHYR.Execution.testDetailGridExecutionPage();
        }
				var counter = 0;
     		// ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult, index){
     		// 	ZEPHYR.Schedule.Execute.getAttachments(stepResult.id, "TESTSTEPRESULT",function(attachment){
			// 			stepResult['stepAttachmentMap'] = attachment;
			// 			counter++;
			// 			if(counter === ZEPHYR.Execution.testDetailsGrid.stepResults.length)
			// 				ZEPHYR.Execution.testDetailGridExecutionPage();
  			// 	});
			//  })
			  ZEPHYR.Execution.testDetailGridExecutionPage();
     		try {
	     		jQuery.ajax({url: getRestURL() +  '/issues/default?project=' + ZEPHYR.Schedule.Execute.data.projectId})
	                .always(initializeDefectPickers);
            } catch(e) {
            	console.log(e);
            }
         	ZEPHYR.Loading.hideLoadingIndicator();
     	}});
     	AJS.$('textarea.comment-status-stepresult').attr('cols', 10);
    },

	fetchPrevNextExecution: function(selectedExecutionId, testChangeSelected, isRightSectionToBepdated) {
	globalSelectedExecutionId = selectedExecutionId;
		var testChangeSelected = testChangeSelected || false;
		var instance	= this,
			query 		= ZephyrZQLFilters.getViewQueryParamFromURL(),
			zql 		= AJS.$("#zqltext").val() || ZEPHYR.ZQL.ZQLSearch.data.zql,
			zqlSearch 	= new ZEPHYR.ZQL.ZQLSearch(),
			offset;

		ZEPHYR.Loading.showLoadingIndicator();
		if (ZEPHYR.ZQL.pagination.offset) offset = ZEPHYR.ZQL.pagination.offset;
		else offset = 0;
		let maxRecords = ZEPHYR.ZQL.pagination.maxResultAllowed;
		zqlSearch.url = getRestURL() + '/execution/navigator/' + selectedExecutionId + '?zql=' + encodeURIComponent(zql)
			+ '&offset=' + offset + '&maxRecords=' + maxRecords + '&expand=executionStatus,checksteps';
		// Fix for ZFJ-2016, displaying the permission message
		AJS.$('#content').before('<div class="notifications zfj-permission-execution-standalone active" id="zfj-permission-message-bar-execution-standalone"></div>');
		zqlSearch.fetch({contentType:'application/json',
			success: function(zqlSearch, response) {
				ZEPHYR.ZQL.prevNextId = {
					previousId: response.prevExecutionId,
					currentId: response.execution.id,
					nextId: response.nextExecutionId,
					offset: parseInt(offset),
				};
				if(response.length == 0){
					instance.triggerZQLErrorOnNavigate();
				} else {
					if(!window.standalone) instance.displayExecutionDetails(selectedExecutionId, zqlSearch, response.execution, isRightSectionToBepdated);
					else instance.displayExecutionDetailsStandAlone(selectedExecutionId, zqlSearch, response.execution, testChangeSelected, isRightSectionToBepdated);
				}
				AJS.$('.zfj-permission-execution-standalone').remove();
			},
			error: function(zqlSearch, jqXHR) {
				ZEPHYR.Loading.hideLoadingIndicator();
				if(AJS.$('#zql-message-bar:hidden').length > 0) {
          if(typeof jqXHR.responseText !== 'string')
					    var msg = (jQuery.parseJSON(jqXHR.responseText));
          else
              var msg = {};
					if(msg.error) {
                        ZephyrZQLFilters.showZQLErrorOnNavigate(msg.error, msg.error);
                    }
                    if(msg.errorId && msg.errorDesc) {
                        ZephyrZQLFilters.showZQLErrorOnNavigate(msg.errorId, msg.errorDesc);
                    }
				} else {
					showZQLError(jqXHR);
				}
				AJS.$('.zfj-permission-execution-standalone').remove();
			}, sync:true
		});
	},

    // Display the prev next UI.
    displayPrevNextNavigator: function(selectedExecutionId, index, zqlSearch) {
    	var totalCount 	= ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount,
    		queryString = ZephyrZQLFilters.getViewQueryParamFromURL();

    	if(!window.standalone) {
    		AJS.$('#prev-next-container').remove();
    		var qString = selectedExecutionId + '?query=' + encodeURIComponent(AJS.$("#zqltext").val()) + '&view=' + window.viewType + '&offset=' + (index + 1);
	    	// Insert the prev next pagination
	    	AJS.$('h1.execute-test-header-left').before(ZEPHYR.ZQL.Search.addPrevNextNavigator({
	    		totalCount: 		totalCount,
	    		currentIndex: 		(index + 1),
	    		prevExecutionId:	zqlSearch.get('prevExecutionId'),
	    		previousOffset: 	index,
	    		nextExecutionId:	zqlSearch.get('nextExecutionId'),
	    		nextOffset: 		(index + 2),
	    		queryString:		qString,
	    		maxResultAllowed:	ZEPHYR.ZQL.pagination.maxResultAllowed
	    	}));
    // 	} else if(window.standalone && queryString != undefined) {
    // 		var viewType = 'list';
    // 		if(this.isPrevDetailView) {
    // 			viewType = 'detail';
    // 		}
    // 		AJS.$('#prev-next-return-container').remove();
    // 		AJS.$('#execute-test-header-right').append(ZEPHYR.ZQL.Search.addStandalonePrevNextNavigator({
				// totalCount: 		totalCount,
	   //  		currentIndex: 		(index + 1),
	   //  		prevExecutionId:	zqlSearch.get('prevExecutionId'),
	   //  		previousOffset: 	index,
	   //  		nextExecutionId:	zqlSearch.get('nextExecutionId'),
	   //  		nextOffset: 		(index + 2),
    //             queryString:		queryString + 'view=' + viewType + '&offset=' + (index + 1),
    //             maxResultAllowed:	ZEPHYR.ZQL.pagination.maxResultAllowed
    // 		}));
    	}
    	window.offset = index + 1;
    },

    getAssigneeParams: function(executionJSON) {
    	if(!executionJSON)
    		return {};

    	return {
    		"executionId": executionJSON.id,
			"assignee": executionJSON.assignedTo,
			"assigneeDisplay": executionJSON.assignedToDisplay,
			"assigneeUserName": executionJSON.assignedToUserName,
			"assigneeType": executionJSON.assigneeType
		};
    },

    attachAssigneeUI: function(executionJSON) {
    	var _assignee = this.getAssigneeParams(executionJSON);
      AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath ] );
  //   	AJS.$('#exec-assignee-wrapper').append(ZEPHYR.Templates.Execution.executionAssigneeDetailView({
		// 	assignee: 	_assignee,
		// 	contextPath: contextPath
		// }));
    	// Attach Edit View UI
    	var editableFieldTrigger = AJS.$('#zexecute div.field-group.execution-assignee-container')[0];
    	// Attach assignee editable view only if user exists
    	if(editableFieldTrigger) {
    		var _executionAssigneeView = new ZEPHYR.Schedule.executionAssigneeView({
    			el: 			editableFieldTrigger,
    			elBeforeEdit:	AJS.$(editableFieldTrigger).find('[id^=execution-field-current-assignee-]'),
    			elOnEdit:		AJS.$(editableFieldTrigger).find('[id^=execution-field-select-assignee-]')
    		});
    	}
    },

    getSelectionId: function() {
    	var index = 0;
		var execution = this.model.models[0].attributes;
		if(ZephyrURL('?selectedId')) {
			return ZephyrURL('?selectedId');
		} else if(window.offset >  0 && window.offset <= execution.totalCount){
			var offset = window.offset % execution.maxResultAllowed;
			if (window.offset == execution.maxResultAllowed * execution.currentIndex) {
				index = execution.maxResultAllowed;
			}
			else if (offset <= execution.executions.length) {
				index = offset;
			}
		}
		if (index != 0) {
			index = index - 1;
		}

		if (triggeringPaginationFrom.length > 0) {
			if (triggeringPaginationFrom == 'details-previous-page') {
				index = this.model.models[0].attributes.executions.length;
			} else if (triggeringPaginationFrom == 'details-next-page') {
				index = 0;
			}
			triggeringPaginationFrom = '';
		}
		if(index != 0) index = index - 1;

		return (this.model.models[0].attributes.executions[index].id);
    },

	render: function() {
		AJS.$(this.el).html("");
    	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0 &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length > 0) {
    		if(!window.standalone) {
    			// html template is ZEPHYR.ZQL.Search.addDetailViewLayout
    			var executions = this.model.models[0].attributes.executions;
    			// executions.sort(function(a,b) {return (a.id > b.id) ? 1 : ((b.id > a.id) ? -1 : 0);} );

	    		AJS.$(this.el).html(ZEPHYR.ZQL.Search.addDetailViewLayout({
	    			executions: executions,
	    			offset:		ZEPHYR.ZQL.pagination.offset,
	    			contextPath: contextPath
	    		}));
	    		// Resizable sidebar
				// AJS.$("#list-results-panel").sidebar({
				//     id: "detail-view-sidebar",
				//     minWidth: function () {
				//         return 230;
				//     },
				//     maxWidth: function () {
				//         return 500;
				//     },
				//     resize: function() {
				//     	// var width = AJS.$('#zqlResponse').parentsUntil('.navigator-group').width() - AJS.$('#list-results-panel').width();
				//     	// AJS.$('#detail-panel-wrapper').width(width - 6);
				//     }
				// });
		    	// var width = AJS.$('#zqlResponse').parentsUntil('.navigator-group').width() - AJS.$('#list-results-panel').width();
		    	// AJS.$('#detail-panel-wrapper').width(width - 6);
		    	// Lock the window scroll for the detail view's list and detail panel
		        // ZEPHYR.scrollLock('#detail-panel-wrapper');
		        // ZEPHYR.scrollLock('#list-results-panel .list-panel');
	    		var selectedExecutionId = this.getSelectionId();
				// display the execution details based on the offset
				if (ZEPHYR.ZQL && !ZEPHYR.ZQL.maintain) {
					this.fetchPrevNextExecution(selectedExecutionId);
				}
    		}
    	} else {
    		AJS.$(this.el).append();
    	}
		return this;
	}
});


AJS.$("a#rssExecutionId,a#csvExecutionId,a#xmlExecutionId,a#htmlExecutionId,a#xlsExecutionId").live('click',function(event) {
	var exportType = event.target.id.substring(0,3);
	AJS.$('#execDownloadFrame').attr('src', function ( i, val ) {
		var zql = AJS.$("#zqltext").val();
        jQuery.ajax({
    		url: getRestURL() + "/execution/export?exportType="+exportType+"&maxAllowedResult=true&expand=teststeps&offset=" + AJS.$("#currentIndex").text() + "&zqlQuery=" + zql,
    		type : "POST",
    		contentType :"application/json",
    		dataType: "json",
            beforeSend: function(){
                AJS.$('#exportExecutionLoader').show();
            },
    		async: true,
    		data: JSON.stringify({
    			  'exportType' : exportType,
    			  'maxAllowedResult': 'true',
    			  'expand': 'teststeps',
    			  'startIndex' :  AJS.$("#currentIndex").text(),
    			  'zqlQuery' : zql,
    			  'executions':ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds
    		}),
    		success: function(response) {
    			if(exportType == 'rss') {
    				if(response.url) {
    					window.location.href=response.url;
    				}
    			} else {
    				AJS.$('#execDownloadFrame').attr('src',response.url);
    			}
                AJS.$('#exportExecutionLoader').hide();
    		},
    		error : function(jqXHR, status) {
    			AJS.log(status)
                AJS.$("#exportExecutionLoader").hide();
        		showZQLError(jqXHR);
    		}
    	});
	});
	event.preventDefault();
});

var highlightDehighightExecution = function(id) {

	var executionsBlocks = AJS.$('.stl-exe-nav-left-wrapper .execution-id');
		executionsBlocks.removeClass('active');
	for (var i=0;i<executionsBlocks.length;i++) {
		var executionBlock = executionsBlocks[i];

		if (executionBlock.dataset && executionBlock.dataset.id == id) {
			executionBlock.classList.add('active');
		}
	}

}
/* This is temporary, once we encapsulate all views, this will become local instance to ready()*/
var globalDispatcher = _.extend({}, Backbone.Events);
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
	/*if(window.InAppMessage && window.InAppMessage.prototype) {
		InAppMessage.prototype.zephyrBaseUrl = document.getElementById('zephyrBaseUrl').value;
	}
	var inAppMessageUrlFieldElem = document.getElementById('inAppMessageUrlField');
	if(inAppMessageUrlFieldElem) {
		window.inAppMessageUrlFieldValue = inAppMessageUrlFieldElem.value;
	}*/

    var analyticUrlFieldElem = document.getElementById('analyticUrlField');
    if(analyticUrlFieldElem) {
        window.analyticUrlFieldValue = analyticUrlFieldElem.value;
    }
	ZEPHYR.ZQL.ZQLSearch.data = {};
	ZEPHYR.ZQL.ZQLFilters.data = {};
	ZEPHYR.ZQL.ZQLColumn.data = {};

	try {
		ZephyrZQLFilters.createZQLFilterPicker();
	} catch(e) {
		console.log(e);
	}
	var scheduleList = [], instance = this;
	window.viewType = 'list', window.offset = 1, window.standalone = false;
	window.zqlFields = AJS.$('#zqlFieldz').html();
	window.zqlFunctionNames = AJS.$('#zqlFunctionNamez').html();
	window.zqlReservedWords = AJS.$('#zqlReservedWordz').html();

	AJS.$('#zephyr-transform-all').click(Zephyr.transform);
	ZEPHYR.ZQL.ZQLSearch.data.searchResults = new ZEPHYR.ZQL.ZQLSearchCollection();
	ZEPHYR.ZQL.ZQLColumn.data.zqlColumns = new ZEPHYR.ZQL.ZQLColumnCollection();

	window.sidebarPopoutView = new ZEPHYR.ZQL.ZQLFilters.SidebarPopoutView();
	window.zqlDisplayView = new ZEPHYR.ZQL.ZQLSearch.SearchDisplayView({model:ZEPHYR.ZQL.ZQLSearch.data.searchResults, dispatcher:globalDispatcher});
	zqlResultView = new ZEPHYR.ZQL.ZQLSearch.SearchResultView({model:ZEPHYR.ZQL.ZQLSearch.data.searchResults, dispatcher:globalDispatcher});
	window.zqlHeaderView = new ZEPHYR.ZQL.ZQLSearch.SearchHeaderView({model:ZEPHYR.ZQL.ZQLSearch.data.searchResults, dispatcher:globalDispatcher});
	window.zqlPaginationView = new ZEPHYR.ZQL.ZQLSearch.SearchPaginationView({model:ZEPHYR.ZQL.ZQLSearch.data.searchResults, dispatcher:globalDispatcher});
	window.zqlDetailView = new ZEPHYR.ZQL.ZQLSearch.DetailExecutionView({model:ZEPHYR.ZQL.ZQLSearch.data.searchResults, dispatcher:globalDispatcher});
	window.zqlListColumnSelectorView = new ZEPHYR.ZQL.ZQLColumn.ListColumnSelectorView({model: ZEPHYR.ZQL.ZQLColumn.data.zqlColumns, dispatcher: globalDispatcher});

	ZephyrZQLFilters.toggleView();

	// If using Tempo plugin, tempo adds shortcut 't', 'j', 'k' to global context,
	// currenlty handling this issue using try/catch.
	try {
		// Toggle list/ detail view on keypress of alphabet 't'
		AJS.whenIType("t").execute(function () {
			if(!window.standalone) {
				window.viewType = window.viewType == 'detail' ? 'list' : 'detail';
				var queryParam = ZephyrZQLFilters.getViewQueryParamFromURL();
				var queryString = queryParam + 'view=' + window.viewType;
				if(window.viewType == 'detail') queryString += '&offset=' + window.offset;
				ZEPHYR.ZQL.router.navigate('?' + queryString, {trigger: true});
			}
		});
	} catch(e) {
		console.log(e.message);
	}
	try {
		AJS.whenIType("j").execute(function () {
			if(window.viewType == 'detail') globalDispatcher.trigger("triggerNextExecution");
		});
	} catch(e) {
		console.log(e.message);
	}
	try {
		AJS.whenIType("k").execute(function () {
			if(window.viewType == 'detail') globalDispatcher.trigger("triggerPreviousExecution");
		});
	} catch(e) {
		console.log(e.message);
	}
	try {
		AJS.whenIType("u").execute(function () {
			if(window.viewType == 'detail') globalDispatcher.trigger("triggerFullScreen");
		});
	} catch(e) {
		console.log(e.message);
	}

	// Display the view based on type (detail/list)
	function updateDisplayView(viewType) {
		// TODO: Find a better way to find if the aui dropdown2 is supported
		var dropdown2Support = true;
		if(typeof JIRA.Dropdowns.bindHeaderDropdown2 !== 'function') dropdown2Support = false;
		AJS.$('.layout-switcher-item span').removeClass('aui-iconfont-success icon-tick');
		ZEPHYR.ZQL.ZQLSearch.data.searchResults.reset();
		if(AJS.$('#zqlResponse').length == 0) {
			AJS.$('#content').html(ZEPHYR.ZQL.Search.enavWrapper({
				zqlFields: 			window.zqlFields,
				zqlFunctionNames: 	window.zqlFunctionNames,
				zqlReservedWords: 	window.zqlReservedWords,
				contextPath: 		contextPath,
				dropdown2Support:	dropdown2Support
			}));
			window.zqlDetailView.setElement('#zqlResponse div.split-view');
			// Reattaching the search elements
			restore('jqlHistory');
		    setupAutocomplete();
		    try {
		    	ZephyrZQLFilters.createZQLFilterPicker();
		    } catch(e) {
		    	console.log(e);
		    }
			ZephyrZQLFilters.toggleView();
			AJS.$('#zephyr-transform-all').click(Zephyr.transform);
			if(window.zqlFiltersView) {
				AJS.$('#contentRelatedId').append(window.zqlFiltersView.render().el);
				window.zqlFiltersView.delegateEvents();
			}
			var zqlPredefinedFiltersView  = new ZEPHYR.ZQL.ZQLFilters.PredefinedSearchFilterView();
			zqlPredefinedFiltersView.delegateEvents();
			window.sidebarPopoutView = new ZEPHYR.ZQL.ZQLFilters.SidebarPopoutView();
			if(!dropdown2Support) JIRA.Dropdowns.bindGenericDropdowns();
			AJS.$('#zqltext').val(ZEPHYR.ZQL.ZQLSearch.data.zql);
			AJS.$('#zqltext').attr('data-query', ZEPHYR.ZQL.ZQLSearch.data.zql);
			AJS.$('#zqltext').attr('filterid', ZephyrZQLFilters.getFilterId());
		}
		// Change view button icon, attach tick icon and update UI for the selected view
		if(viewType == 'detail') {
			if(dropdown2Support) {
				AJS.$('#split-view').find('span').addClass('aui-iconfont-success');
			} else  AJS.$('#split-view').find('span').addClass('icon-tick');
			AJS.$('#view-button-icon').removeClass('icon-view-list').addClass('icon-view-split');

			AJS.$('#split-view').addClass('active-view');
      hideListView = true;
    		AJS.$('#list-view').removeClass('active-view');
			AJS.$('#zqlHeader').append(window.zqlHeaderView.render().el);
			window.zqlHeaderView.delegateEvents();
			AJS.$('#zqlResponse').html(window.zqlDetailView.render().el);
			window.zqlDetailView.delegateEvents();
		} else if(viewType == 'list') {
			if(dropdown2Support) {
				AJS.$('#list-view').find('span').addClass('aui-iconfont-success');
			} else  AJS.$('#list-view').find('span').addClass('icon-tick');
			AJS.$('#split-view').removeClass('active-view');
      		hideListView = false;
    		AJS.$('#list-view').addClass('active-view');
			AJS.$('#view-button-icon').removeClass('icon-view-split').addClass('icon-view-list');
			ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.fetch({
				success: function() {
          AJS.$('#zqlResponse').html('');
					AJS.$('#zqlHeader').append(window.zqlHeaderView.render().el);
					window.zqlHeaderView.delegateEvents();
					AJS.$('#zqlResponse').append(window.zqlDisplayView.render().el);
					window.zqlDisplayView.delegateEvents();
					AJS.$('#zqlResponse').append(zqlResultView.render().el);
					zqlResultView.delegateEvents();
					AJS.$('#zqlResponse').append(window.zqlPaginationView.render().el);
					window.zqlPaginationView.delegateEvents();
				    // Attach the dropdown in IE10: ZFJ-1145
				    JIRA.Dropdowns.bindNavigatorOptionsDds();
				},
				error: function(response,jqXHR) {
	    			showZQLError(jqXHR);
	    		},
	    	});
		}
	}

	//TODO A hack Handle a redirect from ManageExecutionFilters Page.
	function navigateToQuery(queryString){
		AJS.$("#jqlerrormsg").removeClass("jqlgood");
		AJS.$("#jqlerrormsg").addClass("loading");
		window.standalone = false;
		// TODO: dynamically calculate the maxResultAllowed value (20).
		var offset = 0;
	// 	if (ZEPHYR.ZQL.ZQLSearch.data.searchResults && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.maxResultAllowed) {
	// 	if (ZEPHYR.ZQL.pagination && ZEPHYR.ZQL.pagination.maxResultAllowed) {
	// 		offset = Math.floor((ZEPHYR.ZQL.pagination.offset) / ZEPHYR.ZQL.pagination.maxResultAllowed) * ZEPHYR.ZQL.pagination.maxResultAllowed;
	// 	} else {
	// 		offset = Math.floor((window.offset-1)/10)* 10;
	// 	}
	// }
		if(ZephyrURL('?offset')) {
			offset = ZephyrURL('?offset');
		} else if(!ZephyrURL('?query')){
			offset = 0;
			var url = '';
			if(["undefined", "null"].indexOf(encodeURIComponent(ZEPHYR.ZQL.ZQLSearch.data.zql)) === -1) {
				url += '?query=' + encodeURIComponent(ZEPHYR.ZQL.ZQLSearch.data.zql) + '&view=list&offset=0';
			} else {
				url += '?view=list&offset=0';
			}

			ZEPHYR.ZQL.router.navigate(url, { trigger: false, replace: true });
		}
		if(queryString == '' && ZephyrURL('?query')) {
			queryString = decodeURIComponent(ZephyrURL('?query'));
		}
		ZEPHYR.ZQL.pagination.offset = offset;
		// if(isNaN(offset) || offset < 0) offset = 0;
		AJS.$("#zqltext").clearInputs();
		AJS.$("#zqltext").val(queryString);
    var $input = AJS.$("#zqltext");

    if ($input.val()) {
        $input.height(0).expandOnInput();
        var suggestions = AJS.$('div.atlassian-autocomplete').find('div.suggestions.dropdown-ready');
        suggestions.css('top',$input[0].offsetHeight);
    } else {
        $input.expandOnInput().height(0).trigger('refreshInputHeight');
    }
		// if (ZEPHYR.ZQL.ZQLSearch.data.searchResults && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.maxResultAllowed) {
		if (ZEPHYR.ZQL.pagination && ZEPHYR.ZQL.pagination.maxResultAllowed) {
			jQuery.ajax({
				url: getRestURL() + "/preference/paginationWidthPreference",
				type: "GET",
				contentType: "application/json",
				dataType: "json",
				success: function (pagewidthResponse) {
					console.log('RESPONSE : ', pagewidthResponse);
					if (!pagewidthResponse.testGridWidth) {
						ZEPHYR.ExecutionCycle.testStep.limit = 10;
					} else {
						ZEPHYR.ExecutionCycle.testStep.limit = pagewidthResponse.testGridWidth;
					}
					if (!pagewidthResponse.searchTestExecution) {
						pagewidthResponse.searchTestExecution = 10;
					}
					ZEPHYR.ZQL.pagination.maxResultAllowed = pagewidthResponse.searchTestExecution;
					ZEPHYR.ZQL.allPagesPaginationPageWidth = pagewidthResponse;

					globalDispatcher.trigger("searchZQLEvent", queryString, offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
				}
			});
		} else {
			globalDispatcher.trigger("searchZQLEvent", queryString, offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		}
	}

  function getJsonFromUrl(query) {
  	try {
  		//var query = location.search.substr(1);
	    var result = {};
	    query.split("&").forEach(function(part) {
	      var item = part.split("=");
	      result[item[0]] = decodeURIComponent(item[1]);
	    });
	    return result;
  	}
  	catch(err) {
  		//
  	}
  }

	var ZQLRouter = Backbone.Router.extend({
		routes: {
			"*path?filter=:filterId"											:		"searchByFilter",  // ?filter=5
			"*path?query=*query"												:		"searchByQuery",    // ?query="something=nothing"
			'*path?view=*query'													:		"updateView",
			':id'																:		"searchExecutionByID",
			''																	: 		'displayAllExecutions'
		},

		searchByFilter : function(path, query){
			if(path) {
				AJS.$('#content, #footer').hide();
				var query = "execution="+path;
				AJS.$('#zqltext').attr('filterid', path);
				ZephyrZQLFilters.setFilterId(path);
				ZephyrZQLFilters.setZQL(query);
				ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({data:{zqlQuery:query,offset:0,
					maxRecords:0,expand:"executionStatus"},contentType:'application/json',  success:function() {
					window.viewType = 'detail';
					window.standalone = true;
					window.zqlDetailView.fetchPrevNextExecution(path);
					window.zqlDetailView.delegateEvents();
				}});
			} else {
				var urlFilter = ZephyrURL('?filter')
				window.viewType = 'list';
				window.standalone = false;

				if(ZephyrURL('?view')) window.viewType = ZephyrURL('?view');
				if(ZephyrURL('?offset')) window.offset = ZephyrURL('?offset');

				var filter = ZEPHYR.ZQL.ZQLFilters.data.searchResults.get(urlFilter);
				if(filter){
					ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.url = getRestURL() + "/znav/availableColumns?executionFilterId=" + filter.get('id');
					AJS.$('#zqltext').attr('filterid', filter.get('id'));
					AJS.$('#zqltext').attr('data-query', filter.get('query'));
    				ZephyrZQLFilters.setFilterId(filter.get('id'));
    				ZephyrZQLFilters.setFilterName(filter.get('filterName'));
    				ZephyrZQLFilters.setZQL(filter.get('query'));
					navigateToQuery(filter.get('query'));
					updateDisplayView(window.viewType);
				} else {
					jQuery.ajax({
		    			url: getRestURL() + "/zql/executionFilter/"+urlFilter,
		    			contentType: "application/json",
		    			success: function(response) {
		    	    		ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.url = getRestURL() + "/znav/availableColumns?executionFilterId=" + (response.id || response.ID);
		    	    		AJS.$('#zqltext').attr('filterid', response.id || response.ID);
		    	    		AJS.$('#zqltext').attr('data-query', response.query);
		    	    		ZephyrZQLFilters.setFilterId(response.id || response.ID);
		    	    		ZephyrZQLFilters.setFilterName(response.filterName);
		    				ZephyrZQLFilters.setZQL(response.query);
		    				navigateToQuery(response.query);
		    				updateDisplayView(window.viewType);
		    			},
		    			error : function(response,jqXHR) {
		        			showZQLError(jqXHR);
		    			}
		    		});
				}
			}
			ZephyrZQLFilters.updateDocumentTitle();
		},

		searchByQuery: function(path, query) {
			var isRightSectionToBepdated = updateSection == RIGHT_SECTION_TO_BE_UPDATED;
			updateSection = '';

		    var queryParamObj = getJsonFromUrl(query);
		    var viewType = queryParamObj && queryParamObj.view;
		    setTimeout(function() {
		      AJS.$("#zqltext").trigger('keypress');
		    },1500);
		    if(viewType === 'list') {
		      setTimeout(function() {
		        AJS.$('#listViewBtn:visible').trigger('click', [isRightSectionToBepdated]);
		      }, 1000);
			}


			var url = window.location.href;
			var urlParams = url.split('&');
			var isPageWidthSet = false;

			var maxExecutions = 0;
			var offset = 0;

			for (var counter = 0; counter < urlParams.length; counter += 1) {
				if (urlParams[counter].indexOf('pageWidth=') >= 0) {
					maxExecutions = parseInt(urlParams[counter].split('=')[1]);
					isPageWidthSet = true;
				} else if (urlParams[counter].indexOf('offset=') >= 0) {
					offset = parseInt(urlParams[counter].split('=')[1]);
				}

			}

			ZEPHYR.ExecutionCycle.navObj.maxResultAllowed = maxExecutions;
			ZEPHYR.ExecutionCycle.navObj.offset = (offset - 1) * maxExecutions;

			//This is to avoid repetative API calls, when left nav execution is clicked
			if (isRightSectionToBepdated) {
				var zqlQuery = decodeURIComponent(ZephyrURL('?query'));
				var offset = ZEPHYR.ExecutionCycle.navObj.offset;
				if(isNaN(offset)) offset = 0;
				ZephyrZQLFilters.setZQL(zqlQuery);
				//window.zqlDetailView.fetchPrevNextExecution(path);
				//window.zqlDetailView.delegateEvents();
				atLastPagination = ((ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount) <= parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed));
				atFirstPagination = ZEPHYR.ExecutionCycle.navObj.offset === 0;
			} else {

				jQuery.ajax({
					url: getRestURL() + "/preference/paginationWidthPreference",
					type: "GET",
					contentType: "application/json",
					dataType: "json",
					success: function (response) {
						console.log('RESPONSE : ', response);

						if (!response.testGridWidth) {
							ZEPHYR.ExecutionCycle.testStep.limit = 10;
						} else {
							ZEPHYR.ExecutionCycle.testStep.limit = response.testGridWidth;
						}
						if (!isPageWidthSet) {
							if (!response.standaloneExecution) {
								response.standaloneExecution = 10;
							}
							ZEPHYR.ExecutionCycle.navObj.maxResultAllowed = response.standaloneExecution;
						}
						allPagesPaginationPageWidth = response;

						if(path) {
							AJS.$('#content, #footer').hide();
							var offset = ZEPHYR.ExecutionCycle.navObj.offset;
							if(isNaN(offset)) offset = 0;
							window.viewType = viewType;
							hideListView = window.viewType == 'list' ? false : true;
							window.standalone = true;
							zqlQuery = decodeURIComponent(ZephyrURL('?query'));
							ZephyrZQLFilters.setZQL(zqlQuery);
							ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({data:{zqlQuery:zqlQuery,offset:offset,
								maxRecords:ZEPHYR.ExecutionCycle.navObj.maxResultAllowed,expand:"executionStatus"},contentType:'application/json',  success:function(response) {
								cycleObj = response.models[0].attributes.executions;
								// cycleObj.sort(function(a,b) {return (a.id > b.id) ? 1 : ((b.id > a.id) ? -1 : 0);} );
								window.zqlDetailView.fetchPrevNextExecution(path);
								window.zqlDetailView.delegateEvents();
								atLastPagination = (response.models[0].attributes.totalCount <= parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed));
								atFirstPagination = ZEPHYR.ExecutionCycle.navObj.offset === 0;
								//window.zqlDetailView.render();
							}})
						} else {
							window.viewType = 'list';
							window.standalone = false;

							if(ZephyrURL('?view')) window.viewType = ZephyrURL('?view');
							if(ZephyrURL('?offset')) window.offset = ZephyrURL('?offset');

							var filterQuery = decodeURIComponent(ZephyrURL('?query'));
							ZephyrZQLFilters.setZQL(filterQuery);
							navigateToQuery(filterQuery);
							updateDisplayView(window.viewType);
						}
						ZephyrZQLFilters.updateDocumentTitle();
						AJS.$('#zqltext').attr('filterid', ZephyrZQLFilters.getFilterId());
					}
				});
			}

		},

		// If there is no filter or query attached to the URL
		updateView: function(path, viewType) {
			window.viewType = ZephyrURL('?view');
            path = path || '';

            if(ZephyrURL('?offset')) window.offset = ZephyrURL('?offset');
			if(isNaN(path)) updateDisplayView(window.viewType);
			else if(path == '') {
				navigateToQuery('');
				updateDisplayView(window.viewType);
			} else this.searchExecutionByID(path);
			ZephyrZQLFilters.updateDocumentTitle();
		},

		displayAllExecutions: function() {
			window.viewType = 'list';
			navigateToQuery('');
			updateDisplayView(window.viewType);
			ZephyrZQLFilters.updateDocumentTitle();
		},

		searchExecutionByID : function(id) {
			if(isNaN(id)) return false;
			var query = "execution = " + id;
			AJS.$('#content, #footer').hide();
			window.viewType = 'detail';
			window.standalone = true;
			ZephyrZQLFilters.setZQL(query);
			// var queryParam = 	'/' + id + '?query=' + encodeURIComponent(query);
			// ZEPHYR.ZQL.router.navigate(queryParam, {trigger: false, replace: true});
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({data:{zqlQuery:query,offset:0,
				maxRecords:0,expand:"executionStatus"},contentType:'application/json',  success:function(model, response) {
				if(response.length == 0)
					ZephyrZQLFilters.showZQLErrorOnNavigate(AJS.I18n.getText("execute.test.execution.navigator.error.label"), AJS.I18n.getText("execute.test.execution.navigator.error"));
				else {
					atFirstPagination = ZEPHYR.ExecutionCycle.navObj.offset === 0;
					atLastPagination = (response.totalCount <= parseInt(ZEPHYR.ExecutionCycle.navObj.offset) + parseInt(ZEPHYR.ExecutionCycle.navObj.maxResultAllowed));
					window.zqlDetailView.fetchPrevNextExecution(id);
				}
				ZEPHYR.Loading.showLoadingIndicator();
			}, error: function(model, response) {
            	var msg = (jQuery.parseJSON(response.responseText));
				ZephyrZQLFilters.showZQLErrorOnNavigate(msg.error, msg.error);
		    }});
			ZephyrZQLFilters.updateDocumentTitle();
		}
 	});
	ZephyrZQLFilters.init();
    if(!ZEPHYR.ZQL.ZQLFilters.LOGGED_IN_USER) {
        var url = getApiBasepath() + "/zql/executionFilter/user";

        jQuery.ajax({
            url: url,
            type : "GET",
            contentType :"application/json",
            dataType: "json",
            success : function(response) {
            	if(response.LOGGED_IN_USER) {
            		ZEPHYR.ZQL.ZQLFilters.LOGGED_IN_USER = response.LOGGED_IN_USER;
            	}
            }
        });
    }
	ZEPHYR.ZQL.router = new ZQLRouter();
	Backbone.history.start();
});

function getApiBasepath() {
    var url = getRestURLForBlueprints() + "";
    var baseCtxPath = window.baseContextPath;
    if(null != baseCtxPath && null != url) {
        var contextPathFromUrl = url.substring(0,url.indexOf("/rest"));
        if(null != contextPathFromUrl && contextPathFromUrl == "") {
            // context path not found -- append with the base url
            url = baseCtxPath + url;
        }
    }
    return url;
}
