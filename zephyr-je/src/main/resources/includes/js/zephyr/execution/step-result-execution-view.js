ZEPHYR.Issue.TestStep = Backbone.Model.extend();
ZEPHYR.Issue.TestStepCollection = Backbone.Collection.extend({
    model:ZEPHYR.Issue.TestStep,
    url: function(){
    	return getRestURL() + "/teststep/" + ZEPHYR.Schedule.Execute.data.issueId
    },
    parse: function(resp, xhr){
    	return resp
    }
});

ZEPHYR.Schedule.Execution.TeststepResultRowView = Backbone.View.extend({
    tagName:"tr",

    events:{
    	"mousedown .current-comment-status-dd-stepresult"			:		"enableStepCommentArea",
    	"mousedown .test-status-execution-wrapper"					:		"enableExecutionStatusDropDown",
    	"mousedown [id^='readonly-defect-values-stepresult-']"		:		"enableStepDefectPicker",
    	"focusout [id^='comment-status-stepresult-']"				:		"disableStepCommentArea",
    	"mousedown #comment-edit-field-select-cancel"				:		"cancelStepCommentArea",
    	"focusout [id^='execution-field-select-']"					:		"disableExecutionStatusDropDown",
    	"mousedown [id^='execution_cancel-schedule-']"				:		"cancelExecutionStatusDropDown",
    	"mousedown [id^=current-defectpicker-status-dd-stepresult-] a": 	"preventEditDefect"
    },

    render:function (eventName) {
    	var selectedStatusId = this.model.get('status') || -1;
    	var selectedStatus = ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.get(selectedStatusId).toJSON()
    	AJS.$(this.el).html(ZEPHYR.Templates.StepExecution.stepResultRow({teststep:this.options.teststep.toJSON(), teststepResult:this.model.toJSON(),
    						selectedStepExecStatus:selectedStatus, stepExecutionStatuses:ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.toJSON(), baseUrl:contextPath,
                            stepColumns: ZEPHYR.ExecutionCycle && ZEPHYR.ExecutionCycle.executionStepColumns, customFieldsOrder: ZEPHYR.ExecutionCycle.customFieldsOrder}));
    	AJS.$(this.el).attr('id', 'step-row-for-result-id-' + this.model.get('id')).attr("style", "height:4em")
        return this;
    },

    enableStepCommentArea : function(event) {
    	//Fix to handle click events on scrollbars
        //ZFJ-2143 Added AJS. to access the library function.
		var offX  = event.offsetX || event.clientX - AJS.$(event.target).offset().left;
        if(offX > event.target.clientWidth){
            return false;
        }
    	event.preventDefault();
    	var id =  this.model.get('id');
    	this.$el.find("#comment-current-status-stepresult-" + id).hide();
    	_editDiv = this.$el.find("#comment-edit-field-select-stepresult-" + id)
    	_editDiv.show();
    	//_editDiv.bind("focusout", this.disableStepCommentArea);
    	//Let's set the focus on comment text area.
    	AJS.$("#comment-status-stepresult-" + id).focus()
    	//Place the label 5px below textArea - need to figure out a better way of placing label
    	//this.$el.find('#stepresult-comment-counter-'+id).css('top', this.$el.find('textarea').height() + 5)
    	_editDiv.bind("keyup", function(event){
    		var len = ZEPHYR.charCounter(event, '#comment-edit-field-select-stepresult-'+id + " textarea", 750, '#stepresult-comment-counter-'+id);
    		if(len < 0){
    			var clearError = function(){AJS.$("#comment-edit-field-select-stepresult-" + id + " .aui-message-bar").empty()}
    			clearError();
    			AJS.messages.warning("#comment-edit-field-select-stepresult-" + id + " .aui-message-bar", {body: AJS.I18n.getText('zephyr.je.maxChar.tooltip', 750),
    																									closeable: true});
    			setTimeout(clearError, 2500);
    		}
		});
    	ZEPHYR.charCounter(null, '#comment-edit-field-select-stepresult-'+id + " textarea", 750, '#stepresult-comment-counter-'+id);
    	event.preventDefault();
    },

    disableStepCommentArea : function (event) {
    	event.preventDefault();
    	var instance = this;
    	var stepId = this.model.get('id');

    	var comment = AJS.$("#comment-status-stepresult-" + stepId).val();
    	instance.$el.find("span.loading").show();
		var editableCommentControl = AJS.$("#comment-edit-field-select-stepresult-"+stepId);

    	//AJS.$("#comment-edit-field-select-stepresult-" + source.sourceId).unbind("focusout");
    	var req = this.model.save({'comment':comment}, {success: function(model, response, options){
    			instance.$el.find("span.loading").hide();
    			if(!model.get('htmlComment')) AJS.$("#current-comment-status-dd-stepresult-" + stepId + " div").html('<em style="color: #979797;" class="floatleft">Enter Comment</em>');
    			else AJS.$("#current-comment-status-dd-stepresult-" + stepId + " div").html(model.get('htmlComment'));
    			instance.$el.find("#comment-edit-field-select-stepresult-" + stepId).hide();
    			instance.$el.find("#comment-current-status-stepresult-" + stepId).show();
				console.log("Step Execution Comments updated successfully.");
			},
			error : function(model, xhr, options) {
				instance.$el.find("span.loading").show();
				buildExecutionError(xhr, "#comment-edit-field-select-stepresult-" + stepId + " .aui-message-bar");
			}
		});
		// Callback to handle status 403's error condition
		req.error(function(jHR) {
			if(jHR.status == 403)
				instance.hideStepCommentOnSaveFailure(editableCommentControl,AJS.$("#comment-current-status-stepresult-" + stepId));
		});
		editableCommentControl.find('span.loading').show();
    },

    cancelStepCommentArea: function(event) {
    	event.preventDefault();
    	var stepId = this.model.get('id');

    	this.$el.find("span.loading").hide();
    	this.$el.find("#comment-status-stepresult-" + stepId).val(AJS.$.trim(this.model.get('comment')));
		this.$el.find("#comment-edit-field-select-stepresult-" + stepId).hide();
		this.$el.find("#comment-current-status-stepresult-" + stepId).show();
		console.log("Step Execution Comments cancelled.");
    },

	hideStepCommentOnSaveFailure: function(editableCommentControl,comment) {
		comment.val(AJS.$.trim(this.model.get('comment')));
		editableCommentControl.hide();
		editableCommentControl.find('span.loading').hide();
		comment.show();
	},

    enableExecutionStatusDropDown : function(event) {
    	event.preventDefault();
    	var stepId = this.model.get('id');

    	AJS.$("#execution-field-current-status-stepresult-" + stepId).hide();
    	AJS.$("#execution-field-select-stepresult-"+ stepId).show();
    	//Let's set the focus on dropdown element.
    	AJS.$("#exec_status-stepresult-"+ stepId).focus();
    },

    disableExecutionStatusDropDown : function(event) {
    	var instance = this;
    	var stepId = this.model.get('id');
    	var EXEC_STATUS_TEXT_MAX_LENGTH=12;
    	var selectedOption = instance.$el.find("#exec_status-stepresult-" + stepId +" :selected")
    	var newStatus = selectedOption.text();
    	var newStatusId = selectedOption.val();
    	var updateVisibility = function(){
    		instance.$el.find("#execution-field-select-stepresult-" + stepId).hide();
			instance.$el.find("#execution-field-current-status-stepresult-" + stepId).show();
    	}
    	if(this.model.get('status') == newStatusId){
    		updateVisibility();
    		return;
    	}
    	ZEPHYR.Loading.showLoadingIndicator();
    	var statusDescription = selectedOption.attr("title");
    	var color = selectedOption.attr("rel");

    	//alert Text if greater than allowed
    	var title = newStatus + (statusDescription ? (': ' + statusDescription) : '');
  	  ZEPHYR.Schedule.stripTextIfMoreThanAllowed("#current-execution-status-dd-stepresult-" + stepId, newStatus, EXEC_STATUS_TEXT_MAX_LENGTH, title);
    	//Execute given test step or schedule to change the status for the same.
    	var req = this.model.save({status:newStatusId}, {
    		wait: true,	/*This will make sure that Model change event only gets fired on success */
    		success : function(response) {
    			var execution = response[stepId];
    			updateVisibility();
    			var statusLozenge = instance.$el.find("#current-execution-status-dd-stepresult-" + stepId)
    			statusLozenge.css("background-color", color);
    			statusLozenge.html(newStatus);
    			ZEPHYR.Loading.hideLoadingIndicator();
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
    			ZEPHYR.Loading.hideLoadingIndicator();
    		}
    	})
    	// Callback to handle status 403's error condition
    	req.error(function(jHR) {
    		if(jHR.status == 403) {
        		instance.$el.find("#execution-field-select-stepresult-" + stepId).hide();
    			instance.$el.find("#execution-field-current-status-stepresult-" + stepId).show();
    		}
    	});
    },

    // On click of 'Cancel', revert the changes.
    cancelExecutionStatusDropDown: function(ev) {
    	ev.preventDefault();
    	var currentTarget 			= ev.currentTarget || ev.srcElement;
    	var scheduleId				= AJS.$(currentTarget).attr('id').split('-')[2];
    	var readOnlyStatusDiv 		= AJS.$('#current-execution-status-dd-stepresult-' + scheduleId);
    	var currentExecutionStatus 	= AJS.$("#exec_status-stepresult-" + scheduleId + " option:contains(" + readOnlyStatusDiv.text() + ")").val();

    	AJS.$('#execution-field-current-status-stepresult-' + scheduleId).show();
    	AJS.$('#execution-field-select-stepresult-' + scheduleId).hide();
    	AJS.$("#exec_status-stepresult-" + scheduleId).val(currentExecutionStatus);
    },

    preventEditDefect: function(ev) {
    	ev.stopImmediatePropagation();
    },

    enableStepDefectPicker: function (event) {
    	var instance 	= this;
    	var stepId 		= this.model.get('id');
    	var readOnlyDiv = AJS.$("#readonly-defect-values-stepresult-" + stepId)
    	var editableDiv = AJS.$("#editable-defect-values-stepresult-"+ stepId);

    	readOnlyDiv.hide();
    	editableDiv.show();
    	editableDiv.find('.issue-picker-popup').attr('tabindex',1)	//Workaround for Chrome limitation where links are not tab enabled

    	var blurTimer
    	editableDiv.parent().delegate(focusables, 'blur', function(event){
    		if(blurTimer) clearTimeout(blurTimer);
    		blurTimer = setTimeout(function(){instance.disableStepDefectPicker(event)}, 50);
    	});

    	var ta = AJS.$("#zephyrJEdefectskey-stepresult-"+ stepId + "-textarea");
    	//Let's set the focus on defect picker text area.

    	/*Setting height, for some reason, its not working in steps */
    	ta.css('height', Math.max(AJS.$(editableDiv).find('div.representation').height(), ta.height()) + (2 + 6)); //1:border, 3:padding-top/padding:bottom
    	editableDiv.focus();
    	ta.focus();

    	event.preventDefault();
    },

    disableStepDefectPicker: function (event) {
    	var instance = this;
    	var stepId = this.model.get('id');
    	var source = {sourceId:stepId, sourceType:'stepresult'}

    	var readOnlyDiv = AJS.$("#readonly-defect-values-stepresult-" + stepId);
    	var editableDiv = AJS.$("#editable-defect-values-stepresult-"+ stepId);

    	/*If focus is anywhere within the div, ignore the event and return*/
    	if(hasFocus(editableDiv) && !this.updateDefect){
			this.updateDefect = false;
    		return ;
    	}

    	//We are using JIRA's default Defect Tracker which adds "-textarea" to newly created text area where user enters defect ids.
    	//Hence in this, sourceType will have step result ID information.
    	editableDiv.hide();
    	editableDiv.parent().undelegate(focusables, 'blur')

    	readOnlyDiv.show();

    	var value = [];
    	AJS.$('#zephyrJEdefectskey-stepresult-'+ stepId + '-multi-select ul.items li.item-row').each(function() {
    		var val = AJS.$(this).find("button.value-item span.value-text");
    		value.push(val.text());
    	});

    	jQuery.ajax({
    		url: getRestURL() + "/stepResult/"+ stepId,
    		type: "PUT",
    		contentType: "application/json",
			dataType: "json",
    		data:JSON.stringify({
    			'id': stepId,
    			'issueId': ZEPHYR.Schedule.Execute.data.issueId,
    			'executionId':ZEPHYR.Schedule.Execute.currentSchedule.get('id'),
    			'status':   AJS.$("#exec_status-stepresult-"+ stepId).val(),
    			'defectList' : value,
    			'updateDefectList': 'true'
    		}),
    		success: function(response){
    			var stepExecution = response;

                AJS.$(document).trigger( "updateExecutionModelWithSteps", [response] );

    			//Now go through value array which has all entries that user has entered.
    			//First we will remove all right entries that we have received from this AJAX call.
    			//That will give us all invalid entries which we will remove from text area in subsequent call.
    			var updatedDefectsString = [];
    			if(stepExecution.defectList){
    				AJS.$.each(stepExecution.defectList, function(i, issue) {
    					updatedDefectsString.push(ZEPHYR.Templates.StepExecution.defectLink({defect:issue}));
    					if(AJS.$.inArray(issue.key, value) > -1) {
    						value = AJS.$.removeFromArray(issue.key, value);
    					}
    				});
    			}

//    			if(updatedDefectsString.length > 0){
//    				updatedDefectsString = updatedDefectsString.substring(0,updatedDefectsString.length-2);
//    			}

    			//iterate through the array and empty out the Invalid entries from text area.
    			for(var issueKey in value) {
    				AJS.$("#zephyrJEdefectskey-stepresult-"+ stepId +"-multi-select ul.items li.item-row").each(function() {
    					var val = AJS.$(this).find("button.value-item span.value-text");
    					if(value[issueKey] == val.text()) {
    						AJS.$(this).remove();
    						var paddingL = AJS.$("#zephyrJEdefectskey-stepresult-"+ stepId +"-textarea").css("padding-left");
    						paddingL = paddingL.substring(0,paddingL.length - 2);
    						AJS.$("#zephyrJEdefectskey-stepresult-"+ stepId +"-textarea").css("padding-left",paddingL-40);
    					}
    				});
    			}

    			if(updatedDefectsString.length == 0) AJS.$("#current-defectpicker-status-dd-stepresult-"+ stepId).html('<em style="color: #979797;" class="floatleft">Enter Defects</em>');
    			else AJS.$("#current-defectpicker-status-dd-stepresult-"+ stepId).html(updatedDefectsString.join());
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
    		}
    	});
    }
});

