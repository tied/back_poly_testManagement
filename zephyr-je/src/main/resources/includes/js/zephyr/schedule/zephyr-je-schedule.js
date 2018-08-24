if(!ZEPHYR.Schedule)
	ZEPHYR.Schedule = {}

var InlineDialog;
var executionColumnChooser;
var executionColumns = {};

var submitButtonId = 'submitColumnChooser';
var closeButtonId = 'closeColumnChooser';
var testExecutionFreezeColumn = false;
var statusMap = {};
var customColumns = [];
var executionSchedules = [];
var stepLevelDefects = [];
var stepLevelDefectsExecutionId;
var selectedExecutionId = '';
var updatedGridDataIssueExecution = {};
var onlyUpdateGridValueIssueExecution = false;
var pagePreferencesData = {};
var clearData = true;
var initialCountIssueExecution = 10;

var returnDefectsPopoverPosition = function (target) {
    var triggerElement = target.getBoundingClientRect();
    var viewportHeight = window.innerHeight;
    var viewportWidth = window.innerWidth;
    //var height = that.shadowRoot.querySelector('.defects-inlineDialogWrapper').clientHeight;
    var height = '200px';
    var topHeight = triggerElement.top + triggerElement.height + 2;

    var leftOffset, rightOffset, topOffset, bottomOffset;
    if (viewportHeight > topHeight + 200) {
        topOffset = topHeight;
        bottomOffset = 'auto';
    } else {
        topOffset = 'auto';
        bottomOffset = viewportHeight - topHeight + triggerElement.height + 5;
    }
    if (viewportWidth > triggerElement.left + 450) {
        leftOffset = triggerElement.left;
        rightOffset = 'auto';
    } else {
        rightOffset = viewportWidth - (triggerElement.left + triggerElement.width);
        leftOffset = 'auto';
        if (rightOffset < 0) {
            rightOffset = 0;
        }
    }
    return {
        right: rightOffset,
        left: leftOffset,
        bottom: bottomOffset,
        top: topOffset
    }
}

var returnDefectListMarkup = function(defectList) {
    return defectList.map(function(defect){
        return '<div class="defectsList">'
                + '<div class="statusColor" style="background-color : '+ defect.color +'"></div>'
                + '<div class="defectKey">'
                    + '<a href="'+contextPath +'/browse/' + defect.key +'">'+ defect.key +'</a>'
                + '</div>'
                + '<div class="defectStatus">'+ defect.status +'</div>'
                + '<div class="defectSummary">'+ defect.summary +'</div>'
            + '</div>'
        }).join('');

}

var defectsPopupOff = function(ev) {
    if(AJS.$('#testExecutionGridContainer').find('.defects-inlineDialogWrapper').length){
        AJS.$('#testExecutionGridContainer').find('.defects-inlineDialogWrapper').remove();
      }
}

AJS.$('#' + closeButtonId).live('click', function(event){
    InlineDialog.hide();
});

AJS.$('#inline-dialog-executionChooser-column-picker').live("click", function(e) {
   e.stopPropagation();
});

AJS.$('body').live("click", function(e){
    InlineDialog && InlineDialog.hide();
});

AJS.$('#' + submitButtonId).live('click', function() {
    var stepData ={};
		var count = 0;

		AJS.$('#inline-dialog-executionChooser-column-picker li :checkbox').each(function() {
      executionColumns[this.id].isVisible = this.checked;
			var index = AJS.$(AJS.$(".columnChooser-" + this.id)[0]).index();
			if(this.checked) {
        executionColumns[this.id].isVisible = "true";
				AJS.$('#ztestSchedulesTable thead tr th:eq('+ index +')').show();
				AJS.$('.columnChooser-' + this.id).show();
				count++;
			}	else {
				executionColumns[this.id].isVisible = "false";
				AJS.$('#ztestSchedulesTable thead tr th:eq('+ index +')').hide();
				AJS.$('.columnChooser-' + this.id).hide();
			}
    });
		stepData['preferences'] = executionColumns;

		if(count === 0) {
      AJS.$("#errorColumnSelector").show();
    } else {
      AJS.$("#errorColumnSelector").hide();
      stepData = JSON.stringify(stepData);
      jQuery.ajax({
        url: getRestURL() + "/preference/setTestExecutionCustomization",
        type : "post",
        contentType :"application/json",
        data : stepData,
        dataType: "json",
        success : function(response) {
					executionColumnChooser = ZEPHYR.Templates.Steps && ZEPHYR.Templates.Steps.columnCustomisation({columns: executionColumns, submitButtonId: submitButtonId, closeButtonId: closeButtonId});
          InlineDialog && InlineDialog.hide();;
    		}
      });
    }

    // jQuery.ajax({
    //   url: getRestURL() + "/preference/setexecutioncustomization",
    //   type : "post",
    //   contentType :"application/json",
    //   data : stepDataString,
    //   dataType: "json",
    //   success : function(response) {
    //     updateExecutionStepColumns();
    //     // window.executionDetailView.render();
    //     // window.teststepResultView.render();
    //     stepExecutionColumnInlineDialog.hide();
    //   }
    // });
});

function assign(fieldId,assigneeName) {
	jQuery.ajax({
		//url: getRestURL() + "/schedule/"+fieldId + "/assign?touser="+assigneeName,
		url: getRestURL() + "/execution/"+fieldId,
		type : "post",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			 window.location.reload(true);
			 AJS.$('#assign-schedule-cancel').click();
		},
		failure : function(response) {
			buildErrorMessage();
		}
	});
}

function getCustomFieldsIssueView(issueId) {
	jQuery.ajax({
		url: getRestURL() + "/customfield/entity?entityType=EXECUTION&issueId=" + issueId,
		type: "get",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			customColumns = response;
			customColumns.map(function(customField) {
				executionColumns[customField.id] = { 'displayName': customField.name, 'isVisible': 'false' }
			})
			getCustomFieldsProjectLevelIssueView(issueId);
		}
	});
}

function getCustomFieldsProjectLevelIssueView(issueId) {
	jQuery.ajax({
		url: getRestURL() + "/preference/paginationWidthPreference",
		type: "get",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			pagePreferencesData = response;

			if (response.testExecutionColumnFreezer == 'true') {
				testExecutionFreezeColumn = true;
			} else if (response.testExecutionColumnFreezer == 'false') {
				testExecutionFreezeColumn = false;
			}

			if (response.testDetailsColumnFreezer == 'true') {
				testDetailFreezeColumn = true;
			} else if (response.testDetailsColumnFreezer == 'false') {
				testDetailFreezeColumn = false;
			}

			jQuery.ajax({
				url: getRestURL() + "/customfield/byEntityTypeAndProject?entityType=EXECUTION&'&issueId=" + JIRA.Issue.getIssueId(),
				type: "get",
				contentType: "application/json",
				dataType: "json",
				success: function (response) {
					response.map(function (customField) {
						executionColumns[customField.id] = { 'displayName': customField.name, 'isVisible': 'false' }
						customColumns.push(customField);
					})
					getColumnPreference(issueId);
				}
			});
		}
	});

}

