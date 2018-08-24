/**
 * Handles the saving, editing and canceling of an execution fields.
 * This is the parent view handles top level events like click, focusout...
 */
var issueIdOrKey;
var oldStatusKey;
var newStatusKey;
var globalzqlSearch;

if(!ZEPHYR.Schedule)
	ZEPHYR.Schedule = {};
  ZEPHYR.Schedule.assigneeUI = Backbone.Model.extend({});
  ZEPHYR.Schedule.assigneeView = Backbone.View.extend({
    events: {
      'click .assignee-dropDown-Trigger': 'showAssigneeDropdown',
      'keyup .ajax-input': 'getUsersList',
      'keydown .input-wrapper .ajax-input': 'traverseList',
      'hover .dropdown-list li': 'selectDropdownLi',
      'click .dropdown-list li': 'updateAssigneeUI',
      'click .remove-assignee': 'updateAssigneeUI',
      'click .ajax-input':      '_stopImmediatePropagation',
    },
    model: ZEPHYR.Schedule.assigneeUI,
    initialize: function(options){
      if (this.model) {
          this.model.on('change:renderAssigneeUI', this.render, this);
      }
    },
    _stopImmediatePropagation: function(ev) {
      ev.stopImmediatePropagation();
    },
    showAssigneeDropdown: function(ev) {
		ev.stopPropagation();
		ev.preventDefault();
		ev.stopImmediatePropagation();
      var scheduleId = ev.currentTarget.dataset.scheduleid;
      var field = ev.currentTarget.dataset.field;
      var dropDownContainer = AJS.$(this.$el).find('#execution-select-assignee-' + scheduleId);
      if(dropDownContainer.hasClass('active')) {
        dropDownContainer.removeClass('active');
      } else {
        dropDownContainer.addClass('active');
	    }
      AJS.$('#execution-select-assignee-' + scheduleId).find('input').focus();
    },
    
    updateAssigneeUI: function(ev) {
	    ev.stopPropagation();
	    ev.preventDefault();
	    ev.stopImmediatePropagation();
      var scheduleId = ev.currentTarget.dataset.scheduleid;
      var data = !ev.currentTarget.dataset.assignee ? {changeAssignee: true} : {
        assignee: ev.currentTarget.dataset.assignee,
        assigneeType: 'assignee',
        changeAssignee: true
      }

      if(!scheduleId || scheduleId == 'null') {
        var _assignee = {
          "executionId": null,
          "assignee": data.assignee,
          "assigneeType": data.assigneeType,
          "assigneeDisplay": ev.currentTarget.dataset.displayname,
        };
        assigneeUI.set('assignee', _assignee);
        assigneeUI.set('contextPath', contextPath);
        assigneeUI.set('renderAssigneeUI' , !assigneeUI.get('renderAssigneeUI'));
        return;
      }
      jQuery.ajax({
        url: getRestURL() + '/execution/' + scheduleId + '/execute',
        type : "PUT",
        data: JSON.stringify(data),
        contentType :"application/json",
        dataType: 'json',
        success: function(response) {
          //debugger;
			var _assignee = {
			"executionId": response.id,
			"assignee": response.assignedTo,
			"assigneeDisplay": response.assignedToDisplay,
			"assigneeUserName": response.assignedToUserName,
			"assigneeType": response.assigneeType
			};

			ZEPHYR.Schedule.Execute.updateExecutionHistory();
			assigneeUI.set('assignee', _assignee);
			assigneeUI.set('contextPath', contextPath);
			assigneeUI.set('renderAssigneeUI' , !assigneeUI.get('renderAssigneeUI'));


			// if (!window.standalone && AJS.$('#zqltext')[0] && AJS.$('#zqltext')[0].value.length != 0) {
			// 	setTimeout(function () {
			// 		ZEPHYR.ZQL.maintain = true;
			// 		if (AJS.$('#zephyr-transform-all') && AJS.$('#zephyr-transform-all')[0]) {
			// 			AJS.$('#zephyr-transform-all')[0].click();
			// 		}
			// 	}, 1000)
			// }

        }
      });
  },

  selectDropdownLi: function(ev) {
    var liOptions = AJS.$('.dropdown-list li');
    for (var i=0;i<liOptions.length;i++) {
          liOptions[i].classList.remove("active");
    }
    ev.target.classList.add("active");
    },

  traverseList: function(ev) {
    console.log(ev);
    //ev.preventDefault();
    if(ev.keyCode == 38 || ev.keyCode == 40 || ev.keyCode == 13) {
       
      var currentIndex = -1;
      var newIndex = 0;
      var liOptions = AJS.$('.dropdown-list li');
      for (var i=0;i<liOptions.length;i++) {
        if (liOptions[i].className.indexOf('active') > -1) {
          currentIndex = i;
          liOptions[i].classList.remove("active");
          break;
        }
      }

      if (ev.keyCode == 38) {
        newIndex =  Math.max(currentIndex - 1,0);
        liOptions[newIndex].classList.add("active");
      } else if (ev.keyCode == 40) {
        newIndex = Math.min(currentIndex + 1,liOptions.length - 1);
        liOptions[newIndex].classList.add("active");
      } else if (ev.keyCode == 13 && currentIndex > -1) {
        liOptions[currentIndex].click();
      }

      ev.stopPropagation();
      ev.preventDefault();
      ev.stopImmediatePropagation();

    }

  },

  getUsersList: function(ev) {
    ev.preventDefault();
    ev.stopPropagation();
    //console.log('test', ev, ev.target.value);
    if (!([38,40,13].indexOf(ev.keyCode) > -1)) {
      var scheduleId = ev.currentTarget.dataset.scheduleid;
      var dropDownContainer = AJS.$(this.$el).find('#execution-select-assignee-' + scheduleId);
      var restPath = "/rest/api/1.0/users/picker";
      jQuery.ajax({
        url: contextPath + restPath,
        type : "GET",
        data: {
          query: ev.target.value,
          showAvatar: true
        },
        contentType :"application/json",
        dataType: 'json',
        success: function(response) {
			if (AJS.$('#userName')[0] && AJS.$('#userName')[0].dataset && AJS.$('#userName')[0].dataset.user) {
				var existingName = AJS.$('#userName')[0].dataset.user;
				if (response.users && response.users.length > 0) {
					for (var counter = 0; counter < response.users.length; counter += 1) {
						if (response.users[counter].name == existingName) {
							response.users.splice(counter, 1)
							var tempTotal = response.total;
							response.total -= 1;
							response.footer = response.footer.replace(new RegExp(tempTotal.toString(), 'g'), response.total.toString());
							break;
						}
					}
				}
			}

			AJS.$(dropDownContainer).find('.dropdown-list').html("");
			if (response.users) {
				for (var counter = 0; counter < response.users.length; counter += 1) {
					if (response.users[counter].avatarUrl.indexOf('avatarId') < 0) {
						let tempAvatarNameArray = response.users[counter].displayName.split(" ");
						response.users[counter].userNameAvatar = '';
						if (tempAvatarNameArray[0] && tempAvatarNameArray[0][0]) {
							response.users[counter].userNameAvatar += tempAvatarNameArray[0][0];
						}
						if (tempAvatarNameArray[1] && tempAvatarNameArray[1][0]) {
							response.users[counter].userNameAvatar += tempAvatarNameArray[1][0];
						}
					}

				}
				var template = ZEPHYR.Templates.Execution.Assignee.executionAssigneeViewAjaxUI({
					users: response.users && response.users.length ? response.users : [],
					footer: response.footer,
					scheduleId: scheduleId
				});
				AJS.$(dropDownContainer).find('.dropdown-list').html(template);
			}
  		}
      });
    }
  },
  render: function() {
    var html = ZEPHYR.Templates.Execution.executionAssigneeDetailView({
      assignee:   this.model.get('assignee'),
      contextPath: this.model.get('contextPath')
    });
    AJS.$(this.$el).html(html);
    }
  });

