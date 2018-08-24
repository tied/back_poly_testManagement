/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

// Cycles
ZEPHYR.Agile.TestBoard.Cycle = Backbone.Model.extend();
ZEPHYR.Agile.TestBoard.CycleCollection = Backbone.Collection.extend({
    model: ZEPHYR.Agile.TestBoard.Cycle,
    url: function() {
    	return getRestURL() + "/cycle/cyclesByVersionsAndSprint";
    },
    parse: function(resp, xhr) {
		/**
		 * Considering only resp[0] and not looping because the cycles are fetched per sprintId.
		 */
		if(resp[0])
		   	return resp[0].cycles;
    }
});

// {call ZEPHYR.Agile.TestBoard.getCycleZQLQuery data="[$cycle.versionName, $cycle.name]" /}
ZEPHYR.Agile.TestBoard.getCycleZQLQuery = function(params, output) {
	var fixVersion = params[0],
		cycleName = params[1],
		projectKey = params[2],
		zql = 'project = "' + ZEPHYR.Agile.TestBoard.escapeString(projectKey) + '" AND fixVersion = "' + ZEPHYR.Agile.TestBoard.escapeString(fixVersion) + '" AND cycleName in ("'
			+ ZEPHYR.Agile.TestBoard.escapeString(cycleName) +'")';

	return appendSoyOutputOnCall(encodeURIComponent(zql), output);
}

// {call ZEPHYR.Agile.TestBoard.getFolderZQLQuery
ZEPHYR.Agile.TestBoard.getFolderZQLQuery = function(params, output) {
	var fixVersion = params[0],
		cycleName = params[1],
		projectKey = params[2],
		folderName = params[3],
		zql = 'project = "' + ZEPHYR.Agile.TestBoard.escapeString(projectKey) + '" AND fixVersion = "' + ZEPHYR.Agile.TestBoard.escapeString(fixVersion) + '" AND cycleName in ("'
			+ ZEPHYR.Agile.TestBoard.escapeString(cycleName) +'")' + ' AND folderName = "' + ZEPHYR.Agile.TestBoard.escapeString(folderName) + '"';

	return appendSoyOutputOnCall(encodeURIComponent(zql), output);
}

/**
 * - Display Cycles.
 * - Need to decide how show more is shown if many cycles
 */
