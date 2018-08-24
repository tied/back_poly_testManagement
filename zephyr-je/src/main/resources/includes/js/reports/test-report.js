/**
 * @namespace ZEPHYR.REPORT
 */
if (typeof ZEPHYR == 'undefined') {
    ZEPHYR = {};
}

ZEPHYR.REPORT = (function ($) {
    return {
        paint: function (projectId, versionId, grpFld, defectsCount, pickStatus, cycle, reportKey, sprintId) {
            var reportType = reportKey;
            // if reportType is not defined, set it to default EXECUTION report
            if (!reportType)
                reportType = ZEPHYR.REPORT.KEYS.EXECUTION;

            var report;
            // invoke respective report object's paint()
            if (reportType.indexOf(ZEPHYR.REPORT.KEYS.EXECUTION) > -1){
                report = new ZEPHYR.REPORT.EXECUTION();
                report.grpFld = grpFld;
                report.showOthers = ZephyrURL('?showOthers') || false;
                report.itemsXAxis = ZephyrURL('?itemsXAxis') || 0;
                report.itemsPerGroup = ZephyrURL('?itemsPerGroup') || 3;
                report.sortSprintBy = ZephyrURL('?sortSprintBy') || 'count';
            }
            else if (reportType.indexOf(ZEPHYR.REPORT.KEYS.TOP_DEFECTS) > -1){
                report = new ZEPHYR.REPORT.TOPDEFECTS();
                report.defectsCount = defectsCount;
                report.pickStatus = pickStatus;
            }
            else if (reportType.indexOf(ZEPHYR.REPORT.KEYS.BURN_DOWN) > -1){
                report = new ZEPHYR.REPORT.BURNDOWN();
                report.cycle = cycle;
            }

            report.projectId = projectId;
            report.versionId = versionId;
            if(sprintId && sprintId != -1) {
            	report.sprintId = sprintId;
            }
            report.paint();
        }
    }
})(AJS.$);

