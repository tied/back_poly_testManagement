/**
 * @namespace ZEPHYR.ZQL.Bulk
 */

AJS.$.namespace("ZEPHYR.ZQL.Bulk");

ZEPHYR.ZQL.Project = Backbone.Model.extend();
ZEPHYR.ZQL.Cycle = Backbone.Model.extend();
ZEPHYR.ZQL.ProjectCollection = Backbone.Collection.extend({
    model:ZEPHYR.ZQL.Project,
    url: function(){
    	return getJiraRestURL() + "/project"
    },
    parse: function(resp, xhr){
    	return resp
    }
});

ZEPHYR.ZQL.CycleCollection = Backbone.Collection.extend({
    model:ZEPHYR.ZQL.Cycle,
    url: function(){
    	return getRestURL() + "/cycle"
    },
    parse: function(resp, xhr){
    	return resp
    }
});

ZEPHYR.ZQL.CustomFields = [];
var selectedProjectId = null;
var customFieldsData = {};
var customFieldValues = {};
var projectCustomFieldIds = [];

function htmlEncode(value) {
	return AJS.$('<div/>').text(value).html();
}


/**
 * On Project change, get the Versions associated with the project
 */
AJS.$("#execProjectId").live("change", function(e) {
	var model = new ZEPHYR.ZQL.Project();
	model.attributes.key=AJS.$('option:selected', this).attr('key');
	if(AJS.$("#execProjectId").val() != -1) {
		if(model.attributes.key) {
			ZEPHYR.Loading.showLoadingIndicator();
			 fetchProjectVersion(model.attributes.key, function(response) {
					var versions = response.versions;
					AJS.$('#projectVersionId').html("");
					var versionsHtml = AJS.$('#projectVersionId').html("<select id='projectVersionId' class='select' style='max-width:425px'/>");
					AJS.$('#versiondropdown').show();
					AJS.$('#cycledropdown').hide();
					AJS.$('#bulk-move-cycle-form-submit').attr('disabled','disabled');
					versionsHtml.append("<option value='-999' selected:'selected'>"+AJS.I18n.getText('zephyr.je.defects.none') + "</option>");
					if(versions) {
				        AJS.$.each(versions, function(index,version) {
				        	var titleToolTip = version.description ? version.description : version.name;
				        	versionsHtml.append("<option value=\""+ version.id + "\" rel=\""+ version.name + "\" title=\""+ titleToolTip + "\">" + htmlEncode(version.name) + "</option>");
				        });
					}
					versionsHtml.append("<option value='-1'>"+AJS.I18n.getText('zephyr.je.version.unscheduled') + "</option>");
			 });
			 ZEPHYR.Loading.hideLoadingIndicator();
		}
	} else {
		AJS.$('#bulk-move-cycle-form-submit').attr("disabled","disabled");
		AJS.$("#versiondropdown").hide();
		AJS.$("#cycledropdown").hide();
		AJS.$("#newCycleId").hide();
	}
});

/**
 * On Version change, get the cycles associated with the version selected.
 */
AJS.$("#projectVersionId").live("change", function(e) {
	ZEPHYR.ZQL.Cycle.data.searchResults = new ZEPHYR.ZQL.CycleCollection()
	if(AJS.$("#projectVersionId").val() != -999) {
		ZEPHYR.ZQL.Cycle.data.searchResults.fetch({data:{versionId:AJS.$("#projectVersionId").val(),projectId:AJS.$("#execProjectId").val()},
			success:function() {
				AJS.$('#versionCycleId').html("");
				var versionsCycleHtml = AJS.$('#versionCycleId').html("<select id='versionCycleId' class='select' style='max-width:425px'/>");
				AJS.$('#cycledropdown').show();
				versionsCycleHtml.append("<option value='0' selected:'selected'>"+AJS.I18n.getText('zephyr.je.defects.none') + "</option>");
				var count=0;
				AJS.$.each(ZEPHYR.ZQL.Cycle.data.searchResults.models[0].attributes, function(index,cycle) {
		        	if(cycle instanceof Object) {
		        		var cycleName = cycle.name;
		        		if(cycleName.length > 50) {
		        			cycleName = cycleName.substring(0, 50) + "...";
		        		}
		        		versionsCycleHtml.append("<option value=\""+ index + "\" >" + AJS.escapeHtml(cycleName) + "</option>");
		        	}
		        	count=index;
		        });
	    		versionsCycleHtml.append("<option value=\""+ count + "\" rel='newCycle' id='createNewCycleId' title='Create New Cycle'>"+AJS.I18n.getText('zephyr-je.pdb.cycle.add.label') + "</option>");
			},
			error: function(response,jqXHR) {
				showZQLError(jqXHR);
			}
		});
	} else {
		AJS.$('#bulk-move-cycle-form-submit').attr("disabled","disabled");
		AJS.$("#cycledropdown").hide();
		AJS.$("#newCycleId").hide();
	}
});

// Create Cycle if the Create New CYcle is selected, this will enable the input to create cycle
AJS.$("#cycledropdown").live("change",function () {
	   if(AJS.$("option#createNewCycleId:selected").length) {
		   AJS.$("#newCycleId").show();
       AJS.$("#newFolderId").hide();
       AJS.$('#cycleFolderId').html("");
      var cycleFolderHtml = AJS.$('#cycleFolderId').html("<select id='cycleFolderId' class='select' style='max-width:425px'/>");
      AJS.$('#folderdropdown').show();
      cycleFolderHtml.append("<option value='0' selected:'selected'>"+AJS.I18n.getText('zephyr.je.defects.none') + "</option>");
      cycleFolderHtml.append("<option value='newFolder' rel='newFolder' id='createNewFolderId' title='Create New Folder'>"+AJS.I18n.getText('zephyr-je.pdb.folder.add.label') + "</option>");
	   } else {
		    AJS.$("#newCycleId").hide();
        if(AJS.$("#versionCycleId").val() !== 0) {
          var versionId = AJS.$("#projectVersionId").val(),
              projectId = AJS.$("#execProjectId").val(),
              cycleId = AJS.$("#versionCycleId").val();
          fetchCycleFolders(versionId, projectId, cycleId, function(response) {
            AJS.$('#cycleFolderId').html("");
            var cycleFolderHtml = AJS.$('#cycleFolderId').html("<select id='cycleFolderId' class='select' style='max-width:425px'/>");
            AJS.$('#folderdropdown').show();
            cycleFolderHtml.append("<option value='0' selected:'selected'>"+AJS.I18n.getText('zephyr.je.defects.none') + "</option>");
            response.forEach(function(folder, index){
              if(folder instanceof Object) {
                var folderName = folder.folderName;
                if(folderName.length > 50) {
                  folderName = folderName.substring(0, 50) + "...";
                }
                cycleFolderHtml.append("<option value=\""+ folder.folderId + "\" rel=\""+ folder.folderName + "\" title=\""+ folder.folderName + "\">" + AJS.escapeHtml(folderName) + "</option>");
              }
            });
            cycleFolderHtml.append("<option value='newFolder' rel='newFolder' id='createNewFolderId' title='Create New Folder'>"+AJS.I18n.getText('zephyr-je.pdb.folder.add.label') + "</option>");
          })
        }
	   }
});

AJS.$("#folderdropdown").live("change",function () {
    if(AJS.$("option#createNewFolderId:selected").length) {
      AJS.$("#newFolderId").show();
    } else {
      AJS.$("#newFolderId").hide();
    }
})


