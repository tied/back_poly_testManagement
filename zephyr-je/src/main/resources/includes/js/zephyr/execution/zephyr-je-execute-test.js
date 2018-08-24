/**
 * @namespace ZEPHYR.Schedule.Execute
 */
var focusables = ':input, a[href], [tabindex]';
var executionAttachmentInlineDialog;
var stepAttachmentInlineDialog;
var attachmentDuplicate;
var stepLevelDefectId;
var issueType;
var defectResults = {};
var showStepDefectsPopup = false;
var gridRefresh = false;
var permissionGridRefresh = false;
var defectRowId;
var deleteAttachmentStepId;
var selectedStatusId;
var gridIssueStepId;
var updatedGridData = {};
var onlyUpdateGridValue = false;
var initialCount = 10;
var addDefectAjaxDebounce = ''
var paginationDisplayFlag = true;
ZEPHYR.Execution.testDetailsGrid = {};
ZEPHYR.GRID = {
	scrollableDialogue: [],
	stopGridFocus: false,
	stepStatuses: []
};

function hasFocus($container) {
    var activeElement = document.activeElement;
    /*IE reports a focus gain on summary field after create New issue button is pressed, to avoid that we are ignoring summary field*/
    var result = $container.find(activeElement).length > 0 || $container.filter(activeElement).length > 0 || (activeElement.id == 'summary');
    /*
     * Quick fix for the ZFJ-1053 issue: the defect is never stays linked to the issue.
     * Since the customfield id always starts with customfield in the 'Create Issue' dialog,
     * if the activeElement's id contains 'customfield' returning true.
     */
    if(!result) {
    	if(activeElement.id.indexOf('customfield') != -1)
    		result = true;
    }
    //console.log(activeElement.outerHTML + " " + result)
    return result;
}

//http://stackoverflow.com/questions/2805678/limit-number-of-characters-entered-in-textarea
ZEPHYR.charCounter = function (event, el, fldLength, counterSelector){
	var content = AJS.$(el).val();
	var len = fldLength - content.length
	if(len < 0){
		len = 0
		AJS.$(el).val(content.substring(0,fldLength));
	}
	AJS.$(counterSelector).text(len);
	return (fldLength - content.length);
}

ZEPHYR.Schedule.Execute.ScheduleView = Backbone.View.extend({
	initialize:function () {
        this.model.bind("change:executionStatus", this.updateStatusLozenge, this);		//Called in the event of auto status update
        this.updateDefect = false;
        this.updateCommentArea = true;
        AJS.$('#zfj-permission-message-bar-execution-detail').addClass('active');
        AJS.$('#zfj-permission-message-bar-plancycle-list').removeClass('active');
    },
	events:{
		"mousedown [id^='readonly-comment-div']"						:		"showEditableCommentArea",
		"mousedown #schedule-comment-area-cancel"						:		"hideEditableCommentAreaWithoutUpdate",
		'mousedown #schedule-comment-area-update'						: 		"updateCommentOnClick",
		"focusout [id^='schedule-comment-area']"						:		"hideEditableCommentArea",
		//"mousedown .execution_status_wrapper"							:		enableExecutionStatusDropDown,	//Referred from zephyr_common.js
		"click #readonly-schedule-defects .execution-defects-wrapper"	:		"showIssuePicker",
		"mousedown #zephyr_je_create_issue_link-schedule-update"		:		"updateDefects",
		"mousedown #zephyr_je_create_issue_link-schedule-cancel"		:		"cancelUpdateDefects",
		"click #readonly-schedule-defects [id^='current-defectpicker-status-dd-'] a" : 'stopImmediatePropagations',
		"click .item-delete" : 'stopImmediatePropagations',
		"focusout #editable-schedule-defects"						:		"hideDefectPicker"
	},

	/* This method keeps ScheduleView in sync with model change, useful when model is updated outside of the view (stepExecution)
	 * In direct executions from drop down, this call is reduntant but wont cause any harm.
	 * This is equiv of render
	 */
	updateStatusLozenge: function(){
		ZEPHYR.Schedule.Execute.updateExecutionHistory();
		var statusId = this.model.get('executionStatus');
		var optionElement = AJS.$("select[id^='exec_status-schedule'] option[value='"+statusId+"']");
		var color = optionElement.attr('rel');
		var statusText = optionElement.text();
		var statusTitle = statusText + (optionElement.attr('title') ? ': ' + optionElement.attr('title') : '');
		var scheduleId = this.model.get('id')
		AJS.$("select[id^='exec_status-schedule'] option:selected").removeAttr('selected')
		AJS.$("select[id^='exec_status-schedule'] option[value="+statusId+"]").attr('selected', 'selected')
		var scheduleElement =  AJS.$("#current-execution-status-dd-schedule-" + scheduleId )
		scheduleElement.css("background-color", color);
		scheduleElement.html(AJS.escapeHtml(statusText));
		scheduleElement.attr('title', statusTitle);

		/*Update the rel on edit link (This info is used to determine value is updated or not by Zephyr-common.js, ALAS!)*/
		var editLinkSpan = AJS.$("#executionStatus-labels-schedule-" + scheduleId + " span")
		editLinkSpan.html(statusText);
		editLinkSpan.attr('rel', statusId);
	},

	showEditableCommentArea: function (event){
		//Fix to handle click events on scrollbars
		//ZFJ-2143 Added AJS. to access the library function.
		var offX  = event.offsetX || event.clientX - AJS.$(event.target).offset().left;
        if(offX > event.target.clientWidth){
            return false;
        }
		this.$el.find("#readonly-comment-div").hide();
		var editableCommentControl = this.$el.find("#editable-comment-div")
		editableCommentControl.css("display", "inline-block");
		editableCommentControl.find('textarea').focus()
		editableCommentControl.bind("keyup", function(event){
			ZEPHYR.charCounter(event, '#schedule-comment-area', 750, '#comment-counter');
		});
		ZEPHYR.Schedule.Execute.data.comment = editableCommentControl.find('textarea').val();
		ZEPHYR.charCounter(null, '#schedule-comment-area', 750, '#comment-counter');
		this.updateCommentArea = true;
		event.preventDefault();
	},

	hideEditableCommentArea: function(event){
		if(!this.updateCommentArea){
			this.updateCommentArea = true;
			return;
		}
		this.updateCommentArea = true;
		this.updateComment();
	},

	updateCommentOnClick: function(ev) {
		ev.preventDefault();
		ev.stopImmediatePropagation();
		this.updateComment();
		this.updateCommentArea = false;
	},

	hideCommentOnSaveFailure: function() {
		var editableCommentControl = this.$el.find("#editable-comment-div");
		editableCommentControl.hide();
		editableCommentControl.find('span.loading').hide();
		this.$el.find("#readonly-comment-div").show();
	},

	updateComment: function() {
		var instance = this;
		var editableCommentControl = this.$el.find("#editable-comment-div");
		var currentText = editableCommentControl.find('textarea').val();
		editableCommentControl.unbind("keyup");
		if(JSON.stringify(currentText) == JSON.stringify(ZEPHYR.Schedule.Execute.data.comment)) {
			AJS.$(".aui-message").remove();
			this.hideCommentOnSaveFailure();
			return;
	    }

		//Execute given test step or schedule to change the status for the same.
		var req = this.model.save(
			{'comment': currentText, 'changeAssignee': false},
			{ url:getRestURL() + "/execution/"+ this.model.get('id') + "/execute",
			success: function(executionResponse){
				ZEPHYR.Schedule.Execute.updateExecutionHistory();
				AJS.$(".aui-message").remove();
				instance.$el.find("#comment-val").html(executionResponse.get('htmlComment'));
				editableCommentControl.hide();
				editableCommentControl.find('span.loading').hide();
				instance.$el.find("#readonly-comment-div").show();
				var exeSummaries = JSON.parse(executionResponse.get('executionSummaries') || '[]');
				var exeStatus = executionResponse.get('executionStatus');
				var exeStatusObj = exeSummaries.executionSummary.filter(function(executionStatus){ return executionStatus.statusKey == exeStatus});
				AJS.$("#current-execution-status-dd-schedule-"+ executionResponse.get('id')).text(exeStatusObj.length > 0 ? exeStatusObj[0].statusName : '');
				showExecStatusSuccessMessage();
			},
			error: function(response) {
				editableCommentControl.find('span.loading').hide();
				buildErrorMessage();
			}
		});
		// Callback to handle status 403's error condition
		req.error(function(jHR) {
			if(jHR.status == 403)
				instance.hideCommentOnSaveFailure();
		});
		editableCommentControl.find('span.loading').show();
	},

	// Hide the editable comment area on click of 'Cancel'.
	hideEditableCommentAreaWithoutUpdate: function(ev) {
		ev.preventDefault();
		ev.stopImmediatePropagation();
		this.updateCommentArea = false;
		var editableCommentControl = this.$el.find("#editable-comment-div")

		editableCommentControl.find('textarea').val(this.$el.find("#comment-val").html());
		editableCommentControl.hide();
		editableCommentControl.find('span.loading').hide();
		this.$el.find("#readonly-comment-div").show();
	},

	showIssuePicker: function(event){
		event.preventDefault();
		this.$el.find('#readonly-schedule-defects').hide();
		var editableDiv = this.$el.find('#editable-schedule-defects');
		editableDiv.css('display','inline');
		editableDiv.next('.description').show();
		editableDiv.find('.issue-picker-popup').attr('tabindex', 11);	//Workaround for Chrome 30 limitation where links are not tab enabled
		var instance = this;
		var blurTimer;
		ZEPHYR.Schedule.Execute.data.defects = [];
		AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li.item-row').each(function() {
			var val = AJS.$(this).find("button.value-item span.value-text");
			ZEPHYR.Schedule.Execute.data.defects.push(val.text());
		});

		// Commented the code to listen to click event.
		editableDiv.delegate(focusables, 'blur', function(event){
			if(blurTimer) clearTimeout(blurTimer)
			blurTimer = setTimeout(function(){instance.hideDefectPicker(event)}, 50);
		})

		// var _height = editableDiv.find('ul.items').height(),
		// 	_left = (editableDiv.find('ul.items li:last').position() && editableDiv.find('ul.items li:last').position().left) ? editableDiv.find('ul.items li:last').position().left: 0,
		// 	_width = editableDiv.find('ul.items li:last').width(),
		// 	_padding_left = _left + _width + 8,
		// 	_padding_top = _height;

		// if(_height < 30) {
		// 	_height = 30;
		// 	_padding_top = 4;
		// }
		//Let's set the focus on defect picker text area.
		// editableDiv.find("textarea")
		// 	.css({'height': _height, 'padding-top': _padding_top, 'padding-left': _padding_left})
		// 	.focus();
	},

	stopImmediatePropagations: function(ev) {
		ev.stopImmediatePropagation();
	},

	// On click of the 'Update' button trigger update defects call to hideDefectPicker.
	updateDefects: function(event) {
		event.preventDefault();
		event.stopImmediatePropagation();
		this.updateDefect = true;
		this.hideDefectPicker(event);
	},

	hideDefectPicker: function(event){
		var editableDiv = this.$el.find('#editable-schedule-defects');

		if(hasFocus(editableDiv) && !this.updateDefect){
			this.updateDefect = false;
			return;
		}
		// this.updateDefect = false;
		// editableDiv.hide();
		// editableDiv.next('.description').hide()
		// editableDiv.undelegate(focusables, 'blur')
		// this.$el.find('#readonly-schedule-defects').show();
		// Adding a delay so that the drop down elements are created
		setTimeout(function() {
			executeTest(ZEPHYR.Schedule.Execute.data.issueId, ZEPHYR.Schedule.Execute.currentSchedule.get('id'), 'defects');
		}, 100);
	},

	// Cancel update defects without saving the updates.
	cancelUpdateDefects: function(ev) {
		ev.preventDefault();
		var editableDiv 		= this.$el.find('#editable-schedule-defects');
		var instance 			= this;
		var currentDefectPicker = this.$el.find('#current-defectpicker-status-dd-schedule a');
		var executionId			= this.model.get('id');

		editableDiv.hide();
		editableDiv.next('.description').hide();
		editableDiv.undelegate(focusables, 'blur');
		this.$el.find('#readonly-schedule-defects').show();

		AJS.$('#editable-schedule-defects .issue-picker-popup').remove();
		AJS.$('#zephyrJEdefectskey-schedule-' + executionId + '-multi-select').remove();
		AJS.$('#zephyrJEdefectskey-schedule-' + executionId).empty();

		var issuePicker = new AJS.IssuePicker({
			element: AJS.$('select#zephyrJEdefectskey-schedule-' + executionId),
			userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
			uppercaseUserEnteredOnSelect: true
		});
		AJS.$.each(currentDefectPicker, function(i, defectPicker) {
			var issueKey = AJS.$(defectPicker).text();
			var summary = AJS.$(defectPicker).attr('title').split(':')[0];
			var item = new AJS.ItemDescriptor({
				value: issueKey, // value of item added to select
				label: issueKey + " - " + summary, // title of lozenge
				icon:  null // Need to have the canonicalBaseUrl for IE7 to avoid mixed content warnings when viewing the issuepicker over https
			});
			issuePicker.addItem(item);
		});
	}
})

