ZEPHYR.ExecutionCycle = Backbone.Model.extend();
var globalDispatcher = _.extend({}, Backbone.Events);
var fetchProjectDetails = true;
var projectDetails = {};
var modelAttr = new ZEPHYR.Issue.ScheduleModel();
var allStatusList = {};

ZEPHYR.ExecutionCycle.testStep = {
  limit: 10,
  offset: 0,
  maxRecords: 0,
};
var isReadyForPaginationChange = true;

// Detail execution view

var stepExecutionColumnInlineDialog;

ZEPHYR.ExecutionCycle.executionStepColumns = {};
ZEPHYR.ExecutionCycle.customFieldsOrder = [];
ZEPHYR.ExecutionCycle.schedule = {};

var allCustomFields;
var stepSubmitButtonId = 'stepSubmitExecutionColumns';
var stepCloseButtonId = 'stepCloseInlineDialog';

AJS.$('#inline-dialog-step-execution-column-picker').live("click", function(e) {
   e.stopPropagation();
});

AJS.$('body').live("click", function(e){
    stepExecutionColumnInlineDialog && stepExecutionColumnInlineDialog.hide();
});

AJS.$('#' + stepSubmitButtonId).live('click', function() {
    var stepData = {};

    AJS.$('#inline-dialog-step-execution-column-picker li :checkbox').each(function() {
      if(this.checked) {
        ZEPHYR.ExecutionCycle.executionStepColumns[this.id].isVisible = "true";
      }	else {
        ZEPHYR.ExecutionCycle.executionStepColumns[this.id].isVisible = "false"
      }
    });

    stepData['preferences'] = ZEPHYR.ExecutionCycle.executionStepColumns;

    var stepDataString = JSON.stringify(stepData);
    jQuery.ajax({
      url: getRestURL() + "/preference/setexecutioncustomization",
      type : "post",
      contentType :"application/json",
      data : stepDataString,
      dataType: "json",
      success : function(response) {
        window.executionDetailView.render();
        window.teststepResultView.render();
        stepExecutionColumnInlineDialog.hide();
      }
    });
});

var getCustomFields = function(projectId,completed) {
  jQuery.ajax({
    url: getRestURL() + "/customfield/entity?entityType=TESTSTEP&projectId="+projectId,
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
      getProjectCustomFieldsPC(response, projectId, completed);
    }
  });
}

