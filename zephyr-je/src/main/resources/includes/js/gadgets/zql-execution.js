GADGET = {};
/**
 * Returns the ajax requests that should be executed prior to calling GADGET.template
 */
GADGET.templateArgs = function(baseUrl) {
	return [
	    {
	    	key: "zqlSearchResponses",
	    	ajaxOptions: function () {
	    		var gadget = this;
                var zfjQueryType = gadgets.util.unescapeString(gadget.getPref("zfjQueryType"));
                if(zfjQueryType == 'zqlFilter'){
                    var filterId = gadgets.util.unescapeString(gadget.getPref("filterId"));
                }else{
                    var zqlQuery = gadgets.util.unescapeString(gadget.getPref("zqltext"));
                }
	    		var offset = gadget.offset;
	    		if(offset == undefined) {
	    			offset = 0;
	    		}
	    		var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));
	    		if(howMany == undefined || howMany == "") {
	    			howMany = 20;
	    		}

                var queryParams = {"zqlQuery": zqlQuery, filterId:filterId, "offset":offset, "maxRecords": howMany, "expand":"executionStatus"};

	    		return {
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/zql/executeSearch",
	    			contentType: "application/json",
                    data: queryParams,
	    			complete: function(xhr, status, response) {
         			   if(xhr.status != 200) {
         				  JE.gadget.zephyrError(xhr, gadget);
         				  gadgets.window.adjustHeight();
         			   }else{
                           JE.gadget.clearError()
                       }
					}
	    		};
	    	}
	    }
	];
};

/**
* Returns the ajax requests that should be executed prior to calling GADGET.descriptor.
* The descriptor will get the results in the args parameter, with each request response added to the "key" field
*/
GADGET.descriptorArgs = function (baseUrl) {
	return [
       {
	    	key: "autocompleteJSON",
	    	ajaxOptions: function () {
	    		var gadget = this;
	    		return {
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/zql/autocompleteZQLJson",
	    			contentType: "application/json",
					complete: function(xhr, status, response) {
        			   if(xhr.status != 200 ){
        				  JE.gadget.zephyrError(xhr, gadget);
        				  gadgets.window.adjustHeight();
          			   }
					}
	    		};
	    	}
	    }
     ];
};