// Refresh the execution history
ZEPHYR.Schedule.Execute.updateExecutionHistory = function() {
	AJS.$('#executiondetails-refresh').trigger('click');
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
	if(AJS.$("#zerrors")[0]) {
		var errors = AJS.$("#zerrors")[0].value;
		if(errors != null && errors.length > 0) {
			return;
		}
	}

	if(AJS.$('#zScheduleId').val()) {
		var issueId = AJS.$("#issue-id-for-selected-schedule").val(),
			executionId = AJS.$("#zScheduleId").val();
		ZEPHYR.Schedule.Execute.data = {issueId: issueId,
				scheduleId:AJS.$("#zScheduleId").val(),	//This is redundant coz its used in templates. Need to remove
				projectId:AJS.$('#pid').val()}


		var modelAttr = new ZEPHYR.Issue.ScheduleModel()

		modelAttr.fetch({contentType:'application/json',success:function(response) {
			var currentExecutionStatus,
				_assignee = {
					"id": modelAttr.attributes.execution.id,
					"assignee": modelAttr.attributes.execution.assignedTo,
					"assigneeDisplay": modelAttr.attributes.execution.assignedToDisplay,
					"assigneeUserName": modelAttr.attributes.execution.assignedToUserName
				};
			ZEPHYR.Schedule.executionStatus = modelAttr.attributes.status;
			for(var status in modelAttr.attributes.status) {
				if(modelAttr.attributes.status[status].id == modelAttr.attributes.execution.executionStatus) {
					currentExecutionStatus = modelAttr.attributes.status[status];
				}
			}
			ZEPHYR.ExecutionCycle.schedule = modelAttr.attributes.execution;
			ZEPHYR.Execution.currentSchedule = {
				'statusObj': {
					currentExecutionStatus: currentExecutionStatus,
					executionStatuses: modelAttr.attributes.status,
				}
			}
			var detailsHtml = ZEPHYR.Templates.Execution.detailedExecutionView({schedule:modelAttr.attributes.execution,
				executedBy:modelAttr.attributes.execution.executedBy,
				executedByDisplay:modelAttr.attributes.execution.executedByDisplay,
				currentExecutionStatus:currentExecutionStatus,
				executionStatuses:modelAttr.attributes.status,
				contextPath:contextPath,
				projectKey:AJS.$("#projectKey").val(),
				projectName:AJS.$("#projectName").val(),
				projectAvatarId:AJS.$("#projectAvatarId").val(),
				issueDescAsHtml:AJS.$("#issueDescAsHtml").val()});

			var leftHtml = ZEPHYR.Templates.Execution.leftNavDetails({schedule:modelAttr.attributes.execution,
																	  contextPath:contextPath,
																	});
			var exeCont = AJS.$(".exe-cont");
			if (exeCont.length){
				exeCont.html('');
			} else {
				AJS.$("#content").append(ZEPHYR.Templates.Execution.executionWrapper({}));
				exeCont = AJS.$(".exe-cont");
			}

			exeCont.append(leftNavDetails);
			exeCont.append(detailedViewHtml);

    	/* Initialize backbone model using form elements */
			ZEPHYR.Schedule.Execute.currentSchedule = new ZEPHYR.Issue.Schedule({
				id:parseInt(AJS.$('#zScheduleId').val()),
				executionStatus: AJS.$("select[id^=exec_status-schedule] option:selected").attr("value"),
				comment:AJS.$('#schedule-comment')
			});
			ZEPHYR.Schedule.Execute.getAttachments(ZEPHYR.Schedule.Execute.currentSchedule.get("id"), "SCHEDULE");
			scheduleView = new ZEPHYR.Schedule.Execute.ScheduleView({model:ZEPHYR.Schedule.Execute.currentSchedule, el:AJS.$("#zexecute")});
			AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath ] );
			// AJS.$('#exec-assignee-wrapper').append(ZEPHYR.Templates.Execution.executionAssigneeDetailView({
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
			ZEPHYR.Execution.History.init(issueId, executionId);
		}});
	}

	jQuery.ajax({
		url: getRestURL() + '/util/teststepExecutionStatus',
		type: "GET",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			console.log('RESPONSE DATA od test step statuses : ', response);
			var stepStatuses = []
			response.forEach(function(status) {
				stepStatuses[status.name] = status.id;
			})
			ZEPHYR.GRID.stepStatuses = stepStatuses;
		}
	});
});

ZEPHYR.Schedule.Execute.getAttachments = function(entityId, entityType, successCallback){
	jQuery.ajax({
		url: getRestURL() + "/attachment/attachmentsByEntity?entityId="+entityId+"&entityType="+entityType,
		type : "get",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			if(entityType == "SCHEDULE"){
				for (let counter = 0; counter < response.data.length; counter += 1) {
					response.data[counter].imageSizeInMb = parseFloat(response.data[counter].fileSize / 1024 / 1024).toFixed(2);
					response.data[counter].isImageType = (/\.(gif|jpg|jpeg|tiff|png)$/i).test(response.data[counter].fileName);
					response.data[counter].objectStringify = JSON.stringify(response.data[counter]);
				}
				attachmentDuplicate = response.data.filter(function(file) {
					return file.isImageType;
				});
			   var innerhtml = ZEPHYR.Schedule.Attachment.createAttachmentDiv({attachments:response.data,baseUrl:contextPath});
			   var innerhtmladd = ZEPHYR.Schedule.Attachment.addAttachmentBtn({schedule:ZEPHYR.ExecutionCycle.schedule,contextPath:contextPath});
			   AJS.$("#file_attachments").html(innerhtml + innerhtmladd);
		   }
		   else if(entityType == "TESTSTEPRESULT"){
			   var innerhtml = ZEPHYR.Step.Attachment.createAttachmentDiv({attachments:response.data,baseUrl:contextPath,entityId:entityId});
			   AJS.$("#step-result-id-"+ entityId +"-file-attachments").html(innerhtml);
         		if(successCallback)
           			successCallback(response.data);
          		else {
					ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult){
						if(Number(entityId) === stepResult.id) {
							stepResult['stepAttachmentMap'] = response.data;
							stepResult['updateGrid'] = stepResult.hasOwnProperty('updateGrid') ? !stepResult['updateGrid'] : true;
						}
					});
					ZEPHYR.Execution.testDetailGridExecutionPage();
          		}
		   }
		   else if(entityType == "TESTSTEP") {
				var innerhtml = ZEPHYR.Step.Attachment.createTestStepAttachmentDiv({attachments:response.data,baseUrl:contextPath,entityId:entityId});
				var attachemntViewDialog = AJS.$("#inline-dialog-attachment-dialog-" + entityId);
		         if(attachemntViewDialog.length) {
		           attachemntViewDialog.remove();
		         }
				AJS.$("#step-"+ entityId +"-row #file_attachments").html(innerhtml);
			}
		}
	});
	return;
}

function getInvalidDefectKeys(defects, prevDefects) {
	// Create an array of valid saved defects and get the difference w.r.t. invalid defects
	var filteredDefects = _.map(defects, function(defect){ return defect.key; });
	return _.difference(prevDefects, filteredDefects);
}

function getCurrentValidDefectKeys(prevDefects, defects) {
	// Create an array of valid saved defects and get the difference w.r.t. valid defects
	var filteredDefects = _.map(defects, function(defect){ return defect.key; });
	if(filteredDefects.length < prevDefects.length)
		return _.difference(prevDefects, filteredDefects);

	return _.difference(filteredDefects, prevDefects);
}

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

