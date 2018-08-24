var vanillaGrid = vanillaGrid || {};

(function(undefined) {

	var attachEvent = function(elem, eventType, cb, isCapture) {
		if(!isCapture) {
			isCapture = false;
		}
		if(!elem.length && elem.addEventListener) {
			elem.addEventListener(eventType, cb, isCapture);
		} else {
			for (var i = 0; i < elem.length; i++) {
    		elem[i].addEventListener(eventType, cb, isCapture);
			}
		}
	};

	var detachEvent = function(elem, eventType, cb, isCapture) {
		if(!isCapture) {
			isCapture = false;
		}
		if(!elem.length && elem.addEventListener) {
			elem.removeEventListener(eventType, cb, isCapture);
		} else {
			for (var i = 0; i < elem.length; i++) {
    		elem[i].removeEventListener(eventType, cb, isCapture);
			}
		}
	};

	var getEventTarget = function(ev) {
		var target = ev.target
		if(target.tagName === 'IMG' || (target.tagName === 'SPAN' && target.draggable)) {
			target = target.parentElement;
		}
		return target;
	}

	var onGridDblClick = function(config, containerId, ev) {
		ev.stopImmediatePropagation();
		var target = getEventTarget(ev);
		var className = target.className;
		var rowTarget = ev.target.closest('.row');
		var rowClassName = rowTarget ? rowTarget.className : '';
		if(/row/.test(rowClassName)){
			vanillaGrid.utils.selectRowDbl(rowTarget, config, containerId);
		}
	}

	var onGridClick = function(config, containerId, ev) {
		ZEPHYR.GRID.stopGridFocus = false;
		ev.stopImmediatePropagation();
		var target = getEventTarget(ev);
		var className = target.className;
		var rowTarget = ev.target.closest('.row');
		var rowClassName = rowTarget ? rowTarget.className : '';
		var el = document.querySelector('.activeElement');
		if(/row/.test(rowClassName)){
			vanillaGrid.utils.selectRows(rowTarget, config, containerId);
		}

		if(/pin-icon/.test(className)) {
			vanillaGrid.utils.pinColumns(target, className, config, containerId);
		} else if(/sort-icon/.test(className)) {
			vanillaGrid.utils.sortRows(target, className, containerId);
		} else if (/move-row/.test(className)) {
			if (/active/.test(className)) {
				vanillaGrid.utils.handleMoveRowEvent(target, config, containerId);
			}
		} else if(/actions-wrapper/.test(className)) {
			vanillaGrid.utils.handleActionGridEvents(target, config, containerId);
		} else if(/columnChooser-submit/.test(className)) {
			vanillaGrid.utils.columnChooser.columnChooserSubmit(target, config, containerId);
		} else if(/columnChooser-cancel/.test(className)) {
			vanillaGrid.utils.columnChooser.columnChooserCancel(target, config, containerId);
		} else if(/trigger-dropDown/.test(className)) {
			if(/date-dropdown/.test(className)) {
				vanillaGrid.utils.steps.triggerDateDropdown(target, ev, containerId, config);
			} else {
				vanillaGrid.utils.dropdown.triggerDropdown(target, containerId);
			}
		} else if(/dropdown-option/.test(className)) {
			vanillaGrid.utils.dropdown.optionClick(target, target.closest('.row'), containerId, config);
		} else if(/status-select-option/.test(className)) {
			vanillaGrid.utils.dropdown.selectOptionClick(target, config, containerId);
		} else if(/addTest-btn/.test(className)) {
			vanillaGrid.utils.addTests(target, containerId);
		} else if(/bulk-action/.test(className)) {
			if(config.isStepsGrid) {
				vanillaGrid.utils.steps.freezeColumns(target, containerId);
			} else if (config.isExecutionGrid) {
				vanillaGrid.utils.testExecution.freezeColumns(target, containerId);
			} else {
				vanillaGrid.utils.bulkActions(target, config, containerId);
			}
		} else if(/bulkAction-round/.test(className)) {
			vanillaGrid.utils.steps.popupTestSteps(target, containerId);
		} else if(/testStepFocus-btn/.test(className)) {
			vanillaGrid.utils.steps.focusSteps(target, containerId);
		} else if(/row-select-checkbox/.test(className)) {
			vanillaGrid.utils.maintainExecutionsList(target, config, containerId);
		} else if(/remove-data/.test(className)) {
			vanillaGrid.utils.dropdown.removeData(target, config, containerId);
		} else if(/add-attachments-trigger/.test(className)) {
			vanillaGrid.utils.attachments.add(target, containerId);
		} else if(/show-attachments-trigger/.test(className)) {
			vanillaGrid.utils.attachments.showThumbNails(target, containerId);
		} else if(/fetch-attachments-trigger/.test(className)) {
			vanillaGrid.utils.attachments.fetchAttachments(target, containerId);
		} else if(/icon-preview/.test(className)) {
			vanillaGrid.utils.attachments.iconPreview(target, config, containerId);
		} else if(/icon-delete/.test(className)) {
			vanillaGrid.utils.attachments.iconDelete(target, containerId);
		} else if(/columnChooser-btn/.test(className)) {
			vanillaGrid.utils.columnChooser.toggleColumnChooserPopup(target, containerId);
		} else if (/defectCount/.test(className)) {
			var customTextElem = target.closest('.row');
			vanillaGrid.utils.stepDefects.show(customTextElem, containerId);
		} else if (/defectIcons/.test(className)) {
			var customTextElem = target.closest('.row');
			if (/create/.test(className)) {
				vanillaGrid.utils.stepDefects.create(customTextElem, containerId);
			} else{
				vanillaGrid.utils.stepDefects.dialog(customTextElem, rowTarget, containerId);
			}
		} else if (/ajax-input/.test(className)) {
			//detachEvent(document.getElementsByClassName('ajax-input'), 'keydown', fetchDefect.bind(obj), false);
			//attachEvent(document.getElementsByClassName('ajax-input'), 'keydown', fetchDefect.bind(obj), false);
			var obj = {
				rowTarget: rowTarget,
				config: config,
				configRow: config.row,
				containerId: containerId
			}
			var allAjaxInput = document.getElementsByClassName('ajax-input');
			for(var i = 0; i< allAjaxInput.length; i++){
				if(!allAjaxInput[i].dataset.eventlistener){
					allAjaxInput[i].dataset.eventlistener = true;
					attachEvent(allAjaxInput[i], 'keyup', fetchDefect.bind(obj), false);
					attachEvent(allAjaxInput[i], 'keydown', navigateDefects.bind(obj), false);
				}
			}
		} else if (/create-defect/.test(className)) {
			var customTextElem = target.closest('.defects-column');
			vanillaGrid.utils.stepDefects.createDefect(customTextElem, rowTarget, containerId);
		} else if (/removeDefect/.test(className)) {
			var customTextElem = target.closest('.defectsList');
			vanillaGrid.utils.stepDefects.removeDefect(customTextElem, rowTarget, config, target, containerId);
		} else if (/defectId/.test(className)) {
			var customTextElem = target.closest('.defects-column');
			vanillaGrid.utils.stepDefects.attachDefect(customTextElem, rowTarget, config, target && target.innerHTML, containerId);
		} else if (/wiki-renderer-icon/.test(className)) {
			var customTextElem = target.closest('.editable-cell-container');
			vanillaGrid.utils.steps.render(customTextElem, rowTarget, config, target, containerId);
		} else if(target.closest('.custom-text')) {
			var customTextElem = target.closest('.custom-text');
			vanillaGrid.utils.customText.edit(customTextElem, containerId);
		} else if(target.closest('.custom-textarea')) {
			var customTextElem = target.closest('.custom-textarea');
			vanillaGrid.utils.customText.edit(customTextElem, containerId);
		} else if (/eButton/.test(className)) {
			vanillaGrid.utils.stepDefects.eButton(target, containerId);
		} else {
			vanillaGrid.utils.dialogueScrollEvent(target, document.getElementById("moveDropdown"), containerId);
			if (document.getElementById("moveDropdown")) {
				document.getElementById("moveDropdown").parentNode.removeChild(document.getElementById("moveDropdown"));
			}
		}
		if (el && !(/ajax-input/.test(className)) && className && !(target.nodeName.indexOf('LABEL')>-1) && !(target.nodeName.indexOf('OPTION')>-1) && !(target.nodeName.indexOf('SELECT')>-1)) {
			el.classList.add('close');
			el.classList.remove('activeElement');
			vanillaGrid.utils.dialogueScrollEvent(target, el, containerId);
		}
	};

	var fetchDefect = function(ev, rowTarget, configRow, containerId){
		//console.log(ev, rowTarget, configRow, containerId, this)
		vanillaGrid.utils.stepDefects.fetchDefects(ev, this.rowTarget, this.configRow, this.containerId);
	}

	var navigateDefects = function(ev){
		vanillaGrid.utils.stepDefects.navigateDefects(ev, this.rowTarget, this.config, this.containerId);
	}

	var highlightDefect = function(config, containerId, ev){
		vanillaGrid.utils.stepDefects.highlightDefect(ev, this.rowTarget, this.configRow, this.containerId);
	}


	var onMouseEnter = function(config, containerId,ev) {
		ev.stopPropagation();
		ev.stopImmediatePropagation();
		ev.preventDefault();
		var target = ev.target;
		var className = target.className;
		if(/defect-hover/.test(className)) {
			vanillaGrid.utils.defectsMouseAction.defectsMouseEnter(target,containerId);
		}
	}

	var onMouseLeave = function(config, containerId,ev) {
		var target = ev.target;
		var className = target.className;
		var defectPopup = document.getElementById('defects-inlineDialogWrapper');
		setTimeout(function() {
			if(/defect-hover/.test(className)) {
				if(defectPopup && defectPopup.dataset.isDefectPopoverHovered == 'true') {
					return;
				}
				vanillaGrid.utils.defectsMouseAction.defectsMouseLeave(target,containerId);
			}
		}, 200);
	}

	var onGridDragStart = function(config, containerId, ev) {
		var target = getEventTarget(ev);
		var className = target.className;
		if(/draggable-icon/.test(className)) {
			vanillaGrid.utils.drag.dragStart(target, containerId, ev);
		}
	};

	var onGridDragEnter = function(config, containerId, ev) {
		ev.preventDefault();
		var target = getEventTarget(ev);
		vanillaGrid.utils.drag.dragEnter(target, containerId);
	};

	var onGridDragLeave = function(config, containerId, ev) {
		ev.preventDefault();
		var target = getEventTarget(ev);
		vanillaGrid.utils.drag.dragLeave(target, containerId);
	};

	var onGridDragEnd = function(config, containerId, ev) {
		var target = getEventTarget(ev);
		vanillaGrid.utils.drag.dragEnd(target, containerId);
	};

	var onGridDrop = function(config, containerId,ev) {
		ev.preventDefault();
		var target = getEventTarget(ev);
		vanillaGrid.utils.drag.drop(target, containerId);
	};

	var onGridDragOver = function(config, containerId,ev) {
		ev.preventDefault();
		console.log('on drag over');
		var target = getEventTarget(ev);
		vanillaGrid.utils.drag.onDragOver(target, containerId, ev);
	};

	var onGridFocusout = function(config, containerId, ev) {
		var target = ev.target;
		if (!ZEPHYR.GRID.stopGridFocus) {
			var stopBlur = true;
			if (ev.relatedTarget && ev.relatedTarget.className.indexOf('stopBlurEvent') > -1) {
				stopBlur = false;
			}
			if (stopBlur) {
				var rowElem = target.closest('.row');
				if ((target.tagName === 'INPUT') && target.closest('.custom-text')) {
					vanillaGrid.utils.steps.update(rowElem, target.value, null, target, containerId, config);
				} else if ((target.dataset.rowoption == 'true' || /dropDown-container/.test(target.className)) && (!ev.relatedTarget || ev.relatedTarget.dataset.rowoption != 'true')) {
					//checkbox, radio
					vanillaGrid.utils.steps.radioCheckboxUpdate(rowElem, target, containerId, config);
				} else if (target.tagName === 'SELECT') {
					//multiselect
					vanillaGrid.utils.steps.multiSelectUpdate(rowElem, target, containerId, config);
				} else if (target.tagName === 'TEXTAREA') {
					//wiki, multitext
					var clickElementClasses = ev.relatedTarget && ev.relatedTarget.classList || [];
					var isNotWikiElement = true;
					for (var counter = 0; counter < clickElementClasses.length; counter += 1) {
						if (clickElementClasses[counter] == 'wiki-renderer-icon' || clickElementClasses[counter] == 'wiki-icons' || clickElementClasses[counter] == 'stopBlur') {
							isNotWikiElement = false;
							break;
						}
					}
					if (isNotWikiElement == true) {
						vanillaGrid.utils.steps.update(rowElem, target.value, null, target, containerId, config);
					} else {
						target.focus();
					}
					if (!target.closest('.cell-wrapper').dataset.editmode == 'true') {
						rowElem.querySelector('.cell-editMode').classList.add('hide');
						rowElem.querySelector('.cell-readMode').classList.remove('editMode');
						rowElem.querySelector('.cell-readMode').classList.add('readMode');
					}
				}
			}
		} else {
			ZEPHYR.GRID.stopGridFocus = false;
		}
	};

	var onKeyDown = function(config, containerId,event) {
		event.preventDefault();
		var target = event.target;
		var className = target.className;
		if(/drop-down/.test(className)) {
			vanillaGrid.utils.dropdown.keyboardTrigger(target);
		}
	}

	var onVanillaGridUpdate = function(config, containerId , ev) {
		//
		vanillaGrid.utils.onGridUpdate(config, ev.detail);
	};

	vanillaGrid.events = {
		initialize: function(container, config) {
			attachEvent(document.getElementById(container.id).childNodes[0], 'click', onGridClick.bind(this, config, container.id), false);
			attachEvent(document.getElementById(container.id).childNodes[0], 'dblclick', onGridDblClick.bind(this, config, container.id), false);
			attachEvent(document.getElementById(container.id).childNodes[0], 'focusout', onGridFocusout.bind(this, config, container.id), false);
			attachEvent(document.getElementsByClassName('defect-hover'), 'mouseenter', onMouseEnter.bind(this, config, container.id), false);
			attachEvent(document.getElementsByClassName('defect-hover'), 'mouseleave', onMouseLeave.bind(this, config, container.id), false);
			attachEvent(document.getElementById(container.id).querySelectorAll('.status-column'), 'keydown', onKeyDown.bind(this, config, container.id), false);
			attachEvent(document.getElementsByClassName('resultWrapper'), 'mouseover', highlightDefect.bind(this, config, container.id), false);


			if(config.draggableRows) {
				attachEvent(document.getElementById(container.id).childNodes[0], 'dragstart', onGridDragStart.bind(this, config, container.id), false);
				attachEvent(document.getElementById(container.id).childNodes[0], 'dragenter', onGridDragEnter.bind(this, config, container.id), false);
				attachEvent(document.getElementById(container.id).childNodes[0], 'dragleave', onGridDragLeave.bind(this, config, container.id), false);
				attachEvent(document.getElementById(container.id).childNodes[0], 'dragend', onGridDragEnd.bind(this, config, container.id), false);
				attachEvent(document.getElementById(container.id).childNodes[0], 'dragover', onGridDragOver.bind(this, config, container.id), false);
				attachEvent(document.getElementById(container.id).childNodes[0], 'drop', onGridDrop.bind(this, config, container.id), false);
			}

			// document.addEventListener('click', function(ev) {
			// 	var dropdowns = document.querySelectorAll('.dropDown-container');
			// 	for(var i = 0; i < dropdowns.length; i++) {
			// 		dropdowns[i].classList.remove('activeElement');
			// 		dropdowns[i].classList.add('close');
			// 	}
			// 	var attachmentPopup = document.querySelectorAll('.attachments-inlineDialogWrapper');
			// 	for(var i = 0; i < attachmentPopup.length; i++) {
			// 		attachmentPopup[i].classList.remove('activeElement');
			// 		attachmentPopup[i].classList.add('close');
			// 	}
			// }, true);

			//custom-events
			attachEvent(document.getElementById(container.id).childNodes[0], 'onVanillaGridUpdate', onVanillaGridUpdate.bind(this, config, container.id), false);
		}
	};

})();