ZEPHYR.Agile.TestBoard.SprintCyclesView = Backbone.View.extend({
	events: {
		'click .zephyr-tb-key .zephyr-tb-cycle-link': 'preventDefaultNavigation',
		'click .zephyr-tb-cycle-remove': 'removeCycleFromSprintDialogUI'
	},
	initialize: function(options) {
		this.sprintId = options.sprintId;
		this.sprintName = options.sprintName;
		this.model.bind('reset', this.render, this);
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.CYCLES_FILTER, this.filterCycles, this);
		this.triggerSelectedClick = true;
	},
	filterCycles: function(cycleName) {
		cycleName = (cycleName)? cycleName.toString().toLowerCase(): '';
		var cycles = this.model.filter(function(cycle) {
				var name = cycle.get("name").toLowerCase();
				return (name.indexOf(cycleName) > -1);
			}).map(function(cycle) {
				return cycle.attributes;
			});
		this.displayCycles(cycles);
		ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.CYCLES_COUNT_UPDATE + '_' + this.sprintId, cycles.length);
	},
	removeCycleFromSprintDialogUI: function(ev) {
		ev.preventDefault();
		ev.stopImmediatePropagation();
		var cycleId = AJS.$(ev.target).data('id'),
			versionId = AJS.$(ev.target).data('version-id'),
			folderId = AJS.$(ev.target).data('folder-id'),
			folderName = AJS.$(ev.target).data('folder-name'),
			cycleName = this.model.get(cycleId).get('name'),
			sprintId = this.sprintId,
			label = AJS.I18n.getText('zephyr-je.testboard.cycle.remove.title'),
			dialogButtons = [
				{
					id : 'zephyr-je-dialog-remove',
				    name : AJS.I18n.getText('zephyr-je.testboard.cycle.button.remove.label')
				},
		        {
		        	id : 'zephyr-je-dialog-cancel',
		            name : AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		            type : 'link'
		        }
		     ];
		     if (folderId) {
                 label = AJS.I18n.getText('zephyr-je.testboard.folder.remove.title')
	     	     dialogContentHTML = ZEPHYR.Templates.TestBoard.Sprint.removeFolderFromSprintHTML({
	     	    	 folderName: folderName,
	     			 sprintName: this.sprintName
	     	     });

		     } else {
			     dialogContentHTML = ZEPHYR.Templates.TestBoard.Sprint.removeCycleFromSprintHTML({
			    	 cycleName: cycleName,
					 sprintName: this.sprintName
			     });
			 }
		     dialogId = "zephyr-tb-remove-test-cycle",
		     dialogRenderHTML = ZEPHYR.Templates.Dialog.renderDialog({
				"dialogId" : dialogId,
				"dialogSize" : "aui-dialog2-medium",
				"dialogHeader" : label,
				"dialogContent" : dialogContentHTML,
				"dialogButtons" : dialogButtons,
				isModal: true
		     }),
		     instance = this;

		AJS.$('body').append(dialogRenderHTML);
		var removeCycleFromSprintDialog = AJS.dialog2('#' + dialogId); // Create Dialog2 component
		removeCycleFromSprintDialog.show();
		ZEPHYR.About.evalMessage({selector:".zephyr-tb-eval-lic"}, null, function(){
			var contentHeight = removeCycleFromSprintDialog.$el.find('.dialog2-content').height() + 40; // Content height + padding
			removeCycleFromSprintDialog.$el.find('.aui-dialog2-content').css('height', contentHeight);
		});
		// Remove button
		removeCycleFromSprintDialog.$el.find('#zephyr-je-dialog-remove').click(function(ev) {
	        ev.preventDefault();
			AJS.$('#dialog-icon-wait').show();
	        instance.removeCycleFromSprint(cycleId, versionId, sprintId, folderId,function() {
		        if(ZEPHYR.Agile.TestBoard.data.selectedCycle == cycleId) {
		        	AJS.$('.zephyr-tb-detail-close').trigger('click'); // Close the detail panel if the selected cycle is removed from sprint
		        }
		        removeCycleFromSprintDialog.hide();
	        });
	    });
		// Cancel link
		removeCycleFromSprintDialog.$el.find('#zephyr-je-dialog-cancel').click(function(ev) {
	        ev.preventDefault();
	        removeCycleFromSprintDialog.hide();
	    });
	},
	removeCycleFromSprint: function(cycleId, versionId, sprintId,folderId, callback) {
		jQuery.ajax({
    		url: getRestURL() + "/cycle",
    		type : "PUT",
    		accept: "PUT",
    		contentType :"application/json",
    		dataType: "json",
    		data: JSON.stringify( {
    			  'id' : cycleId,
    	          'projectId': ZEPHYR.Agile.TestBoard.data.projectId,
    	          'versionId': versionId,
    	          'sprintId': "",
    	          'folderId' : folderId
    		}),
    		timeout: 0,
    		success : function(response) {
    			if(callback)
					callback();
				ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.CYCLE_PROGRESS_BAR_UPDATE + '_' + sprintId);
    		},
    		error : function(response) {
    			AJS.$('.zephyr-tb-message-bar').html('');
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
				if(response.status == 403 && !responseText) { // Cycle permission error messages
					showAUIError(response, AJS.$('.message-bar'));
    				AJS.$('#zephyr-je-dialog-remove').attr('disabled', 'disabled');
    				AJS.$('#dialog-icon-wait').hide();
    				return;
    			} else {
    				if(callback)
    					callback();
    			}
				ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000);
    		}
    	});
	},
	preventDefaultNavigation: function(ev) {
		ev.preventDefault();
	},
	displayCycles: function(cycles) {
		cyclesAlias = JSON.parse(JSON.stringify(cycles));
		cyclesAlias.forEach(function(cycle){
			cycle.name = cycle.name;
			cycle.build = cycle.build;
			cycle.description = cycle.description;
			cycle.environment = cycle.environment;
			cycle.folders = cycle.folders || [];
		});
		var cyclesHTML = ZEPHYR.Templates.TestBoard.Sprint.cyclesHTML({
			cycles: cyclesAlias,
			sprintId: this.sprintId
		});
		this.$el.html(cyclesHTML);
		if(ZEPHYR.Agile.TestBoard.data.isDetailView && this.sprintId == ZEPHYR.Agile.TestBoard.data.selectedSprint && this.triggerSelectedClick) {
			// Select the previously selected cycle
			this.$el.find('.zephyr-tb-cycle-content[data-cycle-id="' + ZEPHYR.Agile.TestBoard.data.selectedCycle + '"]').click();
		} else {
			this.$el.find('.zephyr-tb-cycle-content[data-cycle-id="' + ZEPHYR.Agile.TestBoard.data.selectedCycle + '"]').addClass('zephyr-tb-selected');
			this.triggerSelectedClick = true;
		}
	},
	render: function() {
		var cycles = this.model.toJSON();
		this.displayCycles(cycles);
		ZEPHYR.Agile.TestBoard.updateContentAreaHeight();
		return this;
	}
});