var assigneeUI = new ZEPHYR.Schedule.assigneeUI({
    contextPath: '',
    assignee: {}
});

AJS.$(document).on("appendAssigneeUI", function(event, _assignee, contextPath, selector) {
  var assigneeUIInstance = new ZEPHYR.Schedule.assigneeView({
    el: selector ? selector : '#exec-assignee-wrapper',
    model: assigneeUI
  });
  assigneeUI.set('assignee', _assignee);
  assigneeUI.set('contextPath', contextPath);
  assigneeUIInstance.render();

});

ZEPHYR.Schedule.executionFieldView = Backbone.View.extend({
	events: {
		'click .cancel': 						'cancelUpdate',
		'keyup':								'cancelOnKeyup',
		'click .zfj-editable-field':			'editField',
		'click .zfj-editable-field a.assignee-user-hover':			'_stopImmediatePropagation',
		'focusout select':						'saveFieldOnUpdate',
		'click .status-dropDown-Trigger':    'openStatusDropdown',
		'click .updateStatus':     'updateStatus',
		'mousedown': 							"saveSelectedText",
		'click .zfj-field-button-save': 		"saveFieldOnClick",
		'change select' :  "saveFieldOnClick",
		'click #execution-assignee-remove' : "removeAssignee",
    "keydown span[id^=executionStatus-trigger]"         : "traverseStatus",
    "hover [id^=execution-field-dropDown-schedule] li"          : "highlightStatusLi",
	},

	initialize: function(options) {
		this.decorate();
		this._editDelay = 0;
    this.currentStatusIndx = -1;
	},

	_stopImmediatePropagation: function(ev) {
		ev.stopImmediatePropagation();
	},

	/**
	 * Decorate the editable container
	 */
	decorate: function() {
		this.$el.addClass("inactive");
	},

    _handleEditingStarted: function (focus) {
        // Ensure the field being focused is not disabled (throws an error in IE8)
        this.$el.find(":input").prop("disabled", false);

        this.$el.find(":input:visible:first").focus();

        this.$el.find('.update_cancel-buttons').attr('tabindex', 1);
    },

    /**
     * The view is put into saving mode and any input is disabled to prevent changes whilst the save request is in flight.
     * Display the loading cursor
     */
    _handleSavingStarted: function () {
    	this.$el.find(":input").prop("disabled", true);
        // Attaching the loading icon
        ZEPHYR.Loading.showLoadingIndicator();
    },

    /**
     * Transitions the view into the view state.
     */
    _displayWriteMode: function () {
        this.$el.addClass("inactive").removeClass("active");
        this.$el.find('.update_cancel-buttons').removeAttr('tabindex');
    },

	/**
     * Transitions the view into the edit state.
     */
    _displayEditMode: function() {
    	this.options.elBeforeEdit.hide();
    	this.options.elOnEdit.show();
    	this.$el.removeClass("inactive").addClass("active");
    	this._handleEditingStarted();
       // this.focus();
        this.containerHasFocus = true;
        this.trigger('editField');
    },

    focus: function () {
        // Ensure the field being focused is not disabled (throws an error in IE8)
        this.$el.find(":input").removeAttr("disabled");

        var $element = this.$el.find(":input:visible:first");
        $element.focus();
    },

    highlightStatusLi : function(e) {
      var statusLi = AJS.$('[id^=execution-field-dropDown-schedule] li');
           for(var i=0;i<statusLi.length;i++) {
            statusLi[i].classList.remove('active');
           }
      e.currentTarget.classList.add('active');
      this.currentStatusIndx = parseInt(e.currentTarget.dataset.index) || -1;
    },

    traverseStatus : function(e) {
      if(e.keyCode == 38 || e.keyCode == 40 || e.keyCode == 13) {
        this.currentStatusIndx = parseInt(this.currentStatusIndx);
             var newIndex = this.currentStatusIndx;
             var statusLi = AJS.$('[id^=execution-field-dropDown-schedule] li');
             for(var i=0;i<statusLi.length;i++) {
              statusLi[i].classList.remove('active');
             }

             if (e.keyCode == 38) {
               newIndex =  Math.max(this.currentStatusIndx - 1,0);
               if (statusLi[newIndex].className.indexOf('selected') > -1) {
                newIndex =  Math.max(this.currentStatusIndx - 2,0);
               }
               statusLi[newIndex].classList.add('active');
               AJS.$('.execution-field-dropDown-container.active')[0].scrollTop -= 23; //Hieght of li element
             } else if (e.keyCode == 40) {
               newIndex = Math.min(this.currentStatusIndx + 1,statusLi.length - 1);
               if (statusLi[newIndex].className.indexOf('selected') > -1) {
                if (newIndex == statusLi.length - 1) {
                  newIndex =  newIndex - 1;
                } else {
                  newIndex =  Math.min(this.currentStatusIndx + 2,statusLi.length - 1);
                }
               }
              statusLi[newIndex].classList.add('active');

               if (newIndex != 0) {
                AJS.$('.execution-field-dropDown-container.active')[0].scrollTop += 23;  //Hieght of li element
               }
             } else if (e.keyCode == 13 && this.currentStatusIndx > -1) {
               statusLi[this.currentStatusIndx].click();
               this.currentStatusIndx = -1;
             }
             this.currentStatusIndx = newIndex;
             e.preventDefault();
        }
    },


    /**
     * Cancels editing when an escape key is encountered
     * @param e {Event}
     */
    cancelOnKeyup: function (ev) {
        if(ev.keyCode === 27) {
            this.cancelUpdate(ev)
        }
    },

    /**
     * Cancels editing when cancel button is clicked.
     * @param e {Event}
     */
    cancelUpdate: function (ev) {
    	ev.preventDefault();
        ev.stopImmediatePropagation();

        this.containerHasFocus = false;
        this.trigger('cancelSaveField');
		AJS.$(this.options.elOnEdit).hide();
		AJS.$(this.options.elBeforeEdit).show();
		this._displayWriteMode();
    },

	/**
	 * If the field is updated, check if the element is focused or not
	 * If true trigger the save
	 */
 	openStatusDropdown: function(ev) {
		var scheduleId = ev.currentTarget.dataset.scheduleid;
		var dropDownContainer = AJS.$('#execution-field-dropDown-schedule-' + scheduleId);

    if(dropDownContainer.hasClass('active')) {
			AJS.$('.execution-field-dropDown-container').removeClass('active');
		} else {
			dropDownContainer.addClass('active');
		}
		ev.stopPropagation();
		ev.preventDefault();

 	},
	updateStatus: function(ev) {
		var scheduleId = (AJS.$(ev.currentTarget).parents('.dropDown-options-wrapper')[0].dataset.scheduleid);
		var newStatusId = ev.currentTarget.dataset.value;
		var color = ev.currentTarget.dataset.color;
		var content = ev.currentTarget.dataset.str;

		ZEPHYR.Schedule.performExecution(scheduleId, newStatusId, function() {
			var parentContainer = AJS.$('#executionStatus-value-schedule-' + scheduleId +' .execution_status_wrapper');
			parentContainer.find('.status-readMode').css("background",color).html(content);

			parentContainer.find('.execution-field-dropDown-container li').each(function(){
				if(this.dataset.value === newStatusId) {
					AJS.$(this).addClass('selected');
				} else {
					AJS.$(this).removeClass('selected');
				}
			})

			// if (!window.standalone && AJS.$('#zqltext')[0] && AJS.$('#zqltext')[0].value.length != 0) {
			// 	setTimeout(function () {
			// 		ZEPHYR.ZQL.maintain = true;
			// 		if (AJS.$('#zephyr-transform-all') && AJS.$('#zephyr-transform-all')[0]) {
			// 			AJS.$('#zephyr-transform-all')[0].click();
			// 		}
			// 	}, 1000)
			// }
		}, function() {});

		AJS.$('.execution-field-dropDown-container.active').removeClass('active');
		ev.stopPropagation();
		ev.preventDefault();
	},

	saveFieldOnUpdate: function(ev) {
		var timeout;
		var BLUR_FOCUS_TIMEOUT = 150;
		var instance = this;
		if(ev.target.getAttribute('data-issueId')) {
				issueIdOrKey = ev.target.getAttribute('data-issueId');
		}

		if(this.containerHasFocus) {
			if (timeout) clearTimeout(timeout);
            timeout = setTimeout(function(ev) {
            	if(instance.containerHasFocus && instance.$el.hasClass("active")) {
            		instance.trigger('saveField', ev);
            		instance._displayWriteMode();
            	}
            }, BLUR_FOCUS_TIMEOUT, ev);
		}

	},

	removeAssignee : function(ev) {
		ev.preventDefault();
		ev.stopPropagation();
		this.trigger('removeAssigneeOnClick', ev);
	},

	saveFieldOnClick: function(ev) {
		ev.preventDefault();
		this.trigger('saveFieldOnClick', ev);
	},

	editField: function(ev) {
	   ev.preventDefault();
       var iconWasClicked = AJS.$(ev.target).is(".zfj-overlay-icon");
       var isEditable = this.$el.hasClass("inactive");
       var noTextSelected = this._getCurrentlySelectedText() === "" && !this._previouslySelectedText;
       var shouldEnterEditMode = isEditable && (iconWasClicked || noTextSelected);

       if (this._editDelay !== 0) {
           // A click event was received while a pending inline edit request was
           // in progress. Cancel this request, since the user is double-clicking
           // or triple-clicking instead.
           clearTimeout(this._editDelay);
           this._editDelay = 0;

       } else if (shouldEnterEditMode) {
           var self = this;

           // Wait briefly to allow the user the opportunity to double-click.
           // Double-clicks will abort the pending transition to inline edit mode.
           this._editDelay = setTimeout(function() {
               if (self.$el.hasClass("inactive") && self._getCurrentlySelectedText() === "") {
                   self._displayEditMode();
               }
               self._editDelay = 0;
           }, 250);
       }
   },

   _getCurrentlySelectedText: function() {
       if (jQuery(document.activeElement).is(":input")) {
           // Text selections inside form elements are not considered.
           return "";
       }
       if (document.selection && document.selection.createRange) {
           return document.selection.createRange().text || "";
       }
       if (window.getSelection) {
           return window.getSelection().toString();
       }
       return "";
   },

	// Remove the loading icon, hide the dropdown and display the updated status
	updateUIOnAction: function() {
		ZEPHYR.Loading.hideLoadingIndicator();
		AJS.$(this.options.elOnEdit).hide();
		AJS.$(this.options.elBeforeEdit).show();
		// Removing the active class on the cycle component's
		this.$el.closest('#issuetable').siblings('.zfj-notifications').removeClass('active');
	},

	/**
     * Text selection is cleared after mousedown; this method stores it in an
     * instance variable so we can determine in onEdit if selection was cleared.
     */
    saveSelectedText: function() {
        this._previouslySelectedText = this._getCurrentlySelectedText();
    }
});