/*In lack of model layer, persisting data by passing thro' functions*/
var bulkMoveToCycleConfirmationDialog = function (models,targetId) {
	var action = 'move';
	var instance = this,
    dialog = new JIRA.FormDialog({
        id: "bulk-move-cycle-id",
        content: function (callback) {
        	var projects = [];
        	var label = AJS.I18n.getText('enav.bulk.move.cycle.label');
        	for(var i in models) {
        		projects[i] = models[i].attributes;
        	}

        	if(targetId == 'zfj-bulkcopycycle-id') {
        		action='copy';
        		label = AJS.I18n.getText('enav.bulk.copy.cycle.label');
        	}
        	var innerHtmlStr = ZEPHYR.Bulk.bulkCopyMoveToExistingCycle({projects:projects,label:label, action: action});
            callback(innerHtmlStr);
        },
        submitHandler: function (e) {
        	var schedules = Zephyr.selectedSchedules();

            //for analytics
            if (za != undefined) {
                za.track({'event':ZephyrEvents.COPY_MOVE_BULK_EXECUTION,
                        'eventType':'Click',
                        'executionCount': schedules.length
                    },
                    function (res) {
                        console.log('Analytics test: -> ',res);
                    });
            }

        	var performBulkCopyOrMove = function(selectedScheduleItems, action, projectId, versionId, cycleId, folderId, clearStatusMappingFlag, clearDefectMappingFlag,clearExecutionAssignmentsFlag, clearCustomFields){
				Zephyr.unSelectedSchedules();
				dialog.hide();
        		updateBulkMoveToCycle(selectedScheduleItems, action, projectId, versionId, cycleId, folderId, clearStatusMappingFlag, clearDefectMappingFlag, clearExecutionAssignmentsFlag, clearCustomFields, function(){
        			setTimeout(function(){
						globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
	    			},1600);
        		})
        	}
        	var selectedIds = ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds
        	var projectId =  AJS.$('#execProjectId').val();
        	var versionId = AJS.$('#projectVersionId').val();
          var cycleId = AJS.$('#versionCycleId').val();
          var folderId = AJS.$('#cycleFolderId').val();
        	var clearExecutionStatus = AJS.$("#exec-clear-execstatus-id")[0].checked;
        	var clearDefectMappings = AJS.$("#exec-clear-defectmapping-id")[0].checked;
        	var clearExecutionAssignments = AJS.$("#exec-clear-execassignee-id")[0].checked;
          var clearCustomFields = AJS.$("#clear-Custom-Fields-id")[0].checked;

          if(AJS.$("option#createNewCycleId:selected").length && AJS.$("option#createNewFolderId:selected").length) {
            createCycle(projectId, versionId, function () {
              createFolder(projectId, versionId, AJS.$('#cycleId').val(),dialog, function() {
                performBulkCopyOrMove(selectedIds, action, projectId, versionId, AJS.$('#cycleId').val(), AJS.$('#folderId').val(), clearExecutionStatus, clearDefectMappings,clearExecutionAssignments, clearCustomFields);
              })
            })

          } else if(AJS.$("option#createNewCycleId:selected").length) {
        		createCycle(projectId, versionId, function () {
        			performBulkCopyOrMove(selectedIds, action, projectId, versionId, AJS.$('#cycleId').val(), null, clearExecutionStatus, clearDefectMappings,clearExecutionAssignments, clearCustomFields);
        		});
        	} else if(AJS.$("option#createNewFolderId:selected").length) {
            createFolder(projectId, versionId, cycleId,dialog, function() {
              performBulkCopyOrMove(selectedIds, action, projectId, versionId, cycleId, AJS.$('#folderId').val(), clearExecutionStatus, clearDefectMappings,clearExecutionAssignments, clearCustomFields);
            })
          } else {
        		performBulkCopyOrMove(selectedIds, action, projectId, versionId, cycleId, folderId, clearExecutionStatus, clearDefectMappings,clearExecutionAssignments, clearCustomFields);
        	}


        	e.preventDefault();
        }
    });
    dialog.show();
}

/**
 * Gets Project Version by ProjectKey
 * @param projectKey
 * @param successCallback
 * @returns
 */
var fetchProjectVersion = function(projectKey,successCallback) {
	jQuery.ajax({
		url: getJiraRestURL() + "/project/"+ projectKey,
		type : "get",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			if(successCallback) {
				successCallback(response);
			}
		},
		failure : function(response) {
			buildErrorMessage();
		}
	});
}

var fetchCycleFolders = function(versionId, projectId, cycleId, successCallback) {
  jQuery.ajax({
    url: getRestURL() + '/cycle/'+ cycleId +'/folders?projectId='+projectId+'&versionId='+versionId+'&limit=1000&offset=0',
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
      if(successCallback) {
        successCallback(response);
      }
    },
    failure : function(response) {
      buildErrorMessage();
    }
  });
}

/**
 * Create Cycle when user decides to copy/move to new cycle
 * @param projectId
 * @param versionId
 * @param completed
 * @returns
 */
var createCycle = function(projectId,versionId,completed) {
	jQuery.ajax({
		url: getRestURL() + "/cycle",
		type : "post",
		contentType :"application/json",
		data: JSON.stringify( {
			  'name' : AJS.$('#cycle_name').val(),
	          'projectId': projectId,
	          'versionId': AJS.$('#projectVersionId').val()
		}),
		success : function(response) {
			var cid = typeof response == "object" ? response.id : 	typeof response == "string" ? response : "-1";
			AJS.$('#cycleId').val(cid);
			if(completed)
				completed.call();
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if(completed)
				completed.call();
		}
	});
}

/**
 * Create Folder when user decides to copy/move to new folder
 * @param projectId
 * @param versionId
 * @param cycleId
 * @param completed
 * @returns
 */
var createFolder = function(projectId, versionId, cycleId,dialog, completed) {
  jQuery.ajax({
    url: getRestURL() + "/folder/create",
    type : "post",
    contentType :"application/json",
    data: JSON.stringify( {
        'name' : AJS.$('#folder_name').val(),
        'projectId': projectId,
        'versionId': versionId,
        'cycleId' : cycleId
    }),
    success : function(response) {
      var cid = typeof response == "object" ? response.id :   typeof response == "string" ? response : "-1";
      AJS.$('#folderId').val(cid);
      if(completed)
        completed.call();
    },
    error : function(response) {
      /*jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
      if(completed)
          completed.call();*/
        var errorResponse = jQuery.parseJSON(response.responseText);
        var consolidatedErrorMessage = "";
        jQuery.each(errorResponse, function(key, value){
            consolidatedErrorMessage += value + "<br/>";
        });

        AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
            title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
            body: consolidatedErrorMessage
        });
        dialog.$popupContent.find('.spinner').remove();
        AJS.$(':submit', dialog.$form).removeAttr('disabled');

    }
  });
}

/**
 * Bulk Move Schedules to Cycles. If the user selects a different project to copy or move, the backend will silently ignore the schedule.
 * @param selectedScheduleItems
 * @param action
 * @param projectId
 * @param versionId
 * @param cycleId
 * @param clearStatusMappingFlag
 * @param clearDefectMappingFlag
 * @param completed
 * @returns
 */
