/* globals AJS, Backbone, _ */
if (typeof ZEPHYR == 'undefined') { ZEPHYR = {}; }

/**
 * Fix for ZFJ-1966
 * Setting the initial loaded serializeObject method if workflow page
 */
if(ZEPHYR.serializeObject && window.location.pathname.indexOf('workflow') > -1) {
    jQuery.fn.serializeObject = ZEPHYR.serializeObject;
}
/**
 * Serializes form fields within the given element to a JSON object
 *
 * {
 *    fieldName: "fieldValue"
 * }
 *
 * @returns {Object}
 */
jQuery.fn.serializeObjectZephyr = function (orderId) {

    var data = {};

    this.find(":input:not(:button):not(:submit):not(:radio):not('select[multiple]')").each(function () {

        if (this.name === "") {
            return;
        }

        if (this.value === null) {
            this.value = "";
        }

        data[this.name] = this.value.match(/^(tru|fals)e$/i) ?
            this.value.toLowerCase() == "true" : this.value;
    });
    if(this.find(":input:not(:button):not(:submit):not(:radio):not('select[multiple]')")) {
    	var trLength = this.parents('#teststep-table').find('tbody.ui-sortable tr.aui-restfultable-readonly').length;
    	var _orderId = (orderId) ? orderId : (trLength + 1);
    	//data['id'] = _orderId;
    	data['orderId'] = _orderId;
    }

    this.find("input:radio:checked").each(function(){
        data[this.name] = this.value;
    });

    this.find("select[multiple]").each(function(){

        var $select = jQuery(this),
            val = $select.val();

        if ($select.data("aui-ss")) {
            if (val) {
                data[this.name] = val[0];
            } else {
                data[this.name] = "";
            }
        } else {

            if (val !== null) {
                data[this.name] = val;
            } else {
                data[this.name] = [];
            }
        }
    });

    return data;
};

