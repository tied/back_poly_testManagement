AJS.$.namespace("GH.gadget.utils");
AJS.$.namespace("JE.gadget.utils");

/**
 * Get the view name as defined in the url
 */
JE.gadget.utils.getViewName = function() {
	var regex = new RegExp("[\\?&]view=([^&#]*)");
    var viewType = regex.exec( window.location.href );
    return (viewType) ? viewType[1].toLowerCase() : '';
};

/**
 * Get the rest resource path based on the gadget load request.
 * This is to check of the gadget is bein accessed from within
 * Confluence Blueprints.
 */
JE.gadget.utils.getRestResourcePath = function() {
    return window.top.Confluence ? "zapi" : "zephyr";
};

JE.gadget.utils.htmlDecode = function(value) {
  return AJS.$('<div/>').html(value).text();
}

JE.gadget.utils.htmlEncode = function(value) {
  return AJS.$('<div/>').text(value).html();
}

/**
 * Generates the title html taking a list of items to display into account
 * @param gadget the gadget to create the dom for
 * @param data the parent element of project/version info objects
 * @param $prependTo a jquery object to which the header should be prepended to
 */
JE.gadget.utils.insertHeaderElements = function(elements, $prependTo) {
	// return if we have no items to display
	if (!elements || !elements.length || elements.length < 1) return;

	var titleHtml = '<h3 style="text-align:center;" id="zephyr-gadget-header">';
	AJS.$(elements).each(function(index, element){
		if (index > 0) titleHtml += ' / ';
		titleHtml += element;
	});
	titleHtml += '</h3>';

	AJS.$(titleHtml).prependTo($prependTo);
};

JE.gadget.utils.getProjectLink = function(gadget, projectKey) {
	return gadget.getBaseUrl() + '/browse/' + projectKey;
};

JE.gadget.utils.getVersionLink = function(gadget, projectKey, versionId) {
	return JE.gadget.utils.getProjectLink(gadget, projectKey) + '/fixforversion/' + versionId;
};

JE.gadget.utils.redirectParent = function(href){
	//Gadgets are in iframes, so window.top is required
	window.top.location.href = href;
}

/** Creates the project link markup. */
JE.gadget.utils.getProjectLinkMarkup = function(gadget, project) {
    if(project){
	var projectLink = JE.gadget.utils.getProjectLink(gadget, project.key);
	return '<a href="'+projectLink+'"><strong>'+JE.gadget.utils.htmlEncode(project.name)+'</strong> ('+project.key+')</a>';
    } else
    return "";
};

/** Creates the version link markup. */
JE.gadget.utils.getVersionLinkMarkup = function(gadget, project, version) {

    if(project && version){
    // generate version link if id available, project link otherwise
    if (version.id && version.id !=  "-1") {
        var link = JE.gadget.utils.getVersionLink(gadget, project.key, version.id);
    } else {
        var link = JE.gadget.utils.getProjectLink(gadget, project.key);
    }
    return '<a href="'+link+'" class="gg-days-version"><strong>'+JE.gadget.utils.htmlEncode(version.name)+'</strong></a>';
    } else
    return "";
};

/**
 * Generates the project/version title display
 * @param gadget the gadget to create the dom for
 * @param data the parent element of project/version info objects
 * @param $prependTo a jquery object to which the header should be prepended to
 */
JE.gadget.utils.getProjectVersionHeaderElements = function(gadget, data) {
	// add the title if requested
	var titleElements = [];
	var project = data.project;
	var version = data.version;

	// project
	var showProjectName = gadget.getPref('showProjectName') == 'true';
	if (showProjectName) titleElements.push(JE.gadget.utils.getProjectLinkMarkup(gadget, project));

	// version
	var showVersionName = gadget.getPref('showVersionName') == 'true';
	if (showVersionName) titleElements.push(JE.gadget.utils.getVersionLinkMarkup(gadget, project, version));

	return titleElements;
};

/**
 * Generates the project/version title display
 * @param gadget the gadget to create the dom for
 * @param data the parent element of project/version info objects
 * @param $prependTo a jquery object to which the header should be prepended to
 */
JE.gadget.utils.addProjectVersionHeader = function(gadget, data, $prependTo) {
	// fetch the elements to display
	var titleElements = JE.gadget.utils.getProjectVersionHeaderElements(gadget, data);
	JE.gadget.utils.insertHeaderElements(titleElements, $prependTo);
};


AJS.$.namespace("GH.gadget.fields");
AJS.$.namespace("JE.gadget.fields")
JE.gadget.fields.foldersArray = [];

/**
 * Creates a section title
 */
JE.gadget.fields.sectionTitle=function(gadget, id, label) {
	return {
        label: 'none',
        id: id,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
			// remove the automatically generated title
			parentDiv.parent().children('label').remove();
			// remove the automatically generated error field
			parentDiv.parent().children('#-error').remove();

			// now generate the html
			var html = '<h3>'+gadget.getMsg(label)+'</h3>';
			parentDiv.append(AJS.$(html));
        }
    };
}

/**
 * Creates a checkbox
 */
JE.gadget.fields.checkbox=function(gadget, checkboxPref, label, selected, description) {
	return {
        userpref: checkboxPref,
        label: gadget.getMsg(label),
        id: "checkbox_" + checkboxPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
			// add the checkbox class to the field-group
			//parentDiv.parent().addClass('checkbox');
			// remove the label added by the the caller code, as we want the label to appear after the input element
			//parentDiv.parent().children('label').remove();

			var checkboxId = checkboxPref+'_checkbox';
			var checkedHtml = selected ? ' checked' : '';
			var html = '<input type="checkbox" id="'+checkboxId+'" '+checkedHtml+' />';
			    //html +='<label for="'+checkboxId+'">'+gadget.getMsg(label)+'</label>';
			    html += '<input type="hidden" id="'+checkboxPref+'" name="'+checkboxPref+'" value="'+selected+'" />';
			parentDiv.append(AJS.$(html));

			// make sure we update the values that get submitted
			AJS.$('#'+checkboxId, parentDiv).each(function() {
				var checkbox = this;
				var $checkbox = AJS.$(checkbox);
				$checkbox.change(function() {
					var hiddenInputSelector = '#'+checkboxPref;
					AJS.$(hiddenInputSelector).attr('value', checkbox.checked);
				});
			});

			// add description
			if (description) {
				parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg(description)));
			}
        }
    };
}


/**
 * Creates a input field for group in test
 */
JE.gadget.fields.inputGroup = function(gadget, inputPref, label, selectedValue, groupFldUserPref) {
	return {
        userpref: inputPref,
        label: gadget.getMsg(label),
        id: "input_" + inputPref,
        type: "callbackBuilder",
        callback: function (parentDiv) {
        	var groupFldUserPrefValue = gadget.getPref(groupFldUserPref);
			var html = JE.gadget.fields.getInputHTMLBasedOnGroup(gadget, inputPref, selectedValue, groupFldUserPrefValue);
			parentDiv.append(AJS.$(html));
			var $groupFldSelectList = AJS.$("#" + groupFldUserPref);
			if(groupFldUserPrefValue == 'sprint-cycle') {
        		JE.gadget.fields.attachItemsPerGroup(gadget, parentDiv);
        	}
			$groupFldSelectList.change(function(event) {
            	var _groupFldUserPrefValue = $groupFldSelectList.val();
            	var _selectedValue = 0; // AJS.$("#" + inputPref).val();
            	var selectedGroupLabel = JE.gadget.fields.getGroupLabel(gadget, _groupFldUserPrefValue);
            	var _html = JE.gadget.fields.getInputHTMLBasedOnGroup(gadget, inputPref, _selectedValue);

            	parentDiv.html(_html);
            	AJS.$('#itemsPerGroup-field-group').remove();
            	AJS.$('#sortSprintBy-field-group').remove();
            	if(_groupFldUserPrefValue == 'sprint-cycle') {
            		JE.gadget.fields.attachItemsPerGroup(gadget, parentDiv);
            	}
            	AJS.$('label[for=' + inputPref + ']').html(selectedGroupLabel);
            	gadget.resize();
            });
        }
    };
}

JE.gadget.fields.attachItemsPerGroup = function(gadget, $parentDiv) {
	var _selectedItemsPerGroup = gadget.getPref('itemsPerGroup');
	var _selectedSortSprintBy = gadget.getPref('sortSprintBy');
	var _itemsLabel = gadget.getMsg('je.gadget.common.number.cycles.per.sprint.label');
	var _sortSprintByLabel = gadget.getMsg('je.gadget.common.sprint.sortby.label');
	var _sortByCountLabel = gadget.getMsg('je.gadget.common.sprint.sortby.count.label');
	var _sortByDateLabel = gadget.getMsg('je.gadget.common.sprint.sortby.date.label');
	var _sortByCountChecked = (_selectedSortSprintBy == 'count')? 'checked="checked"': '';
	var _sortByDateChecked = (_selectedSortSprintBy == 'date')? 'checked="checked"': '';
	var itemsPerGroupHTML = '<div class="field-group" id="itemsPerGroup-field-group"><label for="itemsPerGroup">' + _itemsLabel + '</label><div id="input_itemsPerGroup" class="builder-container"><input class="text zephyr_allow_only_number zephyr_numbers_no_decimals" type="text" id="itemsPerGroup" name="itemsPerGroup" value="' + _selectedItemsPerGroup + '"></div<div id="itemsPerGroup-error" class="error"></div></div>';
	itemsPerGroupHTML += '<div class="field-group" id="sortSprintBy-field-group"><label for="sortSprintBy">' + _sortSprintByLabel +
		'</label><div id="input_sortSprintBy" class="builder-container"><div id="radio_sortSprintBy" class="builder-container">' +
		'<input type="radio" id="sortSprintBy_count" name="sortSprintBy_radio"' + _sortByCountChecked + ' value="count">&nbsp;' + _sortByCountLabel +
		'&nbsp;&nbsp;<input type="radio" id="sortSprintBy_date" name="sortSprintBy_radio" ' + _sortByDateChecked + ' value="date">&nbsp;' + _sortByDateLabel + '<input type="hidden" id="sortSprintBy" name="sortSprintBy" value="' + _selectedSortSprintBy+ '"></div></div<div id="itemsPerGroup-error" class="error"></div></div>';
	$parentDiv.closest('.field-group').after(itemsPerGroupHTML);
	AJS.$('input[name=itemsXAxis], input[name=itemsPerGroup]').blur(function(ev) {
		if(isNaN(this.value)) {
			this.value.replace(/[^0-9\.]/g,'');
		}
		if(!this.value || this.value < 0) {
			this.value = 0;
		}
	});
	AJS.$('input[name=sortSprintBy_radio]').change(function() {
		var $hiddenSortSprintBy = AJS.$('#sortSprintBy');
		$hiddenSortSprintBy.attr('value', this.value);
	});
}

JE.gadget.fields.getInputHTMLBasedOnGroup = function(gadget, inputPref, selectedValue) {
	var html = '<input class="text zephyr_allow_only_number zephyr_numbers_no_decimals" type="text" id="'+inputPref+'" name="'+inputPref+'" value="'+selectedValue+'" />';
	return html;
}