var updateBulkMoveToCycle = function(selectedScheduleItems,action,projectId,versionId,cycleId,folderId, clearStatusMappingFlag,clearDefectMappingFlag,clearExecutionAssignmentsFlag, clearCustomFields, completed) {
	if(folderId <= 0) {
	    folderId = null;
    }
	jQuery.ajax({
		url: getRestURL() + "/cycle/"+cycleId+"/"+action,
		type : "PUT",
		contentType :"application/json",
		dataType: "json",
		async: false,
		data: JSON.stringify({
			  'executions' : selectedScheduleItems,
			  'projectId':projectId,
			  'versionId':versionId,
			  'folderId': folderId,
			  'clearStatusFlag' : clearStatusMappingFlag,
			  'clearDefectMappingFlag' : clearDefectMappingFlag,
			  'clearAssignmentsFlag' : clearExecutionAssignmentsFlag,
        'clearCustomFields' : clearCustomFields
		}),
		success : function(response) {
			if (response != undefined && response != null) {
				var jobProgressToken = response.jobProgressToken;
				var operation = 'Copied';
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						var statusMessage = AJS.I18n.getText('enav.bulk.copy.cycle.status');
						if (action == 'move') {
							operation = 'Moved';
							statusMessage = AJS.I18n.getText('enav.bulk.move.cycle.status');
						}
						// var innerHtmlStr = ZEPHYR.Bulk.warningBulkMoveCopyDialogContent({operation:operation,statusMessage:statusMessage,response:response});
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkMoveCopyDialogContent({
							warningMsg: AJS.I18n.getText('zephyr.je.bulk.move.or.copy.status.update.in.progress',operation),
							statusMessage: statusMessage,
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "bulk_execution_copy_move_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
								var dataContent = jQuery.parseJSON(data.message);
								var success="",invalid="",projectMismatch="",existing="",noPermissionError="";
								if (dataContent.success != undefined){success=dataContent.success;}
								if (dataContent.invalid != undefined){invalid=dataContent.invalid;}
								if (dataContent.projectMismatch != undefined){projectMismatch=dataContent.projectMismatch;}
								if (dataContent.existing != undefined){existing=dataContent.existing;}
								if (dataContent.noPermissionError != undefined){noPermissionError=dataContent.noPermissionError;}
								AJS.$("#cycle-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.movecopy.operation.warn', operation,success,invalid,projectMismatch,existing,noPermissionError));
								clearInterval(intervalId);
								if (completed)
									completed.call();
							}
						}
					})
				}, 1000);
			}
		},
		error : function(response) {
            var message = JSON.parse(response.responseText).error;
            showErrorMessage(message, 5000);
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if(completed)
				completed.call();
		}
	});
}


/**
 * Bulk Associate Defect Dialog
 * @param model
 * @returns
 */
var bulkAssociateDefectConfirmationDialog = function (model) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "bulk-associate-defect-id",
        content: function (callback) {
        	var innerHtmlStr = ZEPHYR.Bulk.bulkAssociateDefects({contextPath:contextPath});
            callback(innerHtmlStr);
    		AJS.$(document.body).find('.aui-field-tescasepickers').each(function () {
    	    	new JIRA.IssuePicker({
    	                element: AJS.$(this),
    	                userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
    	                uppercaseUserEnteredOnSelect: true
    	        });
    	    });
    		// By default disabling the [Save] button and enabling it if any defects are attached
    		AJS.$('#bulk-move-cycle-form-submit').attr('disabled','disabled');
    		AJS.$('#zephyr-je-testkey-textarea').on('focusin focusout keydown keyup', function(ev) {
    			if(AJS.$('#zephyr-je-testkey option').length == 0)
    				AJS.$('#bulk-move-cycle-form-submit').attr('disabled','disabled');
    			else if(AJS.$('#bulk-move-cycle-form-submit').is(":disabled"))
    				AJS.$('#bulk-move-cycle-form-submit').removeAttr('disabled');
    		});
        },
        submitHandler: function (e) {
        	var schedules = Zephyr.selectedSchedules();


        	var value = [];
        	AJS.$('#zephyr-je-testkey-multi-select ul.items li.item-row').each(function() {
        		var val = AJS.$(this).find("button.value-item span.value-text");
        		value.push(val.text());
        	});

            //for analytics
            if (za != undefined) {
                za.track({'event':ZephyrEvents.BULK_ASSOCIATE_DEFECTS,
                        'eventType':'Click',
                        'executionDefectCount': value.length
                    },
                    function (res) {
                        console.log('Analytics test: -> ',res);
                    });
            }

            updateAssociateDefects(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds,
				value,
				function () {
        			Zephyr.unSelectedSchedules();
        			dialog.hide();
					//refresh result after operation.
					globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
            });
			e.preventDefault();
         }
    });
    dialog.show();
}

/**
 * Update Schedules and assign the defects to it in bulk
 * @param selectedScheduleItems
 * @param defects
 * @param completed
 * @returns
 */
var updateAssociateDefects = function(selectedScheduleItems,defects,completed) {
	jQuery.ajax({
		url: getRestURL() + "/execution/updateWithBulkDefects",
		type : "PUT",
		contentType :"application/json",
		dataType: "json",
		async: false,
		data: JSON.stringify({
			  'executions' : selectedScheduleItems,
			  'defects':defects,
			  'detailedResponse':false
		}),
		success : function(response) {
			if(response != undefined && response != null) {
				if (response.error != undefined){return;}
				var jobProgressToken = response.jobProgressToken;
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkAssociateDefectDialogContent({
							html: AJS.I18n.getText('zephyr.je.bulk.associate.defect.update.in.progress'),
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "bulk_execution_associate_defect_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
								var html="", linked="", alreadylinked="", invalid="", noPermission="", noIssuePermission="";
								var invalidDefects = JSON.parse(data.message).invalidDefect;
								AJS.$.each(JSON.parse(data.message), function (index, value) {
									if (index=='linked'){linked = value;}
									if (index=='alreadylinked'){alreadylinked = value;}
									if (index=='invalid'){invalid = value;}
									if (index=='noPermission'){noPermission = value;}
									if (index=='noIssuePermission'){noIssuePermission = value;}
 									html += ZEPHYR.Bulk.bulkAssociateDefectResult({
										linked: linked,
										alreadylinked: alreadylinked,
										invalid: invalid,
										noPermission: noPermission,
										noIssuePermission: noIssuePermission,
										scheduleId: index
									});
								});
								if (invalidDefects) {
									html += "<p><span>" + AJS.I18n.getText('enav.bulk.associatedefect.invalidDefect', invalidDefects) + "</span></p>";
								}
								if (html == "") {
									html = AJS.I18n.getText('enav.bulk.result');
								}
								AJS.$("#cycle-aui-message-bar .aui-message").html(html);
								clearInterval(intervalId);
								if (completed)
									completed.call();
							}
						}
					})
				}, 1000);

			}
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if(completed)
				completed.call();
		}
	});
}

/**
 * Bulk Associate Defect Dialog
 * @param model
 * @returns
 */