/**
* Configuration screen descriptor for Top Defects Gadget!
*/
GADGET.descriptor = function (gadget, args, baseUrl) {
	var curTitle = gadget.getPref('executionName')
	var currZqlQuery = gadgets.util.unescapeString(gadget.getPref("zqltext"));
	var currFilterName = gadgets.util.unescapeString(gadget.getPref("filterName"));

	/*If pref is default, then internationalize it, else leave whatever user has chosen*/
	if(curTitle == "Execution Details")
		curTitle = gadget.getMsg('je.gadget.zql.filter.title');

	//Add overflow to config to fix gadget view issues
	var config = AJS.$("#config");
	config.css("overflow","scroll");

	/*
	 * Attaches event to the ZQL or Filter Selection.
	 */
	AJS.$('#zfjQueryType').live("change",function(e) {
        if(AJS.$('#zfjQueryType').val() == 'zqlFreeform') {
            AJS.$('#filter_filterId_id').val('');
            AJS.$('#filter_filterId_Name').val('');
            AJS.$('#filterName').val('');
            //AJS.$('#zqltext').val('');
            AJS.$('#zqlfilter_picker_filterId').parent().hide();
            AJS.$('#textfield_zqltext').parent().show();
        } else {
            AJS.$('#zqlfilter_picker_filterId').parent().show();
            AJS.$('#textfield_zqltext').parent().hide();
        }
	});

	AJS.$('#columnNames-add').live('click', function(ev) {
		ev.stopImmediatePropagation();
		var selectedEl 	= AJS.$('select#columnNames-select option:selected');
		var columnLabel = selectedEl.text();
		var columnName 	= selectedEl.val();

		if(columnName != '-1') {
			AJS.$('select#columnNames-select').val('-1');
			selectedEl.remove();
			AJS.$('#column-picker-restful-table tbody').append(
    			'<tr class="aui-restfultable-readonly aui-restfultable-row" data-value="' + columnName + '" data-label="' + columnLabel + '">' +
    				'<td class="aui-restfultable-order"><span class="aui-restfultable-draghandle"></span></td>' +
    				'<td class="column-label" data-label="' + columnName + '">' + columnLabel + '</td>' +
    				'<td class="aui-restfultable-operations"><a title="Delete this field" id="column-picker-delete-issuekey" class="aui-restfultable-delete icon-delete icon" target="_parent"></a></td>' +
    			'</tr>'
    		);
			JE.gadget.fields.updateColumns();
			gadgets.window.adjustHeight();
		}
		if(AJS.$('select#columnNames-select option').length == 1) AJS.$('.columnNames-select-wrapper').hide();
	});

	AJS.$('#column-picker-delete-issuekey').live('click', function(ev) {
		ev.preventDefault();
		ev.stopImmediatePropagation();
		AJS.$('.columnNames-select-wrapper:hidden').show();
		var targetEl = AJS.$(ev.target).parent().parent();
		AJS.$('select#columnNames-select').append(AJS.$("<option/>").attr("value", AJS.$(targetEl).attr('data-value')).text(AJS.$(targetEl).attr('data-label')));
		AJS.$(targetEl).remove();
		JE.gadget.fields.updateColumns();
		gadgets.window.adjustHeight();
	});
	// Restore default columns
    AJS.$('#columns_restore_default').live('click', function(ev) {
    	ev.preventDefault();
    	ev.stopImmediatePropagation();
    	var columnNames = 'cyclename|issuekey|testsummary|labels|projectname|priority|component|version|executionstatus|executedby|executedon|creationdate|executiondefects';
    	AJS.$('#columnNames').val(columnNames);
    	// Hide the column names dropdown and clear the options fields
    	AJS.$('.columnNames-select-wrapper').hide();
    	AJS.$('#columnNames-select option:not(:first)').remove();
    	JE.gadget.fields.displayColumns(gadget, columnNames.split('|'), AJS.$('#groupFld_picker_columnNames'), AJS.$('select#columnNames-select', false));
    	gadgets.window.adjustHeight();
    });

   return {
       theme : function()
       {
           if (gadgets.window.getViewportDimensions().width < 500)
           {
               return "gdt top-label";
           }
           else
           {
               return "gdt";
           }
       }(),
       fields: [
                JE.gadget.fields.title(gadget, "executionName", 'je.gadget.common.title.label', curTitle),
                JE.gadget.fields.zqlFilterPicker(gadget,"zfjQueryType"),
                JE.gadget.fields.filterPicker(gadget, "filterId"),
                JE.gadget.fields.zqlSearchBox(gadget,"zqltext", 255, args.autocompleteJSON, currZqlQuery),
                JE.gadget.fields.filterMaxResultPicker(gadget, "howMany", false),
                JE.gadget.fields.columnSelector(gadget, "columnNames"),
                AJS.gadget.fields.nowConfigured()
           ]
   };
};