function getColumnPreference(issueId) {
	jQuery.ajax({
		url: getRestURL() + "/preference/getTestExecutionCustomization",
		type : "get",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			Object.keys(response.preferences).forEach(function(key) {
				if (!executionColumns[key]) {
					var obj = {
							"displayName": response.preferences[key].displayName,
							"isVisible": response.preferences[key].isVisible
					}
					executionColumns[key] = obj;
				} else {
					executionColumns[key].isVisible = response.preferences[key].isVisible;
				}
			});
			getSchedules(issueId);
		}
	});
}

function returnZqlQuery(nodeObj, key) {




    var queryParam  = 'issue = "' + nodeObj.projectKey;


    var zqlQuery    = 'query=' + encodeURIComponent(queryParam);
    //console.log('query', queryParam, zqlQuery)
    return contextPath + '/secure/enav/#?' + zqlQuery;
}

function getAssociatedDefects(scheduleDefects) {
	var defectsHtml = "";
	for(var pos in scheduleDefects){
		var defect = scheduleDefects[pos];

		if(defect.resolution){
			if (defect.maskedIssueKey=='XXXXX'){
				defectsHtml += "<span style='text-decoration:line-through;'data-issueKey='" + defect.maskedIssueKey +
					"' data-defect-resolution='" + defect.defectResolution + "' data-summary='" + AJS.escapeHtml(defect.summary) + "' data-status='" + defect.status + "'>" + defect.maskedIssueKey + "</span>";
			}else {
				defectsHtml += "<a style='text-decoration:line-through;' href='" + contextPath + "/browse/" + defect.key + "' data-issueKey='" + defect.key +
					"' data-defect-resolution='" + defect.defectResolution + "' data-summary='" + AJS.escapeHtml(defect.summary) + "' data-status='" + defect.status + "'>" + defect.key + "</a>";
			}
		}
		else{
			if (defect.maskedIssueKey == 'XXXXX'){
				defectsHtml += "<span  data-issueKey='" + defect.maskedIssueKey + "' data-summary='"+ AJS.escapeHtml(defect.summary) + "' data-status='" + defect.status + "'>" + defect.maskedIssueKey + "</span>";
			}else{
				defectsHtml += "<a href='"+ contextPath + "/browse/" + defect.key + "' data-issueKey='" + defect.key + "' data-summary='"+ AJS.escapeHtml(defect.summary) + "' data-status='" + defect.status + "'>" + defect.key + "</a>";
			}
		}
		if(pos < scheduleDefects.length -1)
			defectsHtml +=", "
	}
	return defectsHtml;
}