JE.gadget.fields.getGroupLabel = function(gadget, groupFldUserPrefValue) {
	if(groupFldUserPrefValue == 'user') {
		return gadget.getMsg('je.gadget.common.number.users.label');
	} else if(groupFldUserPrefValue == 'component'){
		return gadget.getMsg('je.gadget.common.number.components.label');
	} else if(groupFldUserPrefValue == 'sprint-cycle') {
		return gadget.getMsg('je.gadget.common.number.sprints.label');
	} else {
		return gadget.getMsg('je.gadget.common.number.cycles.label');
	}
}

/**
 * Based on the groupFld, set the number of items
 */
JE.gadget.fields.getNumberOfItems = function(gadget, inputPref, groupFldUserPref, selectedValue) {
	var groupFldUserPrefValue = gadget.getPref(groupFldUserPref);
	var itemsXAxis = gadget.getPref(inputPref);
	var selectedGroupLabel = JE.gadget.fields.getGroupLabel(gadget, groupFldUserPrefValue);
	return JE.gadget.fields.inputGroup(gadget, inputPref, selectedGroupLabel, itemsXAxis, groupFldUserPref);
}

JE.gadget.fields.title=function(gadget, titlePref, label, value, maxChar, description) {
	return {
        userpref: titlePref,
        label: gadget.getMsg(label),
        id: "textfield_" + titlePref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
			// add the checkbox class to the field-group
			//parentDiv.parent().addClass('text');
			// remove the label added by the the caller code, as we want the label to appear after the input element
			//parentDiv.parent().children('label').remove();
			var maximumChars = maxChar || 50;
			var lbl = gadget.getMsg(label);
			var textFieldId = titlePref+'_textfield';
			var html = '<input type="text" class="text" id="'+textFieldId+'" value="'+value+'" maxlength="' + maximumChars + '" title="'+gadget.getMsg("je.gadget.common.maxChar.tooltip", [maximumChars])+'"/>';
			    html += '<input type="hidden" id="'+titlePref+'" name="'+titlePref+'" value="'+value+'" />';
			parentDiv.append(AJS.$(html));

			// make sure we update the values that get submitted
			AJS.$('#'+textFieldId, parentDiv).each(function() {
				var textInput = this;
				var $textInput = AJS.$(textInput);
				$textInput.change(function() {
					var hiddenInputSelector = '#'+titlePref;
					AJS.$(hiddenInputSelector).attr('value', textInput.value);
				});
			});

			// add description
			if (description) {
				parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg(description)));
			}
        }
    };
}

JE.gadget.fields.updateStatusesOptions = function(gadget, selectedProjectId, statusUserPref)
{
    var statusSelectList = AJS.$("#" + statusUserPref);
    statusSelectList.empty();

    var statuses = AJS.$.ajax({
        key: "statuses",
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/zchart/issueStatuses",
        contentType: "application/json",
        data: {
            projectId : selectedProjectId
        },
        success: function (response)
        {
        	statuses = response;
            var versionUserPrefValue = gadget.getPref(statusUserPref);

            var selectedOptions = versionUserPrefValue.split("|");

            AJS.$(statuses.IssueStatusesOptionsList).each(function (index,optionObject){
            	var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(optionObject.label);
                var statusOption = AJS.$("<option style='padding-left:2px;'/>").attr("value", optionObject.value).attr("title",optionObject.label).text(optionObject.label);
            	//var statusOption = AJS.$("<option />").val(optionObject.value).html(optionObject.label);
            	//alert(index,optionObject.label, "  ", statusOption);

                statusSelectList.append(statusOption);

                //if(selectedOptions.indexOf(optionObject.value) != -1) {
                if(AJS.$.inArray(optionObject.value,selectedOptions) != -1) {
                    statusOption.attr("selected","selected");
                }
            });
        }
    });
};

/**
 * Updates the cycleSelect Option dropdown with new values given a version selection list.
 * @param showUnscheduled should unscheduled versions be included in the list
 * @param showAutoVersion Should the next due version aka auto version be added to the list
 */
JE.gadget.fields.updateCycleSelectOptions = function(gadget,
														selectedProjectId,
														selectedVersionId,
														cycleUserPref, filterCyclesWithSprint)
{
	var cycleSelectList = AJS.$("#" + cycleUserPref);
	cycleSelectList.empty();
	JE.gadget.clearError();
	var cycleDetails = AJS.$.ajax({
		url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/cycle",
		type : "GET",
		contentType: "application/json",
		global: false,
		data: {
			versionId : selectedVersionId,
			projectId : selectedProjectId,
			expand: "executionSummaries"
		},
		success: function (response){
			cycleDetails = response;
            var cycleUserPrefValue = gadget.getPref(cycleUserPref);
            //Do not show the 'All cycles' option in the cycle drop down for pasttestexecution gadget.
            if(gadget.gadgetType !== "pastExecutionStatus") {
                var cycleOption = AJS.$("<option/>").attr("value", "-2").attr("title", gadget.getMsg('je.gadget.common.cycles.all.label')).text(gadget.getMsg('je.gadget.common.cycles.all.label'));
			    cycleSelectList.append(cycleOption);
            };

			// add all available cycles
    		AJS.$.each(cycleDetails, function(key, cycle) {
                //Do not show the 'Ad hoc' option in the cycle drop down for pasttestexecution gadget.
                if(gadget.gadgetType == "pastExecutionStatus" && cycle.name != 'Ad hoc') {

                    if(key != "recordsCount" && key != "offsetCount") {

                        if(filterCyclesWithSprint && cycle.sprintId) // If filter cycles with sprintId
                            return;
                        var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(cycle.name);
                        var cycleOption = AJS.$("<option/>").attr("value", key).attr("title",cycle.name).text(truncatedValue);
                        cycleSelectList.append(cycleOption);

                        if(cycleUserPrefValue == key) {					// if(cycleUserPrefValue == cycle.ID) {
                            cycleOption.attr("selected","selected");
                        }
                    }
                };
			});
		}
	});
};

/**
 * Updates the cycleSelect Option dropdown with new values given a version selection list.
 * @param showUnscheduled should unscheduled versions be included in the list
 * @param showAutoVersion Should the next due version aka auto version be added to the list
 */
JE.gadget.fields.updateCycleMultiSelectOptions = function(gadget,
                                                     selectedProjectId,
                                                     selectedVersionId,
                                                     cycleUserPref, skipAdhoc)
{
    var cycleSelectList = AJS.$("#" + cycleUserPref);
    cycleSelectList.empty();
    JE.gadget.clearError();
    var cycleDetails = AJS.$.ajax({
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/cycle",
        type : "GET",
        contentType: "application/json",
        global: false,
        data: {
            versionId : selectedVersionId,
            projectId : selectedProjectId,
            expand: "executionSummaries"
        },
        success: function (response){
            cycleDetails = response;
            var cycleUserPrefValue = gadget.getPref(cycleUserPref);
            var selectedOptions = cycleUserPrefValue.split("|");
            // add all available cycles
            AJS.$.each(cycleDetails, function(key, cycle) {
                if(key != "recordsCount" && key != "offsetCount") {
                    var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(cycle.name);
                    if(skipAdhoc && cycle.name != 'Ad hoc') {
                        var cycleOption = AJS.$("<option style='padding-left:2px;'/>").attr("value", key).attr("title", cycle.name).text(truncatedValue);
                        cycleSelectList.append(cycleOption);
                    } else if(!skipAdhoc) {
                        var cycleOption = AJS.$("<option style='padding-left:2px;'/>").attr("value", key).attr("title",cycle.name).text(truncatedValue);
                        cycleSelectList.append(cycleOption);
					}

                }
            });
            cycleSelectList.select2("val", selectedOptions).change();
            gadgets.window.adjustHeight();
            cycleSelectList.on('change', function(ev) {
            	gadgets.window.adjustHeight();
            	if(ev.target && !ev.target.value) {
                    gadget.savePref(cycleUserPref, null);
                    JE.gadget.fields.foldersArray = [];
            	}
            });
        }
    });
};


/**
 * Updates the folderSelect Option dropdown with new values given a cycle selection list.
 * @param showUnscheduled should unscheduled versions be included in the list
 * @param showAutoVersion Should the next due version aka auto version be added to the list
 */
JE.gadget.fields.updateFolderMultiSelectOptions = function(gadget,
                                                     selectedProjectId,
                                                     selectedVersionId,
                                                     cycleIds,
                                                     folderUserPref)
{
	cycleIds = (cycleIds != undefined || cycleIds != null) ? cycleIds.join(',') : "";
    var folderSelectList = AJS.$("#" + folderUserPref);
    folderSelectList.empty();
    JE.gadget.clearError();
    var cycleDetails = AJS.$.ajax({
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/folder",
        type : "GET",
        contentType: "application/json",
        global: false,
        data: {
            versionId : selectedVersionId,
            projectId : selectedProjectId,
            cycleIds: cycleIds
        },
        success: function (response){
        	folderDetails  = response;
            var folderUserPrefValue = gadget.getPref(folderUserPref);
            var selectedOptions = folderUserPrefValue.split("|");
            //AJS.$("#folder_multi_picker_folders .select2-search-choice").each(function(index, elem) { selectedFolders.push(AJS.$(elem).text().trim())});
            // add all available folders
            AJS.$.each(folderDetails, function(key, folder) {
                    var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(folder.folderName);
                    var folderOption = AJS.$("<option style='padding-left:2px;'/>").attr("value", folder.folderId).attr("title",folder.folderName).text(truncatedValue);
                    folderSelectList.append(folderOption);
            });
            if(JE.gadget.fields.foldersArray && JE.gadget.fields.foldersArray.length) {
            	selectedOptions = JE.gadget.fields.foldersArray;
            }
            folderSelectList.select2("val", selectedOptions).change();
            gadgets.window.adjustHeight();
            folderSelectList.on('change', function(ev) {
                gadgets.window.adjustHeight();
            	if(ev.target.value !== "") {
                    JE.gadget.fields.foldersArray = folderSelectList.val();
                }
            	if(ev.target && !ev.target.value) {
                    gadget.savePref(folderUserPref, null);
                    JE.gadget.fields.foldersArray = [];
            	}
            });
        }
    });
};

/**
 * Updates the cycleSelect Option dropdown with new values given a version selection list.
 * @param showUnscheduled should unscheduled versions be included in the list
 * @param showAutoVersion Should the next due version aka auto version be added to the list
 */