function executeTest(issueId, scheduleId, fieldType) {
	//Clear up Previous Message if any
	AJS.$(".aui-message").remove();

	var value = [];
	AJS.$('#zephyrJEdefectskey-schedule-'+ scheduleId + '-multi-select ul.items li.item-row').each(function() {
		var val = AJS.$(this).find("button.value-item span.value-text");
		value.push(val.text());
	});

    if(JSON.stringify(value) == JSON.stringify(ZEPHYR.Schedule.Execute.data.defects)) {
		console.log('Nothing saved, hence skipping save');
		return;
	}

	// var needToRefresh = false;

	// for (var oldDefectCounter = 0; oldDefectCounter < ZEPHYR.Schedule.Execute.data.defects.length; oldDefectCounter += 1) {
	// 	var hasDefect = false;
	// 	for (var newDefectCounter = 0; newDefectCounter < value.length; newDefectCounter += 1) {
	// 		if (ZEPHYR.Schedule.Execute.data.defects[oldDefectCounter] == value[newDefectCounter]) {
	// 			hasDefect = true;
	// 		}
	// 	}
	// 	if (hasDefect == false) {
	// 		needToRefresh = true;
	// 		break;
	// 	}
	// }

	// if (!window.standalone && AJS.$('#zqltext')[0] && AJS.$('#zqltext')[0].value.length != 0) {
	// 	setTimeout(function () {
	// 		ZEPHYR.ZQL.maintain = true;
	// 		if (AJS.$('#zephyr-transform-all') && AJS.$('#zephyr-transform-all')[0]) {
	// 			AJS.$('#zephyr-transform-all')[0].click();
	// 		}
	// 	}, 1000)
	// }

	jQuery.ajax({
		url: getRestURL() + "/execution/"+ scheduleId +"/execute",
		type : "PUT",
		contentType :"application/json",
		global:false,
		data: JSON.stringify( {
			  'defectList' : value,
	          'issueId' :  issueId,
	          'comment': (AJS.$('#schedule-comment-area')).val(),
	          'updateDefectList': 'true',
	          'changeAssignee': false
		}),
		success : function(schedule) {
			AJS.$(".aui-message").remove();
			window.onbeforeunload=null;

			var _invalidDefectIds = getInvalidDefectKeys(schedule.defects, value);
			var previousDefects = ZEPHYR.Schedule.Execute.data.defects || [];
			var _validDefectsIds = getCurrentValidDefectKeys(previousDefects, schedule.defects);
			ZEPHYR.Schedule.Execute.data.defects = schedule.defects && schedule.defects.map(function(defect){
				return defect.key;
			})
			// ZEPHYR.Schedule.Execute.data.defects = [];
			// AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li.item-row').each(function() {
			// 	var val = AJS.$(this).find("button.value-item span.value-text");
			// 	ZEPHYR.Schedule.Execute.data.defects.push(val.text());
			// });

			ZEPHYR.Schedule.Execute.updateExecutionHistory();
			if(schedule.executedBy != null) {
				AJS.$("#executedBy").parent().css("display", "block");
				AJS.$("#executedBy").html("<strong><a href='"+ contextPath +"/secure/ViewProfile.jspa?name="+schedule.executedBy+"' rel='"+schedule.executedBy +"' class='user-hover'>" + schedule.executedBy + "</a></strong>");
				AJS.$("#executedOn").html("<strong>" + schedule.executedOn + "</strong>");
			}else{
				AJS.$("#executedBy").parent().css("display", "none");
				AJS.$("#executedBy").html("");
				AJS.$("#executedOn").html("");
			}

			//Find out the invalid entries and pop them out from thr Value Array used for JSOn request
			if(schedule.defects){
				var issueKeys = [];
				var tempDefects = [];
				AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li.item-row').each(function() {
						var val = AJS.$(this).find("button.value-item span.value-text");
						tempDefects.push(val.text());
				});


				console.log('tempDefects', tempDefects);
				AJS.$.each(schedule.defects, function(i, issue) {
					if(AJS.$.inArray(issue.key, value) > -1) {
						if (value != 'XXXXX') {
							value = AJS.$.removeFromArray(issue.key, value);
						}
					}
					if(tempDefects.indexOf(issue.key) > -1) {
						tempDefects.splice(tempDefects.indexOf(issue.key), 1);
					}
					console.log('hello', issue.key, tempDefects);
					issueKeys.push(ZEPHYR.Templates.StepExecution.defectLink({defect:issue})); // Creating the defect keys HTML
				});
				tempDefects.forEach(function(defect){
						var nodeId;
						AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li.item-row').each(function() {
							var val = AJS.$(this).find("button.value-item span.value-text");
							if(val.text() === defect) {
								nodeId = AJS.$(this).attr('id');
							}
						});
						AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li#' + nodeId).remove();
					})
				AJS.$('#current-defectpicker-status-dd-schedule').html(issueKeys.join());

        listOfElem = AJS.$('#editable-schedule-defects').find('.representation ul').find('li');
    		for(var i = 0; i < listOfElem.length; i += 1) {
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
    				AJS.$(listOfElem[i]).find('.value-text').html('<a data-href="' + contextPath + '/browse/' + issueText + '">'+ issueText + '</a>');
    				AJS.$(listOfElem[i]).find('.value-text').addClass('linkAppended');
    			}
    			var issueId = AJS.$(listOfElem[i]).find('.value-text a').html();
    			var defectObj = _.findWhere(schedule.defects, {key: issueId});
    			var title = defectObj.summary + ' : ' + defectObj.status;
        	AJS.$(listOfElem[i]).attr('title', title);
    			if(defectObj && defectObj.resolution === 'Done') {
    				AJS.$(listOfElem[i]).find('.value-text a').css('text-decoration', 'line-through');
    			}
    		}
			}else{
				AJS.$('#current-defectpicker-status-dd-schedule').text("");
			}

			//iterate through the array and empty out the Invalid entries from text area.
			// for(var issueKey in value) {
			// 	AJS.$("#zephyrJEdefectskey-schedule-"+ scheduleId +"-multi-select ul.items li.item-row").each(function() {
			// 		var val = AJS.$(this).find("button.value-item span.value-text");
			// 		if(value[issueKey] == val.text()) {
			// 			AJS.$(this).remove();
			// 			var paddingL = AJS.$("#zephyrJEdefectskey-schedule-"+ scheduleId +"-textarea").css("padding-left");
			// 			paddingL = paddingL.substring(0,paddingL.length - 2);
			// 			AJS.$("#zephyrJEdefectskey-schedule-"+ scheduleId +"-textarea").css("padding-left",paddingL-40);
			// 		}
			// 	});
			// }

			AJS.$("#exec_status-schedule-"+ scheduleId).val(schedule.executionStatus);
			//AJS.$("#exec_status-schedule-"+ scheduleId + " option[value='"+ scheduleStatus + "']").attr("selected", true);
			var newStatus = AJS.$("#exec_status-schedule-" + scheduleId +" .selected").text();
			var statusDescription = AJS.$("#exec_status-schedule-"+ scheduleId +" .selected").attr("title");
			var color = AJS.$("#exec_status-schedule-" + scheduleId +" option:selected").attr("rel");
			var title = newStatus + (statusDescription ? ': ' + statusDescription : '');
			AJS.$("#current-execution-status-dd-schedule-" + scheduleId ).css("background-color", color);
			AJS.$("#current-execution-status-dd-schedule-"+ scheduleId ).html(newStatus);
			AJS.$("#current-execution-status-dd-schedule-"+ scheduleId ).attr("title",title);
			// Moved Success message to common so that we can resuse
			var _message = '';
			if(_invalidDefectIds.length && !_validDefectsIds.length) {
				_message = AJS.I18n.getText('zephyr.je.invalid.defects', _invalidDefectIds.join(', '));
				showExecStatusInfoMessage(null, _message);
			} else {
				if(_invalidDefectIds.length) {
					_message = AJS.I18n.getText("execute.defects.success.info");
					showExecStatusInfoMessage(null, AJS.I18n.getText('zephyr.je.invalid.defects', _invalidDefectIds.join(', ')));
				} else if(!_invalidDefectIds.length && fieldType === 'defects') {
					_message = AJS.I18n.getText("execute.defects.success.info");
				}
				showExecStatusSuccessMessage(_message);
			}
			resizeTextarea();
			AJS.$(document).trigger( "updateExecutionModel", [schedule] );

		},

		error : function(response) {
			if(!ZEPHYR.globalVars.unloaded)
				buildExecutionError(response);
		}
	});
}

AJS.$.removeFromArray = function(value, arr) {
    return jQuery.grep(arr, function(elem, index) {
        return elem !== value;
    });
};

var addDefect = function(event, issueData) {
  var item = new AJS.ItemDescriptor({
        value: issueData.issueKey, // value of item added to select
        label: issueData.issueKey + " - " +issueData.summary, // title of lozenge
        icon:  null // Need to have the canonicalBaseUrl for IE7 to avoid mixed content warnings when viewing the issuepicker over https
    });

  if(gridRefresh) {
    var data = {
      defectList : [],
      updateDefectList :"true",
    }
    ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult) {
      if(stepResult.id === defectRowId) {
        stepResult.defects && stepResult.defects.map(function(defect){
          data.defectList.push(defect.key);
        });
      }
    });
    data.defectList.push(issueData.issueKey);
  	var scrollPosLeft = document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft;
    jQuery.ajax({
  		url: getRestURL() + '/stepResult/' +  defectRowId,
  		type : 'PUT',
  		contentType :"application/json",
  		data : JSON.stringify(data),
  		dataType: "json",
  		success : function(response) {
  			ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult) {
  				if(stepResult.id === response.id){
  					if(data.defectList) {
  						stepResult.defects = response.defectList;
  				  }
          }
  			});
  			ZEPHYR.Execution.testDetailGridExecutionPage();
  			setTimeout(function(){
            document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
        }, 0)
  		}
  	});
  }
  else if(typeof currentPicker !== 'undefined') {
    if(currentPicker)
      currentPicker.addItem(item);
    // Fix for ZFJ-2003
    try {
      if(currentPicker.$field)
        currentPicker.$field.focus();
    } catch (e){
      console.log(e);
    }
  //console.log("Picker ID - " + currentPicker.model.$element[0].id);
  }
  event.stopImmediatePropagation();
}

//Triggers toggle
InitPageContent(function(){
	//AJS.$("#teststepDetails").delegate('[id^="readonly-defect-values-stepresult-"]', 'click', enableStepDefectPicker);

	// Toggles
    var toggle = new JIRA.ToggleBlock({
	 blockSelector: ".toggle-wrap",
	 triggerSelector: ".mod-header h3",
	 cookieCollectionName: "admin",
	 originalTargetIgnoreSelector: "a"
    });

    // Only init for 5.0+ instances
    if (!JIRA.Version.isGreaterThanOrEqualTo("5.0")) {
        return;
    }

    /*issueCreated is published in global namespace, hence adding listener to AJS*/
    jQuery(AJS).bind("QuickCreateIssue.issueCreated", function (event, issueData){
      addDefect(event, issueData);
    });
    AJS.$(AJS).bind("QuickCreateIssue.issueCreated", function (event, issueData){
    	addDefect(event, issueData);
    });

    AJS.$(document).on('keydown', function (e) {
		if (e.keyCode == '37' || e.keyCode == '39') {
			if (AJS.$('.image-corosel-overlay').length) {
				if (e.keyCode == '37') {
					AJS.$('.previous-image-options').trigger('click');
				} else if (e.keyCode == '39') {
					AJS.$('.next-image-options').trigger('click');
				}

				e.preventDefault();
			}

		}
	});
});

/**
 * _issueType is passed from caller to indicate the pre-selected issueType. This value is fetched via rest API
 * If its invalid, it will get defaulted to 1 (Bug). There is no way to use last used one.
 */
var initializeDefectPickers = function (_issueType){
    _issueType = function(){
        if(_issueType && !isNaN(_issueType))
            return parseInt(_issueType);
        else
            return 1;
    }();
	 // Creates create issue dialog /* Doesnt work in JIRA 7.1 */
	if(JIRA.Forms){
		var createForm = JIRA.Forms.createCreateIssueForm({
			issueCreated: function (data) {
			},
			issueType:_issueType,
      submit: function() {
        console.log(submitted);
      }
		});
	} else {
		// Fix for ZFJ-2003
		AJS.$(document).unbind("DOMNodeRemoved");
		AJS.$(document).bind("DOMNodeRemoved", function(ev) {
		    if(ev.target.id == "create-issue-dialog") {
		    	try {
			    	if(currentPicker && currentPicker.$field)
			    		currentPicker.$field.focus();
        		} catch (e){
        			console.log(e);
        		}
		    }
		});
	}
	/*attaching data-issue-type, can also attach data-pid */
  issueType = _issueType;
  jQuery("a.zephyr_je_create_issue_link-stepresult, a.zephyr_je_create_issue_link-schedule").attr('data-issue-type', _issueType);
    /**
     * Dialog dismiss sequence of events:
     * on Cancel: only Dialog.hide event on dialog instance returned by asDialog method
     * on Submit:
     * 		1. createForm --> sessionComplete
     * 		2. GLOBAL --> QuickCreateIssue.sessionComplete
     * 		3. dialog --> Dialog.hide
     * 		4. createForm.configurableForm/createForm.unconfigurableForm --> sessionComplete
     */
	 //Initializes and calls IssuePicker to create the Drop Down on Execute Test Page
  AJS.$(document.body).find('.aui-field-defectpickers').each(function () {

  	if(!AJS.$(this).hasClass('processed-aui-field-defectpicker')) {
          //Defect picker id is in form zephyrJEdefectskey-<entityType>-<entityId>
        	//Examples zephyrJEdefectskey-schedule-1, zephyrJEdefectskey-stepresult-10
        	//We will extract entityType and entityId from this id.
          AJS.$(this).addClass('processed-aui-field-defectpicker');

        	var defectPickerId = AJS.$(this)[0].id;
        	var source = ZEPHYR.Schedule.getSourceInfoForStatusExecution(defectPickerId);

        	var picker = new JIRA.IssuePicker({
              element: AJS.$(this),
              userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
              uppercaseUserEnteredOnSelect: true
          });

          setExistingDefectMap(picker, source.sourceType, source.sourceId);

      		/* Doesnt work in JIRA 7.1 */
      		if(createForm){
      			jQuery("#zephyr_je_create_issue_link-"+ source.sourceTag).click(function (e) {
      				e.stopPropagation();
      			});
      			var dialog = createForm.asDialog({
      				trigger: jQuery("#zephyr_je_create_issue_link-" + source.sourceTag),
      				id: "create-issue-dialog-" + source.sourceTag
      			});
      			createForm.bind("dialogShown", function (){
      				//Do something after dialog is shown
      			});
      			dialog.bind("Dialog.hide", function (){
      				currentPicker.$field.focus();	//currentPicker.$field is Textarea
      			})
            		dialog.bind("Dialog.submit", function (){
      				currentPicker.$field.focus();	//currentPicker.$field is Textarea
      			})
      		}

          /**
           * Associating a anonymous function which freezes current picker by passing it to the function and executing it at the time of picker creation.
           * When user will press the CreateIssue button link, corresponding method will be called and will set currentPicker to corresponding picker.
           * Caveat: This is not a very scalable approach, as it will created one anonymous function for each Step row.
           */
          AJS.$("#zephyr_je_create_issue_link-"+ source.sourceTag).bind("click", function (pickerRef){
          	return function(){
          		currentPicker = pickerRef;
          		//console.log("Picker ID - " + pickerRef.model.$element[0].id);
          	}
          }(picker));
          if(source.sourceType == 'stepresult'){
          	picker.$container.addClass('medium-multiselect');
        }
    }

    //picker.$field.addClass('zdefecttextarea')
  	//console.log("Picker ID - " + picker.model.$element[0].id );
  });
}