/*
 * ---------------------------------------------------
 * Functions and view related to execution Status
 * ---------------------------------------------------
 */
ZEPHYR.Schedule.performExecution = function(scheduleId, newStatusId, successCallback, errorCallback) {
	var req = jQuery.ajax({
		url: getRestURL() + "/execution/"+ scheduleId + "/execute",
		type : "PUT",
		contentType :"application/json",
		dataType: "json",
		data:JSON.stringify( {
			'status': newStatusId,
			'changeAssignee': false
		}),
		success : function(response) {
			var execution = response;
			var statusMap = {};
      if (execution.executionSummaries) {
  			JSON.parse(execution.executionSummaries).executionSummary.map(function(status) {
  				statusMap[status.statusKey] = {
  					name: status.statusName ,
  					id: status.statusKey,
  					color: status.statusColor,
  					description: status.statusDescription || '',
  				}
  			});
  			ZEPHYR.Execution.currentSchedule = {
  				'statusObj': {
  					currentExecutionStatus: {
  						color: statusMap[execution.executionStatus].color,
  						description: statusMap[execution.executionStatus].description || '',
  						id: statusMap[execution.executionStatus].id,
  						name: statusMap[execution.executionStatus].name,
  					},
  					executionStatuses: statusMap,
  				}
  			}
      }
			if(AJS.$('.zephyr-tb-issue-compact .zephyr-tb-versions-table-progress').length > 0) {
				var progressBar = AJS.$('#issueBoard-progressBar-' + issueIdOrKey);
				var totalExecutions =  AJS.$('#issueBoard-progressBar-' + issueIdOrKey).find('.zephyr-tb-version-progress-percentage').attr('totalExecutions');
				var totalExecuted = 0;
				var summaryList = {};
				if(response.executionSummaries) {
					var executionSummaries = JSON.parse(response.executionSummaries);
					executionSummaries.executionSummary.forEach(function(status){
						summaryList[status.statusKey] = 0;
					});

					if(newStatusKey >= -1) {
						summaryList[newStatusKey]++;
					}

					AJS.$('#issueBoard-progressBar-' + issueIdOrKey + ' .zephyr-tb-progressbar-entry').each(function(){
						summaryList[AJS.$(this).attr('statuskey')] = summaryList[AJS.$(this).attr('statuskey')] + Number(AJS.$(this).attr('statusCount'));
					});

					if(oldStatusKey >= -1) {
						summaryList[oldStatusKey]--;
					}

					executionSummaries.executionSummary.forEach(function(status){
						status.count = summaryList[status.statusKey];
						if(status.statusKey !== -1) {
							totalExecuted = totalExecuted + summaryList[status.statusKey];
						}
					});

					progressBar.html(ZEPHYR.Templates.TestBoard.versionProgressHTML({
						summaryList: executionSummaries,
						totalExecutions: totalExecutions
					}));
					progressBar.append('<span totalExecuted='+ totalExecuted +' totalExecutions='+ totalExecutions + ' class="zephyr-tb-version-progress-percentage">' + ((totalExecuted*100/totalExecutions).toFixed(2)) + '%</span>')
				}
			}

			//We need to update status only for Schedules. In case of step execution, we dont show it right now. Hence those elements will not be present.
			if(AJS.$("#executed-by-schedule-" + scheduleId).length > 0){
				var _executedBy = AJS.$("#executed-by-schedule-" + scheduleId)
				var _executedOn = AJS.$("#executed-on-schedule-" + scheduleId)
				if(execution.executedBy != null) {
					AJS.$("#executed-by-schedule-"+ scheduleId).parent().show();
          AJS.$("#executed-on-schedule-"+ scheduleId).parent().show();
            
					var executedByString = "<a href='"+ contextPath +"/secure/ViewProfile.jspa?name="+ execution.executedBy +"' rel='"+ execution.executedBy +"' class='user-hover'>" + AJS.escapeHtml(execution.executedByDisplay) + "</a>";
					_executedBy.html("<strong>" + executedByString + "</strong>");
					_executedOn.html("<strong>" + execution.executedOn + "</strong>");
					_executedBy.fadeTo(1000, 0.5, function() {AJS.$(this).css('opacity', ''); AJS.$(this).html(executedByString)});
					_executedOn.fadeTo(1000, 0.5, function() {AJS.$(this).css('opacity', ''); AJS.$(this).html(execution.executedOn)});
				}
				else{
					//AJS.$("#executed-by-"+ source.sourceType + "-" + source.sourceId).parent().hide();
					_executedBy.html("");
					_executedOn.html("");
				}
        if(globalzqlSearch){
          if(globalzqlSearch.get('status')[response.executionStatus]){
            AJS.$("#exe-status-"+ scheduleId).css("border-color", globalzqlSearch.get('status')[response.executionStatus].color);
            AJS.$("#exe-status-"+ scheduleId).text(globalzqlSearch.get('status')[response.executionStatus].name);
            for (var i = 0; i < cycleObj.length; i++) {
              if(cycleObj[i].id == response.id){
                cycleObj[i].status = globalzqlSearch.get('status')[response.executionStatus];
                break;
              }
            }
          }
        }
			}
			/*Update the model - only applicable for ExecuteTest*/
			if(ZEPHYR.Schedule && ZEPHYR.Schedule.Execute && ZEPHYR.Schedule.Execute.currentSchedule){
				ZEPHYR.Schedule.Execute.currentSchedule.set(execution)
			}

			if(successCallback){
				successCallback();
			}

      AJS.$(document).trigger( "updateExecutionModel", [response] );
			showExecStatusSuccessMessage();
      AJS.$(document).trigger( "updateGridAndTree");
            //for analytics
            var zaObj = {
                'event': ZephyrEvents.EXECUTE,
                'eventType': 'Change',
                'executionId': scheduleId,
                'executionStatus': newStatusId
            };
            if (za != undefined){
                za.track(zaObj, function(){
                    //res
                })
			}
			for (var counter = 0; counter < AJS.$('#exec_status-schedule-' + scheduleId + ' li').length; counter += 1) {
				AJS.$('#exec_status-schedule-' + scheduleId + ' li')[counter].classList.remove('selected');
				if (parseInt(AJS.$('#exec_status-schedule-' + scheduleId + ' li')[counter].dataset.value) == newStatusId) {
					AJS.$('#exec_status-schedule-' + scheduleId + ' li')[counter].classList.add('selected');
				}
			}
		},
		error: function(response) {
			// Hide the loading indicator on error
      // Callback to handle status 403's error condition
      if(response.status == 403 && errorCallback)
        errorCallback();
			ZEPHYR.Loading.hideLoadingIndicator();
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
			}
			if(response && typeof buildExecutionError == 'function')
				buildExecutionError(response);
			else if(response.status == 401){
				var dialog = new AJS.Dialog({
		 		    width:540,
		 		    height: 200,
		 		    id:	"dialog-error"
		 		});
			 	dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

			 	dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
			 	AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
				    body: AJS.I18n.getText('zapi.login.error'),
				    closeable: false
				});

			 	dialog.addLink("Close", function (dialog) {
			 	    dialog.hide();
			 	}, "#");
		 		dialog.show();
			}
			if(errorCallback)
				errorCallback();
		}
	});
	// // Callback to handle status 403's error condition
	// req.error(function(jHR) {
	// 	if(jHR.status == 403 && errorCallback)
	// 		errorCallback();
	// });
}

