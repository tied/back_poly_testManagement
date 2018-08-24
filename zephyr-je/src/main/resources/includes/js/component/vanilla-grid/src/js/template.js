var vanillaGrid = vanillaGrid || {};

(function(undefined) {
	var renderGrid = function(config, elem) {

		// if(elem.id === "testDetailGridPopover") {
		// 	labelIdPrefix = "testDetailGridPopover";
		// } else {
		// 	labelIdPrefix = '';
		// }
		labelIdPrefix = elem.id;
		var rows = config.row;

		var isDraggable = config.draggableRows;

		var draggableGridBody = '<div id="draggableGridBody" class="draggable-grid-body grid-body"><div data-columnkey="drag" class="grid-column row-column">';

		var freezedGridHeader = '<div id="freezedGridHeader" class="freezed-grid-header grid-header">';
		var freezedGridBody = '<div id="freezedGridBody" class="freezed-grid-body grid-body">';

		var unfreezedGridHeader = '<div id="unfreezedGridHeader" class="unfreezed-grid-header grid-header">';
		var unfreezedGridBody = '<div id="unfreezedGridBody" class="unfreezed-grid-body grid-body">';

		var renderOnce = false;

		var actions = config.actions || [];

		var actionColumn = '<div data-columnkey="action" class="row-column grid-column action-column">';

		var freezedColumns = config.head.filter(function(header) {
			return header.isFreeze;
		});

		config.head.forEach(function(header, headIndex) {

			//column-chooser
			var columnClass = header.isVisible ? 'grid-column ' + header.key + '-column' : 'grid-column hide ' + header.key + '-column';

			//pin icon
			var pinnedClass = '';
			if(header.isFreeze) {
				pinnedClass = 'pinned';
			}
			if((freezedColumns.length < config.maxFreezed)) {
				pinnedClass = '';
			}
			if(!header.isFreeze && freezedColumns.length == config.maxFreezed) {
				pinnedClass = 'unpinned';
			}
			var pinImageUrl = (header.isFreeze) ? config.freezeImageUrl : config.unfreezedImageUrl;

			var sortHtml = header.isSortable ? '<div data-sortkey='+ header.key +' data-sortorder="' + header.sortOrder + '" class="sort-icon '+ header.sortOrder +'"></div>' : '';

			//grid head column
			if (pinImageUrl) {
				var headerColumn = '<div data-columnkey="'+ header.key + '" class="' + columnClass + '"><div><div class="pin-icon ' + pinnedClass + '"><img src="'+ pinImageUrl +'"></div><div title="' + header.displayName + '">' + header.displayName + '</div>' + sortHtml + '</div></div>';
			} else{
				var headerColumn = '<div data-columnkey="'+ header.key + '" class="' + columnClass + '"><div><div title="' + header.displayName + '">' + header.displayName + '</div>' + sortHtml + '</div></div>';
			}

			//grid body column
			var bodyColumn = '<div data-columnkey="'+ header.key + '" class="row-column ' + columnClass + '">';

			rows.forEach(function(row, i) {

				//assign dataset from response obj
				var datastr = '';
				config.dataset && config.dataset.forEach(function(dataObj) {
					datastr = datastr + ' data-' + dataObj.name + '=' + row[dataObj.key];
				});

				var a = createCell(row, header, config.contextPath, i, config);
				if (labelIdPrefix === 'testDetailGridExecutionPage') {
					if(typeof(a) === 'string'){
						var b = a.replace(/'/g, '"');
						var	el = b;
						// var el = document.createElement( 'div' );
						// el.innerHTML = b;
					} else{
						var el = a;
					}
				} else{
					var el = a;
				}

				if (typeof(row[header.key]) === 'string') {
					row[header.key] = row[header.key].replace(/"/g, "'");
				}

				if(row.hasOwnProperty('canViewIssue') && !row['canViewIssue']) {
					if(headIndex === config.maxFreezed) {
						bodyColumn += '<div class="row canViewIssue noPermissionErrorRow">'+ ((config.noPermissionTestIssue + ' ' + row['projectKey']) || '') +'</div>';
					} else {
						bodyColumn += '<div class="row canViewIssue"></div>';
					}
				} else {
					bodyColumn += '<div class="row" '+ datastr +' data-columnid="'+ header.key +'" data-rowid=' + i + ' data-fieldvalue="'+ row[header.key] +'">' + el + '</div>';
					// if (typeof(header.key) === 'number') {
					// 	var title = "";
					// 	if (/<[a-z][\s\S]*>/i.test(el)) {
					// 		var div = document.createElement('div');
					// 		div.innerHTML = el;
					// 		title = div.textContent;
					// 	} else{
					// 		title = el;
					// 	}
					// 	bodyColumn += '<div class="row" '+ datastr +' data-columnid="'+ header.key +'" data-rowid=' + i + ' data-fieldvalue="'+ row[header.key] +'" title="'+title+'">' + el + '</div>';
					// } else{
					// 	bodyColumn += '<div class="row" '+ datastr +' data-columnid="'+ header.key +'" data-rowid=' + i + ' data-fieldvalue="'+ row[header.key] +'">' + el + '</div>';
					// }
				}

				if(isDraggable && !renderOnce) {															// data-downloadurl="" and  <img /> is added to handle jira script errors for all the browsers.
					draggableGridBody += '<div class="draggable-icon row" ' + datastr + ' data-rowid=' + i + '> <span draggable="true" data-downloadurl=""> <img /></span></div>';
				}
				if(!renderOnce && actions.length>0) {
					if(row.hasOwnProperty('canViewIssue') && !row['canViewIssue']) {
						actionColumn += '<div class="row canViewIssue"></div>';
					} else {
						actionColumn += '<div class="row" ' + datastr + ' data-customevent="' + actions[headIndex].customEvent + '" data-orderid="' + row.orderId + '" data-rowid=' + i + ' class="action-icon '+ actions[headIndex].class +'"><div class="action-outer-wrapper">' + renderActionColumn(config, row) +'</div></div>';
					}
				}
			});
			bodyColumn += '</div>';

			actionColumn += '</div>';

			if(header.isFreeze) {
				freezedGridHeader += headerColumn;
				freezedGridBody += bodyColumn;
			} else {
				unfreezedGridHeader += headerColumn;
				unfreezedGridBody += bodyColumn;
			}

			renderOnce = true;
		});
		var selectionGrid = '<div id="gridSelection" class="check-cell table-columns-wrapper">' + renderSelectionGrid(config, elem) + '</div>';
		var freezedGrid = '<div id="freezedGrid">' + freezedGridHeader + '</div>' + freezedGridBody + '</div></div>';
		var unfreezedGrid = '<div id="unfreezedGrid">' + unfreezedGridHeader + '</div>' + unfreezedGridBody + '</div></div>';

		var actionGridHeader = '<div id="actionGridHeader" class="action-grid-header grid-header"><div class="grid-column action-head-column"><div>Actions</div></div></div>';
		var actionGridBody = '<div id="actionGridBody" class="action-grid-body grid-body">' + actionColumn + '</div>';
		var actionGrid = config.actions && config.actions.length ? '<div id="actionGrid">' + actionGridHeader + actionGridBody + '</div>' : '';

		var actionContainer = renderActionContainer(config, elem);
		var table = '<div class="table-container">' + selectionGrid + renderDraggableGrid(isDraggable, draggableGridBody) + freezedGrid + unfreezedGrid + actionGrid+ '</div>';
		if(isDraggable) {
			return actionContainer + '<div class="table-container-wrapper">' + table + '</div>';
		}
		return actionContainer + table;
	};

	var labelIdPrefix = '';

	var renderDraggableGrid = function(isDraggable, draggableGridBody) {
		var draggableGrid = '';
		if(isDraggable) {
			var draggableGridHeader = '<div id="draggableGridHeader" class="draggable-grid-header grid-header"><div class="grid-column draggable-head-column"><div>-</div></div></div>';
			draggableGrid = '<div id="draggableGrid">' + draggableGridHeader + draggableGridBody + '</div></div></div>';
		}
		return draggableGrid;
	};

	var renderSelectionGrid = function(config, elem) {
		var gridMarkup = '';
		var executionList = vanillaGrid.utils.selectedExecutionIds[elem.id] || [];
		if(config.rowSelection) {
			gridMarkup = '<div id="selectionGridHeader" class="selection-grid-header grid-header" data-rowid="0">' +
			                '<div class="grid-column selection-column">' +
			                  '<div class=""></div>' +
			                '</div>' +
			              '</div>' +
			              '<div class="row-column grid-column selection-column selection-body">' +
				              config.row.map(function(execution, i){
				              	var datastr = '';
								config.dataset && config.dataset.forEach(function(dataObj) {
									datastr = datastr + ' data-' + dataObj.name + '=' + execution[dataObj.key];
								});
				              	if(execution.hasOwnProperty('canViewIssue') && !execution['canViewIssue']) {
									return '<div class="row canViewIssue"></div>';
								}
				              	var isChecked = executionList.indexOf(execution.id) > -1 ? 'checked' : '';
				              	return '<div '+ datastr +' data-rowid="'+ i +'" class="row">' +
							                    '<input type="checkbox" ' + isChecked + ' class="row-select-checkbox" value="'+ execution.id +'"/>' +
							                '</div>'
				              }).join('') +
			             	'</div>'
		}
		return gridMarkup;
	}

	var renderActionContainer = function(config, elem) {
		var leftWrapper, rightWrapper, container;
		var bulkActionLeftButtons = '',
			  bulkActionRightButtons = '';
		if(config.hasBulkActions) {
			bulkActionLeftButtons += config.bulkActions.map(function(action) {
				isDisabled = action.disabled ? 'disabled' : '';
				var executionList = vanillaGrid.utils.selectedExecutionIds[elem.id] || [];
				isDisabled = executionList.length ? '' : isDisabled;

				var currentList = config.row.filter(function(execution){
					return executionList.indexOf(execution.id) > -1;
				});
				if(executionList.length && currentList.length === config.row.length && action.customEvent === 'selectRows') {
					// var imgUrl = action.imgSrcChecked;
					// action.imgSrc = imgUrl;
					var imgSrc = action.imgSrcChecked;
					var classSelected= 'allSelected';
				} else{
					var imgSrc = action.imgSrc;
					var classSelected = '';
				}
				return '<div class="bulkAction-container ' + action.customEvent + ' ' + classSelected +'" data-eventname="'+ action.customEvent +'">' +
                '<button type="button" '+ isDisabled +' class="bulk-action">' +
                	'<img src="'+ imgSrc +'">' + action.actionName +'</button>' +
              '</div>'
			}).join('')
		}
		if(config.exportTestSteps && config.exportTestSteps.isEnabled) {
			bulkActionRightButtons += '<div class="bulkAction-buttons exportContainer">' +
            '<div class="bulkAction-container bulkAction-round" data-customevent="'+ config.exportTestSteps.customEvent +'" data-eventname="'+ config.exportTestSteps.customEvent +'" tabindex="-1">' +
              '<img src="' + config.exportTestSteps.imgSrc +'">' +
            '</div>' +
            config.openExportDropDown ? (
            	'<div class="exportListItems">' +
            		config.exportTestSteps.exportOptions.map(function(exportOption) {
            			return '<div class="exportWrapper" data-exportid="' + exportOption.exportId +'" data-customevent="' + config.exportTestSteps.customEvent + '" data-eventname="' + config.exportTestSteps.customEvent +'">' +
                    '<img src="' + exportOption.imgSrc + '">' +
                    '<span>' + exportOption.exportName + '</span>' +
                  '</div>'
            		}).join('') +
            	'</div>'
            ) : '' +
          '</div>'
		}
		if(config.popupTestSteps && config.popupTestSteps.isEnabled) {
			bulkActionRightButtons += '<div class="bulkAction-buttons bulkAction-container bulkAction-round" title="Large View"><img src="'+config.popupTestSteps.imgSrc+'"></div>';
		}
		if(config.testStepFocus && config.testStepFocus.isEnabled) {
			bulkActionRightButtons += '<div class="bulkAction-buttons testStepFocus "><div class="testStepFocus-btn">Add Step<img src="'+ config.testStepFocus.imgSrc +'"></div></div>';
		}
		if(config.columnchooser && config.columnchooser.isEnabled) {
			bulkActionRightButtons += '<div class="bulkAction-buttons">' +
            '<div class="bulkAction-container outlineNone" data-customevent="'+ config.columnchooser.customEvent +'" data-eventname="' + config.columnchooser.customEvent +'" tabindex="-1">' +
              '<button type="button" class="columnChooser-btn"'+ (config.columnchooser.disabled ? 'disabled' : '') +'>' + config.columnchooser.actionName +
                  '<img src="' + config.columnchooser.imgSrc +'">' +
              '</button>' +
            '</div>' +
            '<div tabindex="-1" class="content column-chooser-cont dialogueWrapper close">' +
            	'<div class="columnChooser-heading">' + config.columnChooserHeading + '</div>' +
            	'<div class="column-chooser-options-container checkbox-wrapper content" tabindex="-1">' +
						    renderColumnChooserOptions(config, elem.id) +
						  '</div>' +
            	'<div class="buttonPanel">' +
						    '<button tabindex="-1" type="button" class="content columnChooser-submit">' + config.submit +'</button>' +
						    '<button tabindex="-1" type="button" class="content columnChooser-cancel">' + config.cancel + '</button>' +
						  '</div>' +
            '</div>' +
          '</div>'
		}
		if(config.addTests && config.addTests.isEnabled) {
			bulkActionRightButtons += '<div class="bulkAction-buttons">' +
            '<div class="bulkAction-container" data-customevent="' + config.addTests.customEvent + '" data-eventname="'+ config.addTests.customEvent +'">' +
              '<button type="button" class="addTest-btn">' +
              	'<img src="'+ config.addTests.imgSrc +'">'+ config.addTests.actionName +'</button>' +
            '</div>' +
          '</div>'
		}
		leftWrapper = '<div class="bulkAction-buttons">'+bulkActionLeftButtons+'</div>';
		rightWrapper = '<div class="bulkAction-buttons">'+bulkActionRightButtons+'</div>';

		container = '<div class="action-container grid-component"><div class="bulkAction-leftWrapper">'+leftWrapper+'</div><div class="bulkAction-rightWrapper">'+rightWrapper+'</div></div>';
		return container;
	}

	var renderColumnChooserOptions = function (config, containerId) {
		var columnChooserValues = config.columnchooser.columnChooserValues;
		if(!columnChooserValues)
			return;
		var tempArr = [];
				customFieldKeys = Object.keys(columnChooserValues);
		config.head.forEach(function(column){
			 var key = column.key.toString();
			if (key === 'htmlStep' || key === 'step') {
				key = containerId === 'testDetailGridExecutionPage' ? 'testStep' :'teststep';
			} else if (key === 'htmlData' || key === 'data') {
				key = 'testdata';
			} else if (key === 'htmlResult' || key === 'result') {
				key = containerId === 'testDetailGridExecutionPage' ? 'expectedResult' : 'testresult';
			} else if (key === 'attachmentsMap') {
				key = containerId === 'testDetailGridExecutionPage' ? 'stepAttachment' : 'attachment';
			} else if (key === 'stepAttachmentMap') {
				key = 'attachments';
			} else if (key === 'versionName') {
				key = containerId === 'testExecutionGrid' ? 'version' : key;
			} else if (key === 'cycleName') {
				key = containerId === 'testExecutionGrid' ? 'testCycle' : key;
			} else if (key === 'folderName') {
				key = containerId === 'testExecutionGrid' ? 'folder' : key;
			}
			if(customFieldKeys.indexOf(key) > -1) {
				var label = columnChooserValues[key].displayName;
				var isChecked =  columnChooserValues[key].isVisible === 'true' ? 'checked' : '';
				tempArr.push(
					'<div class="checkbox-container">' +
      		 	'<input class="column-chooser-item" data-id="' + key + '" id="'+ containerId + '-' + key +'" type="checkbox" content="' + label +'" value="'+ key +'" '+ isChecked +'>' +
    		    '<label for="'+ containerId + '-' + key +'" title="' + label +'">'+label+'</label>' +
      		 '</div>'
				)
			}
		});
		return tempArr.join('');

	}

	var createCell = function(row, header, contextPath, tableRowId, config) {
		var cell = '';
		switch(header.type) {
			case 'String':
				cell = createReadOnlyField(row, header, contextPath);
				break;
			case 'HTMLContent':
				cell = createReadOnlyField(row, header);
				break;
			case 'SELECT_STATUS':
				cell = createStatusSelectField(row, header, 'executionSummaries');
				break;
			case 'TEXT':
				cell = createTextField(row, header, config);
				break;
			case 'LARGE_TEXT':
				cell = createLargeTextField(row, header, config);
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
				cell = createDateField(row, header, true);
				break;
			case 'RADIO_BUTTON':
				cell = createRadioField(row, header, 'options', tableRowId);
				break;
			case 'CHECKBOX':
				cell = createCheckboxField(row, header, 'options', tableRowId);
				break;
			case 'NUMBER':
				cell = createNumberField(row, header, config);
				break;
			case 'WIKI_LARGE_TEXT':
				cell = createWikiField(row, header);
				break;
			case 'ATTACHMENTS':
				cell = createAttachmentsField(row, header, contextPath);
				break;
			case 'CYCLE_NAME' :
				cell = createCycleNameLink(row, header, contextPath);
				break;
		}
		if(header.key === 'defects') {
			cell = createDefectsField(row, header, contextPath);
		}

		return cell;
	};

	var createCycleNameLink = function(row, header, contextPath) {
		return '<a href="' + contextPath + '/DisplayCycle.jspa?cycleId=' + row.cycleId + "&versionId=" + row.versionId + "&issueKey=" + row.issueKey + '">' + row[header.key] +'</a>';
	}

	var createReadOnlyField = function(row, header, contextPath) {
		if(header.key === 'executedBy')
			return row["executedByDisplay"] || '-'
		if(header.key === 'issueKey')
			return '<a href="' + contextPath + '/browse/' + row[header.key] +'">' + row[header.key] +'</a>'
		if(header.key === 'orderId' && row.id == '-1')
			return '';
		else if(header.type === 'HTMLContent')
			return row[header.key] || '-';
		else
			return safe_tags(row[header.key]) || '-';
	};

	var createTextField = function(row, header, config) {
		if(row.mode === 'edit') {
			return '<div data-editmode="true" class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode editMode"><div id="editableField" class="editable-field"><div><span class="renderHTML readValues" data-content="' + config.placeholderText + '" title="' + (row[header.key] || '') + '">' + (row[header.key + 'htmlValue'] || row[header.key] || '') + '</span></div></div></div><div id="editMode"><div class="cell-editMode"><input placeholder="Enter Value..." maxlength="255" value="' + (row[header.key] || '') +  '" title=""></div></div></div></div>';
		}
		return '<div class="cell-wrapper custom-text"> <div class="editable-cell-container"> <div class="cell-readMode readMode"> <div id="editableField" class="editable-field"> <div> <span class="renderHTML readValues" data-content="' + config.placeholderText + '" title="' + (row[header.key] || '') + '">' + (row[header.key + 'htmlValue'] || row[header.key] || '') + '</span> </div></div> </div><div id="editMode"> <div class="cell-editMode hide"> <input placeholder="Enter Value..." maxlength="255" value="' + (row[header.key] || '') + '" title=""> </div></div> </div> </div>';
	};

	var createLargeTextField = function(row, header, config) {
		if(row.mode === 'edit') {
			return '<div data-editmode="true" class="cell-wrapper custom-text textarea-custom-field"> <div class="editable-cell-container"> <div class="cell-readMode editMode"> <div id="editableField" class="editable-field"> <div> <div class="renderHTML readValues" data-content="' + config.placeholderText + '" title="' + (row[header.key] || '') + '">'+ (row[header.key + 'htmlValue'] || row[header.key] || '')+'</div> </div> </div> </div><div id="editMode"> <div class="cell-editMode"> <textarea placeholder="Enter Value..." maxlength="750" title=""></textarea> </div> </div> </div> </div>';
		}
		return '<div class="cell-wrapper custom-text textarea-custom-field"> <div class="editable-cell-container"> <div class="cell-readMode readMode"> <div id="editableField" class="editable-field empty"> <div> <div class="renderHTML readValues" data-content="' + config.placeholderText + '" title="' + (row[header.key] || '') + '">' + (row[header.key + 'htmlValue'] || row[header.key] || '') + '</div> </div> </div> </div><div id="editMode"> <div class="cell-editMode hide"><textarea maxlength="750" placeholder="Enter Value...">' + (row[header.key] || '') + '</textarea></div></div> </div> </div>';

	};

	var createDateField = function(row, header, isDateTime) {
		var labelValue = row[header.key] || '';
		var hideClass = labelValue ? '' : 'hide';
		var dateTimeClass = isDateTime ? 'date-time-dropdown' : '';
		var editModeStr = row.mode === 'edit' ? 'data-editmode="true"' : '';
		return '<div '+ editModeStr +' class="cell-wrapper drop-Down drop-downdate"><div class="dropDown-wrapper"><span id="dropDown" title="'+ labelValue +'"><div>'+ labelValue +'</div></span><span class="remove-data '+ hideClass +'">x</span><span class="trigger-dropDown date-dropdown '+ dateTimeClass +'"><img src="'+ header.imgUrl +'"></span></div></div>';
	};

	var createRadioField = function(row, header, optionKey, tableRowId) {
		var options = '';
		var statuses = header[optionKey];
		var values = row[header.key];
		if(values) {
			values = values.split(',');
		}
		var labelValues = [];
		var name = "radio-" + tableRowId;
		for(var key in statuses) {
			var inputStr = '<input data-rowoption="true" type="radio" name="' + name +'" id="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" value="' + statuses[key].value + '">';
			if(values && values.indexOf(statuses[key].value) > -1) {
				labelValues.push(safe_tags(statuses[key].content));
				inputStr = '<input data-rowoption="true" type="radio" checked="checked" name="' + name +'" id="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" value="' + statuses[key].value + '">';
			}
			options += '<li>'+ inputStr +'<label data-rowoption="true" class="content" tabindex="-1" for="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" title="'+safe_tags(statuses[key].content)+'">' + safe_tags(statuses[key].content) + '</label></li>';
		}
		labelValues = labelValues.toString();
		var hideClass = labelValues ? '' : 'hide';
		var editModeStr = row.mode === 'edit' ? 'data-editmode="true"' : '';
		return '<div '+ editModeStr +' class="cell-wrapper drop-Down drop-downradio"><div class="dropDown-wrapper"><span class="dropDown" title="'+ labelValues +'"><div>'+ labelValues + '</div></span><span class="remove-data '+ hideClass +'">x</span><span class="trigger-dropDown" tabindex="-1"><img src="' + header.imgUrl + '"></span><div tabindex="-1" class="content dropDown-container close"><ul>' + options + '</ul></div></div></div>';
	};

	var safe_tags = function(str) {
		if (typeof str === 'string') {
    	return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') ;
		} else {
			return str;
		}
	}

	var createCheckboxField = function(row, header, optionKey, tableRowId) {
		var options = '';
		var statuses = header[optionKey];
		var values = row[header.key];
		if(values) {
			values = values.split(',');
		}
		var labelValues = [];
		for(var key in statuses) {
			var inputStr = '<input data-rowoption="true" type="checkbox" id="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" value="'+ statuses[key].value +'">';
			if(values && values.indexOf(statuses[key].value) > -1) {
				labelValues.push(safe_tags(statuses[key].content));
				inputStr = '<input data-rowoption="true" type="checkbox" checked id="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" value="'+ statuses[key].value +'">';
			}
			options += '<li>'+ inputStr +'<label data-rowoption="true" class="content" tabindex="-1" for="' + labelIdPrefix + '-' + tableRowId + '-' + statuses[key].value + '" title="'+safe_tags(statuses[key].content)+'">'+ safe_tags(statuses[key].content) +'</label></li>';
		}
		labelValues = labelValues.toString();
		var hideClass = labelValues ? '' : 'hide';
		var editModeStr = row.mode === 'edit' ? 'data-editmode="true"' : '';
		return '<div '+ editModeStr +' class="cell-wrapper drop-Down drop-downcheckbox"><div class="dropDown-wrapper"><span class="dropDown" title="'+ labelValues +'"><div>'+ labelValues +'</div></span><span class="remove-data '+ hideClass +'">x</span><span class="trigger-dropDown" tabindex="-1"><img src="'+ header.imgUrl +'"></span><div tabindex="-1" class="content dropDown-container close"><ul>'+ options +'</ul></div></div></div>';
	};

	var createNumberField = function(row, header, config) {
		if(row.mode === 'edit') {
			return '<div data-editmode="true" class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode editMode"><div id="editableField" class="editable-field"><div><span class="renderHTML readValues" data-content="' + config.placeholderText + '" title="'+ (row[header.key] || '') +'">'+ (row[header.key] || '') +'</span></div></div></div><div id="editMode"><div class="cell-editMode"><input placeholder="Enter Value..." maxlength="255" value="' + (row[header.key] || '') + '" title="" type="number"></div></div></div></div>';
		}
		return '<div class="cell-wrapper custom-text"><div class="editable-cell-container"><div class="cell-readMode readMode"><div id="editableField" class="editable-field"><div><span class="renderHTML readValues" data-content="' + config.placeholderText +'" title="' + (row[header.key] || '') + '">'+ (row[header.key] || '') +'</span></div></div></div><div id="editMode"><div class="cell-editMode hide"><input placeholder="Enter Value..." maxlength="255" value="' + (row[header.key] || '') + '" title="" type="number"></div></div></div></div>';
	};

	var createWikiField = function(row, header) {
		if(row.mode === 'edit') {
			return '<div data-editmode="true" class="cell-wrapper custom-textarea"><div class="editable-cell-container"><div class="cell-readMode editMode"><div  class="editable-field empty"><div><span class="renderHTML readValues" title="' + (row[header.key] || '') + '">' + row[header.editKey] + '</span></div></div></div><div id="editMode" style="display: block;"><div class="cell-editMode"><textarea placeholder="Enter Value..." tabindex="100">' + row[header.key] + '</textarea></div><div class="wikiIcons-wrapper"><span class="wiki-icons wiki-renderer-icon stopBlurEvent" title="preview" tabindex="-1"><img src="' + header.wikiPreview +'"></span><a target="_blank" tabindex="-1" class="stopBlurEvent" href="'+ header.wikiHelpUrl +'"><span class="wiki-icons wiki-help" title="Get local help about wiki markup help"><img src="'+ header.wikiHelp +'"></span></a><a><div></div></a></div></div></div></div>';
		}
		return '<div class="cell-wrapper custom-textarea"><div class="editable-cell-container"><div class="cell-readMode readMode"><div  class="editable-field empty"><div><span class="renderHTML readValues" title="' + (row[header.key] || '') + '">' + row[header.editKey] + '</span></div></div></div><div id="editMode" style="display: block;"><div class="cell-editMode hide"><textarea class="editing-field-text-area" placeholder="Enter Value..." tabindex="100">' + row[header.key] + '</textarea></div><div class="wikiIcons-wrapper" style="display: none;"><span class="wiki-icons wiki-renderer-icon stopBlurEvent" title="preview" tabindex="-1"><img src="' + header.wikiPreview +'"></span><a target="_blank" tabindex="-1" class="stopBlurEvent" href="'+ header.wikiHelpUrl +'"><span class="wiki-icons wiki-help" title="Get local help about wiki markup help"><img src="'+ header.wikiHelp +'"></span></a><a><div></div></a></div></div></div></div>';
	};

	var createAttachmentsField = function(row, header, contextPath) {
		attachmentContextPath = contextPath;
		if(row.mode === 'edit') {
			return '';
		}
		var addAttachmentTrigger = header.canAddAttachment ? '<div class="add-attachments show" title="Add Attachment"><a class="add-attachments-trigger"><img src="'+ header.imgUrl +'"></a></div>' : '';
		var attachmentsMap = row[header.key];
		if(attachmentsMap && attachmentsMap.length) {
			var attachmentCount = attachmentsMap.length || 0;
			var attachmentThumbStr = '';
			// attachmentsMap.forEach(function(attachment) {
			// 	attachmentThumbStr += getAttachmentThumbnails(attachment, header.canAddAttachment);
			// });
			for (var i = 0; i < attachmentsMap.length; i++) {
				attachmentThumbStr += getAttachmentThumbnails(attachmentsMap[i], header.canAddAttachment);
			}
			return '<div class="attachment-wrapper"><div class="attachment-count" tabindex="-1"><span class="show-attachments-trigger">'+attachmentCount+' attached</span><div class="attachments-inlineDialogWrapper close">'+ attachmentThumbStr +'</div></div>'+ addAttachmentTrigger +'</div>'
		}
		if(header.fetchAttachment) {
			var fetchAttachmentCount = row[header.fetchAttachmentCountKey];
			var fetchAttachmentClass = fetchAttachmentCount ? 'fetch-attachments-trigger' : 'noAttachments';
			return '<div class="attachment-wrapper"><div class="attachment-count" tabindex="-1"><span class="'+ fetchAttachmentClass +'">'+fetchAttachmentCount+' attached</span><div class="fetch-attachments-preview attachments-inlineDialogWrapper close"></div></div>'+ addAttachmentTrigger +'</div>';
		}

		return '<div class="cell-wrapper grid-cell editValue"><div><span class="renderHTML htmlValues"></span></div></div><div class="attachment-wrapper"><div class="attachment-count" tabindex="-1"><span class="noAttachments">0 attached</span></div>'+ addAttachmentTrigger +'</div>';
	};

	var attachmentContextPath = null;

	var getDeleteAttachmentStr = function(canAddAttachment) {
		return canAddAttachment ? '<a tabindex="-1" class="icon-delete entity-operations-delete" title="Delete this attachment"><img src="'+ attachmentContextPath +'/download/resources/com.thed.zephyr.je/images/icons/delete-attachment_icon.svg"></a>' : '';
	};

	var getPreviewDownloadStr = function(attachment) {
		var previewStr = '<a tabindex="-1" class="icon-preview" title="Preview"><img src="'+ attachmentContextPath +'/download/resources/com.thed.zephyr.je/images/icons/view-attachment_icon.svg"></a>';
		var downloadStr = '<a download title="Download" class="icon-download" data-name="' + attachment.fileName + '" data-fieldid="'+attachment.fileId+'" href="'+ attachmentContextPath +'/plugins/servlet/schedule/viewAttachment?id='+ attachment.fileId +'&amp;name='+ attachment.fileName +'"><img src="'+ attachmentContextPath +'/download/resources/com.thed.zephyr.je/images/icons/download-icon.svg" /></a>'
		return isImageTypeAttachment(attachment.fileName) ? previewStr : downloadStr;
	};

	var isImageTypeAttachment = function(fileName) {
    	return (/\.(gif|jpg|jpeg|tiff|png)$/i).test(fileName);
    };

	var getAttachmentThumbnails = function(attachment, canAddAttachment) {
		var previewDownloadStr = getPreviewDownloadStr(attachment);
		var deleteAttachmentStr = getDeleteAttachmentStr(canAddAttachment);
		var thumbnailImg = isImageTypeAttachment(attachment.fileName) ? '<img height="100%" border="0" src="'+ attachmentContextPath +'/plugins/servlet/schedule/viewAttachment?id='+attachment.fileId+'&amp;name='+attachment.fileName+'" alt="PNG File">' : '<span>' + attachment.fileIconAltText + '</span>';
		return '<div data-attachmentname="'+ attachment.fileName +'" data-attachmentid="'+ attachment.fileId +'" class="zephyr-attachment-thumb"><div class="zephyr-attachment-overlay"></div><div class="zephyr-attachment-actions">'+ previewDownloadStr + deleteAttachmentStr +'</div>'+ thumbnailImg +'<div class="zephyr-attachment-thumb-overlay"><div class="zephyr-attachment-title"><a href="'+ attachmentContextPath +'/plugins/servlet/schedule/viewAttachment?id='+attachment.fileId+'&amp;name=image_95061.png" title="'+ attachment.fileName +'">'+ attachment.fileName +'</a></div></div></div>';
	};

	var createSingleSelectField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		var labelValue = row[header.key] || '';
		for(var key in statuses) {
			if(labelValue === statuses[key].value) {
				labelValue = statuses[key].content;
			} else {
				options += '<li data-value="'+ statuses[key].value +'" class="dropdown-option" title="'+statuses[key].content+'">' + statuses[key].content + '</li>';
			}
		}
		var hideClass = labelValue ? '' : 'hide';
		var editModeStr = row.mode === 'edit' ? 'data-editmode="true"' : '';
		return '<div '+ editModeStr + ' class="cell-wrapper drop-down"><div class="dropDown-wrapper "><span title="'+ labelValue +'"><div>'+ labelValue +'</div></span><span class="remove-data '+ hideClass +'">x</span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span><div class="dropDown-container close"><ul>' + options + '</ul></div></div></div>';
	};

	var createStatusSelectField = function(row, header, optionKey) {
		var options = '';
		var statusArr = header[optionKey];
		var selected = '',
				background = 'none';
		var statuses = {};
		var executionStatus = row.status || row.executionStatus;
		for (var i in statusArr) {
			statuses[i] = statusArr[i].attributes || statusArr[i];
		}
		for(var key in statuses) {

			selected = statuses[key].id === parseInt(executionStatus) ? statuses[key].name : selected;
			background = statuses[key].id === parseInt(executionStatus) ? statuses[key].color : background;
			var dataContent = statuses[key].name;
			if(typeof(dataContent) === 'string'){
				dataContent = htmlEncode(dataContent);
			}
			options += '<li tabindex="0" data-value="'+ statuses[key].id +'" data-content="'+ dataContent +'" class="status-select-option stopBlurEvent">' + statuses[key].name + '</li>';
		}
		var title = selected ? htmlEncode(selected) : '';
		var hideClass = selected ? '' : 'hide';
		var workflowClass = (row['isExecutionWorkflowEnabled'] && (row['executionWorkflowStatus'] === 'COMPLETED')) ? 'disabled' : '';
		if (!header.isInlineEdit) {
			return '<div class="cell-wrapper drop-down readOnly '+ workflowClass +'"><div class="dropDown-wrapper select-status"><span id="dropDown" title="'+ title +'" style="background:'+ background +'"><div>'+ selected +'</div></span><span class="remove-data '+ hideClass +'" style="display: none;">x</span></div></div>';
		} else{
			return '<div tabindex="0" class="cell-wrapper drop-down ' + workflowClass + '"><div class="dropDown-wrapper select-status"><span id="dropDown" title="'+ title +'" style="background:'+ background +'"><div>'+ selected +'</div></span><span class="remove-data '+ hideClass +'" style="display: none;">x</span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span><div class="dropDown-container close"><ul>' + options + '</ul></div></div></div>';
		}

	};

	var createMultiSelectField = function(row, header, optionKey) {
		var options = '';
		var statuses = header[optionKey];
		var values = row[header.key];
		if(values) {
			values = values.split(',');
		}
		var labelValues = [];
		for(var key in statuses) {
			var optionStr = '<option value='+ statuses[key].value +' title="'+statuses[key].content+'">' + statuses[key].content + '</option>';
			if(values && values.indexOf(statuses[key].value) > -1) {
				labelValues.push(statuses[key].content);
				optionStr = '<option selected="selected" value='+ statuses[key].value +' title="'+statuses[key].content+'">' + statuses[key].content + '</option>';
			}
			options += optionStr;
		}
		labelValues = labelValues.toString();
		var hideClass = labelValues ? '' : 'hide';
		var editModeStr = row.mode === 'edit' ? 'data-editmode="true"' : '';
		return '<div '+ editModeStr + ' class="cell-wrapper drop-Down drop-downmultiselect"><div class="dropDown-wrapper"><span title="'+labelValues+'"><div>'+labelValues+'</div></span><span class="remove-data '+ hideClass +'">x</span><span class="trigger-dropDown"><img src="'+ header.imgUrl +'"></span><div class="dropDown-container close"><ul><select class="dropDownSelectElem" multiple="">'+ options +'</select></ul></div></div></div>';
	};

	var createDefectsField = function(row, header, contextPath) {
		if(!row.defects) {
			row.defects = [];
		}
		if (header.type === "STEP_DEFECTS") {
			var defectsList =  row.defects.map(function(defect) {
				return '<div class="defectsList" data-key="'+defect.key+'" style="display: flex;"><div class="statusColor" style="background-color :'+ defect.color+'"></div><div class="defectKey"><a class="defectKey '+ (defect.resolution === "Done" ? "strikeThrough" : "notDone") +'" href='+ contextPath + '/browse/' + defect.key +'>'+ defect.key +'</a></div><div class="defectStatus">'+defect.status+'</div><div class="defectSummary">'+defect.summary+'</div><div class="removeDefect"><img src="'+header.imgRemoveDefect+'"></div></div>'
			}).join('');
			var defectsCountWrapper = '<span class="defectCount">'+ row.defects.length + ' defects' + '</span><span class="defectIcons create" tabindex="-1"><img src="'+header.imgurlAddDefect+'"></span><span class="defectIcons dialog"><img src="'+header.imgurlCreateIssue+'"></span>';
			var addDefect = '<div class="defectsSearchBox close"><div class="input-wrapper"><div><button type="button" class="aui-button create-defect">Create defect</button></div><div class="create-input-sep">Add defect</div><input type="text" class="ajax-input" placeholder="Type to Search..."></div><div class="resultWrapper"></div></div>';
			if(row.defects.length) {
				return '<div class="defect-click">'+ defectsCountWrapper + '</div><div class="step-defect-list stepDefects-inlineDialogWrapper close"><div class="stepLevelDefects"><span class="heading">Step Level Defects Filed</span><div class="defectsList-Container">'+ defectsList + '</div></div></div>' + addDefect;
			} else {
				return '<div class="defect-click">'+ defectsCountWrapper + '</div>' + addDefect;
			}
		} else{
			var defectsList =  row.defects.map(function(defect) {
				return '<a class="defectKey '+ (defect.resolution === "Done" ? "strikeThrough" : "notDone") +'" href='+ contextPath + '/browse/' + defect.key +'>'+ defect.key +'</a>'
			}).join(' , ');
			var defectsCountWrapper = '';
			if(row['executionDefectCount'] || row['stepDefectCount'] || row.defects.length) {
				defectsCountWrapper = '<span class="defectCount">'+ row.executionDefectCount + ' | ' + row.stepDefectCount + '</span>';
				return '<div class="defect-hover">'+ defectsCountWrapper + defectsList + '</div>';
			}
		}
		return '-';
	}

	var renderActionColumn = function(config, row) {
		var actions = (row.mode === 'edit') ? [config['addTestSteps'], config['removeTestSteps']] : config.actions;
		return actions.map(function(action) {
			var tickClass = (action.customEvent === 'addstep') ? 'tick-button' : (action.customEvent === 'clearvalue') ? 'cross-button' : '';
			return '<div class="actions-wrapper ' + tickClass + ' ' + action.customEvent + '" data-customevent="' + action.customEvent + '" data-orderid="' + row.orderId + '" data-action="'+ action.actionName +'" title="' + action.actionName + '">'
				+ (action.customEvent === 'executeRow' ? '<a data-action="' + action.actionName + '" data-customevent="' + action.customEvent + '" class="eButton" href="' + row.executionUrl + '" data-href="' + row.executionUrl + '"><img data-action="' + action.actionName + ' data-customevent="' + action.customEvent + ' src="'+ action.imgSrc +'"/></a>' : '<img class="action-icon" data-customevent="'+ action.customEvent +'" data-action="'+ action.actionName +'" src="'+ action.imgSrc +'"/>')
						 + '</div>'
		}).join('');
	};

	var initialize = function(elem, config) {
		if(!elem) {
			return;
		}
		attachmentContextPath = config.contextPath;
		elem.innerHTML = '<div id="vanillaGridWrapper" class="vanilla-grid-wrapper">' + renderGrid(config, elem) + '</div>';
		adjustRowHeight(elem, config);
		if((elem.id === 'testDetailGrid' || elem.id === 'testDetailGridPopover') && document.getElementById('unfreezedGrid').clientHeight > 350) {
			var freezeColumnsWidth = document.getElementById('freezedGrid').getBoundingClientRect().width;
			var unfreezeColumnsWidth = document.getElementById('unfreezedGrid').scrollWidth;
			document.getElementById(elem.id).dispatchEvent(vanillaGrid.utils.customEventWrapper('emitContainerDimensions', { freezeColumnsWidth: freezeColumnsWidth, unfreezeColumnsWidth: unfreezeColumnsWidth}));
		}
		document.getElementById(elem.id).dispatchEvent(vanillaGrid.utils.customEventWrapper('gridScrollEventCapture', { detail: { freezeColumnsWidth: freezeColumnsWidth, unfreezeColumnsWidth: unfreezeColumnsWidth }, bubbles: true, composed: true }));
	};

	var adjustRowHeight = function(elem, config) {
		//TODO:: refactor the implementation

		for (var k = 0; k < config.row.length; k++) {
			var cellsPerRow = elem.querySelectorAll('.row[data-rowid="'+ k +'"]');
			var rowHeights = [];
			for (var i = 0; i < cellsPerRow.length; i++) {
				//var height = parseInt(getComputedStyle(cellsPerRow[i]).height, 10);
				var height = cellsPerRow[i].clientHeight;
				height = isNaN(height) ? 0 : height;
				rowHeights.push(height);
			}
			var maxHeight = rowHeights.length && rowHeights.reduce(function(a, b) {
			    return Math.max(a, b);
			});
			maxHeight = maxHeight || 0;
			for (var i = 0; i < cellsPerRow.length; i++) {
				cellsPerRow[i].style.height = maxHeight + 'px';
			}
		}
	};

	var addRow = function(rowResponseObj, config, insertBeforeRowIndex, containerId) {
		var allColumns = document.getElementById(containerId).querySelectorAll('.row-column');
		for(var i = 0; i < allColumns.length; i++) {
			var rowid = config.row.length - 1;
			var column = allColumns[i];
			var columnKey = column.dataset.columnkey;
			if (columnKey) {

				var datastr = '';
				config.dataset && config.dataset.forEach(function(dataObj) {
					datastr = datastr + 'data-' + dataObj.name + '=' + rowResponseObj[dataObj.key];
				});
				var row;

				if (columnKey == 'action' && config.actions && config.actions.length > 0) {
					if(rowResponseObj.hasOwnProperty('canViewIssue') && !row['canViewIssue']) {
						row = '<div class="row canViewIssue"></div>';
					} else {
						row = '<div class="row" ' + datastr + ' data-customevent="' + config.actions[0].customEvent + '" data-orderid="' + rowResponseObj.orderId + '" data-rowid=' + rowid + ' class="action-icon '+ config.actions[0].class +'"><div class="action-outer-wrapper">' + renderActionColumn(config, rowResponseObj) +'</div></div>';
					}
				} else if (columnKey == 'drag') {
					row = '	<div class="draggable-icon row" ' + datastr + ' data-rowid=' + rowid + '> <span draggable="true" data-downloadurl=""> <img /></span></div>';
				} else {
					var header = config.head.filter(function(head){
						return head.key == columnKey;
					})[0];
					if (header) {
						row = '<div class="row" ' + datastr + ' data-columnid="' + header.key + '" data-rowid=' + rowid + '>' + createCell(rowResponseObj, header, config.contextPath, rowid, config) + '</div>';
					}
				}

				var div = document.createElement('div');
				div.innerHTML = row.trim();
				if (insertBeforeRowIndex || insertBeforeRowIndex == '0') {
					rowToAppendAfter = column.querySelector('[data-rowid="' + insertBeforeRowIndex + '"]');
				} else {
					rowToAppendAfter = column.children[column.childElementCount - 1];
				}
			    column.insertBefore(div.firstChild,rowToAppendAfter);
			}
		}
		adjustRowHeightCell(containerId, rowid);
	};

	var deleteRow = function(rowId) {
		var allColumns = document.querySelectorAll('.row-column');
		for(var i = 0; i < allColumns.length; i++) {
			var column = allColumns[i];
			var rowToDelete;
			if (rowId != null || rowId != undefined) {
				rowToDelete = column.querySelector('[data-rowid="' + rowId + '"]');
			} else {
				//deleteing second last row
				rowToDelete = column.children[column.childElementCount - 2];
			}
			if (rowToDelete) {
				column.removeChild(rowToDelete);
			}
		}
	};

	var selectRow = function(rowId, containerId, executionId) {
		var grid = document.getElementById(containerId);

		var selectedList = grid.querySelectorAll('.selected');
		for (var i = 0; i < selectedList.length; i++) {
			selectedList[i].classList.remove('selected');
		}

		var unselectedList = rowId ? grid.querySelectorAll('div[data-rowid="' + rowId + '"]') : grid.querySelectorAll('div[data-executionid="' + executionId + '"]');
		for (var i = 0; i < unselectedList.length; i++) {
			unselectedList[i].classList.add('selected');
		}
	}

	var selectExecutions = function(target, config, containerId, isAllSelected) {
		var grid = document.getElementById(containerId);
		var selectionColumn = grid.querySelector('.selection-body');
		var executionList = vanillaGrid.utils.selectedExecutionIds[containerId];
		// selectionColumn.querySelectorAll('input[type="checkbox"]').forEach(function(column) {
		// 	column.checked = executionList.indexOf(parseInt(column.value)) > -1
		// });
		var selectionCheckbox = selectionColumn.querySelectorAll('input[type="checkbox"]');
		for (var i = 0; i < selectionCheckbox.length; i++) {
			selectionCheckbox[i].checked = executionList.indexOf(parseInt(selectionCheckbox[i].value)) > -1;
		}
		var configAction = config.bulkActions.filter(function(action) {
			return action.customEvent === target.parentElement.dataset.eventname
		})[0];
		if(isAllSelected) {
			target.parentElement.classList.remove('allSelected');
			target.querySelector('img').src = configAction.imgSrc;
		} else {
			target.parentElement.classList.add('allSelected');
			target.querySelector('img').src = configAction.imgSrcChecked;
		}
		if (grid.querySelector('.bulkDelete')) {
			grid.querySelector('.bulkDelete').querySelector('button').disabled = executionList.length ? false : true;
		}
		if (containerId === 'moveExecutionGrid') {
			document.querySelector('#cycle-move-executions-form-submit').disabled = executionList.length ? false : true;
		}
	}

	var maintainAllSelect = function (target, config, containerId) {
		var configAction = config.bulkActions.filter(function(action) {
			return action.customEvent === 'selectRows'
		})[0];
		var grid = document.getElementById(containerId);
		var executionList = vanillaGrid.utils.selectedExecutionIds[containerId];
		if (grid.querySelector('.bulkDelete')) {
			grid.querySelector('.bulkDelete').querySelector('button').disabled = executionList.length ? false : true;
		}
		if (containerId === 'moveExecutionGrid') {
			document.querySelector('#cycle-move-executions-form-submit').disabled = executionList.length ? false : true;
		}
		var currentList = config.row.filter(function(execution){
			return executionList.indexOf(execution.id) > -1;
		});
		if(currentList.length === config.row.length) {
			grid.querySelector('.selectRows').querySelector('img').src = configAction.imgSrcChecked;
		} else {
			grid.querySelector('.selectRows').querySelector('img').src = configAction.imgSrc;
		}
	}

	var redrawCell = function(containerId, row, head, contextPath, rowId, config) {
		var cellContent = createCell(row, head, contextPath, rowId, config);
		var targetEle = document.getElementById(containerId).querySelector('.row-column[data-columnkey="'+head.key+'"]').querySelector('div[data-rowid="' + rowId + '"]');
		if(!targetEle)return;
		if(Object.keys(targetEle.dataset).indexOf('fieldvalue') > -1) {
			var fieldValue = row[head.key];
			targetEle.dataset.fieldvalue = fieldValue;
		}
		targetEle.childElementCount > 0 ? targetEle.removeChild(targetEle.childNodes[0]) : null;
		targetEle.innerHTML = cellContent;
	}

	var partialRender = function(containerId, rowId, cellConfig, config){
		if(!containerId || !rowId) return;
		if(!Array.isArray(cellConfig.header)) {
			redrawCell(containerId, cellConfig.row, cellConfig.header, cellConfig.contextPath, rowId, config)
		} else {
			cellConfig.header.forEach(function(head) {
				redrawCell(containerId, cellConfig.row, head, cellConfig.contextPath, rowId, config)
			})
		}
		adjustRowHeightCell(containerId, rowId);
	}

	var partialRenderReorder = function(steps,stepId, data, offset) {
		var stepIdBefore = data.after ? data.after.split('/').pop() : null;
		var allColumns = document.querySelectorAll('.row-column');
		for(var i = 0; i < allColumns.length; i++) {
			var column = allColumns[i];
			var rowToDelete = column.querySelector('[data-stepid="' + stepId + '"]');
			var rowToAppendAfter;
			if (stepIdBefore) {
				rowToAppendAfter = column.querySelector('[data-stepid="' + stepIdBefore + '"]');
			} else {
				rowToAppendAfter = column.children[column.childElementCount - 1]
			}

			!rowToAppendAfter ? rowToAppendAfter = column.querySelector('[data-stepid="' + -1 + '"]') : null;

			if (rowToAppendAfter && rowToDelete) {
				column.insertBefore(rowToDelete,rowToAppendAfter);
			}

			//updating data-attr
			var children = column.children;
			var isOrderIdColumn = column.getAttribute('data-columnkey') == 'orderId';
			for ( j=0;j<children.length;j++) {
				var child  = children[j];
				child.setAttribute('data-rowid', j);
				if (child.getAttribute('data-orderid')) {
					child.setAttribute('data-orderid', offset + j + 1);
				}
				isOrderIdColumn && children.length != (j + 1)? child.innerHTML = offset + j + 1 : null;
			}
		}
	}

	var updateTestStepsDataSet = function(config, offset) {
		var allColumns = document.querySelectorAll('.row-column');
		for(var i = 0; i < allColumns.length; i++) {
			var column = allColumns[i];

			//updating data-attr
			var children = column.children;
			var isOrderIdColumn = column.getAttribute('data-columnkey') == 'orderId';
			for ( j=0;j<children.length;j++) {
				var child  = children[j];
				child.setAttribute('data-rowid', j);
				if (child.getAttribute('data-orderid')) {
					child.setAttribute('data-orderid', offset + j + 1);
				}
				isOrderIdColumn && children.length != (j + 1)? child.innerHTML = offset + j + 1 : null;
			}
		}
	}

	var adjustRowHeightCell = function(containerId, rowId) {
		var cellsPerRow = document.getElementById(containerId).querySelectorAll('.row[data-rowid="'+ rowId +'"]');
		var rowHeights = [];
		AJS.$(cellsPerRow).each(function(cell) {
			cellsPerRow[cell].removeAttribute('style');
			// var height = parseInt(getComputedStyle(cellsPerRow[cell]).height, 10);
			var height = cellsPerRow[cell].clientHeight;
			height = isNaN(height) ? 0 : height;
			rowHeights.push(height);
		});
		var maxHeight = rowHeights.reduce(function(a, b) {
		    return Math.max(a, b);
		});
		AJS.$(cellsPerRow).each(function(cell) {
			cellsPerRow[cell].style.height = maxHeight + 'px';
		});
	}

	vanillaGrid.templates = {
		initialize: initialize,
		addRow: addRow,
		deleteRow: deleteRow,
		selectRow: selectRow,
		selectExecutions: selectExecutions,
		maintainAllSelect: maintainAllSelect,
		renderColumnChooserOptions: renderColumnChooserOptions,
		getAttachmentThumbnails: getAttachmentThumbnails,
		partialRender: partialRender,
		adjustRowHeightCell: adjustRowHeightCell,
		partialRenderReorder : partialRenderReorder,
		updateTestStepsDataSet : updateTestStepsDataSet
	};

})();
