/**
 * @namespace ZEPHYR.Cycle
 */

AJS.$.namespace("ZEPHYR.Cycle");

function selectedVersionChanged() {
	versionChanged(0);
}

function versionChanged(startFrom) {
	var instance = this;
	var versionSelectBox = AJS.$("#select-version2")[0];
	AJS.$(versionSelectBox).next('span.icon').addClass('loading');

	var versionId;
	/* Bug in Chrome, where single select looses its selected option.
	 * Hence if no selected option, adding the class="active-version" to the selected element in html and retrieving the version from it.
	 * If there is no active class attached, then select the first option.
	 * Note: the class="active-version" is assigned only once in vm file and not updated throughout the code as
	 * the bug occurs only on click of back button and the selected version is assigned with class="active-version".
	 */
	if(!AJS.$("#select-version2 option:selected") || AJS.$("#select-version2 option:selected").length == 0) {
		var selectedVersion = AJS.$("#select-version2 option.active-version").val() || AJS.$("#select-version2 option").first().val();
		AJS.$("#select-version2").val(selectedVersion);
		ZEPHYR.Cycle.selectedVersion = selectedVersion;
	} else {
		ZEPHYR.Cycle.selectedVersion = AJS.$("#select-version2 option:selected").val();
		if(ZEPHYR.Cycle.selectedVersion == -1) {
			var activeVersion = AJS.$("#select-version2 option.active-version").val();
			if(activeVersion) {
				ZEPHYR.Cycle.selectedVersion = activeVersion;
				AJS.$("#select-version2 option.active-version").attr('selected', 'selected').removeClass('active-version');
			}
		}
	}

	if(versionSelectBox.options[versionSelectBox.selectedIndex].value != '0')
		versionId = versionSelectBox.options[versionSelectBox.selectedIndex].value;
	var offset = startFrom;
	var processCycleRetrieveSuccess = function(response){
		 AJS.$(versionSelectBox).next('span.icon').removeClass('loading');
		 AJS.$("#project-panel-cycle-summary").empty();
		 AJS.$("#project-panel-cycle-list-summary").empty();
		 var cycleList = generateCycleList(response);
		 console.log('------------------', response);
		 AJS.$("#project-panel-cycle-list-summary").append(cycleList);
		 var arrSchedProperties = [];

		 AJS.$('#project-panel-cycle-list-summary li.expando').each(function(){
		 	var that = AJS.$(this);
		 	if(!(that.find('.versionBanner-description').html())){
		 		that.find('.versionBanner-name').css({'max-width': '68%', 'width': 'auto'});
		 		that.find('.versionBanner-description').css('width', '0%');
		 	}
		 });

		 /*We need to iterate thro' it one more time, as jquery selectors only work after new components are added to DOM*/
		 for(var key in response){
			 var cycleOperationDiv = AJS.$("#cycle-"+ key +"-operations");
			 var dropdown = new AJS.Dropdown({
		            trigger: cycleOperationDiv.children(".cycle-operations-trigger"),
		            content: cycleOperationDiv.children(".cycle-operations-list")
		        });
			 	cycleOperationDiv.find(".cycle-operations-edit").bind("click", (function(cycleId, cycleObj){
	        		return function(e){
	        			editCycle(e, cycleId, cycleObj);
		        		e.preventDefault();
	        		}
	        	})(key, response[key]));
	        	cycleOperationDiv.find(".cycle-operations-delete").bind("click", (function(cycleId, cycleObj){
	        		return function(e){
	        			deleteConfirmationDialog(cycleId, cycleObj);
		        		e.preventDefault();
	        		}
	        	})(key, response[key]));
	        	cycleOperationDiv.find(".cycle-operations-export").bind("click", (function(cycleId, cycleObj){
			 		return function(e){
		 				var pid = AJS.$("#zprojectId").val();
		                var vid = AJS.$("#select-version2").val();
		                var elID = ('-cycle-' + cycleId);
		            	AJS.$('#cycle-wrapper-' + cycleId).show();
		            	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
		                jQuery.ajax({
		            		url: getRestURL() + "/cycle/" + cycleId + "/export?projectId=" + pid + "&versionId="+vid,
		            		type: "get",
		            		contentType: "application/json",
		            		global:false,
		            		success: function(response) {
				                AJS.$('#csvDownloadFrame').attr('src',response.url);
				        		AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
		            		},
		            		error : function(response,jqXHR) {
		                		showZQLError(jqXHR);
		                		AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
		            		}
		            	});
			 			e.preventDefault();
			 		}
			 	})(key, response[key]));
			 	cycleOperationDiv.find(".cycle-operations-add-tests").bind("click", (function(cycleId, cycleObj){
	        		return function(e){
	        			addTestsToCycle(e, AJS.$("#zprojectId")[0].value,cycleId, cycleObj);
		        		e.preventDefault();
	        		}
	        	})(key, response[key]));
			 	cycleOperationDiv.find(".cycle-operations-clone-cycle").bind("click", (function(cycleId, cycleObj){
			 		return function(e){
			 			cloneCycle(e, AJS.$("#zprojectId")[0].value, cycleId, cycleObj);
			 			e.preventDefault();
			 		}
			 	})(key, response[key]));
			 	cycleOperationDiv.find(".cycle-operations-reorder-executions").bind("click", (function(cycleId, cycleObj){
			 		return function(e){
			 			reorderExecutions(e, AJS.$("#zprojectId")[0].value, cycleId, cycleObj);
			 			e.preventDefault();
			 		}
			 	})(key, response[key]));
			 	cycleOperationDiv.find(".cycle-operations-revert-executions-order").bind("click", (function(cycleId, cycleObj){
			 		return function(e){
			 			e.preventDefault();
			 			// TODO: Update the label in properties file
			 			//ZEPHYR.Cycle.updateCycleExecutionsUIOnReorder(cycleObj.soffset, cycleId);
			 		}
			 	})(key, response[key]));
	        	//Create Key Value Pair with Schedule for Pagination and Sorting
	    		if(key != "recordsCount" && key != "offsetCount") {
	    			var offset = response[key].soffset;
	    			var sortQuery = response[key].sortQuery;
	    			if(offset == null) {
	    				offset = 0;
	    			}
	    			var obj = {
   				    'offset': parseInt(offset),
   				    'sortQuery': sortQuery,
   				    'lastSortQuery' : sortQuery
   				};
	    			arrSchedProperties[key] = obj;
	    		}
		 }
		 var expandos = ZEPHYR.Cycle.expandos();
		 expandos.arrSchedProperties = arrSchedProperties;

		 AJS.$('.zcycle-header').each(function(i, cycleDiv){
			 var linkAnchor = AJS.$(cycleDiv).children("a");
            if(linkAnchor.attr("href").indexOf("expand") > -1){
           	 /*Click will perform expand operation (will convert expand into collapse)*/
                AJS.$(cycleDiv).trigger("click");
            }else{
            	/*Replace the server collapse state with client expand action*/
            	linkAnchor.attr("href", linkAnchor.attr("href").replace("action=collapse", "action=expand"));
		 	}
        });

		/* This gets called when schedule is quick executed.
			This fetches cycleHeader and refreshes the html
		 */
		AJS.$('li[id^=cycle]').die('refresh')
		AJS.$('li[id^=cycle]').live('refresh', function(event){
			var instance = this;
			var cycleId = AJS.$(this).attr('id').split('cycle-')[1];
			fetchCycleData(cycleId, function(response){
				response[cycleId].action = response[cycleId].action == "expand" ? "collapse" : "expand" //Reverse state -> Cycle state on server into action needed on click
				var cycleHeaderHtml = generateCycleList(response);
				/*We are only replacing chart table, so that already registerd triggers and listeners still continue to work*/
				AJS.$(instance).find('.zcycle-header .versionProgress table').replaceWith(AJS.$(cycleHeaderHtml).find('.zcycle-header .versionProgress table'));
			})
		})
	}
	var fetchCycleData = function(cycleId, successCallback){
		var projectId = AJS.$("#zprojectId")[0].value;
		//var fetchData = {'projectId' : AJS.$("#zprojectId")[0].value, 'versionId' :  versionId, 'offset': offset}; //Offset for cycle is currently not used
		var fetchData = {'projectId' : projectId, 'versionId' :  versionId, 'offset': offset, expand : 'executionSummaries'};
		var elID = '-project-' + projectId;
		if(cycleId) {
			fetchData['id'] = cycleId;
			elID = ('-cycle-' + cycleId);
		}

		ZEPHYR.Cycle.attachPermissionActiveClass(elID);
		jQuery.ajax({
			url: getRestURL() + "/cycle",
			type : "get",
			contentType :"application/json",
			dataType: "json",
			data: fetchData,
			global: false,
			success : function(response) {
				successCallback(response);
				AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
			},
			error : function(response) {
				AJS.$(versionSelectBox).next('span.icon').removeClass('loading');
				AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
				jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			}
		});
	}

	//fetchCycleData(null, processCycleRetrieveSuccess);

	/*In lack of model layer, persisting data by passing thro' functions*/
	var deleteConfirmationDialog = function (cycleId, cycleObject) {
        var instance = this,
        dialog = new JIRA.FormDialog({
            id: "cycle-" + cycleId + "-delete-dialog",
            content: function (callback) {
            	/*Short cut of creating view, move it to Backbone View and do it in render() */
            	var innerHtmlStr = ZEPHYR.Project.Cycle.deleteCycleConfirmationDialog({cycle:cycleObject});
                callback(innerHtmlStr);
            },

            submitHandler: function (e) {
            	deleteCycle(cycleId, dialog, function () {
            		dialog.hide();
                });
                e.preventDefault();
            }
        });

        dialog.show();
    }

	var deleteCycle = function(cycleId, deletionDialog, completed){
		var elID = ('-cycle-' + cycleId);
    	AJS.$('#cycle-wrapper-' + cycleId).show();
    	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
		jQuery.ajax({
			url: getRestURL() + "/cycle/" + cycleId,
			type : "delete",
			contentType :"application/json",
			dataType: "json",
			success : function(response) {
				var msg = AJS.I18n.getText('zephyr.je.bulk.cycle.delete.in.progress');
				var auiclass = 'info';
				if (response != null && response.PERM_DENIED){
					msg = response.PERM_DENIED;
					auiclass = 'error';
				}
				/*Full server refresh - or shall we just remove the corresponding cycle div?*/
				var jobProgressToken = response.jobProgressToken;
				if(response != null) {
					var msgDlg = new JIRA.FormDialog({
						id: "warning-message-dialog",
						content: function (callback) {
							var innerHtmlStr = ZEPHYR.Project.Cycle.deleteCycleConfirmatedDialog({
								warningMsg:msg,
								progress:0,
								percent:0,
								auiclass:auiclass,
								timeTaken:0
							});
							callback(innerHtmlStr);
						},
					});
					msgDlg.show();
					AJS.$("#cycle-delete-form-submit, #unresolved-ignore-label").remove();
					AJS.$(".cycle-delete").html(AJS.I18n.getText('zephyr-je.close.link.title'));
					if (response.PERM_DENIED){
						AJS.$(".timeTaken, .aui-progress-indicator ").remove();
						return;
					}
				}
				var intervalId = setInterval(function(){
					jQuery.ajax({url: contextPath + "/rest/zephyr/latest/execution/jobProgress/"+jobProgressToken,
						data: {'type':"cycle_delete_job_progress"}, complete:function(jqXHR, textStatus){
							if (jqXHR != undefined && jqXHR != null) {
								var data = jQuery.parseJSON(jqXHR.responseText);
								AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
								AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
								AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
								var errMsg = ((data.errorMessage != undefined && data.errorMessage.length > 0) ? data.errorMessage: null);
								if (errMsg != null){
									AJS.$("#cycle-delete-aui-message-bar .aui-message").html(errMsg);
									AJS.$(".timeTaken, .aui-progress-indicator ").remove();
									clearInterval(intervalId);
								}

								if(data.progress == 1 && errMsg == null) {
									if (data.message != undefined && data.message != null) {
										var message = JSON.parse(data.message);
										var msg = message.success ? message.success: message.error;
										AJS.$("#cycle-delete-aui-message-bar .aui-message").html(msg);
									}
									clearInterval(intervalId);
									instance.selectedVersionChanged();
 								}
								if(completed)
									completed.call();
								AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
							}
						}
					})
				}, 1000);

			},
			error : function(response) {
				var cxt = AJS.$("#cycle-aui-message-bar");
    			cxt.empty();
    			AJS.messages.error(cxt, {
    				title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
    			    body: AJS.I18n.getText("zephyr.je.submit.form.error.message", "cycle") + response.responseText,
    			    closeable: false
    			});
    			AJS.$(':submit', deletionDialog.$form).removeAttr('disabled');
    			deletionDialog.$form.removeClass("submitting");
    			AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
			}
		});
	}

	var editCycle = function(event, cycleId, cycleObj){
		var projectId =cycleObj.projectId;
		editCycleDialog(projectId, ZEPHYR.Cycle.versionList, cycleId, cycleObj, ZEPHYR.Cycle.sprintList);
		event.preventDefault();
		return;
	}

	var addTestsToCycle = function(e, projectId,cycleId, cycleObj){
		ZEPHYR.Dialogs.createAddTestsDialog(e,projectId,cycleId,cycleObj);
		e.preventDefault();
		return;
	}

	var cloneCycle = function(event, projectId, cycleId, cycleObj){
		var lastSelectedVersion = AJS.$("#select-version2 :selected").val();
		var cycleObjClone = jQuery.extend({}, cycleObj);
		cycleObjClone.id = cycleId;
		cycleObjClone.name = AJS.I18n.getText('teststep.operation.clone.step.prefix').toUpperCase() + " - " + cycleObjClone.name;
		ZEPHYR.Cycle.CreateCycleFunction.createCycleDialog(projectId, ZEPHYR.Cycle.versionList, lastSelectedVersion.toString(), cycleObjClone);
		event.preventDefault();
		return;
	}

	var reorderExecutions = function(event, projectId, cycleId, cycleObj) {
		var action = 'collapse';
		var CONST = ZEPHYR.Cycle.expandos();
		if(CONST.arrSchedProperties[cycleId] && CONST.arrSchedProperties[cycleId].action && CONST.arrSchedProperties[cycleId].action == 'expand')
			action = 'expand';
		JIRA.Loading.showLoadingIndicator();
		// currently default sorter is 'OrderId:ASC'
		if(!CONST.arrSchedProperties[cycleId].lastSortQuery || CONST.arrSchedProperties[cycleId].lastSortQuery == 'undefined')
			CONST.arrSchedProperties[cycleId].lastSortQuery = 'OrderId:ASC';
		ZEPHYR.Cycle.reorderExecutions.url = getRestURL() + "/execution?cycleId=" + cycleId + "&action=" + action +
			"&offset=0&sorter=" + CONST.arrSchedProperties[cycleId].lastSortQuery + "&" + CONST.requestParams + "&projectId=" + projectId + "&versionId=" + cycleObj.versionId + '&expand=reorderId';
		var elID = ('-cycle-' + cycleId) || '-project-' + projectId;
    	AJS.$('#cycle-wrapper-' + cycleId).show();
    	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
		ZEPHYR.Cycle.reorderExecutions.fetch({
			success: function(collection, response) {
				collection.sort();
				AJS.$.each(collection.models, function(i, model) {
					model.set('position', i);
					model.set('prevOrderId', model.get('orderId'));
				});
				var reorderExecutionsView = new ZEPHYR.Cycle.ReorderExecutionsView({
					collection: 	collection,
					cycleId: 		cycleId,
					versionId: 		cycleObj.versionId,
					soffset:		cycleObj.soffset,
					status:			response.status

				});
				AJS.$('body').append(reorderExecutionsView.render().el);
				reorderExecutionsView.displayReorderExecutionsDND();
				AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
			},
			error: function(collection, response) {
				JIRA.Loading.hideLoadingIndicator();
				var auiMessageBar = AJS.$("<div class='zephyr-aui-message-bar' id='aui-message-bar'/>").prependTo("#project-tab");
		    	AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
		    	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
		    	    body: response.responseText
		    	});
		    	setTimeout(function(){
		    		auiMessageBar.fadeOut(1000, function(){
						auiMessageBar.remove();
					});
				}, 2000);
		    	AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
			}
		});
	}
}