function getSchedules(issueId){
	jQuery.ajax({
		url: getRestURL() + "/execution",
			type : "get",
			contentType :"application/json",
			dataType: "json",
			data: {'issueId' : issueId, 'offset': 0, 'limit': 200 },
			success : function(response) {
				var EXEC_STATUS_TEXT_MAX_LENGTH=12;
				schedules = response["executions"];
				var hasSchedules = false;
				AJS.$("#zephyr-je-execution-body").empty();
				if(schedules.length)
					hasSchedules = true;
				executionSchedules = schedules;
				statusMap = response["status"];
				if(clearData) {
					AJS.$('#testExecutionGridContainer').html(ZEPHYR.Templates.Schedule.testExecutionGridTemplate());
					testExecutionGridComponent(schedules, response["status"], executionColumns, customColumns);

				} else {
					testExecutionGridComponent(schedules, response["status"], executionColumns, customColumns);
					clearData = true;
				}
				if(schedules.length) {
					AJS.$(".testExecutionGridWrapper .showinzql").remove();
					var viewinZql = '<div class="showinzql"><span>Showing '+ schedules.length + ' of '+ schedules.length +'</span></div>'
					AJS.$('.testExecutionGridWrapper').append(viewinZql);
					if(schedules.length > 200) {
						var zqlQuery  = 'query=' + encodeURIComponent('issue = "' + schedules[0].issueKey + '"'),
						  	href = contextPath + '/secure/enav/#?' + zqlQuery
						console.log('contextPath', contextPath + '/secure/enav/#?' + zqlQuery);
						AJS.$('.showinzql').html('<span>Showing 200 of '+ schedules.length +'</span><a target="_blank" href="'+ href +'">View all executions</a>');
					}

				}

				if(!hasSchedules){
					var td = "<span>"+ AJS.I18n.getText("issue.schedule.no.execution.label", ("<a id='zephyr-je-add-execute-link' class='viewissue-add-execute' href='" + contextPath + "/secure/AddExecute!AddExecute.jspa?id="+issueId+"'>"), "</a>") + "</span>";
					AJS.$('#noExecutions').html(td);
					JIRA.Dialogs.execDialog = new JIRA.FormDialog({
					    id: "zephyr-je-add-execute-link",
					    trigger: "a.viewissue-add-execute",
					    handleRedirect:true,
					    issueMsg : 'thanks_issue_worklogged',
					    onContentRefresh: function () {
					        jQuery(".overflow-ellipsis").textOverflow();
					    }
					});

				} else {
					AJS.$('#noExecutions').html('');
				}

			for(var schedulePos in schedules){
				// 	/*To get around IE8 bug where properties added to prototype also iterated */
					if (!schedules.hasOwnProperty(schedulePos))
						continue;
					hasSchedules = true;
					var schedule = schedules[schedulePos];
					var status = (schedule.executionStatus) ? response["status"][schedule.executionStatus] : response["status"]["-1"];

					var allStatuses = response["status"];
					//var cycleName 	= (schedule.cycleName == "Ad hoc") ? 'Adhoc' : schedule.cycleName;
					var zqlQuery =  encodeURIComponent('issue='+ schedule.issueKey);
                    var execurl = contextPath + '/secure/enav/#/'+schedule.id + '?query='+zqlQuery+"&offset="+(parseInt(schedulePos)+1);

					var defects = schedule.defects;

					var defectHTML;
					if(schedule.totalDefectCount > 0) {
						defects = getAssociatedDefects(defects);
						defectHTML = "<td class='zephyr-test-execution-entry-defect columnChooser-defects'>" +
								     "<div class='zfj-defect-hover' data-issueKey='" + schedule.issueKey + "' data-executionId='" + schedule.id + "' data-color='" + allStatuses[schedule.executionStatus].color + "'><span class='aui-lozenge aui-lozenge-defects'>" + schedule.executionDefectCount + " | " + schedule.stepDefectCount + "</span> " + defects + "</div></td>";
					} else {
						defects = AJS.I18n.getText("zephyr.je.defects.none");
						defectHTML = "<td class='zephyr-test-execution-entry-defect columnChooser-defects'>" + defects + "</td>";
					}
					var executedBy = schedule.executedBy;
					if(executedBy == null){
						executedBy = "-";
					}else{
						if(schedule.executedByDisplay)
							executedBy = "<a href='"+ contextPath +"/secure/ViewProfile.jspa?name="+ executedBy +"' rel='"+ executedBy +"' class='user-hover'>" + AJS.escapeHtml(schedule.executedByDisplay) + "</a>";
					}
					var cycleName = schedule.cycleName;
					var folderName = htmlEncode(schedule.folderName) || "";
					var executedOn = schedule.executedOn || "-";
					var cycleId = schedule.cycleId || -1;
					var deleteUrl = getRestURL() + "/execution/" +  schedule.id ;
					if(status.name.indexOf('<') > -1) {
						status.name = status.name.replace(/</gi, '&lt;');
					}
					if(status.name.indexOf('>') > -1) {
						status.name = status.name.replace(/>/gi, '&gt;');
					}
					var nrowPre = "<tr class='zephyr-test-execution-entry'>" +
								"<td class='zephyr-test-execution-entry-version columnChooser-version' align=center'>"+ AJS.escapeHtml(schedule.versionName)+ "</td>" +
								"<td class='zephyr-test-execution-entry-cycle columnChooser-testCycle'> <a href=\"" + contextPath + "/DisplayCycle.jspa?cycleId="+ cycleId + "&versionId=" + schedule.versionId + "&issueKey=" + schedule.issueKey + "\">" + AJS.escapeHtml(cycleName) + "</a></td>" +
								"<td class='zephyr-test-execution-entry-folder columnChooser-folder'>" + folderName + "</td>" +
								"<td class='columnChooser-status'> " +
									"<div class=\"field-group execution-status-container\">" +
										"<div id=\"execution-field-current-status-schedule-" + schedule.id +"\">" +
											"<div id=\"executionStatus-value-schedule-"+ schedule.id + "\" class=\"labels exec-status-container\">"+
												"<dl class=\"zfj-editable-field\">" +
						        					"<dt>" + AJS.I18n.getText('project.cycle.schedule.table.column.status') + "</dt>" +
						        					"<dd id=\"current-execution-status-dd-schedule-"+ schedule.id + "\" class=\"new-session-status\" style=\"background-color:"+ status.color + "\"  title=\"" + status.name  + (status.description ? (':' + status.description) : '')  + "\">"+ status.name + "</dd>" +
						        					"<a id=\"executionStatus-labels-schedule-" + schedule.id + "\" href=\"#\" class=\"zfj-overlay-icon icon icon-edit-sml\"><span rel='" + status.id + "'>" + status.name + "</span></a>" +
						        				"</dl>" +
						        			"</div>" +
						        		"</div>" +
						        		"<div id=\"execution-field-select-schedule-" + schedule.id + "\" style=\"position:relative;\">";

					        			var dropdownContent = "<select id=\"exec_status-schedule-"+ schedule.id + "\" class=\"select\" style='width:150px; position:relative; z-index:10;'>";
					        			//Following loop goes through all keys available in associative array where key = statusid.
										for(i in allStatuses){
											var iStatus = allStatuses[i];
											if(iStatus.name.indexOf('<') > -1) {
												iStatus.name = iStatus.name.replace(/</gi, '&lt;');
											}
											if(iStatus.name.indexOf('>') > -1) {
												iStatus.name = iStatus.name.replace(/>/gi, '&gt;');
											}
											if(status.id == iStatus.id){
												dropdownContent += "<option value=\""+ status.id + "\" selected=\"selected\" rel=\""+ status.color + "\" title=\""+ status.description + "\">" + status.name + "</option>";
											}
											else{
												dropdownContent += "<option value=\""+ iStatus.id + "\" rel=\""+ iStatus.color + "\" title=\""+ iStatus.description + "\">" + iStatus.name + "</option>";
											}
						        		}
    									dropdownContent +="</select>" +
    									'<div class="update_cancel-buttons" tabindex="1">' +
                        				'<button id="exec_status_update-schedule-' + schedule.id + '" accesskey="s" class="exec_status_update-schedule zfj-button submit" action="#" type="button">' +
                        					'<span class="icon icon-save"></span>' +
                                    	'</button>' +
                                    	'<button id="exec_status_cancel-schedule-' + schedule.id +'" accesskey="`" class="exec_status_cancel-schedule zfj-button cancel" action="#" type="cancel">' +
                                    		'<span class="icon icon-cancel"></span>'+
                                    	'</button></div>';

							var nrow = nrowPre + dropdownContent +
		        								"</div>" +
		        							"</div>" +
										"</td>" +
										defectHTML +
										"<td id=\"executed-by-schedule-"+schedule.id + "\" class='zephyr-test-execution-entry-execby columnChooser-executedBy'>" + executedBy + "</td>" +
										"<td id=\"executed-on-schedule-"+schedule.id + "\" class='zephyr-test-execution-entry-execon columnChooser-executedOn'>" + executedOn + "</td>" +
										"<td style='width:60px;'>" +
										"	<a class=\"aui-button\" onClick=\"window.location=\'"+execurl + "'\" title='"+ AJS.I18n.getText("issue.schedule.execute.title") +"'>"+AJS.I18n.getText("issue.schedule.execute.button.label")+"</a>" +
										"	<a id='schedule-"+schedule.id+"-operations-trigger' class='issue-schedule-operations' href='"+ deleteUrl +"' rel='"+schedule.id+"'><span class='icon icon-delete'></span></a>" +
										"</td>" +
										"</tr>";
							AJS.$(nrow).appendTo("#zephyr-je-execution-body");

							//Strip Text if greater than allowed
							//stripTextIfMoreThanAllowed("#z_execution-status-dd-"+schedule.id,status.name,EXEC_STATUS_TEXT_MAX_LENGTH, status.name + ": " + status.desc);
							// ZEPHYR.Schedule.stripTextIfMoreThanAllowed("#z_execution-status-dd-"+schedule.id, status.name, EXEC_STATUS_TEXT_MAX_LENGTH, schedule.comment);

							AJS.$("#execution-field-select-schedule-"+schedule.id).hide();
			}
				// Create editable view for status field.
				var editableFields = AJS.$('div.field-group.execution-status-container');
				AJS.$.each(editableFields, function(i, $container) {
					var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
						el: 			$container,
						elBeforeEdit:	AJS.$($container).find('[id^=execution-field-current-status-schedule-]'),
						elOnEdit:		AJS.$($container).find('[id^=execution-field-select-schedule-]')
					});
				});

				if(AJS.$("#inline-dialog-executionChooser-column-picker").length > 0) {
					AJS.$("#inline-dialog-executionChooser-column-picker").remove();
				}

				executionColumnChooser = ZEPHYR.Templates.Steps && ZEPHYR.Templates.Steps.columnCustomisation({columns: executionColumns, submitButtonId: submitButtonId, closeButtonId: closeButtonId});
				InlineDialog = AJS.InlineDialog(AJS.$("#executionColumnChooser-inlineDialog"), "executionChooser-column-picker",
					function(content, trigger, showPopup) {
							content.css({"padding":"10px 0 0", "max-height":"none"}).html(executionColumnChooser);
							showPopup();
							return false;
					},
					{
						width: 250,
						closeOnTriggerClick: true,
						persistent: true
					}
				);

				ZEPHYR.Schedule.addDeleteTrigger();
				if(!hasSchedules){
					var td = "<tr><td colspan='6'>"+ AJS.I18n.getText("issue.schedule.no.execution.label", ("<a id='zephyr-je-add-execute-link' class='viewissue-add-execute' href='" + contextPath + "/secure/AddExecute!AddExecute.jspa?id="+issueId+"'>"), "</a>") + "</td></tr>";
					AJS.$(td).appendTo("#zephyr-je-execution-body");
					JIRA.Dialogs.execDialog = new JIRA.FormDialog({
					    id: "zephyr-je-add-execute-link",
					    trigger: "a.viewissue-add-execute",
					    handleRedirect:true,
					    issueMsg : 'thanks_issue_worklogged',
					    onContentRefresh: function () {
					        jQuery(".overflow-ellipsis").textOverflow();
					    }
					});
				}

				Object.keys(executionColumns).forEach(function(key) {
					var index = AJS.$(AJS.$(".columnChooser-" + key)[0]).index();
					if(executionColumns[key].isVisible === 'true') {
						AJS.$('#ztestSchedulesTable thead tr th:eq('+ index +')').show();
						AJS.$('.columnChooser-' + key).show();
					}	else {
						AJS.$('#ztestSchedulesTable thead tr th:eq('+ index +')').hide();
						AJS.$('.columnChooser-' + key).hide();
					}
				});
			}
	});
}