/*Catches Delete Entity and generates Confirmation dialog*/
AJS.$("#entity-operations-delete").live("click", function(e) {
	 var entity = AJS.$(this).attr('val');
	 var attachment = entity.split(':');
	 deleteConfirmationDialogExecution(attachment[0], attachment[1]);
	 e.stopImmediatePropagation();
	 return;
});
/**/
AJS.$("#see-more-desc").live("click", function(e) {
	if(AJS.$('.description-val').hasClass('collapse')) {
        AJS.$('.description-val').css("-webkit-line-clamp", "100");
        AJS.$('.description-val').removeClass('collapse');
        AJS.$('#see-more-desc').html(AJS.I18n.getText('cycle.showless.label'));
    }
    else{
    	AJS.$('.description-val').css("-webkit-line-clamp", "3");
    	AJS.$('#see-more-desc').html(AJS.I18n.getText('cycle.showmore.label'));
    	AJS.$('.description-val').addClass('collapse');
    }
});

AJS.$("#see-more-summ").live("click", function(e) {
	if(AJS.$('.summ-val').hasClass('collapse')) {
        AJS.$('.summ-val').css("-webkit-line-clamp", "100");
        AJS.$('.summ-val').removeClass('collapse');
        AJS.$('#see-more-summ').html(AJS.I18n.getText('cycle.showless.label'));
    }
    else{
    	AJS.$('.summ-val').css("-webkit-line-clamp", "1");
    	AJS.$('#see-more-summ').html(AJS.I18n.getText('cycle.showmore.label'));
    	AJS.$('.summ-val').addClass('collapse');
    }
});

AJS.$(".test-step-filter-icon-wrp").live("click", function(){
	if(AJS.$(".test-step-filter-overlay").hasClass('hide')){
		AJS.$(".test-step-filter-overlay").removeClass('hide');
	} else{
		AJS.$(".test-step-filter-overlay").addClass('hide');
	}
});

AJS.$("#clear-search-step-test").live("click", function(){
	AJS.$("#search-step-test").val("");
	AJS.$('#clear-search-step-test').css("display", "none");
});

AJS.$("#search-step-test").live("keyup", function(){
	if(AJS.$("#search-step-test").val()){
		AJS.$('#clear-search-step-test').css("display", "block");
	} else{
		AJS.$('#clear-search-step-test').css("display", "none");
	}
});

AJS.$("#cancelFilter").live("click", function(){
	AJS.$("#search-step-test").val("");
	AJS.$("#applyFilter").trigger('click');
});

AJS.$('body').live("click", ".test-step-filter-overlay", function (e) {
	if (AJS.$(e.target).parents('.test-step-filter-container').length === 0){
		AJS.$(".test-step-filter-overlay").addClass('hide');
	}
});




/*test step details filter*/
AJS.$(".test-step-filter-overlay #teststepdropdown").live("change", function(e) {
//	console.log('changeed....',e);
	AJS.$("#search-step-test").val("");
})

var teststepresults = [];
AJS.$("#applyFilter").live("click", function(e){

	if (AJS.$('#pagination-outer-container').length && AJS.$("#search-step-test")[0].value != '') {
		paginationDisplayFlag = false;
		AJS.$('.test-step-filter-icon-wrp').addClass('active');

		var filter = AJS.$("#search-step-test").val();

		var filterType = AJS.$("#teststepdropdown option:selected").val();
		if (filterType == 'all') {
			filterType = 'default';
		} else if (filterType == 'step') {
			filterType = 'testStep';
		} else if (filterType == 'data') {
			filterType = 'testData';
		} else if (filterType == 'result') {
			filterType = 'testResult';
		} else if (filterType == 'status') {
			filter = ZEPHYR.GRID.stepStatuses[filter.toUpperCase()];

			//  filter = filter.toLowerCase();
			// if (filter == 'pass') {
			// 	filter = '1';
			// } else if (filter == 'fail') {
			// 	filter = '2';
			// } else if (filter == 'wip') {
			// 	filter = '3';
			// } else if (filter == 'blocked') {
			// 	filter = '4';
			// } else if (filter == 'unexecuted') {
			// 	filter = '-1';
			// }
		} else if (filterType == 'htmlComment') {
			filterType = 'comment';
		}


		jQuery.ajax({
			url: getRestURL() + '/stepResult/byFilter?executionId=' + ZEPHYR.Schedule.Execute.currentSchedule.get('id') + '&expand=executionStatus&filterKey=' + filterType + '&searchKey=' + filter,
			type: "GET",
			contentType: "application/json",
			dataType: "json",
			success: function (response) {
				console.log('RESPONSE DATA : ', response);
				ZEPHYR.Execution.testDetailsGrid.stepResults = response;
				ZEPHYR.Execution.testDetailGridExecutionPage("enable");
				AJS.$(".test-step-filter-overlay").addClass('hide');
			}
		});

	} else {
		paginationDisplayFlag = true;
		// applyFilterFunction();
		var stepResults = new ZEPHYR.Schedule.Execution.TestStepResultCollection();
		stepResults.fetch({
			data: { executionId: ZEPHYR.Schedule.Execute.currentSchedule.get('id'), expand: "executionStatus", offset: ZEPHYR.ExecutionCycle.testStep.offset, limit: ZEPHYR.ExecutionCycle.testStep.limit }, success: function (response) {
				ZEPHYR.Execution.testDetailsGrid.stepResults = [];

				stepResults.models.map(function (stepResult) {
					ZEPHYR.Execution.testDetailsGrid.stepResults.push(stepResult.attributes);
				});

				teststepresults = ZEPHYR.Execution.testDetailsGrid.stepResults;

				if (ZEPHYR.Execution.testDetailsGrid.stepResults.length === 0) {
					ZEPHYR.Execution.testDetailGridExecutionPage();
				}
				ZEPHYR.Execution.testDetailGridExecutionPage();
				try {
					jQuery.ajax({ url: getRestURL() + '/issues/default?project=' + ZEPHYR.Schedule.Execute.data.projectId })
						.always(initializeDefectPickers);
				} catch (e) {
					console.log(e);
				}
				ZEPHYR.Loading.hideLoadingIndicator();

				applyFilterFunction();
				AJS.$('.test-step-filter-icon-wrp').removeClass('active');
			}
		});
	}

	if (!paginationDisplayFlag) {
		if (!AJS.$('#pagination-outer-container').hasClass('hide')) {
			AJS.$('#pagination-outer-container').addClass('hide');
		}
	} else {
		if (AJS.$('#pagination-outer-container').hasClass('hide')) {
			AJS.$('#pagination-outer-container').removeClass('hide');
		}
	}
});

var applyFilterFunction = function () {
	// if (!paginationDisplayFlag) {
	// 	if (!AJS.$('#pagination-outer-container').hasClass('hide')) {
	// 		AJS.$('#pagination-outer-container').addClass('hide');
	// 	}
	// } else {
	// 	if (AJS.$('#pagination-outer-container').hasClass('hide')) {
	// 		AJS.$('#pagination-outer-container').removeClass('hide');
	// 	}
	// }

	var filter = AJS.$("#search-step-test").val().toLowerCase();
	var row = AJS.$("#teststepDetails table tbody tr");
	var selected = AJS.$("#teststepdropdown option:selected").val();
	if (teststepresults.length == 0) {
		teststepresults = ZEPHYR.Execution.testDetailsGrid.stepResults;
	}
	var temp = [];
	if (selected != 'status' && selected != 'all') {
		teststepresults.map(function(stepResult){
			if((stepResult[selected].toLowerCase()).indexOf(filter)>-1){
				temp.push(stepResult);
			}
		});
	} else if (selected == 'status') {
		var sts;
		teststepresults.map(function(stepResult){
			for(var i=0; i< stepResult.executionStatus.length;i++){
				if(stepResult.executionStatus[i].id == stepResult.status){
					sts = stepResult.executionStatus[i].name;
					break;
				}
			}
			if ((sts.toLowerCase()).indexOf(filter)>-1) {
				temp.push(stepResult);
			}
		});
	} else if (selected == 'all') {
		teststepresults.map(function(stepResult){
			if((stepResult["step"].toLowerCase()).indexOf(filter)>-1 || (stepResult["data"].toLowerCase()).indexOf(filter)>-1 || (stepResult["result"].toLowerCase()).indexOf(filter)>-1 || (stepResult["htmlComment"].toLowerCase()).indexOf(filter)>-1){
				temp.push(stepResult);
			} else {
				for(var i=0; i< stepResult.executionStatus.length;i++){
					if(stepResult.executionStatus[i].id == stepResult.status){
						sts = stepResult.executionStatus[i].name;
						break;
					}
				}
				if ((sts.toLowerCase()).indexOf(filter)>-1) {
					temp.push(stepResult);
				}
			}
		});
	}
	ZEPHYR.Execution.testDetailsGrid.stepResults = temp;
	ZEPHYR.Execution.testDetailGridExecutionPage("enable");
	AJS.$(".test-step-filter-overlay").addClass('hide');
}


/*In lack of model layer, persisting data by passing thro' functions*/
var deleteConfirmationDialogExecution = function (entityId,entityN) {
    var instance = this,
    dialog = new JIRA.FormDialog({
        id: "entity-" + entityId + "-delete-dialog",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
        	var innerHtmlStr = ZEPHYR.Project.Confirmation.deleteConfirmationDialog({entityName:entityN});
            callback(innerHtmlStr);
        },

        submitHandler: function (e) {
        	deleteAttachment(entityId, function () {
        		dialog.hide();
          });
          e.preventDefault();
        }
    });

    dialog.show();
}

var deleteAttachment = function(entityId, completed) {
  var	scrollPosLeft = document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft;

	jQuery.ajax({
		url: getRestURL() + "/attachment/" + entityId,
		type : "delete",
		contentType :"application/json",
		dataType: "json",
		success : function(response) {
			/*Full server refresh - or shall we just remove the corresponding cycle div?. Removed DIV*/
			/* Retrieve Attachment Content and remove the one deleted */
      var stepId = AJS.$('#attachment-content'+entityId).closest('.attachment-content-wrapper').attr('data-id');
			AJS.$('#attachment-content'+entityId).remove();
      if(AJS.$("#attachment-content-container-" + stepId + " .attachment-content").length === 0) {
				AJS.$("#attachment-inlineDialog-"+stepId).remove();
				AJS.$("#inline-dialog-attachment-dialog-" + stepId).remove();
			}

      if(gridRefresh) {
        ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult, index) {
          if(stepResult.id === Number(deleteAttachmentStepId)) {
            stepResult.stepAttachmentMap = stepResult.stepAttachmentMap && stepResult.stepAttachmentMap.filter(function(attachment){
              return attachment.fileId !== entityId;
						});
						onlyUpdateGridValue = true;
						  updatedGridData.rowData = {
								index: index,
								stepAttachmentMap: stepResult.stepAttachmentMap || [],
								showAttachmentPopover: false,
							}
			   	}
			  });
			  ZEPHYR.Execution.testDetailGridExecutionPage();
			}
			setTimeout(function(){
          document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
      }, 0);
			if(completed)
				completed.call();
		},
		error : function(response) {
            if(response && response.status == 403) {
                var _responseJSON = {};
                try {
                    _responseJSON = jQuery.parseJSON(response.responseText);
                } catch(e) {
                    console.log(e);
                }
                if(_responseJSON.PERM_DENIED) {
                    showPermissionError(response);
                    return;
                }
            } else {
                buildExecutionError(response);
            }
			if(completed)
				completed.call();
		}
	});
}

