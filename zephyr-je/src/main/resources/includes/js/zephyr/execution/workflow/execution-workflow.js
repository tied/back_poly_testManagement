function ExecutionWorkflow() {

	var makeAjaxRequest = function(url, type, successCb, errorCb) {
		AJS.$.ajax({
			url: getRestURL() + url,
			type: type || 'GET',
			contentType: "application/json",
			success: function(response) {
				if(successCb && typeof successCb === 'function') {
					successCb(response);
				}
			},
			error: function(err) {
				if(errorCb && typeof errorCb === 'function') {
					try {
						errorCb(err && JSON.parse(err.responseText));
					}
					catch(error) {
						errorCb(err.responseText);
					}
				}
			}
		});
	};

	var manageDisabledState = function(elemIds) {
		if(!elemIds) {
			return;
		}
		AJS.$('.workflow-button-wrapper').find('a.aui-button').removeClass('disabled');
		elemIds.forEach(function(id) {
			AJS.$('#' + id).addClass('disabled');
		});
	};

	var manageHideState = function(elemIds) {
		if(!elemIds) {
			return;
		}
		AJS.$('.workflow-button-wrapper').find('a.aui-button').removeClass('hidden');
		elemIds.forEach(function(id) {
			AJS.$('#' + id).addClass('hidden');
		});
	}

	var manageSelectionState = function(elem) {
		AJS.$('.workflow-button-wrapper').find('a.aui-button').removeClass('aui-button-primary disabled-primary');
		AJS.$(elem).addClass('aui-button-primary disabled-primary');
		var id = AJS.$(elem).attr('id');
		if(id === 'workflowTodo' || id === 'workflowDone') {
			AJS.$('#modifyLoggedTime').addClass('disabled');
		} else {
			AJS.$('#modifyLoggedTime').removeClass('disabled');
		}
		if (id != 'workflowTodo') {
			AJS.$('#executionWorkFlowProgressWrapper').css('display', 'flex');
		}
	};

	var disabledMap = {
		'workflowInprogress': ['workflowTodo'],
		'workflowDone': ['workflowTodo', 'workflowInprogress'],
		'workflowTodo' : ['workflowDone']
	};

	var hideMap = {
		'workflowTodo' :  ['workflowReopen'],
		'workflowInprogress': ['workflowReopen'],
		'workflowDone': ['workflowInprogress']
	}

	var workflowInprogress = function() {
		var elem = this;
		makeAjaxRequest('/execution/workflow/' + executionId + '/inProgress', 'PUT', function(response) {
			AJS.$('#estimatedTimeValue').html(response.execution.executionEstimatedTime);
			AJS.$('#estimatedTimeValue').prop('title', timewithyears(response.execution.executionEstimatedTime));
			manageDisabledState(disabledMap['workflowInprogress']);
			manageSelectionState(elem);
			showMessage('success', 'Started');
			ZEPHYR.Schedule.Execute.updateExecutionHistory();
		}, function(err) {
			showMessage('error', err.error || 'Error in starting workflow');
		});
	};

	var workflowReopen = function() {
		var elem = this;
		makeAjaxRequest('/execution/workflow/' + executionId + '/reopen', 'PUT', function(response) {
			AJS.$('#estimatedTimeValue').html(response.execution.executionEstimatedTime);
			AJS.$('#estimatedTimeValue').prop('title', timewithyears(response.execution.executionEstimatedTime));
			manageDisabledState(disabledMap['workflowInprogress']);
			manageSelectionState(AJS.$('#workflowInprogress')[0]);
			manageHideState(hideMap['workflowInprogress']);
			manageExecutionWorkFlowCompletedClass(false);
			showMessage('success', 'Re-started');
			ZEPHYR.Schedule.Execute.updateExecutionHistory();
		}, function(err) {
			showMessage('error', err.error || 'Error in starting workflow');
		});
	};

	var workflowDone = function() {
		isWorflowDoneClicked = true;
		modifyLoggedTime();
	};

	var manageExecutionWorkFlowCompletedClass = function(flag) {
		if (flag) {
			AJS.$('.content-container-execution .content-body.aui-panel').addClass('executionWorkflowStatusCompleted');
			AJS.$('#editable-schedule-defects textarea').attr('disabled', true);
		} else {
			AJS.$('.content-container-execution .content-body.aui-panel').removeClass('executionWorkflowStatusCompleted');
			AJS.$('#editable-schedule-defects textarea').attr('disabled', false);
		}
	}

	var completeWorkflow = function(workflowLoggedTime) {
		makeAjaxRequest('/execution/workflow/' + executionId + '/complete?timeLogged=' + workflowLoggedTime, 'PUT', function(response) {
			manageDisabledState(disabledMap['workflowDone']);
			manageSelectionState(AJS.$('#workflowDone')[0]);
			manageExecutionWorkFlowCompletedClass(true);
			manageHideState(hideMap['workflowDone']);
			showMessage('success', 'Completed');
			closeLogTimeDialog();
			var executionTimeLogged = response && response.execution && response.execution.executionTimeLogged;
			if(executionTimeLogged) {
				loggedTime = executionTimeLogged;
				AJS.$('#loggedTimeValue').html(loggedTime);
				AJS.$('#loggedTimeValue').prop('title', timewithyears(loggedTime));
				if (response.execution.workflowCompletePercentage > 100) {
					updateWorkflowValues(response.execution.workflowLoggedTimedIncreasePercentage, response.execution.workflowCompletePercentage);
				} else{
					updateValues(response && response.execution && response.execution.workflowCompletePercentage);
				}
			}
			ZEPHYR.Schedule.Execute.updateExecutionHistory();
		}, function(err) {
			showMessage('error', err.error || 'Error in completing workflow');
		});
	};

	var initModifyLoggedTimeDialog = function() {
		if(AJS.$('#modifyLoggedTimeDialog').length) {
			AJS.$('#modifyLoggedTimeDialog').remove();
		}
		AJS.$('body').append(ZEPHYR.Templates.Workflow.renderModifyLoggedTimeDialog());
		AJS.$('#modifyLoggedTime').on('click', modifyLoggedTime);
		AJS.$('#logTimeSubmit').on('click', logTimeSubmit);
		AJS.$('#closeLogTimeDialog').on('click', closeLogTimeDialog);
		AJS.$('#workflowloggedTimeForm').on('keyup keypress', preventFormSubmit);
	};

	var preventFormSubmit = function(e) {
		var keyCode = e.keyCode || e.which;
	  if (keyCode === 13) {
	    e.preventDefault();
	    return false;
	  }
	};

	var modifyLoggedTime = function() {
		if(!AJS.$('#modifyLoggedTimeDialog').length) {
			initModifyLoggedTimeDialog();
		}
		AJS.$('#workflowloggedTime').val(loggedTime || '');
		AJS.dialog2("#modifyLoggedTimeDialog").show();
	};

	var logTimeSubmit = function(ev) {
		ev.stopPropagation();
		var workflowLoggedTime = AJS.$('#workflowloggedTime').val();
		//var regexTime=/^((?:[0-9]?\d)w)?\s*(\d+d)?\s*((?:[01]?\d|2[0-3])h)?\s*((?:[0-5]?\d)m)?$/;
		var regexTime=/^(\d+w)?\s*(\d+d)?\s*(\d+h)?\s*(\d+m)?$/;
		if(!workflowLoggedTime || !regexTime.test(workflowLoggedTime)) {
			var errMsg = AJS.I18n.getText('je.gadget.execution.workflow.loggedtime.error');
			showMessage('error', errMsg);
			return;
		}

		if(isWorflowDoneClicked) {
			completeWorkflow(workflowLoggedTime);
			isWorflowDoneClicked = false;
		} else {
			makeAjaxRequest('/execution/workflow/' + executionId + '/loggedTime/modify?timeLogged=' + workflowLoggedTime, 'PUT', function(response) {
				showMessage('success', 'Logged time modified');
				closeLogTimeDialog();
				var executionTimeLogged = response && response.execution && response.execution.executionTimeLogged;
				if(executionTimeLogged) {
					loggedTime = executionTimeLogged;
					AJS.$('#loggedTimeValue').html(loggedTime);
					AJS.$('#loggedTimeValue').prop('title', timewithyears(loggedTime));
					if (response.execution.workflowCompletePercentage > 100) {
						updateWorkflowValues(response.execution.workflowLoggedTimedIncreasePercentage, response.execution.workflowCompletePercentage);
					} else{
						updateValues(response && response.execution && response.execution.workflowCompletePercentage);
					}
				}
				ZEPHYR.Schedule.Execute.updateExecutionHistory();
			}, function(err) {
				showMessage('error', err.error || AJS.I18n.getText('je.gadget.execution.workflow.loggedtime.error'));
			});
		}
	};

	var closeLogTimeDialog = function() {
		AJS.dialog2("#modifyLoggedTimeDialog").hide();
	};

	var bindEvents = function() {
		AJS.$('#workflowInprogress').on('click', workflowInprogress);
		AJS.$('#workflowReopen').on('click', workflowReopen);
		AJS.$('#workflowDone').on('click', workflowDone);
	};

	var updateWorkflowValues = function(estimatedTime, loggedTimePercentage){
		if(estimatedTime > 100) {
			estimatedTime = 100;
		}
		if(loggedTimePercentage > 100) {
			loggedTimePercentage = 100;
		}
		if(AJS.$('#estimatedTimeId').length) {
		    AJS.progressBars.update("#estimatedTimeId", estimatedTime / 100);
		    AJS.progressBars.update("#loggedTimeId", loggedTimePercentage / 100);
		}
	}

	var updateValues = function(loggedTimePercentage) {
		if(loggedTimePercentage > 100) {
			loggedTimePercentage = 100;
		}
		if(AJS.$('#estimatedTimeId').length) {
		    AJS.progressBars.update("#estimatedTimeId", 1);
		    AJS.progressBars.update("#loggedTimeId", loggedTimePercentage / 100);
		}
	};

	var showMessage = function(type, msg) {
		require(['aui/flag'], function(flag) {
            var workflowFlag = flag({
                type: type,
                title: 'Execution workflow',
                body: msg
            });
            setTimeout(function() {
                workflowFlag.close();
            }, 4000);
        });
	};

	var timewithyears = function(time){
		if (time=='') {
			return time;
		}
		var index = time && time.indexOf("w");
		if(index>-1){
			var weeks = time.slice(0, index);
			var year;
			if(weeks >= 52){
				time = time.slice(index+2, time.length);
				year = parseInt(weeks/52);
			  	weeks = weeks % 52;
			  	if(weeks){
					time = year+"y "+ weeks+"w "+ time;
			  	} else{
			  		time = year+"y "+ time;
			  	}
			}
		}
		return time;
	}

	var getSelectedElem = {
		'CREATED': 'workflowTodo',
		'STARTED': 'workflowInprogress',
		'COMPLETED': 'workflowDone',
		'REOPEN' : 'workflowInprogress'
	};

	var executionId = null;
	var loggedTime = null;
	var isWorflowDoneClicked = false;

	this.init = function(options) {
		if(!options) {
			return;
		}
		executionId = options.executionId;
		loggedTime = options.loggedTime;
		AJS.$('#' + options.buttonWrapperId).append(ZEPHYR.Templates.Workflow.createWorkflowButtons());
		AJS.$('#' + options.progressWrapperId).append(ZEPHYR.Templates.Workflow.createWorkflowProgressWrapper({
			estimatedTime: options.estimatedTime || '',
			loggedTime: options.loggedTime || '',
			estimatedTimeTitle: timewithyears(options.estimatedTime || ''),
			loggedTimeTitle: timewithyears(options.loggedTime || '')
		}));
		initModifyLoggedTimeDialog();
		bindEvents();
		if (options.loggedTimePercentage > 100) {
			updateWorkflowValues(options.estimatedTimePercentage, options.loggedTimePercentage);
		} else{
			updateValues(options.loggedTimePercentage);
		}
		manageSelectionState(AJS.$('#' + getSelectedElem[options.workflowStatus || 'CREATED'])[0]);
		manageDisabledState(disabledMap[getSelectedElem[options.workflowStatus || 'CREATED' ]])
		manageHideState(hideMap[getSelectedElem[options.workflowStatus || 'CREATED']]);
	}
}