JE.gadget.fields.updateCycleBySprintSelectOptions = function(gadget,
														selectedProjectId,
														selectedVersionId,
														sprintUserPref,
														selectedSprintId,
														cycleUserPref)
{
	var cycleSelectList = AJS.$("#" + cycleUserPref);
	cycleSelectList.empty();
	JE.gadget.clearError();
	var cycleDetails = AJS.$.ajax({
		url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/cycle",
		type : "GET",
		contentType: "application/json",
		global: false,
		data: {
			versionId : selectedVersionId,
			projectId : selectedProjectId
		},
		dataType:'json',
		success: function (response){
			JE.gadget.fields.cycleDetails = response;
			var cycleUserPrefValue = gadget.getPref(cycleUserPref);
			// Get cycles filtered by sprint
			var cycleDetails = JE.gadget.fields.filterCyclesBySprint(selectedSprintId);
			// Attach Sprints
			JE.gadget.fields.updateSprintSelectOptions(gadget, selectedProjectId, selectedVersionId, sprintUserPref, selectedSprintId, cycleUserPref);
			JE.gadget.fields.attachCycleOptions(cycleSelectList, cycleUserPrefValue, cycleDetails);
		}
	});
};

JE.gadget.fields.updateFilteredCycleSelectOptions = function(sprintUserPrefValue, cycleUserPref) {
	// Get cycles filtered by sprint
	var cycleSelectList = AJS.$("#" + cycleUserPref);
	var cycleDetails = JE.gadget.fields.filterCyclesBySprint(sprintUserPrefValue);
	cycleSelectList.empty();
	JE.gadget.fields.attachCycleOptions(cycleSelectList, -1, cycleDetails);
}

JE.gadget.fields.filterCyclesBySprint = function(sprintUserPrefValue) {
	var _cycleDetails = [];
	var _cycleValues = _.values(JE.gadget.fields.cycleDetails);
	var _sprintIds = _.pluck(_cycleValues, 'sprintId');
	_sprintIds = _.compact(_sprintIds); // Remove falsy values
	JE.gadget.fields.sprintIds = _.uniq(_sprintIds); // Remove duplicates
	var _filterBySprintId = false;
	if(JE.gadget.fields.sprintIds && JE.gadget.fields.sprintIds.length) {
		_filterBySprintId = true;
	}
	/**
	 * Check the sprintIds length
	 * 1. If empty show all the cycles
	 * 2. If having the sprintId, based on the filterId selected filter the cycles.
	 */
	_cycleDetails = _.filter(JE.gadget.fields.cycleDetails, function(cycle, key){
		var _filterClause;
		if(!_filterBySprintId || sprintUserPrefValue == -1) {
			_filterClause = (key != "recordsCount" && key != "offsetCount" && !cycle.sprintId);
		} else {
			_filterClause = (key != "recordsCount" && key != "offsetCount" && cycle.sprintId == sprintUserPrefValue);
		}
		cycle.id = parseInt(key);
		return (_filterClause ? true: false);
	});
	return _cycleDetails;
}

JE.gadget.fields.attachCycleOptions = function(cycleSelectList, cycleUserPrefValue, cycleDetails) {
	//Add 'All Cycles' option
	var cycleOption = AJS.$("<option/>").attr("value", "-2").attr("title", gadget.getMsg('je.gadget.common.cycles.all.label')).text(gadget.getMsg('je.gadget.common.cycles.all.label'));
	cycleSelectList.append(cycleOption);
	//add all available cycles
	AJS.$.each(cycleDetails, function(key, cycle) {
		var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(cycle.name);
		var cycleOption = AJS.$("<option/>").attr("value", cycle.id).attr("title",cycle.name).text(truncatedValue);
		cycleSelectList.append(cycleOption);

		if(cycleUserPrefValue == cycle.id) {
			cycleOption.attr("selected","selected");
		}
	});
}

/**
 * Updates the versionSelect Option dropdown with new values given a project selection list.
 * @param showUnscheduled should unscheduled versions be included in the list
 * @param showAutoVersion Should the next due version aka auto version be added to the list
 */
JE.gadget.fields.updateVersionSelectOptions = function(gadget, selectedProjectId, versionUserPref, showUnscheduled, showAutoVersion)
{
    var versionSelectList = AJS.$("#" + versionUserPref);
    versionSelectList.empty();

    var versions = AJS.$.ajax({
        key: "versions",
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/versionBoard-list",
        type: "GET",
        contentType: "application/json",
        data: {
        	projectId : selectedProjectId,
    		showUnscheduled : showUnscheduled
        },
        success: function (response)
        {
            versions = response;
            if(!versions)
                return;

            var versionUserPrefValue = gadget.getPref(versionUserPref);
            var hasArchiveVersion = false, isArchiveSelected = false;
            var unreleasedVersionLabel = gadget.getMsg('je.gadget.common.version.unreleased.label');
			var headerUnreleasedVersionOption = AJS.$("<optgroup label='" + unreleasedVersionLabel + "' id='unreleased-version'>");
            var unreleasedArchiveVersionOption = '';
            // add all available versions
            AJS.$(versions.unreleasedVersions).each(function() {
            	// if we show an auto version, we do not want to display the "no versions available selection"
            	if (showAutoVersion && this.value == '') {
	            	return;
	            }
	            var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(this.label);
	            var versionOption = AJS.$("<option/>").attr("value", this.value.toString()).attr("title",this.label).text(truncatedValue);
	            if(this.archived) {
	            	hasArchiveVersion = true;
	            	versionOption.attr('class', 'unreleased-archive-option');
	            	if(versionUserPrefValue == this.value) {
	            		isArchiveSelected = true;
		            	versionOption.attr("selected","selected");
		            }
	            	unreleasedArchiveVersionOption += versionOption[0].outerHTML;
	            } else {
	            	headerUnreleasedVersionOption.append(versionOption);
	            	if(versionUserPrefValue == this.value) {
		            	versionOption.attr("selected","selected");
		            }
	            }
            });
            headerUnreleasedVersionOption.append("</optgroup>");

            if(versions.releasedVersions && versions.releasedVersions.length > 0) {
            	// Released versions
				var releasedVersionLabel = gadget.getMsg('je.gadget.common.version.released.label');
                var headerReleasedVersionOption = AJS.$("<optgroup label='"+releasedVersionLabel+"' id='released-version'>");
                var releasedArchiveVersionOption = '';

                AJS.$(versions.releasedVersions).each(function() {
    	            // if we show an auto version, we do not want to display the "no versions available selection"
    	            if (showAutoVersion && this.value == '') {
    	            	return;
    	            }

    	            var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(this.label);
    	            var versionOption = AJS.$("<option/>").attr("value", this.value).attr("title",this.label).text(truncatedValue);
    	            if(this.archived) {
    	            	hasArchiveVersion = true;
    	            	versionOption.attr('class', 'released-archive-option');
    	            	if(versionUserPrefValue == this.value) {
    	            		isArchiveSelected = true;
    		            	versionOption.attr("selected","selected");
    		            }
    	            	releasedArchiveVersionOption += versionOption[0].outerHTML;
    	            } else {
    	            	headerReleasedVersionOption.append(versionOption);
    	            	if(versionUserPrefValue == this.value) {
    		            	versionOption.attr("selected","selected");
    		            }
    	            }
                });
                headerReleasedVersionOption.append("</optgroup>");
            }


            // Append the unreleased version to the version select
            versionSelectList.append(headerUnreleasedVersionOption);
            // Check if the archive version is selected, if true by default show the archive version.
            if(isArchiveSelected) {
            	versionSelectList.find('#unreleased-version').append(unreleasedArchiveVersionOption);
            	AJS.$('#toggle-archive-version').attr('class', 'archive-label-hide').html(gadget.getMsg("je.gadget.common.version.archivedVersions.hide.label"));
            }
            AJS.$('#unreleased-archive-version-wrapper').html(unreleasedArchiveVersionOption);
            orderSelect(AJS.$('optgroup#unreleased-version'));

            if(versions.releasedVersions && versions.releasedVersions.length > 0) {
	            // Append the released version to the version select
	            versionSelectList.append(headerReleasedVersionOption);
	            // Check if the archive version is selected, if true by default show the archive version.
	            // This condition is checked twice to keep the order in which the released and unreleased elements are inserted.
	            if(isArchiveSelected) {
	            	versionSelectList.find('#released-version').append(releasedArchiveVersionOption);
	            }
	            AJS.$('#released-archive-version-wrapper').html(releasedArchiveVersionOption);
        		orderSelect(AJS.$('optgroup#released-version'));
            }
            // Hide 'Show archived versions' if no archived Versions
            if(hasArchiveVersion) {
            	AJS.$('#toggle-archive-version').show();
            } else AJS.$('#toggle-archive-version').hide();
        }
    });
};

/**
 * Updates the selectedProjectId Option dropdown with new values given a project selection list.
 * @param selectedVersionId Option dropdown with new values given a version selection list.
 * @param sprintUserPref Option dropdown with new values given a sprint selection list.
 * @param cycleUserPref Option dropdown with new values given a cycle selection list.
 */
JE.gadget.fields.updateSprintSelectOptions = function(gadget,
														selectedProjectId,
														selectedVersionId,
														sprintUserPref, selectedSprintId, cycleUserPref)
{
	var sprintSelectList = AJS.$("#" + sprintUserPref);
	sprintSelectList.empty();
	JE.gadget.fields.sprintCount = 0;

	if(JE.gadget.fields.sprintIds && JE.gadget.fields.sprintIds.length) {
		JE.gadget.fields.getSprintDetailsFromSprintIds(JE.gadget.fields.sprintIds, function() {
			sprintSelectList.empty();
			JE.gadget.fields.attachSprintOption(gadget, selectedProjectId, selectedVersionId, sprintSelectList, selectedSprintId, cycleUserPref);
		});
	} else {
		var sprintOption = AJS.$("<option/>").attr('value', -1).text("-");
		sprintSelectList.append(sprintOption);
	}
};

JE.gadget.fields.attachSprintOption = function(gadget, selectedProjectId, selectedVersionId, sprintSelectList, sprintUserPrefValue, cycleUserPref) {
	JE.gadget.fields.sprints = _.sortBy(JE.gadget.fields.sprints, function(sprint){ sprint = sprint || ""; return sprint.name.toLowerCase(); }); // Sort sprints by name
	/**
	 * Attach 'No Sprint' option
	 */
	var noSprintOption = AJS.$("<option/>").attr('value', -1).text("-");
	if(sprintUserPrefValue == -1) {
		noSprintOption.attr("selected","selected");
	}
	sprintSelectList.append(noSprintOption);
	// add all available sprints
	AJS.$.each(JE.gadget.fields.sprints, function(key, sprint) {
		var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(sprint.name);
		var sprintOption = AJS.$("<option/>").attr("value", sprint.id).attr("title",sprint.name).text(truncatedValue);
		sprintSelectList.append(sprintOption);

		if(sprintUserPrefValue == sprint.id) {
			sprintOption.attr("selected","selected");
		}
	});
	var selectedSprint = sprintSelectList.val() ? sprintSelectList.val(): sprintUserPrefValue;
}

