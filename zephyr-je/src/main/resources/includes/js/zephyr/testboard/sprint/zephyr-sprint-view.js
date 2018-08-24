/* global AJS */
/* global Backbone */
/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Agile == 'undefined') { ZEPHYR.Agile = {}; }
if (typeof ZEPHYR.Agile.TestBoard == 'undefined') { ZEPHYR.Agile.TestBoard = {}; }

/**
 * - SprintCyclesView - display Cycles. need to decide how show more is shown if many cycles
 * - show/hide cycles
 * - click on cycle render the SprintCycleDetailViews
 */
ZEPHYR.Agile.TestBoard.SprintView = Backbone.View.extend({
	/*events: {
		'.toggle-title':	'toggleSprint'
	},*/
	initialize: function() {
		this.attachViews();
		this.sprintId = this.model.get('id');
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.CYCLES_COUNT_UPDATE + '_' + this.sprintId, this.displayTestCycleCount, this);
		ZEPHYR.Agile.TestBoard.globalDispatcher.off(ZEPHYR.Agile.TestBoard.Events.CYCLE_PROGRESS_BAR_UPDATE + '_' + this.sprintId);
		ZEPHYR.Agile.TestBoard.globalDispatcher.on(ZEPHYR.Agile.TestBoard.Events.CYCLE_PROGRESS_BAR_UPDATE + '_' + this.sprintId, this.updateCycleProgressBar, this);
		this.selectedCycleIds = [];
	},
	displayTestCycleCount: function(cycleCount) {
		var cycleCountHTML;

		if(cycleCount < this.cycleCount) {
			cycleCountHTML = AJS.I18n.getText('zephyr-je.testboard.details.execution.count.visible', cycleCount, this.cycleCount);
		} else {
			cycleCountHTML = AJS.I18n.getText('zephyr-je.testboard.details.execution.count', cycleCount);
		}
		this.$el.find('#zephyr-tb-cycle-count').html(cycleCountHTML);
	},
	attachViews: function() {
		var sprintId = this.model.get('id');
		var instance = this;

		// Cycles View
    	this.cyclesView = new ZEPHYR.Agile.TestBoard.SprintCyclesView({
			el: '#sprint-cycle-container-' + sprintId,
			model: this.model.cycles,
			sprintId: sprintId,
			sprintName: this.model.get('name')
		});
    	this.fetchCyclesByVersionsAndSprint();
    	AJS.$('#zephyr-tb-link-test-cycles_' + sprintId).unbind('click')
    		.bind('click', function(ev) {
    			ev.preventDefault();
    			instance.showLinkTestCyclesDialog(null, null, false, false, null);
    		});

		return this;
	},
	getFilteredProjects: function() {
		var projects = ZEPHYR.Agile.TestBoard.data.projects.toJSON();
		var projectCount = ZEPHYR.Agile.TestBoard.data.projectId.toString().split(',').length;

		if(projectCount == 1) {
			var projectList = [],
				project = ZEPHYR.Agile.TestBoard.data.projects.get(ZEPHYR.Agile.TestBoard.data.projectId);
			if(project)
				projectList.push(project.toJSON());
			return projectList;
		} else {
			return projects;
		}
	},
	showLinkTestCyclesDialog: function(projectId, versionId, isLinked, refreshOnCancel, folderId) {
		var label = AJS.I18n.getText('zephyr-je.testboard.link.test.cycles.header.label'),
			dialogButtons = [
				{
					id : 'zephyr-je-dialog-save',
				    name : AJS.I18n.getText('zephyr.je.save.button.title')
				},
		        {
		        	id : 'zephyr-je-dialog-cancel',
		            name : AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		            type : 'link'
		        }
		     ],
		     dialogContentHTML = ZEPHYR.Templates.TestBoard.Sprint.linkToTestCyclesHTML({
		    	 projects: this.getFilteredProjects(),
				 selectedProjectId: projectId
		     }),
		     dialogId = "zephyr-tb-link-to-test-cycles",
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
		var linkToTestCyclesDialog = AJS.dialog2('#' + dialogId); // Create Dialog2 component
		linkToTestCyclesDialog.show();
		ZEPHYR.About.evalMessage({selector:".zephyr-tb-eval-lic"}, null, function(){
			var contentHeight = linkToTestCyclesDialog.$el.find('.dialog2-content').height() + 40; // Content height + padding
			linkToTestCyclesDialog.$el.find('.aui-dialog2-content').css('height', contentHeight);
		});
		// Save button
		linkToTestCyclesDialog.$el.find('#zephyr-je-dialog-save').click(function(ev) {
	        ev.preventDefault();
			linkToTestCyclesDialog.$el.find('#dialog-icon-wait').show();
			linkToTestCyclesDialog.$el.find('#zephyr-je-dialog-save').attr('disabled', 'disabled');
			instance.saveLinkedTestCycleUI(function() {
				linkToTestCyclesDialog.hide();
			}, linkToTestCyclesDialog.$el.find('#zephyr-tb-add-more-cycle').is(':checked'), refreshOnCancel, folderId);
	    })
		.attr('disabled', 'disabled')
		.before(ZEPHYR.Templates.TestBoard.Sprint.linkToTestCyclesCheckboxHTML({
			isLinked: isLinked
		}));
		// Cancel link
		linkToTestCyclesDialog.$el.find('#zephyr-je-dialog-cancel').click(function(ev) {
	        ev.preventDefault();
	        if(refreshOnCancel) {
	        	ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.VERSION_SELECT_SPRINTS_GET);
	        }
		    linkToTestCyclesDialog.hide();
	    });
		var selectedProjectId = projectId || linkToTestCyclesDialog.$el.find('#zephyr-tb-link-tests-project-select').val();
		this.attachProjectsSingleSelect(selectedProjectId, versionId);
	},
	attachProjectsSingleSelect: function(projectId, versionId) {
		var instance = this;

		AJS.$('#zephyr-tb-link-tests-project-select').auiSelect2({
				formatResult: ZEPHYR.Agile.TestBoard.auiSelect2.formatResult,
				formatSelection: ZEPHYR.Agile.TestBoard.auiSelect2.formatSelection
			}).on("change", function (ev) {
				var selectedProjectId = AJS.$(ev.target).val();
				AJS.$('#dialog-icon-wait').show();
				AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
				var versions = instance.getVersionsByProjectId(selectedProjectId);
				instance.attachVersionsSingleSelect(selectedProjectId, versions, null);
				AJS.$('.zephyr-tb-eval-lic').empty(); // Clear the previous aui messages
			});
		AJS.$('.select2-container').css({'width': '250px'});
		var versions = this.getVersionsByProjectId(projectId);
		this.attachVersionsSingleSelect(projectId, versions, versionId);
	},
	attachVersionsSingleSelect: function(projectId, versions, versionId) {
		var instance = this;
		var versionEl = AJS.$('#zephyr-tb-link-tests-version-select');
		versionEl.html('');
		_.each(versions, function(version, key) {
			var versionName = version.name;
			versionEl.append(AJS.$("<option />")
				.val(version.versionId)
				.attr('title', AJS.escapeHtml(versionName))
				.html(AJS.escapeHtml(versionName)));
		}, this);
		versionEl.val(versionId);
		versionEl.auiSelect2({
				formatResult: ZEPHYR.Agile.TestBoard.auiSelect2.formatResult,
				formatSelection: ZEPHYR.Agile.TestBoard.auiSelect2.formatSelection
			})
			.on("change", function (ev) {
				var versionId = AJS.$(ev.target).val();
				var cycleIds = instance.getCycleIds();
				var projectId = AJS.$('#zephyr-tb-link-tests-project-select').val();
				AJS.$('#dialog-icon-wait').show();
				AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
				instance.getCyclesByVersionId(projectId, versionId, cycleIds);
				instance.toggleSaveButton(versionId);
			});
		var selectedVersionId = versionId || AJS.$('#zephyr-tb-link-tests-version-select').val();
		var cycleIds = this.getCycleIds();
		this.getCyclesByVersionId(projectId, selectedVersionId, cycleIds);
		instance.toggleSaveButton(selectedVersionId);
		AJS.$('.select2-container').css({'width': '250px'});
	},
	getVersionsByProjectId: function(projectId) {
		var versionListPerProject = [];

		if(ZEPHYR.Agile.TestBoard.data.versionId && !isNaN(ZEPHYR.Agile.TestBoard.data.versionId)) {
			var versionId = ZEPHYR.Agile.TestBoard.data.versionId;
			versionListPerProject = _.filter(ZEPHYR.Agile.TestBoard.data.versions, function(version) {
					return (version.versionId == versionId)
				}).filter(function(version) {
					return (version.projectId == projectId && !version.released);
				});
		} else  {
			versionListPerProject = _.filter(ZEPHYR.Agile.TestBoard.data.versions, function(version) {
					return (version.projectId == projectId && !version.released)
				});
		}

		return versionListPerProject;
	},
	filterCyclesByLink: function(cycles, cycleIds) {
		var cyclesJSON = {
			linkedCycles: [],
			unlinkedCycles: []
		};

		AJS.$.each(cycles, function(key, cycle) {
			//if(_.contains(cycleIds, parseInt(key)))
			// 	return true;		//Skip the target cycle
			if(key != "recordsCount" && key != "offsetCount") {
				if (parseInt(key) != -1 ) {
					cycle['id'] = key;
					//var selectedSprint = ZEPHYR.Agile.TestBoard.data.sprints.get(cycle.sprintId);
					//if(cycle.sprintId){
					//	cyclesJSON.linkedCycles.push(cycle);
					//} else {
					cyclesJSON.unlinkedCycles.push(cycle);
					//}
				}
			}
		});

		return cyclesJSON;
	},
	filterFoldersByLink: function(folders, folderIds) {
		var foldersJSON = {
			linkedFolders: [],
			unlinkedFolders: []
		};
		var instance = this;
		folders.forEach(function(folder) {
			// if(_.contains(folderIds, folder.folderId))
			// 	return true;
			if(folder.sprintId == instance.model.get('id')){
				foldersJSON.linkedFolders.push(folder);
			} else {
		 		foldersJSON.unlinkedFolders.push(folder);
			}
		});

		return foldersJSON;
	},
	getCycleLabel: function(cycleName, sprintId, selectId) {
		if(sprintId) {
			var selectedSprint = ZEPHYR.Agile.TestBoard.data.sprints.get(sprintId);
			if(selectedSprint) {
				var sprintName = selectedSprint.get('name');
				return cycleName + ' (' + sprintName  + ')';
			} else {
				// Fix for ZFJ-1774
				ZEPHYR.Utility.getSprintDetailsFromId(sprintId, function(sprint) {
					var _cycleName = cycleName + ' (' + sprint.name  + ')';
					if(AJS.$('#' + selectId +' option:selected').data('sprint-id') == sprintId) {
						AJS.$('#s2id_' + selectId + ' .select2-chosen').html(_cycleName).attr('title', _cycleName);
					}
					AJS.$('#' + selectId).find('option[data-sprint-id=' + sprintId + ']').html(_cycleName)
						.attr('data-sprint-name', sprint.name);
				});
				return cycleName + ' (' + sprintId  + ')';
			}
		}
		return cycleName;
	},
	validateSprintId: function(sprintId) {
		var isSprintValid = false;
		var selectedSprint = ZEPHYR.Agile.TestBoard.data.sprints.get(sprintId);
		if(selectedSprint)
			isSprintValid = true;

		return isSprintValid;
	},
	attachCycleOptionHTML: function(cycles, attachEl, isLinked) {
		_.each(cycles, function(cycle, key) {
			var optionClass =(isLinked && key == 0)? 'zephyr-tb-sprint-cycle-option zephyr-tb-sprint-cycle-seperator': 'zephyr-tb-sprint-cycle-option'
			var cycleName = this.getCycleLabel(cycle.name, cycle.sprintId, attachEl.attr('id'));
			var sprintId = cycle.sprintId ? cycle.sprintId : null;
			attachEl.append(AJS.$("<option class='" + optionClass + "' data-sprint-id='" + sprintId + "' />")
				.val(cycle.id)
				.attr('title', AJS.escapeHtml(cycleName))
				.html(AJS.escapeHtml(cycleName)));
		}, this);
	},
	attachFolderOptionHTML: function(folders, attachEl, isLinked) {
		attachEl.append(AJS.$("<option class='" + "zephyr-tb-sprint-folder-option zephyr-tb-sprint-folder-option-default zephyr-tb-sprint-folder-seperator" + "' data-sprint-id='" + null + "' />")
				.val('')
				.attr('title', AJS.I18n.getText("zephyr-je.testboard.link.test.cycles.select.folder"))
				.html(AJS.I18n.getText("zephyr-je.testboard.link.test.cycles.select.folder")));
		_.each(folders, function(folder, key) {
			var optionClass =(isLinked && key == 0)? 'zephyr-tb-sprint-folder-option zephyr-tb-sprint-folder-seperator': 'zephyr-tb-sprint-folder-option'
			var folderName = this.getCycleLabel(folder.folderName, folder.sprintId, attachEl.attr('id'));
			var sprintId = folder.sprintId ? folder.sprintId : null;
			attachEl.append(AJS.$("<option class='" + optionClass + "' data-sprint-id='" + sprintId + "' />")
				.val(folder.folderId)
				.attr('title', AJS.escapeHtml(folderName))
				.html(AJS.escapeHtml(folderName)));
		}, this);
	},
	attachCyclesDD: function(cycles, cycleIds) {
		var cyclesJSON = this.filterCyclesByLink(cycles, cycleIds);
		var instance = this;
		instance.cycles = cycles;
		AJS.$('#zephyr-tb-link-tests-cycle-select').empty();
		if(cyclesJSON.unlinkedCycles.length) {
			this.attachCycleOptionHTML(cyclesJSON.unlinkedCycles, AJS.$('#zephyr-tb-link-tests-cycle-select'), false);
		}
		// if(cyclesJSON.linkedCycles.length) {
		// 	this.attachCycleOptionHTML(cyclesJSON.linkedCycles, AJS.$('#zephyr-tb-link-tests-cycle-select'), true);
		// }
		AJS.$('#zephyr-tb-link-tests-cycle-select').auiSelect2({
			formatResult: ZEPHYR.Agile.TestBoard.auiSelect2.formatResult,
			formatSelection: ZEPHYR.Agile.TestBoard.auiSelect2.formatSelection
		}).on('change', function(ev) {
			var cycleId = AJS.$(ev.target).val();
			var cycleObj = null;
			Object.keys(instance.cycles).forEach(function (key) {
				if (key == cycleId) {
					cycleObj = instance.cycles[key];
				}
			});
			AJS.$('#dialog-icon-wait').show();
			AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
			instance.getFoldersByCycleId(cycleId,cycleObj);
			instance.toggleSaveButton(cycleId);
		});
		var cycleId = AJS.$('#zephyr-tb-link-tests-cycle-select').val();
		var cycleObj = null;
		Object.keys(cycles).forEach(function (key) {
			if (key == cycleId) {
				cycleObj = cycles[key];
			}
		})
		if(cycleId) {
			instance.getFoldersByCycleId(cycleId,cycleObj);
		} else {
			AJS.$('#zephyr-tb-link-tests-folder-select').empty();
		}
		this.toggleSaveButton(cycleId);
		AJS.$('#dialog-icon-wait').hide();
		AJS.$('.select2-container').css({'width': '250px'});
	},
	attachFoldersDD: function(folders, cycleId) {
		var folderIds = [];
		folders.forEach(function(folder) {
			if(folder.sprintId) {
				folderIds.push(folder.folderId);
			}
		});
		var foldersJSON = this.filterFoldersByLink(folders, folderIds);
		var instance = this;
		AJS.$('#zephyr-tb-link-tests-folder-select').empty();
		if(foldersJSON.unlinkedFolders.length > 0) {
			this.attachFolderOptionHTML(foldersJSON.unlinkedFolders, AJS.$('#zephyr-tb-link-tests-folder-select'), false);
			AJS.$('#zephyr-je-dialog-save').prop('disabled', false);
		} else {
			var sprintId = AJS.$('#zephyr-tb-link-tests-cycle-select option:selected').data('sprint-id');
			var isSprintLinked = false;
			var cycle = this.model.cycles.filter(function(cycle) {
					return (cycle.get('id') == cycleId);
				}).map(function(cycle) {
					return cycle.attributes;
				});

			if(cycle[0] && cycle[0].sprintId === sprintId)
				isSprintLinked = true;

			if(isSprintLinked) {
				AJS.$('#zephyr-je-dialog-save').prop('disabled', true);
			} else {
				this.attachFolderOptionHTML(foldersJSON.unlinkedFolders, AJS.$('#zephyr-tb-link-tests-folder-select'), false);
				AJS.$('#zephyr-je-dialog-save').prop('disabled', false);
			}

		}
		// if(foldersJSON.linkedFolders.length) {
		// 	this.attachFolderOptionHTML(foldersJSON.linkedFolders, AJS.$('#zephyr-tb-link-tests-folder-select'), true);
		// }
		AJS.$('#zephyr-tb-link-tests-folder-select').auiSelect2({
			formatResult: ZEPHYR.Agile.TestBoard.auiSelect2.formatResult,
			formatSelection: ZEPHYR.Agile.TestBoard.auiSelect2.formatSelection
		}).on('change', function(ev) {
			var cycleId = AJS.$(ev.target).val();
			//AJS.$('#dialog-icon-wait').show();
			//AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
			//instance.toggleSaveButton(cycleId);
		});
		var cycleId = AJS.$('#zephyr-tb-link-tests-folder-select').val();
		//this.toggleSaveButton(cycleId);
		AJS.$('#dialog-icon-wait').hide();
		AJS.$('.select2-container').css({'width': '250px'});
	},
	getFoldersByCycleId: function(cycleId,cycleObj) {
		var instance = this;
    	AJS.$.ajax({
    		url: getRestURL() + "/cycle/" + cycleId + "/folders?projectId=" +cycleObj.projectId + "&versionId="+ cycleObj.versionId +  "&limit=1000&offset=0",
			type : "GET",
			contentType :"application/json",
			success: function(fetchedData){
				fetchedData = fetchedData.map(function(folder) {
            folder.folderName = folder.folderName;
            return folder;
        });
				instance.attachFoldersDD(fetchedData, cycleId);
			},
			error: function(response) {
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
	    		if(response.status == 403 && responseText) { // This is for JIRA authorization error messages
	    			AJS.dialog2('#zephyr-tb-link-to-test-cycles').hide();
	    			ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText);
	    		} else {
	    			if(response.status == 403) { // Cycle permission error messages
	    				responseText = responseJSON.PERM_DENIED;
	    				AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
	    			}
					AJS.$('#dialog-icon-wait').hide();
		    		AJS.$('.zephyr-tb-eval-lic').html('');
		    		AJS.$('.zfjEvalLic').remove();
					AJS.messages.error(".zephyr-tb-eval-lic", {
					   title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
					   body: responseText
					});
				}
	    	}
		});
	},
	/**
     * Fetches cycles by project and version
     */
    getCyclesByVersionId: function(projectId, versionId, cycleIds){
    	var instance = this;
    	AJS.$.ajax({
    		url: getRestURL() + "/cycle?versionId="+ versionId+"&projectId=" + projectId,
			type : "GET",
			contentType :"application/json",
			dataType: "json",
			success: function(fetchedData){
				Object.keys(fetchedData).forEach(function(k){
					if(!fetchedData[k].name) { return;}
					fetchedData[k].name = fetchedData[k].name;
				})
				instance.attachCyclesDD(fetchedData, cycleIds);
			},
			error: function(response) {
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
	    		if(response.status == 403 && responseText) { // This is for JIRA authorization error messages
	    			AJS.dialog2('#zephyr-tb-link-to-test-cycles').hide();
	    			ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText);
	    		} else {
	    			if(response.status == 403) { // Cycle permission error messages
	    				responseText = responseJSON.PERM_DENIED;
	    				AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
	    			}
					AJS.$('#dialog-icon-wait').hide();
		    		AJS.$('.zephyr-tb-eval-lic').html('');
		    		AJS.$('.zfjEvalLic').remove();
					AJS.messages.error(".zephyr-tb-eval-lic", {
					   title: AJS.I18n.getText("zephyr.je.submit.form.error.title"),
					   body: responseText
					});
				}
	    	}
		});
    },
	toggleSaveButton: function(value) {
		if(!value) {
			AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
		} else
			AJS.$('#zephyr-je-dialog-save').removeAttr('disabled');
	},
    getCycleIds: function() {
    	var cycleIds = [],
    		cycles = this.model.cycles.toJSON();

    	_.each(cycles, function(cycle) {
    		if(cycle.id) {
    			cycleIds.push(cycle.id);
    		}
    	});
    	cycleIds.push(-1); // Adhoc cycle
		cycleIds = _.union(cycleIds, this.selectedCycleIds);
    	return cycleIds;
    },
	isSprintLinkedToThisCycle: function(cycleId, sprintId) {
		var isSprintLinked = false;
		var cycle = this.model.cycles.filter(function(cycle) {
				return (!cycle.attributes.folders && cycle.get('id') == cycleId);
			}).map(function(cycle) {
				return cycle.attributes;
			});

		if(cycle[0] && cycle[0].sprintId != sprintId)
			isSprintLinked = true;
		return isSprintLinked;
	},
	saveLinkedTestCycle: function(projectId, cycleId, folderId, versionId, completed, errorCallback) {
		var sprintId = this.model.get('id');
		if(!sprintId || !cycleId)
			return;
		jQuery.ajax({
    		url: getRestURL() + "/cycle",
    		type : "PUT",
    		accept: "PUT",
    		contentType :"application/json",
    		dataType: "json",
    		data: JSON.stringify( {
    			  'id' : cycleId,
    			  'folderId': folderId,
    	          'projectId': projectId,
    	          'versionId': versionId,
    	          'sprintId': sprintId
    		}),
    		timeout: 0,
    		success : function(response) {
    			ZEPHYR.Agile.TestBoard.data.selectedSprint = sprintId;
    			if(completed)
    				completed.call();
    		},
    		error : function(response) {
    			AJS.$('.zephyr-tb-message-bar').html('');
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
    			if(response.status == 403 && !responseText) { // Cycle permission error messages
		    		showAUIError(response, AJS.$('.message-bar'));
    				AJS.$('#zephyr-je-dialog-save').attr('disabled', 'disabled');
    				AJS.$('#dialog-icon-wait').hide();
    				return;
    			} else {
    				if(errorCallback)
        				errorCallback.call();
    			}
				ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000);
    		}
    	});
	},
	showSaveLinkedCycleConfirmation: function(projectId, cycleId, folderId, versionId, cycleName, sprintName, selectedSprintName, isLinkMoreTestCycles, refreshOnCancel, folderName) {
		var name;

		if(folderId) {
			name = folderName;
		} else {
			name = cycleName;
		}

		var label = AJS.I18n.getText('zephyr-je.testboard.link.test.cycles.header.label'),
			dialogButtons = [
				{
					id : 'zephyr-je-dialog-save',
				    name : AJS.I18n.getText('zephyr.je.save.button.title')
				},
		        {
		        	id : 'zephyr-je-dialog-cancel',
		            name : AJS.I18n.getText('zephyr.je.submit.form.cancel'),
		            type : 'link'
		        }
		     ],
		     dialogContentHTML = ZEPHYR.Templates.TestBoard.Sprint.linkToTestCyclesConfirmationHTML({
				name: name,
				sprintName: sprintName,
				selectedSprintName: selectedSprintName,
				folderId : folderId
			 }),
		     dialogId = "zephyr-tb-link-to-test-cycles-confirmation",
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
		var linkToTestCyclesConfirmDialog = AJS.dialog2('#' + dialogId); // Create Dialog2 component
		linkToTestCyclesConfirmDialog.show();
		ZEPHYR.About.evalMessage({selector:".zephyr-tb-eval-lic-confirm"}, null, function(){
			var contentHeight = linkToTestCyclesConfirmDialog.$el.find('.dialog2-content').height() + 40; // Content height + padding
			linkToTestCyclesConfirmDialog.$el.find('.aui-dialog2-content').css('height', contentHeight);
		});
		// Save button
		linkToTestCyclesConfirmDialog.$el.find('#zephyr-je-dialog-save').click(function(ev) {
	        ev.preventDefault();
			linkToTestCyclesConfirmDialog.$el.find('#dialog-icon-wait').show();
			linkToTestCyclesConfirmDialog.$el.find('#zephyr-je-dialog-save').attr('disabled', 'disabled');
	        instance.saveLinkedTestCycle(projectId, cycleId, folderId, versionId, function() {
		        linkToTestCyclesConfirmDialog.hide();
				if(isLinkMoreTestCycles) {
					instance.selectedCycleIds.push(parseInt(cycleId));
					instance.showLinkTestCyclesDialog(projectId, versionId, isLinkMoreTestCycles, true, folderId);
				} else {
    				ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.VERSION_SELECT_SPRINTS_GET);
					instance.selectedCycleIds = [];
				}
	        },
			function() {
				linkToTestCyclesConfirmDialog.hide();
				instance.selectedCycleIds = [];
			});
	    });
		// Cancel link
		linkToTestCyclesConfirmDialog.$el.find('#zephyr-je-dialog-cancel').click(function(ev) {
	        ev.preventDefault();
			if(refreshOnCancel) {
				ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.VERSION_SELECT_SPRINTS_GET);
			}
	        linkToTestCyclesConfirmDialog.hide();
	    });
	},
    saveLinkedTestCycleUI: function(completed, isLinkMoreTestCycles, refreshOnCancel, folderId) {
		var cycleId = AJS.$('#zephyr-tb-link-tests-cycle-select').val(),
			folderId = AJS.$('#zephyr-tb-link-tests-folder-select').val(),
			versionId = AJS.$('#zephyr-tb-link-tests-version-select').val(),
			projectId = AJS.$('#zephyr-tb-link-tests-project-select').val(),
			selectedSprintId = AJS.$('#zephyr-tb-link-tests-cycle-select option:selected').data('sprint-id'),
			isSprintLinked = this.isSprintLinkedToThisCycle(cycleId, selectedSprintId),
			cycleName = AJS.$('#zephyr-tb-link-tests-cycle-select option:selected').text(),
			sprintName = this.model.get('name'),
			selectedSprintName = '',
			instance = this,
			folderName;
			if(folderId) {
				folderName = AJS.$('#zephyr-tb-link-tests-folder-select option:selected').text();
				selectedSprintId = AJS.$('#zephyr-tb-link-tests-folder-select option:selected').data('sprint-id');
			}

		if(!isSprintLinked && (selectedSprintId && selectedSprintId != 'undefined' && selectedSprintId != 'null')) {
			var selectedSprint = ZEPHYR.Agile.TestBoard.data.sprints.get(selectedSprintId);
			if(selectedSprint) {
				selectedSprintName = selectedSprint.get('name');
			} else {
				selectedSprintName = AJS.$('#zephyr-tb-link-tests-cycle-select option:selected').data('sprint-name');
				if(!selectedSprintName)
					selectedSprintName = selectedSprintId;
			}
			if(completed)
				completed();
			this.showSaveLinkedCycleConfirmation(projectId, cycleId, folderId, versionId, cycleName, selectedSprintName, sprintName, isLinkMoreTestCycles, refreshOnCancel, folderName);
		} else {
			/**
			 * On save if not linked to sprint,
			 * if user selects to link more than show the link to Test Cycle dialog
			 */
			this.saveLinkedTestCycle(projectId, cycleId, folderId, versionId, function() {
				if(completed)
					completed();
				if(isLinkMoreTestCycles) {
					instance.selectedCycleIds.push(parseInt(cycleId));
					instance.showLinkTestCyclesDialog(projectId, versionId, isLinkMoreTestCycles, true, folderId);
				} else {
					ZEPHYR.Agile.TestBoard.globalDispatcher.trigger(ZEPHYR.Agile.TestBoard.Events.VERSION_SELECT_SPRINTS_GET);
					instance.selectedCycleIds = [];
				}
			}, function() {
				if(completed)
					completed();
				instance.selectedCycleIds = [];
			});

		}
    },
	fetchCyclesByVersionsAndSprint: function() {
    	var projectId = ZEPHYR.Agile.TestBoard.data.projectId,
    		sprintId = this.model.get('id'),
    		instance = this;

    	this.model.cycles.fetch({
    		reset: true,
    		type : 'POST',
    		contentType: "application/json",
			data: JSON.stringify({
				projectId:projectId,
				versionId: ZEPHYR.Agile.TestBoard.data.versionId,
				sprintId: sprintId,
				offset:0,
				expand:"executionSummaries"
			}),
			success: function(cycles, response) {
				/**
				 * Considering only response[0] and not looping because the cycles are fetched per sprintId.
				 */
				instance.cycleCount = (response[0] && response[0].cycles) ? response[0].cycles.length : 0;
				instance.displayTestCycleCount(instance.cycleCount);
			},
			error: function(cycles, response) {
				AJS.$('.zephyr-tb-message-bar').html('');
				var responseJSON = JSON.parse(response.responseText);
				var responseText = responseJSON.message || responseJSON.errorDescHtml;
				ZEPHYR.Agile.TestBoard.showErrorMessage(response, responseText, 3000);
			}
    	});
	},
	updateCycleProgressBar: function() {
		this.cyclesView.triggerSelectedClick = false;
    	this.fetchCyclesByVersionsAndSprint();
	}
});