(function ($) {

    /**
     * A table whose entries/rows can be retrieved, added and updated.
     * It uses backbone.js to sync the table's state back to the server, avoiding page refreshes.
     *
     * @class RestfulTable
     */
	ZEPHYR.RestfulTable = AJS.RestfulTable.extend({
        initialize: function (options) {

            var instance = this;
            // combine default and user options
            instance.options = $.extend(true, instance._getDefaultOptions(options), options);
            // Prefix events for this instance with this id.
            instance.id = this.options.id;

            // faster lookup
            instance._event = AJS.RestfulTable.Events;
            instance.classNames = AJS.RestfulTable.ClassNames;
            instance.dataKeys = AJS.RestfulTable.DataKeys;

            // shortcuts to popular elements
            this.$table = $(options.el)
                .addClass(this.classNames.RESTFUL_TABLE)
                .addClass(this.classNames.ALLOW_HOVER)
                .addClass("aui")
                .addClass(instance.classNames.LOADING);

          //  this.$table.wrapAll("<form class='aui' action='#' />"); // Removing the wrapping to form since in admin pages the form submission happens

            this.$thead = $("<thead/>");
            this.$theadRow = $("<tr />").appendTo(this.$thead);
            this.$tbody = $("<tbody/>");

            if (!this.$table.length) {
                throw new Error("AJS.RestfulTable: Init failed! The table you have specified [" + this.$table.selector + "] cannot be found.")
            }

            if (!this.options.columns) {
                throw new Error("AJS.RestfulTable: Init failed! You haven't provided any columns to render.")
            }

            // Let user know the table is loading
            this.showGlobalLoading();

            $.each(this.options.columns, function (i, column) {
                var header = $.isFunction(column.header) ? column.header() : column.header;
                if (typeof header === "undefined") {
                    console.warn("You have not specified [header] for column [" + column.id + "]. Using id for now...");
                    header = column.id;
                }
                instance.$theadRow.append("<th class='zephyr_create_test_" + column.id + "_th'>" + header + "</th>");
            });
            // create a new Backbone collection to represent rows (http://documentcloud.github.com/backbone/#Collection)
            this._models = this._createCollection();

            // shortcut to the class we use to create rows
            this._rowClass = this.options.views.row;

            this.editRows = []; // keep track of rows that are being edited concurrently


            // Add the refresh button
            var refreshHTML = ''; // '<a style="float: right;" title="Refresh results" class="refresh-table" id="refresh-teststeps" href="javascript:void(0)"><span class="aui-icon aui-icon-small aui-iconfont-build">Refresh</span></a>';

            // columns for submit buttons and loading indicator used when editing
            instance.$theadRow.append('<th>' + refreshHTML + '</th><th></th>');
            if (this.options.allowReorder) {

                // Add allowance for another cell to the thead
                this.$theadRow.prepend("<th />");

                this.attachReorderUI();
            }


            if (this.options.allowCreate !== false) {

                // Create row responsible for adding new entries ...
                this._createRow = new this.options.views.editRow({
                    columns: this.options.columns,
                    isCreateRow: true,
                    model: this.options.model.extend({
                        url: function () {
                            return instance.options.resources.self;
                        }
                    }),
                    cancelAccessKey: this.options.cancelAccessKey,
                    submitAccessKey: this.options.submitAccessKey,
                    allowReorder: this.options.allowReorder,
                    fieldFocusSelector: this.options.fieldFocusSelector
                })
                    .bind(this._event.CREATED, function (values) {
                        if ((instance.options.addPosition == undefined && instance.options.createPosition === "bottom")
                            || instance.options.addPosition === "bottom") {
                            instance.addRow(values);
                        } else {
                            instance.addRow(values, 0);
                        }
                    })
                    .bind(this._event.VALIDATION_ERROR, function () {
                        this.trigger(instance._event.FOCUS);
                    })
                    .render({
                        errors: {},
                        values: {}
                    });

                // ... and appends it as the first row
                this.$create = $('<tbody class="' + this.classNames.CREATE + '" />')
                    .append(this._createRow.el);

                // Manage which row has focus
                this._applyFocusCoordinator(this._createRow);

                // focus create row
                this._createRow.trigger(this._event.FOCUS);
            }

            // when a model is removed from the collection, remove it from the viewport also
            this._models.bind("remove", function (model) {
                $.each(instance.getRows(), function (i, row) {
                    if (row.model === model) {
                        if (row.hasFocus() && instance._createRow) {
                            instance._createRow.trigger(instance._event.FOCUS);
                        }
                        instance.removeRow(row);
                    }
                });
            });
            this.fetchInitialResources();
            // On clone update the UI
            AJS.bindEvt(AJS.RestfulTable.Events.CLONE, function (ev, values) {
            	instance.$el.find('tbody.ui-sortable').html('');
            	instance.populate(values);
            });
            // On delete update the UI
            AJS.bindEvt(ZEPHYR.RestfulTable.Events.DELETE_SUCCESS, function (ev, values) {
            	instance.$el.find('tbody.ui-sortable').html('');
            	_.each(values, function(value, i) {
            		value.orderId = (i+1);
            	});
            	instance.populate(values);
            });
            // Reorder
            AJS.bindEvt(AJS.RestfulTable.Events.REORDER_SUCCESS, function (ev, values) {
            	instance.$el.find('tbody.ui-sortable').html('');
            	instance.populate(values);
            	instance.$tbody.sortable("enable");
            });
            AJS.bindEvt(ZEPHYR.RestfulTable.Events.RENDER_DATA, function(ev, values) {
                instance.options.fetchAll = false;
            });
        },

        reorderEntries: function(model, position) {
        	this._models.remove(model);

            this._models.each(function (_model, index) {
                var orderId = index;
                if (index >= position) {
                	orderId += 1;
                }
                _model.set('orderId', (orderId + 1));
            });
            model.set('orderId', (position+1));
            this._models.add(model, {at: position});
        	// TODO
        	AJS.triggerEvtForInst(this._event.REORDER_SUCCESS, this, [this._models.toJSON()]);
        },

        _createCollection: function() {
            var instance = this;

            // create a new Backbone collection to represent rows (http://documentcloud.github.com/backbone/#Collection)
            var rowsAwareCollection = this.options.Collection.extend({
                // Force the collection to re-sort itself. You don't need to call this under normal
                // circumstances, as the set will maintain sort order as each item is added.
                sort:function (options) {
                    options || (options = {});
                    if (!this.comparator) {
                        throw new Error('Cannot sort a set without a comparator');
                    }
                    this.tableRows = instance.getRows();
                    this.models = this.sortBy(this.comparator);
                    this.tableRows = undefined;
                    if (!options.silent) {
                        this.trigger('refresh', this, options);
                    }
                    return this;
                },
                remove:function (models, options) {
                    this.tableRows = instance.getRows();
                    Backbone.Collection.prototype.remove.apply(this, arguments);
                    this.tableRows = undefined;
                    return this;
                }
            });

            return new rowsAwareCollection([], {
                comparator:function (row) {
                    // sort model in collection based on dom ordering
                    var index;
                    $.each(this.tableRows !== undefined ? this.tableRows : instance.getRows(), function (i) {
                        if (this.model.orderId === row.orderId) {
                            index = i;
                            return false;
                        }
                    });
                    return index;
                }
            });
        },
        fetchInitialResources: function() {
        	var instance = this;

       	 	if ($.isFunction(this.options.resources.all)) {
                this.options.resources.all(function (entries) {
                    instance.populate(entries);
                });
            } else {
            	if(this.options.isEditDialog) {
            		$.get(instance.options.resources.all)
	                    .success(function (entries) {
                            if(entries && entries.length) {
    	                    	instance.options.fetchAll = true;
                            }
	                        instance.populate(entries);
	                    });
            	} else {
	            	var entries = [];
	            	this.populate(entries);
            	}
            }
       },

        attachReorderUI: function() {
        	var instance = this;

        	// Allow drag and drop reordering of rows
            this.$tbody.sortable({
                handle: "." +this.classNames.DRAG_HANDLE,
                helper: function(e, elt) {
                    var helper = $("<div/>").attr("class", elt.attr("class")).addClass(instance.classNames.MOVEABLE);
                    elt.children().each(function (i) {
                        var $td = $(this);
                        var width = $td.outerWidth();
                        helper.append($("<div/>").html($td.html()).attr("class", $td.attr("class")).width(width));
                    });
                    helper = $("<div class='aui-restfultable-readonly'/>").append(helper); // Basically just to get the styles.
                    helper.css({left: elt.offset().left}); // To align with the other table rows, since we've locked scrolling on x.
                    helper.appendTo(document.body);
                    return helper;
                },
                start: function (event, ui) {
                    var cachedHeight = ui.helper[0].clientHeight;
                    var $this = ui.placeholder.find("td");

                    // Make sure that when we start dragging widths do not change
                    ui.item
                        .addClass(instance.classNames.MOVEABLE)
                        .children().each(function (i) {
                            $(this).width($this.eq(i).width());
                        });

                    // Create a <td> to add to the placeholder <tr> to inherit CSS styles.
                    var td = '<td colspan="' + instance.getColumnCount() + '">&nbsp;</td>';

                    ui.placeholder.html(td).css({
                        height: cachedHeight,
                        visibility: 'visible'
                    });

                    // Stop hover effects etc from occuring as we move the mouse (while dragging) over other rows
                    instance.getRowFromElement(ui.item[0]).trigger(instance._event.MODAL);
                },
                stop: function (event, ui) {
                    if (AJS.$(ui.item[0]).is(":visible")) {
                        ui.item
                            .removeClass(instance.classNames.MOVEABLE)
                            .children().attr("style", "");

                        ui.placeholder.removeClass(instance.classNames.ROW);

                        // Return table to a normal state
                        instance.getRowFromElement(ui.item[0]).trigger(instance._event.MODELESS);
                    }
                },
                update: function (event, ui) {

                    var nextModel,
                        nextRow,
                        data = {},
                        row = instance.getRowFromElement(ui.item[0]);

                    if (row) {
                    	var _model = row.model;
                        instance.reorderEntries(_model, ui.item.index());

                        // shows loading indicator (spinner)
                        row.showLoading();
                        row.trigger(instance._event.MODELESS);
                    }
                },
                axis: "y",
                delay: 0,
                containment: "document",
                cursor: "move",
                scroll: true,
                zIndex: 8000
            });

            // Prevent text selection while reordering.
            this.$tbody.bind("selectstart mousedown", function (event) {
                return !$(event.target).is("." + instance.classNames.DRAG_HANDLE);
            });
        },

        /**
         * Refreshes table with entries
         *
         * @param entries
         */
        populate: function (entries) {
             var _customFieldId = AJS.$('#zephyr-je-customfield-id').val();

            if (this.options.reverseOrder) {
                entries.reverse();
            }

            this.hideGlobalLoading();
            if (entries && entries.length) {
                // Empty the model collection
                this._models.reset([], { silent: true });
                // Add all the entries to collection and render them
                this.renderRows(entries);
                // show message to user if we have no entries
                if (this.isEmpty()) {
                    this.showNoEntriesMsg();
                }
            } else {
                this.showNoEntriesMsg();
            }

            AJS.$('input#' + _customFieldId + '-delete-all').remove();
            if(!ZEPHYR.ISSUE.Create.Teststep.noChange) {
                ZEPHYR.ISSUE.Create.Teststep.removeNoChangeElement();
            }
            // Insert customfield input element with 'deleteAll'value in case of empty fields
            if(entries.length == 0 && !ZEPHYR.ISSUE.Create.Teststep.noChange) {
                this.showDeleteAllEntries(_customFieldId);
            }
            if(ZEPHYR.ISSUE.Create.Teststep.noChange) {
                ZEPHYR.ISSUE.Create.Teststep.noChange = false;
            }
            // Ok, lets let everyone know that we are done...
            this.$table
                .append(this.$thead);

            if (this.options.createPosition === "bottom") {
                this.$table.append(this.$tbody)
                    .append(this.$create);
            } else {
                this.$table
                    .append(this.$create)
                    .append(this.$tbody);
            }

            this.$table.removeClass(this.classNames.LOADING)
                .trigger(this._event.INITIALIZED, [this]);

            AJS.triggerEvtForInst(this._event.INITIALIZED, this, [this]);

            if (this.options.autoFocus) {
                this.$table.find(":input:text:first").focus(); // set focus to first field
            }
        },

        showDeleteAllEntries: function (_customFieldId) {
             var _parsedData = ZEPHYR.ISSUE.Create.Teststep.setStepJSON(ZEPHYR.ISSUE.Create.TESTSTEP_DATA_DELETE_ALL, {});

            if (this.$noEntries) {
                this.$noEntries.remove();
            }

            this.$deleteAllEntries = $("<input>")
                    .attr("type", 'hidden')
                    .attr('id',  _customFieldId + '-delete-all')
                    .attr('name', _customFieldId)
                    .val(JSON.stringify(_parsedData))
                    .appendTo(this.$tbody);

            return this;
        },

        /**
         * Adds row to collection and renders it
         *
         * @param {Object} values
         * @param {number} index
         * @return {AJS.RestfulTable}
         */
        addRow: function (values, index) {
            var view,
                model;

            model = new this.options.model(values);
            view = this._renderRow(model, index);
            this._models.add(model);
            this.removeNoEntriesMsg();
            // Let everyone know we added a row
            AJS.triggerEvtForInst(this._event.ROW_ADDED, this, [view, this]);
            return this;
        },

        removeRow: function (row) {

            this._models.remove(row.model);
            row.remove();

            if (this.isEmpty()) {
                this.showNoEntriesMsg();
            }

            // Let everyone know we removed a row
            AJS.triggerEvtForInst(this._event.ROW_REMOVED, this, [row, this]);
        },


        /**
         * Shows message {options.noEntriesMsg} to the user if there are no entries
         *
         * @return {AJS.RestfulTable}
         */
        showNoEntriesMsg: function () {

            if (this.$noEntries) {
                this.$noEntries.remove();
            }

            this.$noEntries = $("<tr>")
                .addClass(this.classNames.NO_ENTRIES)
                .append($("<td>")
                    .attr("colspan", this.getColumnCount())
                    .text(this.options.noEntriesMsg)
                )
                .appendTo(this.$tbody);

            return this;
        },

        /**
         * Removes message {options.noEntriesMsg} to the user if there ARE entries
         *
         * @return {AJS.RestfulTable}
         */
        removeNoEntriesMsg: function () {
            if (this.$noEntries && this._models.length > 0) {
                this.$noEntries.remove();
            }
            return this;
        },

        /**
         * Gets the AJS.RestfulTable.Row from their associated <tr> elements
         *
         * @return {Array<AJS.RestfulTable.Row>}
         */
        getRows: function () {

            var instance = this,
                views = [];

            this.$tbody.find("." + this.classNames.READ_ONLY).each(function () {

                var $row = $(this),
                    view = $row.data(instance.dataKeys.ROW_VIEW);

                if (view) {
                    views.push(view);
                }
            });

            return views;
        },

        /**
         * Appends entry to end or specified index of table
         *
         * @param {AJS.RestfulTable.EntryModel} model
         * @param index
         * @return {jQuery}
         */
        _renderRow: function (model, index) {

            var instance = this,
                $rows = this.$tbody.find("." + this.classNames.READ_ONLY),
                $row,
                view,
                _customFieldId = AJS.$('#zephyr-je-customfield-id').val();

            view = new this._rowClass({
                model: model,
                columns: this.options.columns,
                allowEdit: this.options.allowEdit,
                allowDelete: this.options.allowDelete,
                allowReorder: this.options.allowReorder,
                deleteConfirmation: this.options.deleteConfirmation,
                cloneConfirmation: this.options.cloneConfirmation,
                fetchAll: this.options.fetchAll,
                rowOperations: this.options.rowOperations			// Customize the row operations
            });

            this.removeNoEntriesMsg();
            AJS.$('input#' + _customFieldId + '-delete-all').remove();
            if(!ZEPHYR.ISSUE.Create.Teststep.noChange) {
                ZEPHYR.ISSUE.Create.Teststep.removeNoChangeElement();
            }
            if(ZEPHYR.ISSUE.Create.Teststep.noChange) {
                ZEPHYR.ISSUE.Create.Teststep.noChange = false;
            }
            view.bind(this._event.ROW_EDIT, function (field) {
                AJS.triggerEvtForInst(this._event.EDIT_ROW, {}, [this, instance]);
                instance.edit(this, field);
            });

            $row = view.render().$el;

            if (index !== -1) {

                if (typeof index === "number" && $rows.length !== 0) {
                    $row.insertBefore($rows[index]);
                } else {
                    this.$tbody.append($row);
                }
            }

            $row.data(this.dataKeys.ROW_VIEW, view);

            // deactivate all rows - used in the cases, such as opening a dropdown where you do not want the table editable
            // or any interactions
            view.bind(this._event.MODAL, function () {
                instance.$table.removeClass(instance.classNames.ALLOW_HOVER);
                instance.$tbody.sortable("disable");
                $.each(instance.getRows(), function () {
                    if (!instance.isRowBeingEdited(this)) {
                        this.delegateEvents({}); // clear all events
                    }
                });
            });

            view.bind(this._event.ANIMATION_STARTED, function () {
                instance.$table.removeClass(instance.classNames.ALLOW_HOVER);
            });

            view.bind(this._event.ANIMATION_FINISHED, function () {
                instance.$table.addClass(instance.classNames.ALLOW_HOVER);
            });

            // activate all rows - used in the cases, such as opening a dropdown where you do not want the table editable
            // or any interactions
            view.bind(this._event.MODELESS, function () {
                instance.$table.addClass(instance.classNames.ALLOW_HOVER);
                instance.$tbody.sortable("enable");
                $.each(instance.getRows(), function () {
                    if (!instance.isRowBeingEdited(this)) {
                        this.delegateEvents(); // rebind all events
                    }
                });
            });

            // ensure that when this row is focused no other are
            this._applyFocusCoordinator(view);

            this.trigger(this._event.ROW_INITIALIZED, view);

            return view;
        },

        /**
         * Returns if the row is edit mode or note
         *
         * @param {AJS.RestfulTable.Row} - read onyl row to check if being edited
         * @return {Boolean}
         */
        isRowBeingEdited: function (row) {

            var isBeingEdited = false;

            $.each(this.editRows, function () {
                if (this.el === row.el) {
                    isBeingEdited = true;
                    return false;
                }
            });

            return isBeingEdited;
        },

        /**
         * Ensures that when supplied view is focused no others are
         *
         * @param {Backbone.View} view
         * @return {AJS.RestfulTable}
         */
        _applyFocusCoordinator: function (view) {

            var instance = this;

            if (!view.hasFocusBound) {

                view.hasFocusBound = true;

                view.bind(this._event.FOCUS, function () {
                    if (instance.focusedRow && instance.focusedRow !== view) {
                        instance.focusedRow.trigger(instance._event.BLUR);
                    }
                    instance.focusedRow = view;
                    if (view instanceof AJS.RestfulTable.Row && instance._createRow) {
                        instance._createRow.enable();
                    }
                });
            }

            return this;
        },

        /**
         * Remove specificed row from collection holding rows being concurrently edited
         *
         * @param {AJS.RestfulTable.EditRow} editView
         * @return {AJS.RestfulTable}
         */
        _removeEditRow: function (editView) {
            var index = $.inArray(editView, this.editRows);
            this.editRows.splice(index, 1);
            return this;
        },

        /**
         * Focuses last row still being edited or create row (if it exists)
         *
         * @return {AJS.RestfulTable}
         */
        _shiftFocusAfterEdit: function () {

            if (this.editRows.length > 0) {
                this.editRows[this.editRows.length-1].trigger(this._event.FOCUS);
            } else if (this._createRow) {
                this._createRow.trigger(this._event.FOCUS);
            }

            return this;
        },

        /**
         * Evaluate if we save row when we blur. We can only do this when there is one row being edited at a time, otherwise
         * it causes an infinate loop JRADEV-5325
         *
         * @return {boolean}
         */
        _saveEditRowOnBlur: function () {
            return this.editRows.length <= 1;
        },

        /**
         * Dismisses rows being edited concurrently that have no changes
         */
        dismissEditRows: function () {
            var instance = this;
            $.each(this.editRows, function () {
                if (!this.hasUpdates()) {
                    this.trigger(instance._event.FINISHED_EDITING);
                }
            });
        },

        /**
         * Converts readonly row to editable view
         *
         * @param {Backbone.View} row
         * @param {String} field - field name to focus
         * @return {Backbone.View} editRow
         */
        edit: function (row, field) {

            var instance = this,
                editRow = new this.options.views.editRow({
                    el: row.el,
                    columns: this.options.columns,
                    isUpdateMode: true,
                    allowReorder: this.options.allowReorder,
                    fieldFocusSelector: this.options.fieldFocusSelector,
                    model: row.model,
                    cancelAccessKey: this.options.cancelAccessKey,
                    submitAccessKey: this.options.submitAccessKey
                }),
                values = row.model.toJSON();
            values.update = true;
            editRow.render({
                errors: {},
                update: true,
                values: values
            })
                .bind(instance._event.UPDATED, function (model, focusUpdated) {
                    instance._removeEditRow (this);
                    this.unbind();
                    row.render().delegateEvents(); // render and rebind events
                    row.trigger(instance._event.UPDATED); // trigger blur fade out
                    if (focusUpdated !== false) {
                        instance._shiftFocusAfterEdit();
                    }
                })
                .bind(instance._event.VALIDATION_ERROR, function () {
                    this.trigger(instance._event.FOCUS);
                })
                .bind(instance._event.FINISHED_EDITING, function () {
                    instance._removeEditRow(this);
                    row.render().delegateEvents();
                    this.unbind();  // avoid any other updating, blurring, finished editing, cancel events being fired
                })
                .bind(instance._event.CANCEL, function () {
                    instance._removeEditRow(this);
                    this.unbind();  // avoid any other updating, blurring, finished editing, cancel events being fired
                    row.render().delegateEvents(); // render and rebind events
                    instance._shiftFocusAfterEdit();
                })
                .bind(instance._event.BLUR, function () {
                    instance.dismissEditRows(); // dismiss edit rows that have no changes
                    if (instance._saveEditRowOnBlur()) {
                        this.trigger(instance._event.SAVE, false);  // save row, which if successful will call the updated event above
                    }
                });

            // Ensure that if focus is pulled to another row, we blur the edit row
            this._applyFocusCoordinator(editRow);

            // focus edit row, which has the flow on effect of blurring current focused row
            editRow.trigger(instance._event.FOCUS, field);

            // disables form fields
            if (instance._createRow) {
                instance._createRow.disable();
            }

            this.editRows.push(editRow);

            return editRow;
        },


        /**
         * Renders all specified rows
         *
         * @param {Array} array of objects describing Backbone.Model's to render
         * @return {AJS.RestfulTable}
         */
        renderRows: function (rows) {
            var comparator = this._models.comparator, els = [];

            this._models.comparator = undefined; // disable temporarily, assume rows are sorted

            var models = _.map(rows, function(row) {
                var model = new this.options.model(row);
                els.push(this._renderRow(model, -1).el);
                return model;
            }, this);
            this._models.add(models, {silent:true});

            this._models.comparator = comparator;

            this.removeNoEntriesMsg();

            this.$tbody.append(els);

            return this;
        },

        /**
         * Gets default options
         *
         * @param {Object} options
         */
        _getDefaultOptions: function (options) {
            return {
                model: options.model || ZEPHYR.RestfulTable.EntryModel,
                allowEdit: true,
                views: {
                    editRow: ZEPHYR.RestfulTable.EditRow,
                    row: ZEPHYR.RestfulTable.Row
                },
                Collection: Backbone.Collection.extend({
                    url: options.resources.self,
                    model: options.model || ZEPHYR.RestfulTable.EntryModel
                }),
                allowReorder: false,
                fieldFocusSelector: function(name) {
                    return ":input[name=" + name + "], #" + name;
                },
                loadingMsg: options.loadingMsg || "Loading"
            }
        }

    });

})(AJS.$);