JE.gadget.fields.getSprintDetailsFromSprintIds = function(sprintIds, callBack) {
	JE.gadget.fields.sprints = [];
	if(JE.gadget.fields.sprintCount != 0)
		return;
	else
		JE.gadget.fields.sprintCount = 1;

	for(var i=0; i <sprintIds.length; i++) {
		JE.gadget.fields.getSprintDetailsFromId(sprintIds[i], sprintIds.length, callBack);
	}
}
JE.gadget.fields.getSprintDetailsFromId = function(sprintId, sprintKeyLength, callBack) {
	AJS.$.ajax({
        url: "/rest/agile/1.0/sprint/" + sprintId,
        contentType: "application/json",
        success: function (sprint) {
        	JE.gadget.fields.sprints.push(sprint);
             if(JE.gadget.fields.sprintCount == sprintKeyLength) {
             	JE.gadget.fields.sprintCount = 0;
             	callBack();
             } else
             	JE.gadget.fields.sprintCount++;
        }
    });
}
/**
 * GreenHopper project picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param projectOptions the result of the ajax call that provides the available projects
 */
JE.gadget.fields.projectPicker = function(gadget, projectUserPref, projectOptions)
{
	return {
        userpref: projectUserPref,
        label: gadget.getMsg("je.gadget.common.project.label"),
        id: "project_picker_" + projectUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
    		// add a project options box and description. Note that the label has already been added
            parentDiv.append(
                AJS.$("<select/>").attr({
                    id: projectUserPref,
                    name: projectUserPref
                }).addClass('select')
            );
//            parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg("gh.gadget.project.description")));

            // now fill the project list with values
            var $projectSelectList = AJS.$("#" + projectUserPref);
            $projectSelectList.empty();

            //append the options to the projectPicker, selecting the currently selected project
            var projectUserPrefValue = gadget.getPref(projectUserPref);
            AJS.$(projectOptions).each(function()
            {
            	var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(this.label);
                var projectOption = AJS.$("<option/>").attr("value", this.value).attr('data-type', this.type)
                	.attr('data-has-access-to-software', this.hasAccessToSoftware)
                	.attr("title",this.label).text(truncatedValue);
                $projectSelectList.append(projectOption);
                if (this.value == projectUserPrefValue)
                {
                    projectOption.attr({selected: "selected"});
                    selectedProjectId = this.value;
                }
            });
        }
    };
}

JE.gadget.fields.statusesPicker = function(gadget, projectUserPref, statusUserPref)
{
	return {
        userpref: statusUserPref,
        label: gadget.getMsg("je.gadget.top.defects.testStatus.label"),
        description: gadget.getMsg("je.gadget.top.defects.testStatus.desc"),
        id: "status_picker_" + statusUserPref,
        selected: gadget.getPref(statusUserPref),
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
        	var multiSelectDropdown =  AJS.$("<select multiple='multiple'/>").attr({
        										id: statusUserPref,
								                name: statusUserPref
								            }).addClass('multi-select').css("display", "none");

        	parentDiv.append(multiSelectDropdown);

            // initialize the dropdown
            var $projectSelectList = AJS.$("#" + projectUserPref);
            JE.gadget.fields.updateStatusesOptions(gadget, $projectSelectList.val(), statusUserPref);

            multiSelectDropdown.show(10);

            // get informed of changes
            $projectSelectList.change(function(event)
            {
            	JE.gadget.fields.updateStatusesOptions(gadget, $projectSelectList.val(), statusUserPref);
            });
        }
    };
}

var orderSelect = function(versionSelect) {
	var listitems = versionSelect.children('option').get();
	 listitems.sort(function(a, b) {
	    var optionText1 = AJS.$(a).text().toUpperCase();
	    var optionText2 = AJS.$(b).text().toUpperCase();
	    return (optionText1 < optionText2) ? -1 : (optionText1 > optionText2) ? 1 : 0;
	 })
	 AJS.$.each(listitems, function(idx, itm) { versionSelect.append(itm); });
}

/**
 * Zephyr version picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param versionUserPref the id under which the version is stored
 * @param showUnscheduled whether the unscheduled version should be available
 * @param showAutoVerion whether a "Next Due Version" option should be available
 */
JE.gadget.fields.versionPicker = function(gadget, projectUserPref, versionUserPref, showUnscheduled, showAutoVersion)
{
	return {
        userpref: versionUserPref,
        //label: gadget.getMsg("gh.gadget.common.version"),
        label: gadget.getMsg("je.gadget.common.version.label"),
        id: "version_picker_" + versionUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            parentDiv.append(
        		AJS.$("<select>").attr({
                    id: versionUserPref,
                    name: versionUserPref
                }).addClass('select')
		    );
//            parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg("gh.gadget.version.description")));

            // initialize the dropdown
            var $projectSelectList = AJS.$("#" + projectUserPref);
            parentDiv.append('<div id="unreleased-archive-version-wrapper" style="display: none" />');
            parentDiv.append('<div id="released-archive-version-wrapper" style="display: none" />');
            JE.gadget.fields.updateVersionSelectOptions(gadget, $projectSelectList.val(), versionUserPref, showUnscheduled, showAutoVersion);
            parentDiv.append('<div><a href="javascript:void(0)" class="archive-label-show" id="toggle-archive-version" >' + gadget.getMsg("je.gadget.common.version.archivedVersions.show.label") + '</a></div>')
            /*
             * Toggle between the 'Show archived version' and 'Hide archived version'
             */
            parentDiv.find('#toggle-archive-version').on('click', function(ev) {
            	ev.preventDefault();
            	var archivedVersionLinkLabel = AJS.$(this).attr('class');

            	// Since Safari does not support show/ hide for optgroup, appending the elements from a div
            	if(archivedVersionLinkLabel == 'archive-label-show') {
            		var unreleasedArchiveVersionEl = parentDiv.find('div#unreleased-archive-version-wrapper').html();
            		var releasedArchiveVersionEl = parentDiv.find('div#released-archive-version-wrapper').html();
            		parentDiv.find('optgroup#unreleased-version').append(unreleasedArchiveVersionEl);
            		parentDiv.find('optgroup#released-version').append(releasedArchiveVersionEl);
            		orderSelect(AJS.$('optgroup#unreleased-version'));
            		orderSelect(AJS.$('optgroup#released-version'));
            		AJS.$(this).attr('class', 'archive-label-hide').html(gadget.getMsg("je.gadget.common.version.archivedVersions.hide.label"));
            	} else {
            		parentDiv.find('optgroup#unreleased-version .unreleased-archive-option').remove();
            		parentDiv.find('optgroup#released-version .released-archive-option').remove();
            		orderSelect(AJS.$('optgroup#unreleased-version'));
            		orderSelect(AJS.$('optgroup#released-version'));
            		AJS.$(this).attr('class', 'archive-label-show').html(gadget.getMsg("je.gadget.common.version.archivedVersions.show.label"));
            	}
            });

            // get informed of changes
            $projectSelectList.change(function(event)
            {
            	JE.gadget.fields.updateVersionSelectOptions(gadget, $projectSelectList.val(), versionUserPref, showUnscheduled, showAutoVersion);
            	parentDiv.find('#toggle-archive-version').attr('class', 'archive-label-show').html(gadget.getMsg("je.gadget.common.version.archivedVersions.show.label"));
            });
        }
    };
}


/**
 * Zephyr sprint picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param versionUserPref the id under which the version is stored
 * @param sprintUserPref the id under which the sprint is stored
 * @param cycleUserPref the id under which the cycle is stored
 */
JE.gadget.fields.sprintPicker = function(gadget, projectUserPref, versionUserPref, sprintUserPref, cycleUserPref)
{
	return {
        userpref: sprintUserPref,
        label: gadget.getMsg("je.gadget.common.sprint.label"),
        id: "sprint_picker_" + sprintUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            parentDiv.append(
        		AJS.$("<select>").attr({
                    id: sprintUserPref,
                    name: sprintUserPref
                }).addClass('select')
		    );

            // initialize the dropdown
            var $projectSelectList = AJS.$("#" + projectUserPref);
            var $versionSelectList = AJS.$("#" + versionUserPref);
            var versionUserPrefValue = gadget.getPref(versionUserPref);
        	var versionId = versionUserPrefValue == null ? -1 : versionUserPrefValue;
            $projectSelectList.change(function(event) {
            	var hasAccessToSoftware = $projectSelectList.find(':selected').data('has-access-to-software');
            	if(hasAccessToSoftware) {
            		parentDiv.closest('.field-group').show();
            	} else {
            		parentDiv.closest('.field-group').hide();
            	}
            	gadget.resize();
            });

        	if(!$projectSelectList.find(':selected').data('has-access-to-software')) {
        		parentDiv.closest('.field-group').hide();
        	}
        }
    };
}

/**
 * Zephyr cycle by sprint picker
 *
 * @param projectUserPref the id where the project dropdown is stored
 * @param versionUserPref the id under which the version is stored
 * @param sprintUserPref the id where sprint dropdown is stored
 * @param cycleUserPref the id where cycle dropdown is stored
 */
JE.gadget.fields.cycleBySprintPicker = function(gadget, projectUserPref, versionUserPref, sprintUserPref, cycleUserPref)
{
	return {
        userpref: cycleUserPref,
        label: gadget.getMsg("je.gadget.common.cycle.label"),
        id: "cycle_picker_" + cycleUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            parentDiv.append(
        		AJS.$("<select>").attr({
                    id: cycleUserPref,
                    name: cycleUserPref
                }).addClass('select')
		    );

           var versionUserPrefValue = gadget.getPref(versionUserPref);

            // initialize the dropdown
            var $versionSelectList = AJS.$("#" + versionUserPref);
            var $projectSelectList = AJS.$("#"+ projectUserPref);
            var $sprintSelectList = AJS.$("#"+ sprintUserPref);
            var hasAccessToSoftware = $projectSelectList.find(':selected').data('has-access-to-software');
            var selectedVersionId = versionUserPrefValue == null ? -1 : versionUserPrefValue;
            if(!hasAccessToSoftware) {
            	JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), versionUserPrefValue , cycleUserPref);
            } else {
    			var sprintUserPrefValue = gadget.getPref(sprintUserPref) == null ? -1 : gadget.getPref(sprintUserPref);
            	JE.gadget.fields.updateCycleBySprintSelectOptions(gadget, $projectSelectList.val(), versionUserPrefValue, sprintUserPref, sprintUserPrefValue, cycleUserPref);
            }

            $sprintSelectList.change(function(event) {
            	JE.gadget.fields.updateFilteredCycleSelectOptions(this.value , cycleUserPref);
            });

            // get informed of changes
            $versionSelectList.change(function(event) {
            	var hasAccessToSoftware = $projectSelectList.find(':selected').data('has-access-to-software');
            	if(!hasAccessToSoftware) {
            		JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), $versionSelectList.val(), cycleUserPref);
            	} else {
            		JE.gadget.fields.updateCycleBySprintSelectOptions(gadget, $projectSelectList.val(), $versionSelectList.val(), sprintUserPref, -1, cycleUserPref);
            	}
            });

            $projectSelectList.change(function(event){
            	var hasAccessToSoftware = $projectSelectList.find(':selected').data('has-access-to-software');
            	if(!hasAccessToSoftware) {
            		JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), -1, cycleUserPref);
            	} else {
            		JE.gadget.fields.updateCycleBySprintSelectOptions(gadget, $projectSelectList.val(), -1, sprintUserPref, -1, cycleUserPref);
            	}
            });
        }
    };
}

