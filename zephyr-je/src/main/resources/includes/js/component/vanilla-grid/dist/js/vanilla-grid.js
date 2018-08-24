var vanillaGrid = vanillaGrid || {};

(function(undefined) {
	
	var attachEvent = function(elem, eventType, cb, isCapture) {
		if(!isCapture) {
			isCapture = false;
		}
		elem.addEventListener(eventType, cb, isCapture);
	};

	var detachEvent = function(elem, eventType) {
		elem.removeEventListener(eventType);
	};

	var onGridClick = function(ev) {
		var target = ev.target;
		var className = target.className;
		if(/pin-icon/.test(className)) {
			vanillaGrid.utils.pinColumns(target, className); 
		}
		if(/sort-icon/.test(className)) {
			vanillaGrid.utils.sortRows(target, className); 
		}
		if(/action-icon/.test(className)) {
			vanillaGrid.utils.handleActionGridEvents(target, className); 
		}
	};

	var onGridDragStart = function(ev) {
		var className = ev.target.className;
		if(/draggable-icon/.test(className)) {
			vanillaGrid.utils.drag.dragStart(ev);
		}
	};

	var onGridDragEnter = function(ev) {
		var className = ev.target.className;
		vanillaGrid.utils.drag.dragEnter(ev);
	};

	var onGridDragLeave = function(ev) {
		vanillaGrid.utils.drag.dragLeave(ev);
	};

	var onGridDragEnd = function(ev) {
		vanillaGrid.utils.drag.dragEnd(ev);
	};

	var onGridDrop = function(ev) {
		var className = ev.target.className;
		vanillaGrid.utils.drag.drop(ev);
	};

	vanillaGrid.events = {
		initialize: function(config) {
			attachEvent(document.getElementById('gridWrapper'), 'click', onGridClick, false);
			if(config.draggableRows) {
				attachEvent(document.getElementById('gridWrapper'), 'click', onGridClick, false);
				attachEvent(document.getElementById('gridWrapper'), 'dragstart', onGridDragStart, false);
				attachEvent(document.getElementById('gridWrapper'), 'dragenter', onGridDragEnter, false);
				attachEvent(document.getElementById('gridWrapper'), 'dragleave', onGridDragLeave, false);
				attachEvent(document.getElementById('gridWrapper'), 'dragend', onGridDragEnd, false);
				attachEvent(document.getElementById('gridWrapper'), 'drop', onGridDrop, false);
			}
		}
	};

})();
var vanillaGrid = vanillaGrid || {};

(function() {

	vanillaGrid.init = function(container, config) {
		vanillaGrid.templates.initialize(container, config); 
		vanillaGrid.events.initialize(config);
	}

})();
var vanillaGrid = vanillaGrid || {};