GADGET.template = function (gadget, args, baseUrl) {
	var zqlSearchResponse = args.zqlSearchResponses;
	var autocompleteJSONResponse = args.autocompleteJSON;
	gadget.autocompleteJSONResponse=autocompleteJSONResponse;
	var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));
	var filterId = gadgets.util.unescapeString(gadget.getPref("filterId"));
	var zqlQuery = gadget.zqltext;
	if(zqlQuery != undefined || zqlQuery != '') {
		zqlQuery = gadgets.util.unescapeString(gadget.getPref("zqltext"));
		gadget.filterId = '-1';
	}

	// Update the title of the gadget
	var gadgetId = AJS.$(window).attr('name');
	var gadgetTitle = gadget.getPref('executionName');
	JE.gadget.utils.setGadgetTitle(gadgetId, gadgetTitle);

	/**
	 * Renders an error message
	 */
	var showErrorMessage = function(message) {
		gadget.getView().append(AJS.$('<div class="gg-error-message">'+message+'</div>'));
	};

	var bindEvents = function() {
		AJS.$("#zqlPageId").off('click');
		AJS.$("#zqlPageId").click(function(event) {
	        event.preventDefault();
	        var offset = eval(event.target.attributes['page-id'].value);
	        gadget.offset = offset;
	        gadget.zqltext =  event.target.attributes['data-query'].value;
	        gadget.sortByTriggered = null;
	        gadget.showView(true);
	        setTimeout(function() {gadget.resize();}, 500);
	    });

		AJS.$('[id^=headerrow-id-exec-]').off('click');
		AJS.$('[id^=headerrow-id-exec-]').click(function(event){
	        event.preventDefault();
			var clickedElement = AJS.$("#"+event.target.id+"");
			var newZQLQuery = globalDispatcher.trigger("performSortEvent",clickedElement, zqlQuery, AJS.$(this.el),event.target.id, clickedElement.attr("name"));
			gadget.savePref("zqltext", AJS.$('#zqltext').val());
			gadget.offset = 0;
	        gadget.zqltext =  newZQLQuery;
	        gadget.sortByTriggered = true;
	        gadget.sortByRel= AJS.$("#"+event.target.id).attr("rel");
			populateContent(gadget,zqlQuery,zqlSearchResponse, bindEvents, function() {
				gadget.showView(true);
		        setTimeout(function() {gadget.resize();}, 500);
		        AJS.$("#"+event.target.id).attr("rel",gadget.sortByRel);
				Zephyr.refreshAttributes(event.target.id,clickedElement.attr("name"));
			});
		});

		AJS.$("#refreshZQLId").off('click');
		AJS.$("#refreshZQLId").click(function(event) {
	        event.preventDefault();
	        gadget.offset = 0;
	        gadget.zqltext =  zqlQuery;
	        gadget.sortByTriggered = null;
	        gadget.showView(true);
	        setTimeout(function() {gadget.resize();}, 500);
	    });

		AJS.$('#zqltext').off('click');
		AJS.$('#zqltext').live("keypress",function(e) {
	        var $input = AJS.$("#zqltext");
	        // Need to set the height of the input to 0 for expandOnInput to reliably expand.
	        // However, expandOnInput doesn't change the height of empty inputs, so need to handle those a little differently.
	        if ($input.val()) {
	            $input.height(0).expandOnInput();
	            $input.css('overflow','scroll');
	            var suggestions = AJS.$('div.atlassian-autocomplete').find('div.suggestions.dropdown-ready');
	            suggestions.css('top',$input[0].offsetHeight);
	        } else {
	            $input.expandOnInput().height(0).trigger('refreshInputHeight');
	        }
	        gadgets.window.adjustHeight()
		});

		AJS.$("#issuetable").off('click');
		AJS.$("#issuetable").on("click", "a[id^=execution-field-]", function(e) {
			var currTarget = e.currentTarget.id;
			var fieldName = currTarget.split('-');
			var fieldValue = e.currentTarget.title;
			var $row = AJS.$(e.target).parents('tr');
			var project = $row.attr('data-projectkey');
			var fixVersion = $row.attr('data-versionname');
			var component = AJS.$("#execution-field-component-"+fieldName[3]).attr("title");
			var query;
			// ZFJ-1308
			if(fixVersion == 'Unscheduled')
				fixVersion = AJS.I18n.getText("zephyr.je.version.unscheduled");

			if(fieldName[2] == "project") {
				query = fieldName[2]+ '="' + addSlashes(fieldValue) + '" AND fixVersion = "' + addSlashes(fixVersion) + '"';
			} else if (fieldName[2] == 'fixVersion') {
				query = fieldName[2]+ '="' + addSlashes(fieldValue) + '" AND project = "' + addSlashes(project) + '"';
			} else if (fieldName[2] == 'component') {
				query = fieldName[2]+ '="' + addSlashes(fieldValue) + '" AND project = "' + addSlashes(project) + '"';
			} else {
				query = fieldName[2]+ '="' + addSlashes(fieldValue) + '" AND fixVersion = "' + addSlashes(fixVersion) + '" AND project = "' + addSlashes(project) + '"';
			}
			var href = gadget.getBaseUrl() + '/secure/enav/#?query='+encodeURIComponent(query);
			e.currentTarget.href=href;
		});
	};

	if(gadget.sortByTriggered == undefined) {
		populateContent(gadget,filterId,zqlSearchResponse, bindEvents);
	}

	bindEvents();
};