/**
 * Zephyr cycle picker
 *
 * @param cycleUserPref the id where cycle dropdown is stored
 * @param versionUserPref the id under which the version is stored
 * @param showUnscheduled whether the unscheduled version should be available
 * @param showAutoVerion whether a "Next Due Version" option should be available
 */
JE.gadget.fields.cyclePicker = function(gadget, projectUserPref, versionUserPref, cycleUserPref)
{
	return {
        userpref: cycleUserPref,
        label: gadget.getMsg("je.gadget.common.cycle.label"),
        id: "cycle_picker_" + cycleUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            parentDiv.append(
        		AJS.$("<select>").attr({
                    id: cycleUserPref,
                    name: cycleUserPref
                }).addClass('select')
		    );

            var versionUserPrefValue = gadget.getPref(versionUserPref);

            // initialize the dropdown
            var $versionSelectList = AJS.$("#" + versionUserPref);
            var $projectSelectList = AJS.$("#"+ projectUserPref);

            var selectedVersionId = versionUserPrefValue == null ? -1 : versionUserPrefValue;
            JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), versionUserPrefValue , cycleUserPref);

            // get informed of changes
            $versionSelectList.change(function(event)
            {
                JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), $versionSelectList.val(), cycleUserPref);
            });

            $projectSelectList.change(function(event){
                JE.gadget.fields.updateCycleSelectOptions(gadget, $projectSelectList.val(), -1, cycleUserPref);
            });
        }
    };
}

/**
 * Zephyr cycle picker
 *
 * @param cycleUserPref the id where cycle dropdown is stored
 * @param versionUserPref the id under which the version is stored
 * @param showUnscheduled whether the unscheduled version should be available
 * @param showAutoVerion whether a "Next Due Version" option should be available
 */
JE.gadget.fields.cycleMultiPicker = function(gadget, projectUserPref, versionUserPref, cycleUserPref,skipAdhoc)
{
    return {
        userpref: cycleUserPref,
        label: gadget.getMsg("je.gadget.common.cycle.label"),
        id: "cycle_multi_picker_" + cycleUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            var multiSelectDropdown =  AJS.$("<select multiple=''/>").attr({
                id: cycleUserPref,
                name: cycleUserPref
            }).addClass("multi-select");

            parentDiv.append(multiSelectDropdown);
            AJS.$("#cycles").auiSelect2();
            var versionUserPrefValue = gadget.getPref(versionUserPref);

            // initialize the dropdown
            var $versionSelectList = AJS.$("#" + versionUserPref);
            var $projectSelectList = AJS.$("#"+ projectUserPref);

            var selectedVersionId = versionUserPrefValue == null ? -1 : versionUserPrefValue;
            JE.gadget.fields.updateCycleMultiSelectOptions(gadget, $projectSelectList.val(), versionUserPrefValue , cycleUserPref,skipAdhoc);

            // get informed of changes
            $versionSelectList.change(function(event)
            {
                JE.gadget.fields.updateCycleMultiSelectOptions(gadget, $projectSelectList.val(), $versionSelectList.val(), cycleUserPref,skipAdhoc);
            });

            $projectSelectList.change(function(event){
                JE.gadget.fields.updateCycleMultiSelectOptions(gadget, $projectSelectList.val(), -1, cycleUserPref,skipAdhoc);
            });
        }
    };
}

/**
 * Zephyr folder picker
 *
 * @param cycleUserPref the id where cycle dropdown is stored
 * @param versionUserPref the id under which the version is stored
 * @param showUnscheduled whether the unscheduled version should be available
 * @param showAutoVerion whether a "Next Due Version" option should be available
 */
JE.gadget.fields.folderMultiPicker = function(gadget, projectUserPref, versionUserPref, cycleUserPref, folderUserPref)
{
    return {
        userpref: folderUserPref,
        label: gadget.getMsg("je.gadget.common.folder.label"),
        id: "folder_multi_picker_" + folderUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            var multiSelectDropdown =  AJS.$("<select multiple=''/>").attr({
                id: folderUserPref,
                name: folderUserPref
            }).addClass("multi-select");

            parentDiv.append(multiSelectDropdown);
            AJS.$("#folders").auiSelect2();

            // initialize the dropdown
            var $versionSelectList = AJS.$("#" + versionUserPref);
            var $projectSelectList = AJS.$("#"+ projectUserPref);
            var $cycleSelectList = AJS.$("#" + cycleUserPref);

            $cycleSelectList.on('change', function(ev) {
                JE.gadget.fields.updateFolderMultiSelectOptions(gadget, $projectSelectList.val(), $versionSelectList.val(), $cycleSelectList.val(), folderUserPref);
            });
        }
    };
}

/**
 * GreenHopper project picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param fieldUserPref the id under which the field is stored
 * @param showSytem boolean that determines if system fields are shown or not
 */
JE.gadget.fields.fieldPicker = function(gadget, projectUserPref, fieldUserPref, showSystem)
{
	//Label is hardcoded for burndown and taskboard gadget. Should probably be passed in.
	var label = '';
	if(showSystem){
		label = gadget.getMsg("gh.gadget.task.board.count");
	}else{
		label = gadget.getMsg("gh.gadget.custom.field.burndown.field.label");
	}
	return {
        userpref: fieldUserPref,
        label: label,
        id: "field_picker_" + fieldUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
    		parentDiv.append(
        		AJS.$("<select>").attr({
                    id: fieldUserPref,
                    name: fieldUserPref
                }).addClass('select')
		    );
//            parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg("gh.gadget.custom.field.burndown.field.description")));

            // update the values
            var $projectSelectList = AJS.$("#" + projectUserPref);
            JE.gadget.fields.updateFieldSelectOptions(gadget, $projectSelectList.val(), fieldUserPref, showSystem);

            // get informed of changes
            $projectSelectList.change(function(event)
            {
            	JE.gadget.fields.updateFieldSelectOptions(gadget, $projectSelectList.val(), fieldUserPref, showSystem);
            });
        }
    };
}

/**
 * GreenHopper project picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param contextUserPref the id under which the context is stored
 * @param multipleContexts if true a multiselect field will be displayed, a dropdown if false
 */
JE.gadget.fields.contextPicker = function(gadget, projectUserPref, contextUserPref, multipleContexts)
{
	return {
        userpref: contextUserPref,
        label: gadget.getMsg("gh.gadget.common.context"),
        id: "context_picker_" + contextUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
    		var selectAttrs = {
                id: contextUserPref,
                name: contextUserPref
            };
        	var className = "select";
        	if (multipleContexts) {
                selectAttrs.multiple = "multiple";
                selectAttrs.size = "4";
                className = "multi-select";
        	}
            parentDiv.append(
            	AJS.$("<select>").attr(selectAttrs).addClass(className)
             );
//            parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg("gh.gadget.context.description")));

            // update the values
            var $projectSelectList = AJS.$("#" + projectUserPref);
            JE.gadget.fields.updateContextSelectionOptions(gadget, $projectSelectList.val(), contextUserPref);

            // get informed of changes
            $projectSelectList.change(function(event)
            {
            	JE.gadget.fields.updateContextSelectionOptions(gadget, $projectSelectList.val(), contextUserPref);
            });
        }
    };
}

/**
 * GreenHopper task board mapping picker
 *
 * @param projectUserPref the id under which the project is stored
 * @param tbMappingUserPref the id under which the context is stored
 */
JE.gadget.fields.tbMappingPicker = function(gadget, projectUserPref, tbMappingUserPref)
{
	return {
        userpref: tbMappingUserPref,
        label: gadget.getMsg("gh.gadget.common.tbcolumns"),
        id: "tbMapping_picker_" + tbMappingUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
    		var selectAttrs = {
                id: tbMappingUserPref,
                name: tbMappingUserPref
            };
        	var className = "select";
			selectAttrs.multiple = "multiple";
			selectAttrs.size = "4";
			className = "multi-select";
            parentDiv.append(
            	AJS.$("<select>").attr(selectAttrs).addClass(className)
             );

            // update the values
            var $projectSelectList = AJS.$("#" + projectUserPref);
            JE.gadget.fields.updateTBMappingSelectionOptions(gadget, $projectSelectList.val(), tbMappingUserPref);

            // get informed of changes
            $projectSelectList.change(function(event)
            {
            	JE.gadget.fields.updateTBMappingSelectionOptions(gadget, $projectSelectList.val(), tbMappingUserPref);
            });
        }
    };
}

JE.gadget.fields.daysPreviouslyOptions =  [{value:'30', label:"30"},{value:'45', label:"45"}];
JE.gadget.fields.daysPreviouslyGroupPicker = function(gadget, groupFldUserPref){
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, JE.gadget.fields.daysPreviouslyOptions, "je.gadget.days.previously.dropDown.label", false);
}

JE.gadget.fields.topDefectsCountGroupOptions =  [{value:'5', label:"5"},{value:'10', label:"10"},{value:'15', label:"15"}];
JE.gadget.fields.topDefectsGroupPicker = function(gadget, groupFldUserPref, showAutoVersion){
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, JE.gadget.fields.topDefectsCountGroupOptions, "je.gadget.top.defects.dropDown.label", showAutoVersion);
}

JE.gadget.fields.pastTestProgressGroupOptions =  [{value:'5', label:"5"},{value:'10', label:"10"},{value:'15', label:"15"},{value:'20', label:"20"},{value:'30', label:"30"}];
JE.gadget.fields.pastTestProgressGroupPicker = function(gadget, groupFldUserPref, showAutoVersion){
    return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, JE.gadget.fields.pastTestProgressGroupOptions, "je.gadget.top.defects.dropDown.label", showAutoVersion);
}

JE.gadget.fields.filterMaxResultGroupOptions =  [{value:'5', label:"5"},{value:'10', label:"10"},{value:'15', label:"15"},{value:'20', label:"20"},{value:'25', label:"25"},{value:'30', label:"30"},{value:'35', label:"35"},{value:'40', label:"40"}];
JE.gadget.fields.filterMaxResultPicker = function(gadget, groupFldUserPref, showAutoVersion){
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, JE.gadget.fields.filterMaxResultGroupOptions, "je.gadget.top.defects.dropDown.label", showAutoVersion);
}

JE.gadget.fields.testcaseGroupFldPickerLov = [{value:'user', label: 'je.gadget.common.user.label'},{value:'component', label:'je.gadget.common.component.label'}];
JE.gadget.fields.executionGroupFldPickerLov = [{value:'cycle', label:'je.gadget.common.cycle.label'},{value:'user', label:'je.gadget.common.user.label'},{value:'component', label:'je.gadget.common.component.label'}];
JE.gadget.fields.executionGroupFldWithSprintPickerLov  = [{value:'cycle', label:'je.gadget.common.cycle.label'},{value:'user', label:'je.gadget.common.user.label'},{value:'component', label:'je.gadget.common.component.label'}, {value:'sprint-cycle', label:'je.gadget.common.sprint.cycle.label'}];
JE.gadget.fields.testcaseGroupFldPicker = function(gadget, groupFldUserPref, showAutoVersion){
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, JE.gadget.fields.testcaseGroupFldPickerLov, "je.gadget.common.groupFld.label", showAutoVersion);
}


