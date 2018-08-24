jQuery.namespace("ZEPHYR.ISSUE.Teststep");
// Variable to check if issue is refreshed because if steps then we need to refetch the data
ZEPHYR.ISSUE_REFRESHED = false;
/**
 *
 * Initializes steps restful table. This method idempotent from #838 and all the non-idempotent parts are extrated outside of this method.
 * @returns
 */
ZEPHYR.GRID = {
	scrollableDialogue: [],
	stopGridFocus: false,
};
ZEPHYR.ISSUE.stepColumns = {};
ZEPHYR.ISSUE.steps = [];
ZEPHYR.ISSUE.testDetails = {
	limit: 50,
	offset: 0,
	maxRecords: 0,
	prevObj: {},
	nextObj: {},
	isLastElement: false,
	firstElementOnNextPage: {},
};
var isIssueTypeTest = function () {
	return AJS.$('#view_issue_steps_section_heading').length > 0
}
var isPopupOpen = false;
var configUpdateTrigger = false;
var attachmentDuplicate;
var updateSaveValues = true;

var columnValues = {};
var hasColumnValues = false;
var zStepsInitInProgress = false;
var currentIssueId;
var preference;
var attachmentInlineDialog;
var allCustomFields;
var customFieldsOrder = [];
var customFieldsValue = {};
var testDetailFreezeColumn = false;
var updatedGridDataIssueDetail = {};
var onlyUpdateGridValueIssueDetail = false;
var updatedRowId;
var addedNewRow = false;
var clearValue = false;
var initialCountIssueDetail = 10;
var refreshGrid = false;
var stepsInit = function () {
	var errors = AJS.$("#zerrors").val();
	if (errors != null && errors.length > 0) {
		return;
	}
	/* Removing JIRA standard ops bar */
	if (isIssueTypeTest()) {
		AJS.$("#opsbar-opsbar-operations").remove()
		if (!zshowWF)
			AJS.$("#opsbar-opsbar-transitions").remove()
	}

	isPopupOpen = false;

	var stepsTable = jQuery("#project-config-steps-table");
	var stepsColumnCustomization;
	var stepInlineDialog;
	var submitButtonId = 'submitStepColumn';
	var closeButtonId = 'closeInlineDialog';


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

	function initializeInlineDialog() {
		if (AJS.$('#inline-dialog-step-column-picker').length) {
			AJS.$('#inline-dialog-step-column-picker').remove();
		}
		stepsColumnCustomization = ZEPHYR.Templates.Steps.columnCustomisation({ columns: ZEPHYR.ISSUE.stepColumns, submitButtonId: submitButtonId, closeButtonId: closeButtonId });
		stepInlineDialog = AJS.InlineDialog(AJS.$("#inlineDialog"), "step-column-picker",
			function (content, trigger, showPopup) {
				content.css({ "padding": "10px 0 0" }).html(stepsColumnCustomization);
				showPopup();
				return false;
			},
			{
				width: 250,
				closeOnTriggerClick: true,
				persistent: true
			}
		);
	}

	ZEPHYR.ISSUE.customizeColumn = function () {
		var table = AJS.$("#project-config-steps-table");
		var noOfColumnsVisible = 0;
		Object.keys(ZEPHYR.ISSUE.stepColumns).forEach(function (key) {
			if (ZEPHYR.ISSUE.stepColumns[key].isVisible === "true") {
				AJS.$("#project-config-steps-table thead tr .stepColumn-" + key).css("display", "table-cell");
				AJS.$("#project-config-steps-table tbody tr .stepColumn-" + key).css("display", "table-cell");
			} else {
				AJS.$("#project-config-steps-table thead tr .stepColumn-" + key).css("display", "none");
				AJS.$("#project-config-steps-table tbody tr .stepColumn-" + key).css("display", "none");
			}
		});
	}

	function getStepColumn(callback, data) {
		jQuery.ajax({
			url: getRestURL() + "/preference/getteststepcustomization",
			type: "get",
			contentType: "application/json",
			dataType: "json",
			success: function (response) {
				Object.keys(response.preferences).forEach(function (key) {
					var obj = {
						"displayName": response.preferences[key].displayName,
						"isVisible": response.preferences[key].isVisible
					}
					ZEPHYR.ISSUE.stepColumns[key] = obj;
				});
				allCustomFields.forEach(function (field) {
					if (!ZEPHYR.ISSUE.stepColumns[field.id]) {
						var obj = {
							"displayName": field.name,
							"isVisible": "false",
						}
						ZEPHYR.ISSUE.stepColumns[field.id] = obj;
					}
					else {
						ZEPHYR.ISSUE.stepColumns[field.id].entityType = field.entityType
					}
				});
				preference = response.preferences;
				callback(data);
				initializeInlineDialog();
			}
		});
	}

	function getCustomFields(callback, data) {
		jQuery.ajax({
			url: getRestURL() + "/customfield/entity?entityType=TESTSTEP&issueId="+ currentIssueId,
			type: "get",
			contentType: "application/json",
			dataType: "json",
			success: function (response) {
				allCustomFields = response;
				getStepColumn(callback, data);			}
		});
	}

	// function getProjectCustomFields(callback, data) {
	// 	jQuery.ajax({
	// 		url: getRestURL() + '/customfield/byEntityTypeAndProject?entityType=TESTSTEP' + '&issueId=' + currentIssueId,
	// 		type: "get",
	// 		contentType: "application/json",
	// 		dataType: "json",
	// 		success: function (response) {
	// 			allCustomFields = response;
	// 			getStepColumn(callback, data);
	// 		}
	// 	});
	// }

	function getResourceURL() {
		//return JIRA.REST_BASE_URL + "/project/" + JIRA.ProjectConfig.getKey() +"/versions";
		return getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + '?offset=' + ZEPHYR.ISSUE.testDetails.offset + '&limit=' + ZEPHYR.ISSUE.testDetails.limit;
	}

	function getSteps(callback) {
		JIRA.SmartAjax.makeRequest({
			url: getResourceURL(),
			complete: function (xhr, status, response) {
				if (response.successful) {
					//callback(response.data.reverse())
					currentIssueId = JIRA.Issue.getIssueId()
					getCustomFields(callback, response.data);
				} else {

					var jsonResponse = jQuery.parseJSON(response.data);

					var customErrorMessage = "";
					if (response.hasData) {
						customErrorMessage = "<br/> <p>" + jsonResponse.errorDescHtml + "</p>";
					}

					stepsTable.trigger("serverError",
						[JIRA.SmartAjax.buildSimpleErrorContent(response)] + customErrorMessage);

				}
			}
		});
	}

	function setCustomFieldsObjects() {
		customFieldsOrder = [];
		customFieldsValue = {};
		allCustomFields.forEach(function (field) {
			var orderObj = {
				"customfieldId": field.id,
				"customFieldType": field.fieldType,
				"customFieldName": field.name,
				"customDefaultValue": field.defaultValue
			}
			customFieldsOrder.push(orderObj);

			if (field.customFieldOptionValues) {
				var fieldValues = [];
				var fieldDefaultValues = field.defaultValue.split(',');
				Object.keys(field.customFieldOptionValues).forEach(function (key) {
					var value = false;
					var options = field.customFieldOptionValues[key]
					if (fieldDefaultValues.indexOf(options) >= 0) {
						value = true;
					}
					var obj = {
						'name': options,
						'value': value
					}
					fieldValues.push(obj);
				});
				customFieldsValue[field.id] = fieldValues;
			}
			var insertIndex = AJS.$('#project-config-steps-table thead tr th').length - 2;
			AJS.$("<th class='stepColumn-" + field.id + " " + (ZEPHYR.ISSUE.stepColumns[field.id].isVisible ? '' : 'hide') + "'>" + field.name + "</th>").insertBefore('#project-config-steps-table thead tr th:eq(' + insertIndex + ')');
		});
	}

	AJS.$('body').on("click", "#inline-dialog-step-column-picker", function (e) {
		e.stopPropagation();
	});

	AJS.$('body').on("click", function (e) {
		stepInlineDialog && stepInlineDialog.hide();

		AJS.$.each(AJS.$('.test-step-actions a.aui-steps-dropdown'), function (i, el) {
			var selectedData = AJS.$(el).attr('data-clicked');
			if (AJS.$(el).css('visibility') == 'visible' && selectedData && selectedData == '1')
				AJS.$(el).css('visibility', 'hidden').attr('data-clicked', '0');
		});
	});

	AJS.$('body').on("click", "#closeInlineDialog", function (e) {
		stepInlineDialog && stepInlineDialog.hide();
	});

	AJS.$('body').off("click", "#submitStepColumn");
	AJS.$('body').on("click", "#submitStepColumn", function (e) {
		var data = {};
		var count = 0;

		AJS.$('#inline-dialog-step-column-picker li :checkbox').each(function () {
			ZEPHYR.ISSUE.stepColumns[this.id].isVisible = this.checked;
			if (this.checked) {
				ZEPHYR.ISSUE.stepColumns[this.id].isVisible = "true";
				count++;
			} else {
				ZEPHYR.ISSUE.stepColumns[this.id].isVisible = "false"
			}
		});

		data['preferences'] = ZEPHYR.ISSUE.stepColumns;
		if (count === 0) {
			AJS.$("#errorColumnSelector").show();
		} else {
			AJS.$("#errorColumnSelector").hide();
			data = JSON.stringify(data);
			jQuery.ajax({
				url: getRestURL() + "/preference/setteststepcustomization",
				type: "post",
				contentType: "application/json",
				data: data,
				dataType: "json",
				success: function (response) {
					for (column in response.preferences) {
						ZEPHYR.ISSUE.stepColumns[column].isVisible = response.preferences[column].isVisible;
					}
					initializeInlineDialog();
					// stepsColumnCustomization = ZEPHYR.Templates.Steps.columnCustomisation({columns: ZEPHYR.ISSUE.stepColumns, submitButtonId: submitButtonId, closeButtonId: closeButtonId});
					ZEPHYR.ISSUE.customizeColumn();
					AJS.$("#closeInlineDialog").trigger("click")
					// stepInlineDialog && stepInlineDialog.hide();
				}
			});
		}
	});



	/**
	 * Performs initilization and recreates StepTable. This method is idempotent.
	 * @param steps - data structure holding latest testStep data
	 */
	initialiseStepTable = function (data) {
		/*********************************************************
		 * Extend Restful table to add refreshAll functionality
		 *********************************************************/
		var steps = data.stepBeanCollection;
		ZEPHYR.ISSUE.testDetails.nextObj = data.nextTestStepBean || {};
		ZEPHYR.ISSUE.testDetails.prevObj  = data.prevTestStepBean || {};
		ZEPHYR.ISSUE.testDetails.isLastElement = data.isLastElementOnPage || false;
		ZEPHYR.ISSUE.testDetails.firstElementOnNextPage = data.firstElementOnNextPage || {};
		if (!JIRA.RestfulTable.refreshAll) {
			/*Refresh should be called after model has been updated*/
			JIRA.RestfulTable.prototype.refreshAll = function () {
				this._models.refresh([], { silent: true });
				this._models.refresh(this.options.entries, { silent: true });
				jQuery.each(this.getRows(), function (i, row) {
					row.remove();
				});
				this.renderRows(this.options.entries);
				ZEPHYR.ISSUE.customizeColumn();
				/*TODO - should we trigger refresh event*/
			}
		}

		checkColumnFreeze(ZEPHYR.ISSUE.stepColumns);
		ZEPHYR.ISSUE.testDetails.maxRecords = steps[0] ? steps[0].totalStepCount : 0;
		var paginationHtml = ZEPHYR.Templates.Steps.stepsGridPagination();
		AJS.$('#stepsGridComponentContainer').html(ZEPHYR.Templates.Steps.stepsGridComponent());
		AJS.$('#issuePopoverGridWrapper').html(ZEPHYR.Templates.Steps.issuePopoverGridComponent());
		AJS.$('#stepsGridComponentContainer').after(paginationHtml);
		AJS.$('#issuePopoverGridWrapper').after(paginationHtml);
		//PAGINATION WIDTH API CALL
		var lastPageLowerLimit;
		for (var counter = 0; counter < parseInt(ZEPHYR.ISSUE.testDetails.maxRecords); counter += 1) {
			if (counter % parseInt(ZEPHYR.ISSUE.testDetails.limit) == 0) {
				lastPageLowerLimit = counter;
			}
		}
		if (ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit) {
			var paginationDataHtml = ZEPHYR.Templates.Steps.paginationComponent({ limit: parseInt(ZEPHYR.ISSUE.testDetails.limit), offset: parseInt(ZEPHYR.ISSUE.testDetails.offset), maxRecords: parseInt(ZEPHYR.ISSUE.testDetails.maxRecords), lastPageLowerLimit: lastPageLowerLimit });
			AJS.$('.pagination-outer-container').html(paginationDataHtml);
			AJS.$('#testDetailGrid').addClass('hasPagination');
			AJS.$('#testDetailGridPopover').addClass('hasPagination');
		}
		issueViewGridComponent(steps, allCustomFields, ZEPHYR.ISSUE.stepColumns);
		ZEPHYR.ISSUE.steps = steps;
		if (document.getElementById('stepsGridComponentContainer')) {
			document.getElementById('stepsGridComponentContainer').addEventListener('keydown', function (e) { if (e.keyCode == '39') { e.stopPropagation(); e.stopImmediatePropagation(); } }, true)
		}
		try {
			var userAgent = navigator.userAgent.toLowerCase();
			if(userAgent.indexOf('msie') > -1 || userAgent.indexOf('trident') > -1 || userAgent.indexOf('edge') > -1) {
				var handleJiraMouseEvents = function(ev) {
					ev.stopPropagation();
					ev.stopImmediatePropagation();
				};
				// document.getElementById('stepsGridComponentContainer').addEventListener('mousedown', handleJiraMouseEvents, true);
				// document.getElementById('stepsGridComponentContainer').addEventListener('mouseup', handleJiraMouseEvents, true);
				// document.getElementById('stepsGridComponentContainer').addEventListener('mouseenter', handleJiraMouseEvents, true);
				document.getElementById('stepsGridComponentContainer').addEventListener('mouseleave', handleJiraMouseEvents, true);
				document.getElementById('stepsGridComponentContainer').addEventListener('mousemove', handleJiraMouseEvents, true);
				document.getElementById('stepsGridComponentContainer').addEventListener('mouseout', handleJiraMouseEvents, true);
				document.getElementById('stepsGridComponentContainer').addEventListener('mouseover', handleJiraMouseEvents, true);
			}
		}
		catch(err) {
			console.log('navigator exception');
		}

		/*********************************************************/
		// if(AJS.$('#project-config-steps-table').find('tbody.jira-restfultable-create, tbody.ui-sortable').length < 2){
		// AJS.$("#project-config-steps-table").find('tbody').remove()
		// AJS.$("#project-config-steps-table").append("<tbody></tbody>");
		// setCustomFieldsObjects();
		// steps.forEach(function(index){
		// 	index['customFieldsOrder'] = customFieldsOrder;
		// 	index['customFieldsValue'] = customFieldsValue;
		// 	index['allCustomFields'] = allCustomFields;
		// });
		// ZEPHYR.ISSUE.StepTable = new JIRA.RestfulTable({ el: AJS.$("#project-config-steps-table"), // table to add  entries  to. Entries are appended to the tables <tbody> element
		// 					editable: true,
		// 					reorderable: true,
		// 					url: getResourceURL(), // rest resource for collection
		// 					entries: steps,
		// 					noEntriesMsg: AJS.I18n.getText("view.issue.steps.none"),
		// 					views: {
		// 						editRow: ZEPHYR.ISSUE.Teststep.StepEditRow, row: ZEPHYR.ISSUE.Teststep.StepRow
		// 					}
		// });
		// /*Remove the default event listener - coz this will add the newly added element to the top of the table*/
		// ZEPHYR.ISSUE.StepTable.getCreateRow().unbind("created");

		/*
		 * Lets add our own create. Notice non-numeric index, this is a heck to bypass InsertBefore logic and automatically switch to append.
		//  * See _renderRow in RestfulTable.js
		//  */
		// ZEPHYR.ISSUE.StepTable.getCreateRow().bind("created", function(values) {
		// 	var index = "";
		// 	ZEPHYR.ISSUE.StepTable.addRow(values, index);
		//

		/* To get around IE-8 (http://javascript.gakaa.com/div-focus-4-0-5-.aspx), lets delay focus request.
		 * It typically doesnt matter how much we delay.
		 */
		// 	setTimeout(function(){
		// 			AJS.$(".jira-restfultable-editrow td textarea")[0].focus();
		// 	}, 50);
		// });
		//
		// ZEPHYR.ISSUE.StepTable.getTableBody().bind("sortupdate", function (event, ui) {
		//      //alert("Update done");
		// });

		/*Restful table is created, now lets move the add section to the end of table*/
		// var addBlock = AJS.$("#project-config-steps-table tbody.jira-restfultable-create").detach();
		// AJS.$("#project-config-steps-table tbody#zephyr-restfultable-tbody").after(addBlock);
		//
		// jQuery(".jira-restfultable-init").remove();
		// }else{
		// 	ZEPHYR.ISSUE.StepTable.refreshAll();
		// }
		ZEPHYR.ISSUE.customizeColumn();
		zStepsInitInProgress = false;
	}

	if (zStepsInitInProgress)
		return;
	zStepsInitInProgress = true;
	/*TODO - to remove this delay*/
	setTimeout(function () {
		if (!JIRA.Issue.getIssueId()) {	//checks for both null and undefined
			zStepsInitInProgress = false;
			return;
		}
		//alert(JIRA.Issue.getIssueId());
		/*If issue table is not yet initialized, call getSteps*/
		if (!ZEPHYR.ISSUE.StepTable || currentIssueId != JIRA.Issue.getIssueId() || ZEPHYR.ISSUE_REFRESHED
			|| (ZEPHYR.ISSUE.Create && ZEPHYR.ISSUE.Create.Teststep && ZEPHYR.ISSUE.Create.Teststep.hasEditStepsLoaded)) {
			getSteps(initialiseStepTable);
			ZEPHYR.ISSUE_REFRESHED = false;
			/* Fix for ZFJ-1979 */
			if (ZEPHYR.ISSUE.Create && ZEPHYR.ISSUE.Create.Teststep && ZEPHYR.ISSUE.Create.Teststep.hasEditStepsLoaded)
				ZEPHYR.ISSUE.Create.Teststep.hasEditStepsLoaded = false;
			/*Else, just reinialize it with existing data */
		} else {
			// Fix for ZFJ-2131: sort the array by orderId
			var _stepsByOrderId = _.sortBy(ZEPHYR.ISSUE.StepTable.getModels().models, function (model) {
				if (model.attributes && model.attributes.orderId) { // Check if the backbone model is created and hass attribute orderId
					return model.get('orderId');
				}
			});
			_stepsByOrderId = _stepsByOrderId || [];
			try {
				initialiseStepTable(jQuery.parseJSON(JSON.stringify(_stepsByOrderId)));
			} catch (e) {
				console.log(e);
			}
			//initialiseStepTable(jQuery.parseJSON(JSON.stringify(ZEPHYR.ISSUE.StepTable.getModels().models)));

		}
	}, 50);
}

