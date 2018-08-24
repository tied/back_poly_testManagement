jQuery.namespace("ZEPHYR.ISSUE.Teststep.editRow");
/**
 * * Edit/Create view of version row * *
 *
 * @class VersionEditRow
 */
ZEPHYR.ISSUE.Teststep.StepEditRow = JIRA.RestfulTable.EditRow.extend({
	/**
	 * * Handles all the rendering of the create version row. This includes
	 * handling validation errors if there is any * *
	 *
	 * @param {Object}
	 *            renderData ... {Object} errors - Errors returned from the
	 *            server on validation * ... {Object} vales - Values of fields
	 */
	render : function(renderData) {
		renderData.Issueid = JIRA.Issue.getIssueId();
		renderData.baseUrl = contextPath;
		if(renderData.values.customFields) {
			Object.keys(renderData.values.customFields).forEach(function(field) {
				var type = renderData.values.customFields[field].customFieldType;
				if( type === 'CHECKBOX' || type === 'RADIO_BUTTON' || type === 'MULTI_SELECT' || type === 'SINGLE_SELECT') {
					renderData.values.customFieldsValue[field].forEach(function(fieldValues) {
						if(renderData.values.customFields[field].value.indexOf(fieldValues.name) >= 0) {
							fieldValues.value = true;
						} else {
							fieldValues.value = false;
						}
					})
				}
			});
		}
		this.$el.html(ZEPHYR.Templates.Steps.editStepRow(renderData	));
		_.each(this.$el.find('td.teststep-editable'), function($cell) {
			if(AJS.$($cell).find('.textarea').length)
				ZEPHYR.wikiEditor.init(AJS.$($cell));
		});
		// auto adjust the height of test step textarea
		this.$el.find( 'textarea.ztextarea-result, textarea.ztextarea-step, textarea.ztextarea-data, textarea.ztextarea-customField' ).on('keyup', this.resizeTextarea);
		this.$el.find( 'textarea.ztextarea-result, textarea.ztextarea-step, textarea.ztextarea-data, textarea.ztextarea-customField' ).keyup().data('issue-key', JIRA.Issue.getIssueKey());
		return this;
	},

	resizeTextarea: function() {
		if(this.scrollHeight > 1) {
			if(this.className.indexOf('ztextarea-step')) {
				var width = AJS.$('.ztextarea-result').width() + 12;
				this.style.width = width + 'px';
			}
			// this.style.height = 'auto';
			this.style.height = (this.scrollHeight - 2)+'px';
			var _scrollHeight = this.scrollHeight;
			if(AJS.$(this).closest('tbody.ui-sortable').length > 0) {
				_scrollHeight += 17;
				// AJS.$(this).siblings('.wiki-field-tools').css({'margin-top': this.scrollHeight + 2});
			}
			AJS.$(this).parent().height(this.scrollHeight + 17);
			if(this.className.indexOf('ztextarea-step')) {
				this.style.width = '100%';
			}
		}
	}
});