var setExistingDefectMap = function(issuePicker, entityType, entityId) {
	var restResourceToCall = "/stepResult/";
	var restResourceURL = getRestURL();

	if (entityType == "schedule"){
		restResourceToCall = "/execution/";
		restResourceURL += restResourceToCall + entityId + "/defects"
	}else{
		//When we are getting defect map for Steps, we need to send schedule ID to make sure we get defect map for correct Test Execution.
		restResourceURL += restResourceToCall + entityId + "/defects?executionId="+ ZEPHYR.Schedule.Execute.currentSchedule.get('id');
	}

	jQuery.ajax({
		url: restResourceURL,
		type : "get",
		contentType :"application/json",
		global:false,
		success : function(response) {
			var issueKeys = [];
			for(var key in response) {
				AJS.$.each(response[key], function(issueKey, value) {
					var maskedIssueKey = value.maskedIssueKey ? value.maskedIssueKey : issueKey;
			    	var item = new AJS.ItemDescriptor({
			            value: issueKey, // value of item added to select
			            label: issueKey + " - " +value.summary, // title of lozenge
			            icon:  null // Need to have the canonicalBaseUrl for IE7 to avoid mixed content warnings when viewing the issuepicker over https
			        });
			    	issuePicker.addItem(item);
			    	var title = AJS.escapeHtml(value.summary + ":" + value.status);
			    	if(value.resolution){
						if (maskedIssueKey=='XXXXX') {
                            issueKeys.push("<span style='text-decoration:line-through;' title='" + title + "'>" + maskedIssueKey + "</span>");
						}else{
							issueKeys.push("<a style='text-decoration:line-through;' href='" + contextPath + "/browse/" + maskedIssueKey + "' title='" + title + "'>" + issueKey + "</a>");
						}
					}
					else{
						if (maskedIssueKey=='XXXXX') {
                            issueKeys.push("<span title='" + title + "'>" + maskedIssueKey + "</span>");
						}else{
							issueKeys.push("<a href='" + contextPath + "/browse/" + issueKey + "' title='" + title + "'>" + maskedIssueKey + "</a>");
						}
					}
				});
				if(entityType == 'schedule'){
					AJS.$('#current-defectpicker-status-dd-schedule').html(issueKeys.join(''));
					ZEPHYR.Schedule.Execute.data.defects = [];
					AJS.$('div[id^="zephyrJEdefectskey-schedule-"] ul.items li.item-row').each(function() {
						var val = AJS.$(this).find("button.value-item span.value-text");
						ZEPHYR.Schedule.Execute.data.defects.push(val.text());
					});
				}
			}
		},
		error : function(response) {
			jQuery(document).trigger(JIRA.SERVER_ERROR_EVENT, response);
		}
	});
}


/**
 * response: xhr object
 * Context: message div selector - defaults to element with id #zephyr-aui-message-bar
 */
function buildExecutionError(response, context){
	var contextSelector = context || ".zephyr-aui-message-bar";
	AJS.$(contextSelector).empty()
	/*Incase response is not JSON based*/
	try{
		var errorResponse = jQuery.parseJSON(response.responseText);
		var consolidatedErrorMessage = "";
		jQuery.each(errorResponse, function(key, value){
			consolidatedErrorMessage += value + "<br/>";
		});
	}catch(err){
		console.log(response.responseText);
		console.log(err);
	}
	consolidatedErrorMessage = consolidatedErrorMessage || (response.status + " - " + response.statusText)
	AJS.messages.error(contextSelector, {
	    title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
	    body: consolidatedErrorMessage
	});

	if(typeof(dialog) != "undefined")
		AJS.$(':submit', dialog.$form).removeAttr('disabled');
}

/**
 * Attaches File and does a silent refresh
 */
function attachFiles(entityId, entityType) {
	data = AJS.$("form#zephyr-attach-file").serialize();
    if(data.indexOf('&filetoconvert=') < 0 ){
		alert(AJS.I18n.getText('zephyr.je.execute.test.alert.selectAtleastOneFile'));asdf
        return;
    }

    if(data.indexOf('&comment=') > 0 ) {
        var begin = data.indexOf('&comment=') + 9;
        var end;
        if(data.indexOf('&commentLevel=') > 0) {
            end = data.indexOf('&commentLevel=');
        }else {
            end = data.length;
        }

        var commentText = data.substring(begin,end);

        if(commentText.length > 250 ) {
			alert(AJS.I18n.getText('field.limit.exceed.validationError.description','Comment','250'));
            return;
        }
    }

  var	scrollPosLeft = document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft;
	jQuery.ajax({
		url: AJS.$("form#zephyr-attach-file").attr("action")+"&inline=true&decorator=dialog",
		type : "post",
		contentType :"application/x-www-form-urlencoded",
		enctype: 'multipart/form-data',
		data : AJS.$("form#zephyr-attach-file").serialize(),
		dataType: "html",
		complete: function(xhr, status, response){
		   if(xhr.status != 200 ){
			   JE && JE.gadget.zephyrError(xhr);
		   } else {
			   //Recreate Div
			   showExecStatusSuccessMessage(AJS.I18n.getText('zephyr.je.execute.test.message.addedSuccess'));
			   ZEPHYR.Schedule.Execute.getAttachments(entityId,entityType);
			   AJS.$("#attach-file-cancel").click();
			   setTimeout(function(){
            document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
        }, 100);
		   }
		}
	});
}

var triggerInlineDialogStepAttachment = function(stepId) {
  var html = AJS.$("#step-attachment-content-container-" + stepId)[0].innerHTML;
	console.log(html);
	if(AJS.$("#inline-dialog-step-attachment-dialog-" + stepId).length > 0) {
		AJS.$("#inline-dialog-step-attachment-dialog-" + stepId).remove();
	}
	stepAttachmentInlineDialog = AJS.InlineDialog(AJS.$("#step-attachment-inlineDialog-" + stepId), "step-attachment-dialog-" + stepId,
		function(content, trigger, showPopup) {
				content.css({"overflow-y":"auto"}).html(html);
				showPopup();
				return false;
		},
		{
			width: 250,
			closeOnTriggerClick: true,
			persistent: false,
			noBind: true
		}
	);
  stepAttachmentInlineDialog.show();
  setTimeout( function() {
    AJS.$("#inline-dialog-step-attachment-dialog-" + stepId + " .aui-inline-dialog-contents.contents").css("max-height","none");
  }, 0);
}

var triggerInlineDialogExecutionAttachment = function(stepId) {
  var html = AJS.$("#attachment-content-container-" + stepId)[0].innerHTML;
	if(AJS.$("#inline-dialog-attachment-dialog-" + stepId).length > 0) {
		AJS.$("#inline-dialog-attachment-dialog-" + stepId).remove();
	}
	executionAttachmentInlineDialog = AJS.InlineDialog(AJS.$("#attachment-inlineDialog-" + stepId), "attachment-dialog-" + stepId,
		function(content, trigger, showPopup) {
				content.css({"overflow-y":"auto"}).html(html);
				showPopup();
				return false;
		},
		{
			width: 250,
			closeOnTriggerClick: true,
			persistent: false,
			noBind: true
		}
	);

	executionAttachmentInlineDialog.show();
  setTimeout( function() {
    AJS.$("#inline-dialog-attachment-dialog-" + stepId + " .aui-inline-dialog-contents.contents").css("max-height","none");
  }, 0);
}