/**
 * Capture all AJAX Events and acts only on move operation
 */
jQuery(document).ajaxSuccess(function (e, xhr, options) {
	/*Need a Stronger Regex*/
	// Fix for ZFJ-2151
	// if (/\/teststep\/[^\s\.]+\/move/.test(options.url)) {
	//   ZEPHYR.ISSUE.StepTable.options.entries = jQuery.parseJSON(xhr.responseText);
	//   ZEPHYR.ISSUE.StepTable.refreshAll();
	// }
});

var cleanOpsBarsIfExist = function () {
	if (!isIssueTypeTest())
		return;
	AJS.$('#comment-issue').removeClass('inline-comment')
	//For 5.1.x onwards
	if (AJS.$("#opsbar-opsbar-operations").length > 0 || AJS.$("#opsbar-opsbar-transitions").length > 0) {
		AJS.$("#opsbar-opsbar-operations").remove()
		if (!zshowWF)
			AJS.$("#opsbar-opsbar-transitions").remove()
		//Removing this class, in FF, this class caused comment dialog to not show up
		return true
	}
	//For 5.1
	if (AJS.$("#opsbar-operations").length > 0 || AJS.$("#opsbar-transitions").length > 0) {
		AJS.$("#opsbar-operations").remove()
		AJS.$("#opsbar-transitions").remove()
		//Removing this class, in FF, this class caused comment dialog to not show up
		return true
	}
	return false
}

/**
 * Called on IssueRefresh (after every inline or other type of edit)
 * This is crucial because JIRA unnecessarily reloads the web panels on issueEdit.
 * @param e Event
 * @returns
 */
var reInit = function (e, panel, $new, $existing) {
	if (panel === "view_issue_steps_section") {
		/*If there was a license error, lets just show that */
		if (AJS.$('input#zerrors').val()) {
			$new.replaceWith($existing)
		}
		/*Remove Ops bars and initialize steps table again */
		else {
			if (!cleanOpsBarsIfExist()) {
				var times = 0;
				var t = setInterval(function () {
					var opsBarsCleaned = cleanOpsBarsIfExist(); times++;
					//Wait for ops bars to be clean or 30 sec whichever is earlier
					if (opsBarsCleaned || times > 300) {
						clearInterval(t); console.log(times)
					}
				}, 100)
			}
			/* Reinitialize steps table*/
			stepsInit();
		}
	}
}

function isLoadedInIframe() {
	try {
		return (window !== window.parent);
	} catch (e) {
		return false;
	}
}

var InitPageContent = function (initCallback) {
	if (isLoadedInIframe()) {
		AJS.$(window).load(function () {
			initCallback();
		});
	} else {
		AJS.$(document).ready(function () {
			initCallback();
		});
	}
}

InitPageContent(function () {
	if (JIRA.Events.PANEL_REFRESHED) {
		JIRA.unbind(JIRA.Events.PANEL_REFRESHED, reInit)
		JIRA.bind(JIRA.Events.PANEL_REFRESHED, reInit)
		JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, panel, reason) {
			console.log(panel + " " + reason)
			if (panel.length && typeof panel.attr === "function" && panel.attr('id') === "view_issue_steps_section") {
				ZEPHYR.ISSUE_REFRESHED = true;
				stepsInit();
			} else
				cleanOpsBarsIfExist();
		})
		JIRA.bind(JIRA.Events.LAYOUT_RENDERED, function (e) {
			console.log("rendered e " + e);
		})
	}
	if (JIRA.Events.ISSUE_REFRESHED) {
		JIRA.bind(JIRA.Events.ISSUE_REFRESHED, function (event, issueId) {
			stepsInit();
		})
	}
	stepsInit()
});

AJS.$("#entity-operations-delete").live("click", function (e) {
	var entity = AJS.$(this).attr('val');
	var attachment = entity.split(':');
	deleteConfirmationDialog(attachment[0], attachment[1]);
	e.stopImmediatePropagation();
	return;
});

/*In lack of model layer, persisting data by passing thro' functions*/
var deleteConfirmationDialog = function (entityId, entityN) {
	var instance = this,
		dialog = new JIRA.FormDialog({
			id: "entity-" + entityId + "-delete-dialog",
			content: function (callback) {
				/*Short cut of creating view, move it to Backbone View and do it in render() */
				var innerHtmlStr = ZEPHYR.Project.Confirmation.deleteConfirmationDialog({ entityName: entityN });
				callback(innerHtmlStr);
			},

			submitHandler: function (e) {
				deleteAttachmentSteps(entityId, function () {
					dialog.hide();
				});
				e.preventDefault();
			}
		});

	dialog.show();
}