ZEPHYR.Schedule.getSchedules = function(issueId) {
	if(!issueId)
		return;
	getCustomFieldsIssueView(issueId);
}

function createSchedule(isAdhoc,versionId,cycleId,projectId,issueId, folderId) {

    if(folderId == -1 || cycleId == -1) {
        folderId = null;
    }

	//Common Method. If isAdhoc is 0, version and cycleId has to be null
	if(isAdhoc == 0) {
		versionId="-1";
		cycleId=null;
	}

    var scheduleParams = {
	    	'issueId': issueId,
	    	'versionId': versionId,
	    	'cycleId': cycleId,
	    	'projectId' : projectId
	    },
	    //assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('zephyr-je-execution-assignee-execute');
	    assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('exec-assignee-wrapper');
	    if(folderId) {
	    	scheduleParams.folderId = folderId;
	    }
    if(!assigneeParams)
    	return;
	if(assigneeParams.assigneeType && assigneeParams.assigneeType !== 'null' && assigneeParams.assigneeType !== 'undefined'){
    	_.extend(scheduleParams, assigneeParams); // Merge assigneeParams to schedule params
	}
	AJS.$('#zephyr-je-add-execute').addClass('zephyr-je-add-execution');
	jQuery.ajax({
		url: getRestURL() + "/execution",
		type : "post",
		contentType :"application/json",
		dataType: "json",
		data: JSON.stringify(scheduleParams),
		success : function(response) {
			 for(var key in response) {
				 window.location.href = contextPath + '/secure/enav/#/'+response[key].id;
			 }
		},
		error : function(response) {
			if(response.status == 403) {
				AJS.$('.zephyr-je-add-tests-execution-button').attr('aria-disabled', 'true');
				document.getElementsByClassName('zephyr-je-add-tests-execution-button')[0].onclick = null; // Disable the onclick
			} else {
				AJS.$('.aui-message-error').remove();
			}
			AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
			    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
			    body: getErrorMessageFromResponse(response) //"Error Adding Issue to Cycle."
			});
		}
	});
}

ZEPHYR.Schedule.fetchFoldersByCycle = function(cycleId,projectId,versionId, cb) {
	AJS.$.ajax({
		url: getRestURL() + "/cycle/" + cycleId + "/folders?projectId="+ projectId +"&versionId=" + versionId + "&offset=0&limit=1000",
		type : "GET",
		contentType :"application/json",
		success : function(response) {
			if(cb && typeof cb === 'function') {
				cb(response);
			}
		},
		error : function(response) {
			if(response.status == 403) {
				AJS.$('.zephyr-je-add-tests-execution-button').attr('aria-disabled', 'true');
				document.getElementsByClassName('zephyr-je-add-tests-execution-button')[0].onclick = null;
			} else {
				AJS.$('.aui-message-error').remove();
			}
			AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
			    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
			    body: getErrorMessageFromResponse(response) //"Error Adding Issue to Cycle."
			});
		}
	});
}

ZEPHYR.Schedule.refreshTestExecutions = function(issueId) {
    window.onbeforeunload=null;
    ZEPHYR.Schedule.getSchedules(issueId);
}