/*In lack of model layer, persisting data by passing thro' functions*/
var editCycleDialog = function (projectId,versionList,cycleId,cycleObj,sprintList) {
    dialog = new JIRA.FormDialog({
        id: "edit-cycle-dialog",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
        	//var innerHtmlStr = ZEPHYR.Project.Cycle.editCycleDialog({projectId:projectId,versions:versionList,cycle:cycleObj, sprints: sprintList.values});
        	var innerHtmlStr = ZEPHYR.Project.Cycle.editCycleDialog({projectId:projectId,versions:versionList,cycle:cycleObj, sprints: []});
        	callback(innerHtmlStr);
        	new AJS.SingleSelect({
                element: AJS.$('#cycle_sprint'),
                maxInlineResultsDisplayed: 15,
                maxWidth: 250
            });
        },

        submitHandler: function (e) {
        	// Attach Loading icon
        	dialog.$popupContent.find('.buttons-container').append('<span class="icon throbber loading dialog-icon-wait dialog-icon-wait-top">Loading...</span>');
          	editCycle(projectId, cycleId, cycleObj.sprintId, function () {
        		dialog.hide();
            });
            e.preventDefault();
        }
    });
    dialog.show();
}

var editCycle = function(projectId, cycleId, sprintId, completed) {
	var elID = ('-cycle-' + cycleId);
	AJS.$('#cycle-wrapper-' + cycleId).show();
	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
	jQuery.ajax({
		//url: getRestURL() + "/cycle/update",
		url: getRestURL() + "/cycle",
		type : "PUT",
		accept: "PUT",
		contentType :"application/json",
		dataType: "json",
		data: JSON.stringify( {
			  'id' : cycleId,
			  'name' : AJS.$('#cycle_name').val(),
	          'build' :  AJS.$('#cycle_build').val(),
	          'environment': AJS.$('#cycle_environment').val(),
	          'description' : AJS.$('#cycle_description').val(),
	          'startDate': AJS.$('#cycle_startDate').val(),
	          'endDate': AJS.$('#cycle_endDate').val(),
	          'projectId': projectId,
	          'versionId': AJS.$('#cycle_version').val(),
	          'sprintId': sprintId,
	          'issueId': AJS.$("#issueId").val()
		}),
		success : function(response) {
			selectedVersionChanged();
			if(completed)
				completed.call();
			AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
		},
		error : function(response) {
			validateCycleInput(response);
			AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
		}
	});
}