/**
 * Populate Gadget Content
 * @param gadget
 * @param zqlQuery
 * @param zqlSearchResponse
 */
function populateContent(gadget,zqlQuery,zqlSearchResponse, bindEvents, postCb) {
	// empty content and set root classes
	gadget.getView().empty();
	// root element
	var $zWrapper = AJS.$('<div id="zqlExecutionWrapper" style="height:auto;overflow:scroll;"/>').attr('class', 'gg-wrapper');
	gadget.getView().append($zWrapper);
	var xhr = {
			url: gadget.getBaseUrl() + "/rest/"+JE.gadget.utils.getRestResourcePath()+ '/latest/customfield/entity?entityType=EXECUTION',
			method: 'GET',
	};
	customXhrCall(xhr, function (response) {
		var htmlContent = getHtmlContent(gadget,zqlQuery,zqlSearchResponse, response);
		AJS.$(htmlContent).appendTo($zWrapper);
		//binding click event on pagination element after view is rendered
		AJS.$("#zqlPageId").off('click');
		AJS.$("#zqlPageId").click(function(event) {
	        event.preventDefault();
	        var offset = eval(event.target.attributes['page-id'].value);
	        gadget.offset = offset;
	        gadget.zqltext =  event.target.attributes['data-query'].value;
	        gadget.sortByTriggered = null;
	        gadget.showView(true);
	        setTimeout(function() {gadget.resize();}, 500);
	    });
	    if(bindEvents && typeof bindEvents === 'function') {
	    	bindEvents();
	    }
	    if(postCb && typeof postCb === 'function') {
	    	postCb();
	    }
		gadget.resize();
	}, function (response) {
			console.log('fail response : ', response);
	});
}