(function ($) {
	ZEPHYR.RestfulTable.Events = {
		DELETE_SUCCESS: 'DELETE_SUCCESS',
		RENDER_DATA: 'RENDER_DATA'
	};

    /**
     * A class provided to fill some gaps with the out of the box Backbone.Model class. Most notiably the inability
     * to send ONLY modified attributes back to the server.
     *
     * @class EntryModel
     * @namespace AJS.RestfulTable
     */
    ZEPHYR.RestfulTable.EntryModel = AJS.RestfulTable.EntryModel.extend({
    	initialize: function(options) {
    		AJS.RestfulTable.EntryModel.prototype.initialize.call(this, options);
    	},

        /**
         * Overrides default save handler to only save (send to server) attributes that have changed.
         * Also provides some default error handling.
         *
         * @override
         * @param attributes
         * @param options
         */
        save: function (attributes, options) {


            options = options || {};

            var instance = this,
                Model,
                syncModel,
                error = options.error, // we override, so store original
                success = options.success;


            // override error handler to provide some defaults
            options.error = function (model, xhr) {

                var data = $.parseJSON(xhr.responseText || xhr.data);

                // call original error handler
                if (error) {
                    error.call(instance, instance, data, xhr);
                }
            };

            // if it is a new model, we don't have to worry about updating only changed attributes because they are all new
            if (this.isNew()) {

                // call super
                Backbone.Model.prototype.save.call(this, attributes, options);

                // only go to server if something has changed
            } else if (attributes) {

                // create temporary model
                Model = AJS.RestfulTable.EntryModel.extend({
                    url: this.url()
                });

                syncModel = new Model({
                    id: this.id
                });

                syncModel.save = Backbone.Model.prototype.save;

                options.success = function (model, xhr) {

                    // update original model with saved attributes
                    instance.clear().set(model.toJSON());

                    // call original success handler
                    if (success) {
                        success.call(instance, instance, xhr);
                    }
                };

                // update temporary model with the changed attributes
                syncModel.save(attributes, options);
            }
        }
    });

})(AJS.$);