// function generateCycleList(response){
// 	var cycleList = "";
// 	var url = getRestURL() + "/execution";
//     var limit=10;
//     var offset = response.offsetCount+1;
//     for(var key in response) {
// 		if(key != "recordsCount" && key != "offsetCount") {
// 			//Get the Execution Status in
// 			var summaries = response[key].executionSummaries;
// 			var summaryList = summaries.executionSummary;
// 			var action = response[key].action;
// 			var soffset = response[key].soffset;
// 			var sortQuery = response[key].sortQuery;
// 			var sprintId = response[key].sprintId;

// 			if(sortQuery == null || sortQuery == undefined || sortQuery == '' || sortQuery == 'undefined') {
// 				sortQuery = "OrderId:ASC";		// Setting default sort query to OrderId:ASC[order By orderId].
// 			}

// 			if(soffset == null) {
// 				soffset = 0;
// 			}
// 			/*If action is not expand, we convert it to empty. This will get replaced with collapase after auto reopening is performed*/
// 			if(!action){
// 				action = "collapse";
// 			}
// 			// zqlQuery
// 			var fixVersion 	= AJS.$('#select-version2 option:selected').attr("title");
// 			var queryParam 	= 'project = "' + addSlashes(response[key].projectKey) + '" AND fixVersion = "' + addSlashes(fixVersion) + '" AND cycleName in ("' + addSlashes(response[key].name) + '")';
// 			var zqlQuery 	= 'query=' + encodeURIComponent(queryParam);

// 			cycleList += ZEPHYR.Project.Cycle.cycleHeader({ctx:contextPath, cycle:response[key], cycleId:key, restUrl:url, offset:soffset,summaryList:summaryList,action:action,sorter:sortQuery,zqlQuery:zqlQuery});
// 		}
// 	}

// 	return cycleList;
// }

function addSlashes(str) {
	if(str) {
	    return str.replace(/\\/g, '\\\\').
	        replace(/\u0008/g, '\\b').
	        replace(/\t/g, '\\t').
	        replace(/\n/g, '\\n').
	        replace(/\f/g, '\\f').
	        replace(/\r/g, '\\r').
	        replace(/'/g, '\\\'').
	        replace(/"/g, '\\"');
	}
}

function getPercentage(part, total){
	if(total == 0 || total < part){
		return 0;
	}
   var particularTestCountPercentage = (part * 100)/total;
   return particularTestCountPercentage;
}


// Triggers toggle
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
    // Toggles
    var toggle = new JIRA.ToggleBlock({
        blockSelector: ".toggle-wrap",
        triggerSelector: ".mod-header h3",
        cookieCollectionName: "block-states",
        originalTargetIgnoreSelector: "a"
    });

	//This will add click event handler on table level and will in turn pass it to right status dropdown list!
	//That way we don't have to add listeners for each status dropdown!
	// AJS.$("#project-panel-cycle-list-summary").delegate('[id^="executionStatus-value-"]', 'click', enableExecutionStatusDropDown);
});


/*
 * On return to test cycle, the executionStatus events were not delegated in JIRA 6.2rc1.
 * So delegating the events after execution status in DOM is loaded.
 */
var delegateExecuteStatus = function(cycleId) {
	/*
	AJS.$("#project-panel-cycle-list-summary").undelegate('[id^="executionStatus-value-"]', 'click', enableExecutionStatusDropDown);
	AJS.$("#project-panel-cycle-list-summary").delegate('[id^="executionStatus-value-"]', 'click', enableExecutionStatusDropDown);*/
	// Create editable view for status field.
	var editableFields = AJS.$('div.field-group.execution-status-container');
	AJS.$.each(editableFields, function(i, $container) {
		var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
			el: 			$container,
			elBeforeEdit:	AJS.$($container).find('[id^=execution-field-current-status-schedule-]'),
			elOnEdit:		AJS.$($container).find('[id^=execution-field-select-schedule-]')
		});
	});
}