ZEPHYR.Schedule.addIssueToCycle = function(isSelected,versionId,cycleId,projectId,issueId, folderId) {
	if(cycleId == -1) {
		folderId = null;
	}
	if(folderId == -1) {
        folderId = null;
	}
	var scheduleParams = {
			'issueId': issueId,
			'versionId': versionId,
			'cycleId': cycleId,
			'projectId' : projectId
		},
	    //assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('zephyr-je-execution-assignee-add-tests');
	    assigneeParams = ZEPHYR.Execution.Assignee.getSelectedParams('exec-assignee-wrapper');

    if(folderId) {
        scheduleParams.folderId = folderId;
    }
	if(!assigneeParams)
		return;
	if(assigneeParams.assigneeType && assigneeParams.assigneeType !== 'null' && assigneeParams.assigneeType !== 'undefined'){
    _.extend(scheduleParams, assigneeParams); // Merge assigneeParams to schedule params
  }
	if (scheduleParams.assignee == 'undefined') {
		delete scheduleParams.assignee;
	}
	if (scheduleParams.assigneeType == 'undefined') {
		delete scheduleParams.assigneeType;
	}
    AJS.$('#viewissue-add-cycle').addClass('zephyr-je-add-execution');
	jQuery.ajax({
		url: getRestURL() + "/execution",
		type : "POST",
		accept: "POST",
		contentType :"application/json",
		dataType: "json",
		data: JSON.stringify(scheduleParams),
		success : function(response) {
            var isReturnSchedule;
            var message,title;
            for(var key in response) {
                isReturnSchedule = response[key].isReturnSchedule;
            }

            if(isReturnSchedule == true || isReturnSchedule === 'true') {
                message = 'This test is already available in this cycle/folder.';
            }

			if(isSelected) {

                if(isReturnSchedule == true || isReturnSchedule === 'true') {
										AJS.messages.error(AJS.$(AJS.$('#custom-context')[0]), {
                        title: "Error!",
                        body: message
                    });
										setTimeout(function(){
                        AJS.$(".aui-message").fadeOut(1000, function(){
                            AJS.$(".aui-message").remove();
                        });
                    }, 1000);
                }else {
										AJS.messages.success(AJS.$(AJS.$('#custom-context')[0]), {
								        title: "Success!",
								        body: "Successfully Created or Added Test to cycle/folder."
								    });
                    setTimeout(function(){
                        AJS.$(".aui-message").fadeOut(1000, function(){
                            AJS.$(".aui-message").remove();
                        });
                    }, 1000);
                }

			} else {
                if(isReturnSchedule == true || isReturnSchedule === 'true') {
										AJS.messages.error(AJS.$(AJS.$('#custom-context')[0]), {
                        title: "Error!",
                        body: message
                    });
                    setTimeout(function(){
                        AJS.$(".aui-message").fadeOut(1000, function(){
                            AJS.$(".aui-message").remove();
                            window.onbeforeunload=null;
                            AJS.$('#zephyr-je-dlgclose').click();
                            ZEPHYR.Schedule.getSchedules(issueId);
                        });
                    }, 2000);
                }else {
                    message = 'Successfully Created or Added Test to cycle/folder.';
                    displayAddTestToCycleResponse(message,issueId);
                }
			}
		},
		error : function(response) {
			if(response.status == 403) {
				AJS.$('.zephyr-je-add-tests-execution-button').attr('aria-disabled', 'true');
				document.getElementsByClassName('zephyr-je-add-tests-execution-button')[0].onclick = null;
			} else {
				AJS.$('.aui-message-error').remove();
			}
			AJS.messages.error(AJS.$(AJS.$('.zephyr-aui-message-bar')[0]), {
			    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
			    body: getErrorMessageFromResponse(response) //"Error Adding Issue to Cycle."
			});
		}
	});
}

function displayAddTestToCycleResponse(message,issueId) {
		AJS.messages.success(AJS.$(AJS.$('#custom-context')[0]), {
        title: "Success!",
        body: message
    });
    setTimeout(function(){
        AJS.$(".aui-message").fadeOut(1000, function(){
            AJS.$(".aui-message").remove();
            window.onbeforeunload=null;
            AJS.$('#zephyr-je-dlgclose').click();
            ZEPHYR.Schedule.getSchedules(issueId);
        });
    }, 2000);
}
function isEncHTML(str) {
	if(str.search(/&amp;/g) != -1 || str.search(/&lt;/g) != -1 || str.search(/&gt;/g) != -1)
		return true;
	else
		return false;
}

function decHTMLifEnc(str){
	if(isEncHTML(str))
		str = str.replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g,'"');
	return str;
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
	if (JIRA.Events.PANEL_REFRESHED) {
		JIRA.unbind(JIRA.Events.PANEL_REFRESHED, ZEPHYR.Schedule.refreshFunction)
		JIRA.bind(JIRA.Events.PANEL_REFRESHED, ZEPHYR.Schedule.refreshFunction)
	}

	if(JIRA.Events.ISSUE_REFRESHED){
		JIRA.bind(JIRA.Events.ISSUE_REFRESHED, function(event, issueId){
			ZEPHYR.Schedule.reInitViewIssueSchedules(event)
		})
	}
	ZEPHYR.Schedule.reInitViewIssueSchedules();
	/*
	$(".assignee-edit-group").each(function(){
        var $this = $(this);
        var assigneeFieldId = $this.attr("rel");

        $("#assignee_userpicker_dummy_" + assigneeFieldId +"_container").click(function (){
            $("#assignee_radio_picker_" + assigneeFieldId).attr("checked", "true");
        });

        $this.parents("form[name=jiraform]").submit(function(){
            $this.find("input[name=assignee_radio]:checked").each(function(){
                if($(this).attr("id") == "assignee_radio_picker_" + assigneeFieldId){
                    $("#" + assigneeFieldId).val($("#assignee_userpicker_dummy_" + assigneeFieldId).val());
                } else {
                    $("#" + assigneeFieldId).val($(this).val());
                }
            });
        });
    });*/
});

ZEPHYR.Schedule.initScheduleListeners = function(){
//	AJS.$("#ztestSchedulesTable").undelegate('[id^="executionStatus-value-"]', 'click', enableExecutionStatusDropDown);
//	AJS.$("#ztestSchedulesTable").delegate('[id^="executionStatus-value-"]', 'click', enableExecutionStatusDropDown);
		AJS.$(document).unbind('gridActions');
		AJS.$(document).bind('gridActions', gridComponentActions);
		AJS.$(document).unbind('gridValueUpdated');
		AJS.$(document).bind('gridValueUpdated', gridValueUpdated);
		AJS.$(document).unbind('gridRowSelected');
		AJS.$(document).unbind('gridBulkActions');
		AJS.$(document).bind('gridBulkActions', gridBulkActions);
		AJS.$(document).unbind('defecthover');
		AJS.$(document).bind('defecthover', issueViewDefectsPopup);
		AJS.$(document).unbind('defecthoverOff');
    	AJS.$(document).bind('defecthoverOff', defectsPopupOff);
		AJS.$(document).unbind('freezetoggle');
		AJS.$(document).bind('freezetoggle', freezeToggle);
}

ZEPHYR.Schedule.reInitViewIssueSchedules = function(e, panel, $new, $existing) {
	var errors = AJS.$("#zerrors").val();
	if(errors != null && errors.length > 0) {
		return;
	}

	/*Get schedules from DB, Moved from VM to here, so that we only do it once on page load. */
    try {
        ZEPHYR.Schedule.getSchedules(JIRA.Issue.getIssueId());
    }catch(e){}

	//This will add click event handler on table level and will in turn pass it to right status dropdown list!
	//That way we don't have to add listeners for each status dropdown!
	ZEPHYR.Schedule.initScheduleListeners();
}