var deleteAttachmentSteps = function (entityId, completed) {
	jQuery.ajax({
		url: getRestURL() + "/attachment/" + entityId,
		type: "delete",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			/*Full server refresh - or shall we just remove the corresponding cycle div?. Removed DIV*/
			/* Retrieve Attachment Content and remove the one deleted */
			var stepId = AJS.$('#attachment-content' + entityId).closest('.attachment-content-wrapper').attr('data-id');
			AJS.$('.attachment-content' + entityId).remove();
			if (AJS.$("#attachment-content-container-" + stepId + " .attachment-content").length === 0) {
				if (AJS.$("#attachment-inlineDialog-" + stepId).length) {
					AJS.$("#attachment-inlineDialog-" + stepId).remove();
					AJS.$("#inline-dialog-attachment-dialog-" + stepId).remove();
				} else if (AJS.$("#attachment-inlineDialog-edit-" + stepId).length) {
					AJS.$("#attachment-inlineDialog-edit-" + stepId).remove();
					AJS.$("#inline-dialog-edit-attachment-dialog-" + stepId).remove();
				}
			}

			if (completed)
				completed.call();
		},
		error: function (response) {
			if (response && response.status == 403) {
				var _responseJSON = {};
				try {
					_responseJSON = jQuery.parseJSON(response.responseText);
				} catch (e) {
					console.log(e);
				}
				if (_responseJSON.PERM_DENIED) {
					showPermissionError(response);
					return;
				}
			} else {
				buildExecutionError(response);
			}
			if (completed)
				completed.call();
		}
	});
}