ZEPHYR.Cycle = (function(){

	var permissionsCheck = function(xhr, response, url) {
        var loginPageRedirect = xhr && xhr.getResponseHeader('X-Atlassian-Dialog-Control') == 'permissionviolation';
        var permissionError = response && response.permissionError;
        if (loginPageRedirect || permissionError) {
            window.location.href = url.replace(/\?.*/,"");
            return false;
        } else {
            return true;
        }
    };

	return {
	permissionsCheck: permissionsCheck,
	/**
	 * Singleton that handles async expanding fragments
	 * @function {Public} expandos
	 * @returns {Function}
	 */
	expandos: function () {
	    var CONST = {
	    	offset: 0,
	        containerSelector: "li.expando", // this is where the click listener will be
	        linkSelector: ".versionBanner-link", // we will use this link's "href" attribute to make the request
	        contentClass: "versionBanner-content", // we will inject the html fragment here
	        activeClass: "active", // applied to container when expanded
	        tweenSpeed: "fast", // speed of expand/contract
	        requestParams: "decorator=none&contentOnly=true&noTitle=true", // params to ensure response is not decorated with furniture
	        collapseCycleParam: "collapse", // this value is toggled in the href attribute request correct fragment
	        expandCycleParam: "expand", // this value is toggled in the href attribute request correct fragme
	        arrSchedProperties:[] // Array which Holds CycleId:Offset:value,Sort:value
	    };

		var statusColName = AJS.I18n.getText('project.cycle.schedule.table.column.status');
	    var populateSchedules = function(element, response){
		    var cycleId = AJS.$(element).parent().attr("id").substr(6);
	    	var htmlVal = "<div class='notifications zfj-notifications' id='zfj-permission-message-bar-cycle-" + cycleId + "'/>" +
	    				  "<table id='issuetable' class='aui KeyTable ztable'>" +
	    				  "	<thead>" +
	    				  " 	<tr>"+
	    				  "		<th id='headerrow-id-" +cycleId+ "' class='colHeaderLink sortable headerrow-id' rel='ID:ASC'>"+AJS.I18n.getText("project.cycle.schedule.table.column.id")+"</th>"+
	    				  "		<th id='headerrow-status-"+cycleId+ "'class='colHeaderLink sortable headerrow-status' rel='ExecutionStatus:ASC'>"+AJS.I18n.getText("project.cycle.schedule.table.column.status")+"</th>"+
	    				  "		<th>"+AJS.I18n.getText("project.cycle.schedule.table.column.summary")+"</th>"+
	    				  "		<th>"+AJS.I18n.getText("project.cycle.schedule.table.column.defect")+"</th>"+
	    				  "		<th>"+AJS.I18n.getText("project.cycle.schedule.table.column.component")+"</th>"+
	    				  "		<th>"+AJS.I18n.getText("project.cycle.schedule.table.column.label")+"</th>"+
	    				  "		<th id='headerrow-executedBy-" +cycleId+ "'class='colHeaderLink sortable headerrow-executedBy' rel='ExecutedBy:ASC'>"+AJS.I18n.getText("project.cycle.schedule.table.column.executedBy")+"</th>"+
	    				  "		<th id='headerrow-executedOn-" +cycleId+ "'class='colHeaderLink sortable headerrow-executedOn' rel='ExecutionDate:ASC'>"+AJS.I18n.getText("project.cycle.schedule.table.column.executedOn")+"</th>"+
	    				  "		<th></th>"+ /*Execute, Delete Button*/
	    				  "		</tr>" +
	    				  "	</thead>";
    		var limit = 10;
    		for(var key in response.executions) {
    			var schedule = response.executions[key];
    			var statusMap;
    			if(schedule.executionStatus)
    				statusMap = response.status[schedule.executionStatus];
    			else
    				statusMap = response.status["-1"];

    			var allStatusesMap = response["status"];
    			//It seems we can not iterate over associative array in Google Soy template. Hence creating simple array with status IDs only.
    			var statusIdArray = [];
				for(i in allStatusesMap){
					statusIdArray.push(i);
				}

                var cycleName = (schedule.cycleId == -1) ? "Ad hoc" : schedule.cycleName
				var queryParam 	= 'project = "' + addSlashes(schedule.projectKey) + '" AND fixVersion = "' + addSlashes(schedule.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '") ORDER BY Execution ASC';
				var zqlQuery 	= 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(key) + 1);
        		// IE8 fix
				if(!isNaN(key))
        			htmlVal += ZEPHYR.Project.Cycle.scheduleTable({schedule:schedule, stMap:statusMap, url:contextPath, currentlySelectedScheduleId:response.currentlySelectedScheduleId, allStatusList:allStatusesMap, statusIdArray:statusIdArray, dtStatusName:statusColName, zqlQuery:zqlQuery}) + "\n";
	    	}

    		htmlVal += "</table>"
            	var object = CONST.arrSchedProperties[cycleId];
        		var offsetCnt = object.offset;
        		if((response.recordsCount - offsetCnt) > limit) {
            		var labelCount =  (offsetCnt+1) + "-" + (offsetCnt+limit);
        			htmlVal += "<a id='getMoreSchedules' type='button' class='' href='#' style='float:right'>"+ AJS.I18n.getText("project.cycle.schedule.next.label") +"</a>" +
    						"<label id='getMoreSchedulesSeparator'  style='float:right'> &nbsp;|&nbsp; </label>" +
    						" <a id='getMoreSchedulesPrev' type='button' style='float:right'>"+ AJS.I18n.getText("project.cycle.schedule.previous.label") +"</a>" +
    						"<label id='getMoreSchedulesLbl'  style='float:right'>"+AJS.I18n.getText("project.cycle.schedule.count.label",  labelCount, response.recordsCount)+" |&nbsp;</label>";
    			} else {
    				if(offsetCnt >= limit) {
    	        		var labelCount =  (offsetCnt+1) + "-" + response.recordsCount;
    	    			htmlVal += "<a id='getMoreSchedules' type='button' class='' style='float:right'>"+ AJS.I18n.getText("project.cycle.schedule.next.label") +"</a>" +
    							"<label id='getMoreSchedulesSeparator'  style='float:right'> &nbsp;|&nbsp; </label>" +
    							" <a id='getMoreSchedulesPrev' href='#' type='button' style='float:right'>"+ AJS.I18n.getText("project.cycle.schedule.previous.label") +"</a>" +
    							"<label id='getMoreSchedulesLbl'  style='float:right'>"+AJS.I18n.getText("project.cycle.schedule.count.label",  labelCount, response.recordsCount)+" |&nbsp;</label>";
    				}
    			}
    		ZEPHYR.Schedule.addDeleteTrigger();

	    	element.html(htmlVal);
	    	delegateExecuteStatus(cycleId);

	    	CONST.arrSchedProperties[cycleId].offset=offsetCnt;

	    	//Code to Handle Sorting Start
	    	AJS.$(element).find("#headerrow-id-"+cycleId).bind("click", function(e){
	    		performSort(e, element, "headerrow-id", "ID", cycleId);
	    	});

	    	AJS.$(element).find("#headerrow-status-"+cycleId).bind("click", function(e){
	    		performSort(e, element, "headerrow-status", "ExecutionStatus", cycleId);
	    	});

	    	AJS.$(element).find("#headerrow-executedBy-"+cycleId).bind("click", function(e){
	    		performSort(e, element, "headerrow-executedBy", "ExecutedBy", cycleId);
	    	});

	    	AJS.$(element).find("#headerrow-executedOn-"+cycleId).bind("click", function(e){
	    		performSort(e, element, "headerrow-executedOn", "ExecutionDate", cycleId);
	    	});

	    	/**
			* Perform sort by the column user clicked on.
			* e:ClickEvent, element:Parent cycleDiv, activeColPrefix:Column that is clicked, sortQueryPrefix:QueryPrefix to perform sort on, cycleId:Cycle ID
			*/
	    	function performSort(e, element, activeColPrefix, sortQueryPrefix, cycleId){
	    		var activeColumnJQueryElement = AJS.$("#" + activeColPrefix + "-" + cycleId);
	    		sortBy(e, element, activeColumnJQueryElement);
	    		element.bind("schedulesFetched", function(){
		    		var columnPrefixes = new Array("headerrow-id", "headerrow-status", "headerrow-executedBy", "headerrow-executedOn");
		    		for(var index in columnPrefixes){
		    			if(columnPrefixes[index] != activeColPrefix){
		    				var colIdentifier = "#" + columnPrefixes[index] + "-" +cycleId
		    				AJS.$(colIdentifier).attr("class","colHeaderLink sortable " + columnPrefixes[index]);
		    			}
		    		}
		    		var longStr = getSortString(activeColumnJQueryElement,true);
		    		activeColumnJQueryElement.removeClass();
		    		activeColumnJQueryElement.addClass("active sortable " + longStr + " " + activeColPrefix);
	                var shortStr = getSortString(activeColumnJQueryElement, false);
	                CONST.arrSchedProperties[cycleId].sortQuery = sortQueryPrefix + ":"+ shortStr;
	                activeColumnJQueryElement.attr("rel", CONST.arrSchedProperties[cycleId].sortQuery);
	                element.unbind("schedulesFetched");
	    		});
	    	}

	    	function getSortString(changeKey,longForm) {
	    		var array = changeKey.attr('rel').split(':')
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

	    	//call common method for sorting
	    	function sortBy(e,element,changeKey) {
	    		var array = changeKey.attr('rel').split(':')
	    		if(array.length != 2) {
	    			return;
	    		}
	    		var sortKey = array[0];
	    		var value = array[1];
	    		CONST.arrSchedProperties[cycleId].sortQuery=sortKey+":"+value;
	    		CONST.arrSchedProperties[cycleId].lastSortQuery=sortKey+":"+value;
	    		getMoreSchedules(e,element,-1);
	    	}
	    	//Code to End Sorting Start


	    	// Next Href being handled here
	    	AJS.$(element).find("#getMoreSchedules").bind("click", function(e){

	    		offsetCnt = CONST.arrSchedProperties[cycleId].offset;
	    		getMoreSchedules(e,element,0);
		    	//Add Href Attr to the Prev Link
		    	if(offsetCnt >= limit) {
		    		AJS.$(element).find("#getMoreSchedulesPrev").attr("href","#");
		    	}
                e.preventDefault();
	    	});

	    	//We disable the clicking action on Prev unless the Offset exceedes the limit
	    	AJS.$(element).find("#getMoreSchedulesPrev").bind("click", function(e){
	    		offsetCnt = CONST.arrSchedProperties[cycleId].offset;
	    		if(offsetCnt < limit || offsetCnt == 0) {
		    		e.preventDefault();
		    		return false;
		    	} else {
		    		getMoreSchedules(e,element,10);
	                e.preventDefault();
		    	}
	    		if(offsetCnt == 0)
		    		AJS.$(element).find("#getMoreSchedulesPrev").removeAttr("href");
	    	});


	    	AJS.$(element).on("click", "a[id^=execution-button-]", function(e) {
	    		e.stopImmediatePropagation();
	    		var query = e.currentTarget.href;
	    		var zql = query.split('&offset=')[0];
	    		var off = query.split('&offset=')[1] || 1;

	    		if(CONST.arrSchedProperties[cycleId].lastSortQuery) {
		    		var sortQ = CONST.arrSchedProperties[cycleId].lastSortQuery.split(':');
					if(query && (sortQ.length == 2)) {
			    		var count = query.indexOf("ORDER%20BY");
			    		if(count != -1) {
			    			zql = zql.substring(0,count);
			    		}
                        var isSortedByOrder = false;
			    		if(sortQ[0] == "ID") {
				    		zql += " ORDER BY " + "Issue " + sortQ[1];
			    		} else if(sortQ[0] == "OrderId") {
				    		zql += " ORDER BY " + "Execution " + sortQ[1];		// Order by OrderId
                            isSortedByOrder = true;
			    		} else {
				    		zql += " ORDER BY " + sortQ[0] + " " + sortQ[1];
			    		}

                        if(!isSortedByOrder){
                            zql += ", Execution " + sortQ[1];		// always add order as secondary field if query is sorted
                        }
					}
	    		}
	    		zql += "&offset="+ (CONST.arrSchedProperties[cycleId].offset + parseInt(off));
	    		e.currentTarget.href = zql;
	    	});

	    	var getMoreSchedules = function(e,element,offsetChanged) {
	    		if(!CONST.arrSchedProperties[cycleId].lastSortQuery || CONST.arrSchedProperties[cycleId].lastSortQuery == 'undefined') {
	    			CONST.arrSchedProperties[cycleId].lastSortQuery = "OrderId:ASC";
	            }
	    		var sortQuery= '&sorter='+CONST.arrSchedProperties[cycleId].lastSortQuery;

	    		//Action is reversed when the CycleDiv is clicked, incase of Pagination we need to reverse the action we find
	    		// on the DIV. code below does that
	    		var cycleDiv = AJS.$(element).parent().parent().find('.zcycle-header');
				var linkAnchor = AJS.$(cycleDiv).children("a");
				CONST.arrSchedProperties[cycleId].action = "expand";
				if(linkAnchor.attr("href").indexOf("expand") > -1) {
					 CONST.arrSchedProperties[cycleId].action = "collapse";
				}

				//Setup Offset Count and Next and Previous Links
	    		if(offsetChanged == 10) {
	    			offsetCnt = offsetCnt-limit;
			    	CONST.arrSchedProperties[cycleId].offset=offsetCnt;
		    		//enable next link
		    		if((response.recordsCount - offsetCnt) > limit) {
			    		AJS.$(element).find("#getMoreSchedules").attr("href","#");
		    		}
	    		} else if(offsetChanged == 0) {
	    			if((response.recordsCount - offsetCnt) < limit) {
    		    		  AJS.$(element).find("#getMoreSchedules").removeAttr("href");
				  e.preventDefault();
				  return false;
		    		}
				CONST.arrSchedProperties[cycleId].offset=offsetCnt+limit;
				offsetCnt = CONST.arrSchedProperties[cycleId].offset;

		    	} else if(offsetChanged == -1) {
	    			CONST.arrSchedProperties[cycleId].offset=0;
	    			offsetCnt = CONST.arrSchedProperties[cycleId].offset;
	    			sortQuery = '&sorter='+CONST.arrSchedProperties[cycleId].sortQuery;
	    		}

	    		var pid = '&projectId=' + AJS.$("#zprojectId")[0].value;
                var vid = '&versionId=' + AJS.$("#select-version2").val();
                var action = CONST.arrSchedProperties[cycleId].action;
                var url = getRestURL() + "/execution";
                var elID = ('-cycle-' + cycleId);
            	AJS.$('#cycle-wrapper-' + cycleId).show();
            	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
                jQuery.ajax({
                    url: url,
                    data: CONST.requestParams + "&cycleId="+cycleId+"&action="+CONST.arrSchedProperties[cycleId].action+"&offset="+offsetCnt+ pid + vid+sortQuery,
                    dataType: "json",
                    error: function(xhr) {
                        permissionsCheck(xhr, null, url);
                        AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
                    },
                    success: function (response, textStatus, xhr) {
                        if (permissionsCheck(xhr, response, url)) {
                        	var htmlVal = "";
                        	AJS.$(element).find("tbody>tr").remove();
                        	var allStatusesMap = response["status"];
                			//It seems we can not iterate over associative array in Google Soy template. Hence creating simple array with status IDs only.
                			var statusIdArray = [];
            				for(i in allStatusesMap){
            					statusIdArray.push(i);
            				}
                        	for(var key in response.executions) {
                        		var schedule = response.executions[key];
                                var cycleName = (schedule.cycleId == -1) ? "Ad hoc" : schedule.cycleName
                   				var queryParam 	= 'project = "' + addSlashes(schedule.projectKey) + '" AND fixVersion = "' +  addSlashes(schedule.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '") ORDER BY Execution ASC';
                				var zqlQuery 	= 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(key) + 1);

                    			var htmlRow = ZEPHYR.Project.Cycle.scheduleTable({schedule:schedule, stMap:response.status[schedule.executionStatus], url:contextPath,
                    							currentlySelectedScheduleId:response.currentlySelectedScheduleId, allStatusList:allStatusesMap, statusIdArray:statusIdArray, zqlQuery:zqlQuery});
                    			if(AJS.$(element).find("tbody").length < 1){
                    				AJS.$(element).find("thead").after(AJS.$('<tbody></tbody>'));
                    			}
                    			AJS.$(element).find("tbody").append(htmlRow);
                	    	}
                        	if((response.recordsCount - offsetCnt) > limit) {
                        		var labelCount =  (offsetCnt+1) + "-" + (offsetCnt+limit);
                        		AJS.$(element).find("#getMoreSchedulesLbl").html(AJS.I18n.getText("project.cycle.schedule.count.label", labelCount, response.recordsCount)+" |&nbsp;");
                        	} else {
                        		var labelCount =  (offsetCnt+1) + "-" + response.recordsCount;
                        		AJS.$(element).find("#getMoreSchedulesLbl").html(AJS.I18n.getText("project.cycle.schedule.count.label", labelCount, response.recordsCount)+" |&nbsp;");
            		    		AJS.$(element).find("#getMoreSchedules").removeAttr("href");
                        	}
                        }
                        var elHeight = element.find('table#issuetable').prop("scrollHeight") + 40; 	// Height of the execution table and the pagination
                        element.css({display: "block"}).animate({height: elHeight},  CONST.tweenSpeed,function(){
                        });
                        element.trigger("schedulesFetched");
                        delegateExecuteStatus(cycleId);
                		AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
                    }
                })
	    	}
	    }

	    return function () {

	        var handler = function () {
	            // we are using event delegation to avoid assigning event handlers each time the tab is loaded via ajax. Using zcycle-header selector to avoid double initializing.
	            //JIRA.Project.navigationTabs.getProjectTab().find(".zcycle-header").die('click')
                AJS.$(".zcycle-header").off('click');
	            //JIRA.Project.navigationTabs.getProjectTab().find(".zcycle-header").live('click', function(e) {
                AJS.$(".zcycle-header").on('click', function(e) {
	            	CONST.offset = 0; //reset the offset

	            	var cycleId = AJS.$(this).parent().attr("id").substr(6);
                    //Get the Offset from Array.
	            	var offset = CONST.arrSchedProperties[cycleId].offset;
                    //Get the sortQuery from Array.
	            	var sortQuery = CONST.arrSchedProperties[cycleId].lastSortQuery;

	            	if(offset == null) {
	            		offset = 0;
	            	}

	            	if(sortQuery == null || sortQuery == undefined || sortQuery == '') {
	            		sortQuery = "OrderId:ASC"; 				// Setting default sort query to OrderId:ASC[order By orderId].
	            	}

	                // lets use event delegation, to check if what we are click on is an expando
	                var parent = jQuery(this).parent(), contentElement = parent.find("." + CONST.contentClass),
	                        linkTarget = jQuery(this).find(CONST.linkSelector);
                    var url = linkTarget.attr("href");
                    var start = url.indexOf("offset=");
                    var startRest = url.indexOf("offset=",start);
                    var startRest1 = url.indexOf("&",start);
                    var offsetStr = url.substring(startRest,startRest1);

                    var sorter = url.indexOf("sorter=");
                    var endSorter = url.indexOf("sorter=",sorter);
                    var sorterStr = url.substring(endSorter,url.length);

	                // if we click on a link then bail out and follow link
	                if (e.target.nodeName === "A" || jQuery(e.target).parent().get(0).nodeName === "A") {
	                    return;
	                }
	                // if this element is not active then I assume we are expanding it
	                if (!parent.hasClass(CONST.activeClass) && !contentElement.is(":animated")) {
	                    // we are now active
	                    parent.addClass(CONST.activeClass);

	                    linkTarget.attr("href", linkTarget.attr("href").replace(offsetStr, "offset="+offset));
	                	linkTarget.attr("href", linkTarget.attr("href").replace(sorterStr, "sorter="+sortQuery));

	                    var throbberTarget = {target: AJS.$(".vertical.tabs .active")};
	                    var pid = '&projectId=' + AJS.$("#zprojectId")[0].value;
	                    var selectedVersionId = AJS.$("#select-version2").val();
	                    if(Array.isArray(selectedVersionId)) {	// Fix for ZFJ-1598: the issue is unscheduled version ID is -1 and it does not override the previous Single select selection.
	                    	AJS.$("#select-version2 option:selected").removeAttr('selected');
	                    	selectedVersionId = selectedVersionId[0];
	                    	AJS.$("#select-version2").val(selectedVersionId);
	                    }
	                    var vid = '&versionId=' + selectedVersionId;
	                    var elID = ('-cycle-' + cycleId);
	                	AJS.$('#cycle-wrapper-' + cycleId).show();
	                	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
	                    // make request
	                    AJS.$(AJS.$.ajax({
	                        url: url,
	                        data: CONST.requestParams + pid + vid,
	                        dataType: "json",
	                        error: function(xhr) {
	                            permissionsCheck(xhr, null, url);
	                            AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
	                        },
	                        success: function (response, textStatus, xhr) {
	                            if (permissionsCheck(xhr, response, url)) {
	                                if (contentElement.length === 0) {
	                                    // if we don't have a place to inject the response lets make one
	                                    contentElement = jQuery("<div>").css({
	                                        display: "block",
	                                        height: "0"
	                                    }).addClass(CONST.contentClass).appendTo(parent).click(function (e) {
	                                        e.stopPropagation();
	                                    });
	                                }
	                                // lets add content, I am assuming there is no event handlers on this content,
	                                // otherwise this approach has the potential to create memory leaks
	                                //contentElement.html(html);
	                                populateSchedules(contentElement, response);
	                                // expand (had issues with slide toggle for ie7, so using animate instead)
	                                contentElement.css({display: "block"});
	                                var bannerHeight = (AJS.$('#isProjectCentricViewEnabled').length) ? 'auto' : contentElement.prop("scrollHeight");
	                                contentElement.animate({height: bannerHeight},  CONST.tweenSpeed,function(){
	                                	 CONST.arrSchedProperties[cycleId].action = CONST.expandCycleParam;
	                                    // get ready for the next time we click(contract)
	                                    linkTarget.attr("href", linkTarget.attr("href").replace(CONST.expandCycleParam, CONST.collapseCycleParam));
	                                    parent.addClass("expanded");
	                                });
	                        		AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
	                            }
	                        }
	                    })).throbber(throbberTarget);  // lets use the throbber plugin, we will only see the throbber when the request is latent...
	                // if this element is active then I assume we are contracting it
	                } else if (parent.hasClass(CONST.activeClass) && !parent.hasClass("locked")) {
	                	//Set the Offset
	                	linkTarget.attr("href", linkTarget.attr("href").replace(offsetStr, "offset="+offset));
	                	linkTarget.attr("href", linkTarget.attr("href").replace(sorterStr, "sorter="+sortQuery));

	                    // retains hidden state if we reload the page
	                    jQuery.get(linkTarget.attr("href") + "&" + CONST.requestParams + "&versionId=" + AJS.$("#select-version2").val() + "&limit=0", function () {
	                        // we are not active anymore
	                        parent.removeClass(CONST.activeClass);
	                        // expand (had issues with slide toggle for ie7, so using animate instead)
	                        contentElement.animate({
	                          //  height: 0
	                        }, CONST.tweenSpeed, function () {
	                            contentElement.css({display: "none"});
	                            CONST.arrSchedProperties[cycleId].action = CONST.collapseCycleParam;
	                            // get ready for the next time we click(expand)
	                            linkTarget.attr("href", linkTarget.attr("href").replace(CONST.collapseCycleParam, CONST.expandCycleParam));
	                            parent.removeClass("expanded");
	                        });

	                    });

	                }
	            });
	            return arguments.callee;
	        }();
//	        JIRA.Project.navigationTabs.addLoadEvent("roadmap-panel-panel", handler);
//	        JIRA.Project.navigationTabs.addLoadEvent("changelog-panel-panel", handler);
//	        JIRA.Project.navigationTabs.addLoadEvent("component-roadmap-panel-panel", handler);
//	        JIRA.Project.navigationTabs.addLoadEvent("component-changelog-panel-panel", handler);
	        //Refresh Entire Block on schedule deletion
	        	//(workaround for multiple add, Fix it the right way, dont refresh entire cycle, refresh just that cycle)
	        AJS.$("#project-panel-cycle-list-summary").unbind("scheduleDeleted");
	        AJS.$("#project-panel-cycle-list-summary").bind("scheduleDeleted", function(e){
    			selectedVersionChanged();
    		});
	        /*
	         * AS we are using JIRA Form dialog to create the delete schedule dialog,
	         * JIRA triggers 'JIRA.Events.NEW_CONTENT_ADDED' event when the dialog is created.
	         * As we listening to 'JIRA.Events.NEW_CONTENT_ADDED' event to re-render the cycles, the cycles will be refetched which
	         * is not necessary when delete dialog is created. So setting a flag ZEPHYR.Cycle.isScheduleDelete.
	         */
	        AJS.$("#project-panel-cycle-list-summary").unbind("scheduleDeleteStarted");
	        AJS.$("#project-panel-cycle-list-summary").bind("scheduleDeleteStarted", function(e){
	        	ZEPHYR.Cycle.isScheduleDelete = true;
    		});
	        return CONST;
	    };
	}()
	};
})();

ZEPHYR.Cycle.globalDispatcher = _.extend({}, Backbone.Events);
ZEPHYR.Cycle.Execution = Backbone.Model.extend({
	defaults: {
		prevOrderId: 	-1,
		position:		-1
	}
});
ZEPHYR.Cycle.ExecutionsCollection = Backbone.Collection.extend({
    model:ZEPHYR.Cycle.Execution,
    comparator: function(model) {
		return model.get("orderId");
	},
    parse: function(resp, xhr){
    	return resp.executions
    }
});
ZEPHYR.Cycle.reorderExecutions = new ZEPHYR.Cycle.ExecutionsCollection();

ZEPHYR.Cycle.ReorderExecutionView = Backbone.View.extend({
    tagName: 'tr',
    className: 'execution-row',

    events: {
        'drop' : 'drop'
    },

    initialize: function() {
    	// On change in the orderId attribute update the orderId in UI.
    	this.model.on('change:orderId', this.updateOrderIdInUI, this);
    },

    drop: function(ev, index) {
    	ZEPHYR.Cycle.globalDispatcher.trigger("updateExecutionsOnDND", this.model, index);
    },

    updateOrderIdInUI: function() {
    	this.$el.find('td.reorder-orderId').html(this.model.get('position') + 1);
    },

    render: function() {
    	// Attach reorderExecutionContent for each execution
    	AJS.$(this.el).html(ZEPHYR.Project.Cycle.reorderExecutionContent({
        	execution: 		this.model.toJSON(),
			status:			this.options.status,
			contextPath: 	contextPath
        }));
        return this;
    }
});

ZEPHYR.Cycle.ReorderExecutionsView = Backbone.View.extend({
	className: 	'reorder-executions-dialog-container',

	initialize: function() {
		ZEPHYR.Cycle.globalDispatcher.on('updateExecutionsOnDND', this.updateExecutions, this);
	},

    updateExecutions: function(model, position) {
    	var newPosition 	= position;
    	var prevPosition	= model.get('position');
    	var newOrderId		= this.collection.at(newPosition).get('orderId');
    	/*
    	 * There can be three conditions to check when user performs drag and drop action.
    	 * 1. If the new position is same as previous, then do not make any changes.
    	 * 2. If previous position is greater than new position.
    	 * 3. If new position is greater than previous position.
    	 */
    	if(newPosition != prevPosition) {
    		if(prevPosition > newPosition) {
    	    	for(var i = newPosition; i < prevPosition; i++) {
    	    		var model = this.collection.at(i);
    	    		var prevModel = this.collection.at(i+1);
    	    		model.set({
    	    			orderId: 	prevModel.get('orderId'),
    	    			position: 	prevModel.get('position')
    	    		});
    	    	}
    		} else if(newPosition > prevPosition) {
    			for(var i = newPosition; i > prevPosition; i--) {
    	    		var model 	  = this.collection.at(i);
    	    		var prevModel = this.collection.at(i-1);
    	    		model.set({
    	    			orderId: 	prevModel.get('orderId'),
    	    			position: 	i-1
    	    		});
    	    	}
    		}
			this.collection.at(prevPosition).set({
				'orderId': 	newOrderId,
				position: 	newPosition
			});
    		this.collection.sort();
    	}
    },

    /*
     * Three cases handled:
     * i. When the user does not make any changes in order: displaying the warning
     * ii. If there is no orderId present, then displaying warning
     * iii. If the orderId has been updated: save the changes and update the ui on success.
     */
    saveReorderDetails: function(completed) {
    	var executionReorders 	= [],
    		instance			= this,
    		hasError 			= false;

    	AJS.$.each(this.collection.models, function(i, model) {
    		if(!model.get('orderId') || !model.get('prevOrderId')) {
    			executionReorders = [];
    			hasError = true;
    			return false;
    		} else if((model.get('prevOrderId') != model.get('orderId'))) {
    			executionReorders.push({
    				executionId: 	model.get('id'),
    				oldOrderId:		model.get('prevOrderId'),
    				newOrderId:		model.get('orderId')
    			});
    		}
    	});
    	var reorderDetails = {
    		cycleId: 			this.options.cycleId,
    		versionId:			this.options.versionId,
    		executionReorders: 	executionReorders
    	}
    	if(executionReorders.length > 0) {
    		var elID = ('-cycle-' + this.options.cycleId);
    		AJS.$('#cycle-wrapper-' + this.options.cycleId).show();
    		ZEPHYR.Cycle.attachPermissionActiveClass(elID);
	    	jQuery.ajax({
				url: getRestURL() + "/execution/reorder",
				type : "post",
				contentType :"application/json",
				data: JSON.stringify(reorderDetails),
				success : function(response) {
					if(response.success){
						if(completed) completed.call();
						// Update UI on success of saving reordered executions.
						//ZEPHYR.Cycle.updateCycleExecutionsUIOnReorder(instance.options.soffset, instance.options.cycleId);
					} else {
						instance.displayReorderErrorMessage(AJS.I18n.getText("cycle.reorder.executions.error", response.failedExecutions));
					}
					AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
				},
				error : function(response) {
					var errorResponse = jQuery.parseJSON(response.responseText);
			    	var consolidatedErrorMessage = "";
			    	jQuery.each(errorResponse, function(key, value){
			    		consolidatedErrorMessage += value + "<br/>";
			    	});
					instance.displayReorderErrorMessage(consolidatedErrorMessage);
					AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
				}
			});
    	} else {
    		console.log('Nothing saved, hence skipping save');
    		if(hasError) {
    			var errorMessage = AJS.I18n.getText("cycle.reorder.executions.general.error");
    			instance.displayReorderErrorMessage(errorMessage);
    		} else {
        		var warningMessage = AJS.I18n.getText("cycle.reorder.executions.general.warning");
        		instance.displayReorderWarningMessage(warningMessage);
    		}
    	}
    },

    displayReorderErrorMessage: function(message) {
    	AJS.$(".zephyr-aui-message-bar").empty();

    	AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
    	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
    	    body: message
    	});
    	// Scroll top to the error message
    	AJS.$('#reorder-executions-dialog .aui-dialog2-content').scrollTop('.zephyr-aui-message-bar');
    },

    displayReorderWarningMessage: function(message) {
    	AJS.$(AJS.$('.zephyr-aui-message-bar')[0]).empty();

    	AJS.messages.warning(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
    	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
    	    body: message
    	});
    	// Scroll top to the error message
    	AJS.$('#reorder-executions-dialog .aui-dialog2-content').scrollTop('.zephyr-aui-message-bar');
    },

    // Display the reorder executions modal if aui dialog2 feature is supported and attach the drag n drop functionality.
	displayReorderExecutionsDND: function(callback) {
		var instance = this;
		if(AJS.dialog2) {
			// Display the reorder executions modal
			AJS.dialog2("#reorder-executions-dialog").show();
			// Hides the dialog
			AJS.$("#reorder-executions-dialog-save").click(function(ev) {
			    ev.preventDefault();
			    instance.saveReorderDetails(function() {
			    	AJS.dialog2("#reorder-executions-dialog").hide();
			    	if(callback) {
			    		callback.call();
			    	}
	        });
			});
			// Trigger save reorder execution details function
			AJS.$("#reorder-executions-dialog-close").click(function(ev) {
			    AJS.dialog2("#reorder-executions-dialog").hide();
			});
			// On hide clear the reorder executions modal DOM elements.
			AJS.dialog2.on("hide", function() {
				ZEPHYR.Cycle.globalDispatcher.off('updateExecutionsOnDND');
				instance.remove();
				AJS.$("#reorder-executions-dialog").remove();
				AJS.$('.reorder-executions-dialog-container').remove();
			});
		} else AJS.$(window).trigger('resize');
		// Attach jquery ui sortable event for the DOM
		AJS.$('.ui-sortable').sortable({
			placeholder: 	"ui-state-highlight",
			handle: 		".reorder-draghandler",
			create: function(ev, ui) {
				JIRA.Loading.hideLoadingIndicator();
			},
			stop: function(ev, ui) {
		    	ev.stopImmediatePropagation();
				ui.item.trigger('drop', ui.item.index());
			}
		});
	},

	// Append individual execution to the Reorder dialog
	appendExecutionView: function(model) {
        var reorderExecutionView = new ZEPHYR.Cycle.ReorderExecutionView({
        	model: 		model,
        	status:		this.options.status
        });
        if(AJS.dialog2) this.$el.find('tbody.ui-sortable').append(reorderExecutionView.render().el);
        else AJS.$('#reorder-executions-dialog-wrapper').find('tbody.ui-sortable').append(reorderExecutionView.render().el);
        reorderExecutionView.$el.attr('id', 'execution-row-' + model.get('id'));
    },

	render: function() {
		var instance = this;
		if(AJS.dialog2) {
			// Attach reorderExectionDialog2 to the DOM
			AJS.$(this.el).html(ZEPHYR.Project.Cycle.reorderExectionDialog2());
		} else {
			var dialog = new JIRA.FormDialog({
				id: "reorder-executions-dialog-wrapper",
				width: '70%',
				height: '90%',
				content: function (callback) {
					callback(ZEPHYR.Project.Cycle.reorderExectionDialog());
				},
				submitHandler: function (e) {
					e.preventDefault();
					instance.saveReorderDetails(function() {
						ZEPHYR.Cycle.globalDispatcher.off('updateExecutionsOnDND');
						instance.remove();
		        		dialog.hide();
		            });
				}
			});
			dialog.show();
			AJS.$('#reorder-executions-dialog-wrapper .buttons-container a.cancel').click(function(ev) {
				ZEPHYR.Cycle.globalDispatcher.off('updateExecutionsOnDND');
			});
			AJS.$(window).resize(function(ev) {
				AJS.$('#reorder-executions-dialog-wrapper .reorder-executions-wrapper').css('height', (AJS.$(window).height()- 260));
			});
		}
		this.collection.each(this.appendExecutionView, this);
        return this;
	}
});