var bulkAssignUserConfirmationDialog = function (model) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "bulk-assign-user-id",
        content: function (callback) {
        	var innerHtmlStr = ZEPHYR.Bulk.bulkAssignUser();
            callback(innerHtmlStr);
    		// Attach Assignee user picker
    		ZEPHYR.Execution.Assignee.init({
    			id: 'zephyr-je-execution-assignee-bulk'
    		});
          var _assignee = {
            "executionId": null,
            "assignee": null,
            "assigneeDisplay": null,
            "assigneeUserName": null,
            "assigneeType": null
          };
          AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath ] );
          //AJS.$('#exec-assignee-wrapper').append('<div class="description">' + AJS.I18n.getText("enav.bulk.assign.user.warn.label") + '</div>');

        },
        submitHandler: function (e) {
        	e.preventDefault();
        	var schedules = Zephyr.selectedSchedules();
        	var value = [];
        	var assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams(
        		'exec-assignee-wrapper'
        	);
        	if(!assigneeParams)
        		return;

			//for analytics
            if (za != undefined) {
                za.track({'event':ZephyrEvents.ADD_ASSIGNEE_BULK_EXECUTION,
                        'eventType':'Click',
                        'executionCount': schedules.length
                    },
                    function (res) {
                        console.log('Analytics test: -> ',res);
                    });
            }


        	updateAssignUser(
        		ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds,
        		assigneeParams,
        		function () {
	        		Zephyr.unSelectedSchedules();
	        		dialog.hide();
					//refresh result after move/copy.
					globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
	            }
        	);
			e.preventDefault();
        }
    });
    dialog.show();
}

/**
 * Update Schedules and assign the defects to it in bulk
 * @param selectedScheduleItems
 * @param assigneeType
 * @param! assignee if assigneeType is 'assignee' then pass the selected username
 * @param completed
 * @returns
 */
var updateAssignUser = function(selectedScheduleItems, assigneeParams,completed) {
	var bulkParams = {
		  'executions' : selectedScheduleItems
		};
    if(assigneeParams.assignee !== 'null' && assigneeParams.assigneeType !== 'null'){
      _.extend(bulkParams, assigneeParams); // Merge assigneeParams to bulk params
    }
	jQuery.ajax({
		url: getRestURL() + "/execution/bulkAssign",
		type : "PUT",
		contentType :"application/json",
		dataType: "text",
		async: false,
		data: JSON.stringify(bulkParams),
		success : function(response) {
			if (response != undefined && response != null) {
				response = JSON.parse(response);
				var jobProgressToken = response.jobProgressToken;
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						// var innerHtmlStr = ZEPHYR.Bulk.warningBulkAssignUserDialogContent({response:response});
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkAssignUserDialogContent({
							warningMsg: AJS.I18n.getText('zephyr.je.bulk.execution.assign.user.in.progress'),
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "bulk_execution_assign_user_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
  								var dataContent = jQuery.parseJSON(data.message);
								var errZPermission = ((dataContent.noZephyrPermission != undefined && dataContent.noZephyrPermission != "-")?dataContent.noZephyrPermission:"");
								var errPermission = ((dataContent.noPermission != undefined && dataContent.noPermission != "-")?dataContent.noPermission:"");
								AJS.$("#bulk-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.assign.operation.warn',dataContent.success, dataContent.error, errZPermission+" "+errPermission));
								clearInterval(intervalId);
								if (completed)
									completed.call();
							}
						}
					})
				}, 1000);
			}
		},
		error : function(response) {
			var ctx = AJS.$("#bulk-assign-user-id .zephyr-aui-message-bar");
    		ctx.empty();

			AJS.messages.error(ctx, {
			    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
			    body: getErrorMessageFromResponse(response),
			    closeable: true
			});
			AJS.$("#bulk-assign-user-id .buttons-container").find('span.icon.throbber').removeClass('loading');
			AJS.$("#bulk-assign-user-id .buttons-container").find('#bulk-assign-user-form-submit').removeAttr('disabled');
		}
	});
}

//handles Bulk Status change event
AJS.$("#zfj-bulkstatuschange-id").live("click", function(e) {
	e.preventDefault();
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		 bulkStatusChangeConfirmationDialog(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes);
		 return;
	}
});

//Enable the button when the cycle is selected .
AJS.$("#versionCycleId").live("change", function(e) {
	AJS.$("#cycle_name").val("");
	if(AJS.$("#versionCycleId").val() != 0
			&& AJS.$("#versionCycleId :selected").attr("rel") != 'newCycle') {
		AJS.$('#bulk-move-cycle-form-submit').removeAttr("disabled");
	} else {
		AJS.$('#bulk-move-cycle-form-submit').attr("disabled","disabled");
	}
});


//Enable the button when the cycle cycle name text is changed.
AJS.$("#cycle_name").live('input',function(e) {
	if(AJS.$("#cycle_name").val().trim() != '') {
		AJS.$('#bulk-move-cycle-form-submit').removeAttr("disabled");
	} else {
		AJS.$('#bulk-move-cycle-form-submit').attr("disabled","disabled");
	}
});

//Intercept the Move to Cycle and Move to New Cycle call
AJS.$("#zfj-bulkmovecycle-id,#zfj-bulkcopycycle-id").live("click", function(e) {
	e.preventDefault();
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		 bulkMoveToCycleConfirmationDialog(ZEPHYR.ZQL.Project.data.searchResults.models,this.id);
		 return;
	}
});

//Associate defect bulk event triggered
AJS.$("#zfj-bulkassociatedefect-id").live("click", function(e) {
	e.preventDefault();
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		 bulkAssociateDefectConfirmationDialog(ZEPHYR.ZQL.Project.data.searchResults.models);
		 return;
	}
});

// Bulk assign user event
AJS.$('#zfj-bulkassignuser-id').live('click', function(ev) {
	ev.preventDefault();
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		bulkAssignUserConfirmationDialog(ZEPHYR.ZQL.Project.data.searchResults.models);
		return;
	}
});

//Bulk delete event triggered
AJS.$("#zfj-bulkdelete-id").live("click", function(e) {
	e.preventDefault();
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		 bulkDeleteConfirmationDialog(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes);
		 return;
	}
});

AJS.$('#zfj-bulkCustomField-id').live('click', function(e) {
  e.preventDefault();
  if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length > 0) {
		 bulkEditCustomFieldDialog(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes);
		 return;
	}
});

//Bulk Step Status Select box shown/hidden here based on checkbox selection
AJS.$("#exec_status_step_bulk_change").live("click",function() {
	if(AJS.$("#exec_status_step_bulk_change")[0].checked) {
		AJS.$('#stepStatusChangeId').show();
	} else {
		AJS.$('#stepStatusChangeId').hide();
	}
});

//Prevent Tools dropdown based if there are no execution selected
AJS.$("#enavBulkToolId").live("click",function(e) {
	if(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds.length == 0) {
		AJS.$("#toolOptions-dropdown").css("display","block");
	} else {
		AJS.$("#enavBulkToolId").removeClass("disabled");
	}
});