ZEPHYR.Execution.testDetailGridExecutionPage = function(enableFilter) {
	// ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult){
	// 	ZEPHYR.Execution.testDetailsGrid.testStep.map(function(testStep){
	// 		if(stepResult.stepId === testStep.id){
	// 			Object.keys(testStep).map(function(index){
	// 				if(index !== 'id')
	// 					stepResult[index] = testStep[index];
	// 			})
	// 		}
	// 	});
	// });
	ZEPHYR.Execution.testDetailsGrid.stepResults.sort(function (a, b) {
		return a.orderId - b.orderId;
	});

	var config = {
		"head": [
			{
				key: 'orderId',
				displayName: '',
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				type: 'String',
				isSortable: false,
				isVisible: true,
			},
			{
				key: 'htmlStep',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.step'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				type: 'HTMLContent',
				isSortable: false,
				isVisible: true,
			},
			{
				key: 'htmlData',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.data'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				type: 'HTMLContent',
				isSortable: false,
				isVisible: true,
			},
			{
				key: 'htmlResult',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.result'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				type: 'HTMLContent',
				isSortable: false,
				isVisible: true,
			},
			{
				key: 'attachmentsMap',
				displayName: AJS.I18n.getText('zephyr.je.execute.test.config.stepAttachment'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'ATTACHMENTS',
				isVisible: true,
				baseUrl: contextPath,
				canAddAttachment: false
			},
			{
				key: 'status',
				displayName: AJS.I18n.getText('enav.status.label'),
				isFreeze: false,
				editable: true,
				isInlineEdit: true,
				type: 'SELECT_STATUS',
				imgUrl: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
				isSortable: false,
				sortOrder: '',
				executionSummaries: ZEPHYR.Execution.testDetailsGrid.executionStatus,
				isVisible: true,
				isGrid: true
			}, {
				key: 'comment',
				htmlValue: 'htmlComment',
				editKey: 'comment',
				displayName: AJS.I18n.getText('execute.test.comment.label'),
				isFreeze: false,
				editable: true,
				isInlineEdit: true,
				isSortable: false,
				type: 'LARGE_TEXT',
				isVisible: true,
			},
			{
				key: 'stepAttachmentMap',
				displayName: AJS.I18n.getText('zephyr.je.execute.test.config.attachment'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'ATTACHMENTS',
				isVisible: true,
				baseUrl: contextPath,
				canAddAttachment: true,
				fetchAttachment: true,
				fetchAttachmentCountKey: 'stepResultAttachmentCount',
				tooltipLabel: AJS.I18n.getText('zephyr.common.addAttachment.tooltip.label'),
				imgUrl: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/plus_button.svg'
			},
			{
				key: 'defects',
				displayName: AJS.I18n.getText('execute.test.defect.label'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'STEP_DEFECTS',
				isVisible: true,
				contextPath: contextPath,
				imgRemoveDefect: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/cross_button-defect.svg',
				imgurlAddDefect: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
				imgurlCreateIssue: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/plus_button.svg',
			}
		],
		"row": [],
		"maxFreezed": 0,
		"hasBulkActions": false,
		"rowSelection": false,
		"freezeToggle": false,
		"highlightSelectedRows": true,
		"firstColumnId": true,
		"gridComponentPage": 'testDetailExecution',
		"draggableRows": false,
		"refreshGrid": permissionGridRefresh,
		"columnchooser": {
			isEnabled: true,
			actionName: AJS.I18n.getText('enav.executions.view.column.title'),
			customEvent: 'columnchooser',
			columnChooserValues: [],
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
		},
		"contextPath": contextPath,
		"columnChooserUrl": contextPath + '/download/resources/com.thed.zephyr.je/images/icons/column-chooser-white_button.svg',
		"initialCount": getGridInitialCount(initialCount),
		"columnChooserHeading": AJS.I18n.getText('cycle.ColumnChooser.label'),
		"selectAtleaseoneOption": AJS.I18n.getText('zephyr.gridData.selectAtleastOneOption'),
		"submit": AJS.I18n.getText('zephyr.je.submit.form.Submit'),
		"cancel": AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		"noPermission": AJS.I18n.getText('zephyr.cycle.noPermissionOnTestAndIssue'),
		"placeholderText": AJS.I18n.getText('zephyr.customfield.textarea.placeholder'),
		"action": AJS.I18n.getText('cycle.actions.label'),
		"loading": AJS.I18n.getText('zephyr.gridData.loading'),
		"dataset": [{
            name: 'executionid',
            key: 'id'
        },
        {
            name: 'stepid',
            key: 'id'
        }],
        "noPermissionTestIssue": AJS.I18n.getText('cycle.noPermissionTestIssue.label')
	}

  allCustomFields.map(function(columnCell){
		var fieldValues = [];
		if(columnCell.customFieldOptionValues) {
			var fieldDefaultValues = columnCell.defaultValue.split(',');
			Object.keys(columnCell.customFieldOptionValues).forEach(function(key) {
				var value = false;
				var options = columnCell.customFieldOptionValues[key]
				if(fieldDefaultValues.indexOf(options) >= 0 ) {
					value = true;
				}
				var obj = {
					'content': options,
					'value': options,
					'selected' : value,
				}
				fieldValues.push(obj);
			});
		}
			var imgUrl = configureTriggerOptions(columnCell.fieldType)
			var obj = {
				"key": columnCell.id,
				"displayName" : columnCell.name,
				"isFreeze" : false,
				"editable" : true,
				"isInlineEdit" : true,
				"imgUrl" : contextPath + imgUrl || '',
				"type": (columnCell.fieldType == 'TEXT' || columnCell.fieldType == 'LARGE_TEXT') ? 'HTMLContent' : 'String',
				"options" : fieldValues ? fieldValues : [],
				"isSortable" : false,
				"isVisible" : true,
			}
      config.head.push(obj);
  })

	var columnChooserValues = [];
  config.head.map(function(column) {
		if((column.key !== "issueKey" && column.key !== "orderId")){
			var key = column.key;
			if(key === 'htmlStep') {
				key = 'testStep';
			} else if(key === 'htmlData') {
				key = 'testdata';
			} else if(key === 'htmlResult') {
				key = 'expectedResult';
			} else if(key === 'attachmentsMap') {
				key = 'stepAttachment';
			} else if(key === 'comment') {
				key = 'comment';
			} else if(key === 'stepAttachmentMap') {
				key = 'attachments';
			}
			var stepColumnVal = ZEPHYR.ExecutionCycle.executionStepColumns[key];
     	if(stepColumnVal && stepColumnVal.isVisible === 'true') {
       	column.isVisible = true;
     	} else if(column.key === 'issueKey') {
       	column.isVisible = true;
			} else {
        column.isVisible = false;
      }
      columnChooserValues[key] = {
        displayName: column.displayName,
        isVisible: column['isVisible'] ? 'true' : 'false'
    	};
     } else {
       column.isVisible = true;
     }
  })

  config.columnchooser.columnChooserValues = columnChooserValues;

	ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(row, index) {
      var obj = row;
      // if(index === 0) {
      //   selectedExecutionId = obj.id;
      // }
      var customFields = {};
		if (obj.customFieldValues && typeof obj.customFieldValues === 'string')
			customFields = JSON.parse(obj.customFieldValues);
		else if (obj.customFieldValues)
			customFields = obj.customFieldValues;

      Object.keys(customFields).map(function(field) {
				if(customFields[field].customFieldType === 'DATE' ) {
					obj[customFields[field].customFieldId] = convertDate({value : customFields[field].value, isDateTime : false});
				} else if (customFields[field].customFieldType === 'DATE_TIME') {
					obj[customFields[field].customFieldId] = convertDate({value : customFields[field].value, isDateTime : true});
				}else if(customFields[field].customFieldType === 'NUMBER') {
					obj[customFields[field].customFieldId] = customFields[field].value;
				} else if (customFields[field].customFieldType === 'TEXT' || customFields[field].customFieldType === 'LARGE_TEXT') {
					obj[customFields[field].customFieldId] = customFields[field].htmlValue;
				}
				else {
					obj[customFields[field].customFieldId] = customFields[field].value;
				}
      });

      if(obj.defects) {
        obj.defects.map(function(defect){
          defect['color'] = ZEPHYR.Execution.testDetailsGrid.executionStatus[obj.status].attributes.color;
        })
      }

      if((obj.id === stepLevelDefectId)) {
        if(showStepDefectsPopup) {
          obj['stepLevelDefect'] = defectResults;
		  obj['showDefectsResult'] = true;
			updatedGridData.rowData = {
			index: index,
			stepLevelDefect: defectResults,
			showDefectsResult: true
		}
        } else {
		  obj['showDefectsResult'] = false;
			updatedGridData.rowData = {
				index: index,
				showDefectsResult: false
			}
			stepLevelDefectId = '';
        }
      }
      config.row.push(obj);
  });
  gridRefresh = false;
  config.columnchooser.disabled = config.row.length === 0 ? true : false;
  if(config.columnchooser.disabled && enableFilter != 'enable'){
	  AJS.$('.test-step-filter-container').addClass('disabled');
  } else {
	  AJS.$('.test-step-filter-container').removeClass('disabled');
  }
	AJS.$(document).unbind('gridScrollEventCapture');
	AJS.$(document).bind('gridScrollEventCapture', gridScrollEventCapture);
	// if (onlyUpdateGridValue) {
	// 	AJS.$('#testDetailGridExecutionPage').attr('updatedconfig', JSON.stringify(updatedGridData));
	// 	updatedGridData = {};
	// 	onlyUpdateGridValue = false;
	// } else {
		vanillaGrid.init(document.getElementById('testDetailGridExecutionPage'), config);
		//AJS.$('#testDetailGridExecutionPage').attr('config', JSON.stringify(config));
	// }

	AJS.$('#testDetailGridExecutionPage').unbind('stepGridComponentActions');
	AJS.$('#testDetailGridExecutionPage').bind('stepGridComponentActions', testStepComponentActions);
	AJS.$('#testDetailGridExecutionPage').unbind('stepGridValueUpdated');
	AJS.$('#testDetailGridExecutionPage').bind('stepGridValueUpdated', testStepValueUpdated);
	AJS.$('#testDetailGridExecutionPage').unbind('fetchattachment');
  	AJS.$('#testDetailGridExecutionPage').bind('fetchattachment', fetchattachment);

  AJS.$('#testDetailGridExecutionPage').unbind('gridActions');
  AJS.$('#testDetailGridExecutionPage').bind('gridActions', testStepComponentActions);
  AJS.$('#testDetailGridExecutionPage').unbind('gridValueUpdated');
  AJS.$('#testDetailGridExecutionPage').bind('gridValueUpdated', testStepValueUpdated);
  /*AJS.$('#testDetailGridExecutionPage').unbind('fetchattachment');
  AJS.$('#testDetailGridExecutionPage').bind('fetchattachment', fetchattachment);*/
  // AJS.$('#testDetailGridExecutionPage').unbind('gridRowSelected');
  // AJS.$('#testDetailGridExecutionPage').bind('gridRowSelected', gridSelectedExecution);
  // AJS.$('#testDetailGridExecutionPage').unbind('gridBulkActions');
  // AJS.$('#testDetailGridExecutionPage').bind('gridBulkActions', gridBulkActions);
  AJS.$('#testDetailGridExecutionPage').unbind('fetchdefect');
  AJS.$('#testDetailGridExecutionPage').bind('fetchdefect', _fetchDefects);
  AJS.$('#testDetailGridExecutionPage').unbind('dialogueScroll');
  AJS.$('#testDetailGridExecutionPage').bind('dialogueScroll', dialogueScroll);
}

function fetchattachment(ev) {
	ZEPHYR.Schedule.Execute.getAttachments(ev.originalEvent.detail.id, "TESTSTEPRESULT", function (attachment) {
		var fetchAttachmentsCb = ev.originalEvent.detail.fetchAttachmentsCb;
		if(fetchAttachmentsCb && typeof fetchAttachmentsCb === 'function') {
			fetchAttachmentsCb(attachment, 'testDetailGridExecutionPage');
		}

		/*onlyUpdateGridValue = true;
		ZEPHYR.Execution.testDetailsGrid.stepResults.map(function (stepResult, index) {
			if (stepResult.id === ev.originalEvent.detail.id) {
				stepResult['stepAttachmentMap'] = attachment;
				stepResult['updateGrid'] = stepResult.hasOwnProperty('updateGrid') ? !stepResult['updateGrid'] : true;
				updatedGridData.rowData = {
						index: index,
						stepAttachmentMap: attachment || [],
						showAttachmentPopover: true,
					}
			}
		});
		ZEPHYR.Execution.testDetailGridExecutionPage();*/
	});
}

function _fetchDefects(ev) {
	var defects = ev.originalEvent.detail.defects || [];
	if(ev.originalEvent.detail.defectsPopup) {
		jQuery.ajax({
			url: contextPath + "/rest/api/1.0/issues/picker?currentIssueKey=&currentProjectId=&query=" + ev.originalEvent.detail.query,
			type : 'GET',
			contentType :"application/json",
			dataType: "json",
			success : function(response) {
				var issues = [];
				response.sections.forEach(function (obj) {
					obj.issues.forEach(function (issue) {
						var issueKey = issue.key,flag=true;
                        defects.forEach(function(def){
                        	if(def.key === issueKey){flag=false;}
						});
                        if (flag){issues.push(issue);}
                    });
                });
                response.sections[0].issues = issues;
                var defectsPopupList = issues.map(function(issue, index){
	                var defect = '<div class="defectList-wrapper" data-defectid="'+issue.key+'" data-index="'+index+'" data-createdefect="true">'
		                      +'<div class="defectImage">'
		                        +'<img src="'+contextPath+issue.img+'">'
		                      +'</div>'
		                      +'<div class="defectId" data-id="1">'+issue.key+'</div>'
		                    +'</div>';
		                    return defect;
                }).join('');
                var sectionsSub = response.sections[0].sub ? (' ' + (response.sections[0].sub || '')) : '';
                var defectsPopup = '<div class="inputResultWrapper">'
	                +'<div class="defectLabel">"'+response.sections[0].label + sectionsSub +'"</div>'
	               +' <div class="defectList-container stopBlur">'
	                  + defectsPopupList
	                  +'<div class="dummyHeightIE"></div>'
	                +'</div>'
	            +'</div>';
	            var el = AJS.$(".defectsSearchBox.activeElement");
	            var target = el[0];
	            AJS.$('div').remove(".inputResultWrapper");
	            AJS.$(target).find('.resultWrapper').append(defectsPopup);
				vanillaGrid.utils.stepDefects.calcPosition(AJS.$(target).parent('.row')[0], AJS.$(target).siblings('.defect-click')[0], '.defectsSearchBox', 0, true, 'testDetailGridExecutionPage');
				defectResults = response;
				stepLevelDefectId = ev.originalEvent.detail.id;
				showStepDefectsPopup = true;
				onlyUpdateGridValue = true;
				// ZEPHYR.Execution.testDetailGridExecutionPage();
			}
		});
	} else {
		showStepDefectsPopup = false;
		onlyUpdateGridValue = true;
    	defectResults = {};
    	stepLevelDefectId = ev.originalEvent.detail.id;
		// ZEPHYR.Execution.testDetailGridExecutionPage();
	}
}

var testStepValueUpdated = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  var data = ev.originalEvent.detail.updatedValue;
  var scrollPosLeft = 0
  if(ev.target.id === 'testDetailGridExecutionPage') {
  	scrollPosLeft = document.getElementById(ev.target.id).querySelector('#unfreezedGrid').scrollLeft;
  }
	if (ev.originalEvent.detail.isObject) {
		Object.keys(data).map(function (key) {
			data[key] = data[key].value;
		});
	}
	if(data.defectList) {
		data['updateDefectList'] = "true";
	}
	if (ev.originalEvent.detail.wasAlreadyAdded) {
		showErrorMessage(AJS.I18n.getText("defect.already.there"));
	} else {
		jQuery.ajax({
			url: getRestURL() + '/stepResult/' +  ev.originalEvent.detail.stepId,
			type : 'PUT',
			contentType :"application/json",
			data : JSON.stringify(data),
			dataType: "json",
			success : function(response) {
				updatedGridData = {};
				onlyUpdateGridValue = true;
				ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult, index) {
					if(stepResult.id === response.id){
						if(data.defectList) {
							stepResult.defects = response.defectList || [];
							updatedGridData.rowData = {
								index: index,
								defects: response.defectList || [],
							}
						showExecStatusSuccessMessage(AJS.I18n.getText('zephyr.je.execute.test.config.defectsUpdateSuccess'));
						} else if(data.status) {
							stepResult.status = response.status;
							updatedGridData.rowData = {
								index: index,
								status: response.status,
							}
							showExecStatusSuccessMessage(AJS.I18n.getText('zephyr.je.execute.test.config.statusUpdateSuccess'));
						} else {
							stepResult.htmlComment = response.htmlComment;
							stepResult.comment = response.comment;
							updatedGridData.rowData = {
								index: index,
								htmlComment: response.htmlComment,
								comment: response.comment,
							}
							showExecStatusSuccessMessage(AJS.I18n.getText('zephyr.je.execute.test.config.CommentUpdateSuccess'));
						}
					}
				});
				ZEPHYR.Execution.testDetailGridExecutionPage();
				if (Object.keys(ev.originalEvent.detail.updatedValue).indexOf('status') >= 0) {
					stepResultStatusUpdated(ZEPHYR.Execution.testDetailsGrid.stepResults, response);
				}
				if(ev.target.id === 'testDetailGridExecutionPage') {
					setTimeout(function(){
	            document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
	        }, 0)
				}
			},
			statusCode : {
				403 : function() {
					permissionGridRefresh = !permissionGridRefresh;
					ZEPHYR.Execution.testDetailGridExecutionPage();
				}
			}

		});
	}
}