ZEPHYR.Schedule.stripTextIfMoreThanAllowed = function(selector, selectedText, maxlength, title) {
	AJS.$(selector).css("text-overflow", "ellipsis");
	// AJS.$(selector).css("overflow", "hidden");
	AJS.$(selector).css("white-space", "nowrap");
	if(title)
		AJS.$(selector).attr("title", title);
}

ZEPHYR.Schedule.getSourceInfoForStatusExecution = function(eventSourceId){
	//Execution Status element DIV ids are created in following manner
	//execution-field-current-status-<sourceType>-<sourceId>, exec_status-<sourceType>-<sourceId>
	//execution-field-select-<sourceType>-<sourceId>
	//current-execution-status-dd-<sourceType>-<sourceId>
	//executionStatus-labels-<sourceType>-<sourceId>
	//where sourceType -> "schedule" OR "step" and sourceId -> scheduleId OR teststepId

	//when user clicks on pencil icon, executionStatus-labels-<sourceType>-<sourceId> get's available in event source ID
	//so we will return the last two tokens from this.
	if(eventSourceId) {
        var sourceElementArray = eventSourceId.split("-");
        var sourceTag = sourceElementArray[sourceElementArray.length - 2] + "-" + sourceElementArray[sourceElementArray.length - 1];
        var sourceId = sourceElementArray[sourceElementArray.length - 1];
        var sourceType = sourceElementArray[sourceElementArray.length - 2];
        return {sourceTag: sourceTag, sourceId: sourceId, sourceType: sourceType}
    }
}