ZEPHYR.Schedule.Execution.TeststepResultView = Backbone.View.extend({
    tagName:'tbody',

    initialize:function () {
        this.model.bind("reset", this.render, this);
        /*This event gets bubbled up from stepRow model on row model update - USED for AUTO-EXECUTE*/
        this.model.bind("change:status", this.stepResultStatusUpdated, this);
        AJS.$('#zfj-permission-message-bar-step-result-execution-detail').addClass('active');
    },
    render:function (eventName) {
    	// To retain order of steps, as defined in view issue page,
    	// iterating over stepCollection instead stepResultCollection.
        _.each(ZEPHYR.Schedule.Execution.data.teststeps.models, function (step) {
        	var teststepResult = this.model.where({stepId:step.get('id')})[0];
        	if(teststepResult)
        	AJS.$(this.el).append(new ZEPHYR.Schedule.Execution.TeststepResultRowView({model:teststepResult, teststep:step}).render().el);
        }, this);
        return this;
    },

    stepResultStatusUpdated : function(eventName){
    	var firstStepStatus = _.first(this.model.models).get("status");
    	/*If current schedule has same status with one of the steps, no need to do anything*/
    	if(ZEPHYR.Schedule.Execute.currentSchedule.get('executionStatus') == firstStepStatus){
    		return;
    	}
    	/*Schedule has diff status than step, now lets make sure all steps have same status*/
    	var noOfStepsWithNewStatus = 0;
    	var cntByStatusObject = _.each(this.model.models, function(stepResult){
    		if(stepResult.get("status") == firstStepStatus){
    			noOfStepsWithNewStatus++
    		}
		})

		/*If all steps are of same status*/
    	if(noOfStepsWithNewStatus == this.model.models.length){
    		this.executeScheduleConfirmationDialog(firstStepStatus);
    	}
    },

    /**
     * Pops up a dialog to ask user if they want to execute current schedule with the same status.
     */
    executeScheduleConfirmationDialog : function (stepResultStatus) {
        var instance = this;
        var stepResultStatusText = ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.get(stepResultStatus).get("name");
        /*Search by Option value(id) and text match */
        var scheduleOptionElement = AJS.$("select[id^='exec_status-schedule'] option[value='"+ stepResultStatus +"']'");
        var isSameStatus = scheduleOptionElement.text().indexOf(stepResultStatusText);
        // Commenting the below line as jquery option:contains function with special character will throw error.
        // var scheduleOptionElement = AJS.$("select[id^='exec_status-schedule'] option[value=='"+ stepResultStatus +"']:contains('"+stepResultStatusText+"')");
        /*If no option is found by both value(id) and text match, we will try by just text match */
        if(isSameStatus == -1){
        	// jquery option:contains function throws error so using text.
        	// scheduleOptionElement = AJS.$("select[id^='exec_status-schedule'] option:contains('"+stepResultStatusText+"')");
        	scheduleOptionElement = AJS.$("select[id^='exec_status-schedule'] option").filter(function(index) {
        	    return stepResultStatusText == AJS.$(this).text();
        	});
        }
        /** If more than one element found, lets pick the first one */
        if(isSameStatus != -1){
        	scheduleOptionElement = AJS.$(scheduleOptionElement[0]);
        }
        var preselectExecutionStatus;
        if(scheduleOptionElement.length > 0){
        	preselectExecutionStatus = scheduleOptionElement.attr('value');
        }
        dialog = new JIRA.FormDialog({
            id: "schedule-confirmation-dialog",
            content: function (callback) {
            	/*ERROR: stepResultStatus may not be a valid execution Status. In that case, following call will fail */
            	var stepStatus = "<font color='"+ ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.get(stepResultStatus).get("color") +"'> " + stepResultStatusText + "</font>"
            	var statuses = AJS.$("<div>").append(AJS.$("select[id^='exec_status-schedule']").clone())
            	statuses.find('select').attr('id', "popup_status")
            	statuses.find("select option:selected").removeAttr('selected');
            	statuses.find('select').removeAttr('disabled');
            	var innerHtmlStr = ZEPHYR.Templates.StepExecution.autoExecuteConfirmationDialog({entityName:"Testcase", stepStatus:stepStatus, statuses:statuses.html()});
                callback(innerHtmlStr);
                /*Lets change the status of the dropdown to match the value - this is done after adding to DOM - IE issue*/
                if(preselectExecutionStatus)
            		AJS.$("#popup_status").val(preselectExecutionStatus)
            },

            submitHandler: function (e) {
            	ZEPHYR.Schedule.performExecution(ZEPHYR.Schedule.Execute.currentSchedule.get('id'),
            			AJS.$("select#popup_status option:selected").attr("value"), function(){
            				dialog.hide();
            				/* ExecNavigator listen to this event for quick execution
            				 * Need to remove this once we internalize statusUpdate inside the backbone views
            				 */
            				if(typeof globalDispatcher != 'undefined'){
            					globalDispatcher.trigger('scheduleUpdated');
            				} else {
            					jQuery(document).trigger("ScheduleUpdated");
            				}
                            var allStatusList = {}
                            ZEPHYR.Schedule.Execution.data.stepExecutionStatuses.toJSON().forEach(function(status){
                                allStatusList[status.id] = status;
                            })
                            var paginationData = window.executionDetailView.model.paginationData ? window.executionDetailView.model.paginationData : null;
                            AJS.$(document).trigger( "triggerExecutionDetails", [ZEPHYR.Schedule.Execute.currentSchedule.get('id') , false, allStatusList, paginationData ] );

            			}) //From execution-field-view.js

            	e.preventDefault();	//To avoid dialog submit propogation
            }
        });
        dialog.show();
    }
});

