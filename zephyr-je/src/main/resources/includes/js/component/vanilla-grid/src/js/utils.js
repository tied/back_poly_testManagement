var vanillaGrid = vanillaGrid || {};

(function(undefined) {
	var selectedExecutionIds = {};
	var customEventWrapper = function(eventName, detailObj, isEditMode) {
		var obj = {
			detail: detailObj,
			bubbles: true,
			composed: true
		}
		if(isEditMode) {
			obj.isEditMode = isEditMode === 'true';
		}
		try {
	return new CustomEvent(eventName, obj);
		}
		catch(err) {
			console.error(err);
			var evt = document.createEvent("CustomEvent");
			evt.initCustomEvent(eventName, true, true, detailObj);
			return evt;
		}
	}
	var pinColumns = function(target, className, config, containerId) {

		var configCopy = {};
		configCopy = JSON.parse(JSON.stringify(config));
		var clickedColumn = target.closest('.grid-column').dataset.columnkey;
		if(target.closest(".freezed-grid-header")) {
			//perform unpin
			configCopy.head.forEach(function(column) {
				if(column.key.toString() === clickedColumn) {
					column.isFreeze = false;
				}
			})
		}
		else {
			//perform pin
			configCopy.head.forEach(function(column) {
				if(column.key.toString() === clickedColumn) {
					column.isFreeze = true;
				}
			})
		};

		document.getElementById(containerId).dispatchEvent(customEventWrapper('freezetoggle', { testConfig: configCopy }))
	};

	var sortRows = function(target, className,containerId) {

		var sortOrder = target.dataset.sortorder;
    var sortkey = target.dataset.sortkey;
		var headerColumn = target.parentElement;

		if (sortOrder === '' || sortOrder === 'DESC') {
      sortOrder = 'ASC';
    } else if (sortOrder === 'ASC') {
      sortOrder = 'DESC';
    }

		var obj = {
      sortQuery: { sortkey: sortkey, sortOrder: sortOrder },
      actionName: 'sortGrid',
      customEvent: 'sortGrid'
    };
		document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj))
	};

	var columnChooser = {
		hideColumns: function(columnsToHide) {
			columnsToHide.forEach(function(columnKey) {
				document.querySelector('.' + columnKey + '-column').classList.add('hide');
			});
		},
		toggleColumnChooserPopup: function (target, containerId) {
			var nextSibling = target.parentElement.nextElementSibling;
			var isOpen = nextSibling.classList.contains('open');
			var obj = {
				target: target,
				dialogueElement: nextSibling,
				isOpen: !isOpen,
				onlyWindowScroll : true,
			}
			if(!isOpen) {
				var triggerElement = target.getBoundingClientRect();
	      		var viewportHeight = window.innerHeight;
	      		var height = nextSibling.clientHeight;
	      		var topHeight = triggerElement.top;
	      		nextSibling.classList.remove('close');
	      		nextSibling.classList.add('open');
	      		if (viewportHeight > topHeight + height) {
	      			nextSibling.style.top = topHeight + triggerElement.height + 5 + 'px';
	      			nextSibling.style.left = triggerElement.left - 222 + triggerElement.width + 'px';
	      		}
	      		else {
	        		nextSibling.style.top = topHeight + triggerElement.height - height + 'px';
	        		nextSibling.style.left = triggerElement.left - 222 + 'px';
				}
			} else {
				nextSibling.classList.remove('open');
				nextSibling.classList.add('close');
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj))
		},
		columnChooserSubmit: function(target, config, containerId) {
			var inputNodes = target.parentElement.previousElementSibling.querySelectorAll('input');
			var columnDetails = [];
			for(var i = 0; i< inputNodes.length; i++){
				columnDetails.push({
					key: inputNodes[i].dataset.id,
					displayName: inputNodes[i].getAttribute('content'),
					isVisible: inputNodes[i].checked
				})
			}

			var obj = {
				actionName: 'columnChooser',
				customEvent: 'columnChooser',
				columnDetails: columnDetails
			}
			if(config.isStepsGrid) {
				document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', obj));
			} else {
				document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj));
			}
		},
		columnChooserCancel: function(target, config, containerId) {
			var columnChooserContainer = target.closest('.column-chooser-cont');
			columnChooserContainer.querySelector('.column-chooser-options-container').innerHTML = vanillaGrid.templates.renderColumnChooserOptions(config, containerId);
			this.toggleColumnChooserPopup(columnChooserContainer.previousElementSibling.querySelector('.columnChooser-btn'), containerId);
		}
	};

	var handleActionGridEvents = function(target, config, containerId) {
		var rowDataset = target.closest('.row').dataset;
		var isStepView = rowDataset.stepid ? true : false;
		var rowDetail = isStepView ? rowDataset.stepid : rowDataset.executionid;
		var rowOrderId = isStepView ? parseInt(rowDataset.orderid) : rowDataset.executionid;
		var rowId = isStepView ? rowDataset.rowid : undefined;
		var dataset = target.dataset;
		var objScroll = {
			target: target,
			isOpen: true,
			isActionDropdown: true,
		}
		var obj = {
    		rowDetail: {
					id: rowDetail,
					orderId: rowOrderId,
					rowId : rowId
				},
    		actionName: dataset.action,
    		customEvent: dataset.customevent
  		};
		if(!isStepView && dataset && dataset.action === 'Delete') {
		document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj));
		}
		if (dataset && dataset.customevent === 'moreOptions') {
			if (target.className.indexOf("isOpen") == -1) {
				var list = document.getElementsByClassName('isOpen');
				for (var i = 0; i < list.length; i++) {
					list[i].classList.remove("isOpen");
				}
				target.classList.add("isOpen");
				if (document.getElementById("moveDropdown")) {
					objScroll.dialogueElement = document.getElementById("moveDropdown");
					objScroll.target = target;
					objScroll.isOpen = false;
					document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', objScroll));
					document.getElementById("moveDropdown").parentNode.removeChild(document.getElementById("moveDropdown"));
				}
				objScroll.isOpen = true;
				var triggerElement = target.getBoundingClientRect();
				var viewportHeight = window.innerHeight;
				var height = 52;
				var topHeight = triggerElement.top + triggerElement.height + 2;
				var parentWidth = target.parentElement.clientWidth;
				var top, left;
				if (viewportHeight > topHeight + height) {
					top = topHeight + 'px';
					left = triggerElement.left - parentWidth + triggerElement.width - 76 + 'px';
				}
				else {
					top = topHeight - triggerElement.height - height - 8 + 'px';
					left = triggerElement.left - parentWidth + triggerElement.width - 76 + 'px'
				}
				var prev = config.isPrevEnabled ? 'active' : '';
				var next = config.isNextEnabled ? 'active' : '';
				target.parentNode.parentNode.innerHTML += '<div id="moveDropdown" class="moreOptions-dropdown" style="position:fixed;top:' + top + ';left:' + left + '">'
					+ '<div title="' + config.movePrevPageLabel + '" class="prevPage move-row ' + prev + '">' + config.movePrevPageLabel + '</div>' + '<div title="' + config.moveNextPageLabel + '" class="nextPage move-row ' + next + '">' + config.moveNextPageLabel + '</div>'
					+ '</div>';
			// document.getElementById(containerId).appendChild('<div class="moreOptions-dropdown"> style="top:' + top + ';left:' + left + '"'
			// 	+ '<div class="prevPage">Move To Prev Page</div>' + '<div class="prevPage">Move To Next Page</div>'
			// 	+ '</div>');
				objScroll.dialogueElement = document.getElementById("moveDropdown");
				objScroll.target = document.getElementsByClassName('actions-wrapper moreOptions isOpen')[0];
			} else {
				var objScroll = {
					target: target,
					isOpen: false,
					isActionDropdown: true,
				}
				target.classList.remove("isOpen");
				objScroll.dialogueElement = document.getElementById("moveDropdown");
				if (document.getElementById("moveDropdown")) {
					document.getElementById("moveDropdown").parentNode.removeChild(document.getElementById("moveDropdown"));
				}
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', objScroll));
		}
		if(isStepView) {
			if (dataset.customevent === 'addstep') {
				obj['columnsValues'] = vanillaGrid.utils.steps.stepValues;
			} else if (dataset.customevent === 'clearvalue') {
				var lastRow = config.row[config.row.length - 1];
				config.head.forEach(function(head, index){
					if (head.isInlineEdit) {
						lastRow[head.key] ? lastRow[head.key] = '' : null;
					}
				});
				obj['currentConfig'] = config;
				obj['containerId'] = containerId;
			} else if (dataset.customevent === 'cloneRow') {
				obj['currentConfig'] = config;
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', obj, 'false'));
			vanillaGrid.utils.steps.stepValues = {};
		}
		//document.dispatchEvent(new CustomEvent(dataset.customevent, { bubbles: true, detail: { rowId: dataset.rowid } }))
	};

	var handleMoveRowEvent = function (target, config, containerId) {
		var rowDataset = target.closest('.row').dataset;
		var isStepView = rowDataset.stepid ? true : false;
		var rowDetail = isStepView ? rowDataset.stepid : rowDataset.executionid;
		var rowOrderId = isStepView ? parseInt(rowDataset.rowid) + 1 : rowDataset.executionid
		var obj = {
			id: rowDetail,
			actionName: target.className.indexOf('prevPage') > -1 ? 'moveToPrev' : 'moveToNext',
			customEvent: 'movestep'
		};
		var objScroll = {
			target: target,
			isOpen: false,
			isActionDropdown: true,
			dialogueElement :document.getElementById("moveDropdown"),
		};
		document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', objScroll));
		if (document.getElementById("moveDropdown")) {
			document.getElementById("moveDropdown").parentNode.removeChild(document.getElementById("moveDropdown"));
		}
		document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', obj, 'false'));
		vanillaGrid.utils.steps.stepValues = {};
	}

	var dialogueScrollEvent = function (target, el, containerId) {
		var obj = {
			target: target,
			dialogueElement: el,
			isOpen: false,
		}
		document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj));
	}

	var drag = {
		draggableElementTop: null,
		dragDirection: null,
		draggedStepId: null,
		enteredRowId : null,
		leftRowId : null,
		maxHeight : 0,
		dragStart: function(target, containerId, ev) {
			this.resetValues();
			this.draggedStepId = target.dataset.stepid;
			this.draggableElementTop = parseInt(document.getElementById(containerId).querySelector('.table-container-wrapper').getBoundingClientRect().top, 10);
			this.maxHeight =  AJS.$(ev.currentTarget).find('.table-container').height();
			//this.draggableElementTop = parseInt(target.getBoundingClientRect().top, 10);
		},
		dragEnter: function(target, containerId) {
			if(!this.draggedStepId) {
				return;
			}
			console.log('drag enter');
			var currentEnteredRowId = parseInt(target.dataset.rowid, 10);
			if (!isNaN(currentEnteredRowId)) {
				this.enteredRowId = currentEnteredRowId;
				this.addEmptyRow(currentEnteredRowId, containerId);
			}
		},
		dragLeave: function(target, containerId) {
			if(!this.draggedStepId) {
				return;
			}
			console.log('drag leave');
			var currentLeftRowId = parseInt(target.dataset.rowid, 10);
			if (!isNaN(currentLeftRowId) && this.leftRowId != currentLeftRowId) {
				this.leftRowId = currentLeftRowId
				if(currentLeftRowId === 0) {
					this.addEmptyRow(-1, containerId);
				}
			}
		},
		dragEnd: function(target, containerId) {
			if(!this.draggedStepId) {
				return;
			}
			var allCells = document.getElementById(containerId).querySelectorAll('div.row[data-stepid="'+ this.draggedStepId +'"]');
			var styles = {
				position: 'static',
				top: 'auto',
				//transform: 'translateY(' + top + 'px)',
				zIndex: 'auto',
				backgroundColor: 'inherit',
				left: 'auto',
				pointerEvents: 'auto'
			};
			this.updateCellStyles(allCells, styles);
			this.dragDirection = null;
			this.draggedStepId = null;
			this.dragEnteredRowId = null;
			this.removeEmptyNode(containerId);
			this.resetValues();
		},
		onDragOver: function(target, containerId, ev) {
			if(!this.draggedStepId || (parseInt(target.dataset.rowid, 10) == this.dragEnteredRowId)) {
				return;
			}
			this.dragEnteredRowId = parseInt(target.dataset.rowid, 10);
			var containerScrollTop = document.getElementById(containerId).querySelector('.table-container-wrapper').scrollTop;
			var top = ev.clientY - this.draggableElementTop - 65 + containerScrollTop;
			if (top > (this.maxHeight - 100)) { //excluding height of new test step
				top = this.maxHeight - 100;
			}
			if(top && !isNaN(top)) {
				var allCells = document.getElementById(containerId).querySelectorAll('div.row[data-stepid="'+ this.draggedStepId +'"]');
				var styles = {
					position: 'absolute',
					top: top + 'px',
					//transform: 'translateY(' + top + 'px)',
					zIndex: '1',
					backgroundColor: 'white',
					left: 'auto',
					pointerEvents: 'none'
				};
				this.updateCellStyles(allCells, styles);
			}
		},
		drop: function(target, containerId, ev) {
			if(!this.draggedStepId) {
				return;
			}
			var nextElement = target.nextElementSibling;
			if(!nextElement) {
				nextElement = target.previousElementSibling;
			}
			var droppedStepId = nextElement && nextElement.dataset.stepid;
			if(droppedStepId == '-1') {
				droppedStepId = 'First';
			}
			if(!droppedStepId) {
				return;
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', { customEvent: 'movestep', id: this.draggedStepId, position: droppedStepId }, 'false'));
			this.resetValues();
		},
		addEmptyRow: function(rowIndex, containerId) {
			console.log('addemptyrow');
			if(isNaN(rowIndex)) {
				return;
			}
			this.removeEmptyNode(containerId);
			var allColumns = document.getElementById(containerId).querySelectorAll('.row-column');
			for(var i = 0; i < allColumns.length; i++) {
				var rowColumn = allColumns[i];
				var emptyNode = document.createElement('div');
				emptyNode.className = 'empty-node';
				var index = rowIndex + 1;
				if(!rowColumn.children[index]) {
					return;
				}
				rowColumn.insertBefore(emptyNode, rowColumn.children[index]);
			}
		},
		removeEmptyNode: function(containerId) {
			var allEmptyNodes = document.getElementById(containerId).querySelectorAll('.empty-node');
			for(var i = 0; i < allEmptyNodes.length; i++) {
				allEmptyNodes[i].parentElement.removeChild(allEmptyNodes[i]);
			}
		},
		updateCellStyles: function(allCells, styles) {
			for(var i = 0; i < allCells.length; i++) {
				if (allCells[i].firstChild && allCells[i].firstChild.className &&  allCells[i].firstChild.className.indexOf('action-outer-wrapper') > -1) {
					styles.backgroundColor = 'inherit';
				}
				for(var key in styles) {
					allCells[i].style[key] = styles[key];
				}
			}
		},
		resetValues: function() {
			this.leftRowId = null;
			this.enteredRowId = null;
			this.maxHeight = 0;
		}
	};

	var dropdown = {
		triggerDropdown: function(target, containerId) {
			var nextSibling = target.nextSibling;
			var className = nextSibling.className;

			var triggerElement = target.getBoundingClientRect();
			var viewportHeight = window.innerHeight;
			//var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
			var height = 100;
			var topHeight = triggerElement.top + triggerElement.height + 2;
			var parentWidth = target.parentElement.clientWidth;
			if (viewportHeight > topHeight + height) {
				nextSibling.style.top = topHeight + 'px';
				nextSibling.style.width = parentWidth + 'px';
				nextSibling.style.left = triggerElement.left - parentWidth + triggerElement.width + 'px';
			}
			else {
				nextSibling.style.top = topHeight - triggerElement.height - height - 8 + 'px';
				nextSibling.style.width = parentWidth + 'px';
				nextSibling.style.left = triggerElement.left - parentWidth + triggerElement.width + 'px'
			}
			var input = nextSibling.getElementsByTagName('input');
			var select = nextSibling.getElementsByTagName('option');
			var checkedValues = {};
			var isInput = true;
			if(input.length) {
				for(var i=0; i < input.length; i++) {
					checkedValues[input[i].id] = input[i].checked;
				}
			}
			if(select.length) {
				isInput = false;
				for (var i = 0; i < select.length; i++) {
					checkedValues[i] = select[i].selected;
				}
			}
			var obj = {
				target: target,
				dialogueElement: nextSibling,
				isOpen: true,
				isDropDown : true,
				checkedValues: checkedValues,
				isInput: isInput,
				isSelect: !isInput,
			}
			if(className.indexOf('close') > -1) {
				obj.isOpen = true;
				nextSibling.classList.remove('close');
				nextSibling.classList.add('activeElement');
			} else {
				obj.isOpen = false;
				nextSibling.classList.add('close');
				nextSibling.classList.remove('activeElement');
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj))
		},
		optionClick: function(target, row, containerId, config) {
			steps.singleSelectUpdate(row, target, containerId, config);
		},

		selectOptionClick: function(target,config,  containerId) {
			var obj = {
				status: {
					selectedOptions: target.dataset.content,
					value: target.dataset.value
				}
			}

			var executionId = target.closest('.row').dataset.executionid,
					rowId = target.closest('.row').dataset.rowid,
					stepId = target.closest('.row').dataset.stepid;
			document.getElementById(containerId).dispatchEvent(customEventWrapper('gridValueUpdated', { type: 'SINGLE_SELECT', updatedValue: obj, isObject: true, rowId: rowId, stepId: stepId, executionId: executionId, config: config }));
		},
		removeData: function(target, config, containerId) {
			var dataset = target.closest('.row').dataset;
			var updatedValue = {};
			updatedValue[dataset.columnid] = {
				value: '',
				selectedOptions: ''
			};
			if(target.closest('.cell-wrapper').dataset.editmode == 'true') {
				steps.stepUIRender(containerId, target.closest('.row'), config, '');
				return;
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridValueUpdated', { containerId: containerId, isObject: true, key: dataset.columnid, rowId: parseInt(dataset.rowid, 10), stepId: parseInt(dataset.stepid, 10), updatedValue: updatedValue, config: config }));
		},
		keyboardTrigger: function(target) {
			var currentIndex = target.currentIndex || 0;
			var list = target.querySelectorAll('li');

			if (event.keyCode == 38 || event.keyCode == 40 || event.keyCode == 13) {
				if(event.keyCode == 40 && (+currentIndex !== list.length)) {
					if(currentIndex) {
						list[+currentIndex - 1].classList.remove('hover-class');
						target.querySelector("ul").scrollTop += 23;
					}
					list[currentIndex].classList.add('hover-class');
					target.currentIndex = currentIndex + 1;
				}
				else if(event.keyCode == 38 && currentIndex !==0) {
						if(!currentIndex !==list.length){
							target.querySelector("ul").scrollTop -= 23;
						}
						list[+currentIndex - 2].classList.add('hover-class');
						list[currentIndex - 1].classList.remove('hover-class');
						target.currentIndex = currentIndex - 1;
						return;
				}
				else if(event.keyCode == 13) {
					var enterTarget = list[currentIndex - 1];
					vanillaGrid.utils.dropdown.selectOptionClick(enterTarget);
					this.currentIndex = 0;
				}
			}
		}
	};

	var defectsMouseAction = {
		obj : {},
		defectsMouseEnter: function(target, containerId) {
			obj = {
				id: target.parentElement.dataset.executionid,
      			defectsPopup: true,
      			targetEle: target
			};
		document.getElementById(containerId).dispatchEvent(customEventWrapper('defecthover', obj));
		},
		defectsMouseLeave : function (target, containerId) {
			obj = {
	      		id: target.parentElement.dataset.executionid,
	      		defectsPopup: false,
	      		targetEle: target
	    	};
		document.getElementById(containerId).dispatchEvent(customEventWrapper('defecthoverOff', obj));
		}
	}

	var customText = {
		edit: function(customTextElem, containerId) {
			var cellEditMode = customTextElem.querySelector('.cell-editMode');
			var cellReadMode = customTextElem.querySelector('.cell-readMode');
			var classList = customTextElem.classList;
			var multiline = false;

			if (/textarea-custom-field/.test(classList)) {
				multiline = true;
			}

			if(/editMode/.test(cellReadMode.classList)) {
				return;
			}

			if (multiline) {
				cellEditMode.querySelector('textarea').style.height = cellReadMode.clientHeight + 'px';
			}

			cellEditMode.classList.remove('hide');
			cellEditMode.classList.add('currentEditingField');
			cellReadMode.classList.remove('readMode');
			cellReadMode.classList.add('editMode');

			if (cellEditMode && cellEditMode.parentElement && cellEditMode.parentElement.childNodes[1]) {
				cellEditMode.parentElement.childNodes[1].style.display = 'flex';
			}
			// cellEditMode.parentElement.childNodes[1].style.display = 'flex';
			vanillaGrid.templates.adjustRowHeightCell(containerId, customTextElem.parentElement.dataset.rowid);
			setTimeout(function() {
				cellEditMode.children[0].focus();
				var val = cellEditMode.children[0].value;
				cellEditMode.children[0].value = '';
				cellEditMode.children[0].value = val;
				var numberOfLines = val.split("\n");
				if (AJS.$('.currentEditingField .editing-field-text-area') && AJS.$('.currentEditingField .editing-field-text-area').length != 0) {
					AJS.$('.currentEditingField .editing-field-text-area').scrollTop(numberOfLines.length * 20);
				}
			}, 100);
		}
	};

	var steps = {
		stepValues: {},
		closeDropdownContainer: function(target) {
			target.closest('.dropDown-container').classList.add('close');
		},

		render: function(customTextElem, rowTarget, config, target, containerId){
			var dummyElSelector = customTextElem.querySelector('#dummy');
			var selector = customTextElem.querySelector('textarea');
			if (dummyElSelector) {
				dummyElSelector.parentNode.removeChild(dummyElSelector);
				selector.classList.remove("hide");
				selector.focus();
				return;
			}
			var value = selector.value;
			var data = {
				issueKey: "",
				rendererType: "zephyr-wiki-renderer",
				unrenderedMarkup: value
	        };
			document.getElementById(containerId).dispatchEvent(customEventWrapper('renderPreview', { data: data, customTextElem: customTextElem, selector: selector, rowId: customTextElem.parentElement.parentElement.dataset.rowid, containerId: containerId }));
		},

		update: function(row, value, selectedOptions, target, containerId, config) {
			var dataset = row.dataset;
			var updatedValue = {};
			var rowId = dataset.rowid,
					key = dataset.columnid;
			updatedValue[dataset.columnid] = {
				value: value,
				selectedOptions: selectedOptions
			};
			//var cellWrapper = target.closest('.cell-wrapper');
			var cellWrapper = row.querySelector('.cell-wrapper');
			if(cellWrapper && cellWrapper.dataset.editmode == 'true') {
				this.saveStepValues(row, value, selectedOptions);
			} else {
				if(dataset.fieldvalue == value) {
					var cellEditMode = cellWrapper.querySelector('.cell-editMode');
					var cellReadMode = cellWrapper.querySelector('.cell-readMode');
					cellEditMode.classList.add('hide');
					cellEditMode.classList.remove('currentEditingField');
					cellReadMode.classList.add('readMode');
					cellReadMode.classList.remove('editMode');
					cellEditMode.parentElement.childNodes[1].style.display = 'none';
					vanillaGrid.templates.adjustRowHeightCell(containerId, rowId);
					return;
				}
				document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridValueUpdated', { containerId: containerId, key: key,  rowId: rowId, config: config, isObject: true, stepId: parseInt(dataset.stepid, 10), updatedValue: updatedValue }));
			}
		},
		triggerDateDropdown: function(target, ev, containerId, config) {

			var dateType = /date-time-dropdown/.test(target.className) ? 'DATE_TIME' : 'DATE';
			var row = target.closest('.row');
			var rowId = row.dataset.rowid,
					key = row.dataset.columnid;
			var cellWrapperDataset = target.closest('.cell-wrapper').dataset;
			var isEditMode = (cellWrapperDataset.editmode && cellWrapperDataset.editmode == 'true') ? true : false;
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', { containerId: containerId, key: key,  rowId: rowId, config: config, isEditMode: isEditMode, columnsValues: '', type: dateType, 'onlyUpdateValue': false, 'event': ev, 'value': this.value, cellKey: parseInt(row.dataset.columnid, 10), stepId: parseInt(row.dataset.stepid, 10), target: target }, 'false'));
		},
		radioCheckboxUpdate: function(row, target, containerId, config) {
			var allInputs = target.closest('.dropDown-container').querySelectorAll('input');
			var selectedOptions = [];
			var value = [];
			for(var i = 0; i < allInputs.length; i++) {
				var input = allInputs[i];
				if(input.checked) {
					value.push(input.value);
					selectedOptions.push(input.nextSibling.textContent);
				}
			}
			this.stepUIRender(containerId, row, config, value);
			this.update(row, value.toString(), selectedOptions.toString(), target, containerId, config);
			this.closeDropdownContainer(target);
		},
		stepUIRender: function(containerId, row, config, value) {
			if(row.dataset.stepid && row.dataset.stepid == '-1') {
				var cellConfig = {};
				var columnkey = row.parentElement.dataset.columnkey;
				cellConfig.header = config.head.filter(function(header){
	    		return header.key.toString() === columnkey
	  		})[0];
	  		cellConfig.row = config.row.filter(function(step){
	  			return step.id === -1
	  		})[0];
	  		cellConfig.row[columnkey] = value.toString();
				vanillaGrid.templates.partialRender(containerId, row.dataset.rowid, cellConfig, config)
			}
		},
		singleSelectUpdate: function(row, target, containerId, config) {
			var value = target.dataset.value;
			var selectedOption = target.textContent;
			this.stepUIRender(containerId, row, config, value);
			this.update(row, value, selectedOption, target, containerId, config);
			this.closeDropdownContainer(target);
		},
		multiSelectUpdate: function(row, target, containerId, config) {
			var value = [];
			var selectedOptions = [];
			for (var i = 0; i < target.options.length; i++) {
				if(target[i].selected){
				var option = target[i];
				value.push(option.value);
				selectedOptions.push(option.textContent);
				}
			}
			this.stepUIRender(containerId, row, config, value);
			this.update(row, value.toString(), selectedOptions.toString(), target, containerId, config);
			this.closeDropdownContainer(target);
		},
		saveStepValues: function(row, value, selectedOptions) {
			var dataset = row.dataset;
			var rowId = dataset.stepid;
			var cellKey = dataset.columnid;
			this.saveStepFieldValues(rowId, cellKey, value, selectedOptions);
		},
		saveStepFieldValues: function(rowId, cellKey, value, selectedOptions) {
			if (this.stepValues[rowId]) {
	        	this.stepValues[rowId][cellKey] = {
	          		selectedOptions: selectedOptions,
	          		value: value
	        	};
	      	} else {
	        	this.stepValues[rowId] = {};
	        	this.stepValues[rowId][cellKey] = {
	          		selectedOptions: selectedOptions,
	          		value: value
	        	};
	      	}
		},
		freezeColumns: function(target, containerId) {
			var actionName = target.closest('.bulkAction-container').dataset.eventname;
			var obj = {
				actionName: actionName,
				columnsValues: {},
				customEvent: null
			};
			document.getElementById(containerId).dispatchEvent(customEventWrapper('gridBulkActions', obj, 'false'));
		},
		focusSteps: function(target, containerId) {
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', {actionName: 'scrollDown', customEvent: 'scrollDown', containerId: containerId}, 'false'));
		},
		popupTestSteps: function(target, containerId) {
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', {actionName: 'largeview', customEvent: 'largeview'}, 'false'));
		}
	};

	var onGridUpdate = function(config, data) {
		//
		console.log('ongridupdate');
	};

	var addTests = function(target, containerId) {
		var obj = {
      actionName: target.parentElement.dataset.customevent
    };
    document.getElementById(containerId).dispatchEvent(customEventWrapper('addTests', obj));
	}

	var attachments = {
		add: function(target, containerId) {
			var row = target.closest('.row');
			document.getElementById(containerId).dispatchEvent(customEventWrapper('stepGridComponentActions', { actionName: 'addAttachment', customEvent: 'addAttachment', rowDetail: parseInt(row.dataset.stepid, 10) }, 'false'));
		},
		lastFetchedAttachments: null,
		fetchAttachments: function(target, containerId) {
			//
			var id = target.closest('.row').dataset.stepid;
			document.getElementById(containerId).dispatchEvent(customEventWrapper('fetchattachment', { id: id, fetchAttachmentsCb: this.fetchAttachmentsCb.bind(this, target, containerId) }));
		},
		fetchAttachmentsCb: function (target, containerId, attachments) {
			if(attachments && attachments.length) {
				this.lastFetchedAttachments = attachments;
				var attachmentThumbStr = '';
				attachments.forEach(function(attachment) {
					attachmentThumbStr += vanillaGrid.templates.getAttachmentThumbnails(attachment, true);
				});
				target.nextElementSibling.innerHTML = attachmentThumbStr;
				this.showThumbNails(target, containerId);
			}
		},
		showThumbNails: function (target, containerId) {
			var attachmentsInlineDialogWrapper = target.nextElementSibling;
			var classList = attachmentsInlineDialogWrapper.classList;
			var obj = {
				target: target,
				dialogueElement: attachmentsInlineDialogWrapper,
				isAttachment : true
			}
			if(/close/.test(classList)) {
				obj.isOpen = true;
				classList.remove('close');
				classList.add('activeElement');
				var triggerElement = target.getBoundingClientRect();
	            var viewportHeight = window.innerHeight;
				var viewportWidth = window.innerWidth;
	            var height = attachmentsInlineDialogWrapper.clientHeight;
				var width = attachmentsInlineDialogWrapper.clientWidth;
	            var topHeight = triggerElement.top + triggerElement.height + 2;
				var leftWidth = triggerElement.left + triggerElement.width + 2;
	            if (viewportHeight > topHeight + height) {
	            	attachmentsInlineDialogWrapper.style.top = topHeight + 'px';
	            }
	            else {
	            	attachmentsInlineDialogWrapper.style.top = topHeight - triggerElement.height - 5 - height + 'px';
	            }
				if (viewportWidth > leftWidth + width) {
	            	attachmentsInlineDialogWrapper.style.left = triggerElement.left + 'px';
	            }
	            else {
	            	attachmentsInlineDialogWrapper.style.left = leftWidth - 5 - width + 'px';
	            }
	            //triggerElement.focus();
			} else {
				obj.isOpen = false;
				classList.add('close');
				classList.remove('activeElement');
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj))
		},
		iconPreview: function(target, config, containerId) {
			var selectedAttachment = target.closest('.zephyr-attachment-thumb').dataset.attachmentid;
			var rowDataset = target.closest('.row').dataset;
			var stepId = rowDataset.stepid;
			var stepObj = config.row.filter(function(rowObj) {
				if(parseInt(rowObj.id, 10) === parseInt(stepId, 10)) {
					return true;
				}
				return false;
			});
			var attachmentKey = rowDataset.columnid;
			var attachmentArray = stepObj[0] && stepObj[0][attachmentKey] || [];

			if(target.closest('.fetch-attachments-preview')) {
				attachmentArray = this.lastFetchedAttachments;
			}
			target.closest('.attachments-inlineDialogWrapper').classList.add('close');
			document.getElementById(containerId).dispatchEvent(new CustomEvent('stepGridComponentActions', { isEditMode: false, detail: { actionName: 'viewImage', customEvent: 'viewImage', attachmentArray: attachmentArray, selectedAttachment: selectedAttachment }, bubbles: true, composed: true }));
		},
		iconDelete: function(target,containerId) {
			var attachment = target.closest('.zephyr-attachment-thumb').dataset;
			var rowDetail = target.closest('.row').dataset.stepid + ':' + attachment.attachmentid + ':' + attachment.attachmentname;
			document.getElementById(containerId).dispatchEvent(new CustomEvent('stepGridComponentActions', { isEditMode: false, detail: { actionName: 'deleteAttachment', customEvent: 'deleteAttachment', rowDetail: rowDetail }, bubbles: true, composed: true }));
		}
	};

	var bulkActions = function(target, config, containerId) {
		selectedExecutionIds[containerId] = selectedExecutionIds[containerId] || [];
		var currentExecutionList = config.row.map(function(execution){
			return execution.id;
		})
		var selectAll = target.parentElement.classList.contains('selectRows');
		var bulkDelete = target.parentElement.classList.contains('bulkDelete')
		var isAllSelected;
		if(selectAll) {
			isAllSelected = target.parentElement.classList.contains('allSelected');
			if(isAllSelected){
				selectedExecutionIds[containerId] = selectedExecutionIds[containerId].filter(function(executionId) {
  				return !currentExecutionList.includes(executionId);
				});
			} else {
				selectedExecutionIds[containerId] = selectedExecutionIds[containerId].concat(currentExecutionList);
			}
			vanillaGrid.templates.selectExecutions(target, config, containerId, isAllSelected);
			if(containerId === 'moveExecutionGrid') {
				var rowData = config.row.map(function(execution){
					return {
						rowId: execution.id,
						selected: selectedExecutionIds[containerId].indexOf(execution.id) > -1
					}
				});
				var obj = {
					actionName: "rowSelection",
					customEvent: "selectRows",
					rowsSelection: selectedExecutionIds[containerId],
					rowData: rowData
				}
	document.getElementById(containerId).dispatchEvent(customEventWrapper('gridBulkActions', obj));
			}
		}
		if(bulkDelete) {
			var obj = {
        //columnsValues: this.saveRowValues,
        rowsSelection: selectedExecutionIds[containerId],
        //rowData: this.get('rowsSelection'),
        actionName: target.parentElement.dataset.eventname,
        //customEvent: element.dataCustomevent
      };

      document.getElementById(containerId).dispatchEvent(customEventWrapper('gridBulkActions', obj));
		}


	}

	var maintainExecutionsList = function (target, config, containerId) {
		selectedExecutionIds[containerId] = selectedExecutionIds[containerId] || [];
		if(target.checked) {
			selectedExecutionIds[containerId].push(parseInt(target.value));
		} else {
			selectedExecutionIds[containerId].splice(selectedExecutionIds[containerId].indexOf(parseInt(target.value)), 1);
		}
		vanillaGrid.templates.maintainAllSelect(target, config, containerId);
		if(containerId === 'moveExecutionGrid') {
				var rowData = config.row.map(function(execution){
					return {
						rowId: execution.id,
						selected: selectedExecutionIds[containerId].indexOf(execution.id) > -1
					}
				});
				var obj = {
					actionName: "rowSelection",
					customEvent: "selectRows",
					rowsSelection: selectedExecutionIds[containerId],
					rowData: rowData
				}
	document.getElementById(containerId).dispatchEvent(customEventWrapper('gridBulkActions', obj));
			}
	}

	var selectRows = function(target, config, containerId) {
		vanillaGrid.templates.selectRow( target.dataset.rowid, containerId, target.dataset.executionid);
    var rowId = target.dataset.executionid;
    document.getElementById(containerId).dispatchEvent(customEventWrapper('gridRowSelected', { rowId: rowId }));
	}

	var selectRowDbl = function(target, config, containerId) {
      var rowId = target.dataset.executionid;
      if(!rowId)
      	return;
      document.getElementById(containerId).dispatchEvent(customEventWrapper('gridRowSelected', { rowId: rowId, dblClick: true }));
	}

	var stepDefects = {
		calcPosition: function (target, cellEditMode, selector, leftAdjust, option, containerId) {
			var nextSibling = target.querySelector(selector);
			if(nextSibling) {
				var className = nextSibling.className;
				var obj = {
					target: target.querySelector('.defect-click') || target,
					dialogueElement: nextSibling,
					isOpen : true,
					isDefects : true,
					leftAdjust: leftAdjust,
				}
				if(!option){
					if(className.indexOf('close') > -1) {
						nextSibling.classList.remove('close');
						nextSibling.classList.add('activeElement');
					} else {
						nextSibling.classList.add('close');
						nextSibling.classList.remove('activeElement');
						obj.isOpen = false;
						document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj))
						return;
					}
				}
				var triggerElement = cellEditMode.getBoundingClientRect();
				var viewportHeight = window.innerHeight;
				//var height = this.shadowRoot.querySelector('.dropDown-container').clientHeight;
				var height = nextSibling.getBoundingClientRect().height;
				var topHeight = triggerElement.top + triggerElement.height + 2;
				var parentWidth = cellEditMode.parentElement.clientWidth;
				if (viewportHeight > topHeight + height) {
					nextSibling.style.top = topHeight + 'px';
					nextSibling.style.width = parentWidth + 'px';
					nextSibling.style.left = triggerElement.left - parentWidth + triggerElement.width - leftAdjust + 'px';
				}
				else {
					nextSibling.style.top = topHeight - triggerElement.height - height - 8 + 'px';
					nextSibling.style.width = parentWidth + 'px';
					nextSibling.style.left = triggerElement.left - parentWidth + triggerElement.width - leftAdjust + 'px'
				}
				document.getElementById(containerId).dispatchEvent(customEventWrapper('dialogueScroll', obj));
			}
		},
		show: function(customTextElem, containerId){
			var cellEditMode = customTextElem.querySelector('.defect-click');
			var leftAdjust = 260;
			this.calcPosition(customTextElem, cellEditMode, '.step-defect-list', leftAdjust, false ,containerId);
		},

		create: function(customTextElem, containerId){
			var cellEditMode = customTextElem.querySelector('.defect-click');
			var leftAdjust = 0;
			this.calcPosition(customTextElem, cellEditMode, '.defectsSearchBox', leftAdjust, false ,containerId);
		},

		dialog: function(customTextElem, rowTarget, containerId){
			var rowId = parseInt(rowTarget.dataset.executionid);
			var obj = {
	          rowDetail: rowId,
	          actionName: 'defectPicker',
	          customEvent: 'defectPicker'
	        };
		document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj));
		},

		currentIndex: -1,

		fetchDefects: function(ev, rowTarget, configRow, containerId){
			if (([38,40,13].indexOf(ev.keyCode) > -1)) return;
			var rowId = parseInt(rowTarget.dataset.executionid);
			for (var i = 0; i < configRow.length; i++) {
				if(configRow[i].id === rowId){
					var defects = configRow[i].defects;
				}
			}
			var value = ev.target.value;
			document.getElementById(containerId).dispatchEvent(customEventWrapper('fetchdefect', { id: rowId, defectsPopup: true, query: value, defects: defects }));
		},

		navigateDefects: function(ev, rowTarget, config, containerId) {
			var optionsWrapper = ev.target.closest('.input-wrapper').nextSibling.children;
			if(!optionsWrapper.length || optionsWrapper[0].className !== 'inputResultWrapper') return;

			var options = optionsWrapper[0].querySelectorAll('.defectList-wrapper');
			var defectsContainer = optionsWrapper[0].querySelector('.defectList-container');
			if (ev.keyCode == 38 || ev.keyCode == 40 || ev.keyCode == 13) {
        var newIndex = this.currentIndex;

        if (ev.keyCode == 38) {
          newIndex = Math.max(this.currentIndex - 1, 0);
          if (options[newIndex].classList.contains('selected')) {
            newIndex = Math.max(this.currentIndex - 2, 0);
          }
        	defectsContainer.scrollTop -= 20;
        } else if (ev.keyCode == 40) {
          newIndex = Math.min(this.currentIndex + 1, options.length - 1);
          if (options[newIndex].classList.contains('selected')) {
            newIndex = Math.min(this.currentIndex + 2, options.length - 1);
          }
          if (newIndex != 0) {
        		defectsContainer.scrollTop += 20; //Height of li element
          }
        } else if (ev.keyCode == 13) {
        	if(this.currentIndex > -1) {
        		this.attachDefect(rowTarget.closest('.row-column'), rowTarget, config, options[this.currentIndex].querySelector('.defectId').innerHTML, containerId)
        	} else if (ev.target.value && ev.target.value.indexOf('-') > -1 && !isNaN(ev.target.value.split('-')[1])){
        		var issueKey = ev.target.value.toUpperCase();
        		this.attachDefect(rowTarget.closest('.row-column'), rowTarget, config, issueKey, containerId)
        	}

          this.currentIndex = -1;
        }
        for(var j=0; j<options.length; j++) {
        	options[j].classList.remove('selected');
        }
        this.currentIndex = newIndex;
        options[this.currentIndex] && options[this.currentIndex].classList.add('selected');
        ev.preventDefault();

      }
	    //console.log('currentIndex', this.currentIndex);
		},

		highlightDefect: function(ev, rowTarget, configRow, containerId) {

			//console.log('targeted option', ev.target);
			var options = ev.target.closest('.inputResultWrapper').querySelectorAll('.defectList-wrapper')
			var defectEntry = ev.target.closest('.defectList-wrapper');
			if(!defectEntry) return;
			this.currentIndex = defectEntry.dataset.index;
			for(var j=0; j<options.length; j++) {
      	options[j].classList.remove('selected');
      }
      options[this.currentIndex].classList.add('selected');
      ev.preventDefault();

		},

		createDefect: function(customTextElem, rowTarget, containerId){
			var rowId = parseInt(rowTarget.dataset.executionid);
			var obj = {
	          rowDetail: rowId,
	          actionName: 'createDefect',
	          customEvent: 'createDefect'
	        };
	        document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj));
		},

		eButton: function(target, containerId){

			var rowDataset = target.closest('.row').dataset;
			var isStepView = rowDataset.stepid ? true : false;
			var rowDetail = isStepView ? rowDataset.stepid : rowDataset.executionid;
			var rowOrderId = isStepView ? parseInt(rowDataset.rowid) + 1 : rowDataset.executionid
			var dataset = target.dataset;
			var obj = {
				rowDetail: {
					id: rowDetail,
					orderId: rowOrderId
				},
				actionName: dataset.action,
				customEvent: dataset.customevent,
				target: target,
			};
			document.getElementById(containerId).dispatchEvent(customEventWrapper('gridActions', obj));
		},

		removeDefect: function(customTextElem, rowTarget, config, target, containerId){
			var rowId = parseInt(rowTarget.dataset.executionid);
			var key = customTextElem.dataset.key;
			var updateDefectArr = [];
			var wasAlreadyAdded;
			for (var i = 0; i < config.row.length; i++) {
				if(config.row[i].id === rowId){
					var defects = config.row[i].defects;
					break;
				}
			}
			for (var i = 0; i < defects.length; i++) {
				if (defects[i].key != key) {
					updateDefectArr.push(defects[i].key);
				}
			}
			var obj = {
	          defectList: updateDefectArr
	        };
	        document.getElementById(containerId).dispatchEvent(customEventWrapper('gridValueUpdated', { wasAlreadyAdded: wasAlreadyAdded, updatedValue: obj, stepId: rowId, testConfig: config }));
		},

		attachDefect: function(customTextElem, rowTarget, config, issueKey, containerId){
			var rowId = parseInt(rowTarget.dataset.executionid);
			var updateDefectArr = [];
			for (var i = 0; i < config.row.length; i++) {
				if(config.row[i].id === rowId){
					var defects = config.row[i].defects;
					break;
				}
			}
			for (var i = 0; i < defects.length; i++) {
				updateDefectArr.push(defects[i].key);
			}
			updateDefectArr.push(issueKey);
			var obj = {
				updateDefectList : true,
				defectList: updateDefectArr
			}
			document.getElementById(containerId).dispatchEvent(customEventWrapper('gridValueUpdated', { wasAlreadyAdded: false, updatedValue: obj, stepId: rowId, testConfig: config }));
		}
	};

	var testExecution = {
		freezeColumns: function(target, containerId) {
			var actionName = target.closest('.bulkAction-container').dataset.eventname;
			var obj = {
				actionName: actionName,
				columnsValues: {},
				customEvent: null
			};
			document.getElementById(containerId).dispatchEvent(customEventWrapper('executiongridBulkActions', obj, 'false'));
		},
	};

	vanillaGrid.utils = {
		pinColumns: pinColumns,
		sortRows: sortRows,
		columnChooser: columnChooser,
		drag: drag,
		dropdown: dropdown,
		defectsMouseAction: defectsMouseAction,
		customText: customText,
		steps: steps,
		onGridUpdate: onGridUpdate,
		handleActionGridEvents: handleActionGridEvents,
		addTests: addTests,
		attachments: attachments,
		bulkActions: bulkActions,
		selectRows: selectRows,
		selectRowDbl: selectRowDbl,
		stepDefects: stepDefects,
		maintainExecutionsList: maintainExecutionsList,
		selectedExecutionIds: selectedExecutionIds,
		testExecution: testExecution,
		handleMoveRowEvent: handleMoveRowEvent,
		dialogueScrollEvent: dialogueScrollEvent,
		customEventWrapper : customEventWrapper
	};

})();