(function ($) {

    /**
     * An abstract class that gives the required behaviour for the creating and editing entries. Extend this class and pass
     * it as the {views.row} property of the options passed to AJS.RestfulTable in construction.
     *
     * @class EditRow
     * @namespace AJS.RestfulTable
     */
    ZEPHYR.RestfulTable.EditRow = AJS.RestfulTable.EditRow.extend({

        tagName: "tr",

        // delegate events
        events: {
            "focusin" : "_focus",
            "click" : "_focus",
            "keyup" : "_handleKeyUpEvent"
        },

        /**
         * @constructor
         * @param {Object} options
         */
        initialize: function (options) {

            this.$el = $(this.el);
            this.fieldFocusSelector = options.fieldFocusSelector;
            AJS.RestfulTable.EditRow.prototype.initialize.call(this, options);
        },

        /**
         * Renders default cell contents
         *
         * @param data
         */
        defaultColumnRenderer: function (data) {
            if (data.allowEdit !== false) {
            	var _textArea = $("<textarea />")
	                .addClass("textarea ztextarea noresize")
	                .attr({
	                    name: data.name,
	                    value: data.value,
	                    rows: 4
	                })
	                .css({
	                'width': '100%',
                    'height': '150px',
                    'min-height': '150px',
                    'overflow-y' : 'hidden'
	                })
	                .on('keyup', this.resizeTextarea);

            	return _textArea;
            } else if (data.value) {
                return document.createTextNode(data.value);
            }
        },

        /**
         * Add resize textarea capability
         */
        resizeTextarea: function() {
    		if(this.scrollHeight > 1) {
    			// this.style.height = 'auto';
    			this.style.height = (this.scrollHeight - 2)+'px';
    			AJS.$(this).parent().height(this.scrollHeight + 5);
    		}
    	},

        /**
         * Focus specified field (by name or id - first argument), first field with an error or first field (DOM order)
         *
         * @param name
         * @return AJS.RestfulTable.EditRow
         */
        focus: function (name) {

            var $focus,
                $error;

            this.enable();

            if (name) {
                $focus = this.$el.find(this.fieldFocusSelector(name));
            } else {

                $error = this.$el.find(this.classNames.ERROR + ":first");

                if ($error.length === 0) {
                    $focus = this.$el.find(":input:text:first");
                } else {
                    $focus = $error.parent().find(":input");
                }
            }

            this.$el.addClass(this.classNames.FOCUSED);

            $focus.focus().trigger("select");

            return this;
        },

        /**
         * If any of the fields have changed
         * @return {Boolean}
         */
        hasUpdates: function () {
            return !!this.mapSubmitParams(this.serializeObject());
        },

        /**
         * Serializes the view into model representation.
         * Default implementation uses simple jQuery plugin to serialize form fields into object
         * @return Object
         */
        serializeObject: function(orderId) {
            return this.$el.serializeObjectZephyr(orderId);
        },

        mapSubmitParams: function (params) {
            return this.model.changedAttributes(params);
        },

        /**
         *
         * Handle submission of new entries and editing of old.
         *
         * @param {Boolean} focusUpdated - flag of whether to focus read-only view after succssful submission
         * @return AJS.RestfulTable.EditRow
         */
        submit: function (focusUpdated) {


            var instance = this,
                values;

            // IE doesnt like it when the focused element is removed

            if (document.activeElement !== window) {
                $(document.activeElement).blur();
            }

            if (this.isUpdateMode) {
            	var orderId = this.model.get('orderId');
                values = this.mapSubmitParams(this.serializeObject(orderId)); // serialize form fields into JSON

                if (!values) {
                    return instance.trigger(instance._event.CANCEL);
                }
            } else {
                this.model.clear();
                values = this.mapSubmitParams(this.serializeObject(0)); // serialize form fields into JSON
            }

            this.trigger(this._event.SUBMIT_STARTED);

            /* Attempt to add to server model. If fail delegate to createView to render errors etc. Otherwise,
             add a new model to this._models and render a row to represent it. */
            this.model.set(values);
            if (this.isUpdateMode) {
                instance.trigger(instance._event.UPDATED, instance.model, focusUpdated);
            } else {
                instance.trigger(instance._event.CREATED, instance.model.toJSON());
                instance.model = new instance._modelClass(); // reset
                instance.render({errors: {}, values: {}}); // pulls in instance's model for create row
                instance.$el.find('textarea[name="step"]').trigger(instance._event.FOCUS);
            }
            instance.trigger(instance._event.SUBMIT_FINISHED);

            return this;
        },

        /**
         * Handles rendering of row
         *
         * @param {Object} renderData
         * ... {Object} vales - Values of fields
         */
        render: function  (renderData) {

            var instance = this;

            this.$el.empty();

            if (this.allowReorder) {
                $('<td  class="' + this.classNames.ORDER + '" />').append(this.renderDragHandle()).appendTo(instance.$el);
            }

            $.each(this.columns, function (i, column) {

                var contents,
                    $cell,
                    value = renderData.values[column.id],
                    args = [
                        {name: column.id, value: value, allowEdit: column.allowEdit},
                        renderData.values,
                        instance.model
                    ];

                if (value) {
                    instance.$el.attr("data-" + column.id, value); // helper for webdriver testing
                }

                if (instance.isCreateRow && column.createView) {
                    // TODO AUI-1058 - The row's model should be guaranteed to be in the correct state by this point.
                    contents = new column.createView({
                        model: instance.model
                    }).render(args[0]);

                } else if (column.editView) {
                    contents = new column.editView({
                        model: instance.model
                    }).render(args[0]);
                } else {
                    contents = instance.defaultColumnRenderer.apply(instance, args);
                }

                if(column.allowEdit !== false) {
                	$cell = $("<td class='zephyr-wiki-edit' />");
                } else {
                	$cell = $("<td />");
                }
                if (typeof contents === "object" && contents.done) {
                    contents.done(function (contents) {
                        $cell.append(contents);
                    });
                } else {
                    $cell.append(contents);
                }

                if (column.styleClass) {
                    $cell.addClass(column.styleClass);
                }

                $cell.appendTo(instance.$el);

                // Attaching wiki
                if(column.allowEdit !== false) {
                	ZEPHYR.wikiEditor.init($cell);
                	// Resize the textarea based on content
                    if($cell.find('textarea'))
                    	instance.resizeTextarea.apply($cell.find('textarea')[0]);
                }
            });

            this.$el
                .append(this.renderOperations(renderData.update, renderData.values)) // add submit/cancel buttons
                .addClass(this.classNames.ROW + " " + this.classNames.EDIT_ROW);

            this.trigger(this._event.RENDER, this.$el, renderData.values);

            this.$el.trigger(this._event.CONTENT_REFRESHED, [this.$el]);

            return this;
        },

        /**
         *
         * Gets markup for add/update and cancel buttons
         *
         * @param {Boolean} update
         */
        renderOperations: function (update) {

            var $operations = AJS.$('<td class="zephyr-aui-restfultable-operations" />');
            var instance = this;
            if (update) {
                $operations.append($('<input class="aui-button" type="button" />').attr({
                        accesskey: this.submitAccessKey,
                        value: AJS.I18n.getText('common.forms.update')
                    }).bind('click', function(ev) {
                    	ev.preventDefault();
                    	instance.trigger(instance._event.SAVE);
                    }))
                    .append($('<a class="aui-button aui-button-link" href="#" />')
                        .addClass(this.classNames.CANCEL)
                        .text(AJS.I18n.getText('common.forms.cancel'))
                        .attr({
                            accesskey:  this.cancelAccessKey
                        }).bind('click', function(ev) {
                        	ev.preventDefault();
                        	instance.trigger(instance._event.CANCEL)
                        }));
            } else {
                $operations.append($('<input class="aui-button" type="button" />').attr({
                    accesskey: this.submitAccessKey,
                    value: AJS.I18n.getText('common.forms.add')
                }).bind('click', function(ev) {
                	ev.preventDefault();
                	instance.trigger(instance._event.SAVE);
                }))
            }
            return $operations.add($('<td class="aui-restfultable-status" />').append(ZEPHYR.RestfulTable.throbber()));
        }
    });

})(AJS.$);

