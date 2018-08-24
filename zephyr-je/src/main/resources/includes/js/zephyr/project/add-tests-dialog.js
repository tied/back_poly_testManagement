/**
 * Creates a Add issue dialog
 *
 * @param {projectId, cycleId, cycleObject}
 */
if(ZEPHYR.Dialogs == undefined){
    ZEPHYR.Dialogs = {}
}
ZEPHYR.Dialogs.createAddTestsDialog = function (e,projectId,cycleId,cycleObj,folderObj, callback) {
	/*Only chrome support const, so using var for XBrowser compatibility*/
	var INDIVIDUALLY = "1";
	var BY_SAVED_SEARCH = "2";
	var FROM_PREVIOUS_CYCLE = "3";
    if(AJS.$('#add-tests-dialog').length) {
        AJS.$('#add-tests-dialog').remove();
    }
	var dialog = new AJS.Dialog({width:810, height:475, id:"add-tests-dialog", closeOnOutsideClick: true});
    var folderId = folderObj ? folderObj.folderId : null;

	// PAGE 0 (first page)
	// adds header for first page
	dialog.addHeader(folderId ? AJS.I18n.getText('folder.add.tests.label') + ": " + folderObj.folderName : AJS.I18n.getText('cycle.add.tests.label') + ": " + cycleObj.name);

	// add panel 1
	dialog.addPanel(AJS.I18n.getText('cycle.add.tests.individually.label'), /*Empty Body*/"", "panel-body", INDIVIDUALLY);
	//dialog.get("panel:0").setPadding(0);

	// add panel 2 (this will create a menu on the left side for selecting panels within page 0)
	dialog.addPanel(AJS.I18n.getText('cycle.add.tests.filter.label'), /*Empty Body*/"", "panel-body", BY_SAVED_SEARCH);

	dialog.addPanel(AJS.I18n.getText('cycle.add.tests.another.cycle.label'), /*Empty Body*/"", "panel-body", FROM_PREVIOUS_CYCLE);

//	dialog.gotoPage(0);
    dialog.gotoPanel(0);

    dialog.addSubmit(AJS.I18n.getText('zephyr.je.add.button.title'), performSave);

	dialog.addLink(AJS.I18n.getText('zephyr.je.submit.form.cancel'), function (dialog) {
		dialog.remove()
	});

    dialog.show();
    /*JIRA 6.0 dialog doesnt resize automatically. Hence adding additional px*/
    ZEPHYR.About.evalMessage({selector:".dialog-title", position:"after"}, null, function(){
    	var evalMessageHeight = dialog.popup.element.find('.zfjEvalLic').height(),
    		paddingTop = parseInt(dialog.popup.element.find('.zfjEvalLic').css('padding-top')) || 0,
    		paddingBottom = parseInt(dialog.popup.element.find('.zfjEvalLic').css('padding-bottom')) || 0,
    		marginTop = parseInt(dialog.popup.element.find('.zfjEvalLic').css('margin-top')) || 0;

    	if(!evalMessageHeight || evalMessageHeight < 40) {
    		evalMessageHeight = 40;
    	}
    	dialog.height +=  evalMessageHeight + paddingTop + paddingBottom + marginTop; //For eval message
    	dialog.popup.element.css('height', dialog.height);
    });
    getPanelContent();

    AJS.$('.button-panel-submit-button').css('padding', '2px 6px 3px')
    AJS.$('#zephyr-je-testkey-multi-select').css('width', '350px') //Making issue key textarea bit bigger (default size is 207)
    AJS.$('.item-button').bind('click', function(event){
    	getPanelContent();
    });

    function getPanelContent(){
    	var selectedButton = AJS.$(dialog.getCurrentPanel().button);
    	//For performance reasons, lets only do it once per panel.
    	if(AJS.$(dialog.getCurrentPanel().body).text() != "") {
    		var _selectedButtonType = '';
    		if(selectedButton[0].id == INDIVIDUALLY)
    			_selectedButtonType = 'individual';
    		else if(selectedButton[0].id == BY_SAVED_SEARCH)
    			_selectedButtonType = 'filter';
    		else if(selectedButton[0].id == FROM_PREVIOUS_CYCLE)
    			_selectedButtonType = 'prev';
    		// Attach Execution Assignee UI events
            // ZEPHYR.Execution.Assignee.init({
            // 	id: 'zephyr-je-execution-assignee-' + _selectedButtonType
            // });
            //return;
    	}

    	AJS.$(dialog.getCurrentPanel().body).empty();
    	if(selectedButton[0].id == INDIVIDUALLY){
    		AJS.$(dialog.getCurrentPanel().body).append(ZEPHYR.Project.Cycle.addTestsToCycleDialog({projectId:projectId, contextPath:contextPath}));
    		AJS.$(document.body).find('.aui-field-tescasepickers').each(function () {
    	    	new JIRA.IssuePicker({
    	                element: AJS.$(this),
    	                userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
    	                uppercaseUserEnteredOnSelect: true
    	        });
    	    });

        	var _assigneeHTML = ZEPHYR.Templates.Execution.Assignee.executionAssigneeView({
        		hasHeader: false,
        		hasLabel: true,
        		id: 'zephyr-je-execution-assignee-individual'
        	});
    		//AJS.$('#add-tests-cycle-assignee-container-individual').html(_assigneeHTML);
            // Attach Execution Assignee UI events
            var _assignee = {
                "executionId": null,
                "assignee": null,
                "assigneeDisplay": null,
                "assigneeUserName": null,
                "assigneeType": null
            };
            AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath, '#zephyr-je-execution-assignee-individual .exec-assignee-wrapper' ] );
            // ZEPHYR.Execution.Assignee.init({
            // 	id: 'zephyr-je-execution-assignee-individual'
            // });
    	}else if(selectedButton[0].id == BY_SAVED_SEARCH){
    		AJS.$(dialog.getCurrentPanel().body).append(ZEPHYR.Project.Cycle.addTestsFromSavedSearchToCycleDialog());
    		AJS.$(document.body).find('.aui-field-filterpicker').each(function () {
    	        new AJS.MultiSelect({
     	           element: AJS.$("#addTestsSavedSearch"),
    	           itemAttrDisplayed: "label",
    	           maxInlineResultsDisplayed: 15,
    	           maxWidth:350,
    	           ajaxOptions: {
    	        	   url:contextPath + "/rest/zephyr/latest/picker/filters",
    	        	   query:"true",
    	        	   formatResponse: function (response) {
    	        		   var ret = [];
    	        		   AJS.$(response).each(function(i, category) {
	        	                var groupDescriptor = new AJS.GroupDescriptor({
	        	                    weight: i, // order or groups in
	        	                    label: category.label
	        	                });

	        	                AJS.$(category.options).each(function(){
	        	                    groupDescriptor.addItem(new AJS.ItemDescriptor({
	        	                        value: this.id, // value of
	        	                        label: this.label, // title
	        	                        html: this.html,
	        	                        highlighted: true
	        	                    }));
	        	                });
	        	                ret.push(groupDescriptor);
	        	            });
	        	            return ret;
    	        	   }
    	           }
    	        });
    	        AJS.$('#addTestsSavedSearch-multi-select').css('width', '350px') //Making issue key textarea bit bigger (default size is 207)
    	        var filterSelect = AJS.$('#addTestsSavedSearch') 	//This select element is same as AJS.Multiselect.model.$element
    	        filterSelect.bind('selected', savedSearchChanged);	//Dont add a change listener, it gets fired twice.
    	        filterSelect.bind('unselect', savedSearchChanged);	//Change event doesnt get fired on deletion


    	    	var _assigneeHTML = ZEPHYR.Templates.Execution.Assignee.executionAssigneeView({
    	    		hasHeader: false,
    	    		hasLabel: true,
    	    		id: 'zephyr-je-execution-assignee-filter'
    	    	});
    			//AJS.$('#add-tests-cycle-assignee-container-filter').html(_assigneeHTML);
    	        // Attach Execution Assignee UI events
                var _assignee = {
                    "executionId": null,
                    "assignee": null,
                    "assigneeDisplay": null,
                    "assigneeUserName": null,
                    "assigneeType": null
                };
                AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath, '#zephyr-je-execution-assignee-filter .exec-assignee-wrapper' ] );
    	        // ZEPHYR.Execution.Assignee.init({
    	        // 	id: 'zephyr-je-execution-assignee-filter'
    	        // });
    		});
    	}else{
			AJS.$("#add-tests-dialog .page-menu-item .item-button").css("pointer-events", "none");
    		fetchDataFrom("/rest/zephyr/latest/util/cycleCriteriaInfo?projectId=" + projectId).done(function(fetchedData){
				var dlist = "";
				AJS.$("#select-version2 optgroup").each(function() {
					dlist += "<optgroup label="+this.label+" >";
					AJS.$(this).find("option").each(function(i){
						dlist += "<option value="+this.value+">"+htmlEncode(this.text)+"</option>";
					});
					dlist += "</optgroup>"
				});

				//Added as part of ZFJ-2174
				fetchedData.executionStatuses.forEach(function(status) {
					if(status.id === -1) {
						status.id = "'-1'";
					}
				});

				var panelBody = ZEPHYR.Project.Cycle.addTestsFromPreviousCycleDialog({ criteriaInfo:fetchedData});

	    		AJS.$(dialog.getCurrentPanel().body).append("<div>" + panelBody + "</div>");
				AJS.$("#addTestsVersion").html(dlist);


	    		/** TO Register Dropdowns as multiSelects ***/
		    		(function () {
		        	    function createPicker($selectField) {
		        	        return new AJS.MultiSelect({
		        	           element: $selectField,
		        	           itemAttrDisplayed: "label",
		        	           errorMessage: "{0} is not a valid " + $selectField.prev().html(),
		        	           maxInlineResultsDisplayed: 15,
		        	           maxWidth:200
		        	        });
		        	    }
		        	    function locateSelect(parent) {
		        	        var $parent = AJS.$(parent),
		        	            $selectField;
		        	        if ($parent.is("select")) {
		        	            $selectField = $parent;
		        	        } else {
		        	            $selectField = $parent.find("select");
		        	        }
		        	        return $selectField;
		        	    }
		        	    var DEFAULT_SELECTORS = [
		        	        "div.aui-field-prioritypicker.frother-control-renderer",  // aui forms
		        	        "div.aui-field-executionstatuspicker.frother-control-renderer",  // aui forms
		        	        "div.aui-field-componentpicker.frother-control-renderer",  // aui forms
		        	        "div.aui-field-labelpicker.frother-control-renderer", // aui forms
		        	        "div.aui-field-statuspicker.frother-control-renderer" // aui forms
		        	    ];

		        	    function findComponentSelectAndConvertToPicker(context, selector) {
		        	        selector = selector || DEFAULT_SELECTORS.join(", ");
		        	        AJS.$(selector, context).each(function () {
		        	            var $selectField = locateSelect(this);
		        	            if ($selectField.length) {
		        	            	createPicker($selectField);
		        	            }
		        	        });
		        	    }

		        	    //JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, context) {
		        	        findComponentSelectAndConvertToPicker('div');
		        	    //});


	        	    	var _assigneeHTML = ZEPHYR.Templates.Execution.Assignee.executionAssigneeView({
	        	    		hasHeader: true,
	        	    		hasLabel: true,
	        	    		id: 'zephyr-je-execution-assignee-prev'
	        	    	});
	        			//AJS.$('#add-tests-cycle-assignee-container-prev').html(_assigneeHTML);
	        	        // Attach Execution Assignee UI events
                        var _assignee = {
                            "executionId": null,
                            "assignee": null,
                            "assigneeDisplay": null,
                            "assigneeUserName": null,
                            "assigneeType": null
                        };
                        AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath, '#zephyr-je-execution-assignee-prev .exec-assignee-wrapper' ] );
	        	        // ZEPHYR.Execution.Assignee.init({
	        	        // 	id: 'zephyr-je-execution-assignee-prev'
	        	        // });
		        	})();

	    		AJS.$('#addTestsVersion').bind('change', getCycles)
				AJS.$('#addTestDefects').bind('change', updateStatusDropdownState)
	    		getCycles();
          AJS.$('#addTestsCycle').bind('change', getFolders);
		  AJS.$("#add-tests-dialog .page-menu-item .item-button").css("pointer-events", "auto");
    		});
    	}
	}

    /**
     * Updates labels for changed savedSearch
     */
    function savedSearchChanged(eve) {
    	var value = (AJS.$("#addTestsSavedSearch").val() || []).join(',');
		if(!value || value.length == 0){
			AJS.$('#savedSearch-description').parent().addClass('hidden');
			AJS.$('#savedSearch-count').parent().addClass('hidden');
			AJS.$('#savedSearch-description').text("");
			AJS.$('#savedSearch-count').text("");
			return;
		}
		fetchDataFrom("/rest/zephyr/latest/test/mySearches/"+value).done(function(fetchedData){
			AJS.$('#savedSearch-description').parent().removeClass('hidden');
			AJS.$('#savedSearch-count').parent().removeClass('hidden');
			AJS.$('#savedSearch-description').text(fetchedData.desc);
			AJS.$('#savedSearch-count').text(fetchedData.count);
		})
    }

    /**
     * Fetches cycles by project and version
     */
    function getCycles(){
    	var versionId = AJS.$("#addTestsVersion option:selected").attr('value');
    	//fetchDataFrom("/rest/zephyr/latest/cycle/get?vid="+ versionId+"&pid=" + projectId).done(function(fetchedData){
    	fetchDataFrom("/rest/zephyr/latest/cycle?versionId="+ versionId+"&projectId=" + projectId).done(function(fetchedData){
    		AJS.$('#addTestsCycle').empty();
    		AJS.$.each(fetchedData, function(key, cycle) {
    			if(cycle.id == cycleId)
    				return true;		//Skip the target cycle
    			if(key != "recordsCount" && key != "offsetCount") {
    				//AJS.$('#addTestsCycle').append(AJS.$("<option/>").val(key).html(AJS.escapeHtml(cycle.name)));
            AJS.$('#addTestsCycle').append(AJS.$("<option value="+ key +">"+AJS.escapeHtml(cycle.name)+"</option>"));
    			}
    		});
        getFolders();
    	});
    }

    function getFolders() {
        var versionId = AJS.$("#addTestsVersion option:selected").attr('value');
        var cycleId = AJS.$("#addTestsCycle option:selected").attr('value');
        if(cycleId == -1) {
            AJS.$('#addTestsFolder').empty();
            AJS.$('#addTestsFolder').append(AJS.$("<option/>").val("-1").html(AJS.escapeHtml("-")));
        }else {
            fetchDataFrom("/rest/zephyr/latest/cycle/"+cycleId+"/folders?versionId="+ versionId+"&projectId=" + projectId +"&limit=1000&offset=0").done(function(fetchedData){
                AJS.$('#addTestsFolder').empty();
                AJS.$('#addTestsFolder').append(AJS.$("<option/>").val("-1").html(AJS.escapeHtml("-")));
                AJS.$.each(fetchedData, function(key,folder) {
                    //AJS.$('#addTestsFolder').append(AJS.$("<option/>").val(folder.folderId).html(AJS.escapeHtml(folder.folderName)));
                    AJS.$('#addTestsFolder').append(AJS.$("<option value="+folder.folderId+">"+AJS.escapeHtml(folder.folderName)+"</option>"));

                });
            });
        }
    }

    function updateStatusDropdownState(e){
    	AJS.$('#addTestsStatus').parent().toggleClass("hidden");
    }

    /**
     * Generic method, calls given url and return deferred promise back.
     */
    function fetchDataFrom(url) {
        var deferred = jQuery.Deferred();
        JIRA.SmartAjax.makeRequest({
            url: contextPath + url,
            complete: function (xhr, textStatus, smartAjaxResult) {
                if (smartAjaxResult.successful) {
                    deferred.resolve(smartAjaxResult.data);
                } else {
                    deferred.reject(JIRA.SmartAjax.buildDialogErrorContent(smartAjaxResult));
                }
            }
        });
        return deferred.promise();
    }

    /**
     * Saved data to server
     */
    function performSave(dialog){
    	var addButton = this


    	var selectedButton = AJS.$(dialog.getCurrentPanel().button);
    	var payload ;
    	if(selectedButton[0].id == INDIVIDUALLY){
    		payload = getParamsForTypedTestcases();
    	}else if(selectedButton[0].id == BY_SAVED_SEARCH){
    		payload = getParamsForSavedSearchTestcases();
    	}else if(selectedButton[0].id == FROM_PREVIOUS_CYCLE){
    		payload = getParamsForTestsFromPreviousCycle();
    	}

        //for analytics
		if (payload.data) {
            var nodeParams = JSON.parse(payload.data)
            var zaObj = {
                'event': ZephyrEvents.ADD_TEST_TO_CYCLE,
                'eventType': 'Click',
                'cycleId': nodeParams.cycleId,
                'projectId': nodeParams.projectId,
                'versionId': nodeParams.versionId
            };
            if (za != undefined) {
                za.track(zaObj, function () {
                    //res
                })
            }
        }
      AJS.$(dialog.popup.element[0]).find('.dialog-panel-body').eq(2).animate({'scrollTop' : 0}, 300);
      if(!payload) {
        return;
      }

      AJS.$(addButton).attr("disabled", "disabled") /*Disabling add button so that it cant be pressed again.*/
      // Attach loading icon
      AJS.$('#' + dialog.id).find('.dialog-button-panel').prepend('<span class="icon throbber loading dialog-icon-wait">Loading...</span>');

    	var ajaxParams = {
    			url: getRestURL() + "/execution/addTestsToCycle",
        		type : "post",
        		contentType :"application/json",
        		dataType: "json",
        		data: payload.data,
		    	success: function(response) {
                    var isAddingTestToFolder = JSON.parse(payload.data).folderId;
					window.onbeforeunload=null;
					selectedVersionChanged();
					dialog.remove();
                    var message = '';
                    if(isAddingTestToFolder) {
                        message = AJS.I18n.getText('zephyr.je.add.tests.to.folder.in.progress');
                    }else {
                        message = AJS.I18n.getText('zephyr.je.add.tests.to.cycle.in.progress');
                    }

					if (response == undefined || response == null){ return;}
					var jobProgressToken = response.jobProgressToken;
					if(response != null) {
						var msgDlg = new JIRA.FormDialog({
							id: "warning-message-dialog",
							content: function (callback) {
								var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
									warningMsg:message,
									progress:0,
									percent:0,
									timeTaken:0
								});
								callback(innerHtmlStr);
							},
						});
						msgDlg.show();
					}

					var intervalId = setInterval(function(){
							jQuery.ajax({url: contextPath + "/rest/zephyr/latest/execution/jobProgress/"+jobProgressToken,
								data: {'type':"add_tests_to_cycle_job_progress"}, complete:function(jqXHR, textStatus){
								var data = jQuery.parseJSON(jqXHR.responseText);
  								AJS.$(".aui-progress-indicator").attr("data-value",data.progress);
								AJS.$(".aui-progress-indicator-value").css("width",data.progress*100+"%");
								AJS.$(".timeTaken").html("Time Taken: "+data.timeTaken);
								var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage: null);
								if (errMsg != null){
									AJS.$("#cycle-aui-message-bar .aui-message").html(errMsg);
									AJS.$(".timeTaken, .aui-progress-indicator ").remove();
									clearInterval(intervalId);
								}
								if(data.totalSteps == 0 && data.progress == 0 && data.completedSteps == 0){
                                    AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);
                                    AJS.$(".timeTaken, .aui-progress-indicator ").remove();
                                    clearInterval(intervalId);
								}
								if(data.progress == 1 && errMsg == null) {
								AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);
								clearInterval(intervalId);
                                if(callback) {
                                    callback.call();
                                }
								return true;
								}
							}
						})
					}, 1000);
					return false;
				},
				error:function(response) {
					AJS.$(addButton).removeAttr("disabled");
		    		var ctx = AJS.$(dialog.getCurrentPanel().body).find("#zephyr-aui-message-bar");
		    		ctx.empty();
					AJS.messages.error(ctx, {
					    title: "Error:",
					    body: "In adding issue to Cycle. " + getErrorMessageFromResponse(response),
					    closeable: true
					});
					AJS.$('#' + dialog.id).find('.dialog-icon-wait').remove(); // Remove loading icon
					AJS.$('.button-panel-submit-button', dialog.$form).removeAttr('disabled');	//Enable the Add button for user to retry
					return false;	//This will prevent default event from propagating
				}
    	}
    	jQuery.ajax(ajaxParams);
    }

    /**
     * Add typed testcase IDs to selected cycle
     */
    function getParamsForTypedTestcases() {
    	var value = [],
    		_typedTestcasesParams,
    		_assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('zephyr-je-execution-assignee-individual');

    	AJS.$('ul.items li.item-row').each(function() {
    		var val = AJS.$(this).find("button.value-item span.value-text");
    		value.push(val.text());
    	});
    	_typedTestcasesParams = {
	  	          'issues': value,
		          'versionId': cycleObj.versionId,
		          'cycleId': cycleId,
		          'projectId' : projectId,
		          'method':INDIVIDUALLY
			};
      if(folderId) {
        _.extend(_typedTestcasesParams, {'folderId': folderId});
      }
      if(_assigneeParams.assigneeType && _assigneeParams.assigneeType !== 'null' && _assigneeParams.assigneeType !== 'undefined'){
        _.extend(_typedTestcasesParams, _assigneeParams); // Merge assigneeParams to typed test case params
      }

      if(_assigneeParams === false) {
        return;
      }
    	return {
    		data: JSON.stringify(_typedTestcasesParams)
    	}
    }

    function getParamsForSavedSearchTestcases() {
    	var _savedSearchParams,
			_assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('zephyr-je-execution-assignee-filter');

    	_savedSearchParams = {
  	          'searchId': (AJS.$("#addTestsSavedSearch").val() || []).join(","),
	          'versionId': cycleObj.versionId,
	          'cycleId': cycleId,
	          'projectId' : projectId,
	          'method':BY_SAVED_SEARCH
		};
      if(folderId) {
        _savedSearchParams.folderId = folderId;
      }
      if(_assigneeParams.assigneeType && _assigneeParams.assigneeType !== 'null' && _assigneeParams.assigneeType !== 'undefined'){
        _.extend(_savedSearchParams, _assigneeParams); // Merge assigneeParams to saved search test case params
      }

      if(_assigneeParams === false) {
        return;
      }
    	return {
    		data: JSON.stringify(_savedSearchParams)
    	}
    }

    function getParamsForTestsFromPreviousCycle(){
    	var fromVersionId = AJS.$("#addTestsVersion option:selected").attr('value');
		var fromCycleId = AJS.$("#addTestsCycle option:selected").attr('value');
		var priorityId = (AJS.$("#addTestsPriority").val() || []).join(',');
		var statusId = (AJS.$("#addTestsExecutionStatus").val() || []).join(',');

		// Added as part of ZFJ-2174
		if(statusId.indexOf('-1') != -1) {
			statusId = statusId.replace("'-1'","-1");
		}

		var componentId = (AJS.$("#addTestsComponent").val() || []).join(',');
		var labels = (AJS.$("#addTestsLabels").val() || []).join(',');
		var defects = AJS.$("#addTestDefects").is(":checked");
		var addCustomFields = AJS.$("#addCustomFields").is(":checked");
		var _assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('zephyr-je-execution-assignee-prev');
		var fixed, _prevCycleParams;
        var fromFolderId = AJS.$("#addTestsFolder option:selected").attr('value');

		if(defects)
			fixed = (AJS.$("#addTestsStatus").val() || []).join(',');

		if(fromFolderId == '-1') {
		    fromFolderId = null;
        }

		_prevCycleParams = {
			'versionId': cycleObj.versionId,
			'fromVersionId': fromVersionId,
			'cycleId': cycleId,
			'fromCycleId': fromCycleId,
			'priorities': priorityId,
			'statuses': statusId,
			'components': componentId,
			'labels': labels,
			'hasDefects': defects,
			'withStatuses': fixed,
			'projectId' : projectId,
			'fromFolderId' : fromFolderId,
			'method':FROM_PREVIOUS_CYCLE,
			'addCustomFields': addCustomFields
		};

    if(folderId) {
        _prevCycleParams.folderId = folderId;
      }
    if(_assigneeParams.assigneeType && _assigneeParams.assigneeType !== 'null' && _assigneeParams.assigneeType !== 'undefined'){
       _.extend(_prevCycleParams, _assigneeParams); // Merge assigneeParams to saved search test case params
    }

    if(_assigneeParams === false) {
        return;
      }
		return {
    		data: JSON.stringify(_prevCycleParams)
    	}
    }
}