/* Update UI on success of saving reordered executions or selected Executions order by orderId in drop down.
 * There are two conditions that needs to be checked here,
 * 1. If the cycle tab is collapsed: Update the version expando target element href.
 * 2. If the cycle tab is expanded: In this case make a call to fetch 10 executions with sortQuery 'OrderId:ASC',
 * 		display them and update the version expando target element href.
 */
/*ZEPHYR.Cycle.updateCycleExecutionsUIOnReorder = function(offsetCnt, cycleId) {
	var limit 			= 10;
	var CONST 			= ZEPHYR.Cycle.expandos();
	var expandoAction 	= AJS.$('a#version-expando-' + cycleId).attr('href').split('&action=');
	var expandoHref 	= getRestURL() + "/execution?cycleId=" + cycleId + "&action=" + expandoAction[1].split('&')[0] + "&offset=0&sorter=OrderId:ASC";

	offsetCnt = parseInt(offsetCnt);
	AJS.$('a#version-expando-' + cycleId).attr('href', expandoHref);
	AJS.$('li#cycle-' + cycleId + ' th.sortable').removeClass('active');
	CONST.arrSchedProperties[cycleId].lastSortQuery = 'OrderId:ASC';
	CONST.arrSchedProperties[cycleId].sortQuery = 'OrderId:ASC';
	CONST.arrSchedProperties[cycleId].offset = 0;
	if (AJS.$('li#cycle-' + cycleId).hasClass('active')) {
		var action 	= 'collapse';
    	var pid 	= '&projectId=' + AJS.$("#zprojectId")[0].value;
        var vid 	= '&versionId=' + AJS.$("#select-version2").val();

		if(CONST.arrSchedProperties[cycleId] && CONST.arrSchedProperties[cycleId].action && CONST.arrSchedProperties[cycleId].action == 'expand')
			action 	= 'expand';
    	var url 	= getRestURL() + "/execution?cycleId=" + cycleId + "&action=" + action + "&offset=0&sorter=OrderId:ASC";
    	var elID = ('-cycle-' + cycleId);
    	AJS.$('#cycle-wrapper-' + cycleId).show();
    	ZEPHYR.Cycle.attachPermissionActiveClass(elID);
        jQuery.ajax({
            url: url,
            data: CONST.requestParams + pid + vid,
            dataType: "json",
            error: function(xhr) {
            	ZEPHYR.Cycle.permissionsCheck(xhr, null, url);
            	AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
            },
            success: function (response, textStatus, xhr) {
            	if (ZEPHYR.Cycle.permissionsCheck(xhr, response, url)) {
            		AJS.$('li#cycle-' + cycleId + ' .versionBanner-content').find("tbody>tr").remove();
                    for(var key in response.executions) {
                        var schedule 		= response.executions[key];
                    	var allStatusesMap 	= response.status;
                    	var recordsCount 	= response.recordsCount;
                    	var versionEl 		= AJS.$('li#cycle-' + cycleId + ' .versionBanner-content');
                		//It seems we can not iterate over associative array in Google Soy template. Hence creating simple array with status IDs only.
                		var statusIdArray 	= [];
                		for(i in allStatusesMap){
                			statusIdArray.push(i);
                		}

                        var cycleName = (schedule.cycleId == -1) ? "Ad hoc" : schedule.cycleName
                        var queryParam	= 'project = "' + addSlashes(schedule.projectKey) + '" AND fixVersion = "' +  addSlashes(schedule.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '") ORDER BY Execution ASC';
                        var zqlQuery    = 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(key) + 1);

                        var htmlRow = ZEPHYR.Project.Cycle.scheduleTable({schedule:schedule, stMap:allStatusesMap[schedule.executionStatus], url:contextPath,
                                        currentlySelectedScheduleId: response.currentlySelectedScheduleId, allStatusList:allStatusesMap, statusIdArray:statusIdArray, zqlQuery:zqlQuery});
                        if(versionEl.find("tbody").length < 1){
                        	versionEl.find("thead").after(AJS.$('<tbody></tbody>'));
                        }
                        versionEl.find("tbody").append(htmlRow);
                        var elHeight = versionEl.find('table#issuetable').prop("scrollHeight") + 40; 	// Height of the execution table and the pagination
                        versionEl.height(elHeight);
                    }
                    var labelCount =  1 + "-" + limit;
                    versionEl.find("#getMoreSchedulesLbl").html(AJS.I18n.getText("project.cycle.schedule.count.label", labelCount, recordsCount)+" |&nbsp;");
                    versionEl.find("#getMoreSchedules").removeAttr("href");
                    delegateExecuteStatus(cycleId);
            		AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
            	}
            }
        });
	}
}*/