(function ($) {

    /**
     * An abstract class that gives the required behaviour for RestfulTable rows.
     * Extend this class and pass it as the {views.row} property of the options passed to AJS.RestfulTable in construction.
     *
     * @class Row
     * @namespace AJS.RestfulTable
     */
	ZEPHYR.RestfulTable.Row = AJS.RestfulTable.Row.extend({
        // Static Const
        tagName: "tr",

        // delegate events
        events: {
            "click .aui-restfultable-editable" : "edit"
        },

        /**
         * @constructor
         * @param {object} options
         */
        initialize: function (options) {

            var instance = this;

            options = options || {};

            AJS.RestfulTable.Row.prototype.initialize.call(this, options);
        },

        /**
         * Renders default cell contents
         *
         * @param data
         * @return {undefiend, String}
         */
        defaultColumnRenderer: function (data) {
            if (data.allowEdit !== false && data.value && !this.options.fetchAll) {
            	var mkEl = document.createElement('div');
            	// wiki to html
    			if(data.value) {
	            	AJS.$.ajax({
	        	 	   type: 'POST',
	        	 	   url: getRestURL() + "/util/render",
	        	 	   data: JSON.stringify({
	        	 		   "rendererType": "zephyr-wiki-renderer",
	        	 		   "unrenderedMarkup": data.value
	        	 	   }),
	        	 	   contentType: "application/json",
	        	 	   success: function(response) {
	        	 		  mkEl.innerHTML = response.renderedHTML;
	        	 	   }
	            	});
            	}

                return mkEl;
            } else if(data.value) {
                var mkEl = document.createElement('div');
                mkEl.innerHTML = data.value.toString();
            	return mkEl;
            }
        },

        /**
         * Fades row from blue to transparent
         */
        _showUpdated: function () {

            var instance = this,
                cells = this.$el
                    .addClass(this.classNames.ANIMATING)
                    .find("td")
                    .css("backgroundColor","#ebf1fd");

            this.trigger(this._event.ANIMATION_STARTED);

            instance.delegateEvents({});

            setTimeout(function () {
                cells.animate({
                    backgroundColor: "white"
                }, function () {
                    cells.css("backgroundColor", "");
                    instance.trigger(instance._event.ANIMATION_FINISHED);
                    $(document).one("mousemove", function () {
                        instance.delegateEvents();
                        instance.$el.removeClass(instance.classNames.ANIMATING);
                    });
                });
            }, 500)
        },

        /**
         * Get model from server and re-render
         *
         * @return {AJS.RestfulTable.Row}
         */
        refresh: function (success, error) {

            var instance = this;

            this.showLoading();

            this.model.fetch({
                success: function () {
                    instance.hideLoading().render();
                    if (success) {
                        success.apply(this, arguments);
                    }
                },
                error: function () {
                    if(response.status === 401){
                        ZEPHYR.TEST.showUnautorizedError();
                    }
                    else{
                        instance.hideLoading();
                        if (error) {
                            error.apply(this, arguments);
                        }
                    }
                }
            });

            return this;
        },

        /**
         * Returns true if row has focused class
         *
         * @return Boolean
         */
        hasFocus: function () {
            return this.$el.hasClass(this.classNames.FOCUSED);
        },

        /**
         * Adds focus class (Item has been recently updated)
         *
         * @return AJS.RestfulTable.Row
         */
        focus: function () {
            $(this.el).addClass(this.classNames.FOCUSED);
            return this;
        },

        /**
         * Removes focus class
         *
         * @return AJS.RestfulTable.Row
         */
        unfocus: function () {
            $(this.el).removeClass(this.classNames.FOCUSED);
            return this;

        },

        /**
         * Adds loading class (to show server activity)
         *
         * @return AJS.RestfulTable.Row
         */
        showLoading: function () {
            this.$el.addClass(this.classNames.LOADING);
            return this;
        },

        /**
         * Hides loading class (to show server activity)
         *
         * @return AJS.RestfulTable.Row
         */
        hideLoading: function () {
            this.$el.removeClass(this.classNames.LOADING);
            return this;
        },

        /**
         * Switches row into edit mode
         *
         * @param e
         */
        edit: function (e) {
            var field;
            if ($(e.target).is("." + this.classNames.EDITABLE)) {
                field = $(e.target).attr("data-field-name");
            } else {
                field = $(e.target).closest("." + this.classNames.EDITABLE).attr("data-field-name");
            }
            this.trigger(this._event.ROW_EDIT, field);
            return this;
        },

        /**
         * Removes entry from table
         */
        destroy: function () {
            if (this.deleteConfirmation) {
            	// TODO
            } else {
                //this.model.destroy();
            	this.model.attributes = {};
                var collection = this.model.collection;
                if(collection) {
	                collection.remove(this.model);
	                AJS.triggerEvtForInst(ZEPHYR.RestfulTable.Events.DELETE_SUCCESS, this, [collection.toJSON()]);
                }
            }

        },

        /**
         * Renders a generic edit row. You probably want to override this in a sub class.
         *
         * @return AJS.RestfulTable.Row
         */
        render: function  () {

            var instance = this,
                renderData = this.model.toJSON(),
                //$opsCell = $("<td class='aui-restfultable-operations' />").append(this.renderOperations({}, renderData)),
                $throbberCell = AJS.$("<td class='aui-restfultable-status' />").append(ZEPHYR.RestfulTable.throbber()),
                _customFieldId = AJS.$('#zephyr-je-customfield-id').val();

            // restore state
            this.$el
                .removeClass(this.classNames.DISABLED + " " + this.classNames.FOCUSED + " " + this.classNames.LOADING + " " + this.classNames.EDIT_ROW)
                .addClass(this.classNames.READ_ONLY)
                .empty();

            if (this.allowReorder) {
                AJS.$('<td  class="' + this.classNames.ORDER + '" />').append(this.renderDragHandle()).appendTo(instance.$el);
            }

            this.$el.attr("data-id", this.model.orderId); // helper for webdriver testing

            $.each(this.columns, function (i, column) {

                var contents,
                    $cell = AJS.$("<td />"),
                    value = (instance.options.fetchAll) ? renderData[column.htmlId]: renderData[column.id],
                    fieldName = column.fieldName || column.id,
                    args = [{name: fieldName, value: value, allowEdit: column.allowEdit}, renderData, instance.model];

                if (value) {
                    instance.$el.attr("data-" + column.id, value); // helper for webdriver testing

                }

                if (column.readView) {
                    contents = new column.readView({
                        model: instance.model
                    }).render(args[0]);
                } else {
                    contents = instance.defaultColumnRenderer.apply(instance, args);
                }

                if (instance.allowEdit !== false && column.allowEdit !== false) {
                    var $editableRegion = AJS.$("<span />")
                        .addClass(instance.classNames.EDITABLE)
                        .addClass('zfj-editable-field')
                        //.append(aui.icons.icon({useIconFont: true, icon: 'edit'}))
                        .append(contents)
                        .attr("data-field-name", fieldName)
                        .append('<span style="top: 0px; right: 0px;" class="zfj-overlay-icon aui-icon aui-icon-small aui-iconfont-edit"/>');

                    $cell  = AJS.$("<td />")
                    		.addClass('zephyr_create_word_break')
                    		.append($editableRegion).appendTo(instance.$el);

                    if (!contents || AJS.$.trim(contents) == "") {
                        $cell.addClass(instance.classNames.NO_VALUE);
                        $editableRegion.append(AJS.$("<em />").text(this.emptyText || "Enter value"));
                    }

                } else {
                    $cell.append(contents);
                }
                if (column.styleClass) {
                    $cell.addClass(column.styleClass);
                }

                $cell.appendTo(instance.$el);
            });

            var $inputCustomField = AJS.$('<input type="hidden" name="' + _customFieldId + '" id="' + _customFieldId + '-' + renderData.orderId +'" />');
            var _parsedData = ZEPHYR.ISSUE.Create.Teststep.setStepJSON(ZEPHYR.ISSUE.Create.TESTSTEP_DATA_ADD_UPDATE, renderData);
            $inputCustomField.val(JSON.stringify(_parsedData));

            this.$el
                //.append($opsCell)
            	.append($inputCustomField)
                .append($throbberCell)
                .addClass(this.classNames.ROW + " " + this.classNames.READ_ONLY);

            if (this.allowDelete) {
                AJS.$('<td class="zephyr-aui-restfultable-operations" />').append('<span class="aui-icon aui-icon-small aui-iconfont-delete zephyr-teststep-delete"/>').appendTo(this.$el);
            }
            this.trigger(this._event.RENDER, this.$el, renderData);
            this.options.fetchAll = false;
            AJS.triggerEvtForInst(ZEPHYR.RestfulTable.Events.RENDER_DATA, this, renderData);
            this.$el.trigger(this._event.CONTENT_REFRESHED, [this.$el]);
            this._assignEvents();
            return this;
        },

        _assignEvents: function () {
            var instance = this;
            this.$(".zephyr-teststep-delete").click(function (e) {
            	e.preventDefault();
            	instance.destroy();
            });
        }
    });
    /**
     * Fix for ZFJ-2137
     * In 7.2 JIRA removed default exposure of AJS.RestfulTable.throbber function,
     * so created a function to return the throbber
     */
    ZEPHYR.RestfulTable.throbber = function () {
        return '<span class="aui-restfultable-throbber"></span>';
    };

})(AJS.$);