ZEPHYR.Schedule.refreshFunction = function(e, panel, $new, $existing){
	if(panel === "view_issue_execution_section"){
		/*For both error block as well as schedules, we can use existing block*/
		$new.replaceWith($existing)
		//Now add the listeners again.
		if(!AJS.$('input#zerrors').val()){
			ZEPHYR.Schedule.initScheduleListeners();
		}
	}
}

var dummyConfig = {};
var testExecutionGridComponent = function(schedules, executionStatus, allColumns, customFields) {
	var config = {
		"head" :[
			{
				key: 'versionName',
				displayName : 'Version',
				isFreeze : testExecutionFreezeColumn,
				editable : false,
				isInlineEdit : false,
				type: 'String',
				isSortable : false,
				isVisible : true,
			}, {
		        key: 'status',
		        displayName: 'Status',
		        isFreeze: testExecutionFreezeColumn,
		        editable: true,
		        isInlineEdit: true,
		        type: 'SELECT_STATUS',
				imgUrl : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
		        isSortable: false,
		        executionSummaries: executionStatus,
		        isVisible : true,
		    }, {
				key: 'cycleName',
				displayName: 'Test Cycle',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				type: 'CYCLE_NAME',
				isSortable: false,
				isVisible : true,
			}, {
				key: 'folderName',
				displayName: 'Folder',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'String',
				isVisible : true,
			}, {
				key: 'defects',
				displayName: 'Defects',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'String',
				isVisible : true,
			}, {
				key: 'executedBy',
				displayName: 'Executed By',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'String',
				isVisible : true,
			}, {
				key: 'executedOn',
				displayName: 'Executed On',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'String',
				isVisible : true,
			}
		],
		"row" :[],
		"actions" : [{
						actionName : 'Execute',
						customEvent : 'executeRow',
						imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/execute_icon.svg',
					}, {
						actionName : 'Delete',
						customEvent : 'deleteRow',
						imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/delete_button.svg',
					}
		],
		"maxFreezed" : 2,
		"bulkActions" : [{
					actionName : testExecutionFreezeColumn ? 'Unfreeze version and status' : 'Freeze version and status',
					customEvent : 'freezeColumns',
					disabled : false,
					imgSrc : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pin-issue-page_button.svg',
				}
		],
		"columnchooser": {
            isEnabled: true,
            actionName: 'Columns',
            customEvent: 'columnchooser',
            imgSrc:  contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
        },
        "contextPath": contextPath,
		"hasBulkActions" : true,
		"freezeToggle" : false,
		"highlightSelectedRows": true,
		"draggableRows" : false,
		"columnChooserUrl" : contextPath + '/download/resources/com.thed.zephyr.je/images/icons/column-chooser_button.svg',
		"rowSelection" : false,
		"gridComponentPage" : 'issueView',
		"initialCount": getGridInitialCount(initialCountIssueExecution),
		"columnChooserHeading": AJS.I18n.getText('cycle.ColumnChooser.label'),
		"selectAtleaseoneOption": AJS.I18n.getText('zephyr.gridData.selectAtleastOneOption'),
		"submit": AJS.I18n.getText('zephyr.je.submit.form.Submit'),
		"cancel": AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		"noPermission": AJS.I18n.getText('zephyr.cycle.noPermissionOnTestAndIssue'),
		"placeholderText": AJS.I18n.getText('zephyr.customfield.textarea.placeholder'),
		"action": AJS.I18n.getText('cycle.actions.label'),
		"loading": AJS.I18n.getText('zephyr.gridData.loading'),
		"isExecutionGrid": true,
		"dataset": [{
            name: 'executionid',
            key: 'id'
        }],
        "noPermissionTestIssue": AJS.I18n.getText('cycle.noPermissionTestIssue.label')
	}

	customFields.map(function(columnCell) {
		var obj = {
			"key": columnCell.id,
			"displayName": columnCell.name,
			"isFreeze": false,
			"editable": false,
			"isInlineEdit": false,
			"type": (columnCell.fieldType == 'TEXT' || columnCell.fieldType == 'LARGE_TEXT') ? 'HTMLContent' : 'String',
			"isSortable": false,
			"isVisible": false,
		}
		config.head.push(obj);
	})

  var columnChooserValues = [];
	config.head.map(function(column) {
		 if((column.key !== "issueKey" && column.key !== "orderId")){
			 var key = column.key;
			 if(key === 'versionName') {
				 key = 'version';
			 } else if(key === 'cycleName') {
				 key = 'testCycle';
			 } else if(key === 'folderName') {
				 key = 'folder';
			 }
			 if(allColumns[key] && allColumns[key].isVisible === 'true') {
					column.isVisible = true;
				} else {
					column.isVisible = false;
				}
       columnChooserValues[key] = {
        displayName: column.displayName,
        isVisible: column['isVisible'] ? 'true' : 'false'
      };
		 } else{
			 column.isVisible = true;
		 }
	});

  config.columnchooser.columnChooserValues = columnChooserValues;
	if((_.findWhere(allColumns, {displayName: 'Version'}).isVisible === 'false') && (_.findWhere(allColumns, {displayName: 'Status'}).isVisible === 'false') && config['bulkActions'] && config['bulkActions'][0]) {
		config['bulkActions'][0]['disabled'] = true;
	}

	schedules.map(function(row, index) {
        var obj = row;
        // if (ZEPHYR.Cycle.clickedExecutionId) {
        //     selectedExecutionId = ZEPHYR.Cycle.clickedExecutionId;
        // } else if(index === 0) {
        //     selectedExecutionId = obj.id;
        // }
        var customFields = {};
        if(typeof obj.customFields === 'string')
            customFields = JSON.parse(obj.customFields);
        else
            customFields = obj.customFields;

        schedules.map(function (r, index) {
			if (r.id === row.id) {
				position = index;
			}
		});

        var url = getExecutionUrl({ execution: row, index: index, url: contextPath});
            url +='&view=detail';
        obj.executionUrl = url;
        Object.keys(customFields).map(function(field) {
            var _customFieldValue = customFields[field].value;
            if ((customFields[field].customFieldType == 'DATE' || customFields[field].customFieldType == 'DATE_TIME') && _customFieldValue) {
                try {
				    _customFieldValue = convertDateExecution({value: _customFieldValue, isDateTime: customFields[field].customFieldType == 'DATE_TIME'});
				}
				catch(err) {
				    //
				}
            }
            if(customFields[field].customFieldType == 'NUMBER') {
                _customFieldValue = _customFieldValue;
            }

            if (customFields[field].customFieldType === 'TEXT' || customFields[field].customFieldType === 'LARGE_TEXT') {
              _customFieldValue = customFields[field].htmlValue;
            }
            obj[customFields[field].customFieldId] = _customFieldValue;
        });

        if(obj.defects) {
            obj.defects.map(function(defect){
                defect['color'] = statusMap[obj.executionStatus] && statusMap[obj.executionStatus].color;
            })
        }

        if((obj.id === stepLevelDefectsExecutionId)) {
            if(showDefectsPopup) {
                obj['stepDefect'] = stepLevelDefects;
                obj['showPopup'] = true;
                updatedGridDataCycleSummary.rowData = {
                    index: index,
                    showPopup: true,
                    stepDefect : stepLevelDefects
                }
            } else {
                obj['showPopup'] = false;
                updatedGridDataCycleSummary.rowData = {
                    index: index,
                    showPopup: false,
                }
                stepLevelDefectsExecutionId = '';
            }
        }
        obj['permission'] = !row.canViewIssue;
        config.row.push(obj);
    });

	config['freezeColumns'] = testExecutionFreezeColumn;
	config.columnchooser.disabled = config.row.length === 0 ? true : false;
	if(onlyUpdateGridValueIssueExecution) {
		AJS.$('#testExecutionGrid').attr('updatedconfig', JSON.stringify(updatedGridDataIssueExecution));
		updatedGridDataIssueExecution = {};
		onlyUpdateGridValueIssueExecution = false;
	} else {
		if (initialCountIssueExecution != 10) {
			initialCountIssueExecution = 10;
		}
		initialCountIssueExecution = getGridInitialCount(initialCountIssueExecution);
		try {
		    vanillaGrid.init(document.getElementById('testExecutionGrid'), config);
		}
		catch(err) {
		    //Added try catch for ZFJ-4135 as je-scheule.js is loaded in general context
		}
		//AJS.$('#testExecutionGrid').attr('config', JSON.stringify(config));
	}
	AJS.$("#testExecutionGrid").unbind('gridActions');
	AJS.$("#testExecutionGrid").bind('gridActions', gridComponentActions);
	AJS.$("#testExecutionGrid").unbind('gridValueUpdated');
	AJS.$("#testExecutionGrid").bind('gridValueUpdated', gridValueUpdated);
	AJS.$("#testExecutionGrid").unbind('gridRowSelected');
	AJS.$("#testExecutionGrid").unbind('executiongridBulkActions');
	AJS.$("#testExecutionGrid").bind('executiongridBulkActions', gridBulkActions);
	AJS.$("#testExecutionGrid").unbind('defecthover');
	AJS.$("#testExecutionGrid").bind('defecthover', issueViewDefectsPopup);
	AJS.$("#testExecutionGrid").unbind('defecthoverOff');
    AJS.$("#testExecutionGrid").bind('defecthoverOff', defectsPopupOff);
	AJS.$("#testExecutionGrid").unbind('freezetoggle');
	AJS.$("#testExecutionGrid").bind('freezetoggle', freezeToggle);
	// AJS.$('#testExecutionGrid').unbind('gridScrollEventCapture');
	// AJS.$('#testExecutionGrid').bind('gridScrollEventCapture', gridScrollEventCapture);
	AJS.$('#testExecutionGrid').unbind('dialogueScroll');
	AJS.$('#testExecutionGrid').bind('dialogueScroll', dialogueScroll);
}

