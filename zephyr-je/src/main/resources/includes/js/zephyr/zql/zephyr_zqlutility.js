// var paginationWidth = 10;

ZEPHYR.ZQL.pagination = {
	offset: 0,
	maxResultAllowed: 10,
};

ZEPHYR.ZQL.allPagesPaginationPageWidth = {};

AJS.$(document).ready(function (event) {
	//  NEED TO HIT THE GET API HERE TO GET THE PAGINATION WIDTH

});

ZEPHYR.ZQL.ZQLSearch.SearchResultView = Backbone.View.extend({
    tagName:'table',
    className :'aui KeyTable ztable',
    id:'issuetable',

    events:{
    	"click [id^='headerrow-id-exec-']"	: "sortSchedules",
    	"click [id^='scheduleCheck-']" : "selectIds",
		//"click [id^='executionStatus-labels-schedule'], [id^='current-execution-status-dd-schedule-']" :		enableExecutionStatusDropDown,	//Referred from zephyr_common.js,
		"click .execution-field-fixVersion"					:		'displayVersionsOnUnscheduled'
    },

    initialize:function (options) {
        this.model.bind("reset", this.render, this);
		options.dispatcher.on('searchZQLEvent', this.searchZQL, this);
        options.dispatcher.on('performSortEvent', this.performSort, this);
        options.dispatcher.on('scheduleUpdated', this.postDisableRefreshEvent, this);
        options.dispatcher.on('schedulesCountRefresh', this.refreshCount, this);
    },

    /*
     * Hack to display the versions if the version is 'Unscheduled' as the version tab URL has changed in 6.2.
     * TODO: Find an alternative method else we need to update this code every time JIRA updates the version url
     */
    displayVersionsOnUnscheduled: function(ev) {
    	var targetEl 	= ev.currentTarget || ev.srcElement,
    		projectKey 	= AJS.$(targetEl).attr('data-projectKey'),
    		versionHash	= '',
    		href		= '';

    	if(JIRA.Version.compare('6.2') == -1) versionHash = '?selectedTab=com.atlassian.jira.plugin.system.project%3Aversions-panel';
    	else versionHash = '?selectedTab=com.atlassian.jira.jira-projects-plugin:versions-panel';

        href = AJS.contextPath() + '/browse/' + projectKey + versionHash;
		ev.currentTarget.href = href;
    },

    render:function (eventName) {
		//Need to check for previous REL for sortBy before removing the DOM
		var selectedColumnRel = [];
		var columnPrefixes = [];
    	// var columnPrefixes = new Array("headerrow-id-exec-schedule", "headerrow-id-exec-cycle", "headerrow-id-exec-folderName", "headerrow-id-exec-issue", "headerrow-id-exec-test-summary", "headerrow-id-exec-labels", "headerrow-id-exec-project","headerrow-id-exec-component",
		// 		"headerrow-id-exec-priority","headerrow-id-exec-fixVersion","headerrow-id-exec-status","headerrow-id-exec-executedBy","headerrow-id-exec-executionDefectKey","headerrow-id-exec-executedOn","headerrow-id-exec-createdOn","headerrow-id-exec-assignee");
		AJS.$('#issuetable').find('.colHeaderLink.sortable').each(function() {
			columnPrefixes.push(this.id);
		})
		for(var index in columnPrefixes) {
			var colIdentifier = "#" + columnPrefixes[index];
			var rel = AJS.$(colIdentifier).attr("rel");
			selectedColumnRel.push({colIdentifier:colIdentifier,rel:rel});
		}

    	AJS.$(this.el).empty();
    	var body = AJS.$(this.el).find("#resultBodyId");
    	var selectedColumns = [];
    	var filterIdentifier;
    	body.empty();
    	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0 &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes != null &&
                ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
        	    ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length > 0) {
    			if(AJS.$(this.el).html() == null || AJS.$(this.el).html().length == 0) {
    				if(ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models && ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0]) {
    				_.each(ZEPHYR.ZQL.ZQLColumn.data.zqlColumns.models[0].attributes.columnItemBean, function(columnItem) {
                        var isMatching = function(columnName, i18NName){
                            /*For backward compatibility, check i18N name or english Name*/
                            if(columnName == i18NName || ZephyrZQLFilters.COLUMN_NAME_I18N_MAPPING[columnName] == i18NName)
                                return true;
                            return false
                        }
    					if((columnItem.visible)) {
							var isCustomField = false;
    						var columnName = (columnItem.filterIdentifier).replace(/\s/g, ""), columnClass;
    						if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('enav.newcycle.name.label'))) columnName = 'cycleName', columnClass = 'cycle', filterIdentifier = AJS.I18n.getText('enav.newcycle.name.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('enav.newfolder.name.label'))) columnName = 'folderName',columnClass = 'folderName', filterIdentifier = AJS.I18n.getText('enav.newfolder.name.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('cycle.reorder.executions.issue.label'))) columnName = 'Issue', columnClass = 'issue', filterIdentifier = AJS.I18n.getText('cycle.reorder.executions.issue.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('execute.test.testsummary.label'))) columnName = 'summary', columnClass = 'test-summary', filterIdentifier = AJS.I18n.getText('execute.test.testsummary.label');
							else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('issue.field.labels'))) columnName = 'labels', columnClass = 'labels', filterIdentifier = AJS.I18n.getText('issue.field.labels');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('enav.projectname.label'))) columnName = 'project', columnClass = 'project', filterIdentifier = AJS.I18n.getText('enav.projectname.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('project.cycle.addTests.priority.label'))) columnName = 'priority', columnClass = 'priority', filterIdentifier = AJS.I18n.getText('project.cycle.addTests.priority.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('je.gadget.common.component.label'))) columnName = 'component', columnClass = 'component', filterIdentifier = AJS.I18n.getText('je.gadget.common.component.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('je.gadget.common.version.label'))) columnName = 'fixVersion', columnClass = 'fixVersion', filterIdentifier = AJS.I18n.getText('je.gadget.common.version.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('project.cycle.schedule.table.column.executedBy'))) columnName = 'ExecutedBy', columnClass = 'executedBy', filterIdentifier = AJS.I18n.getText('project.cycle.schedule.table.column.executedBy');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('project.cycle.schedule.table.column.executedOn'))) columnName = 'ExecutionDate', columnClass = 'executedOn', filterIdentifier = AJS.I18n.getText('project.cycle.schedule.table.column.executedOn');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('execute.test.executionstatus.label'))) columnName = 'ExecutionStatus',columnClass = 'status', filterIdentifier = AJS.I18n.getText('execute.test.executionstatus.label');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('plugin.license.storage.admin.license.attribute.creationdate.title'))) columnName = 'creationDate',columnClass = 'createdOn', filterIdentifier = AJS.I18n.getText('plugin.license.storage.admin.license.attribute.creationdate.title');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('enav.search.execution.defects'))) columnName = 'executionDefectKey',columnClass = 'executionDefectKey', filterIdentifier = AJS.I18n.getText('enav.search.execution.defects');
    						else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('project.cycle.schedule.table.column.assignee'))) columnName = 'assignee',columnClass = 'assignee', filterIdentifier = AJS.I18n.getText('project.cycle.schedule.table.column.assignee');
                            else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('execute.test.workflow.estimated.time.label'))) columnName = 'estimatedTime',columnClass = 'estimatedTime', filterIdentifier = AJS.I18n.getText('execute.test.workflow.estimated.time.label');
                            else if(isMatching(columnItem.filterIdentifier, AJS.I18n.getText('execute.test.workflow.logged.time.label'))) columnName = 'loggedTime',columnClass = 'loggedTime', filterIdentifier = AJS.I18n.getText('execute.test.workflow.logged.time.label');
    						else {
								columnClass = columnItem.customFieldId;
								filterIdentifier = columnItem.filterIdentifier;
								columnName = columnItem.filterIdentifier;
								isCustomField = true;
							}
							selectedColumns.push({ columnName: columnName, filterIdentifier: filterIdentifier, columnClass: columnClass, isCustomField: isCustomField});
    					}
    				}, this);
          	}
    				AJS.$(this.el).append(ZEPHYR.ZQL.Search.addTableHeaderWithColumns({isEditable:true, selectedColumns: selectedColumns}));
    				for(var index in selectedColumnRel) {
        				if(selectedColumnRel[index].rel != 'undefined') {
							AJS.$(selectedColumnRel[index].colIdentifier).attr("rel",selectedColumnRel[index].rel);
						}
    				}
    				body = AJS.$(this.el).find("#resultBodyId");
    			}
    			AJS.$(".selectAllID").selected(false);
			var offsetX = ((ZEPHYR.ZQL.pagination.offset / ZEPHYR.ZQL.pagination.maxResultAllowed) + 1) || 0;
    			offsetX = (window.isSortingDone) ? 1 : offsetX;
			var queryParam = 'query=' + encodeURIComponent(AJS.$('#zqltext').val() || ZEPHYR.ZQL.ZQLSearch.data.zql) + "&offset=" + offsetX + "&pageWidth=" + ZEPHYR.ZQL.pagination.maxResultAllowed;
    			var executionStatuses = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionStatuses;
				var schedules = JSON.parse(JSON.stringify(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions));
				// var tempSchedules = [];
				// for (var schedulesCounter = schedules.length - 1; schedulesCounter >= 0; schedulesCounter -= 1) {
				// 	tempSchedules.push(schedules[schedulesCounter]);
				// }
				// schedules = tempSchedules;
                schedules.forEach(function(execution) {
                  if(execution.cycleName) {
                    execution.cycleName = execution.cycleName;
                  }
                  if(execution.folderName) {
                    execution.folderName = execution.folderName;
				  }
				  if(execution.customFieldsValueMap) {
					for(var prop in execution.customFieldsValueMap) {
						execution.customFieldsValueMap[prop] = (Number.isNaN(+execution.customFieldsValueMap[prop])) ? execution.customFieldsValueMap[prop] : +execution.customFieldsValueMap[prop]
					}
				};
				});
        		if(body.html() == null || body.html() == "") {
        			body.append(ZEPHYR.ZQL.Search.addTableBodyWithColumns({schedules:schedules,contextPath:contextPath,executionStatuses:executionStatuses,isEditable:true,selectedColumns:selectedColumns,queryParam:queryParam,isGadget: false}));
        		} else {
            	body.html(ZEPHYR.ZQL.Search.addTableBodyWithColumns({schedules:schedules,contextPath:contextPath,executionStatuses:executionStatuses,isEditable:true,selectedColumns:selectedColumns,queryParam:queryParam,isGadget: false}));
        		}
        		AJS.$('#column-selector').show();
        		this.attacExecutionStatusViews();
        		window.isSortingDone = false;
        		return this;
    	} else {
			ZEPHYR.Loading.hideLoadingIndicator(); // Fix for ZFJ-1392
    		AJS.$(this.el).append();
    		AJS.$('#column-selector').hide();
    		return this;
    	}
    },

    toggleBulkToolBar: function() {
    	var selectedExecCount = AJS.$(this.el).find('input.execution-check:checked').length;
    	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds != "undefined" &&
    			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds != null &&
    			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length) {
    		AJS.$("#enavBulkToolId").removeClass("disabled");
    	}
		if (ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
         selectedExecCount == ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length) {
    		AJS.$(this.el).find("#selectAllID").attr("checked", 'checked');
    	}
    },

    attacExecutionStatusViews: function() {
    	// Setting timeout of .5sec for IE, because if the elements are not loaded the view won't get attached.
		setTimeout(function(){
			var editableFields = AJS.$('td.execution-status-td');
			AJS.$.each(editableFields, function(i, $container) {
				var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
					el: 			$container,
					elBeforeEdit:	AJS.$($container).find('#executionStatus-value-schedule'),
					elOnEdit:		AJS.$($container).find('[id^=execution-field-select-schedule-]')
				});
			});

		}, 500);
    },

    selectIds : function(event) {
    	if(event.currentTarget.checked) {
    		if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds != null) {
	    		var arrLength = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length;
	    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds[arrLength]=event.currentTarget.name;
	    		AJS.$("#enavBulkToolId").removeClass("disabled");
    		}
    	} else {
    		var y = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
    		var removeItem = event.currentTarget.name;
    		y = jQuery.grep(y, function(value) {
    			  return value != removeItem;
    		});
    		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds = y;
    		if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length == 0) {
    			AJS.$("#enavBulkToolId").addClass("disabled");
			}
			if (AJS.$(this.el).find("#selectAllID")[0].getAttribute('checked')) {
				AJS.$(this.el).find("#selectAllID").attr("checked", false);
			}
    	}
		this.options.dispatcher.trigger("schedulesCountRefresh",ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds);
    },
    sortSchedules : function(event) {
    	var clickedElement = AJS.$("#"+event.target.id+"");
		var query = this.performSort(clickedElement, AJS.$("#zqltext").val(), AJS.$(this.el),event.target.id, clickedElement.attr("name"));
		var queryParam = window.viewType == 'detail' ? '&view=' + window.viewType + '&offset=0' : '&view=' + window.viewType;
		window.isSortingDone = true;
		this.options.dispatcher.trigger("searchZQLEvent", query, 0, ZEPHYR.ZQL.pagination.maxResultAllowed,event.target.id, clickedElement.attr("name"));
    	ZEPHYR.ZQL.router.navigate("?query=" + encodeURIComponent(query) + queryParam, {trigger:false})
    },

	/**
	* Perform sort by the column user clicked on.
	* e:ClickEvent, element:Parent  activeColPrefix:Column that is clicked, sortQueryPrefix:QueryPrefix to perform sort on, cycleId:Cycle ID
	*/
	performSort : function(e, zqlQuery, element, activeColPrefix, sortQueryPrefix) {
		var activeColumnJQueryElement = AJS.$("#" + activeColPrefix);
		var array = activeColumnJQueryElement.attr('rel').split(':');
        if(array && array[0] && (array[0].indexOf(' ') > -1)) {
            array[0] = "'" + array[0] + "'";
        }
        var tempArray=[];
		var orderByText;
		var tempTxt = "ORDER BY ";
		var indexOfOrderBy = zqlQuery.indexOf(tempTxt); // check order by
		if(indexOfOrderBy != -1) {
			var orderBySubstring = zqlQuery.substring(indexOfOrderBy,zqlQuery.length);
			var orderbyQueryString = zqlQuery.substring(indexOfOrderBy+tempTxt.length,zqlQuery.length);
			tempArray = orderbyQueryString.split(",");
			var zqltext = tempTxt;
			var isExist = false;
			for(var i in tempArray) {
				//Need to compare the entire string
				var exists = tempArray[i].indexOf("DESC");
				if(exists < 1) {
					exists = tempArray[i].indexOf("ASC");
				}
				var prevClause;
				if(exists != -1) {
					prevClause = tempArray[i].substring(0,exists).trim();
				}
				var exists = prevClause == array[0];
				if(exists) {
					var arr = array[0] + " " + array[1];
					tempArray.splice(i,1);
					tempArray.splice(0,0,arr);
					isExist = true;
				}
			}
			if(!isExist) {
				var arr = array[0] + " " + array[1];
				tempArray.splice(0,0,arr);
			}
			for(var i in tempArray) {
				if(i < tempArray.length-1) {
					zqltext +=  tempArray[i] + ",";
				} else {
					zqltext +=  tempArray[i];
				}
			}
			zqlQuery = zqlQuery.substring(0,indexOfOrderBy)  + zqltext;
		} else{
			orderByText = "ORDER BY " + array[0] + " " + array[1];
			zqlQuery += " " + orderByText;
			tempArray[0] = array;
		}
		this.sortBy(e, element,activeColumnJQueryElement);
		AJS.$("#zqltext").val(zqlQuery);
		return zqlQuery;
	},

	//call common method for sorting
	sortBy : function(e,element,changeKey) {
		if(changeKey != null && changeKey.attr('rel') != null) {
			var array = changeKey.attr('rel').split(':')
			if(array.length != 2) {
				return;
			}
			var sortKey = array[0];
			var value = array[1];
		}
	},

	searchZQL : function(zqlQuery, offset, maxRecords, colPrefix, queryPrefix) {
		ZEPHYR.ZQL.ZQLSearch.data.zql = zqlQuery;
		var selectedSchedules = [];
		var instance = this;
		AJS.$("div.navigator-content").html("");
		ZEPHYR.Loading.showLoadingIndicator();
		if (ZEPHYR.ZQL.ZQLSearch.data.searchResults != null && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] != null &&
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] &&
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
				scheduleList = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
			}
		ZEPHYR.ZQL.ZQLSearch.data.searchResults.fetch({
			data:{
				zqlQuery:zqlQuery,
				offset:offset,
				maxRecords: maxRecords,
				expand:"executionStatus"
			},
			contentType:'application/json',
			reset:true,
			success:function() {
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
            }
				if (ZEPHYR.ZQL.maintain == true) {
					var pageWidth = parseInt(allPagesPaginationPageWidth.searchTestExecution);
					var tempOffset = parseInt(offset);
					if (ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length == 0 && (tempOffset - pageWidth) >= 0) {
						offset = parseInt(offset) - parseInt(allPagesPaginationPageWidth.searchTestExecution);

						var params = window.location.hash;
						if (params.indexOf('offset=') >= 0) {
							var tempParam = params.split('offset=');
							if (tempParam[1].indexOf('&') >= 0) {
								var temp = tempParam[1];
								var innerTempParam = temp.split('&');
								innerTempParam[0] = offset;
								tempParam[1] = '';
								for (var paramCounter = 0; paramCounter < innerTempParam.length; paramCounter += 1) {
									tempParam[1] += innerTempParam[paramCounter];
									if (paramCounter + 1 < innerTempParam.length) {
										tempParam[1] += '&';
									}
								}
								params = tempParam[0] + 'offset=' + tempParam[1];
							} else {
								tempParam[1] = offset;
								params = tempParam[0] + 'offset=' + tempParam[1];
							}
						} else {
							params += '&offset=' + offset;
						}

						// var selectedId = ZEPHYR.ZQL.prevNextId.previousId;
						// if (params.indexOf('selectedId=') >= 0) {
						// 	var tempParam = params.split('selectedId=');
						// 	if (tempParam[1].indexOf('&') >= 0) {
						// 		innerTempParam = tempParam[1].split('&');
						// 		innerTempParam[0] = selectedId;
						// 		tempParam[1] = '';
						// 		for (var paramCounter = 0; paramCounter < innerTempParam.length; paramCounter += 1) {
						// 			tempParam[1] += innerTempParam[paramCounter];
						// 			if (paramCounter + 1 < innerTempParam.length) {
						// 				tempParam[1] += '&';
						// 			}
						// 		}
						// 		params = tempParam[0] + 'selectedId=' + tempParam[1];
						// 	} else {
						// 		tempParam[1] = selectedId;
						// 		params = tempParam[0] + 'selectedId=' + tempParam[1];
						// 	}
						// } else {
						// 	params += '&selectedId=' + selectedId;
						// }
						ZEPHYR.ZQL.router.navigate(params, { trigger: false });

						instance.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
					} else {

						ZEPHYR.ZQL.maintain = false;
						var selectedIdFound = false;
						var selectedId = null;
						for (var counter = 0; counter < ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length; counter += 1) {
							if (ZEPHYR.ZQL.prevNextId.currentId == ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions[counter].id) {
								selectedId = ZEPHYR.ZQL.prevNextId.currentId;
								selectedIdFound = true;
								break;
							}
						}
						if (!selectedIdFound) {
							for (var counter = 0; counter < ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length; counter += 1) {
								if (ZEPHYR.ZQL.prevNextId.nextId == ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions[counter].id) {
									selectedId = ZEPHYR.ZQL.prevNextId.nextId;
									selectedIdFound = true;
									break;
								}
							}
						}
						if (!selectedIdFound) {
							for (var counter = 0; counter < ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length; counter += 1) {
								if (ZEPHYR.ZQL.prevNextId.previousId == ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions[counter].id) {
									selectedId =
										ZEPHYR.ZQL.prevNextId.previousId;
									selectedIdFound = true;
									break;
								}
							}
						}
						var params = window.location.hash;
						if (selectedIdFound) {
							if (params.indexOf('selectedId=') >= 0) {
								var tempParam = params.split('selectedId=');
								if (tempParam[1].indexOf('&') >= 0) {
									var innerTempParam = tempParam[1].split('&');
									innerTempParam[0] = selectedId;
									tempParam[1] = '';
									for (var paramCounter = 0; paramCounter < innerTempParam.length; paramCounter += 1) {
										tempParam[1] += innerTempParam[paramCounter];
										if (paramCounter + 1 < innerTempParam.length) {
											tempParam[1] += '&';
										}
									}
									params = tempParam[0] + 'selectedId=' + tempParam[1];
								} else {
									tempParam[1] = selectedId;
									params = tempParam[0] + 'selectedId=' + tempParam[1];
								}
							} else {
								params += '&selectedId=' + selectedId;
							}

							var selectedIdPosition = null;
							var offsetPosition = null;

							if (window.location.hash.indexOf('&offset=' + offset) >= 0) {
								offsetPosition = window.location.hash.indexOf('&offset=' + offset);
							}
							if (window.location.hash.indexOf('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId) >= 0) {
								selectedIdPosition = window.location.hash.indexOf('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId);
							}

							if (offsetPosition && selectedIdPosition) {
								params = params.replace('&offset=' + offset, '');
								params = params.replace('&selectedId=' + selectedId, '');
								// params = params.slice(offsetPosition, ('&selectedId=' + ZEPHYR.ZQL.prevNextId.currentId).length);
								if (offsetPosition > selectedIdPosition) {
									params += '&offset=' + offset + '&selectedId=' + selectedId;
								} else {
									params += '&selectedId=' + selectedId + '&offset=' + offset;
								}
							}

						} else {
							if (params.indexOf('&selectedId=') >= 0) {
								var tempParams = params.split('&selectedId=');
								if (tempParams[1].indexOf('&') >= 0) {
									var innertempParams = tempParams[1].split('&');
									tempParams[1] = '';
									for (var paramCounter = 1; paramCounter < innertempParams.length; paramCounter += 1) {
										tempParams[1] += '&' + innertempParams[paramCounter];
									}
									params = tempParams[0] + tempParams[1];
								} else {
									params = tempParams[0];
								}
							}
						}
						ZEPHYR.ZQL.router.navigate(params, { trigger: true });
						console.log('ZEPHYR.ZQL.ZQLSearch.data.searchResults : ', ZEPHYR.ZQL.ZQLSearch.data.searchResults);
					}

				} else {
					if (ZEPHYR.ZQL.ZQLSearch.data.searchResults != null && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] != null &&
						ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
						ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length == 0) {
						AJS.$("div.navigator-content").addClass("empty-result");
						var message = AJS.I18n.getText('enav.results.none.found');
						AJS.$("div.navigator-content").html(ZEPHYR.ZQL.Search.noResults({ message: message }));
					} else {
						if (ZEPHYR.ZQL.ZQLSearch.data.searchResults != null && ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0] != null &&
							ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
							selectedSchedules = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
						}

						if (selectedSchedules == null || selectedSchedules.length == 0) {
							if (typeof scheduleList != 'undefined') {
								selectedSchedules = scheduleList;
							}
						}
						//set the Checkbox to check previous selected executionIds
						if (selectedSchedules != null && selectedSchedules.length > 0) {
							AJS.$.each(selectedSchedules, function (index, schedule) {
								ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.push(schedule);
								AJS.$('#scheduleCheck-' + schedule).attr('checked', true);
							});
						}
						if (colPrefix != null || colPrefix != undefined) {
							Zephyr.refreshAttributes(colPrefix, queryPrefix);
						}
						JIRA.Dropdowns.bindNavigatorOptionsDds();
						globalDispatcher.trigger('triggerPageNavigation');
						globalDispatcher.trigger("schedulesCountRefresh", ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds);
					}
					// Disable save button for predefined filters
					if (AJS.$("#zqltext").attr("filterId") != "" && AJS.$("#zqltext").attr("filterId") > 0 &&
						AJS.$("#zqltext").val() != AJS.$('#zqltext').attr('data-query')) {
						AJS.$("#zephyr-zql-update-filter").removeAttr('disabled');
					} else {
						AJS.$("#zephyr-zql-update-filter").attr('disabled', 'disabled');
						AJS.$("#zephyr-zql-update-filter").off("click");
					}
					if (window.viewType == 'list')
						ZEPHYR.Loading.hideLoadingIndicator();
					AJS.$("#jqlerrormsg").removeClass("loading");
					AJS.$("#jqlerrormsg").addClass("jqlgood");
					instance.toggleBulkToolBar();
				}
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
	postDisableRefreshEvent : function(event) {
		if(!window.standalone) {
			this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
		ZephyrZQLFilters.findFiltersByUser();
		}
	},
	refreshCount : function(executionIds) {
		AJS.$("#countCheckId").text(AJS.I18n.getText('enav.bulk.select.label',executionIds.length));
		if(executionIds.length > 0) {
			AJS.$("#selectionCountId").show();
		} else AJS.$("#selectionCountId").hide();
	}
});


