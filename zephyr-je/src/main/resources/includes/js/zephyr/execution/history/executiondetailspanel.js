if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
if(typeof ZEPHYR.Execution == 'undefined') ZEPHYR.Execution = {};
if(typeof ZEPHYR.Execution.History == 'undefined') ZEPHYR.Execution.History = {};
ZEPHYR.Execution.History.field = {
	EXECUTED_ON:	'executed_on',
	EXECUTED_BY: 	'executed_by'
};
ZEPHYR.Execution.History.Order = {
	ASCENDING: 'ascending',
	DESCENDING: 'descending'
};

// var editingFieldId = null;
// var currentType = 'mini';
var minLogsToShow = 2;
ZEPHYR.Execution.History.ExecutionAudit = Backbone.Model.extend({
	isFieldExecutedByOrOn: function() {
		if(this.get('auditItems') && this.get('auditItems').field) {
			var field = this.get('auditItems').field;
			return (field.indexOf(ZEPHYR.Execution.History.field.EXECUTED_BY) != -1 || field.indexOf(ZEPHYR.Execution.History.field.EXECUTED_ON) != -1);
		} else
			return false;
	}
});

ZEPHYR.Execution.History.ExecutionAuditCollection = Backbone.Collection.extend({
    model:ZEPHYR.Execution.History.ExecutionAudit,
    url: function(){
    	return getRestURL() + "/audit?entityType=EXECUTION,EXECUTION_WORKFLOW&event=EXECUTION_UPDATED,EXECUTION_WORKFLOW_UPDATED,EXECUTION_CUSTOMFIELD_UPDATED";
    },
    parse: function(resp, xhr){
    	return resp.auditLogs;
    }
});

ZEPHYR.Execution.History.ExecutionAuditDetailsDialogView = Backbone.View.extend({
	initialize: function(options) {
        this.currentIssueId = options.currentIssueId;
        this.executionId = options.executionId;
	},

	displayDialog: function() {
		var instance = this;
    	if(AJS.dialog2) {
			var label = AJS.I18n.getText('execute.test.execution.history.subheader.label'),
	    		dialogButtons = [
	 		        {
	 		        	id : 'zephyr-je-dialog-close',
	 		            name : AJS.I18n.getText('zephyr-je.close.link.title'),
	 		            type : 'link'
	 		        }
	 		     ],
	 		     dialogContentHTML = '<div id="execution-history-dialog-container" />',
	 		     dialogId = "execution-history-id"
	 		     dialogRenderHTML = ZEPHYR.Templates.Dialog.renderDialog({
			 				"dialogId" : dialogId,
			 				"dialogSize" : "aui-dialog2-xlarge z-dialog2-xlarge",
			 				"dialogHeader" : label,
			 				"dialogContent" : dialogContentHTML,
			 				"dialogButtons" : dialogButtons,
			 				isModal: true
	 		     });

 			AJS.$('body').append(dialogRenderHTML);
 			var executionHistoryDialog = AJS.dialog2('#' + dialogId); // Create Dialog2 component
 			executionHistoryDialog.show();
 			executionHistoryDialog.$el.find('.aui-dialog2-content').addClass('zfj-execution-history-dialog-content');
			executionHistoryDialog.$el.find('.aui-dialog2-content').prepend(ZEPHYR.About.evalMessage());
 			// Close link
 			executionHistoryDialog.$el.find('#zephyr-je-dialog-close').click(function(ev) {
 		        ev.preventDefault();
 		        executionHistoryDialog.hide();
 		        instance.remove();
 		    });
		} else {
			var dialog = new JIRA.FormDialog({
				id: "execution-history-id-dialog",
				width: '80%',
				height: '70%',
				content: function (callback) {
					callback(ZEPHYR.Execution.Templates.ExecutionDetails.exectionAuditHistoryDialog());
				}
			});
			dialog.show();
			dialog.$buttonContainer.find('.dialog-close').click(function(ev) {
				AJS.$('#execution-history-id-dialog').remove();
				instance.remove();
			});
			dialog.$popup.find('#execution-history-dialog-container').css('height', (AJS.$(window).height() - 300));
		}
	},

	render: function() {
		var instance = this;

		this.displayDialog();
		this.executionAuditList = new ZEPHYR.Execution.History.ExecutionAuditCollection();
		this.executionAuditDetailsView = new ZEPHYR.Execution.History.ExecutionAuditDetailsView({
			el: '#execution-history-dialog-container',
			currentIssueId: this.currentIssueId,
			executionId: 	this.executionId,
			isDialog: 		true,
			maxAllowed: 	20,
			model: 			this.executionAuditList
		});
		this.executionAuditDetailsView.render();
	}
});