var testStepComponentActions = function(ev) {
  ev.preventDefault();
  ev.stopPropagation();
  ev.stopImmediatePropagation();
  var scrollPosLeft = 0
  if(ev.target.id === 'testDetailGridExecutionPage') {
  	scrollPosLeft = document.getElementById(ev.target.id).querySelector('#unfreezedGrid').scrollLeft;
  }
  var actionDetail = ev.originalEvent.detail;
  if(actionDetail.actionName ==='columnChooser') {
      var data = {};
      var keys = Object.keys(ZEPHYR.ExecutionCycle.executionStepColumns);
      var columnKeysArr = [];
      actionDetail.columnDetails.map(function(column) {
  			if(column.key !== "issueKey" && column.key !== "orderId") {
  				var key = column.key;
  				if(key === 'htmlStep') {
  					key = 'testStep';
  				} else if(key === 'htmlData') {
  					key = 'testdata';
  				} else if(key === 'htmlResult') {
  					key = 'expectedResult';
  				} else if(key === 'attachmentsMap') {
  					key = 'stepAttachment';
  				} else if(key === 'htmlComment') {
  					key = 'comment';
  				} else if(key === 'stepAttachmentMap') {
  					key = 'attachments';
  				}
          		if (ZEPHYR.ExecutionCycle.executionStepColumns[key]) {
					if(column.isVisible) {
						ZEPHYR.ExecutionCycle.executionStepColumns[key].isVisible = "true";
					} else {
						ZEPHYR.ExecutionCycle.executionStepColumns[key].isVisible = "false";
					}
				} else{
					ZEPHYR.ExecutionCycle.executionStepColumns[key] = {
						displayName: column.displayName,
						isVisible: String(column.isVisible)
					};
				}
				columnKeysArr.push(key);
        }
      });

		if (keys.length != actionDetail.columnDetails.length) {
	      for (var i = 0; i < keys.length; i++) {
	      var match = false;
	      	for (var j = 0; j < columnKeysArr.length; j++) {
		   		if(columnKeysArr[j] == keys[i]){
		      		match = true;
		      		break;
		      	}
		    }

	      if (!match) {
	      		delete ZEPHYR.ExecutionCycle.executionStepColumns[keys[i]];
	      	}
	      }
	  }

      data['preferences'] = ZEPHYR.ExecutionCycle.executionStepColumns;

  		jQuery.ajax({
  			url: getRestURL() + "/preference/setexecutioncustomization",
  			type : "post",
  			contentType :"application/json",
  			data : JSON.stringify(data),
  			dataType: "json",
  			success : function(response) {
  				ZEPHYR.Execution.testDetailGridExecutionPage();
  			}
  		});
  }
  else if (actionDetail.customEvent === 'viewImage') {
   for (let counter = 0; counter < actionDetail.attachmentArray.length; counter += 1) {
     actionDetail.attachmentArray[counter].imageSizeInMb = parseFloat(actionDetail.attachmentArray[counter].fileSize / 1024 / 1024).toFixed(2);
     actionDetail.attachmentArray[counter].objectStringify = JSON.stringify(actionDetail.attachmentArray[counter]);
   }
   attachmentDuplicate = actionDetail.attachmentArray.filter(function (attachment) {
			return (/\.(gif|jpg|jpeg|tiff|png)$/i).test(attachment.fileName);
		});
   var fileId = actionDetail.selectedAttachment;
   for (let counter = 0; counter < attachmentDuplicate.length; counter += 1) {
     if (attachmentDuplicate[counter].fileId == fileId) {
       showImageCarousel(attachmentDuplicate[counter]);
       break;
     }
   }
 }
  else if(actionDetail.customEvent === 'addAttachment')
	{
		var url = contextPath + "/secure/AttachFileAction!default.jspa?id=" + ZEPHYR.Execution.CustomField.execution.issueId + "&entityId=" + actionDetail.rowDetail + "&entityType=TESTSTEPRESULT&projectId=";
		AJS.$('#teststepDetails .zephyr-file-dialog').attr('href', url);
		AJS.$('#teststepDetails .zephyr-file-dialog').trigger('click');
  }
  else if(actionDetail.customEvent === 'createDefect')
	{
   		 gridRefresh = true;
    	defectRowId = actionDetail.rowDetail;
		var url = contextPath + "/secure/CreateIssue!default.jspa";
    	AJS.$('a.gridAddDefect').attr('data-issue-type', issueType);
    	AJS.$('a.gridAddDefect').attr('id', 'zephyr_je_create_issue_link-stepresult-' + actionDetail.rowDetail);
		AJS.$('a.gridAddDefect').attr('href', url);
		AJS.$('a.gridAddDefect').trigger('click');
  } else if (actionDetail.customEvent === 'defectPicker') {
	  gridIssueStepId = actionDetail.rowDetail;
	  AJS.$('#gridIssuePicker').html(ZEPHYR.Templates.Execution.gridIssuePicker({ id: actionDetail.rowDetail, contextPath: contextPath}));
	  if (JIRA.Forms) {
		  var createForm = JIRA.Forms.createCreateIssueForm({
			  issueCreated: function (data) {
			  },
			  submit: function () {
				  console.log(submitted);
			  }
		  });
	  }
	  AJS.$('#gridIssuePicker').find('.aui-field-defectpickers').each(function () {

		  //Defect picker id is in form zephyrJEdefectskey-<entityType>-<entityId>
		  //Examples zephyrJEdefectskey-schedule-1, zephyrJEdefectskey-stepresult-10
		  //We will extract entityType and entityId from this id.

		  var defectPickerId = AJS.$(this)[0].id;
		  var source = ZEPHYR.Schedule.getSourceInfoForStatusExecution(defectPickerId);

		  var picker = new JIRA.IssuePicker({
			  element: AJS.$(this),
			  userEnteredOptionsMsg: AJS.I18n.getText('linkissue.enter.issue.key'),
			  uppercaseUserEnteredOnSelect: true
		  });

		  setExistingDefectMap(picker, source.sourceType, source.sourceId);

		  /* Doesnt work in JIRA 7.1 */
		  if (createForm) {
			  jQuery("#zephyr_je_create_issue_link-" + source.sourceTag).click(function (e) {
				  e.stopPropagation();
			  });
			  var dialog = createForm.asDialog({
				  trigger: jQuery("#zephyr_je_create_issue_link-" + source.sourceTag),
				  id: "create-issue-dialog-" + source.sourceTag
			  });
			  createForm.bind("dialogShown", function () {
				  //Do something after dialog is shown
			  });
			  dialog.bind("Dialog.hide", function () {
				  currentPicker.$field.focus();	//currentPicker.$field is Textarea
			  })
			  dialog.bind("Dialog.submit", function () {
				  currentPicker.$field.focus();	//currentPicker.$field is Textarea
			  })
		  }

		  /**
		   * Associating a anonymous function which freezes current picker by passing it to the function and executing it at the time of picker creation.
		   * When user will press the CreateIssue button link, corresponding method will be called and will set currentPicker to corresponding picker.
		   * Caveat: This is not a very scalable approach, as it will created one anonymous function for each Step row.
		   */
		  AJS.$("#zephyr_je_create_issue_link-" + source.sourceTag).bind("click", function (pickerRef) {
			  return function () {
				  currentPicker = pickerRef;
				  //console.log("Picker ID - " + pickerRef.model.$element[0].id);
			  }
		  }(picker));
		  if (source.sourceType == 'stepresult') {
			  picker.$container.addClass('medium-multiselect')
		  }

		  //picker.$field.addClass('zdefecttextarea')
		  //console.log("Picker ID - " + picker.model.$element[0].id );
	  });
	AJS.$('#gridIssuePicker div.issue-picker-popup').trigger('click');

	AJS.$('#gridIssuePicker textarea').on('focusin', function (ev) {
		var defectsList = [];
		var data = {};
		AJS.$('.zephyrJEdefectskey-stepresult option').each(function () {
			defectsList.push(this.value);
		});
		data['defectList'] = defectsList;
		if (data.defectList) {
			data['updateDefectList'] = "true";
		}

    if(addDefectAjaxDebounce) {
      clearTimeout(addDefectAjaxDebounce);
    }
		addDefectAjaxDebounce = setTimeout(function() {
      jQuery.ajax({
			url: getRestURL() + '/stepResult/' + gridIssueStepId,
			type: 'PUT',
			contentType: "application/json",
			data: JSON.stringify(data),
			dataType: "json",
			success: function (response) {
				ZEPHYR.Execution.testDetailsGrid.stepResults.map(function (stepResult) {
					if (stepResult.id === response.id) {
						if (data.defectList) {
							stepResult.defects = response.defectList || [];
						} else if (data.status) {
							stepResult.status = response.status;
						} else {
							stepResult.htmlComment = response.htmlComment;
							stepResult.comment = response.comment
						}
					}
				});
				ZEPHYR.Execution.testDetailGridExecutionPage();
        showExecStatusSuccessMessage('defectList Updated Successfully');
				setTimeout(function(){
            document.getElementById('testDetailGridExecutionPage').querySelector('#unfreezedGrid').scrollLeft = scrollPosLeft;
        }, 0)
			}
		});
  }, 1000);
	});

  }
	else if(actionDetail.customEvent === 'deleteAttachment')
	{
    	gridRefresh = true;
		var attachment = actionDetail.rowDetail.split(':');
    	deleteAttachmentStepId = attachment[0];
 	 	deleteConfirmationDialogExecution(attachment[1], attachment[2]);
  	}
}

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

function showImageCarousel(imageObject) {
	let flagObject = {
		previousFlag: true,
		nextFlag: true,
	};
	if (imageObject.fileId == attachmentDuplicate[0].fileId) {
		flagObject.previousFlag = false;
	}
	if (imageObject.fileId == attachmentDuplicate[attachmentDuplicate.length - 1].fileId) {
		flagObject.nextFlag = false;
	}
	var imageUrl = contextPath + "/plugins/servlet/schedule/viewAttachment?id=" + imageObject.fileId + "&name=" + imageObject.fileName;
	var imageCaroselHtml = ZEPHYR.Templates.Execution.caroselView({ image: imageUrl, altImage: imageObject, changeFlag: flagObject });
	AJS.$('#teststepDetails').append(imageCaroselHtml);

}

function closeCorosel() {
	if (AJS.$("#image-corosel-overlay")) {
		AJS.$("#image-corosel-overlay").remove();
	}
}

function previousImageCorosel(event) {
	for (let counter = 0; counter < attachmentDuplicate.length; counter += 1) {
		if (event.dataset.currentid == attachmentDuplicate[counter].fileId) {
			if (attachmentDuplicate[counter - 1]) {
				closeCorosel();
				showImageCarousel(attachmentDuplicate[counter - 1]);
			}
			break;
		}
	}
}

function nextImageCorosel(event) {
	for (let counter = 0; counter < attachmentDuplicate.length; counter += 1) {
		if (event.dataset.currentid == attachmentDuplicate[counter].fileId) {
			if (attachmentDuplicate[counter + 1]) {
				closeCorosel();
				showImageCarousel(attachmentDuplicate[counter +1]);
			}
			break;
		}
	}
}