/*Caches and creates Cycle Dialog*/
ZEPHYR.Cycle.initCycleCreate = function(){
	AJS.$("#pdb-create-cycle-dialog").bind("click", function(e) {
		var projectId = AJS.$("#zprojectId").val();
		var lastSelectedVersion = AJS.$("#select-version2 :selected").val();
		ZEPHYR.Cycle.CreateCycleFunction.createCycleDialog(projectId, ZEPHYR.Cycle.versionList,lastSelectedVersion.toString());
		e.preventDefault();
		return;
	});
}

ZEPHYR.Cycle.CreateCycleFunction = {
	/*In lack of model layer, persisting data by passing thro' functions*/
	createCycleDialog : function (projectId,versionList,lastvisitedVersion, cycleBeingCloned) {
	    dialog = new JIRA.FormDialog({
	        id: "create-cycle-dialog",
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
	        	//var innerHtmlStr = ZEPHYR.Project.Cycle.createCycleDialog({projectId:projectId,versions:versionList,lastvisitedVersion:lastvisitedVersion, cycle:cycleBeingCloned, sprints: ZEPHYR.Cycle.sprintList.values});
	        	var innerHtmlStr = ZEPHYR.Project.Cycle.createCycleDialog({projectId:projectId,versions:versionList,lastvisitedVersion:lastvisitedVersion, cycle:cycleBeingCloned, sprints: []});
	        	callback(innerHtmlStr);
	        	new AJS.SingleSelect({
	                element: AJS.$('#cycle_sprint'),
	                maxInlineResultsDisplayed: 15,
	                maxWidth: 250
	            });
	        },

	        submitHandler: function (e) {
	        	// Attach Loading icon
	        	dialog.$popupContent.find('.buttons-container').append('<span class="icon throbber loading dialog-icon-wait dialog-icon-wait-top">Loading...</span>');
	        	ZEPHYR.Cycle.CreateCycleFunction.createCycle(projectId, cycleBeingCloned, function () {
	        		//If newly created cycle belongs to same version that is showing, refresh the list
	        		if(lastvisitedVersion == AJS.$('#cycle_version').val()){
		        		selectedVersionChanged();
	        		}
	        		dialog.hide();
	            });
	            e.preventDefault();
	        }
	    });
	    dialog.show();
	},
	createCycle : function(projectId, cycleBeingCloned, completed) {
		var elID = ('-project-' + projectId);
		ZEPHYR.Cycle.attachPermissionActiveClass(elID);
		jQuery.ajax({
			//url: getRestURL() + "/cycle/create",
			url: getRestURL() + "/cycle",
			type : "post",
			contentType :"application/json",
			data: JSON.stringify( {
				  'clonedCycleId': cycleBeingCloned ? cycleBeingCloned.id :"",
				  'name' : AJS.$('#cycle_name').val(),
		          'build' :  AJS.$('#cycle_build').val(),
		          'environment': AJS.$('#cycle_environment').val(),
		          'description' : AJS.$('#cycle_description').val(),
		          'startDate': AJS.$('#cycle_startDate').val(),
		          'endDate': AJS.$('#cycle_endDate').val(),
		          'projectId': projectId,
		          'versionId': AJS.$('#cycle_version').val(),
		          'sprintId': AJS.$('#cycle_sprint :selected').val(),
		          'issueId': AJS.$("#issueId").val()
			}),
			success : function(response) {

				if (cycleBeingCloned){
					var jobProgressToken = response.jobProgressToken;
					if(response != null) {
						var msgDlg = new JIRA.FormDialog({
							id: "warning-message-dialog",
							content: function (callback) {
								var innerHtmlStr = ZEPHYR.Project.Cycle.warningDialogContent({
									warningMsg:AJS.I18n.getText('zephyr.je.clone.cycle.in.progress'),
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
                                    selectedVersionChanged();
								}
								if(data.progress == 1 && errMsg == null) {
									AJS.$("#cycle-aui-message-bar .aui-message").html(data.message);
									clearInterval(intervalId);
                                    selectedVersionChanged();
								}
							}
						})
					}, 1000);

				}else{

				if(completed)
					completed.call();
				AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
				}
			},
			error : function(response) {
				validateCycleInput(response);
				AJS.$('#zfj-permission-message-bar' + elID).removeClass('active');
			}
		});
	}
}