(function(undefined) {

	var renderGrid = function(config) {
		
		var rows = config.row;

		var isDraggable = config.draggableRows;

		var draggableGridBody = '<div id="draggableGridBody" class="draggable-grid-body grid-body"><div class="grid-column row-column">';

		var freezedGridHeader = '<div id="freezedGridHeader" class="freezed-grid-header grid-header">';
		var freezedGridBody = '<div id="freezedGridBody" class="freezed-grid-body grid-body">';
		
		var unfreezedGridHeader = '<div id="unfreezedGridHeader" class="unfreezed-grid-header grid-header">';
		var unfreezedGridBody = '<div id="unfreezedGridBody" class="unfreezed-grid-body grid-body">';

		var isDragRendered = false;
		var actionCounter = 0;

		var actions = config.actions;

		var actionColumn = '';

		config.head.forEach(function(header, headIndex) {

			//column-chooser
			var columnClass = header.isVisible ? 'grid-column ' + header.key + '-column' : 'grid-column hide ' + header.key + '-column';
			
			//pin icon
			var pinnedClass = (header.isFreeze) ? 'pinned' : 'unpinned';

			//grid head column
			var headerColumn = '<div class="' + columnClass + '"><div>' + header.displayName + '</div><div class="pin-icon ' + pinnedClass + '"></div><div data-sortkey='+ header.key +' class="sort-icon sort-desc"></div></div>';

			if(actionCounter < actions.length) {
				actionColumn += '<div class="row-column grid-column">';
			}

			//grid body column
			var bodyColumn = '<div class="row-column ' + columnClass + '">';
			rows.forEach(function(row, i) {
				bodyColumn += '<div data-rowid=' + i + '>' + createCell(row, header) + '</div>';
				if(isDraggable && !isDragRendered) {
					draggableGridBody += '<div data-rowid=' + i + ' class="draggable-icon"></div>';
				}
				if(actionCounter < actions.length) {
					actionColumn += '<div data-customevent="'+ actions[headIndex].customEvent +'" title="'+ actions[headIndex].actionName +'" data-rowid=' + i + ' class="action-icon '+ actions[headIndex].class +'"></div>';
				}
			});
			bodyColumn += '</div>';

			if(actionCounter < actions.length) {
				actionColumn += '</div>';
			}
		
			if(header.isFreeze) {
				freezedGridHeader += headerColumn;
				freezedGridBody += bodyColumn;
			} else {
				unfreezedGridHeader += headerColumn;
				unfreezedGridBody += bodyColumn;
			}

			isDragRendered = true;
			actionCounter++;
		});

		var freezedGrid = '<div id="freezedGrid">' + freezedGridHeader + '</div>' + freezedGridBody + '</div></div>';
		var unfreezedGrid = '<div id="unfreezedGrid">' + unfreezedGridHeader + '</div>' + unfreezedGridBody + '</div></div>';

		var actionGridHeader = '<div id="actionGridHeader" class="action-grid-header grid-header"><div class="grid-column action-head-column"><div>Actions</div></div></div>';
		var actionGridBody = '<div id="actionGridBody" class="action-grid-body grid-body">' + actionColumn + '</div>';
		var actionGrid = '<div id="actionGrid">' + actionGridHeader + actionGridBody + '</div>';

		if(isDraggable) {
			var draggableGridHeader = '<div id="draggableGridHeader" class="draggable-grid-header grid-header"><div class="grid-column draggable-head-column"><div>-</div></div></div>';
			var draggableGrid = '<div id="draggableGrid">' + draggableGridHeader + draggableGridBody + '</div></div></div>';
			return draggableGrid + freezedGrid + unfreezedGrid + actionGrid;
		}
		return freezedGrid + unfreezedGrid + actionGrid;
	};

	var createCell = function(row, header) {
		var cell = '';
		switch(header.type) {
			case 'String':
				cell = createReadOnlyField(row, header);
				break;
			case 'SELECT_STATUS':
				cell = createSingleSelectField(row, header, 'executionSummaries');
				break;
			case 'TEXT':
				cell = createTextField(row, header);
				break;
			case 'LARGE_TEXT':
				cell = createLargeTextField(row, header);
				break;
			case 'SINGLE_SELECT':
				cell = createSingleSelectField(row, header, 'options');
				break;
			case 'MULTI_SELECT':
				cell = createMultiSelectField(row, header, 'options');
				break;
			case 'DATE':
				cell = createDateField(row, header);
				break;
			case 'DATE_TIME':
				cell = createDateField(row, header);
				break;
			case 'RADIO_BUTTON':
				cell = createRadioField(row, header);
				break;
			case 'CHECKBOX':
				cell = createCheckboxField(row, header);
				break;
			case 'NUMBER':
				cell = createNumberField(row, header);
				break;
			case 'WIKI_LARGE_TEXT':
				cell = createWikiField(row, header);
				break;
			case 'ATTACHMENTS':
				cell = createAttachmentsField(row, header);
				break;
		}
		return cell;
	};

	var createReadOnlyField = function(row, header) {
		return row[header.key];
	};

	var createTextField = function(row, header) {
		return '<div class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode readMode"><div id="editableField" class="editable-field"><div><span class="renderHTML readValues" title=""><p>' + row[header.key] + '</p></span></div></div></div><div id="editMode"><div class="cell-editMode" style="display: none;"><input placeholder="Enter Value..." maxlength="255" title=""></div></div></div></div>';
	};

	var createLargeTextField = function(row, header) {
		return '<div class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode readMode"><div id="editableField" class="editable-field empty"><div><span class="renderHTML readValues" title=""><p>' + row[header.key] + '</p></span></div></div></div><div id="editMode"><div class="cell-editMode" style="display: none;"><textarea maxlength="750" placeholder="Enter Value..."> </textarea></div></div></div></div>';
	};

	var createDateField = function(row, header) {
		return '<div class="cell-wrapper drop-Down drop-downdate"><div class="dropDown-wrapper"><span id="dropDown" title=""><div></div></span><span class="remove-data">x</span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span></div></div>';
	};

	var createRadioField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		for(var key in statuses) {
			options += '<li><input type="radio" name="radio" id="radio-10-1-0" value="' + statuses[key].id + '"><label class="content" tabindex="-1" for="radio-10-1-0" title="">' + statuses[key].name + '</label></li>';
		}
		return '<div class="cell-wrapper drop-Down drop-downradio"><div class="dropDown-wrapper"><span class="dropDown" title=""><div> '+ row[header.key] + '</div></span><span class="remove-data">x</span><span class="trigger-dropDown" tabindex="-1"><img src="' + header.imgUrl + '"></span><div tabindex="-1" class="content dropDown-container close"><ul style="display: none;">' + options + '</ul></div></div></div>';
	};

	var createCheckboxField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		for(var key in statuses) {
			options += '<li><input type="checkbox" id="check-1-1-0" value="'+ statuses[key].id +'"><label class="content" tabindex="-1" for="check-1-1-0" title="">'+ statuses[key].name +'</label></li>';
		}
		return '<div class="cell-wrapper drop-Down drop-downcheckbox"><div class="dropDown-wrapper"><span class="dropDown" title=""><div>'+ row[header.key] +'</div></span><span class="remove-data">x</span><span class="trigger-dropDown" tabindex="-1"><img src="'+ header.imgUrl +'"></span><div tabindex="-1" class="content dropDown-container close"><ul style="display: none;">'+ options +'</ul></div></div></div>';
	};

	var createNumberField = function(row, header) {
		return '<div class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode readMode"><div id="editableField" class="editable-field"><div><span class="renderHTML readValues" title="">'+ row[header.key] +'</span></div></div></div><div id="editMode"><div class="cell-editMode" style="display: none;"><input placeholder="Enter Value..." maxlength="255" title="" type="number"></div></div></div></div>';
	};

	var createWikiField = function(row, header) {
		return '<div class="cell-wrapper custom-textarea"><div class="editable-cell-container"><div class="cell-readMode readMode"><div  class="editable-field empty"><div><span class="renderHTML readValues"></span></div></div></div><div id="editMode" tabindex="-1" style="display: block;"><div class="cell-editMode" style="display: none;"><textarea placeholder="Enter Value..." tabindex="100"></textarea></div><div class="wikiIcons-wrapper" style="display: none;"><span class="wiki-icons wiki-renderer-icon stopBlur" tabindex="-1" title="preview"><img src="'+ header.wikiPreview +'"></span><a target="_blank" tabindex="-1" class="stopBlur" href="/secure/WikiRendererHelpAction.jspa?section=texteffects"><span class="wiki-icons wiki-help" title="Get local help about wiki markup help"><img src="'+ header.wikiHelp +'"></span></a><a><div></div></a></div></div></div></div>';
	};

	var createAttachmentsField = function(row, header) {
		return '<div class="cell-wrapper grid-cell editValue"><div><span class="renderHTML htmlValues"></span></div></div><div class="attachment-wrapper"><div class="attachment-count attachmentTrigger" tabindex="-1"><span class="noAttachments">0 attached</span></div><div class="add-attachments show" title="Add Attachment"><a><img src="'+ header.imgUrl +'"></a></div></div>';
	};

	var createSingleSelectField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		for(var key in statuses) {
			options += '<li>' + statuses[key].name + '</li>';
		}
		return '<div class="cell-wrapper drop-down"><div class="dropDown-wrapper "><span id="dropDown" title=""><div></div></span><span class="remove-data" style="display: none;">x</span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span><div class="dropDown-container"><ul style="display: none;">' + options + '</ul></div></div></div>';
	};

	var createMultiSelectField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		for(var key in statuses) {
			options += '<option value='+ statuses[key].id +'>' + statuses[key].name + '</option>';
		}
		return '<div class="cell-wrapper drop-Down drop-downmultiselect"><div class="dropDown-wrapper"><span id="dropDown"><div></div></span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span><div class="dropDown-container close"><ul><select class="dropDownSelectElem" multiple="">'+ options +'</select></ul></div></div></div>';
	};

	var actionGrid = function(config) {
		return '<div id="actionGrid">' + renderGrid(config) + '</div>';
	};

	var initialize = function(elem, config) {
		elem.innerHTML = '<div id="gridWrapper" class="grid-wrapper">' + renderGrid(config) + '</div>';
	};

	vanillaGrid.templates = {
		initialize: initialize
	};

})();
var vanillaGrid = vanillaGrid || {};