ZEPHYR.Execution.History.ExecutionAuditDetailsView = Backbone.View.extend({
	initialize: function(options) {
		_.bindAll(this, 'getAuditDetails', 'applyData', 'displayAuditDetailsOnShowMore', 'refreshExecutionDetails', 'displayDialogOnShowMore', 'showMoreAuditLogs', 'fetchPrevAuditLogs','fetchNextAuditLogs');
        this.currentIssueId = options.currentIssueId;
        this.executionId = options.executionId;
        this.maxAllowed = options.maxAllowed;
        this.order = ZEPHYR.Execution.History.Order.DESCENDING;
        this.resetData();
        this.isDialog = options.isDialog;
	},

    getAuditData: function() {
    	return {
            maxRecords: this.maxRecords,
            offset: 	this.offset,
            issueId: 	this.currentIssueId,
            executionId:this.executionId
        };;
    },

	getAuditDetails: function(data, callback) {
		var instance = this;

		this.model.fetch({
			data: data,
			success: function(model, response) {
				instance.auditExecutionHistory.totalItemsCount = response.totalItemsCount;
				if(callback)
					callback();
			},
			error: function(model, response) {
				if(response.status == 401){
					if(instance.isDialog) {
						AJS.messages.error(AJS.$('#execution-history-dialog-container'), {
						    body: AJS.I18n.getText('zapi.login.error'),
						    closeable: false
						});
					} else {
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
				} else if(response && typeof buildExecutionError == 'function')
					buildExecutionError(response);
			}
		});
	},

	getAuditLogsFromList: function() {
		var auditLogs = [],
			instance = this;
		if(!this.isDialog) {
			_.each(this.model.models, function(model) {
		        if(!model.isFieldExecutedByOrOn() && auditLogs.length < minLogsToShow) {
					auditLogs.push(model.toJSON());
		        }
		    });
		} else {
			auditLogs = this.model.toJSON();
		}
		return auditLogs;
	},

	getShowMore: function() {
		//if(this.count < this.auditExecutionHistory.totalItemsCount)
    if(this.count > 0)
			return true;
		else
			false;
	},

	showMoreAuditLogs : function(ev) {
		ev.preventDefault();

		if(AJS.$(ev.target).hasClass('expanded')) {
			minLogsToShow = 5;
		} else {
			minLogsToShow = 10;
		}
		this.applyData();
	},

	fetchNextAuditLogs : function(ev) {
		var instance = this;
		ev.preventDefault();
		this.offset = this.offset + 20;
		var data = this.getAuditData();
		this.getAuditDetails(data, function() {
        	instance.applyData();
        });
	},
	fetchPrevAuditLogs : function(ev) {
		var instance = this;
		ev.preventDefault();
		this.offset = this.offset - 20;
		var data = this.getAuditData();
		this.getAuditDetails(data, function() {
        	instance.applyData();
        });
	},

	/* Idempotent method, cant be called multiple times */
	applyData: function() {
		var auditLogs = this.getAuditLogsFromList(),
			instance = this;
		this.count = auditLogs.length;
		var showMore = this.getShowMore();
		var url = window.location.href;
		var oldValue = false;
		var expandMore = true;
		var titleForSort = AJS.I18n.getText('execute.test.execution.history.descending.label');
		var titleForRefresh = AJS.I18n.getText('execute.test.execution.history.refresh.label');
		if(url.indexOf("/enav/") >= 0) {
			oldValue = true;
		}
		if(minLogsToShow === 10) {
			expandMore = false;
		}

		for (let counter = 0; counter < auditLogs.length; counter += 1) {
			let tempNameArray = auditLogs[counter].creator.split(" ");
			if (tempNameArray[0] && tempNameArray[0][0]) {
				auditLogs[counter].avatarName = tempNameArray[0][0];
			}
			if (tempNameArray[1] && tempNameArray[1][0]) {
				auditLogs[counter].avatarName = tempNameArray[1][0];
			}
		}

		for (counter = 0; counter < auditLogs.length; counter += 1) {
			if (auditLogs[counter].avatarUrl.indexOf('avatarId') < 0) {
				let tempAvatarNameArray = auditLogs[counter].creator.split(" ");
				let tempName = '';
				if (tempAvatarNameArray[0] && tempAvatarNameArray[0][0]) {
					tempName += tempAvatarNameArray[0][0];
				}
				if (tempAvatarNameArray[1] && tempAvatarNameArray[1][0]) {
					tempName += tempAvatarNameArray[1][0];
				}
				auditLogs[counter].userNameAvatar = tempName;
			}

		}
		titleForSort = (instance.order === ZEPHYR.Execution.History.Order.DESCENDING) ? AJS.I18n.getText('execute.test.execution.history.descending.label') :  AJS.I18n.getText('execute.test.execution.history.ascending.label');
		var htmlData = ZEPHYR.Execution.Templates.ExecutionDetails.addExecutionDetailsHistory({
	            auditLogs: 		auditLogs,
	            showMore: 		showMore,
	            showHeader:		!this.isDialog,
	            contextPath:	contextPath,
							oldValue:	oldValue,
							isDialog : this.isDialog,
							totalCount: this.auditExecutionHistory.totalItemsCount,
							currentIndex: this.offset,
							titleForSort: titleForSort,
							titleForRefresh: titleForRefresh
			});


		currentIssueId = this.currentIssueId;

		this.$el.html(htmlData);
		this.$el.find('#audit-history-detail-mode').unbind('click');
		this.$el.find('#audit-history-detail-mode').bind('click', this.displayDialogOnShowMore);
		if(this.isDialog) {
        	this.$el.find('#audit-history-show-more').unbind('click');
        	this.$el.find('#audit-history-show-more').bind('click', this.displayAuditDetailsOnShowMore);
        	this.$el.find('#audit-history-expand').unbind('click');
			this.$el.find('#prev-page-execution-detail').bind('click', this.fetchPrevAuditLogs);
			this.$el.find('#audit-history-expand').unbind('click');
			this.$el.find('#next-page-execution-detail').bind('click', this.fetchNextAuditLogs);
        } else {
        	this.$el.find('#audit-history-show-more').unbind('click');
        	this.$el.find('#audit-history-show-more').bind('click', this.displayDialogOnShowMore);
					this.$el.find('#audit-history-expand').unbind('click');
					this.$el.find('#audit-history-expand').bind('click', this.showMoreAuditLogs);
        }
        this.$el.find('#executiondetails-refresh').unbind('click');
        this.$el.find('#executiondetails-refresh').bind('click', this.refreshExecutionDetails);
        this.$el.find('.execution-history-sort').unbind('click');
    	this.$el.find('.execution-history-sort').bind('click', function(ev) {
    		ev.preventDefault();

			instance.displayExecutionLogs();

			var _targetOrder = instance.order;

    		if(_targetOrder == ZEPHYR.Execution.History.Order.DESCENDING) {
    			// AJS.$(ev.target).removeClass('zephyr-icon-sort-up')
    			// 	.addClass('zephyr-icon-sort-down')
    			// 	.attr('title', AJS.I18n.getText('execute.test.execution.history.ascending.label'));
    			// AJS.$(ev.target).find('span').html(AJS.I18n.getText('execute.test.execution.history.ascending.label'));
    			instance.order = ZEPHYR.Execution.History.Order.ASCENDING;
    		} else if(_targetOrder == ZEPHYR.Execution.History.Order.ASCENDING) {
    			// AJS.$(ev.target).removeClass('zephyr-icon-sort-down')
				// 	.addClass('zephyr-icon-sort-up')
				// 	.attr('title', AJS.I18n.getText('execute.test.execution.history.descending.label'));
    			// AJS.$(ev.target).find('span').html(AJS.I18n.getText('execute.test.execution.history.descending.label'));
    			instance.order = ZEPHYR.Execution.History.Order.DESCENDING;
    		}
    	});
    },

    displayExecutionLogs: function($wrapperEl) {
		var instance = this;
		var data = this.getAuditData();
		data.orderBy = (instance.order === ZEPHYR.Execution.History.Order.DESCENDING) ? 'ASC' : 'DESC';
		this.getAuditDetails(data, function() {
			instance.applyData();
		});
    },

	render: function() {
		this.displayAuditDetailsOnInit();
	},

    displayAuditDetailsOnInit: function() {
        var instance = this;

        this.auditExecutionHistory = {"id": this.currentIssueId};
        var data = this.getAuditData();
        this.getAuditDetails(data, function() {
        	instance.applyData();
        });
    },

    displayDialogOnShowMore: function(ev) {
    	ev.preventDefault();

    	this.executionAuditDetailsDialogView = new ZEPHYR.Execution.History.ExecutionAuditDetailsDialogView({
    		currentIssueId: this.currentIssueId,
    		executionId: this.executionId
    	});
    	this.executionAuditDetailsDialogView.render();
    },

    displayAuditDetailsOnShowMore: function(ev) {
		ev.preventDefault();
		var instance = this;
		this.offset = this.offset + this.maxRecords;
		var data = this.getAuditData();
		this.getAuditDetails(data, function() {
			var auditLogs = instance.getAuditLogsFromList();
			instance.count += auditLogs.length;
	        var showMore = instance.getShowMore();
	        instance.$el.find('#executiondetails-wrapper').append(ZEPHYR.Execution.Templates.ExecutionDetails.addAuditHistory({
				auditLogs: 		auditLogs,
				displayHeader: 	false,
				contextPath:	contextPath
			}));
			if(!showMore) instance.$el.find('.audit-history-show-more-container').remove();
		});
    },

    resizeExecHistory: function() {
    	var $executionHeight = AJS.$('.aui-execution').css('height'),
    		$testStepHeight = AJS.$('#teststepDetails').css('height'),
    		$height = (parseInt($executionHeight) - parseInt($testStepHeight)) - 70; // $executionHeight - $testStepHeight -padding

    	this.$el.find('#execution-history-details .mod-content').css({
    		'height': $height + 'px',
    		'overflow': 'auto'
    	});
    },

    // Refresh the execution details
    refreshExecutionDetails: function(ev) {
		ev.preventDefault();
		ev.stopPropagation();
		this.order = ZEPHYR.Execution.History.Order.DESCENDING;
		this.resetData();
    	this.displayAuditDetailsOnInit();
    },

    resetData: function() {
    	this.offset = 0;
        this.maxRecords = this.maxAllowed;
        this.count = this.maxAllowed;
        this.auditExecutionHistory = null;
    }
});

ZEPHYR.Execution.History.htmlEscape = function(str) {
    var divE = document.createElement('div');
    divE.appendChild(document.createTextNode(str));
    return divE.innerHTML;
}

ZEPHYR.Execution.History.getValueBasedOnField = function(params, output) {
    if(params.field == 'status' && ZEPHYR.Schedule.executionStatus) {
    	var statusJSON = ZEPHYR.Schedule.executionStatus[params.value];
    	if(statusJSON) {
    		var statusName = ZEPHYR.Execution.History.htmlEscape(statusJSON.name);
    		var statusLozenges = '<span class="aui-lozenge aui-lozenge-subtle status-right zfj-execution-history-lozenges" title="' + statusName + ': ' + statusJSON.description + '" style="border-color: ' + statusJSON.color + '">' + statusName + '</span>';
    		return appendSoyOutputOnCall(statusLozenges, output);
    	}
    }
		if(params.field == 'executed_on') {
			var date = new Date(Number(params.value));
			var h =  date.getHours(), m = date.getMinutes();
            m = m > 9 ? m : '0' + m;

            if(h === 0 ) {
				time = '12' + ':' + m +' AM';
			} else {
				time = (h > 12) ? (h-12 + ':' + m +' PM') : (h + ':' + m +' AM');
			}
			var dateStr = date.getDate() + '/' + (date.getMonth()+1) + '/' + date.getFullYear() + ' ' + time;
			return appendSoyOutputOnCall(dateStr, output)
		}
    if (params.field == 'execution_defect' || params.field == 'step_defect'){
		var issueAnchorText = "";
		var arr1 = params.value.split(",");
		arr1.forEach(function (el, idx) {
			if (el == 'XXXXX') {
				issueAnchorText += "<span>" + el + "</span> ";
			}else{
				issueAnchorText += "<a href='" + contextPath + "/browse/" + el + "' >" + el + "</a> ";
			}
			if (arr1.length > 1 && idx < (arr1.length-1)){
				issueAnchorText += ", ";
			}
		})
		return appendSoyOutputOnCall(issueAnchorText, output);
	}
    return appendSoyOutputOnCall(params.value, output);
},

ZEPHYR.Execution.History.init = function(issueId, executionId) {
	if(ZEPHYR.Execution.History.executionAuditDetailsView) {
		ZEPHYR.Execution.History.executionAuditDetailsView.$el.empty().off();
		if(ZEPHYR.Execution.History.executionAuditDetailsView && ZEPHYR.Execution.History.executionAuditDetailsView.stopListening)
			ZEPHYR.Execution.History.executionAuditDetailsView.stopListening();
	}
	var executionAuditList = new ZEPHYR.Execution.History.ExecutionAuditCollection();
	ZEPHYR.Execution.History.executionAuditDetailsView = new ZEPHYR.Execution.History.ExecutionAuditDetailsView({
		el: '#execution-history-container',
		currentIssueId: issueId,
		executionId: executionId,
		isDialog: false,
		maxAllowed: 15,
		model: executionAuditList
	});
	ZEPHYR.Execution.History.executionAuditDetailsView.render();
}