var triggerInlineDialog = function (stepId) {
	var html = AJS.$("#attachment-content-container-" + stepId)[0].innerHTML;
	if (AJS.$("#inline-dialog-attachment-dialog-" + stepId).length > 0) {
		AJS.$("#inline-dialog-attachment-dialog-" + stepId).remove();
	}
	attachmentInlineDialog = AJS.InlineDialog(AJS.$("#attachment-inlineDialog-" + stepId), "attachment-dialog-" + stepId,
		function (content, trigger, showPopup) {
			content.css({ "overflow-y": "auto" }).html(html);
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

	attachmentInlineDialog.show();
	setTimeout(function () {
		AJS.$("#inline-dialog-attachment-dialog-" + stepId + " .aui-inline-dialog-contents.contents").css("max-height", "none");
	}, 0);
}

var triggerInlineDialogEditSteps = function (stepId) {
	var html = AJS.$("#attachment-content-container-" + stepId)[0].innerHTML;
	if (AJS.$("#inline-dialog-edit-attachment-dialog-" + stepId).length > 0) {
		AJS.$("#inline-dialog-edit-attachment-dialog-" + stepId).remove();
	}
	attachmentInlineDialog = AJS.InlineDialog(AJS.$("#attachment-inlineDialog-edit-" + stepId), "edit-attachment-dialog-" + stepId,
		function (content, trigger, showPopup) {
			content.css({ "overflow-y": "auto" }).html(html);
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

	attachmentInlineDialog.show();
	setTimeout(function () {
		AJS.$("#inline-dialog-edit-attachment-dialog-" + stepId + " .aui-inline-dialog-contents.contents").css("max-height", "none");
	}, 0);
}

var renderPreview = function(ev){
	var detail = ev.originalEvent.detail;
	var data = detail.data;
	var customTextElem = ev.originalEvent.detail.customTextElem;
	var selector = ev.originalEvent.detail.selector;
	var containerId = ev.originalEvent.detail.containerId;
	var rowId = ev.originalEvent.detail.rowId;
	jQuery.ajax({
		url: getRestURL() + "/util/render",
		type: "post",
		contentType: "application/json",
		data: JSON.stringify(data),
		dataType: "json",
		success: function(response) {
			setTimeout(function () {
				var editEl = customTextElem.querySelector('.cell-editMode');
				selector.classList.add("hide");
				var selectorValue = selector.value;
				// var dummyEl = document.createElement('div');
				// dummyEl.id = 'dummy';
				var dummyEl = '<div id="dummy">' + response.renderedHTML + '</div>';
				editEl.innerHTML += dummyEl;
				editEl.querySelector('textarea').value = selectorValue || '';
				vanillaGrid.templates.adjustRowHeightCell(containerId, rowId);
			}, 0);
		}
    });
}

window.onbeforeunload = function (e) {
	return;
}
var dummyConfig = {};
var issueViewGridComponent = function (steps, customColumns, allColumns, currentConfig, isFromLargeView, isFocusNewTestStep, partialRender) {
	new JIRA.FormDialog({
		id: "zephyr-attach-file-dialog",
		trigger: "#zephyr-file-dialog",
		handleRedirect: true,
		issueMsg: 'thanks_issue_worklogged',
		onContentRefresh: function () {
			setTextOverflowEllipses();
		}
	});
	var containerId = isFromLargeView ? 'testDetailGridPopover' : 'testDetailGrid';
	var config = {
		"head": [
			{
				key: 'orderId',
				displayName: '',
				isFreeze: testDetailFreezeColumn,
				editable: false,
				isInlineEdit: false,
				type: 'String',
				isSortable: false,
				isVisible: true,
			},
			{
				key: 'step',
				editKey: 'htmlStep',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.step'),
				isFreeze: testDetailFreezeColumn,
				editable: true,
				isInlineEdit: true,
				type: 'WIKI_LARGE_TEXT',
				isSortable: false,
				isVisible: true,
				wikiBaseUrl: getRestURL(),
				wikiPreview: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/mark-up_button.svg',
				wikiHelp: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/question-mark_button.svg',
				wikiHelpUrl: contextPath + '/secure/WikiRendererHelpAction.jspa?section=texteffects'
			}, {
				key: 'data',
				editKey: 'htmlData',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.data'),
				isFreeze: false,
				editable: true,
				isInlineEdit: true,
				isSortable: false,
				type: 'WIKI_LARGE_TEXT',
				isVisible: true,
				wikiBaseUrl: getRestURL(),
				wikiPreview: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/mark-up_button.svg',
				wikiHelp: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/question-mark_button.svg',
				wikiHelpUrl: contextPath + '/secure/WikiRendererHelpAction.jspa?section=texteffects',
			}, {
				key: 'result',
				editKey: 'htmlResult',
				displayName: AJS.I18n.getText('view.issue.steps.table.column.result'),
				isFreeze: false,
				editable: true,
				isInlineEdit: true,
				isSortable: false,
				type: 'WIKI_LARGE_TEXT',
				isVisible: true,
				wikiBaseUrl: getRestURL(),
				wikiPreview: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/mark-up_button.svg',
				wikiHelp: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/question-mark_button.svg',
				wikiHelpUrl: contextPath + '/secure/WikiRendererHelpAction.jspa?section=texteffects',
			}, {
				key: 'attachmentsMap',
				displayName: AJS.I18n.getText('zephyr.grid.attachmentsMap.displayName'),
				isFreeze: false,
				editable: false,
				isInlineEdit: false,
				isSortable: false,
				type: 'ATTACHMENTS',
				isVisible: true,
				imgUrl: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/plus_button.svg',
				baseUrl: contextPath,
				canAddAttachment: true,
				fetchAttachment: false,
				tooltipLabel: AJS.I18n.getText('zephyr.common.addAttachment.tooltip.label')
			}
		],
		"row": [],
		"actions": [
			{
				actionName: AJS.I18n.getText('zephyr.grid.actions.clone.actionName'),
				customEvent: 'cloneRow',
				imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/clone_button.svg',
			},
			{
				actionName: AJS.I18n.getText('zephyr.grid.actions.delete.actionName'),
				customEvent: 'deleteRow',
				imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/delete_button.svg',
			},
		],
		"maxFreezed": 2,
		"bulkActions": [{
			actionName: testDetailFreezeColumn ? AJS.I18n.getText('zephyr.grid.actions.bulkActionsUnfreeze.actionName') : AJS.I18n.getText('zephyr.grid.actions.bulkActionsfreeze.actionName'),
			customEvent: 'freezeColumns',
			disabled: false,
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pin-issue-page_button.svg',
		}
		],
		"columnchooser": {
			isEnabled: true,
			actionName: AJS.I18n.getText('enav.executions.view.column.title'),
			customEvent: 'columnchooser',
			columnChooserValues: [],
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
		},
		"testStepFocus": {
			isEnabled: true,
			actionName: AJS.I18n.getText('zephyr.grid.actions.testStepFocus.actionName'),
			customEvent: 'scrollDown',
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/add-step_icon.svg',
		},
		"addTestSteps": {
			isEnabled: true,
			actionName: AJS.I18n.getText('zephyr.grid.actions.addTestSteps.actionName'),
			customEvent: 'addstep',
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/tick_button.svg',
		},
		"removeTestSteps": {
			isEnabled: true,
			actionName: AJS.I18n.getText('zephyr.grid.actions.removeTestSteps.actionName'),
			customEvent: 'clearvalue',
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/cross_button.svg',
		},
		"exportTestSteps": {
			isEnabled: false,
			actionName: AJS.I18n.getText('zephyr.grid.actions.exportTestSteps.actionName'),
			customEvent: 'exportvalue',
			exportOptions: [
				{
					'imgSrc': contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export-csv_icon.svg',
					'exportId': 'csvTeststepId',
					'exportName': 'CSV'
				},
				{
					'imgSrc': contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export-xml_icon.svg',
					'exportId': 'xmlTeststepId',
					'exportName': 'XML'
				},
				{
					'imgSrc': contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export-html_icon.svg',
					'exportId': 'htmlTeststepId',
					'exportName': 'HTML'
				},
				{
					'imgSrc': contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export-xls_icon.svg',
					'exportId': 'xlsTeststepId',
					'exportName': 'Excel'
				},
				{
					'imgSrc': contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export-json_icon.svg',
					'exportId': 'jsonTeststepId',
					'exportName': 'Json'
				},
			],
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/export_button.svg',
		},
		"popupTestSteps": {
			isEnabled: isPopupOpen ? false : true,
			actionName: AJS.I18n.getText('zephyr.grid.actions.popupTestSteps.actionName'),
			customEvent: 'largeview',
			imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pop-up_button.svg',
		},
		"hasBulkActions": true,
		"freezeToggle": false,
		"draggableRows": true,
		"highlightSelectedRows": true,
		"dragImageUrl": contextPath + '/download/resources/com.thed.zephyr.je/images/icons/drag_icon.svg',
		"columnChooserUrl": contextPath + '/download/resources/com.thed.zephyr.je/images/icons/column-chooser_button.svg',
		"rowSelection": false,
		"firstColumnId": true,
		"gridComponentPage": 'issueView',
		"configUpdateTrigger": configUpdateTrigger,
		"isPopupOpen": isPopupOpen,
		"updateSaveValues": updateSaveValues,
		"initialCount": getGridInitialCount(initialCountIssueDetail),
		"columnChooserHeading": AJS.I18n.getText('cycle.ColumnChooser.label'),
		"selectAtleaseoneOption": AJS.I18n.getText('zephyr.gridData.selectAtleastOneOption'),
		"submit": AJS.I18n.getText('zephyr.je.submit.form.Submit'),
		"cancel": AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		"noPermission": AJS.I18n.getText('zephyr.gridDatanoPermission'),
		"action": AJS.I18n.getText('cycle.actions.label'),
		"loading": AJS.I18n.getText('zephyr.gridDataloading'),
		"placeholderText": AJS.I18n.getText('zephyr.customfield.textarea.placeholder'),
		"isStepsGrid": true,
		"contextPath": contextPath,
		"isPrevEnabled": ZEPHYR.ISSUE.testDetails.prevObj.id ? true : false,
		"isNextEnabled": ZEPHYR.ISSUE.testDetails.nextObj.id ? true : false,
		"dataset": [{
			name: 'stepid',
			key: 'id'
		}],
		"movePrevPageLabel": AJS.I18n.getText('zephyr.grid.actions.moveOption.prevPage'),
		"moveNextPageLabel": AJS.I18n.getText('zephyr.grid.actions.moveOption.nextPage'),
		"noPermissionTestIssue": AJS.I18n.getText('cycle.noPermissionTestIssue.label')
	}

	customColumns.map(function (columnCell) {
		var fieldValues = [];
		if (columnCell.customFieldOptionValues) {
			var fieldDefaultValues = columnCell.defaultValue.split(',');
			Object.keys(columnCell.customFieldOptionValues).forEach(function (key) {
				var value = false;
				var options = columnCell.customFieldOptionValues[key]
				if (fieldDefaultValues.indexOf(options) >= 0) {
					value = true;
				}
				var obj = {
					'content': options,
					'value': key,
					'selected': value,
				}
				fieldValues.push(obj);
			});
		}
		var imgUrl = configureTriggerOptions(columnCell.fieldType)
		var obj = {
			"key": columnCell.id,
			"displayName": columnCell.name,
			"isFreeze": false,
			"editable": true,
			"isInlineEdit": true,
			"imgUrl": contextPath + imgUrl || '',
			"type": columnCell.fieldType,
			"options": fieldValues ? fieldValues : [],
			"isSortable": false,
			"isVisible": true,
			"editKey": (columnCell.fieldType == 'LARGE_TEXT' || columnCell.fieldType == 'TEXT') ? columnCell.id + 'htmlValue' : '',
		}

		config.head.push(obj);
	})

	steps.map(function (row, index) {
		var obj = row;
		if (index === 0) {
			selectedExecutionId = obj.id;
		}

		obj.customFields && (Object.keys(obj.customFields).map(function (field) {
			if (obj.customFields[field].customFieldType === 'DATE') {
				obj[obj.customFields[field].customFieldId] = convertDate({ value: obj.customFields[field].value, isDateTime: false });
			} else if (obj.customFields[field].customFieldType === 'DATE_TIME') {
				obj[obj.customFields[field].customFieldId] = convertDate({ value: obj.customFields[field].value, isDateTime: true });
			}
			else if (obj.customFields[field].customFieldType === 'NUMBER' && obj.customFields[field].value != '') {
				obj[obj.customFields[field].customFieldId] = obj.customFields[field].value;
			} else if (obj.customFields[field].customFieldType === 'TEXT' || obj.customFields[field].customFieldType === 'LARGE_TEXT') {
				obj[obj.customFields[field].customFieldId] = obj.customFields[field].value;
				obj[obj.customFields[field].customFieldId + 'htmlValue'] = obj.customFields[field].htmlValue;
			} else {
				obj[obj.customFields[field].customFieldId] = obj.customFields[field].selectedOptions;
			}
		}));
		if (updatedRowId === obj.id) {
			if (addedNewRow) {
				updatedGridDataIssueDetail.newRow = true;
			}
			updatedGridDataIssueDetail.rowData = obj;
			updatedGridDataIssueDetail.rowData['index'] = index;
			updatedRowId = '';
		}

		config.row.push(obj);
	});

	var obj = {};


	config.head.map(function (columns) {
		if (columns.key === 'orderId') {
			obj[columns.key] = config.row.length + 1;
		} else {
			if (hasColumnValues) {
				obj[columns.key] = columnValues[columns.key] && columnValues[columns.key].value || '';
			} else {
				obj[columns.key] = '';
			}
		}
	})
	obj['editMode'] = true;
	obj['mode'] = 'edit';
	obj['id'] = -1;
	if (hasColumnValues || clearValue) {
		obj['refreshGrid'] = !refreshGrid;
		refreshGrid = !refreshGrid;
		updatedGridDataIssueDetail.rowData = obj;
		updatedGridDataIssueDetail.rowData.index = steps.length;
	}
	if (addedNewRow) {
		updatedGridDataIssueDetail.addStepRow = obj;
		addedNewRow = false;
	}
	clearValue = false;
	hasColumnValues = false;
	config.row.push(obj);

	var columnChooserValues = [];
	config.head.map(function (column) {
		if ((column.key !== "issueKey" && column.key !== "orderId")) {
			var key = column.key;
			if (key === 'step') {
				key = 'teststep';
			} else if (key === 'data') {
				key = 'testdata';
			} else if (key === 'result') {
				key = 'testresult';
			} else if (key === 'attachmentsMap') {
				key = 'attachment';
			}
			if (allColumns[key] && allColumns[key].isVisible === 'true') {
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

	config.columnchooser.disabled = config.row.length === 0 ? true : false;
	config['freezeColumns'] = testDetailFreezeColumn;

	/*if (onlyUpdateGridValueIssueDetail) {
		AJS.$('#testDetailGrid').attr('updatedconfig', JSON.stringify(updatedGridDataIssueDetail));
		if(!isPopupOpen) {
			onlyUpdateGridValueIssueDetail = false;
		}
	} else {
		if (initialCountIssueDetail != 10) {
			initialCountIssueDetail = 10;
		}
		initialCountIssueDetail = getGridInitialCount(initialCountIssueDetail);
		if (isFromLargeView == true) {
			let updatedconfig = {
				updateOpenPopup : true,
				isPopupOpen : isPopupOpen,
				popupTestSteps: {
					isEnabled: isPopupOpen ? false : true,
					actionName: AJS.I18n.getText('zephyr.grid.actions.popupTestSteps.actionName'),
					customEvent: 'largeview',
					imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/pop-up_button.svg',
				}
			}
			AJS.$('#testDetailGrid').attr('updatedconfig', JSON.stringify(updatedconfig));
		} else {
			AJS.$('#testDetailGrid').attr('config', JSON.stringify(config));
		}
	}*/
	AJS.$(document).unbind('gridScrollEventCapture');
	AJS.$(document).bind('gridScrollEventCapture', gridScrollEventCapture);
	AJS.$(document).unbind('emitContainerDimensions');
	AJS.$(document).bind('emitContainerDimensions', renderExternalScroll);

	if (isFromLargeView) {
		if (AJS.$('#testDetailGridPopover').hasClass('hasPagination')) {
			config.actions.push({
				actionName: AJS.I18n.getText('zephyr.grid.actions.moveOption.actionName'),
					customEvent: 'moreOptions',
						imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
			});
		}
	} else {
		if (AJS.$('#testDetailGrid').hasClass('hasPagination')) {
			config.actions.push({
				actionName: AJS.I18n.getText('zephyr.grid.actions.moveOption.actionName'),
				customEvent: 'moreOptions',
				imgSrc: contextPath + '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg',
			});
		}
	}

	if (partialRender && partialRender.isPartialRender && partialRender.type) {
		var cellConfig = {};
		 	cellConfig.row = partialRender.rowData;
		 	cellConfig.contextPath = contextPath;
		switch (partialRender.type) {
			case 'attachment' : cellConfig.header = config.head.filter(function(header){
									return header.key.toString() === 'attachmentsMap';
								})[0];
								if (isFromLargeView) {
									vanillaGrid.templates.partialRender('testDetailGridPopover', partialRender.rowId.toString(), cellConfig, config);
								} else {
									vanillaGrid.templates.partialRender('testDetailGrid', partialRender.rowId.toString(), cellConfig, config);
								}
								break;
			case 'deleteRow' : vanillaGrid.templates.deleteRow(partialRender.rowId);
							   if (ZEPHYR.ISSUE.testDetails.maxRecords >= ZEPHYR.ISSUE.testDetails.limit && config.row.length > 1 && (ZEPHYR.ISSUE.testDetails.maxRecords >= ZEPHYR.ISSUE.testDetails.offset + ZEPHYR.ISSUE.testDetails.limit)) {

							   	 vanillaGrid.templates.addRow(config.row[config.row.length - 2], config, null, containerId);
							   }
								if (ZEPHYR.ISSUE.testDetails.maxRecords == ZEPHYR.ISSUE.testDetails.limit) {
									for (var counter = 0; counter < ZEPHYR.ISSUE.steps.length; counter += 1) {
										if (AJS.$(document.querySelector('.action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div'))[0].children.length == 3) {
											AJS.$(document.querySelector('.action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div > div:nth-child(3)')).remove();
										}
										if (AJS.$(document.querySelector('.action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div:nth-child(2)'))) {
											AJS.$(document.querySelector('.action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div:nth-child(2)')).remove();
										}

										if (isFromLargeView) {
											if (AJS.$(document.querySelector('#testDetailGridPopover .action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div'))[0].children.length == 3) {
												AJS.$(document.querySelector('#testDetailGridPopover .action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div > div:nth-child(3)')).remove();
											}
											if (AJS.$(document.querySelector('#testDetailGridPopover .action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div:nth-child(2)'))) {
												AJS.$(document.querySelector('#testDetailGridPopover .action-column > div:nth-child(' + ZEPHYR.ISSUE.steps[counter].orderId + ') > div:nth-child(2)')).remove();
											}
										}
									}
								}
							   vanillaGrid.templates.updateTestStepsDataSet(config, ZEPHYR.ISSUE.testDetails.offset);
							   break;
			case 'move'  : vanillaGrid.templates.partialRenderReorder(ZEPHYR.ISSUE.steps, partialRender.stepId, partialRender.data, ZEPHYR.ISSUE.testDetails.offset);
						   break;
			case 'clone' :
							var rowCloned;
							var index;
							var insertBeforeRowIndex;
							var rowToBeCloned = ZEPHYR.ISSUE.steps.filter(function(step,indexS) {
								if (step.id == partialRender.stepId) {
									index = indexS;
									return true;
								}
							})[0];
							var isPaginated = ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit;
							var isLastPage = !(ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.offset + ZEPHYR.ISSUE.testDetails.limit)
							if (isPaginated) {
								switch (partialRender.position) {
									case '0' : rowCloned = index == undefined ? ZEPHYR.ISSUE.steps[ZEPHYR.ISSUE.steps.length - 1] : index == 0 ? ZEPHYR.ISSUE.steps[index] : ZEPHYR.ISSUE.steps[index - 1];
											   insertBeforeRowIndex = index == undefined ? null : index == 0 ? index : index - 1;
											   if (index == undefined) {
											   	isLastPage || vanillaGrid.templates.deleteRow(null);
											   	vanillaGrid.templates.addRow(rowCloned, config, insertBeforeRowIndex, containerId);
											   } else {
											   	vanillaGrid.templates.addRow(rowCloned, config, insertBeforeRowIndex, containerId);
											   	isLastPage || vanillaGrid.templates.deleteRow(null);
											   }
												break;
									case '-1': rowCloned = index == ZEPHYR.ISSUE.testDetails.limit - 1 ? null : ZEPHYR.ISSUE.steps[index + 1];
											   if (rowCloned) {
    											 insertBeforeRowIndex = index + 1;
											   	 vanillaGrid.templates.addRow(rowCloned, config, insertBeforeRowIndex, containerId);
											   	 isLastPage || vanillaGrid.templates.deleteRow(null);
											   }
											   break;

									case '-2': if (!(ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.offset + ZEPHYR.ISSUE.testDetails.limit )) {
											  	rowCloned = ZEPHYR.ISSUE.steps[ZEPHYR.ISSUE.steps.length - 1];
											   	vanillaGrid.templates.addRow(rowCloned, config, null, containerId);
											   }

											   break;
									default : if (ZEPHYR.ISSUE.testDetails.offset < parseInt(partialRender.position) && parseInt(partialRender.position) <= ZEPHYR.ISSUE.testDetails.offset + ZEPHYR.ISSUE.testDetails.limit) {
												  rowCloned = ZEPHYR.ISSUE.steps[(partialRender.position - 1) % ZEPHYR.ISSUE.testDetails.limit];
												  insertBeforeRowIndex = (partialRender.position - 1) % ZEPHYR.ISSUE.testDetails.limit;
												  vanillaGrid.templates.addRow(rowCloned, config, insertBeforeRowIndex, containerId);
												  isLastPage || vanillaGrid.templates.deleteRow(null);
											   } else {
											   	rowCloned = ZEPHYR.ISSUE.steps[0];
											   	 if (isLastPage) {
											   	 	vanillaGrid.templates.addRow(rowCloned, config, 0, containerId);
											   	 }
											   }

								}
								if (ZEPHYR.ISSUE.testDetails.maxRecords == ZEPHYR.ISSUE.testDetails.limit + 1) {
									vanillaGrid.init(document.getElementById('testDetailGrid'), config);
									if (isFromLargeView) {
										vanillaGrid.init(document.getElementById('testDetailGridPopover'), config);
									}
								}
								vanillaGrid.templates.updateTestStepsDataSet(config, ZEPHYR.ISSUE.testDetails.offset);

							} else {
								switch (partialRender.position) {
									case '0' : rowCloned = index == 0 ? ZEPHYR.ISSUE.steps[index] : ZEPHYR.ISSUE.steps[index - 1];
											   insertBeforeRowIndex = index == 0 ? index : index - 1;
												break;
									case '-1': rowCloned = ZEPHYR.ISSUE.steps[index + 1];
											   insertBeforeRowIndex = index + 1;
											   break;

									case '-2': rowCloned = ZEPHYR.ISSUE.steps[ZEPHYR.ISSUE.steps.length - 1];
											   insertBeforeRowIndex = null;
												break;
									default : rowCloned = ZEPHYR.ISSUE.steps[partialRender.position - 1];
											  insertBeforeRowIndex = partialRender.position - 1;
								}
								vanillaGrid.templates.addRow(rowCloned, config, insertBeforeRowIndex, containerId);
								vanillaGrid.templates.updateTestStepsDataSet(config, ZEPHYR.ISSUE.testDetails.offset);
							}
							break;

		}
	} else if(isFromLargeView) {
		vanillaGrid.init(document.getElementById('testDetailGridPopover'), config);
	} else {
		vanillaGrid.init(document.getElementById('testDetailGrid'), config);
	}

	if (isFocusNewTestStep) {
		if (isFromLargeView) {
			focusNewTestStep('testDetailGridPopover');
		} else {
			focusNewTestStep('testDetailGrid');
		}

	}

	AJS.$('#testDetailGrid').unbind('stepGridComponentActions');
	AJS.$('#testDetailGrid').bind('stepGridComponentActions', stepGridComponentActions);
	AJS.$('#testDetailGrid').unbind('stepGridValueUpdated');
	AJS.$('#testDetailGrid').bind('stepGridValueUpdated', stepGridValueUpdated);
	AJS.$('#testDetailGrid').unbind('gridRowSelected');
	AJS.$('#testDetailGrid').unbind('gridBulkActions');
	AJS.$('#testDetailGrid').bind('gridBulkActions', stepGridBulkActions);
	AJS.$('#testDetailGrid').unbind('renderPreview');
	AJS.$('#testDetailGrid').bind('renderPreview', renderPreview);
	AJS.$('#testDetailGrid').unbind('dialogueScroll');
	AJS.$('#testDetailGrid').bind('dialogueScroll', dialogueScroll);




	/*if (isPopupOpen) {
		if (onlyUpdateGridValueIssueDetail) {
			//AJS.$('#testDetailGridPopover').attr('updatedconfig', JSON.stringify(updatedGridDataIssueDetail));
			onlyUpdateGridValueIssueDetail = false;
		} else {
			config.isPopup = true;
			config.showLoader = true;
			config.loading = AJS.I18n.getText('zephyr.gridData.loading');
			vanillaGrid.init(document.getElementById('testDetailGridPopover'), config);
			//AJS.$('#testDetailGridPopover').attr('config', JSON.stringify(config));
		}
	}*/
	updatedGridDataIssueDetail = {};
	AJS.$('#testDetailGridPopover').unbind('stepGridComponentActions');
	AJS.$('#testDetailGridPopover').bind('stepGridComponentActions', stepGridComponentActions);
	AJS.$('#testDetailGridPopover').unbind('stepGridValueUpdated');
	AJS.$('#testDetailGridPopover').bind('stepGridValueUpdated', stepGridValueUpdated);
	AJS.$('#testDetailGridPopover').unbind('gridRowSelected');
	AJS.$('#testDetailGridPopover').unbind('gridBulkActions');
	AJS.$('#testDetailGridPopover').bind('gridBulkActions', stepGridBulkActions);
	AJS.$('#testDetailGridPopover').unbind('renderPreview');
	AJS.$('#testDetailGridPopover').bind('renderPreview', renderPreview);
	AJS.$('#testDetailGridPopover').unbind('dialogueScroll');
	AJS.$('#testDetailGridPopover').bind('dialogueScroll', dialogueScroll);
	//AJS.$('#testDetailGridPopover').unbind('emitContainerDimensions');
	//AJS.$('#testDetailGridPopover').bind('emitContainerDimensions', renderExternalScroll);

	updateSaveValues = true;
}

function configureTriggerOptions(type) {
	var imgUrl;
	switch (type) {
		case 'DATE':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/date-picker_icon.svg'
			break;
		case 'DATE_TIME':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/date-and-time_icon.svg'
			break;
		case 'SINGLE_SELECT':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/single-select_icon.svg'
			break;
		case 'MULTI_SELECT':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/multi-select_icon.svg'
			break;
		case 'RADIO_BUTTON':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/radio-button_icon.svg'
			break;
		case 'CHECKBOX':
			imgUrl = '/download/resources/com.thed.zephyr.je/images/icons/check-box_icon.svg'
			break;
	}
	return imgUrl;
}

var stepGridBulkActions = function (ev) {
	ev.preventDefault();
	ev.stopPropagation();
	if (ev.originalEvent.detail.actionName === 'freezeColumns') {

		var sendingData = pagePreferencesData;
		sendingData.testDetailsColumnFreezer = !testDetailFreezeColumn;
		jQuery.ajax({
			url: getRestURL() + "/preference/paginationWidthPreference",
			type: "PUT",
			contentType: "application/json",
			dataType: "json",
			data: JSON.stringify(sendingData),
			success: function (response) {
				updateSaveValues = false;
				testDetailFreezeColumn = !testDetailFreezeColumn;
				columnValues = ev.originalEvent.detail.columnsValues[Object.keys(ev.originalEvent.detail.columnsValues)[0]] || {};
				hasColumnValues = true;
				initialCountIssueDetail = ZEPHYR.ISSUE.steps.length;
				// onlyUpdateGridValueIssueDetail = true;
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen);
			}
		});

	}
}

var renderExternalScroll = function(ev) {
	ev.preventDefault();
	ev.stopPropagation();
	var dimensionObj = ev.originalEvent.detail;
	var id = ev.target.id;
	console.log('in renderExternalScroll', dimensionObj, ev.target.id);
	var scrollHtml = ZEPHYR.Templates.Steps.stepsGridComponentScroll({
			freezeColumnWidth : dimensionObj.freezeColumnsWidth + 15 + 'px',
			unfreezeColumnWidth : dimensionObj.unfreezeColumnsWidth + 15 +'px'
	});
	if(AJS.$(ev.target).parents('.gridComponent').find('#external-scroll').length) {
		AJS.$(ev.target).parents('.gridComponent').find('#external-scroll').remove()
	}
	AJS.$(ev.target).parents('.gridComponent').append(scrollHtml);
	var scrollableGrid = document.getElementById(id).querySelector('#unfreezedGrid');
	AJS.$(".unfrozen").scroll(function(ev){
		var scrollPosLeft = AJS.$(this).scrollLeft();
    //console.log(scrollPosLeft);
    scrollableGrid.scrollLeft = scrollPosLeft;
    //AJS.$('#'+ id).attr('scrollgrid', JSON.stringify(scrollPosLeft));
	});

}



var stepGridComponentActions = function (ev) {
	ev.preventDefault();
	ev.stopPropagation();

	var actionDetail = ev.originalEvent.detail;

	if (actionDetail.type === 'DATE' || actionDetail.type === 'DATE_TIME') {
		if (actionDetail.onlyUpdateValue) {
			updateSaveValues = false;
			if (actionDetail.isEditMode) {
				columnValues = actionDetail.columnsValues || {};
				columnValues[actionDetail.cellKey] = '';
				hasColumnValues = true;
				onlyUpdateGridValueIssueDetail = true;
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns);
			} else {
				var customFieldValues = {};

				var customFieldValueId;
				var data = {};
				ZEPHYR.ISSUE.steps.map(function (step) {
					if (step.id === actionDetail.stepId && step.customFields) {
						customFieldValueId = step.customFields[actionDetail.cellKey] && step.customFields[actionDetail.cellKey].customFieldValueId;
					}
				});

				var obj = {
					customFieldId: actionDetail.cellKey,
					customFieldType: actionDetail.type,
					value: '',
				}

				if (customFieldValueId) {
					obj['customFieldValueId'] = customFieldValueId;
				}

				customFieldValues[actionDetail.cellKey] = obj;
				data['customFieldValues'] = customFieldValues;
				data['id'] = actionDetail.stepId;
				updateTestStep(data, actionDetail);
			}
		} else {
			var obj = {
				target: ev.originalEvent.detail.event.target.parentElement,
				isDateTimePicker: true,
				onWindowScrollDoNothing : true,
			};
			var inputDateField = AJS.$('#date-picker');
			var positionInputField = inputDateField[0].getBoundingClientRect();
			var targetPosition = ev.originalEvent.detail.event.target.parentElement.getBoundingClientRect();
			var dateValue =  '';
			var inputValue = '';
			inputDateField.val('');
			inputDateField.css({ "top": 0, "left": 0 })
			inputDateField.css({ "top": (targetPosition.top + targetPosition.height) - positionInputField.top, "left": targetPosition.left - positionInputField.left });
			inputDateField.datetimepicker({
				value: actionDetail.value,
				step: 30,
				timepicker: (actionDetail.type === 'DATE') ? false : true,
				format: (actionDetail.type === 'DATE') ? 'm/d/Y' : 'n/j/Y H:i',
				onChangeDateTime: function (dp, $input) {
					updateSaveValues = false;
					inputValue = $input.val();
					if (actionDetail.type === 'DATE_TIME' && inputValue.slice(-2) !== "30") {
						inputValue = inputValue.slice(0, -2) + "00";
					}
					var date = dp;
					if (actionDetail.type === 'DATE_TIME' && date.getMinutes() != 30) {
						date.setMinutes(00);
					}
					var value = date.getTime();
					value = Math.round(value / 1000);
					dateValue = value;
				},
				onClose: function () {
					if(dateValue) {
						if (actionDetail.isEditMode) {
							if(inputValue) {
								var target = actionDetail.target;
								var row = target.closest('.row');
								row.querySelector('.dropDown-wrapper').querySelector('div').innerHTML = inputValue;
								row.querySelector('.remove-data').classList.remove('hide');
							}
							vanillaGrid.utils.steps.saveStepFieldValues(actionDetail.stepId, actionDetail.cellKey, inputValue, '');
							/*columnValues = actionDetail.columnsValues || {};
							columnValues[actionDetail.cellKey] = {
								value: inputValue,
								selectedOptions: ''
							}
							hasColumnValues = true;*/
							//onlyUpdateGridValueIssueDetail = true;
							//issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns);
						} else {
							var customFieldValues = {};
							var customFieldValueId;
							var data = {};
							ZEPHYR.ISSUE.steps.map(function (step) {
								if (step.id === actionDetail.stepId && step.customFields) {
									customFieldValueId = step.customFields[actionDetail.cellKey] && step.customFields[actionDetail.cellKey].customFieldValueId;
								}
							});

							var obj = {
								customFieldId: actionDetail.cellKey,
								customFieldType: actionDetail.type,
								value: dateValue
							}

							if (customFieldValueId) {
								obj['customFieldValueId'] = customFieldValueId;
							}

							customFieldValues[actionDetail.cellKey] = obj;
							data['customFieldValues'] = customFieldValues;
							data['id'] = actionDetail.stepId;
							updateTestStep(data, actionDetail);
						}
					}
					inputDateField.datetimepicker('destroy');
					inputDateField.css({ "top": 0, "left": 0 });
				}
			});
			inputDateField.datetimepicker('show');
			obj.dialogueElement = AJS.$('.xdsoft_datetimepicker')[0];
			obj.inputDateField = inputDateField;
			ZEPHYR.GRID.scrollableDialogue.push(obj);
		}
	}
	else if (actionDetail.customEvent === 'largeview') {
		AJS.$('.issueViewPopover').addClass('showPopup');
		isPopupOpen = true;
		issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, undefined, true);
	}
	else if (actionDetail.customEvent === 'exportvalue') {
		exportTestStep(actionDetail.exportId);
	}
	else if (actionDetail.customEvent === 'clearvalue') {
		configUpdateTrigger = !configUpdateTrigger;
		onlyUpdateGridValueIssueDetail = true;
		clearValue = true;
		var tempConfig = actionDetail.currentConfig;
		var cellConfig = [];
			cellConfig.header = [];
		var rowId = actionDetail.currentConfig.row.length - 1;
		tempConfig.head.forEach(function(header) {
		  	cellConfig.header.push(header);
		});
		cellConfig.row = actionDetail.currentConfig.row[rowId];
		vanillaGrid.templates.partialRender(actionDetail.containerId, rowId.toString(), cellConfig, tempConfig);
	}
	else if (actionDetail.customEvent === 'scrollDown') {
		 focusNewTestStep(actionDetail.containerId);
		// if (isPopupOpen)
		// 	AJS.$('.gridComponent').scrollTop(AJS.$('.gridComponent')[0].scrollHeight);
		// else
		// 	AJS.$('#project-config-panel-versions').scrollTop(AJS.$('#project-config-panel-versions')[0].scrollHeight);

		//document.querySelector('#unfreezedGridBody div[data-columnid="step"] textarea').focus();
	} else if (actionDetail.customEvent === 'deleteRow') {
		deleteConfirmationDialog(actionDetail.rowDetail);
	} else if (actionDetail.customEvent === 'cloneRow') {
		var _stepObj = actionDetail.rowDetail;
		if(!_stepObj.hasOwnProperty('step')) {
			var selectedStepObj = ZEPHYR.ISSUE.steps.filter(function(step){
					return step.id == actionDetail.rowDetail.id;
			})[0];
			_stepObj['step'] = selectedStepObj ? selectedStepObj.step : '';
		}
		cloneStepConfirmationDialog(_stepObj);
	} else if (actionDetail.customEvent === 'addAttachment') {
		var url = contextPath + "/secure/AttachFileAction!default.jspa?entityId=" + actionDetail.rowDetail + "&entityType=TESTSTEP&projectId=&id=";
		AJS.$('#zephyr-file-dialog').attr('href', url);
		AJS.$('#zephyr-file-dialog').trigger('click');
	} else if (actionDetail.customEvent === 'deleteAttachment') {
		var attachment = actionDetail.rowDetail.split(':');
		deleteAttachmentConfirmationDialog(attachment[0], attachment[1], attachment[2]);
	} else if (actionDetail.customEvent === 'addstep') {
		var customFieldValues = {};
		var data = actionDetail.columnsValues[Object.keys(actionDetail.columnsValues)[0]] || {};
		allCustomFields.map(function (customField) {
			if (data[customField.id]) {
				var value;
				if (customField.fieldType === 'DATE' || customField.fieldType === 'DATE_TIME') {
					var date = new Date(data[customField.id].value);
					value = date.getTime();
					value = Math.round(value / 1000);
				} else {
					value = data[customField.id].value;
				}
				var obj;
				if (customField.fieldType == 'CHECKBOX' || customField.fieldType == 'RADIO_BUTTON' || customField.fieldType == 'MULTI_SELECT' || customField.fieldType == 'SINGLE_SELECT') {
					obj = {
						customFieldId: customField.id,
						customFieldType: customField.fieldType,
						entityType: customField.entityType,
						value: data[customField.id].selectedOptions + [],
						selectedOptions: value || '',
					}
				} else {
					obj = {
						customFieldId: customField.id,
						customFieldType: customField.fieldType,
						entityType: customField.entityType,
						value: value + [],
						selectedOptions: '',
					}
				}
				customFieldValues[customField.id] = obj;
				delete data[customField.id];
			}
		});
		Object.keys(data).map(function (key) {
			data[key] = data[key] && data[key].value;
		});
		data['customFieldValues'] = customFieldValues;
		addTestStep(data, isPopupOpen);
	}
	else if (actionDetail.customEvent === 'movestep') {
		var data = {}
		var moveBetweenPages = true;
		if (actionDetail.actionName === 'moveToPrev') {
			data = { after: '/jira/rest/zephyr/latest/teststep/' + JIRA.Issue.getIssueId() + '/' + ZEPHYR.ISSUE.testDetails.prevObj.id }
		} else if (actionDetail.actionName === 'moveToNext')  {
			if (ZEPHYR.ISSUE.testDetails.isLastElement) {
				data = { position: "First" };
			} else {
				data = { after: '/jira/rest/zephyr/latest/teststep/' + JIRA.Issue.getIssueId() + '/' + ZEPHYR.ISSUE.testDetails.nextObj.id }
			}
		}
		else {
			moveBetweenPages = false;
			if (actionDetail.position === 'First') {
				if (ZEPHYR.ISSUE.testDetails.firstElementOnNextPage.id) {
					data = { after: '/jira/rest/zephyr/latest/teststep/' + JIRA.Issue.getIssueId() + '/' + ZEPHYR.ISSUE.testDetails.firstElementOnNextPage.id }
				} else if (ZEPHYR.ISSUE.testDetails.isLastElement) {
					data = { after: '/jira/rest/zephyr/latest/teststep/' + JIRA.Issue.getIssueId() + '/' + ZEPHYR.ISSUE.testDetails.nextObj.id }
				} else {
					moveBetweenPages = false;
					data = { position: "First" };
				}
			} else {
				data = { after: '/jira/rest/zephyr/latest/teststep/' + JIRA.Issue.getIssueId() + '/' + actionDetail.position }
			}
		}
		moveTestStep(data, actionDetail.id, moveBetweenPages);
	} else if (actionDetail.customEvent === 'viewImage') {
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
	} else if (actionDetail.actionName === 'columnChooser') {
		var data = {}
		var keys = Object.keys(ZEPHYR.ISSUE.stepColumns);
        var columnKeysArr = [];
		actionDetail.columnDetails.map(function (column) {
			if (column.key !== "issueKey" && column.key !== "orderId") {
				var key = column.key;
				if (key === 'step') {
					key = 'teststep';
				} else if (key === 'data') {
					key = 'testdata';
				} else if (key === 'result') {
					key = 'testresult';
				} else if (key === 'attachmentsMap') {
					key = 'attachment';
				}
				if (ZEPHYR.ISSUE.stepColumns[key]) {
					if(column.isVisible) {
						ZEPHYR.ISSUE.stepColumns[key].isVisible = "true";
					} else {
						ZEPHYR.ISSUE.stepColumns[key].isVisible = "false";
					}
				} else{
					ZEPHYR.ISSUE.stepColumns[key] = {
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
	      		delete ZEPHYR.ISSUE.stepColumns[keys[i]];
	      	}
	      }
	  }

		data['preferences'] = ZEPHYR.ISSUE.stepColumns;
		jQuery.ajax({
			url: getRestURL() + "/preference/setteststepcustomization",
			type: "post",
			contentType: "application/json",
			data: JSON.stringify(data),
			dataType: "json",
			success: function (response) {
				checkColumnFreeze(ZEPHYR.ISSUE.stepColumns);
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen);
				// stepInlineDialog && stepInlineDialog.hide();
			}
		});
	}
}

var checkColumnFreeze = function (columns) {
	var i = 0;
	var columnsKeys = Object.keys(columns);
	var keysLength = columnsKeys.length;

	if (keysLength > 4) {
		for (var prop in columns) {
			if (columns[prop].isVisible === "true" && columns[prop].entityType == "TESTSTEP") {
				i++;
			}
		}
	}
	//commented out for ZFJ-4082
	// if (i > 7 && columns["teststep"].isVisible) {
	// 	testDetailFreezeColumn = true;
	// }
};

var moveTestStep = function (data, stepId, moveBetweenPages) {
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + '/' + stepId + '/move' + '?offset=' + ZEPHYR.ISSUE.testDetails.offset + '&limit=' + ZEPHYR.ISSUE.testDetails.limit,
		type: "POST",
		contentType: "application/json",
		data: JSON.stringify(data),
		dataType: "json",
		success: function (response) {
			ZEPHYR.ISSUE.steps = response.stepBeanCollection;
			ZEPHYR.ISSUE.testDetails.prevObj = response.prevTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.nextObj = response.nextTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.isLastElement = response.isLastElementOnPage || false;
			ZEPHYR.ISSUE.testDetails.firstElementOnNextPage = response.firstElementOnNextPage || {};
			ZEPHYR.ISSUE.testDetails.maxRecords = ZEPHYR.ISSUE.steps[0] && ZEPHYR.ISSUE.steps[0].totalStepCount;

			var lastPageLowerLimit;
			for (var counter = 0; counter < parseInt(ZEPHYR.ISSUE.testDetails.maxRecords); counter += 1) {
				if (counter % parseInt(ZEPHYR.ISSUE.testDetails.limit) == 0) {
					lastPageLowerLimit = counter;
				}
			}
			if (ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit) {
				var paginationDataHtml = ZEPHYR.Templates.Steps.paginationComponent({ limit: parseInt(ZEPHYR.ISSUE.testDetails.limit), offset: parseInt(ZEPHYR.ISSUE.testDetails.offset), maxRecords: parseInt(ZEPHYR.ISSUE.testDetails.maxRecords), lastPageLowerLimit: lastPageLowerLimit });
				AJS.$('.pagination-outer-container').html(paginationDataHtml);
				AJS.$('#testDetailGrid').addClass('hasPagination');
				AJS.$('#testDetailGridPopover').addClass('hasPagination');
			} else {
				AJS.$('.pagination-outer-container').html('');
				AJS.$('#testDetailGrid').removeClass('hasPagination');
				AJS.$('#testDetailGridPopover').removeClass('hasPagination');
			}
			initialCountIssueDetail = ZEPHYR.ISSUE.steps.length;
			onlyUpdateGridValueIssueDetail = true;
			if (moveBetweenPages) {
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen);
			} else {
				var partialRenderObj = {
					isPartialRender : true,
					type: 'move',
					stepId: stepId,
					data : data
				}
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen,false, partialRenderObj);
			}
		},
		error : function(response, status, error) {
			console.log('error occured----',error);
		}
	});
}

var stepGridValueUpdated = function (ev) {
	dummyConfig = ev.originalEvent.detail.testConfig;
	ev.preventDefault();
	ev.stopPropagation();
	var actionDetail = ev.originalEvent.detail;
	var data = {};
	var customFieldValues = {};
	var key = Object.keys(actionDetail.updatedValue)[0];

	data[key] = actionDetail.updatedValue[key];

	allCustomFields.map(function (customField) {
		if (data[customField.id]) {
			var customFieldValueId;
			ZEPHYR.ISSUE.steps.map(function (step) {
				if (step.id === actionDetail.stepId && step.customFields) {
					customFieldValueId = step.customFields[customField.id] && step.customFields[customField.id].customFieldValueId;
				}
			});
			var obj;
			if (customField.fieldType == 'CHECKBOX' || customField.fieldType == 'RADIO_BUTTON' || customField.fieldType == 'MULTI_SELECT' || customField.fieldType == 'SINGLE_SELECT') {
				obj = {
					customFieldId: customField.id,
					customFieldType: customField.fieldType,
					entityId: actionDetail.stepId,
					entityType: customField.entityType,
					value: data[customField.id].selectedOptions ? data[customField.id].selectedOptions + [] : '',
					selectedOptions: data[customField.id].value || '',
				}
			} else {
				obj = {
					customFieldId: customField.id,
					customFieldType: customField.fieldType,
					entityId: actionDetail.stepId,
					entityType: customField.entityType,
					value: data[customField.id].value + [],
					selectedOptions: '',
				}
			}

			if (customFieldValueId) {
				obj['customFieldValueId'] = customFieldValueId;
			}
			customFieldValues[customField.id] = obj;
			delete data[customField.id];
		}
	});
	Object.keys(data).map(function (key) {
		data[key] = data[key].value;
	});
	if (Object.keys(actionDetail.updatedValue).length) {
		data['customFieldValues'] = customFieldValues;
		data['id'] = actionDetail.stepId;
		updateTestStep(data, actionDetail);
	}
}

var focusNewTestStep = function (containerId) {
	var grid = document.getElementById(containerId);
	var scrollHeight = grid.querySelector('.table-container').clientHeight;
	grid.querySelector('.table-container-wrapper').scrollTop = scrollHeight;
	var elementToFocus = grid.querySelectorAll('div[data-stepid="-1"]')[2].querySelector('textarea');
	if(elementToFocus) {
		elementToFocus.focus();
	}
}

var updateTestStep = function (data, stepConfig) {
	var tempConfig = stepConfig.config;
	 		cellConfig = {},
	 		rowId = stepConfig.rowId;
  cellConfig.header = tempConfig.head.filter(function(header){
    return header.key.toString() === stepConfig.key
  })[0];
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + '/' + data.id,
		type: "PUT",
		contentType: "application/json",
		data: JSON.stringify(data),
		dataType: "json",
		success: function (response) {

			response.customFields && (Object.keys(response.customFields).map(function (field) {
				var customField = response.customFields[field];
				if (customField.customFieldType === 'DATE') {
					response[customField.customFieldId] = convertDate({ value: customField.value, isDateTime: false });
				} else if (customField.customFieldType === 'DATE_TIME') {
					response[customField.customFieldId] = convertDate({ value: customField.value, isDateTime: true });
				}
				else if (customField.customFieldType === 'NUMBER' && customField.value != '') {
					response[customField.customFieldId] = customField.value;
				} else if (customField.customFieldType === 'TEXT' || customField.customFieldType === 'LARGE_TEXT') {
					response[customField.customFieldId] = customField.value;
					response[customField.customFieldId + 'htmlValue'] = customField.htmlValue;
				} else {
					response[customField.customFieldId] = customField.selectedOptions;
				}
			}));
			ZEPHYR.ISSUE.steps.map(function (steps, index) {
				if (steps.id === data.id) {
					ZEPHYR.ISSUE.steps[index] = response;
					updatedRowId = data.id;
					cellConfig.row = response;
				}
			});
			onlyUpdateGridValueIssueDetail = true;

			vanillaGrid.templates.partialRender(stepConfig.containerId, rowId.toString(), cellConfig, tempConfig);
			//document.getElementById('stepsGridComponentContainer').childNodes[0].dispatchEvent(new CustomEvent('onVanillaGridUpdate', { detail: {}, bubbles: true, composed: true }));
			//issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns);
		}, error: function (xhr, status, error) {
			cellConfig.row = ZEPHYR.ISSUE.steps[parseInt(rowId)];
			showErrorMessage(JSON.parse(xhr.responseText).errorMessages);
			vanillaGrid.templates.partialRender(stepConfig.containerId, rowId.toString(), cellConfig, tempConfig);
		},
	});
}