JE.gadget.fields.zqlSelectionGroupOptions =  [{value:'zqlFreeform', label:"je.gadget.common.no.zql.label"},{value:'zqlFilter', label:"je.gadget.common.zql.filter.label"}];
JE.gadget.fields.zqlFilterPicker = function(gadget, groupFldUserPref, showAutoVersion){
    /* Free form ZQL is not supported in Confluence Gadgets */
    var optionsArr = JE.gadget.fields.zqlSelectionGroupOptions;
    if(window.top.Confluence){
        var optionsArr = [JE.gadget.fields.zqlSelectionGroupOptions[1]];
    }
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, optionsArr, "je.gadget.zqlfilter.selection.label", showAutoVersion);
}


JE.gadget.fields.getPickerLabel = function(gadget, lov, value){
	for(var key in lov){
		var entry = lov[key];
		if(entry.value == value)
			return gadget.getMsg(entry.label);
	}

}

JE.gadget.fields.getExecutionGroupFldPickerLov = function(isProjectTypeSoftware) {
	if(isProjectTypeSoftware) {
		return JE.gadget.fields.executionGroupFldWithSprintPickerLov;
	}
	return JE.gadget.fields.executionGroupFldPickerLov;
}

JE.gadget.fields.executionGroupFldPicker = function(gadget, projectUserPref, groupFldUserPref, showAutoVersion, projectOptions){
	var projectUserPrefValue = gadget.getPref(projectUserPref),
		isProjectTypeSoftware = false;

	if(projectUserPrefValue) {
		var selectedSoftwareProject = _.filter(projectOptions, function(project){return (project.value == projectUserPrefValue);}).filter(function(project) {return ((project.hasAccessToSoftware == true || project.hasAccessToSoftware == 'true'));});
		if(selectedSoftwareProject && selectedSoftwareProject.length) {
			isProjectTypeSoftware = true
		}
	} else {
		// If no project is selected then, consider the first project in list
		if(projectOptions[0] && (projectOptions[0].hasAccessToSoftware === 'true' || projectOptions[0].hasAccessToSoftware === true)) {
			isProjectTypeSoftware = true;
		}
	}
	var executionGroupFldPickerLov = JE.gadget.fields.getExecutionGroupFldPickerLov(isProjectTypeSoftware);
	return JE.gadget.fields.groupFldPicker(gadget, groupFldUserPref, executionGroupFldPickerLov,"je.gadget.common.groupFld.label", showAutoVersion, true, projectUserPref);
}

JE.gadget.fields.attachGroupFldPickerOptions = function(gadget, groupFldUserPref, groupByOptions) {
    // now fill the Group Field list with values
    var $groupFldSelectList = AJS.$("#" + groupFldUserPref);
    $groupFldSelectList.empty();
	// initialize the dropdown
    //append the options to the groupFldPicker, selecting the currently selected groupFld
    var groupFldUserPrefValue = gadget.getPref(groupFldUserPref);
    AJS.$(groupByOptions).each(function()
    {
        var groupFldOption = AJS.$("<option/>").attr("value", this.value).text(gadget.getMsg(this.label));
        $groupFldSelectList.append(groupFldOption);
        if (this.value == groupFldUserPrefValue)
        {
        	groupFldOption.attr({selected: "selected"});
        }
    });
}

JE.gadget.fields.groupFldPicker = function(gadget, groupFldUserPref, groupByOptions, dropdownLabel, showAutoVersion, handleProjectChange, projectUserPref){
	return {
        userpref: groupFldUserPref,
        //label: gadget.getMsg("gh.gadget.common.version"),
        label: gadget.getMsg(dropdownLabel),
        id: "groupFld_picker_" + groupFldUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // groupFld box label, select box and description
            parentDiv.append(
        		AJS.$("<select>").attr({
                    id: groupFldUserPref,
                    name: groupFldUserPref
                }).addClass('select')
		    );

            JE.gadget.fields.attachGroupFldPickerOptions(gadget, groupFldUserPref, groupByOptions);

            if(handleProjectChange) {
	            var $projectSelectList = AJS.$("#" + projectUserPref);
	            // get informed of changes
	            $projectSelectList.change(function(event) {
	            	var hasAccessToSoftware = $projectSelectList.find(':selected').data('has-access-to-software'),
	            		isProjectTypeSoftware = false;
	            	if(hasAccessToSoftware) {
	            		isProjectTypeSoftware = true;
	            	}
	            	var executionGroupFldPickerLov = JE.gadget.fields.getExecutionGroupFldPickerLov(isProjectTypeSoftware);
	            	JE.gadget.fields.attachGroupFldPickerOptions(gadget, groupFldUserPref, executionGroupFldPickerLov);
	            });
            }
        }
    };
}


JE.gadget.fields.zqlSearchBox=function(gadget, textFieldId, maxChar,autocompleteJSON,currZqlQuery) {
	return {
		userpref: textFieldId,
        label: gadget.getMsg("je.gadget.zql.search.label"),
        id: "textfield_" + textFieldId,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
        	// Remove the hidden zqlText element attached for sorting
        	AJS.$('input:hidden#' + textFieldId).remove();
        	parentDiv.addClass("navigator-search");
        	if(!gadget.getPref("currZqlQuery") && gadget.getPref("zfjQueryType") && AJS.$('#zfjQueryType').val() != 'zqlFreeform') {
        		parentDiv.parent().css("display","none");
        	}

			var maximumChars = maxChar || 255;
			var lbl = gadget.getMsg("je.gadget.zql.search.label");
			var html = ZEPHYR.ZQL.Search.searchZQL({zqlFieldz:autocompleteJSON.jqlFieldZ,zqlReservedWordz:autocompleteJSON.reservedWords,zqlQuery:currZqlQuery});
			parentDiv.append(AJS.$(html));
			jQuery(document).trigger("refreshAutocomplete");
            /* Trigging refresh in the ZFJQueryType, to refresh visibility of ZQL/Fitler textField. For BluePrint only one option is visible and this refresh is must for that*/
            AJS.$("#zfjQueryType").trigger("change");
        }
    };
}

/**
 * Truncates Label which is greater than 50 characters and appends ellipsis style ... at the end. Ellipsis is not supported on select elements
 */
JE.gadget.utils.getTruncatedTextIfLengthGreater = function(label) {
	var maximumChar=50;
	var truncatedValue = label;
	if(label.length > maximumChar) {
		truncatedValue = label.substring(0, maximumChar) + "...";
	}
	return truncatedValue;
};

/**
 * Set the gadget title
 */
JE.gadget.utils.setGadgetTitle = function(gadgetId, gadgetTitle) {
	var parentEl = window.parent.document;
	if(AJS.$(parentEl).find('#' + gadgetId + '-title').text() != gadgetTitle)
		AJS.$(parentEl).find('#' + gadgetId + '-title').html(gadgetTitle);
};


JE.gadget.fields.filterPicker = function(gadget, userpref){
    if (!gadget.filterId){
        gadget.filterId = gadget.getMsg("gadget.common.filterid.none.selected");
    }

    return {
        userpref: userpref,
        label: gadget.getMsg("je.gadget.zql.filter.label"),
        description:gadget.getMsg("gadget.common.filterid.description"),
        id: "zqlfilter_picker_" + userpref,
        type: "callbackBuilder",
        callback: function(parentDiv){
        	var _filterName = JE.gadget.utils.htmlDecode(gadget.getPref('filterName'));
            parentDiv.append(
                AJS.$("<input/>").attr({
                    id: "filter_" + userpref + "_id",
                    type: "hidden",
                    name: userpref
                }).val(gadget.getPref(userpref))
            ).append(
                    AJS.$("<input/>").attr({
                        id: "filterName",
                        type: "hidden",
                        name: "filterName"
                    }).val(_filterName)
            ).append(
                AJS.$("<span/>").attr({id:"filter_" + userpref + "_name"}).addClass("filterpicker-value-name field-value").text(_filterName || gadget.getMsg('je.gadget.zql.none.filter.label'))
            );
            parentDiv.append(
                AJS.$("<div/>").attr("id", "quickfind-container").append(
                    AJS.$("<label/>").addClass("overlabel").attr({
                        "for":"quickfind",
                        id: "quickfind-label"
                    }).text(gadget.getMsg("gadget.common.quick.find"))
                ).append(
                    AJS.$("<input/>").attr("id", "quickfind").attr("type", "text").addClass("text")
                ).append(
                    AJS.$("<span/>").addClass("inline-error")
                )
            );

            AJS.gadget.fields.applyOverLabel("quickfind-label");
            JE.gadget.fields.ZQLFilters({
            	fieldID: "quickfind",
                ajaxData: {},
                baseUrl: jQuery.ajaxSettings.baseUrl,
                relatedId: "filter_" + userpref + "_id",
                relatedDisplayId: "filter_" + userpref + "_name",
                relatedFilterName:"filterName",
                gadget: gadget,
                filtersLabel: gadget.getMsg("gadget.common.filters"),
                projectsLabel: gadget.getMsg("gadget.common.projects")
            });

            //Disable the Filter if ZQL was selected as zfjQueryType
        	if(!gadget.getPref("filterId") && gadget.getPref("zfjQueryType") != 'zqlFilter') {
        		parentDiv.parent().css("display","none");
        	}
        }
    };
};

var gadget = this.gadgets.Prefs();
// Column selector group options. The columnName and the columnClass fields are for the template header
JE.gadget.fields.columnSelectorGroupOptions =  [{"label":gadget.getMsg('enav.newcycle.name.label'),"value":"cyclename", "columnName": "cycleName", "columnClass": "cycle"},
                                                {"label":gadget.getMsg('cycle.reorder.executions.issue.label'),"value": "issuekey", "columnName":  "Issue", "columnClass":  "issue"},
                                                {"label":gadget.getMsg('execute.test.testsummary.label'),"value":"testsummary", "columnName":  "summary", "columnClass":  "test-summary"},
												{"label":gadget.getMsg('je.gadget.zql.labels.label'),"value":"labels", "columnName":  "labels", "columnClass":  "labels"},
                                                {"label":gadget.getMsg('enav.projectname.label'),"value":"projectname", "columnName":  "project", "columnClass":  "project"},
                                                {"label":gadget.getMsg('project.cycle.addTests.priority.label'),"value":"priority", "columnName":  "priority", "columnClass":  "priority"},
                                                {"label":gadget.getMsg('je.gadget.common.component.label'),"value":"component", "columnName":  "component", "columnClass":  "component"},
                                                {"label":gadget.getMsg('je.gadget.common.version.label'),"value":"version", "columnName":  "fixVersion", "columnClass":  "fixVersion"},
    											{"label":gadget.getMsg('execute.test.executionstatus.label'),"value":"executionstatus", "columnName":  "ExecutionStatus", "columnClass":  "status"},
    											{"label":gadget.getMsg('project.cycle.schedule.table.column.executedBy'),"value":"executedby", "columnName":  "ExecutedBy", "columnClass":  "executedby"},
    											{"label":gadget.getMsg('project.cycle.schedule.table.column.executedOn'),"value":"executedon", "columnName":  "ExecutionDate", "columnClass":  "executedOn"},
    											{"label":gadget.getMsg('plugin.license.storage.admin.license.attribute.creationdate.title'),"value":"creationdate", "columnName":  "creationDate", "columnClass":  "creationdate"},
    											{"label":gadget.getMsg('enav.assigned.to.searcher.name'), "value": "assignedTo", "columnName": 'assignee', "columnClass": 'assignee'},
    											{"label":gadget.getMsg('enav.search.execution.defects'), "value": "executiondefects", "columnName": 'executionDefectKey', "columnClass": 'executionDefectKey'},
    											{"label":gadget.getMsg('enav.newfolder.name.label'), "value": "foldername", "columnName": 'folderName', "columnClass": 'folderName'}];