ZEPHYR.ZQL.ZQLSearch.SearchPaginationView = Backbone.View.extend({
    tagName:'div',
    events:{
    	"click [id^='zql-pagination-']" : 'executePaginatedSearch',
		"click [id^='refreshZQLId']"	: "refreshZQLView",
		"click [id^='change-pagination-width']": "paginationWidthChangeSelected",
		"click .goToPage": "fetchExecutions",
		"click [id^='pagination-zql-next-page-']": "paginationChangeNextPage",
		"click [id^='pagination-zql-prev-page-']": "paginationChangePrevPage",
		"click [id='change-pagination-page-width']": "paginationWidthChange",
    },
    initialize:function (options) {
		this.model.bind("reset", this.render,this);
		//options.dispatcher.on('togglePagination', this.paginationWidthChange, this);
    },
    render:function (eventName) {
		if (AJS.$("#pagination-zql-search-container")) {
			AJS.$("#pagination-zql-search-container").remove();
		}
		AJS.$(this.el).html("");
		if (ZEPHYR.ZQL.ZQLSearch.data.searchResults.models != null &&
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models.length > 0 &&
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes != null &&
            ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions != null &&
			ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executions.length > 0) {
			AJS.$(this.el).append(ZEPHYR.ZQL.Search.addZQLPaginationFooter({
				viewType: window.viewType,
				totalPages: Math.ceil(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount / ZEPHYR.ZQL.pagination.maxResultAllowed),
				entriesCount: ZEPHYR.ZQL.pagination.maxResultAllowed,
				totalCount: ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.totalCount,
				zqlQuery: AJS.$("#zqltext").val(),
				linksNew: ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.linksNew,
				maxAllowed: ZEPHYR.ZQL.pagination.maxResultAllowed,
				currentIndex: ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.currentIndex,
			}));
			return this;
		} else {
			AJS.$(this.el).append();
			return this;
		}
	},

	paginationWidthChange: function (event) {
        if (event) {
		    event.stopPropagation();
            event.preventDefault();
            event.stopImmediatePropagation();

            if (AJS.$("#pagination-zql-pagewidth-dropdown").hasClass('active')) {
                AJS.$("#pagination-zql-pagewidth-dropdown").removeClass('active')
            } else {
                AJS.$("#pagination-zql-pagewidth-dropdown").addClass('active');
            }
        }


	},

	paginationWidthChangeSelected: function (event) {
		// ZEPHYR.ZQL.pagination.maxResultAllowed = event.currentTarget.dataset.entries;

		var sendingData = ZEPHYR.ZQL.allPagesPaginationPageWidth;
		sendingData.searchTestExecution = event.currentTarget.dataset.entries;

		var that = this;
		jQuery.ajax({
			url: getRestURL() + "/preference/paginationWidthPreference",
			type: "PUT",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(sendingData),
			success: function (response) {
				//that.options.dispatcher.trigger("togglePagination");
				ZEPHYR.ZQL.pagination.maxResultAllowed = event.currentTarget.dataset.entries;
				ZEPHYR.ZQL.pagination.offset = 0;
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
				}
				newUrl = splitUrl[0];
				for (var counter = 1; counter < splitUrl.length; counter += 1) {
					newUrl += '&' + splitUrl[counter];
				}
				ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
				that.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);

			}
		});


		//  NEED TO HIT THE PUT/POST API HERE TO SET THE PAGINATION WIDTH
	},

	paginationChangeNextPage: function (event) {
		event.preventDefault();
		event.stopPropagation();
		event.stopImmediatePropagation();
		ZEPHYR.ZQL.pagination.offset = (event.currentTarget.dataset.currentpage) * ZEPHYR.ZQL.pagination.maxResultAllowed;
		var url = window.location.hash;
		var splitUrl = url.split('&');
		var newUrl = '';

		for (var counter = 0; counter < splitUrl.length; counter += 1) {
			if (splitUrl[counter].indexOf('offset=') >= 0) {
				splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + ZEPHYR.ZQL.pagination.offset;
			}
		}
		newUrl = splitUrl[0];
		for (var counter = 1; counter < splitUrl.length; counter += 1) {
			newUrl += '&' + splitUrl[counter];
		}

		ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
		this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
	},

	paginationChangePrevPage: function (event) {
		event.preventDefault();
		event.stopPropagation();
		event.stopImmediatePropagation();
		ZEPHYR.ZQL.pagination.offset = (event.currentTarget.dataset.currentpage - 2) * ZEPHYR.ZQL.pagination.maxResultAllowed;
		var url = window.location.hash;
		var splitUrl = url.split('&');
		var newUrl = '';

		for (var counter = 0; counter < splitUrl.length; counter += 1) {
			if (splitUrl[counter].indexOf('offset=') >= 0) {
				splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + ZEPHYR.ZQL.pagination.offset;
			}
		}
		newUrl = splitUrl[0];
		for (var counter = 1; counter < splitUrl.length; counter += 1) {
			newUrl += '&' + splitUrl[counter];
		}

		ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });
		this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
	},

	fetchExecutions: function (ev) {
		ZEPHYR.ZQL.pagination.offset = (ev.target.dataset.offset === "0") ? 0 : (parseInt(ev.target.dataset.offset) - 1) * ZEPHYR.ZQL.pagination.maxResultAllowed;
        var url = window.location.hash;
        var splitUrl = url.split('&');
        var newUrl = '';

        for (var counter = 0; counter < splitUrl.length; counter += 1) {
            if (splitUrl[counter].indexOf('offset=') >= 0) {
                splitUrl[counter] = splitUrl[counter].split('=')[0] + '=' + ZEPHYR.ZQL.pagination.offset;
            }
        }
        newUrl = splitUrl[0];
        for (var counter = 1; counter < splitUrl.length; counter += 1) {
            newUrl += '&' + splitUrl[counter];
        }

        ZEPHYR.ZQL.router.navigate(newUrl, { trigger: false, replace: true });

		// ZEPHYR.ZQL.pagination.offset = (event.currentTarget.dataset.currentpage - 2) * paginationWidth;
		this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
	},


    executePaginatedSearch : function(event) {
    	var zqlQuery = event.target.attributes['data-query'].value;
		var offset = eval(event.target.attributes['page-id'].value);
		ZEPHYR.ZQL.pagination.offset = offset;
		scheduleList = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds;
		this.options.dispatcher.trigger("searchZQLEvent", zqlQuery, offset, ZEPHYR.ZQL.pagination.maxResultAllowed);

		var zqlQuery = ZephyrZQLFilters.getViewQueryParamFromURL();
		ZEPHYR.ZQL.router.navigate('?' + zqlQuery + 'view=' + window.viewType + '&offset=' + offset, {trigger: false});
		window.offset = offset + 1;
		event.preventDefault();
    },

    refreshZQLView: function(event) {
    	event.preventDefault();
    	Zephyr.unSelectedSchedules();
		this.options.dispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);

    	if(window.viewType == 'detail') {
			var offset = ZEPHYR.ZQL.pagination.offset;
    		var zqlQuery = ZephyrZQLFilters.getViewQueryParamFromURL();
        	ZEPHYR.ZQL.router.navigate('?' + zqlQuery + 'view=' + window.viewType + '&offset=' + (offset), {trigger: true});
    	}
    }
});

function getCustomFieldValueSoy(data) {
    var columnClass = data['columnClass'];
    var customFieldsValueMap = data['customFieldsValueMap'];
    return customFieldsValueMap ? (customFieldsValueMap[columnClass] ? customFieldsValueMap[columnClass] : '') : '';
}

function navigateToExecution(event) {
	var sendingData = ZEPHYR.ZQL.allPagesPaginationPageWidth;
	sendingData.fromPage = 'enav';
	jQuery.ajax({
		url: getRestURL() + "/preference/paginationWidthPreference",
		type: "PUT",
		contentType: "application/json",
		dataType: "json",
		data: JSON.stringify(sendingData),
		success: function (response) {
			window.location = event.dataset.contextpath + '/secure/enav/#/' + event.dataset.id + '?' + event.dataset.query;
			waitTillStandaloneLoad();
		}
	});
}

AJS.$('body').live('click',function(ev) {
    if(AJS.$('.dropDown-container.active').length > 0 || AJS.$('.droplist.active').length > 0 ) {
        AJS.$('.dropDown-container.active, .droplist.active').each(function(){
            AJS.$(this).removeClass('active');
        })
    }
})