function addSlashes(str) {
	if(str && isNaN(str)) {
	    return str.replace(/\\/g, '\\\\').
	        replace(/\u0008/g, '\\b').
	        replace(/\t/g, '\\t').
	        replace(/\n/g, '\\n').
	        replace(/\f/g, '\\f').
	        replace(/\r/g, '\\r').
	        replace(/'/g, '\\\'').
	        replace(/"/g, '\\"');
	} else
		return str; // ZFJ-1309
}

/**
 * fetches Html Content for Result Table
 * @param zqlQuery
 * @param resultData
 * @returns {String}
 */
function getHtmlContent(gadget, zqlQuery, resultData, customFieldsResponse) {
	if(resultData.executions.length != 0) {
		var displayHeader = ZEPHYR.ZQL.Search.addZQLPaginationFooterOldUI(
							{totalCount:resultData.totalCount,
							currentIndex:resultData.currentIndex,
							maxAllowed:resultData.maxResultAllowed,
							linksNew:resultData.linksNew,
							zqlQuery:zqlQuery});
		var searchHeader = '<table class="aui KeyTable ztable" id="issuetable">';
		var columnNames = gadgets.util.unescapeString(gadget.getPref('columnNames')).split('|');
		var selectedColumns = [];
		AJS.$(columnNames).each(function(i, columnItem) {
			JE.gadget.fields.columnSelectorGroupOptions.map(function(column) {
				if(column.value == columnItem)
					selectedColumns.push({columnName: column.columnName, filterIdentifier: column.label, columnClass: column.columnClass});
			});
		});

		AJS.$(customFieldsResponse).each(function(customField) {
			if(customFieldsResponse[customField].active && AJS.$.inArray(customFieldsResponse[customField].name, columnNames) != -1) {
				selectedColumns.push({columnName: customFieldsResponse[customField].name, filterIdentifier: customFieldsResponse[customField].name, columnClass: customFieldsResponse[customField].id, isCustomField: true});
			}
		});

		searchHeader += ZEPHYR.ZQL.Search.addTableHeaderWithColumns({isEditable:false, selectedColumns: selectedColumns});
		//searchHeader += ZEPHYR.ZQL.Search.addTableHeader({isEditable:false});
		var executionStatuses = resultData.executionStatuses;
    resultData.executions.forEach(function(execution) {
      if(execution.cycleName) {
        execution.cycleName = execution.cycleName;
      }
      if(execution.folderName) {
        execution.folderName = execution.folderName;
      }

			if(execution.customFieldsValueMap) {
			for(var prop in execution.customFieldsValueMap) {
				execution.customFieldsValueMap[prop] = (Number.isNaN(+execution.customFieldsValueMap[prop])) ? execution.customFieldsValueMap[prop] : +execution.customFieldsValueMap[prop];
			}
		};

    })
		searchHeader += ZEPHYR.ZQL.Search.addTableBodyWithColumns({schedules:resultData.executions,contextPath:gadget.getBaseUrl(),executionStatuses:executionStatuses,isEditable:false,selectedColumns:selectedColumns,queryParam:'',isGadget:true});
		// searchHeader += ZEPHYR.ZQL.Search.addTableBody({schedules:resultData.executions,contextPath:AJS.contextPath(),executionStatuses:executionStatuses,isEditable:false});
		searchHeader += "</table>"
		var result = searchHeader + displayHeader;
		// Adding a hidden zqltext field to store and save the zql when updated.
		result += '<input type="hidden" name="zqltext" id="zqltext" value="' + gadget.getPref('zqltext') + '" />';
		return result;
	} else {
		AJS.$('#zqlExecutionWrapper').removeAttr('style').css('padding', '10px');
		JE.gadget.zephyrWarning(AJS.$('#zqlExecutionWrapper'), AJS.I18n.getText("enav.results.none.found"));
	}
}

// API HITTING FUNCTION
function customXhrCall(xhr, successCallback, errorCallback) {
    var method = '';
    if (!!xhr.data) {
        method = 'POST';
    } else {
        method = 'GET';
    }

    if (!!xhr.method) {
        method = xhr.method;
    }

    ZEPHYR.Loading.showLoadingIndicator();

    AJS.$.ajax({
        url: xhr.url,
        type: method,
        contentType: "application/json",
        data: JSON.stringify(xhr.data),
        Accept: "application/json",
        success: function (response) {
            setTimeout(function() {
                successCallback(response);
                ZEPHYR.Loading.hideLoadingIndicator();
            }, 500);
        },
        error: function (xhr, status, error) {
            if (xhr.status !== 403) {
                console.log('status code : ', xhr.status);
                var errorMsg = xhr.responseText && JSON.parse(xhr.responseText) && JSON.parse(xhr.responseText).error;
                if (errorMsg) {
                    showToastrMsg(errorMsg, 'error');
                }
                errorCallback(xhr);
                ZEPHYR.Loading.hideLoadingIndicator();
            }
            console.log('error', xhr, error);
        },
        statusCode: {
            403: function (xhr, status, error) {
                console.log('status code : 403')
                errorCallback(xhr);
                ZEPHYR.Loading.hideLoadingIndicator();
            }

        }
    });
}