ZEPHYR.Schedule.executionStatusView = ZEPHYR.Schedule.executionFieldView.extend({
	initialize: function() {
		_.bindAll(this, 'removeDOMListeners');
		this.bind('saveField', this.saveExecutionStatus, this);
		this.bind('cancelSaveField', this.cancelStatusUpdate, this);
		ZEPHYR.Schedule.executionFieldView.prototype.initialize.call(this);
		this.source = ZEPHYR.Schedule.getSourceInfoForStatusExecution(this.options.elOnEdit.attr('id'));
		// Remove event handlers associated to the view when element is removed from the DOM.
		this.$el.on("remove", this.removeDOMListeners);
	},

	removeDOMListeners: function() {
	    this.unbind();
	    // Check for older Backbone versions which does not have this.stopListening()
	    if (typeof this.stopListening == 'function')
	    	this.stopListening();
	},

	cancelStatusUpdate: function(ev) {
		var readOnlyStatusDiv = AJS.$("#execution-field-current-status-" + this.source.sourceTag);
		AJS.$("#exec_status-" + this.source.sourceTag).val(readOnlyStatusDiv.find("a span").attr('rel'));
	},

	saveExecutionStatus: function() {
		var source						= this.source;
		var EXEC_STATUS_TEXT_MAX_LENGTH	= 12;
		var instance = this;

		this._handleSavingStarted();

		var readOnlyStatusDiv = AJS.$("#execution-field-current-status-" + source.sourceTag);
		if(!readOnlyStatusDiv.length)
			readOnlyStatusDiv = AJS.$('a#executionStatus-labels-' + source.sourceTag)
		var dropdown = AJS.$("#exec_status-"+ source.sourceTag);
		var oldStatusId = readOnlyStatusDiv.find("a span").attr('rel') || AJS.$('a#executionStatus-labels-' + source.sourceTag).find("span").attr('rel');
		var newStatus = AJS.$("#exec_status-" + source.sourceTag +" :selected").text();
		var newStatusId = AJS.$("#exec_status-" + source.sourceTag +" :selected").val();
		if(newStatus.indexOf('<') > -1) {
			newStatus = newStatus.replace(/</gi, '&lt;');
		}
		if(newStatus.indexOf('>') > -1) {
			newStatus = newStatus.replace(/>/gi, '&gt;');
		}
		if(oldStatusId == newStatusId){
			this.updateUIOnAction();
			console.log('Nothing saved, hence skipping save');
			return;
		}
		oldStatusKey = oldStatusId;
		newStatusKey = newStatusId;
		var statusDescription = AJS.$("#exec_status-" + source.sourceTag +" :selected").attr("title");

		var color = AJS.$("#exec_status-" + source.sourceTag +" option:selected").attr("rel");
		var currentExecutionStatusDD = AJS.$("#current-execution-status-dd-" + source.sourceTag )
		currentExecutionStatusDD.css("background-color", color);
		currentExecutionStatusDD.html(newStatus);
		readOnlyStatusDiv.find("a span").attr('rel', newStatusId);
		AJS.$('a#executionStatus-labels-' + source.sourceTag).find("span").attr('rel', newStatusId);

		//alert Text if greater than allowed
		var title = newStatus + (statusDescription ? ": " + statusDescription : '');
		ZEPHYR.Schedule.stripTextIfMoreThanAllowed("#current-execution-status-dd-" + source.sourceTag, newStatus, EXEC_STATUS_TEXT_MAX_LENGTH, title);

		this.$el.closest('#issuetable').siblings('.zfj-notifications').addClass('active');
		//Execute given test step or schedule to change the status for the same.
		ZEPHYR.Schedule.performExecution(source.sourceId, newStatusId, function(){
			var cycleListElement = currentExecutionStatusDD.parents("li[id^='cycle']");
			if(cycleListElement.length > 0){
				//jQuery(document).trigger("ScheduleUpdated", response);
				AJS.$(cycleListElement).trigger('refresh');
			}
			instance.updateUIOnAction();

			/* ExecNavigator listen to this event for quick execution
			 * Need to remove this once we internalize statusUpdate inside the backbone views
			 */
			if(typeof globalDispatcher != 'undefined'){
				globalDispatcher.trigger('scheduleUpdated');
			} else {
				jQuery(document).trigger("ScheduleUpdated", source.sourceId);
			}
		}, function() {
			var oldStatusEl = dropdown.find('option[value=' + oldStatusId+ ']');
			dropdown.val(oldStatusId);
			currentExecutionStatusDD.css("background-color", oldStatusEl.attr('rel'));
			currentExecutionStatusDD.html(oldStatusEl.text());
			readOnlyStatusDiv.find("a span").attr('rel', oldStatusId);
			instance.updateUIOnAction();
		});
	}
});