var freezeToggle = function (ev) {
	ev.preventDefault();
	ev.stopPropagation();

	dummyConfig = ev.originalEvent.detail.testConfig;
}

var gridComponentActions = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();

  var actionDetail = ev.originalEvent.detail;
  if(actionDetail.actionName === 'Delete') {
	  executionSchedules.map(function (row, index) {
		  if (row.id === actionDetail.rowDetail.id) {
			  updatedGridDataIssueExecution.rowData = {
				  index: index,
				  deleteRow: true,
			  }
		  }
	  });
    var url = contextPath + '/rest/zephyr/latest/execution/' + actionDetail.rowDetail.id;
    ZEPHYR.Schedule.addDeleteTrigger(function(){
		initialCountIssueExecution = executionSchedules.length;
	//   onlyUpdateGridValueCycleSummary = true;
	//   clearData = false;
      ZEPHYR.Schedule.refreshTestExecutions(JIRA.Issue.getIssueId());
    }, actionDetail.rowDetail.id, url, false, true);
    AJS.$('a.trigger-delete-dialog').trigger('click');
  } else if (actionDetail.customEvent === 'executeRow') {
	  var url = actionDetail.target.dataset.href;
	  jQuery.ajax({
		  url: getRestURL() + "/preference/paginationWidthPreference",
		  type: "get",
		  contentType: "application/json",
		  dataType: "json",
		  success: function (response) {
			  var sendingData = response;
			  sendingData.fromPage = "planCycleSummaryactions"
			  jQuery.ajax({
				  url: getRestURL() + "/preference/paginationWidthPreference",
				  type: "PUT",
				  contentType: "application/json",
				  dataType: "json",
				  data: JSON.stringify(sendingData),
				  success: function (finalResponse) {
					  window.location.assign(url);
				  }
			  });
		  }
	  });
  } else if(actionDetail.actionName ==='columnChooser') {

		var data = {}

    actionDetail.columnDetails.map(function(column) {
      if(column.key !== "issueKey" && column.key !== "orderId") {
				var key = column.key;
	 			 if(key === 'versionName') {
	 				 key = 'version';
	 			 } else if(key === 'cycleName') {
	 				 key = 'testCycle';
	 			 } else if(key === 'folderName') {
	 				 key = 'folder';
	 			 }
        if(column.isVisible) {
          executionColumns[key].isVisible = "true";
        } else {
    			executionColumns[key].isVisible = "false";
        }
      }
    });

		data['preferences'] = executionColumns;

		jQuery.ajax({
			url: getRestURL() + "/preference/setTestExecutionCustomization",
			type : "post",
			contentType :"application/json",
			data : JSON.stringify(data),
			dataType: "json",
			success : function(response) {
				testExecutionGridComponent(executionSchedules, statusMap, executionColumns, customColumns);
				 // stepInlineDialog && stepInlineDialog.hide();
			}
		});
  }
}