/*In lack of model layer, persisting data by passing thro' functions*/
var bulkStatusChangeConfirmationDialog = function (attributes) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "bulk-change-id",
        content: function (callback) {
			var executionStatuses = attributes.executionStatuses;
			var stepExecutionStatuses = attributes.stepExecutionStatuses;

        	/*Short cut of creating view, move it to Backbone View and do it in render() */
        	var innerHtmlStr = ZEPHYR.Bulk.bulkStatusChange({executionStatuses:executionStatuses,stepExecutionStatuses:stepExecutionStatuses});
            callback(innerHtmlStr);
        },
        submitHandler: function (e) {
            var schedules = Zephyr.selectedSchedules();

            //for analytics
            if (za != undefined) {
                za.track({'event':ZephyrEvents.UPDATE_BULK_EXECUTION_STATUS,
                        'eventType':'Click',
						'executionCount': schedules.length
                    },
                    function (res) {
                        console.log('Analytics test: -> ',res);
                    });
            }

        	updateBulkStatus(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds, AJS.$("#exec_status-bulk_change").val(), AJS.$("#exec_status_step_bulk_change").is(':checked'),
        	   AJS.$("#exec-clear-defectmapping-id").is(':checked'), function () {
    			setTimeout(function(){
					globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
            	},1600);
    			Zephyr.unSelectedSchedules();
        		dialog.hide();
            });
            e.preventDefault();
        }
    });
    // change the defect mapping
    var updateDefectMappingCB = function(e){
    	var defectMappingDiv = AJS.$("#exec-clear-defectmapping-div")
    	if(AJS.$("#exec_status-bulk_change").val() == -1) {
    		defectMappingDiv.css("display", "block");
    	} else {
    		AJS.$(defectMappingDiv).find('input.checkbox').attr('checked', false);	//Undo the selection when we are about to hide the checkbox
    		defectMappingDiv.css("display", "none");
    	}
    }

    dialog.show();
    AJS.$("#exec_status-bulk_change").live("change", updateDefectMappingCB);
    updateDefectMappingCB();
}

var fetchGlobalCustomFields = function () {
  jQuery.ajax({
    url: getRestURL() + "/customfield/entity?entityType=EXECUTION",
    type: "GET",
    contentType: "application/json",
    dataType: "json",
    async: false,
    success: function (response_global) {
      customFieldsData['global'] = renderAllCustomFields(response_global);
    }
  });
}

var attachCustomEvents = function() {
  AJS.$('drop-downcheckbox').unbind('submitvalue');
  AJS.$('drop-downcheckbox').bind('submitvalue', _submitCustomField);
  AJS.$('drop-downradio').unbind('submitvalue');
  AJS.$('drop-downradio').bind('submitvalue', _submitCustomField);
  AJS.$('drop-down').unbind('submitvalue');
  AJS.$('drop-down').bind('submitvalue', _submitCustomField);
  AJS.$('drop-downmultiselect').unbind('submitvalue');
  AJS.$('drop-downmultiselect').bind('submitvalue', _submitCustomField);
  AJS.$('custom-textarea').unbind('submitvalue');
  AJS.$('custom-textarea').bind('submitvalue', _submitCustomField);
  AJS.$('custom-text').unbind('submitvalue');
  AJS.$('custom-text').bind('submitvalue', _submitCustomField);
  AJS.$('drop-downdate').unbind('triggerdatechooser');
  AJS.$('drop-downdate').bind('triggerdatechooser', _triggerdatechooserCustomField);
}