var getProjectCustomFieldsPC = function(generalCustomField, projectId, completed) {
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

var getExecutionCustomization = function(completed) {
  jQuery.ajax({
    url: getRestURL() + "/preference/getexecutioncustomization",
    type : "get",
    contentType :"application/json",
    dataType: "json",
    success : function(response) {
      var preferencesObj = response.preferences;
      Object.keys(preferencesObj).forEach(function(key) {
        var obj = {
            "displayName": preferencesObj[key].displayName,
            "isVisible": preferencesObj[key].isVisible
        }
          ZEPHYR.ExecutionCycle.executionStepColumns[key] = obj;
        });

      if(Object.keys(preferencesObj).length ===  8) {
        allCustomFields.forEach(function(field) {
          var obj = {
            "displayName": field.name,
            "isVisible": "false"
          }
          ZEPHYR.ExecutionCycle.executionStepColumns[field.id] = obj;
        });
      }
       if(completed)
        completed.call();
    }
  });
}

AJS.$('#' + stepCloseButtonId).live('click', function(event){
    stepExecutionColumnInlineDialog.hide();
});

var timeoutNext = true, timeoutPrev = true;
ZEPHYR.ExecutionCycle.DetailExecutionView = Backbone.View.extend({
	initialize: function() {
	},

	prevExecution: function(ev) {
    var self = this;
    if (timeoutPrev) {
      timeoutPrev = false;
      var localIndex = self.model.paginationData.localIndex;
      var executionIds = self.model.paginationData.executionIds;

      if (localIndex > 0 || self.model.paginationData.dbIndex) {
        var newExecutionId = executionIds[localIndex - 1];
        if (!newExecutionId) {
          AJS.$(document).trigger("triggerGridClick", ['prev']);
          return;
        }
        AJS.$(document).trigger("triggerExecutionDetails", [newExecutionId, false, executionsTableModelNew.attributes.allStatusList, { total: parseInt(self.model.paginationData.total), dbIndex: self.model.paginationData.dbIndex - 1, localIndex: self.model.paginationData.localIndex - 1, executionIds: self.model.paginationData.executionIds }]);
        AJS.$(document).trigger("updateSelectionId", [newExecutionId]);
      }
      setTimeout(function () {
        timeoutPrev = true;
      }, 300);
    }
  },

  nextExecution: function(ev) {
    var self = this;
    if (timeoutNext) {
      timeoutNext = false;
      var localIndex = self.model.paginationData.localIndex;
      var executionIds = self.model.paginationData.executionIds;

      if (localIndex < self.model.paginationData.total) {
        var newExecutionId = executionIds[localIndex + 1];
        if (!newExecutionId) {
          AJS.$(document).trigger("triggerGridClick", ['next']);
          return;
        }
        AJS.$(document).trigger("triggerExecutionDetails", [newExecutionId, false, executionsTableModelNew.attributes.allStatusList, { total: parseInt(self.model.paginationData.total), dbIndex: self.model.paginationData.dbIndex + 1, localIndex: self.model.paginationData.localIndex + 1, executionIds: self.model.paginationData.executionIds }]);
        AJS.$(document).trigger("updateSelectionId", [newExecutionId]);
      }
      setTimeout(function() {
        timeoutNext = true;
      }, 300);
    }
  },

    //function listening on change of textbox for defects in standAlone page and strikeThrough
  	//text if resolution is done. fetches the li elements and loops through them to check its inner html value
  	//matches with defect key value and resolution is done.

  strikeOutDefectText: function(e) {
    var listOfElem = AJS.$('#editable-schedule-defects').find('.representation ul').find('li');
    if(!listOfElem.length || !this.model.execution.defects || (listOfElem.length !== this.model.execution.defects.length)){
      return;
    }
    for(var i = 0; i < listOfElem.length; i += 1) {
      var issueId = AJS.$(listOfElem[i]).find('.value-text a').html() || AJS.$(listOfElem[i]).find('.value-text').html();
      var defectObj = _.findWhere(this.model.execution.defects, {key: issueId});
      //var title = defectObj.summary + ':' + defectObj.status;
      if(!AJS.$(listOfElem[i]).find('.value-text').hasClass('linkAppended')) {
        var issueText = AJS.$(listOfElem[i]).find('.value-text').html();
        AJS.$(listOfElem[i]).find('.value-item').on('click', function(event) {
          var url = AJS.$(event.currentTarget).find('a').attr('data-href');
          if (event.ctrlKey) {
            window.open(url, '_blank')
          } else {
            window.location.href = AJS.$(event.currentTarget).find('a').attr('data-href');
          }
        });
        //AJS.$(listOfElem[i]).attr('title', title);
        AJS.$(listOfElem[i]).find('.value-text').html('<a data-href="' + contextPath + '/browse/' + issueText + '">'+ issueText + '</a>');
        AJS.$(listOfElem[i]).find('.value-text').addClass('linkAppended');
      }

      if(defectObj && defectObj.resolution === 'Done') {
        AJS.$(listOfElem[i]).find('.value-text a').css('text-decoration', 'line-through');
      }
    }
  },

  fetchExecution: function(ev) {
    var newExecutionId = ev.currentTarget.dataset.id;
    AJS.$(document).trigger( "triggerExecutionDetails", [ newExecutionId, false, executionsTableModelNew.attributes.allStatusList, {total:parseInt(this.model.paginationData.total), dbIndex: this.model.paginationData.dbIndex, localIndex: this.model.paginationData.localIndex, executionIds:this.model.paginationData.executionIds} ] );
    AJS.$(document).trigger("updateSelectionId", [newExecutionId]);
  },

	attachDetailView: function(detailViewContainer, executionObject , statuses, paginationObject) {

    executionObject.cycleName = executionObject.cycleName;
    executionObject.folderName = executionObject.folderName;
		ZEPHYR.Schedule.executionStatus = 	statuses[executionObject['executionStatus']];
		ZEPHYR.Schedule.Execute.data = {
				issueId:		executionObject['issueId'],
				scheduleId:		executionObject['id'],	//This is redundant coz its used in templates. Need to remove
				projectId:		executionObject['projectId']
			};
      var _assignee = {
        "executionId": executionObject.id,
        "assignee": executionObject.assignedTo,
        "assigneeDisplay": executionObject.assignedToDisplay,
        "assigneeUserName": executionObject.assignedToUserName,
        "assigneeType": executionObject.assigneeType
      };

			var issueSummary = executionObject['issueSummary'] || '';
      var issueDescription = executionObject['issueDescription'] || '';
      executionObject['createdBy'] = executionObject['createdBy'] || ''; //This is added to check for the null value for 'createdBy'
      ZEPHYR.Execution.currentSchedule = {
        'statusObj': {
          currentExecutionStatus: statuses[executionObject['executionStatus']],
          executionStatuses: statuses,
        }
      }
      paginationObject.nextExecutionId = 0;
      paginationObject.prevExecutionId = 0;
      var previous = null;
      var next = null;
      var previousId = 0;
      var nextId = 0;
      for(var i=0; i<ZEPHYR.Cycle.executions.length; i++){
        if(ZEPHYR.Cycle.executions[i].id === ZEPHYR.Schedule.Execute.data.scheduleId){
          if (ZEPHYR.Cycle.executions[i+1]) {
            paginationObject.nextExecutionId = ZEPHYR.Cycle.executions[i+1].id;
            next = ZEPHYR.Cycle.executions[i + 1];
            nextId = ZEPHYR.Cycle.executions[i + 1].id;
          }
          if (ZEPHYR.Cycle.executions[i-1]) {
            paginationObject.prevExecutionId = ZEPHYR.Cycle.executions[i-1].id;
            previous = ZEPHYR.Cycle.executions[i - 1];
            previousId = ZEPHYR.Cycle.executions[i - 1].id;;
          }
          break;
        }
      }

      var atLastPagination= false;
      var atFirstPagination = false;
      if (selectedPage == 1) {
        atFirstPagination = true;
      }
      if (selectedPage == totalPages) {
        atLastPagination = true;
      }


      ZEPHYR.ExecutionCycle.schedule = executionObject;
      executionObject.executedOnVal = new Date(executionObject.executedOnVal).toDateString();
      executionObject.createdOnVal = new Date(executionObject.createdOnVal).toDateString();
      var scheduleIndex = ZEPHYR.Cycle.executions.indexOf(_.findWhere(ZEPHYR.Cycle.executions, {id: executionObject.id}));
			var detailedViewHtml = ZEPHYR.Templates.Execution.detailedExecutionView({
        previous: previous,
        next: next,
        previousId: previousId,
        nextId: nextId,
        executions : ZEPHYR.Cycle.executions,
				schedule:				executionObject,
				executedByDisplay:		executionObject.executedByDisplay,
        executedBy:				executionObject.executedBy,
				currentExecutionStatus:	statuses[executionObject['executionStatus']],
				executionStatuses:		statuses,
				contextPath:			contextPath,
				projectKey:				executionObject.projectKey,
				projectName:			projectDetails.name,
				projectAvatarId:	    fetchProjectAvatarId(projectDetails.avatarUrls),
				issueDescAsHtml:		issueDescription,
				issueSummary: 			issueSummary,
				summary: 				issueSummary,
				isCycleSummaryExecution: true,
				paginationObject : paginationObject,
				stepColumns: ZEPHYR.ExecutionCycle.executionStepColumns,
        customFieldsOrder: ZEPHYR.ExecutionCycle.customFieldsOrder,
        atLastPagination: atLastPagination,
        atFirstPagination:  atFirstPagination,
        scheduleIndex: scheduleIndex
			});
      var breadcrumbTemplate = ZEPHYR.Templates.PlanCycle.breadCrumbsView({
        schedule:       executionObject,
        contextPath:      contextPath,
        projectKey:       executionObject.projectKey,
        projectName:      projectDetails.name
      });
      var leftNavDetails = ZEPHYR.Templates.Execution.leftNavDetails({
          schedule:       executionObject,
          contextPath:      contextPath,
          isCycleSummaryExecution: true,
          atLastPagination: atLastPagination,
          atFirstPagination:  atFirstPagination,
      });
      AJS.$('#breadcrumbs-wrapper').html(breadcrumbTemplate);


			//detailViewContainer.html(detailedViewHtml);
      var exeCont = detailViewContainer.find('.exe-cont');
      if (exeCont.length){
        exeCont.html('');
      } else {
        detailViewContainer.html(ZEPHYR.Templates.Execution.executionWrapper({}));
        exeCont = detailViewContainer.find('.exe-cont');
      }

      exeCont.append(leftNavDetails);
      exeCont.append(detailedViewHtml);
      //setTimeout(function() {
        var observerUtility = new ObserverUtility();
        if (document.getElementById('editable-schedule-defects')) {
          observerUtility.observer.observe(document.getElementById('editable-schedule-defects'));
        }
      //}, 0);
      if(executionObject.executionWorkflowStatus == 'COMPLETED') {
        setTimeout(function() {
          AJS.$('#editable-schedule-defects textarea').attr('disabled', true);
        }, 2000);
      }

      if(AJS.$('#estimatedTimeId').length) {
        AJS.progressBars.update("#estimatedTimeId", 1);
        AJS.progressBars.update("#loggedTimeId", 0.4);
      }
			// Attach executionStatusView
			var editableFieldTrigger = AJS.$('#zexecute div.field-group.execution-status-container')[0];
			var executionStatusView = new ZEPHYR.Schedule.executionStatusView({
				el: 			editableFieldTrigger,
				elBeforeEdit:	AJS.$(editableFieldTrigger).find('[id^=execution-field-current-status-schedule-]'),
				elOnEdit:		AJS.$(editableFieldTrigger).find('[id^=execution-field-select-schedule-]')
			});

		/* Initialize backbone model using form elements */
		ZEPHYR.Schedule.Execute.currentSchedule = new ZEPHYR.Issue.Schedule({
			id:					executionObject['id'],
			executionStatus: 	AJS.$("select[id^=exec_status-schedule] option:selected").attr("value"),
			comment:			AJS.$('#schedule-comment')
		});
    AJS.$(document).trigger( "appendAssigneeUI", [_assignee , contextPath ] );
    // AJS.$('#exec-assignee-wrapper').append(ZEPHYR.Templates.Execution.executionAssigneeDetailView({
    //   assignee:   _assignee,
    //   contextPath: contextPath
    // }));

    // Attach Edit View UI
        var editableFieldTrigger = AJS.$('#zexecute div.field-group.execution-assignee-container')[0];
        // Attach assignee editable view only if user exists
        if(editableFieldTrigger) {
          var _executionAssigneeView = new ZEPHYR.Schedule.executionAssigneeView({
            el:       editableFieldTrigger,
            elBeforeEdit: AJS.$(editableFieldTrigger).find('[id^=execution-field-current-assignee-]'),
            elOnEdit:   AJS.$(editableFieldTrigger).find('[id^=execution-field-select-assignee-]')
          });
        }
      if(executionObject.canViewIssue) {
   		  ZEPHYR.Execution.History.init(executionObject['issueId'],executionObject['id']);
        ZEPHYR.Execution.CustomField.init(executionObject);
      }

   		if(AJS.$("#inline-dialog-step-execution-column-picker").length > 0) {
          AJS.$("#inline-dialog-step-execution-column-picker").remove();
        }

   		var stepColumnCustomization = ZEPHYR.Templates.PlanCycle.columnCustomisation({columns: ZEPHYR.ExecutionCycle.executionStepColumns, submitButtonId: stepSubmitButtonId, closeButtonId: stepCloseButtonId });
        stepExecutionColumnInlineDialog = AJS.InlineDialog(AJS.$("#step-columnCustomisation-inlineDialog"), "step-execution-column-picker",
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
        //var innerhtmladd = ZEPHYR.Schedule.Attachment.addAttachmentBtn({schedule:executionObject,contextPath:contextPath});
      //AJS.$("#file_attachments").html() = AJS.$("#file_attachments").html() + innerhtmladd;
	},

	attachTestSteps: function() {
		var instance = this;
		ZEPHYR.Schedule.Execute.getAttachments(ZEPHYR.Schedule.Execute.currentSchedule.get("id"), "SCHEDULE");
		scheduleView = new ZEPHYR.Schedule.Execute.ScheduleView({model:ZEPHYR.Schedule.Execute.currentSchedule, el:AJS.$("#zexecute")});
		ZEPHYR.Schedule.Execution.data = {};
		ZEPHYR.Schedule.Execution.data.teststeps = new ZEPHYR.Issue.TestStepCollection();
   		ZEPHYR.Schedule.Execution.data.teststeps.fetch({success: function(teststeps, response) {
        ZEPHYR.Execution.testDetailsGrid.testStep = response;
   			instance.fetchStepResults();
   		}});
	},

  changePaginationWidth: function (event) {
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

  togglePaginationOptionDrodown: function() {
    if (AJS.$('#pagination-options-container').hasClass('hide')) {
      AJS.$('#pagination-options-container').removeClass('hide');
    } else {
      AJS.$('#pagination-options-container').addClass('hide');
    }
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

  resetPaginationFlag: function () {
    setTimeout(function () {
      isReadyForPaginationChange = true;
    }, 200);
  },


	fetchStepResults: function() {
       var stepResults = new ZEPHYR.Schedule.Execution.TestStepResultCollection();
       if (allPagesPaginationPageWidth.testGridWidth) {
          ZEPHYR.ExecutionCycle.testStep.limit = allPagesPaginationPageWidth.testGridWidth;
       } else {
        var sendingData = allPagesPaginationPageWidth;
        sendingData.testGridWidth = 10;

        jQuery.ajax({
          url: getRestURL() + "/preference/paginationWidthPreference",
          type: "PUT",
          contentType: "application/json",
          data: JSON.stringify(sendingData),
          dataType: "json",
          success: function (response) {
            allPagesPaginationPageWidth = response;
          }
        });

        ZEPHYR.ExecutionCycle.testStep.limit = 10;
       }
    stepResults.fetch({
      data: { executionId: ZEPHYR.Schedule.Execute.currentSchedule.get('id'), expand: "executionStatus", limit: ZEPHYR.ExecutionCycle.testStep.limit, offset: ZEPHYR.ExecutionCycle.testStep.offset}, success:function(){

        if (stepResults.models && stepResults.models.length > 0) {

          ZEPHYR.ExecutionCycle.testStep.maxRecords = stepResults.models[0].attributes.stepResultsCount;
          var lastPageLowerLimit = parseInt(ZEPHYR.ExecutionCycle.testStep.maxRecords / parseInt(ZEPHYR.ExecutionCycle.testStep.limit)) * parseInt(ZEPHYR.ExecutionCycle.testStep.limit);

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
        var counter = 0;
        if(ZEPHYR.Execution.testDetailsGrid.stepResults.length === 0) {
          ZEPHYR.Execution.testDetailGridExecutionPage();
        }
     		/*ZEPHYR.Execution.testDetailsGrid.stepResults.map(function(stepResult, index){
     			ZEPHYR.Schedule.Execute.getAttachments(stepResult.id, "TESTSTEPRESULT",function(attachment){
						stepResult['stepAttachmentMap'] = attachment;
						counter++;
						if(counter === ZEPHYR.Execution.testDetailsGrid.stepResults.length)
							ZEPHYR.Execution.testDetailGridExecutionPage();
  				});
     		})*/
        //Above code should be removed: Attachments are now fetched on click of attachment trigger.
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

    attachEvents: function() {
    	var that = this;
    	AJS.$('.previous-link').on('click', function(e) {
    		that.prevExecution(e);
    	});
    	AJS.$('.next-link').on('click', function(e) {
    		that.nextExecution(e);
    	});
      AJS.$('.selectable-link').on('click', function(e) {
        that.fetchExecution(e);
      });
      AJS.$('.aui-field-defectpickers').on('focusout, change', function(e) {
        that.strikeOutDefectText(e);
      });
      AJS.$('.change-pagination-width-function').live('click', function (e) {
        that.changePaginationWidth(e);
      });
      AJS.$('#pagination-dropdown-button').live('click', function (e) {
        that.togglePaginationOptionDrodown(e);
      });
      AJS.$('#pagination-go-to-previous-page').live('click', function (e) {
        that.nextPage(e);
      });
      AJS.$('#pagination-go-to-next-page').live('click', function (e) {
        that.previousPage(e);
      });
      AJS.$('#first-page-pagination').live('click', function (e) {
        that.goToFirstPage(e);
      });
      AJS.$('#last-page-pagination').live('click', function (e) {
        that.goToLastPage(e);
      });
    },

	render: function() {

    // Attach execution detail
    this.attachDetailView(AJS.$('.execution-details-wrapper'), this.model.execution, allStatusList, this.model.paginationData);
    this.attachEvents();
    // Attach Test steps

      if(this.model.execution.canViewIssue) {
        var executionJSON = this.model.execution;
        if(executionJSON.isExecutionWorkflowEnabled && executionJSON.isTimeTrackingEnabled && executionJSON.cycleId != -1 && !executionJSON.isIssueEstimateNil) {
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
        }

        if (!this.model.updateStatusOnly) {
          this.attachTestSteps();
        }
    }
		return this;
	}
});



var isLoadedInIframe = function() {
    try {
        return (window !== window.parent);
    } catch(e) {
        return false;
    }
}

var fetchProjectAvatarId = function (avatarUrls) {
	var id = avatarUrls && avatarUrls['16x16']  && avatarUrls['16x16'].slice( avatarUrls['16x16'].indexOf('avatarId=') + 9  );
	return id;
}



var fetchProjectVersion = function(projectKey, successCallback) {
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
			console.log('failure while fetching project data');
		}
	});
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

var hideExecutionDetails = function() {
	AJS.$('.execution-details-wrapper').html('');
}


var LoadExecutionDetails = function(executionId , updateStatusOnly, paginationData) {
	modelAttr.executionId = executionId;
	modelAttr.updateStatusOnly = updateStatusOnly;
  if(paginationData.executionIds.length > 1){
    paginationData.dbIndex = executionId - 1;
    for(var i = 0; i<paginationData.executionIds.length;i++){
      if(executionId == paginationData.executionIds[i]){
        paginationData.localIndex = i;
        break;
      }
    }
  }
	modelAttr.fetch({contentType:'application/json', success:function(response) {
    ZEPHYR.Cycle.clickedExecutionId = response.changed.execution.id;
		    modelAttr.attributes.updateStatusOnly = updateStatusOnly;
		    modelAttr.attributes.updateStatusOnly = updateStatusOnly;
		    modelAttr.attributes.paginationData = paginationData;
			if (fetchProjectDetails) {
				fetchProjectDetails = false;
				fetchProjectVersion(modelAttr.attributes.execution.projectId,function(response) {
          projectDetails = response;
          getCustomFields(modelAttr.attributes.execution.projectId, function(){
            window.executionDetailView = new ZEPHYR.ExecutionCycle.DetailExecutionView({model: modelAttr.attributes, dispatcher:globalDispatcher});
  					AJS.$('.html').html(window.executionDetailView.render().el);
          });
				});
			} else {
        getCustomFields(modelAttr.attributes.execution.projectId, function(){
          window.executionDetailView = new ZEPHYR.ExecutionCycle.DetailExecutionView({model: modelAttr.attributes, dispatcher:globalDispatcher});
  				AJS.$('.html').html(window.executionDetailView.render().el);
        });
			}
  }, error: function (collection, xhr){
    if (JSON.parse(xhr.responseText).errorId.toLowerCase() == 'insufficient issue permissions' || JSON.parse(xhr.responseText).errorId.toLowerCase() == 'insufficient project permissions') {
      modelAttr.attributes.execution = {
        canViewIssue:false,
        comment:"",
        component:"",
        createdBy:"XXXXX",
        customFields:"{}",
        cycleId:'',
        cycleName:"",
        executedBy:"XXXXX",
        executedByDisplay:"XXXXX",
        executedOn:"XXXXX",
        executionDefectCount:0,
        executionStatus:"XXXXX",
        htmlComment:"",
        id: executionId,
        issueId:'',
        issueKey:"SP-XXXXX",
        label:"XXXXX",
        modifiedBy:"XXXXX",
        orderId:'',
        projectAvatarId:'',
        projectId:'',
        projectKey:"",
        projectName:"",
        stepDefectCount:1,
        summary:"XXXXX",
        totalDefectCount:1,
        versionId:'',
        versionName:"XXXXX",
      }
      modelAttr.attributes.updateStatusOnly = updateStatusOnly;
      modelAttr.attributes.updateStatusOnly = updateStatusOnly;
      modelAttr.attributes.paginationData = paginationData;
      window.executionDetailView = new ZEPHYR.ExecutionCycle.DetailExecutionView({ model: modelAttr.attributes});
      AJS.$('.html').html(window.executionDetailView.render().el);
      // if (fetchProjectDetails) {
      //   fetchProjectDetails = false;
      //   fetchProjectVersion(modelAttr.attributes.execution.projectId, function (response) {
      //     projectDetails = response;
      //     getCustomFields(function () {
      //       window.executionDetailView = new ZEPHYR.ExecutionCycle.DetailExecutionView({ model: modelAttr.attributes, dispatcher: globalDispatcher });
      //       AJS.$('.html').html(window.executionDetailView.render().el);
      //     });
      //   });
      // } else {

      // }
    }

    }});
}

InitPageContent(function(){
	AJS.$( document ).on( "triggerExecutionDetails", function( event, executionId , updateStatusOnly, statusList, paginationData) {
		//console.log('status list', statusList);
    if (executionId) {
			allStatusList = statusList;
	    	LoadExecutionDetails(executionId , updateStatusOnly, paginationData);
		} else {
			hideExecutionDetails();
		}
	});
});