ZEPHYR.Cycle.isCycleTab = function() {
	var _location = window.location.href,
		isProjectCentricViewEnabled = AJS.$('#isProjectCentricViewEnabled').val(),
		activeTab = AJS.$("input#zephyr-proj-tab").val();

	if((isProjectCentricViewEnabled && _location.indexOf('test-cycles-tab') == -1) || (!isProjectCentricViewEnabled && activeTab != 'test-cycles-tab'))
		return false;
	else
		return true;
}



InitPageContent(function(){
	function htmlEncode(value){
		return AJS.$('<div/>').text(value).html();
	}
	var errorElement = AJS.$("#zerrors");
	if(errorElement.length > 0){
		var errors = errorElement[0].value;
		if(AJS.$('#isProjectCentricViewEnabled').length){
			errors = htmlEncode(errors);
		}
		if(errors != null && errors.length > 0) {
			return;
		}
	}

	JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, panel, reason) {
        var activeTab = AJS.$("li.active a#com\\.thed\\.zephyr\\.je\\:pdb_cycle_panel_section-panel");
		if(!ZEPHYR.Cycle.isCycleTab())
			return;

        if(activeTab.length < 1 || ZEPHYR.Cycle.isScheduleDelete){
        	ZEPHYR.Cycle.isScheduleDelete = false;
        	return;
        }

        //ZEPHYR.Cycle.fetchSprints();
        initListeners();
        ZEPHYR.Cycle.versionList = [];
        AJS.$("#select-version2 > optgroup > option").each(function() {
            ZEPHYR.Cycle.versionList.push({id:this.value, name:this.text});
        });
        AJS.params.zdateformat = AJS.$("#zdateformat").val();
        createVersionPicker(panel);
    });

	if(!ZEPHYR.Cycle.isCycleTab())
		return;

    //ZEPHYR.Cycle.fetchSprints();
	initListeners();
    createVersionPicker(AJS.$("#project-tab"));

    //renderNewPlanCycleUI();

    ZEPHYR.Cycle.versionList = [];
    AJS.$("#select-version2 > optgroup > option").each(function() {
        ZEPHYR.Cycle.versionList.push({id:this.value, name:this.text});
    });
    AJS.params.zdateformat = AJS.$("#zdateformat").val();
});