var gridValueUpdated = function(ev) {
  dummyConfig = ev.originalEvent.detail.testConfig;

  ev.preventDefault();
  ev.stopPropagation();
  var data = ev.originalEvent.detail.updatedValue;
  var rowId = ev.originalEvent.detail.rowId,
      executionId = ev.originalEvent.detail.executionId;
  if (ev.originalEvent.detail.isObject) {
	  	Object.keys(data).map(function(key){
			data[key] = data[key].value;
		});
  }
  data['changeAssignee'] = false;
  var xhr = {
      url: '/execution/'+ executionId  + '/execute',
      method: 'PUT',
      data:data
  }


	jQuery.ajax({
			url: getRestURL() + xhr.url,
			type : xhr.method,
			accept: "application/json",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify(xhr.data),
			success : function(response) {
					executionSchedules.map(function(row, index){
						if(row.id === response.id) {
							updatedGridDataIssueExecution.rowData = {
								executionStatus: response.executionStatus,
								executedBy: response.executedBy,
								executedOn: response.executedOn,
								index: index,
							}
						}
					});
					//onlyUpdateGridValueIssueExecution = true;
					clearData = false;
					ZEPHYR.Schedule.refreshTestExecutions(JIRA.Issue.getIssueId());
					// console.log('saved successfully');
			},
			error : function(xhr, status, error) {
					console.log('error', xhr, error);
			},
			statusCode: {
			    403: function(xhr, status, error) {
			    	executionSchedules.map(function (row, index) {
			    			if (row.id === rowId) {
								updatedGridDataIssueExecution.rowData = {
			    					executionStatus:row.executionStatus,
			    					index: index,
			    				}
			    			}
			    		});
			    		onlyUpdateGridValueIssueExecution = true;
			       testExecutionGridComponent(executionSchedules, statusMap, executionColumns, customColumns);
			    }

			}
	});
}

var getExecutionUrl = function(params) {
    var execution = params.execution;
    // var cycleName = (execution.cycleId == -1) ? "Ad hoc" : execution.cycleName
  	// var queryParam = 'project = "' + addSlashes(execution.projectKey) + '" AND fixVersion = "' +  addSlashes(execution.versionName) + '" AND cycleName in ("' + addSlashes(cycleName) + '") ORDER BY Execution ASC';
		var queryParam = 'issue = ' + addSlashes(execution.issueKey);
		var zqlQuery = 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(params.index) + 1);
    queryParam = "issue="+ execution.issueKey;
    var zqlQuery = 'query=' + encodeURIComponent(queryParam) + '&offset=' + (parseInt(params.index) + 1);
    return params.url + '/secure/enav/#/' + execution.id + '?' + zqlQuery;
}

var gridBulkActions = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  if(ev.originalEvent.detail.actionName === 'freezeColumns') {
	  var sendingData = pagePreferencesData;
	  sendingData.testExecutionColumnFreezer = !testExecutionFreezeColumn;
	  jQuery.ajax({
		  url: getRestURL() + "/preference/paginationWidthPreference",
		  type: "PUT",
		  contentType: "application/json",
		  dataType: "json",
		  data: JSON.stringify(sendingData),
		  success: function (response) {

			testExecutionFreezeColumn = !testExecutionFreezeColumn;
			testExecutionGridComponent(executionSchedules, statusMap, executionColumns, customColumns);
		  }
	  });
  }
}

var issueViewDefectsPopup = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  if(ev.originalEvent.detail.defectsPopup) {
    showDefectsPopup = true;
    stepLevelDefects = [];
    jQuery.ajax({
        url: getRestURL() + "/stepResult/stepDefects?executionId=" + ev.originalEvent.detail.id + "&expand=executionStatus&",
        type : "get",
        contentType :"application/json",
        dataType: "json",
        success : function(response) {
          Object.keys(response.stepDefects).map(function(index){
            var color;
            var executionStatus = response.stepDefects[index].currentStepExecutionStatus;
            color = response.executionStatus[executionStatus].color;
            response.stepDefects[index].stepDefects.map(function(defect) {
              defect['color'] = color;
              stepLevelDefects.push(defect);
            });
		  });
		  var executionList = schedules;
          var currentExecution = executionList.filter(function(execution) {
            return execution.id === parseInt(ev.originalEvent.detail.id);
          })[0];

          var dimensions = returnDefectsPopoverPosition(ev.originalEvent.detail.targetEle);
          Object.keys(dimensions).forEach(function(key){
            dimensions[key] = dimensions[key] !== 'auto' ? dimensions[key] + 'px' : dimensions[key];
          });
          var defectsPopupOver = '<div id="defects-inlineDialogWrapper" class="defects-inlineDialogWrapper" style="right: '+ dimensions.right +'; left:' + dimensions.left +'; top: '+ dimensions.top +'; bottom: '+ dimensions.bottom +'">'
                                    + '<div class="defects-container">'
                                        + '<div class="executionLevelDefects">'
                                            + '<span>Defects Filed For </span>'
                                            + '<div class="defectsList-Container">'
                                                + returnDefectListMarkup(currentExecution.defects)
                                            + '</div>'
                                        + '</div>'
                                        + (stepLevelDefects.length ? '<div class="stepLevelDefects">'
                                            + '<span>Step Level Defects Filed</span>'
                                            + '<div class="defectsList-Container">'
                                                + returnDefectListMarkup(stepLevelDefects)
                                            + '</div>'
                                        + '</div>'
                                        : '')
                                    + '</div>'
                                + '</div>';

          AJS.$('#testExecutionGridContainer').append(defectsPopupOver);
          AJS.$("#defects-inlineDialogWrapper").mouseenter(function() {
                this.dataset['isDefectPopoverHovered'] = 'true';
            });
          AJS.$( "#defects-inlineDialogWrapper" ).mouseleave(function() {
          	setTimeout(function() {
				ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
					return dialogue.dialogueElement != AJS.$('#defects-inlineDialogWrapper')[0];
				});
            	defectsPopupOff();
          	}, 100);
		  });
		  ZEPHYR.GRID.scrollableDialogue.push({ isOpen: true, target: ev.originalEvent.detail.targetEle, dialogueElement: AJS.$('#defects-inlineDialogWrapper')[0]});
		  onlyUpdateGridValueIssueExecution = true;
          stepLevelDefectsExecutionId = ev.originalEvent.detail.id;
          testExecutionGridComponent(executionSchedules, statusMap, executionColumns, customColumns);
        }
    });
  } else {
    showDefectsPopup = false;
	stepLevelDefects = [];
	onlyUpdateGridValueIssueExecution = true;
    stepLevelDefectsExecutionId = ev.originalEvent.detail.id;
    testExecutionGridComponent(executionSchedules, statusMap, executionColumns, customColumns);
  }
}

function dialogueScroll(event) {
	if (event.originalEvent.detail.isOpen) {
		ZEPHYR.GRID.scrollableDialogue.push(event.originalEvent.detail);
	} else {
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
			return dialogue.dialogueElement != event.originalEvent.detail.dialogueElement;
		});
	}
}