ZEPHYR.Schedule.executionAssigneeView = ZEPHYR.Schedule.executionFieldView.extend({
	initialize: function() {
		_.bindAll(this, 'removeDOMListeners');
		this.bind('editField', this.attachExecutionAssignee, this);
		this.bind('saveFieldOnClick', this.saveExecutionAssignee, this);
		this.bind('removeAssigneeOnClick', this.removeExecutionAssignee, this);
		this.bind('cancelSaveField', this.cancelStatusUpdate, this);
		ZEPHYR.Schedule.executionFieldView.prototype.initialize.call(this);
		this.source = ZEPHYR.Schedule.getSourceInfoForStatusExecution(this.options.elOnEdit.attr('id'));
		// Remove event handlers associated to the view when element is removed from the DOM.
		this.$el.on("remove", this.removeDOMListeners);
	},

	removeDOMListeners: function() {
	    this.unbind();
	    // Check for older Backbone versions which does not have this.stopListening()
	    if (typeof this.stopListening == 'function')
	    	this.stopListening();
	},

	cancelStatusUpdate: function(ev) {
		var readOnlyStatusDiv = AJS.$("#execution-field-current-assignee-" + this.source.sourceTag);
		AJS.$('input[name=zephyr-je-execution-assignee-quick-' + this.source.sourceId + '-type]').removeAttr('checked');
		AJS.$("#exec_assignee-" + this.source.sourceTag).val(readOnlyStatusDiv.find("a span").attr('rel'));
		this.options.elOnEdit.find('#assignee-error').html('');
	},

	updateAssigneeUIOnSave: function(response) {
		var _executionAssignee = '&nbsp;';

		if(response.assignedToUserName) {
			_executionAssignee = '<a href="{$contextPath}/secure/ViewProfile.jspa?name=' + response.assignedToUserName +'" rel="' + response.assignedToUserName +'" class="assignee-user-hover user-hover">' + response.assignedToDisplay +'</a>';
		} else {
			if(response.assignedToDisplay)
				_executionAssignee = response.assignedToDisplay;
			else if(response.assignedTo)
				_executionAssignee = response.assignedTo;
		}
		if(!response.assignedToUserName && !response.assignedToDisplay && !response.assignedTo) {
			this.options.elBeforeEdit.find('#execution-assignee-label')
				.html('')
				.attr('data-assigneeUserName', (response.assignedToUserName || null))
				.attr('data-assigneeType', (response.assigneeType || null));
		} else {
			this.options.elBeforeEdit.find('#execution-assignee-label')
				.html('<div class="execution-assignee-wrapper"><strong>' + _executionAssignee + '</strong><span id="execution-assignee-remove" class="aui-icon aui-icon-small aui-iconfont-remove-label"></span></div>')
				.attr('data-assigneeUserName', (response.assignedToUserName || null))
				.attr('data-assigneeType', (response.assigneeType || null));
		}


		AJS.$('#executiondetails-refresh').trigger('click');
		this._displayWriteMode();
		this.updateUIOnAction();
	},

	saveExecutionAssignee: function() {
		var assigneeParams = ZEPHYR.Execution.AssigneeNewUI.getSelectedParams('zephyr-je-execution-assignee-quick-' + this.source.sourceId),
			instance = this;

		if(!assigneeParams)
    		return;

		assigneeParams['changeAssignee'] = true; // Set flag changeAssignee
		var req = jQuery.ajax({
			url: getRestURL() + "/execution/"+ instance.source.sourceId + "/execute",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify(assigneeParams),
			success : function(response) {
				instance.updateAssigneeUIOnSave(response);
				AJS.$(document).trigger( "updateExecutionModel", [response] );
			},
			error: function(response) {
				AJS.$('#exec_assignee_cancel-schedule-' + instance.source.sourceId).click();
				if(response && typeof buildExecutionError == 'function')
					buildExecutionError(response);
				else if(response.status == 401){
					var dialog = new AJS.Dialog({
			 		    width:540,
			 		    height: 200,
			 		    id:	"dialog-error"
			 		});
				 	dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

				 	dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
				 	AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
					    body: AJS.I18n.getText('zapi.login.error'),
					    closeable: false
					});

				 	dialog.addLink("Close", function (dialog) {
				 	    dialog.hide();
				 	}, "#");
			 		dialog.show();
				}
			}
		});
		// Callback to handle status 403's error condition
		// req.error(function(jHR) {
		// 	if(jHR.status == 403)
		// 		AJS.$('#exec_assignee_cancel-schedule-' + instance.source.sourceId).click();
		// });
	},

	removeExecutionAssignee : function() {
		var assigneeParams = {},
			instance = this;

		if(!assigneeParams)
    		return;

		assigneeParams['changeAssignee'] = true; // Set flag changeAssignee
		var req = jQuery.ajax({
			url: getRestURL() + "/execution/"+ instance.source.sourceId + "/execute",
			type : "PUT",
			contentType :"application/json",
			dataType: "json",
			data: JSON.stringify(assigneeParams),
			success : function(response) {
				instance.updateAssigneeUIOnSave(response);
				AJS.$(document).trigger( "updateExecutionModel", [response] );
			},
			error: function(response) {
				AJS.$('#exec_assignee_cancel-schedule-' + instance.source.sourceId).click();
				if(response && typeof buildExecutionError == 'function')
					buildExecutionError(response);
				else if(response.status == 401){
					var dialog = new AJS.Dialog({
			 		    width:540,
			 		    height: 200,
			 		    id:	"dialog-error"
			 		});
				 	dialog.addHeader(AJS.I18n.getText('zephyr.je.submit.form.error.title'));

				 	dialog.addPanel("panel-1", "<p></p>", "dialog-error-panel-body");
				 	AJS.messages.error(AJS.$('.dialog-error-panel-body'), {
					    body: AJS.I18n.getText('zapi.login.error'),
					    closeable: false
					});

				 	dialog.addLink("Close", function (dialog) {
				 	    dialog.hide();
				 	}, "#");
			 		dialog.show();
				}
			}
		});
	},

	attachExecutionAssignee: function() {
		this.options.elOnEdit.css('display', 'inline-block').find('#assignee-error').html('');
		var assigneeType = this.options.elBeforeEdit.find('#execution-assignee-label').attr('data-assigneeType'),
			assigneeeJSON = null;

		if(assigneeType) {
			var assignee = this.options.elBeforeEdit.find('#execution-assignee-label').attr('data-assigneeUserName');
			assigneeeJSON = {
				assigneeType: assigneeType,
				assignee: assignee
			}
		}
    	// Attach Assignee user picker
		ZEPHYR.Execution.AssigneeNewUI.init({
			id: 'zephyr-je-execution-assignee-quick-' + this.source.sourceId,
			clear: true,
			assigneeeJSON: assigneeeJSON
		});
		this.initFirstAttempt = true;
	}
});

AJS.$('body').live('click',function(ev) {
	if(AJS.$('.dropDown-container.active').length > 0 || AJS.$('.droplist.active').length > 0 ) {
		AJS.$('.dropDown-container.active, .droplist.active').each(function(){
			AJS.$(this).removeClass('active');
		})
	}
})