var addTestStep = function (data, isPopupOpen) {
	var tableGridId = !isPopupOpen ? 'testDetailGrid' : 'testDetailGridPopover';
	var tableGrid = document.getElementById(tableGridId);
	var scrollTopPos;
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId(),
		type: "POST",
		contentType: "application/json",
		data: JSON.stringify(data),
		dataType: "json",
		success: function (response) {
			ZEPHYR.ISSUE.testDetails.maxRecords = response.totalStepCount;
			if (response.totalStepCount > ZEPHYR.ISSUE.testDetails.limit + ZEPHYR.ISSUE.testDetails.offset) {
				if ((response.totalStepCount % ZEPHYR.ISSUE.testDetails.limit) == 0) {
					ZEPHYR.ISSUE.testDetails.offset = parseInt(response.totalStepCount / ZEPHYR.ISSUE.testDetails.limit) * ZEPHYR.ISSUE.testDetails.limit;
					if (ZEPHYR.ISSUE.testDetails.offset != 0) {
						ZEPHYR.ISSUE.testDetails.offset -= ZEPHYR.ISSUE.testDetails.limit;
					}
				} else {
					ZEPHYR.ISSUE.testDetails.offset = parseInt(response.totalStepCount / ZEPHYR.ISSUE.testDetails.limit) * ZEPHYR.ISSUE.testDetails.limit;
				}
				loadMoreData(true);
			} else {
				ZEPHYR.ISSUE.steps.push(response);
				updatedRowId = response.id;
				onlyUpdateGridValueIssueDetail = true;
				addedNewRow = true;
				initialCountIssueDetail = ZEPHYR.ISSUE.steps.length;
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen, true);
				// scrollTopPos = tableGrid.querySelector('.table-container-wrapper').offsetHeight;
				// tableGrid.querySelector('.table-container-wrapper').scrollTop = scrollTopPos;
				// tableGrid.querySelectorAll('div[data-stepid="-1"]')[2].querySelector('textarea').focus();
			}
		}
	});
}