function stepResultStatusUpdated(stepResult, stepResult) {
	/*If current schedule has same status with one of the steps, no need to do anything*/

	if (ZEPHYR.Execution.currentSchedule.statusObj.currentExecutionStatus.name == ZEPHYR.Execution.testDetailsGrid.executionStatus[stepResult.status].attributes.name) {
		return;
	}
	/*Schedule has diff status than step, now lets make sure all steps have same status*/
	// var noOfStepsWithNewStatus = 0;
	// if(teststepresults.length)
	// 	stepResult = teststepresults;
	// stepResult.map(function (stepResult) {
	// 	if (stepResult.status == stepResult.status) {
	// 		noOfStepsWithNewStatus++
	// 	}
	// })

	/*If all steps are of same status*/
	// if (noOfStepsWithNewStatus == stepResult.length) {
	// 	executeScheduleConfirmationDialog(status);
	// }

	if (stepResult.updateStatus == "true") {
		executeScheduleConfirmationDialog(stepResult.status);
	}
}

/**
 * Pops up a dialog to ask user if they want to execute current schedule with the same status.
 */
function executeScheduleConfirmationDialog(stepResultStatus) {
	/** If more than one element found, lets pick the first one */
	var options = [];
	var statusMap = ZEPHYR.Execution.currentSchedule.statusObj.executionStatuses;
	var stepResultStatusText = ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.get(stepResultStatus).get("name");
	var statusColor = ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.get(stepResultStatus).get("color");
	var count=0;
	Object.keys(statusMap).map(function (index) {
		if ((statusMap[index].name) == stepResultStatusText) {
			stepResultStatus = statusMap[index].id;
			count = 1;
		}
	});
	if(count == 0) {
		stepResultStatus = '1';
	}

	Object.keys(statusMap).map(function (index) {
		var obj = {
			"content": statusMap[index].name,
			"value":  statusMap[index].id,
			"selected": (statusMap[index].id) == stepResultStatus ? true : false,
			"isStatus": true,
			"color": statusMap[index].color,
			"readOnly": false
		}
		options.push(obj);
	});
	selectedStatusId = stepResultStatus;
	options = JSON.stringify(options);
	options = options.replace(/'/g, '&#39;');

	// options = '[{"content":"PASS","value":1,"selected":false,"isStatus":true,"color":"#75B000","readOnly":false},{"content":"FAIL","value":2,"selected":false,"isStatus":true,"color":"#CC3300","readOnly":false},{"content":"WIP","value":3,"selected":false,"isStatus":true,"color":"#F2B000","readOnly":false},{"content":"BLOCKED","value":4,"selected":true,"isStatus":true,"color":"#6693B0","readOnly":false},{"content":"NEWSTATUS","value":5,"selected":false,"isStatus":true,"color":"#99ccff","readOnly":false},{"content":"NEWSTATUS2","value":6,"selected":false,"isStatus":true,"color":"#9933ff","readOnly":false},{"content":"!@#$ %^ ","value":7,"selected":false,"isStatus":true,"color":"#666699","readOnly":false},{"content":"DXCFV < CVB > DFG12345","value":9,"selected":false,"isStatus":true,"color":"#666699","readOnly":false},{"content":"UNEXECUTEDEDITED","value":-1,"selected":false,"isStatus":true,"color":"#A0A0A0","readOnly":false}]';
	dialog = new JIRA.FormDialog({
		id: "schedule-confirmation-dialog",
		content: function (callback) {
			/*ERROR: stepResultStatus may not be a valid execution Status. In that case, following call will fail */
			var stepStatus = "<font color='" + statusColor + "'> " + stepResultStatusText + "</font>"
			var innerHtmlStr = ZEPHYR.Templates.StepExecution.autoExecuteConfirmationDialog({ entityName: "Testcase", stepStatus: stepStatus, options: options, imgUrl: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'});
			callback(innerHtmlStr);
			AJS.$('#executeTestStatus').unbind('submitvalue');
			AJS.$('#executeTestStatus').bind('submitvalue', _submitValue);
			/*Lets change the status of the dropdown to match the value - this is done after adding to DOM - IE issue*/
		},

		submitHandler: function (e) {
			ZEPHYR.Schedule.performExecution(ZEPHYR.Schedule.Execute.currentSchedule.get('id'),
				selectedStatusId, function () {
					dialog.hide();
					/* ExecNavigator listen to this event for quick execution
					 * Need to remove this once we internalize statusUpdate inside the backbone views
					 */
					// if (typeof globalDispatcher != 'undefined') {
					// 	globalDispatcher.trigger('scheduleUpdated');
					// } else {
					// 	jQuery(document).trigger("ScheduleUpdated");
					// }
					// var allStatusList = {}
					// ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.toJSON().forEach(function (status) {
					// 	allStatusList[status.id] = status;
					// })
					// AJS.$(document).trigger("triggerExecutionDetails", [ZEPHYR.Schedule.Execute.currentSchedule.get('id'), false, allStatusList]);
					var parentContainer = AJS.$('#executionStatus-value-schedule-' + ZEPHYR.Schedule.Execute.currentSchedule.get('id')+ ' .execution_status_wrapper');
					parentContainer.find('.status-readMode').css("background", ZEPHYR.Execution.currentSchedule.statusObj.currentExecutionStatus.color).html(ZEPHYR.Execution.currentSchedule.statusObj.currentExecutionStatus.name);

					parentContainer.find('.execution-field-dropDown-container li').each(function () {
						if (this.dataset.value === ZEPHYR.Execution.currentSchedule.statusObj.currentExecutionStatus.id) {
							AJS.$(this).addClass('selected');
						} else {
							AJS.$(this).removeClass('selected');
						}
					})
				}) //From execution-field-view.js

			e.preventDefault();	//To avoid dialog submit propogation
		}
	});
	dialog.show();
}

function _submitValue(ev) {
	selectedStatusId = Array.isArray(ev.originalEvent.detail.value) ? ev.originalEvent.detail.value.join(',') : ev.originalEvent.detail.value;
}

function gridScrollEventCapture(event) {

	AJS.$('#detail-panel-wrapper').scroll(function (event) {
		var scrollableElement = event.currentTarget.getBoundingClientRect();
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
			var returnValue = true;
			var triggerElement = dialogue.target.getBoundingClientRect();
			var viewportHeight = window.innerHeight;
			var height = dialogue.dialogueElement.clientHeight;
			var topHeight = triggerElement.top;

			if (scrollableElement.top > triggerElement.bottom || scrollableElement.bottom < triggerElement.top) {
				ZEPHYR.GRID.stopGridFocus = true;
				if (dialogue.isDropDown) {
					dialogue.dialogueElement.classList.add('close');
					dialogue.dialogueElement.classList.remove('activeElement');
					if (dialogue.isInput) {
						var input = dialogue.dialogueElement.getElementsByTagName('input');
						if (input.length) {
							for (var i = 0; i < input.length; i++) {
								input[i].checked = dialogue.checkedValues[input[i].id];
							}
						}
					} else if (dialogue.isSelect) {
						var select = dialogue.dialogueElement.getElementsByTagName('option');
						if (select.length) {
							for (var i = 0; i < select.length; i++) {
								select[i].selected = dialogue.checkedValues[i];
							}
						}
					}
				} else if (dialogue.isAttachment) {
					dialogue.dialogueElement.classList.add('close');
					dialogue.dialogueElement.classList.remove('activeElement');
				} else if (dialogue.isDefects) {
					dialogue.dialogueElement.classList.add('close');
					dialogue.dialogueElement.classList.remove('activeElement');
				} else {
					dialogue.dialogueElement.classList.add('close');
					dialogue.dialogueElement.classList.remove('open');
				}
				returnValue = false;

			}

			if (viewportHeight > topHeight + height) {
				dialogue.dialogueElement.style.top = topHeight + triggerElement.height + 5 + 'px';
			}
			else {
				dialogue.dialogueElement.style.top = topHeight + triggerElement.height - height - triggerElement.height + 'px';
			}
			return returnValue;
		});
	});

	AJS.$('#testDetailGridExecutionPage .table-container-wrapper, #cycleSummaryGrid .table-container-wrapper').scroll(function (event) {
		var scrollableElement = event.currentTarget.getBoundingClientRect();
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
			var returnValue = true;
			if (!dialogue.onlyWindowScroll) {
				var triggerElement = dialogue.target.getBoundingClientRect();
				var viewportHeight = window.innerHeight;
				var height = dialogue.dialogueElement.clientHeight;
				var topHeight = triggerElement.top + triggerElement.height + 2;
				if (scrollableElement.top > triggerElement.bottom || scrollableElement.bottom < triggerElement.top) {
					ZEPHYR.GRID.stopGridFocus = true;
					if (dialogue.isDropDown) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
						if (dialogue.isInput) {
							var input = dialogue.dialogueElement.getElementsByTagName('input');
							if (input.length) {
								for (var i = 0; i < input.length; i++) {
									input[i].checked = dialogue.checkedValues[input[i].id];
								}
							}
						} else if (dialogue.isSelect) {
							var select = dialogue.dialogueElement.getElementsByTagName('option');
							if (select.length) {
								for (var i = 0; i < select.length; i++) {
									select[i].selected = dialogue.checkedValues[i];
								}
							}
						}
					} else if (dialogue.isAttachment) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
					} else if (dialogue.isDefects) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
					}
					returnValue = false;

				}
				if (viewportHeight > topHeight + height) {
					dialogue.dialogueElement.style.top = topHeight + 'px';
				}
				else {
					dialogue.dialogueElement.style.top = topHeight - triggerElement.height - 5 - height + 'px';
				}
			}
			return returnValue;

		});
	});


	AJS.$('#testDetailGridExecutionPage #unfreezedGrid, #cycleSummaryGrid #unfreezedGrid').scroll(function (event) {
		var scrollableElement = event.currentTarget.getBoundingClientRect();
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
			var returnValue = true;

			if (!dialogue.onlyWindowScroll) {
				var triggerElement = dialogue.target.getBoundingClientRect();
				var viewportWidth = window.innerWidth;
				var width = dialogue.dialogueElement.clientWidth;
				var leftWidth = triggerElement.left + triggerElement.width + 2;
				var parentWidth = dialogue.target.parentElement.clientWidth;
				if (scrollableElement.right < triggerElement.left || scrollableElement.left > triggerElement.right) {
					ZEPHYR.GRID.stopGridFocus = true;
					if (dialogue.isDropDown) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
						if (dialogue.isInput) {
							var input = dialogue.dialogueElement.getElementsByTagName('input');
							if (input.length) {
								for (var i = 0; i < input.length; i++) {
									input[i].checked = dialogue.checkedValues[input[i].id];
								}
							}
						} else if (dialogue.isSelect) {
							var select = dialogue.dialogueElement.getElementsByTagName('option');
							if (select.length) {
								for (var i = 0; i < select.length; i++) {
									select[i].selected = dialogue.checkedValues[i];
								}
							}
						}
					} else if (dialogue.isAttachment) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
					} else if (dialogue.isDefects) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('activeElement');
					}
					returnValue = false;

				}
				if (dialogue.isDropDown) {
					dialogue.dialogueElement.style.left = triggerElement.left - parentWidth + triggerElement.width + 'px';
				} else if (dialogue.isAttachment) {
					if (viewportWidth > leftWidth + width) {
						dialogue.dialogueElement.style.left = triggerElement.left + 'px';
					}
					else {
						dialogue.dialogueElement.style.left = leftWidth - 5 - width + 'px';
					}
				} else if (dialogue.isDefects) {
					dialogue.dialogueElement.style.left = triggerElement.left - parentWidth + triggerElement.width - dialogue.leftAdjust + 'px';
				}
			}
			return returnValue;
		});
	});
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

AJS.$(window).scroll(function (event) {
	ZEPHYR.GRID.scrollableDialogue.map(function (dialogue) {
		var triggerElement = dialogue.target.getBoundingClientRect();
		var viewportHeight = window.innerHeight;
		var height = dialogue.dialogueElement.clientHeight;
		var topHeight = triggerElement.top;
		if (viewportHeight > topHeight + height) {
			dialogue.dialogueElement.style.top = topHeight + triggerElement.height + 5 + 'px';
		}
		else {
			dialogue.dialogueElement.style.top = topHeight + triggerElement.height - height - triggerElement.height + 'px';
		}
	});
});