var bulkEditCustomFieldDialog = function (attributes) {
	if (AJS.$("#bulk-edit-customField").length > 0) {
		AJS.$("#bulk-edit-customField").remove();
	}

  fetchGlobalCustomFields();

  var instance = this,
  dialog = new JIRA.FormDialog({
      id: "bulk-edit-customField",
      content: function (callback) {
		/*Short cut of creating view, move it to Backbone View and do it in render() */
		  var innerHtmlStr = ZEPHYR.Bulk.bulkEditCustomFieldProjectSelection({customfieldsData: customFieldsData, mode: 'edit' });
        // var innerHtmlStr = ZEPHYR.Bulk.bulkEditCustomField({customFields: ZEPHYR.ZQL.CustomFields});
          callback(innerHtmlStr);
          AJS.messages.warning("#custom-warning", {body: AJS.I18n.getText('message.custom-field.warning'), closeable: false});
          try {
			var userAgent = navigator.userAgent.toLowerCase();
			if(userAgent.indexOf('msie') > -1 || userAgent.indexOf('trident') > -1 || userAgent.indexOf('edge') > -1) {
				var handleJiraMouseEvents = function(ev) {
					ev.stopPropagation();
					ev.stopImmediatePropagation();
				};
				document.getElementById('custom-fields-outer-container').addEventListener('mousedown', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mouseup', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mouseenter', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mouseleave', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mousemove', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mouseout', handleJiraMouseEvents, true);
				document.getElementById('custom-fields-outer-container').addEventListener('mouseover', handleJiraMouseEvents, true);
			}
		}
		catch(err) {
			console.log('navigator exception');
		}
          attachCustomEvents();
      },
      submitHandler: function (e) {
        submitCustomFields(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes.executionIds,customFieldValues, selectedProjectId,function () {
        setTimeout(function(){
			globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
            },1600);
        Zephyr.unSelectedSchedules();
        selectedProjectId = null;
        customFieldsData = {};
        customFieldValues = {};
          dialog.hide();
          });
          e.preventDefault();
      }
    });
    dialog.show();
  for (var counter = 0; counter < ZEPHYR.ZQL.CustomFields.length; counter += 1) {
    if(ZEPHYR.ZQL.CustomFields[counter].customFieldType == 'DATE_TIME') {
      dateTimePickerBulkEdit(ZEPHYR.ZQL.CustomFields[counter].customFieldId, ZEPHYR.ZQL.CustomFields[counter]);
    } else if(ZEPHYR.ZQL.CustomFields[counter].customFieldType == 'DATE') {
      datePickerBulkEdit(ZEPHYR.ZQL.CustomFields[counter].customFieldId, ZEPHYR.ZQL.CustomFields[counter]);
    }
  }
}

var _submitCustomField = function(ev) {
  //console.log(ev, ev.target.dataset, ev.originalEvent.detail);
  var customFieldId = ev.target.dataset.customfieldid,
        customFieldType = ev.originalEvent.detail.type,
        value, selectedOptions;
  if(customFieldType === 'SINGLE_SELECT' || customFieldType === 'MULTI_SELECT'
      || customFieldType === 'RADIO_BUTTON' || customFieldType === 'CHECKBOX') {
      value = Array.isArray(ev.originalEvent.detail.contentValue) ? ev.originalEvent.detail.contentValue.join(',') : ev.originalEvent.detail.contentValue,
      selectedOptions = Array.isArray(ev.originalEvent.detail.value) ? ev.originalEvent.detail.value.join(',') : ev.originalEvent.detail.value;

  } else {
      value = Array.isArray(ev.originalEvent.detail.value) ? ev.originalEvent.detail.value.join(',') : ev.originalEvent.detail.value;
      selectedOptions = '';
  }
  customFieldValues[customFieldId] = {
    customFieldId : parseInt(customFieldId),
    value: value,
    entityType: "EXECUTION",
    selectedOptions: selectedOptions
  }
    //console.log('customFieldValues', customFieldValues);
};

var _triggerdatechooserCustomField = function(ev) {

  if (ev.originalEvent.detail.onlyUpdateValue) {
      ev.originalEvent.detail.value = '';
      ev.originalEvent.detail.type = ev.target.dataset.type;
      _submitCustomField(ev);
  } else {
    var inputDateField = AJS.$('#date-pickerCustomField');
    var positionInputFiled = inputDateField[0].getBoundingClientRect();
	var targetPosition = ev.target.getBoundingClientRect();
	var dateValue = '';
	var date;
    inputDateField.val('');
    inputDateField.css({ "top": 0, "left": 0 })
    inputDateField.css({ "top": (targetPosition.top + targetPosition.height) - positionInputFiled.top, "left": targetPosition.left - positionInputFiled.left });
    inputDateField.datetimepicker({
      value: ev.originalEvent.detail.value,
      step: 30,
      timepicker: (ev.target.dataset.type === 'DATE') ? false : true,
      format: (ev.target.dataset.type === 'DATE') ? 'm/d/Y' : 'n/j/Y H:i',
      onChangeDateTime: function (dp, $input) {
        date = dp;
        var value = date.getTime();
        var customFieldId = ev.target.dataset.customfieldid;
		value = Math.round(value / 1000);
		dateValue = value;
	  },
	  onClose: function() {
		  if(dateValue) {
			  ev.originalEvent.detail.value = dateValue.toString();
			  ev.originalEvent.detail.type = ev.target.dataset.type;
			  var displayDate = convertDate({
				  value: date,
				  isDateTime: ev.target.dataset.type !== 'DATE'
			  });
        var customFieldId = ev.target.dataset.customfieldid;
			  console.log('display date', displayDate);
			  AJS.$('drop-downdate[data-customfieldid=' + customFieldId + ']').attr('options', JSON.stringify([{ value: displayDate }]));
			  _submitCustomField(ev);
			  inputDateField.datetimepicker('destroy');
		  }
		inputDateField.datetimepicker('destroy');
		inputDateField.css({"top":0, "left" : 0});
	  }
    });
    inputDateField.datetimepicker('show');
  }
};

function convertDate(data) {
  if(data.value) {
    date = new Date(Number(data.value));
    h =  date.getHours(), m = date.getMinutes();
	  if (m < 10) {
		  m = "0" + m;
	  }
	  if (h < 10) {
		  h = "0" + h;
	  }
	  time = h + ':' + m;
    if(data.isDateTime) {
      return dateStr = (date.getMonth()+1) + '/' + date.getDate() + '/' + date.getFullYear() + ' ' + time;
    } else {
      return dateStr = (date.getMonth()+1) + '/' + date.getDate() + '/' + date.getFullYear();
    }
  } else {
    return '';
  }
}
AJS.$('#enable-project-custom-fields').live('click', function(e){
  //AJS.$('.select-group').toggleClass('hide');
  // if(AJS.$(this).attr('checked')) {
  //   AJS.$('.select-group').show();
  // } else {
  //   AJS.$('.select-group').hide();
  // }
  var isChecked = false,
      selectedProjectId = null;
  if(AJS.$(this).attr('checked')) {
    isChecked = true;
  }
  if(!isChecked) {
    AJS.$('#custom-fields-outer-container .project-custom-fields').remove();
    projectCustomFieldIds.forEach(function(customFieldId) {
      delete customFieldValues[customFieldId];
    })
  }
  var projectList = [];
    for (var counter = 0; counter < ZEPHYR.ZQL.Project.data.searchResults.models.length; counter += 1) {
      projectList.push(ZEPHYR.ZQL.Project.data.searchResults.models[counter].attributes);
  }
  var dropDownContent = ZEPHYR.Bulk.renderProjectDropdown({projectList: projectList, selectedProjectId: selectedProjectId, isChecked: isChecked});
  AJS.$('#select-project-dropdown').remove();
  AJS.$('.type-select').append(dropDownContent);
});

AJS.$("#bulk-custom-fields-update-cancle-button").live("click", function (e) {
	selectedProjectId = null;
	customFieldsData = {};
  customFieldValues = {};
	// e.preventDefault();
});

//handles Bulk Status change event
AJS.$("#select-project").live("change", function (e) {
	e.preventDefault();
	jQuery.ajax({
		url: getRestURL() + "/customfield/byEntityTypeAndProject?entityType=EXECUTION&projectId=" + e.target.value,
		type: "GET",
		contentType: "application/json",
		dataType: "json",
		async: false,
		success: function (response_project) {
			selectedProjectId = e.target.value;
      projectCustomFieldIds = response_project.length && response_project.map(function(customField) {
        return customField.id;
      });
      customFieldsData['project'] = renderAllCustomFields(response_project);

      var projectList = [];
      for (var counter = 0; counter < ZEPHYR.ZQL.Project.data.searchResults.models.length; counter += 1) {
          projectList.push(ZEPHYR.ZQL.Project.data.searchResults.models[counter].attributes);
      }

      console.log('all custom fields data', customFieldsData);

			// var innerHtmlStr = ZEPHYR.Bulk.bulkEditCustomField({ customFields: ZEPHYR.ZQL.CustomFields });
			// AJS.$('#custom-fields-outer-container').append(innerHtmlStr);
			// bulkEditCustomFieldDialog(ZEPHYR.ZQL.ZQLSearch.data.searchResults.models[0].attributes);
		  var htmlContent = ZEPHYR.Bulk.renderBulkCustomFields({customFields: customFieldsData['project'], mode: 'edit', title: 'Custom Fields(Project)', className:'project-custom-fields'})


      //console.log('htmlCOntent', htmlContent);
      AJS.$('#custom-fields-outer-container .project-custom-fields').remove();
      AJS.$('#custom-fields-outer-container').prepend(htmlContent);

      var dropDownContent = ZEPHYR.Bulk.renderProjectDropdown({projectList: projectList, selectedProjectId: selectedProjectId, isChecked: true});

      AJS.$('#select-project-dropdown').remove();
      AJS.$('.type-select').append(dropDownContent);
      attachCustomEvents();
    },
		error: function (response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if (completed)
				completed.call();
		}
	});
});

var renderAllCustomFields = function(response) {
  var jsonData = [];
  response.forEach(function(customField) {
    jsonData.push({
      "customFieldValueId": null,
      "customFieldId": customField.id,
      "customFieldName": customField.name,
      "customFieldType": customField.fieldType,
      "value": "",
      "options": customField.customFieldOptionValues
    })
  });
  console.log('jsonData in bulk dialog', jsonData);

  var executionCustomFieldsData = [];
  Object.keys(jsonData).forEach(function(key) {
    if ((jsonData[key].customFieldType == 'DATE' || jsonData[key].customFieldType == 'DATE_TIME') && jsonData[key].value) {
      jsonData[key].displayDate = convertDate({value: jsonData[key].value, isDateTime: jsonData[key].customFieldType == 'DATE_TIME'});
      jsonData[key].options = [{'value': jsonData[key].displayDate }];
    } else if (jsonData[key].customFieldType == 'MULTI_SELECT' || jsonData[key].customFieldType == 'CHECKBOX'
          || jsonData[key].customFieldType == 'RADIO_BUTTON' || jsonData[key].customFieldType == 'SINGLE_SELECT') {
      var newOptions = [];
      var selectedArray = jsonData[key].selectedOptions && jsonData[key].selectedOptions.split(',');

      Object.keys(jsonData[key].options).forEach(function(id) {
        var tempOption = {};
        tempOption.value = id;
        tempOption.content = jsonData[key].options[id];
        tempOption.selected = (selectedArray && selectedArray.length) ? selectedArray.indexOf(id) >= 0 : false;
        newOptions.push(tempOption);
      })

      jsonData[key].options = newOptions;
    } else {
      jsonData[key].options = [{
        value: jsonData[key].value,
        content: jsonData[key].value,
        type: jsonData[key].customFieldType
      }];
    }
    jsonData[key].options = JSON.stringify(jsonData[key].options);
    jsonData[key].imgUrl = contextPath + configureTriggerOptions(jsonData[key].customFieldType);
    executionCustomFieldsData.push(jsonData[key]);
  });
  return executionCustomFieldsData;
}

// change the defect mapping
var updateDefectMappingCB = function(e){
  var defectMappingDiv = AJS.$("#exec-clear-defectmapping-div")
  if(AJS.$("#exec_status-bulk_change").val() == -1) {
    defectMappingDiv.css("display", "block");
  } else {
    AJS.$(defectMappingDiv).find('input.checkbox').attr('checked', false);	//Undo the selection when we are about to hide the checkbox
    defectMappingDiv.css("display", "none");
  }
}

/*In lack of model layer, persisting data by passing thro' functions*/
var bulkDeleteConfirmationDialog = function (attributes) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "execution-delete-dialog",
        content: function (callback) {
        	var innerHtmlStr = ZEPHYR.Bulk.deleteExecutionConfirmationDialog();
            callback(innerHtmlStr);
        },

        submitHandler: function (e) {
        	var schedules = Zephyr.selectedSchedules();
            //for analytics
            if (za != undefined) {
                za.track({'event':ZephyrEvents.DELETE_BULK_EXECUTION,
                        'eventType':'Click',
                        'executionCount': schedules.length
                    },
                    function (res) {
                        console.log('Analytics test: -> ',res);
                    });
            }
			var selectedSchedules = attributes.executionIds;
        	deleteSchedules(attributes.executionIds, dialog, function () {
				Zephyr.unSelectedSchedules();
				dialog.hide();
    			setTimeout(function() {
    				var maxResultAllowed = attributes.maxResultAllowed;
    				var totalCount = attributes.totalCount;
    				var currentOffset = attributes.offset;
    				if((totalCount - selectedSchedules.length) % maxResultAllowed == 0) {
						ZEPHYR.ZQL.pagination.offset = currentOffset - maxResultAllowed;
    				}
					globalDispatcher.trigger("searchZQLEvent", AJS.$("#zqltext").val(), ZEPHYR.ZQL.pagination.offset, ZEPHYR.ZQL.pagination.maxResultAllowed);
    			},1600);
            });
			e.preventDefault();
         }
    });

    dialog.show();
}