ZEPHYR.REPORT.prototype = AJS.$.extend(ZEPHYR.REPORT.prototype, {

    getVersionInfo: function (projectId, versionId) {
        return {
            key: "versionInfo",
            ajaxOptions: function () {
                return {
                    url: getRestURL() + "/util/versionBoard-list?projectId=" + projectId + "&versionId=" + versionId,
                    contentType: "application/json",
                    complete: function (xhr, status, response) {
                        if (xhr.status != 200) {
                            JE.gadget.zephyrError(xhr, gadget);
                        }
                    }
                };
            }
        }
    },
    getSprintsByVersion: function(projectId, versionId, sprintId, sprintSelectElement) {
    	ZEPHYR.REPORT.prototype.sprintCount = 0;
    	if(ZEPHYR.REPORT.prototype.sprintIds && ZEPHYR.REPORT.prototype.sprintIds.length) {
			// attach all available sprints
    		ZEPHYR.REPORT.prototype.getSprintDetailsFromSprintIds(ZEPHYR.REPORT.prototype.sprintIds, function() {
    			ZEPHYR.REPORT.prototype.attachSprintOption(sprintSelectElement, sprintId);
    		});  
		} else {
			var sprintOption = AJS.$("<option/>").attr('value', -1)
				.text("-");
			sprintSelectElement.append(sprintOption);
			sprintSelectElement.val(sprintId);
		}
    },
    getSprintDetailsFromSprintIds: function(sprintIds, callBack) {
		ZEPHYR.REPORT.prototype.sprints = [];
		if(ZEPHYR.REPORT.prototype.sprintCount != 0)
			return;
		else
			ZEPHYR.REPORT.prototype.sprintCount = 1;	
		
		for(var i=0; i <sprintIds.length; i++) {
			ZEPHYR.REPORT.prototype.getSprintDetailsFromId(sprintIds[i], sprintIds.length, callBack);
		}
    },
    getSprintDetailsFromId: function(sprintId, sprintKeyLength, callBack) {
    	AJS.$.ajax({
            url: contextPath + "/rest/agile/1.0/sprint/" + sprintId,
            contentType: "application/json",
            success: function (sprint) {
            	ZEPHYR.REPORT.prototype.sprints.push(sprint);
                 if(ZEPHYR.REPORT.prototype.sprintCount == sprintKeyLength) {
                 	ZEPHYR.REPORT.prototype.sprintCount = 0;
                 	callBack();
                 } else
                 	ZEPHYR.REPORT.prototype.sprintCount++;
            }
        });
    },
    updateCycleSelectOptions: function (projectId, versionId, cycleSelectElem, filterCyclesWithSprint) {
        AJS.$.ajax({
            url: getRestURL() + "/cycle",
            type: "GET",
            contentType: "application/json",
            data: {
                versionId: versionId,
                projectId: projectId,
                expand: "executionSummaries"
            },
            success: function (response) {
                var cycleDetails = response;
                // add all available cycles
                var cycleOption = AJS.$("<option/>").attr("value", "-2").attr("title", AJS.I18n.getText('je.gadget.common.cycles.all.label')).text(AJS.I18n.getText('je.gadget.common.cycles.all.label'));
                cycleSelectElem.append(cycleOption);
                AJS.$.each(cycleDetails, function (key, cycle) {
                    if (key && cycle) {
                        if(filterCyclesWithSprint && cycle.sprintId) // If filter cycles with sprintId
                        	return;
                        var cycleOption = AJS.$("<option/>").attr("value", key).attr("title", cycle.name).text(cycle.name);
                        cycleSelectElem.append(cycleOption);
                    }
                });
            }
        });
    },
    updateCycleBySprintSelectOptions: function(projectId, versionId, sprintId, cycleId, cycleSelectElem, sprintSelectElem) {
    	cycleSelectElem.empty();

    	AJS.$.ajax({
            url: getRestURL() + "/cycle",
            type: "GET",
            contentType: "application/json",
            data: {
                versionId: versionId,
                projectId: projectId
            },
    		dataType:'json',
    		success: function (response){
    			ZEPHYR.REPORT.prototype.cycleDetails = response;
    			// Get cycles filtered by sprint
    			var cycleDetails = ZEPHYR.REPORT.prototype.filterCyclesBySprint(sprintId); // Default 'No Sprint'
    			// Attach Sprints
    			ZEPHYR.REPORT.prototype.getSprintsByVersion(projectId, versionId, sprintId, sprintSelectElem);
    			ZEPHYR.REPORT.prototype.attachCycleOptions(cycleSelectElem, cycleDetails, cycleId);
    		}
    	});
    },
    filterCyclesBySprint: function(sprintUserPrefValue) {
    	var _cycleDetails = [];
    	var _cycleValues = _.values(ZEPHYR.REPORT.prototype.cycleDetails);
    	var _sprintIds = _.pluck(_cycleValues, 'sprintId');
    	_sprintIds = _.compact(_sprintIds); // Remove falsy values
    	ZEPHYR.REPORT.prototype.sprintIds = _.uniq(_sprintIds); // Remove duplicates
    	var _filterBySprintId = false;
    	
    	if(ZEPHYR.REPORT.prototype.sprintIds && ZEPHYR.REPORT.prototype.sprintIds.length) {
    		_filterBySprintId = true;
    	}
    	/**
    	 * Check the sprintIds length
    	 * 1. If empty show all the cycles
    	 * 2. If having the sprintId, based on the filterId selected filter the cycles.
    	 */
    	_cycleDetails = _.filter(ZEPHYR.REPORT.prototype.cycleDetails, function(cycle, key){
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
    },
    updateSprintSelectOptions: function(sprintId, cycleSelectElem) {
    	// Get cycles filtered by sprint
    	var cycleDetails = ZEPHYR.REPORT.prototype.filterCyclesBySprint(sprintId);
    	cycleSelectElem.empty();
    	ZEPHYR.REPORT.prototype.attachCycleOptions(cycleSelectElem, cycleDetails);
    },
    attachCycleOptions: function(cycleSelectElem, cycleDetails, cycleId) {    	
        var cycleOption = AJS.$("<option/>").attr("value", "-2").attr("title", AJS.I18n.getText('je.gadget.common.cycles.all.label')).text(AJS.I18n.getText('je.gadget.common.cycles.all.label'));
        cycleSelectElem.append(cycleOption);
		// add all available cycles
		AJS.$.each(cycleDetails, function(key, cycle) { 
			var cycleOption = AJS.$("<option/>").attr("value", cycle.id).attr("title", cycle.name).text(cycle.name);
            cycleSelectElem.append(cycleOption);
		});
		cycleSelectElem.val(cycleId);
    },
    prepareCycleSelectOption: function () {
        // this is a hack to get "Cycles" for selected project and version. We are capturing projectId in cycle select field from server side.
        // Once we get the projectId, we will remove this cycle field, fetch new cycles and add those.
        var cycleSelectElem = AJS.$('#cycle_select');
        var versionSelectElem = AJS.$('#versionId_select');
        var sprintSelectElem = AJS.$('#sprintId_select');
        var isValidProjectFld = cycleSelectElem && cycleSelectElem.length > 0;
        var isValidVersionFld = versionSelectElem && versionSelectElem.length > 0;
        var filterCyclesWithSprint = false;

        if (isValidProjectFld && isValidVersionFld) {
            if(!this.selectedProjectId){
                var projectId = cycleSelectElem.val();
                this.selectedProjectId = projectId;
            }
            var versionId = versionSelectElem.val();
            //remove project from cycle select field.
            cycleSelectElem.find('option').remove();
            if(sprintSelectElem.length && sprintSelectElem.val() == -1)
            	filterCyclesWithSprint = true;

            // now get the cycles for above found projectId and versionId
            this.updateCycleSelectOptions(this.selectedProjectId, versionId, cycleSelectElem, filterCyclesWithSprint);
        }
    },
    prepareCycleBySprintSelectOption: function(sprintId, cycleId) {
        // this is a hack to get "Cycles" for selected project and version. We are capturing projectId in cycle select field from server side.
        // Once we get the projectId, we will remove this cycle field, fetch new cycles and add those.
        var cycleSelectElem = AJS.$('#cycle_select');
        var versionSelectElem = AJS.$('#versionId_select');
        var sprintSelectElem = AJS.$('#sprintId_select');
        var isValidProjectFld = cycleSelectElem && cycleSelectElem.length > 0;
        var isValidVersionFld = versionSelectElem && versionSelectElem.length > 0;

        if (isValidProjectFld && isValidVersionFld) {
            if(!this.selectedProjectId){
                var projectId = cycleSelectElem.val();
                this.selectedProjectId = projectId;
            }
        	sprintId = sprintId || -1;
        	cycleId = cycleId || '';
            var versionId = versionSelectElem.val();
            // var sprintId = sprintSelectElem.val();
            //remove project from cycle select field.
            cycleSelectElem.find('option').remove();
            //remove project from cycle select field.
            sprintSelectElem.find('option').remove();
            if(versionId != -2 && versionId != -3) { // Released and Unreleased version
            	this.updateCycleBySprintSelectOptions(this.selectedProjectId, versionId, sprintId, cycleId, cycleSelectElem, sprintSelectElem);
            } 
        }
    },
    attachSprintOption: function(sprintSelectElement, sprintId) {
    	/** 
    	 * Attach 'No Sprint' option
    	 */
    	var noSprintOption = AJS.$("<option/>").attr('value', -1)
    		.text("-");
    	sprintSelectElement.append(noSprintOption);
    	ZEPHYR.REPORT.prototype.sprints = _.sortBy(ZEPHYR.REPORT.prototype.sprints, function(sprint){ return sprint.name.toLowerCase(); }); // Sort sprints by name
    	AJS.$.each(ZEPHYR.REPORT.prototype.sprints, function(key, sprint) { 
			var sprintOption = AJS.$("<option/>").attr("value", sprint.id).attr("title",sprint.name).text(sprint.name);
			sprintSelectElement.append(sprintOption);
		});
    	sprintSelectElement.val(sprintId);
    },
    initSprintOrCycleBasedOnProjectType: function() {
    	// this is a hack to get "Cycles" for selected project and version. We are capturing projectId in cycle select field from server side.
        // Once we get the projectId, we will remove this cycle field, fetch new cycles and add those.
        var sprintSelectElem = AJS.$('#sprintId_select');
        var isProjectTypeSoftware = sprintSelectElem && sprintSelectElem.length && sprintSelectElem.find(':selected').text().trim() == 'true';
        if (isProjectTypeSoftware) {
        	var sprintId = ZephyrURL('?sprintId');
        	var cycleId = ZephyrURL('?cycle');
        	ZEPHYR.REPORT.prototype.prepareCycleBySprintSelectOption(sprintId, cycleId);
        } else {
        	ZEPHYR.REPORT.prototype.prepareCycleSelectOption();
        	sprintSelectElem.closest('.field-group').remove();
        }
        return isProjectTypeSoftware;
    }
});

ZEPHYR.REPORT.KEYS = {EXECUTION: "testcase-execution-report", TOP_DEFECTS: "test-topdefects-report", BURN_DOWN: "test-burndown-report"}