var deleteTestStep = function (teststep, completed) {
	var isGridToBeRefreshed = false;
	if (ZEPHYR.ISSUE.steps.length == 1 && ZEPHYR.ISSUE.testDetails.offset!=0) {
		ZEPHYR.ISSUE.testDetails.offset -= ZEPHYR.ISSUE.testDetails.limit;
		isGridToBeRefreshed = true;
	}
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + '/' + teststep.id + '?id=' + teststep.id + '&offset=' + ZEPHYR.ISSUE.testDetails.offset + '&limit=' + ZEPHYR.ISSUE.testDetails.limit,
		type: "DELETE",
		contentType: "application/json",
		success: function (response) {
			ZEPHYR.ISSUE.steps = response.stepBeanCollection;
			ZEPHYR.ISSUE.testDetails.prevObj = response.prevTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.nextObj = response.nextTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.isLastElement = response.isLastElementOnPage || false;
			ZEPHYR.ISSUE.testDetails.firstElementOnNextPage = response.firstElementOnNextPage || {};
			ZEPHYR.ISSUE.testDetails.maxRecords = ZEPHYR.ISSUE.steps[0] && ZEPHYR.ISSUE.steps[0].totalStepCount;

			var lastPageLowerLimit;
			for (var counter = 0; counter < parseInt(ZEPHYR.ISSUE.testDetails.maxRecords); counter += 1) {
				if (counter % parseInt(ZEPHYR.ISSUE.testDetails.limit) == 0) {
					lastPageLowerLimit = counter;
				}
			}
			if (ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit) {
				var paginationDataHtml = ZEPHYR.Templates.Steps.paginationComponent({ limit: parseInt(ZEPHYR.ISSUE.testDetails.limit), offset: parseInt(ZEPHYR.ISSUE.testDetails.offset), maxRecords: parseInt(ZEPHYR.ISSUE.testDetails.maxRecords), lastPageLowerLimit: lastPageLowerLimit });
				AJS.$('.pagination-outer-container').html(paginationDataHtml);
				AJS.$('#testDetailGrid').addClass('hasPagination');
				AJS.$('#testDetailGridPopover').addClass('hasPagination');
			}
			else {
				AJS.$('.pagination-outer-container').html('');
				AJS.$('#testDetailGrid').removeClass('hasPagination');
				AJS.$('#testDetailGridPopover').removeClass('hasPagination');
			}
			if (completed)
				completed.call(this, isGridToBeRefreshed);
		}, error: function (response) {
			showErrorMessage(JSON.parse(response.responseText).errorMessages);
			if (completed) {
				completed.call(this, false);
			}
		}
	});
}