var submitCustomFields = function(selectedScheduleItems,values,selectedProjectId, completed) {
  //selectedProjectId = AJS.$("#select-project").val() == '-1' ? '' : AJS.$("#select-project").val();
  //var projectSuffix = selectedProjectId ? '?projectId=' + selectedProjectId : ''
  jQuery.ajax({
		url: getRestURL() + "/execution/bulkAssignCustomFields",
		type : "PUT",
		contentType :"application/json",
		dataType: "json",
		async: false,
		data: JSON.stringify({'executions':selectedScheduleItems,
					  'customFieldValues':values
		}),
		success : function(response) {
			if (response != null || response != undefined) {
				var jobProgressToken = response.jobProgressToken;
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkCustomFieldsDialogContent({
							warningMsg: AJS.I18n.getText('enav.bulk.status.customFieldAdd.label'),
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "bulk_execution_customfields_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
 							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
								var dataContent = jQuery.parseJSON(data.message);
								var errZPermission = (dataContent && (dataContent.noZephyrPermission != undefined && dataContent.noZephyrPermission != "-")?dataContent.noZephyrPermission:"");
								var errPermission = (dataContent && (dataContent.noPermission != undefined && dataContent.noPermission != "-")?dataContent.noPermission:"");
								AJS.$("#cycle-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.status.operation.warn',dataContent ? dataContent.success : '', dataContent ? dataContent.error : '', errZPermission+" "+errPermission));
								clearInterval(intervalId);
								if (completed)
									completed.call();
							}
						}
					})
				}, 1000);

				if (ZephyrZQLFilters)
					setTimeout(function () {
						ZephyrZQLFilters.findFiltersByUser();
					}, 1200);
			}
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if(completed)
				completed.call();
		}
	});
}

//bulk status change
var updateBulkStatus = function(selectedScheduleItems,status,testStepStatusChangeFlag,clearDefectMappingFlag,completed) {
	jQuery.ajax({
		url: getRestURL() + "/execution/updateBulkStatus",
		type : "PUT",
		contentType :"application/json",
		dataType: "json",
		async: false,
		data: JSON.stringify({'executions':selectedScheduleItems,
					  'status':status,
					  'stepStatus':AJS.$('#exec_step_status_bulk_change').val(),
					  'testStepStatusChangeFlag' : testStepStatusChangeFlag,
					  'clearDefectMappingFlag' : clearDefectMappingFlag
		}),
		success : function(response) {
			if (response != null || response != undefined) {
				var jobProgressToken = response.jobProgressToken;
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkStatusDialogContent({
							warningMsg: AJS.I18n.getText('zephyr.je.bulk.execution.status.update.in.progress'),
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "update_bulk_execution_status_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
 							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
								var dataContent = jQuery.parseJSON(data.message);
								var errZPermission = ((dataContent.noZephyrPermission != undefined && dataContent.noZephyrPermission != "-")?dataContent.noZephyrPermission:"");
								var errPermission = ((dataContent.noPermission != undefined && dataContent.noPermission != "-")?dataContent.noPermission:"");
								AJS.$("#cycle-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.status.operation.warn',dataContent.success, dataContent.error, errZPermission+" "+errPermission));
								clearInterval(intervalId);
								if (completed)
									completed.call();
							}
						}
					})
				}, 1000);

				if (ZephyrZQLFilters)
					setTimeout(function () {
						ZephyrZQLFilters.findFiltersByUser();
					}, 1200);
			}
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			if(completed)
				completed.call();
		}
	});
}

var deleteSchedules = function(selectedScheduleItems, deletionDialog, completed){
	jQuery.ajax({
		url: getRestURL() + "/execution/deleteExecutions" ,
		type : "delete",
		contentType :"application/json",
		dataType: "json",
		data: JSON.stringify({
			  'executions' : selectedScheduleItems
		}),
		success : function(response) {
			if (response != null || response != undefined) {
				var jobProgressToken = response.jobProgressToken;
				var msgDlg = new JIRA.FormDialog({
					id: "warning-message-dialog",
					content: function (callback) {
						var innerHtmlStr = ZEPHYR.Bulk.warningBulkDeleteDialogContent({
							warningMsg: AJS.I18n.getText('zephyr.je.bulk.execution.delete.in.progress'),
							progress: 0,
							percent: 0,
							timeTaken: 0
						});
						callback(innerHtmlStr);
					}
				});
				msgDlg.show();

				var intervalId = setInterval(function () {
					jQuery.ajax({
						url: contextPath + "/rest/zephyr/latest/execution/jobProgress/" + jobProgressToken,
						data: {'type': "bulk_executions_delete_job_progress"},
						complete: function (jqXHR, textStatus) {
							var data = jQuery.parseJSON(jqXHR.responseText);
							AJS.$(".aui-progress-indicator").attr("data-value", data.progress);
							AJS.$(".aui-progress-indicator-value").css("width", data.progress * 100 + "%");
							AJS.$(".timeTaken").html("Time Taken: " + data.timeTaken);
							if (data.progress == 1) {
								var dataContent = jQuery.parseJSON(data.message);
								if(dataContent.conflict) {
                                    AJS.$("#execs-delete-aui-message-bar .aui-message").html(dataContent.conflict);
								} else {
                                    var errZPermission = ((dataContent.noIssuePermission != undefined && dataContent.noIssuePermission != "-") ? dataContent.noIssuePermission : "");
                                    var errPermission = ((dataContent.noPermission != undefined && dataContent.noPermission != "-") ? dataContent.noPermission : "");
                                    AJS.$("#execs-delete-aui-message-bar .aui-message").html(AJS.I18n.getText('enav.bulk.delete.operation.warn', dataContent.success, dataContent.error, errZPermission + " " + errPermission));
                                }
								clearInterval(intervalId);
								if (completed)
									completed.call();
							} else if(data.errorMessage) {
								var dataContent = jQuery.parseJSON(data.message);
								AJS.$("#execs-delete-aui-message-bar .aui-message").html(data.errorMessage);
								clearInterval(intervalId);
							}
						}
					})
				}, 1000);
			}
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
			AJS.$(':submit', deletionDialog.$form).removeAttr('disabled');
			deletionDialog.$form.removeClass("submitting");
		}
	});
}