/*
 * Commenting this code as we are not using it.
//Router
var ExecutionRouter = Backbone.Router.extend({

    routes:{
        "":"list"
    },
    list:function () {
    	ZEPHYR.Schedule.Execution.data.teststeps = new ZEPHYR.Issue.TestStepCollection();
   		ZEPHYR.Schedule.Execution.data.teststeps.fetch({success:this.fetchStepResults});
    },
    fetchStepResults: function (eventName) {
    	var stepResults = new ZEPHYR.Schedule.Execution.TestStepResultCollection()
    	stepResults.fetch({data:{executionId:ZEPHYR.Schedule.Execute.currentSchedule.get('id'),expand:"executionStatus"}, success:function(){
    		window.teststepResultView = new ZEPHYR.Schedule.Execution.TeststepResultView({model:stepResults});
    		if(stepResults.models != null && stepResults.models.length > 0) {
    			ZEPHYR.Schedule.Execution.data.stepExecutionStatuses = new Backbone.Collection(stepResults.models[0].get('executionStatus'));
    		}
    		AJS.$('div.mod-content table.aui').append(window.teststepResultView.render().el);
    		_.each(stepResults.models, function(stepResult){
    			ZEPHYR.Schedule.Execute.getAttachments( stepResult.get('id') , "TESTSTEPRESULT");
    		})
    		initializeDefectPickers();
    	}})
    }
});
*/

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
	if(ZEPHYR.Schedule.Execute.currentSchedule) {
		ZEPHYR.Schedule.Execution.data = {}
		var app = new ExecutionRouter();
		Backbone.history.start();
	}
})