var cloneTestStep = function (teststep, stepPosition, completed) {
	var step;
	if (teststep.step && teststep.step != '') {
		var stepMarkup = teststep.step;
		if (/^\s*(#|h[1-6]\.|\*|\|\||bq.|\-|{quote})/.test(stepMarkup)) { // Check for the block element syntax with space in the start.
			step = 'CLONE - ' + ' \n' + teststep.step;
		} else
			step = 'CLONE - ' + ' ' + teststep.step;
	}
	var data = {
		'data': teststep.data,
		'position': stepPosition,
		'result': teststep.result,
		'step': step,
	}
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + "/clone/" + teststep.id + '?offset=' + ZEPHYR.ISSUE.testDetails.offset + '&limit=' + ZEPHYR.ISSUE.testDetails.limit,
		type: "POST",
		contentType: "application/json",
		data: JSON.stringify(data),
		dataType: "json",
		success: function (response) {
			ZEPHYR.ISSUE.steps = response.stepBeanCollection;
			ZEPHYR.ISSUE.testDetails.prevObj = response.prevTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.nextObj = response.nextTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.isLastElement = response.isLastElementOnPage || false;
			ZEPHYR.ISSUE.testDetails.firstElementOnNextPage = response.firstElementOnNextPage || {};
			ZEPHYR.ISSUE.testDetails.maxRecords = ZEPHYR.ISSUE.steps[0] && ZEPHYR.ISSUE.steps[0].totalStepCount;

			var lastPageLowerLimit;
			for (var counter = 0; counter < parseInt(ZEPHYR.ISSUE.testDetails.maxRecords); counter += 1) {
				if (counter % parseInt(ZEPHYR.ISSUE.testDetails.limit) == 0) {
					lastPageLowerLimit = counter;
				}
			}
			if (ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit) {
				var paginationDataHtml = ZEPHYR.Templates.Steps.paginationComponent({ limit: parseInt(ZEPHYR.ISSUE.testDetails.limit), offset: parseInt(ZEPHYR.ISSUE.testDetails.offset), maxRecords: parseInt(ZEPHYR.ISSUE.testDetails.maxRecords), lastPageLowerLimit: lastPageLowerLimit });
				AJS.$('.pagination-outer-container').html(paginationDataHtml);
				AJS.$('#testDetailGrid').addClass('hasPagination');
				AJS.$('#testDetailGridPopover').addClass('hasPagination');
			} else {
				AJS.$('.pagination-outer-container').html('');
				AJS.$('#testDetailGrid').removeClass('hasPagination');
				AJS.$('#testDetailGridPopover').removeClass('hasPagination');
			}
			if (completed)
				completed.call();
		}
	});
}

var addAttachmentConfirmationDialog = function () {
	var dialog = new JIRA.FormDialog({
		id: "zephyr-attach-file-dialog",
		trigger: "#zephyr-file-dialog",
		handleRedirect: true,
		issueMsg: 'thanks_issue_worklogged',
		onContentRefresh: function () {
			setTextOverflowEllipses();
		}
	});

	dialog.show();
}

var deleteConfirmationDialog = function (teststep) {
	// ZEPHYR.ISSUE.steps.map(function (row, index) {
	// 	if (row.id === teststep.id ) {
	// 		updatedGridDataIssueDetail = {
	// 			index: index,
	// 			deleteRow: true,
	// 		}
	// 	}
	// });
	var dialog = new JIRA.FormDialog({
		id: "entity-" + teststep.id + "-delete-dialog",
		content: function (callback) {
			/*Short cut of creating view, move it to Backbone View and do it in render() */
			var innerHtmlStr = ZEPHYR.Templates.Steps.deleteStepsConfirmationDialog({ teststep: teststep });
			callback(innerHtmlStr);
		},

		submitHandler: function (e) {
			deleteTestStep(teststep, function (isGridToBeRefreshed) {
				// onlyUpdateGridValueIssueDetail = true;
				var partialRenderObj = {};
				if (!isGridToBeRefreshed) {
					partialRenderObj = {
						type: 'deleteRow',
						isPartialRender:true,
						rowId: teststep.rowId,
					}
				}
				initialCountIssueDetail = ZEPHYR.ISSUE.steps.length;
				issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen, false, partialRenderObj);
				dialog.hide();
			});
			e.preventDefault();
		}
	});

	dialog.show();
}

var cloneStepConfirmationDialog = function (teststep) {
	var instance = this,
		dialog = new JIRA.FormDialog({
			id: "entity-" + teststep.id + "-clone-dialog",
			content: function (callback) {
				/*Short cut of creating view, move it to Backbone View and do it in render() */
				var innerHtmlStr = ZEPHYR.Templates.Steps.cloneStepConfirmationDialog({ teststep: teststep });
				callback(innerHtmlStr);
				AJS.$('#clone-insert-at').bind('focusin', function () {
					AJS.$('#teststep-clone-insertat-error').html('');
					AJS.$('input[name=clone-append]:checked').prop('checked', false);
				});
			},

			submitHandler: function (ev) {
				ev.preventDefault();
				var positionLength = ZEPHYR.ISSUE.testDetails.maxRecords + 1;
				//var step = AJS.$(ev.target).find('#clone-test-step').val();
				var insertAt = AJS.$.trim(AJS.$(ev.target).find('input#clone-insert-at').val());
				var stepPosition = AJS.$(ev.target).find('input[name=clone-append]:checked').val();
				if (stepPosition || (insertAt != '' && insertAt >= 1 && insertAt <= positionLength)) {
					stepPosition = stepPosition || insertAt;
					cloneTestStep(teststep, stepPosition, function () {
						initialCountIssueDetail = ZEPHYR.ISSUE.steps.length;
						var partialRenderObj = {
							isPartialRender : true,
							type: 'clone',
							position : stepPosition,
							stepId:teststep.id
						}
						dialog.hide();
						issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen, false, partialRenderObj);
						AJS.$('#clone-insert-at').unbind('focusin');
					});
				} else {
					AJS.$('#teststep-clone-insertat-error').html(AJS.I18n.getText("cycle.operation.clone.error.position", positionLength));
					AJS.$('input#assign-issue-submit').prop('disabled', false);
					AJS.$('.teststep-clone-dialog-form .loading').remove();
				}
			}
		});
	dialog.show();
}