var getBulkCustomField =  function (projectId){
  jQuery.ajax({
		url: getRestURL() + "/customfield/entity?entityType=EXECUTION",
		type : "get",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			// getBulkCustomFieldByProject(response, projectId)
      var newResponse = [];
      for (var counter = 0; counter < response.length; counter += 1) {
  			var temp = JSON.parse(JSON.stringify(response[counter]));
  			if (temp.customFieldOptionValues) {
  				temp.customFieldOptionValues = [];
  				for (var key in response[counter].customFieldOptionValues) {
  					if (response[counter].customFieldOptionValues.hasOwnProperty(key)) {
  						temp.customFieldOptionValues.push(response[counter].customFieldOptionValues[key]);
  					}
  				}
  			}
  			newResponse.push(temp);
  		}
      var jsonData = {};
      for (var counter = 0; counter < newResponse.length; counter += 1) {
  			jsonData[newResponse[counter].id] = {
  				"customFieldId": newResponse[counter].id,
  				"customFieldName": newResponse[counter].name,
  				"customFieldType": newResponse[counter].fieldType,
  				"value": "",
  				"options": newResponse[counter].customFieldOptionValues,
  			};
      }

      Object.keys(jsonData).forEach(function(key) {
    		if (jsonData[key].customFieldType == 'DATE' && jsonData[key].value) {
    			jsonData[key].displayDate = convertDate({value: jsonData[key].value, isDateTime: false});
    		} else if (jsonData[key].customFieldType == 'DATE_TIME' && jsonData[key].value) {
    			jsonData[key].displayDateTime = convertDate({value: jsonData[key].value, isDateTime: true});
    		} else if (jsonData[key].customFieldType == 'MULTI_SELECT' || jsonData[key].customFieldType == 'RADIO_BUTTON' || jsonData[key].customFieldType == 'CHECKBOX') {
    			var newOptions = [];
    			var selectedArray = jsonData[key].value.split(',');
    			for(var counter = 0; counter < jsonData[key].options.length; counter += 1) {
    				var tempOption = {};
    				tempOption.value = jsonData[key].options[counter];
    				if (selectedArray.indexOf(jsonData[key].options[counter]) >= 0) {
    					tempOption.selected = true;
    				} else {
    					tempOption.selected = false;
    				}
    				newOptions.push(tempOption);
    			}
    			jsonData[key].options = newOptions;
    		}
    		ZEPHYR.ZQL.CustomFields.push(jsonData[key]);
    	});

		},
		failure : function(response) {
			buildErrorMessage();
		}
	});
}

// var getBulkCustomFieldByProject = function (commonCustomFields, projectId){
//   jQuery.ajax({
// 		url: getRestURL() + '/customfield/byEntityTypeAndProject?entityType=EXECUTION' + '&projectId=' + projectId,
// 		type : "get",
// 		contentType :"application/json",
// 		dataType: "json",
// 		success : function(response) {
// 			ZEPHYR.ZQL.CustomFields = commonCustomFields.concat(response);
// 		},
// 		failure : function(response) {
// 			buildErrorMessage();
// 		}
// 	});
// }

function datePickerBulkEdit(rowId, customField) {
	var date = null;
	var inputField;
	if(rowId) {
	  inputField = 'bulk-date-' + customField.customFieldId;
	  date = convertDate({'value': customField.value, 'isDateTime' : false});
	}
   Calendar.setup({
		firstDay : 0,
		inputField : inputField,
		align : 'Br',
		singleClick : true,
		showsTime : false,
		useISO8601WeekNumbers : false,
		timeFormat: '12',
		ifFormat : '%m/%d/%Y',
		date: date,
	});
}

//DATE TIME PICKER FUNCTION
function dateTimePickerBulkEdit(rowId, customField) {
	var date = null;
	var inputField;
	if(rowId) {
		inputField = 'bulk-dateTime-' + customField.customFieldId;
		date = convertDate({'value': customField.value, 'isDateTime' : true});
	}
	Calendar.setup({
		firstDay : 0,
		inputField : inputField,
		align : 'Br',
		singleClick : true,
		showsTime : true,
		timeFormat: '12',
		useISO8601WeekNumbers : false,
		ifFormat : '%m/%d/%Y %I:%M %p',
		date: date,
	});
}

function editBulkCustomFieldValues() {
	var customFieldValues = {};
	var values = {};
	AJS.$(".execution-details-customFields").each(function() {
		var obj = {};
		var that = AJS.$(this);
		var customFieldId = that[0].dataset.customfieldid;
		obj.customFieldId = customFieldId;
		obj.customFieldType = that[0].dataset.customfieldtype;

		var type = AJS.$(this)[0].dataset.customfieldtype;
		var value = '';

		if (type === 'MULTI_SELECT') {
			that.find('option:selected').each(function(){
				if(value) {
					value = value + ',' + AJS.$(this).val();
				}
				else {
					value = AJS.$(this).val();
				}
			  }
			)
		} else if (type === 'CHECKBOX') {
			that.find('input[type=checkbox]:checked').each(function(){
				if(value) {
					value = value + ',' + AJS.$(this)[0].dataset.value;
				}
				else {
					value = AJS.$(this)[0].dataset.value;
				}
			  }
			)
		} else if (type === 'NUMBER') {
			value = parseFloat(that.find('input').val());
		} else if (type === 'RADIO_BUTTON') {
			var selectedButton = that.find('input[type=radio]:checked');

			if(selectedButton.length) {
				value = selectedButton[0].dataset.value;
			}
		} else if (type === 'SINGLE_SELECT') {
			var selectedField = that.find('option:selected');

			if(selectedField.length) {
				value = that.find('option:selected').val();
			}
		} else if (type === 'LARGE_TEXT') {
			value = that.find('input, textarea').val();
		} else if (type === 'TEXT') {
			value = that.find('input').val();
		} else if (type === 'DATE' || type === 'DATE_TIME') {
			value = that.find('input').val();
			if(value.length) {
				var date = new Date(value);
				value = date.getTime();
				value = Math.round(value/1000);
			}
		}
		obj.value = value;
		values[customFieldId] = obj.value;
	});
	return values;
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
	ZEPHYR.ZQL.Project.data = {}
	ZEPHYR.ZQL.Cycle.data = {}

	ZEPHYR.ZQL.Project.data.searchResults = new ZEPHYR.ZQL.ProjectCollection()
	ZEPHYR.ZQL.Project.data.searchResults.fetch({
		success:function(response) {
      getBulkCustomField(response.models[0].id);
		},
		error: function(response,jqXHR) {
			showZQLError(jqXHR);
		}
	});
});