JE.gadget.fields.columnSelector = function(gadget, groupFldUserPref) {
	gadget.sortByTriggered = null;
	return {
        userpref: groupFldUserPref,
        label: gadget.getMsg("je.gadget.zql.columns.label"),
        id: "groupFld_picker_" + groupFldUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv) {
        	parentDiv.append(
        		AJS.$('<div>')
        		.html(
        			'<table id="column-picker-restful-table" class="aui-restfultable aui aui-restfultable-allowhover"><tbody class="ui-sortable"></tbody></table>'
        		)
        	);

        	parentDiv.append(
        		AJS.$('<input>')
        		.attr({
        			type: 	'hidden',
        			id:		groupFldUserPref,
        			name: 	groupFldUserPref
        		})
        		.val(gadgets.util.unescapeString(gadget.getPref('columnNames')))
        	);

        	var columnNames = gadgets.util.unescapeString(gadget.getPref('columnNames')).split('|');
        	parentDiv.append(
        		AJS.$('<div>')
        		.html(gadget.getMsg("je.gadget.zql.columns.dndDescription") + ". <a role='button' href='#' id='columns_restore_default'>"+gadget.getMsg('je.gadget.zql.columns.restoreDefault.label')+"</a>")
        		.attr({
        			id:		groupFldUserPref + '-dnd-helpText'
        		}).addClass('description')
		    ).append('<p>');

        	// groupFld box label, select box and description
        	parentDiv.append(
        		AJS.$("<div>")
        		.addClass(groupFldUserPref + '-select-wrapper')
        		.append(AJS.$("<select>").attr({
                    id: 	groupFldUserPref + '-select',
                    name: 	groupFldUserPref + '-select'
                }).addClass('select'))
                .append(AJS.$("<input>").attr({
                	type: 	'button',
                	value: 	'Add',
                	id: 	groupFldUserPref + '-add'
                }).addClass('button'))
                .append(AJS.$('<div>')
        		.html(gadget.getMsg("je.gadget.zql.columns.addDescription") + '.')
        		.attr({
        			id:		groupFldUserPref + '-select-helpText'
        		}).addClass('description'))
        		.hide()
        	);

            // now fill the Group Field list with values
            var $groupFldSelectList = AJS.$("#" + groupFldUserPref + '-select');
            $groupFldSelectList.empty();

            // initialize the dropdown
            // append the options to the groupFldPicker, selecting the currently selected groupFld
            $groupFldSelectList.append(AJS.$("<option/>").attr("value", "-1").text("Select a field..."));
            JE.gadget.fields.displayColumns(gadget, columnNames, parentDiv, $groupFldSelectList, true);

            // Jquery UI sortable
            AJS.$('#column-picker-restful-table tbody').sortable({
  		      	placeholder: "ui-state-highlight",
  		      	dropOnEmpty: true,
  		      	axis: 		 'y',
  		      	handle: 	 ".aui-restfultable-draghandle",
  		      	stop: function(ev, ui) {
  		      		JE.gadget.fields.updateColumns();
  		      	}
            });
            AJS.$('#column-picker-restful-table tbody').disableSelection();
        }
    };

};

JE.gadget.fields.displayColumns = function(gadget, columnNames, parentDiv, $groupFldSelectList, isResize) {
	var xhr = {
			url: zqlGadgetBaseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+ '/latest/customfield/entity?entityType=EXECUTION',
			method: 'GET',
	};
	var customFields = [];
	customXhrCall(xhr, function (response) {
		var selectedColumns = JSON.parse(JSON.stringify(JE.gadget.fields.columnSelectorGroupOptions));
		AJS.$(response).each(function(customField) {
			if(response[customField].active) {
				selectedColumns.push({columnName: response[customField].name, label: response[customField].name, value: response[customField].name, filterIdentifier: response[customField].name, columnClass: response[customField].id, isCustomField: true});
			}
		});
		AJS.$(parentDiv).find('#column-picker-restful-table tbody').html('');
		AJS.$(columnNames).each(function(i, columnItem) {
			selectedColumns.map(function(column) {
				if(column.value == columnItem){
					AJS.$(parentDiv).find('#column-picker-restful-table tbody').append(
	        			'<tr class="aui-restfultable-readonly aui-restfultable-row" data-value="' + column.value + '" data-label="' + column.label + '">' +
	        				'<td class="aui-restfultable-order"><span class="aui-restfultable-draghandle"></span></td>' +
	        				'<td class="column-label" data-label="' + column.value + '">' + column.label + '</td>' +
	        				'<td class="aui-restfultable-operations"><a title="Delete this field" id="column-picker-delete-issuekey" class="aui-restfultable-delete icon-delete icon" target="_parent"></a></td>' +
	        			'</tr>'
	        		);
				}
			});
		});

		AJS.$(selectedColumns).each(function() {
	    	if(AJS.$.inArray(this.value, columnNames) == -1) {
	            var groupFldOption = AJS.$("<option/>").attr("value", this.value).text(this.label);
	            $groupFldSelectList.append(groupFldOption);
	            AJS.$('.columnNames-select-wrapper').show();
	        }
	    });
		isResize &&	gadget.resize();
	}, function (response) {
			console.log('fail response : ', response);
	});
};

JE.gadget.fields.updateColumns = function() {
	var columns = '';
	AJS.$(AJS.$('td.column-label')).each(function(i, column) {
		columns = columns + AJS.$(column).attr('data-label');
		if((i+1) != AJS.$('td.column-label').length) columns = columns + '|';
	});
	AJS.$('#columnNames').val(columns);
};

JE.gadget.fields.ZQLFilters = function(options) {
    // prototypial inheritance (http://javascript.crockford.com/prototypal.html)
    var that = begetObject(jira.widget.autocomplete.REST);

    that.getAjaxParams = function(){
        return {
            url: options.baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/zql/executionFilter/quickSearch",
            data: {
                query: AJS.$("#quickfind").val()
            },
    		type : "GET",
    		contentType :"application/json",
    		dataType: "json",
            global:false,
            error: function(XMLHttpRequest, textStatus, errorThrown){
                if (XMLHttpRequest.data){
                    var errorCollection = XMLHttpRequest.data.errors;
                    if (errorCollection){
                        AJS.$(errorCollection).each(function(){
                            var parent = AJS.$("#" + this.field).parent();
                            parent.find("span.inline-error").text(options.gadget.getMsg(this.error));
                        });
                    }
                }
            }
        };
    };

    that.completeField = function(value) {
        AJS.$("#" + options.relatedId).val(value.id);
        AJS.$("#" + options.relatedDisplayId).addClass("success").text(value.filterName);
        AJS.$("#" + options.relatedFilterName).val(value.filterName);
        AJS.$("#" + options.fieldID).val("");
    	AJS.$('#zqltext').val(value.query);
    	AJS.$('#zqltext').text(value.query);
    };

    /**
     * Create html elements from JSON object
     * @method renderSuggestions
     * @param {Object} response - JSON object
     * @returns {Array} Multidimensional array, one column being the html element and the other being its
     * corressponding complete value.
     */
    that.renderSuggestions = function(response) {

        var resultsContainer, suggestionNodes = [];
        this.responseContainer.addClass("aui-list");

        // remove previous results
        this.clearResponseContainer();

        var parent = AJS.$("#" + options.fieldID).parent();
        parent.find("span.inline-error").text("");

        if (response && response.length > 0) {

            if (resultsContainer) {
                resultsContainer.removeClass("aui-last");
            }

            AJS.$("<h5/>").text(options.filtersLabel).appendTo(this.responseContainer);
            resultsContainer = AJS.$("<ul class='aui-list-section aui-first aui-last' />").appendTo(this.responseContainer);

            jQuery(response).each(function() {
                var item = jQuery("<li class='aui-list-item'/>").attr(
                    {
                        id: this.id +"_" + options.fieldID + "_listitem"
                    }
                );


                var link = AJS.$("<a href='#' class='aui-list-item-link' />").append(
                    AJS.$("<span/>").addClass("filter-name").html(htmlEncode(this.filterName))
                )
                .click(function (e) {
                    e.preventDefault();
                })
                .appendTo(item);
                if (this.query){
                    link.append(
                        AJS.$("<span/>").addClass("filter-desc").html(" - " + htmlEncode(this.query))
                    );
                }

                item.attr("title", link.text());

                // add html element and corresponding complete value  to sugestionNodes Array
                suggestionNodes.push([item.appendTo(resultsContainer), this]);

            });
        }

        if (suggestionNodes.length > 0) {
            this.responseContainer.removeClass("no-results");
            that.addSuggestionControls(suggestionNodes);
        } else {
            this.responseContainer.addClass("no-results");
        }
        return suggestionNodes;
    };
    options.maxHeight = 200;
    // Use autocomplete only once the field has atleast 2 characters
    options.minQueryLength = 1;
    // wait 1/4 of after someone starts typing before going to server
    options.queryDelay = 0.25;
    that.init(options);
    return that;
};

/**
 * Extracts error message from XHR and displays it in gadget
 */
JE.gadget.zephyrError = function(xhr, gadget){
	var ctx = AJS.$("#ag-sys-msg");
	if(ctx.length) {
		AJS.$("<div id='ag-sys-msg' />").appendTo("body");
	}
	ctx.css({display: "block"});
	JE.gadget.clearError(ctx)
	var errMsg = xhr.responseText;

	try {
		if(xhr.status == 403) {
			AJS.$('.aui-blanket').hide();
			AJS.$('#dialog-error').remove();
			AJS.$('#zqlExecutionWrapper').remove();
			var errMsg = gadget.getMsg("je.gadget.common.login.error");
		} else {
			var jsonResponse = jQuery.parseJSON(xhr.responseText);
			errMsg = jsonResponse.errorDescHtml || jsonResponse.error;
			if(!errMsg) {
				if(jsonResponse instanceof Object) {
					errMsg = jsonResponse.warning;
				} else {
					errMsg = jsonResponse;
				}
			}
		}
	}catch(err){}
	AJS.messages.error(ctx, {
	    body: errMsg,
	    closeable: true
	});
};

/**
 * Display the warning message in the gadget
 */
JE.gadget.zephyrWarning = function(ctx, message) {
	var ctx = ctx || AJS.$("#ag-sys-msg");
	JE.gadget.clearError(ctx);
	AJS.messages.warning(ctx, {
	    body: message,
	    closeable: false
	});
};