function attachFiles(entityId, entityType) {
	data = AJS.$("form#zephyr-attach-file").serialize();
	if (data.indexOf('&filetoconvert=') < 0) {
		alert("Please select at least one file to attach.");
		return;
	}

	if (data.indexOf('&comment=') > 0) {
		var begin = data.indexOf('&comment=') + 9;
		var end;
		if (data.indexOf('&commentLevel=') > 0) {
			end = data.indexOf('&commentLevel=');
		} else {
			end = data.length;
		}

		var commentText = data.substring(begin, end);

		if (commentText.length > 250) {
			alert("The comment text entered exceeds the maximum length. Maximum 250 characters are allowed.");
			return;
		}
	}
	jQuery.ajax({
		url: AJS.$("form#zephyr-attach-file").attr("action") + "&inline=true&decorator=dialog",
		type: "post",
		contentType: "application/x-www-form-urlencoded",
		enctype: 'multipart/form-data',
		data: AJS.$("form#zephyr-attach-file").serialize(),
		dataType: "html",
		complete: function (xhr, status, response) {
			if (xhr.status != 200) {
				JE.gadget.zephyrError(xhr);
			} else {
				//Recreate Div
				getAttachments(entityId, entityType);
				AJS.$("#attach-file-cancel").click();
			}
		}
	});
}

function getAttachments(entityId, entityType) {
	jQuery.ajax({
		url: getRestURL() + "/attachment/attachmentsByEntity?entityId=" + entityId + "&entityType=" + entityType,
		type: "get",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			var rowData,rowId;
			ZEPHYR.ISSUE.steps.map(function (steps,index) {
				if (steps.id === Number(entityId)) {
					steps.attachmentsMap = response.data;
					updatedRowId = steps.id;
					rowData = steps;
					rowId = index;
				}
			});
			onlyUpdateGridValueIssueDetail = true;
			//vanillaGrid.templates.partialRender(stepConfig.containerId, rowId.toString(), cellConfig, tempConfig);
			var partialRenderObj = {
				isPartialRender : true,
				type: 'attachment',
				rowId : rowId,
				rowData : rowData,
			}
			issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen, false, partialRenderObj);
		}
	});
	return;
}

var deleteAttachmentConfirmationDialog = function (rowId, entityId, entityN) {
	var instance = this,
		dialog = new JIRA.FormDialog({
			id: "entity-" + entityId + "-delete-dialog",
			content: function (callback) {
				/*Short cut of creating view, move it to Backbone View and do it in render() */
				var innerHtmlStr = ZEPHYR.Project.Confirmation.deleteConfirmationDialog({ entityName: entityN });
				callback(innerHtmlStr);
			},

			submitHandler: function (e) {
				deleteAttachmentSteps(entityId, function () {
					ZEPHYR.ISSUE.steps.map(function (steps) {
						if (steps.id === Number(rowId)) {
							steps.attachmentsMap = steps.attachmentsMap.filter(function (attachment) {
								return attachment.fileId !== entityId;
							})
							updatedRowId = steps.id;
						}
					});
					onlyUpdateGridValueIssueDetail = true;
					issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen);
					dialog.hide();
				});
				e.preventDefault();
			}
		});

	dialog.show();
}

function convertDate(data) {
	if (data.value) {
		date = new Date(Number(data.value));
		h = date.getHours(), m = date.getMinutes();
		if (m < 10) {
			m = "0" + m;
		}
		if (h < 10) {
			h = "0" + h;
		}
		time = h + ':' + m;
		if (data.isDateTime) {
			return dateStr = (date.getMonth() + 1) + '/' + date.getDate() + '/' + date.getFullYear() + ' ' + time;
		} else {
			return dateStr = (date.getMonth() + 1) + '/' + date.getDate() + '/' + date.getFullYear();
		}
	} else {
		return '';
	}
}

AJS.$('#close-test-details-popup').live('click', function () {
	AJS.$('.issueViewPopover').removeClass('showPopup');
	isPopupOpen = false;
	issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns,undefined , false );
})

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
	var imageCaroselHtml = ZEPHYR.Templates.Steps.caroselView({ image: imageUrl, altImage: imageObject, changeFlag: flagObject });
	AJS.$('#project-config-panel-versions').append(imageCaroselHtml);
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
				showImageCarousel(attachmentDuplicate[counter + 1]);
			}
			break;
		}
	}
}


// AJS.$('.change-pagination-width-function').live('click', function (event) {
// 	var that = this;

// 	var sendingData = pagePreferencesData;
// 	sendingData.testDetailsIssueViewGridWidth = parseInt(event.target.dataset.entries);

// 	jQuery.ajax({
// 		url: getRestURL() + "/preference/paginationWidthPreference",
// 		type: "PUT",
// 		contentType: "application/json",
// 		data: JSON.stringify(sendingData),
// 		dataType: "json",
// 		success: function (response) {
// 			pagePreferencesData = response;
// 			ZEPHYR.ISSUE.testDetails.offset = 0;
// 			ZEPHYR.ISSUE.testDetails.limit = event.target.dataset.entries;
// 			loadMoreData();
// 		}
// 	});

// 	// ZEPHYR.ISSUE.testDetails.offset = 0;
// 	// ZEPHYR.ISSUE.testDetails.limit = event.target.dataset.entries;
// 	// loadMoreData();

// 	//API CALL FUNCTION
// })

// AJS.$('#pagination-dropdown-button').live('click', function () {
// 	if(AJS.$('#pagination-options-container').hasClass('hide')) {
// 		AJS.$('#pagination-options-container').removeClass('hide');
// 	} else {
// 		AJS.$('#pagination-options-container').addClass('hide');
// 	}
// })

function paginationGoToPreviousPage() {
	ZEPHYR.ISSUE.testDetails.offset = parseInt(ZEPHYR.ISSUE.testDetails.offset) + parseInt(ZEPHYR.ISSUE.testDetails.limit);
	loadMoreData();
	// API CALL FUNCTION
}

function paginationGoToNextPage() {
	ZEPHYR.ISSUE.testDetails.offset = parseInt(ZEPHYR.ISSUE.testDetails.offset) - parseInt(ZEPHYR.ISSUE.testDetails.limit);
	loadMoreData();

	//API CALL FUNCTION
}

function firstPagePagination() {
	ZEPHYR.ISSUE.testDetails.offset = 0;
	loadMoreData();

	//API CALL FUNCTION
}

function lastPagePagination() {
	var lastPageLowerLimit;
	for (var counter = 0; counter < ZEPHYR.ISSUE.testDetails.maxRecords; counter += 1) {
		if (counter % ZEPHYR.ISSUE.testDetails.limit == 0) {
			lastPageLowerLimit = counter;
		}
	}
	ZEPHYR.ISSUE.testDetails.offset = lastPageLowerLimit;
	loadMoreData();

	//API CALL FUNCTION
}


function loadMoreData(isFocusNewTestStep) {
	jQuery.ajax({
		url: getRestURL() + "/teststep/" + JIRA.Issue.getIssueId() + '?offset=' + ZEPHYR.ISSUE.testDetails.offset + '&limit=' + ZEPHYR.ISSUE.testDetails.limit,
		type: "get",
		contentType: "application/json",
		dataType: "json",
		success: function (response) {
			// prevDataCount = offset;
			ZEPHYR.ISSUE.steps = response.stepBeanCollection;
			ZEPHYR.ISSUE.testDetails.prevObj = response.prevTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.nextObj = response.nextTestStepBean || {};
			ZEPHYR.ISSUE.testDetails.firstElementOnNextPage = response.firstElementOnNextPage || {};
			ZEPHYR.ISSUE.testDetails.isLastElement = response.isLastElementOnPage || false;
			ZEPHYR.ISSUE.testDetails.maxRecords = ZEPHYR.ISSUE.steps[0] && ZEPHYR.ISSUE.steps[0].totalStepCount;

			var lastPageLowerLimit;
			for (var counter = 0; counter < parseInt(ZEPHYR.ISSUE.testDetails.maxRecords); counter += 1) {
				if (counter % parseInt(ZEPHYR.ISSUE.testDetails.limit) == 0) {
					lastPageLowerLimit = counter;
				}
			}

			if (ZEPHYR.ISSUE.testDetails.maxRecords > ZEPHYR.ISSUE.testDetails.limit) {
				var paginationDataHtml = ZEPHYR.Templates.Steps.paginationComponent({ limit: parseInt(ZEPHYR.ISSUE.testDetails.limit), offset: parseInt(ZEPHYR.ISSUE.testDetails.offset), maxRecords: parseInt(ZEPHYR.ISSUE.testDetails.maxRecords), lastPageLowerLimit: lastPageLowerLimit });
				AJS.$('.pagination-outer-container').html(paginationDataHtml);
				AJS.$('#testDetailGrid').addClass('hasPagination');
				AJS.$('#testDetailGridPopover').addClass('hasPagination');
			} else {
				AJS.$('.pagination-outer-container').html('');
				AJS.$('#testDetailGrid').removeClass('hasPagination');
				AJS.$('#testDetailGridPopover').removeClass('hasPagination');
			}
			issueViewGridComponent(ZEPHYR.ISSUE.steps, allCustomFields, ZEPHYR.ISSUE.stepColumns, null, isPopupOpen,isFocusNewTestStep );

			// console.log(response);
		}
	});
}

function dialogueScroll(event) {
	if (event.originalEvent.detail.isOpen) {
		ZEPHYR.GRID.scrollableDialogue.push(event.originalEvent.detail);
	} else {
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function(dialogue){
			return dialogue.dialogueElement != event.originalEvent.detail.dialogueElement;
		});
	}
}

AJS.$(window).scroll(function (event) {
	ZEPHYR.GRID.scrollableDialogue.map(function(dialogue) {
		if (!dialogue.onWindowScrollDoNothing) {
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
		}
	});
});

function gridScrollEventCapture(event) {
	AJS.$('#testDetailGrid .table-container-wrapper, #testDetailGridPopover .table-container-wrapper, #testExecutionGrid .table-container-wrapper, .detail-panel').scroll(function (event) {
		var scrollableElement = event.currentTarget.getBoundingClientRect();
		ZEPHYR.GRID.scrollableDialogue = ZEPHYR.GRID.scrollableDialogue.filter(function (dialogue) {
			var returnValue = true;
			var isDetailsPanel = false;
			for (var counter = 0; counter < event.currentTarget.classList.length; counter += 1) {
				if (event.currentTarget.classList[counter] == 'detail-panel') {
					isDetailsPanel = true;
					break;
				}
			}
			if (!dialogue.onlyWindowScroll || isDetailsPanel) {
				var triggerElement = dialogue.target.getBoundingClientRect();
				var dropdownElement = dialogue.dialogueElement.getBoundingClientRect();
				var viewportHeight = window.innerHeight;
				var height = dialogue.dialogueElement.clientHeight;
				var topHeight = triggerElement.top + triggerElement.height + 2;
				if (dialogue.isActionDropdown) {
					if (scrollableElement.top > dropdownElement.top || scrollableElement.bottom < dropdownElement.top) {
						var list = document.getElementsByClassName('isOpen');
						for (var i = 0; i < list.length; i++) {
							list[i].classList.remove("isOpen");
						}
						if (document.getElementById("moveDropdown")) {
							document.getElementById("moveDropdown").parentNode.removeChild(document.getElementById("moveDropdown"));
						}
						returnValue = false;
					}
				}
				else if (scrollableElement.top > triggerElement.bottom || scrollableElement.bottom < triggerElement.top) {
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
						} else if(dialogue.isSelect) {
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
					} else if(dialogue.isDateTimePicker) {
						dialogue.inputDateField.datetimepicker('hide');
					} else if (dialogue.onlyWindowScroll) {
						dialogue.dialogueElement.classList.add('close');
						dialogue.dialogueElement.classList.remove('open');
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


	AJS.$('#testDetailGrid #unfreezedGrid, #testDetailGridPopover #unfreezedGrid, #testExecutionGrid #unfreezedGrid').scroll(function (event) {
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
					} else if (dialogue.isDateTimePicker) {
						dialogue.inputDateField.datetimepicker('hide');
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
				} else if(dialogue.isDefects) {
					dialogue.dialogueElement.style.left = triggerElement.left - parentWidth + triggerElement.width - dialogue.leftAdjust + 'px';
				} else {
					dialogue.dialogueElement.style.left = triggerElement.left - parentWidth + triggerElement.width + 'px';
				}
			}
			return returnValue;
		});
	});
}