/* Check if cycle panel is active */
	//	var cycleLi = AJS.$("#pdb_cycle_panel_section-panel").parent();
	//	if(cycleLi.hasClass("active"))
/* In case you want to find out when the cycle tab becomes active. */
//	JIRA.Project.navigationTabs.addLoadEvent("pdb_cycle_panel_section-panel", initListeners);


function initListeners(){
	var selectVersionElement = AJS.$("#select-version2");
	if(selectVersionElement.length > 0){
		AJS.$("body").off("change", "#select-version2");
		AJS.$("body").on("change", "#select-version2", null, selectedVersionChanged);
		ZEPHYR.Cycle.initCycleCreate();
		selectedVersionChanged();
	}
}


function createVersionPicker(context) {
	if(!context.length && AJS.$('#isProjectCentricViewEnabled').length){
		context = AJS.$('#test-cycles-tab');
	}
    context.find("select#select-version2").each(function (i, el) {
        new AJS.SingleSelect({
            element: el,
            maxInlineResultsDisplayed: 15,
            maxWidth:400
        });
    });
    AJS.$("#select-version2-field").css("width","150px");
    AJS.$("#select-version2-field").css("height","28px");
    AJS.$("#select-version2-field").unbind('focusout');
    AJS.$("#select-version2-field").bind('focusout', function(ev) {
    	if(AJS.$(this).val() == '' || AJS.$(this).val() == null || AJS.$(this).val() == undefined) {
    		AJS.$(this).val(ZEPHYR.Cycle.selectedVersion);
    	}
    });
}

function validateCycleInput(response){
	AJS.$(".zephyr-aui-message-bar").show();
	AJS.$(".zephyr-aui-message-bar").empty();

	var errorResponse = jQuery.parseJSON(response.responseText);
	var consolidatedErrorMessage = "";
	jQuery.each(errorResponse, function(key, value){
		consolidatedErrorMessage += value + "<br/>";
	});

	AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
	    body: consolidatedErrorMessage
	});
	dialog.$popupContent.find('.dialog-icon-wait').remove();
	AJS.$(':submit', dialog.$form).removeAttr('disabled');
}

ZEPHYR.Cycle.attachPermissionActiveClass = function(elID) {
	AJS.$('.zfj-notifications, .zfj-permission-message-bar').removeClass('active');
	AJS.$('#zfj-permission-message-bar' + elID).addClass('active');
}

ZEPHYR.Cycle.fetchSprints = function() {
	var agileBoardId = 1; // TODO: delete
	ZEPHYR.Cycle.sprintList = [];

	 // TODO: add the board Id
	 jQuery.ajax({
 		url: contextPath + "/rest/agile/1.0/board/" + agileBoardId + "/sprint",
 		type: "get",
 		contentType: "application/json",
		data: {
			"maxResults":1000,
			"offset":0
		},
 		success: function(response) {
 			ZEPHYR.Cycle.sprintList = response;
 		},
 		error : function(response,jqXHR) {
     		showZQLError(jqXHR);
 		}
 	});
}

function getRestURL() {
	var baseURL = contextPath + "/rest/zephyr/latest";
	return baseURL;
}