JE.gadget.clearError = function(ctx){
    ctx = ctx || AJS.$("#ag-sys-msg");
    ctx.empty();
}

JE.gadget.fields.chartTypePicker = function(gadget, chartTypeUserPref, gadgetType)
{
	if(gadgetType && gadgetType == "pastExecutionStatus"){

		var chartTypeOptions = [
	        {label: 'Stacked Chart', value: 'stack'},
	        {label: 'Tabular Chart', value: 'table'},
	    ];
	}else if(gadgetType && gadgetType == "cycleExecutionTimeName") {
		var chartTypeOptions = [
	        {label: 'Grouped Bar Chart', value: 'bar'},
	        {label: 'Tabular Chart', value: 'table'},
	    ];
	}else {
		var chartTypeOptions = [
	        {label: 'Pie Chart', value: 'pie'},
	        {label: 'Tabular Chart', value: 'table'},
	    ];
	}
	return {
        userpref: chartTypeUserPref,
        label: gadget.getMsg("je.gadget.common.chartType.label"),
        id: "chartType_picker_" + chartTypeUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
    		// add a chartType options box and description. Note that the label has already been added
            parentDiv.append(
                AJS.$("<select/>").attr({
                    id: chartTypeUserPref,
                    name: chartTypeUserPref
                }).addClass('select')
            );
//            parentDiv.append(AJS.$("<div/>").addClass("description").text(gadget.getMsg("gh.gadget.chartType.description")));

            // now fill the chartType list with values
            var $chartTypeSelectList = AJS.$("#" + chartTypeUserPref);
            $chartTypeSelectList.empty();

            //append the options to the chartTypePicker, selecting the currently selected chartType
            var chartTypeUserPrefValue = gadget.getPref(chartTypeUserPref);
            AJS.$(chartTypeOptions).each(function()
            {
            	var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(this.label);
                var chartTypeOption = AJS.$("<option/>").attr("value", this.value).attr('data-type', this.type)
                	.attr('data-has-access-to-software', this.hasAccessToSoftware)
                	.attr("title",this.label).text(truncatedValue);
                $chartTypeSelectList.append(chartTypeOption);
                if (this.value == chartTypeUserPrefValue)
                {
                    chartTypeOption.attr({selected: "selected"});
                }
            });
        }
    };
}

/**
 *
 * @param gadget
 * @param projectUserPref
 * @param componentUserPref
 * @returns {{userpref: *, label, id: string, type: string, callback: callback}}
 */
JE.gadget.fields.componentPicker = function(gadget, projectUserPref, componentUserPref)
{
    return {
        userpref: componentUserPref,
        label: gadget.getMsg("je.gadget.common.component.label"),
        id: "component_picker_" + componentUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            // version box label, select box and description
            parentDiv.append(
                AJS.$("<select>").attr({
                    id: componentUserPref,
                    name: componentUserPref
                }).addClass('select')
            );

            // initialize the dropdown
            var $projectSelectList = AJS.$("#"+ projectUserPref);

            JE.gadget.fields.updateComponentSelectOptions(gadget, $projectSelectList.val(), componentUserPref);

            $projectSelectList.change(function(event){
                JE.gadget.fields.updateComponentSelectOptions(gadget, $projectSelectList.val(), componentUserPref);
            });
        }
    };
}

/**
 *
 * @param gadget
 * @param projectUserPref
 * @param componentUserPref
 * @returns {{userpref: *, label, id: string, type: string, callback: callback}}
 */
JE.gadget.fields.componentMultiPicker = function(gadget, projectUserPref, componentUserPref)
{
    return {
        userpref: componentUserPref,
        label: gadget.getMsg("je.gadget.common.component.label"),
        id: "component_multi_picker_" + componentUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {
            var multiSelectDropdown =  AJS.$("<select multiple=''/>").attr({
                id: componentUserPref,
                name: componentUserPref
            }).addClass("multi-select");

            parentDiv.append(multiSelectDropdown);
            AJS.$("#components").auiSelect2();

            // initialize the dropdown
            var $projectSelectList = AJS.$("#"+ projectUserPref);

            JE.gadget.fields.updateComponentMultiSelectOptions(gadget, $projectSelectList.val(), componentUserPref);

            $projectSelectList.change(function(event){
                JE.gadget.fields.updateComponentMultiSelectOptions(gadget, $projectSelectList.val(), componentUserPref);
            });
        }
    };
}
JE.gadget.fields.datePicker = function(gadget, dateUserPref, label)
{
return {
        userpref: dateUserPref,
        label: gadget.getMsg(label),
        id: dateUserPref,
        type: "callbackBuilder",
        callback: function (parentDiv)
        {

			var lbl = gadget.getMsg(label);
			var datePickerId = dateUserPref;
			var date = new Date();
			var formattedDate = date.getFullYear() + "-" + ("0" + (date.getMonth() + 1)).slice(-2) + "-" + ("0" + date.getDate()).slice(-2);

			var html = '<input class="aui-date-picker" id="'+datePickerId+'" type="date" value="'+gadget.getPref("inputDate")+'" max="'+formattedDate+'" style="max-width: 250px;width:100%" required/>';
			parentDiv.append(AJS.$(html));

			// make sure we update the values that get submitted
			AJS.$('#'+datePickerId, parentDiv).each(function() {
				var textInput = this;
				var $textInput = AJS.$(textInput);
				var hiddenInputSelector = '#'+dateUserPref;
				AJS.$(hiddenInputSelector).attr('value', textInput.value);
				$textInput.change(function() {
					var hiddenInputSelector = '#'+dateUserPref;
					AJS.$(hiddenInputSelector).attr('value', textInput.value);
				});
			});
        }
    };
}


/**
 *
 * @param gadget
 * @param selectedProjectId
 * @param componentUserPref
 */
JE.gadget.fields.updateComponentSelectOptions = function(gadget, selectedProjectId, componentUserPref)
{
    var componentSelectList = AJS.$("#" + componentUserPref);
    componentSelectList.empty();
    JE.gadget.clearError();
    var componentDetails = AJS.$.ajax({
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/component-list",
        type : "GET",
        contentType: "application/json",
        global: false,
        data: {
            projectId : selectedProjectId
        },
        success: function (response){
            componentDetails = response;
            var componentUserPrefValue = gadget.getPref(componentUserPref);
            //var componentOption = AJS.$("<option/>").attr("value", "-2").attr("title", gadget.getMsg('je.gadget.common.cycles.all.label')).text(gadget.getMsg('je.gadget.common.cycles.all.label'));
            var componentOption = AJS.$("<option/>").attr("value", "-1").attr("title", "No Component").text("No Component");
            componentSelectList.append(componentOption);
            // add all available component
            AJS.$.each(componentDetails, function(key, value) {
                var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(value);
                var component = AJS.$("<option/>").attr("value", key).attr("title",value).text(truncatedValue);
                componentSelectList.append(component);

                if(componentUserPrefValue == key) {
                    component.attr("selected","selected");
                }
            });
        }
    });
};


/**
 *
 * @param gadget
 * @param selectedProjectId
 * @param componentUserPref
 */
JE.gadget.fields.updateComponentMultiSelectOptions = function(gadget, selectedProjectId, componentUserPref)
{
    var componentSelectList = AJS.$("#" + componentUserPref);
    componentSelectList.empty();
    JE.gadget.clearError();
    var componentDetails = AJS.$.ajax({
        url: "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/component-list",
        type : "GET",
        contentType: "application/json",
        global: false,
        data: {
            projectId : selectedProjectId
        },
        success: function (response){
            componentDetails = response;
            var componentUserPrefValue = gadget.getPref(componentUserPref);
            var selectedOptions = componentUserPrefValue.split("|");

            // add all available components
            AJS.$.each(componentDetails, function(key, value) {
				var truncatedValue = JE.gadget.utils.getTruncatedTextIfLengthGreater(value);
				var componentOption = AJS.$("<option style='padding-left:2px;'/>").attr("value", key).attr("title",value).text(truncatedValue);
				componentSelectList.append(componentOption);
            });
            componentSelectList.select2("val", selectedOptions).change();
			gadgets.window.adjustHeight();
            componentSelectList.on('change', function(ev) {
            	gadgets.window.adjustHeight();
            	if(ev.target && !ev.target.value) {
            		gadget.savePref(componentUserPref, null);
            	}
            });
        }
    });
};

/**
 * Allow only numbers
 */
AJS.$('input.zephyr_allow_only_number').live('keyup', function (ev) {
	if(AJS.$(ev.target).hasClass('zephyr_numbers_no_decimals')) {
		this.value = this.value.replace(/[^0-9]/g,'');
	} else {
		this.value = this.value.replace(/[^0-9\.]/g,'');
	}
});


AJS.$.ajaxPrefilter(function( options, originalOptions, jqXHR ) {
	   if (/\/zephyr/.test(options.url)) {
		  jqXHR.setRequestHeader(parent.zEncKeyFld, parent.zEncKeyVal);
	   }
	   var error = options.error;
	   options.error = function (jqXHR, textStatus, errorThrown) {
			var errorResponse = jQuery.parseJSON(jqXHR.responseText);
			if (jqXHR.status == 403 && errorResponse.PERM_DENIED) {
				AJS.$(".jira-dialog").hide();
				showPermissionError(jqXHR);
			} else {
				if(error) {
					return error(jqXHR, textStatus, errorThrown);
				} else {
					return options.error;
				}
			}
	   };
});

var showPermissionError = function(jqXHR) {
	var ctx = AJS.$("#ag-sys-msg");
	if(ctx.length) {
		AJS.$("<div id='ag-sys-msg' />").appendTo("body");
	}
	ctx.css({display: "block"});
	JE.gadget.clearError(ctx)

	if(jqXHR.responseText){
		var msg = jQuery.parseJSON(jqXHR.responseText);
		message = msg.PERM_DENIED;
		AJS.messages.error(ctx, {
		    body: message,
		    closeable: true
		});
	}
}

function customXhrCall(xhr, successCallback, errorCallback) {
    var method = '';
    if (!!xhr.data) {
        method = 'POST';
    } else {
        method = 'GET';
    }

    if (!!xhr.method) {
        method = xhr.method;
    }


    AJS.$.ajax({
        url: xhr.url,
        type: method,
        contentType: "application/json",
        data: JSON.stringify(xhr.data),
        Accept: "application/json",
        success: function (response) {
            setTimeout(function() {
                successCallback(response);
            }, 500);
        },
        error: function (xhr, status, error) {
            if (xhr.status !== 403) {
                console.log('status code : ', xhr.status);
                var errorMsg = xhr.responseText && JSON.parse(xhr.responseText) && JSON.parse(xhr.responseText).error;
                if (errorMsg) {
                    showToastrMsg(errorMsg, 'error');
                }
                errorCallback(xhr);
            }
            console.log('error', xhr, error);
        },
        statusCode: {
            403: function (xhr, status, error) {
                console.log('status code : 403')
                errorCallback(xhr);
            }

        }
    });
}