(function(undefined) {

	var pinColumns = function(target, className) {
		var headerColumn = target.parentElement;
		var index = Array.prototype.slice.call(headerColumn.parentElement.children).indexOf(headerColumn);
		if(/unpinned/.test(className)) {
			//freeze column
			var classList = headerColumn.querySelector('.unpinned').classList;
			classList.add('pinned');
			classList.remove('unpinned');
			
			var bodyColumn = document.getElementById('unfreezedGridBody').children[index];
			document.getElementById('freezedGridHeader').appendChild(headerColumn);
			document.getElementById('freezedGridBody').appendChild(bodyColumn);
		} else {
			//unfreeze column
			var classList = headerColumn.querySelector('.pinned').classList;
			classList.add('unpinned');
			classList.remove('pinned');

			var bodyColumn = document.getElementById('freezedGridBody').children[index];
			document.getElementById('unfreezedGridHeader').appendChild(headerColumn);
			document.getElementById('unfreezedGridBody').appendChild(bodyColumn);
		}
	};

	var sortRows = function(target, className) {
		var sortOrder;
		var sortKey = target.dataset.sortkey;
		var headerColumn = target.parentElement;
		if(/sort-desc/.test(className)) {
			//sort ascending
			sortOrder = 'ASC';
			var classList = headerColumn.querySelector('.sort-desc').classList;
			classList.add('sort-asc');
			classList.remove('sort-desc');
		} else {
			//sort descending
			sortOrder = 'DESC';
			var classList = headerColumn.querySelector('.sort-asc').classList;
			classList.add('sort-desc');
			classList.remove('sort-asc');
		}
		document.dispatchEvent(new CustomEvent('sort', { bubbles: true, detail: { sortKey: sortKey, sortOrder: sortOrder } }))
	};

	var columnChooser = function(columnsToHide) {
		columnsToHide.forEach(function(columnKey) {
			document.querySelector('.' + columnKey + '-column').classList.add('hide');
		});
	};

	var handleActionGridEvents = function(target) {
		document.dispatchEvent(new CustomEvent(target.dataset.customevent, { bubbles: true, detail: { rowId: target.dataset.rowid } }))
	};

	var drag = {
		dragDirection: null,
		draggedRowId: null,
		dragEnteredRowId: null,
		dragStart: function(ev) {
			this.draggedRowId = ev.target.dataset.rowid;
		},
		dragEnter: function(ev) {
			if(!this.draggedRowId) {
				return;
			}
			var enteredRowId = parseInt(ev.target.dataset.rowid, 10);
			this.dragDirection =  enteredRowId < this.dragEnteredRowId ? 'up' : 'down';
			this.dragEnteredRowId = enteredRowId;
		},
		dragLeave: function(ev) {
			if(!this.draggedRowId) {
				return;
			}
			this.addEmptyRow(parseInt(ev.target.dataset.rowid, 10));
		},
		dragEnd: function() {
			this.dragDirection = null;
			this.draggedRowId = null;
			this.dragEnteredRowId = null;
			this.removeEmptyNode();
		},
		drop: function(ev) {
			if(!this.draggedRowId) {
				return;
			}
			document.dispatchEvent(new CustomEvent('reorder', { bubbles: true, detail: { draggedId: this.draggedRowId, droppedId: ev.target.dataset.rowid } }))
		},
		removeEmptyNode: function() {
			document.querySelectorAll('.empty-node').forEach(function(emptyNode) {
				emptyNode.parentElement.removeChild(emptyNode);
			});
		},
		addEmptyRow: function(leftRowIndex) {
			this.removeEmptyNode();
			var allColumns = document.querySelectorAll('.row-column');
			allColumns.forEach(function(rowColumn) {
				var emptyNode = document.createElement('div');
				emptyNode.className = 'empty-node';
				var index = (this.dragDirection === 'up') ? leftRowIndex - 1: leftRowIndex;
				rowColumn.insertBefore(emptyNode, rowColumn.children[index]);
			});
		}
	};

	vanillaGrid.utils = {
		pinColumns: pinColumns,
		sortRows: sortRows,
		columnChooser: columnChooser,
		drag: drag
	};

})();