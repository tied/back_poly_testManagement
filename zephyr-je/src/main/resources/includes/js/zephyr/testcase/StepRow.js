jQuery.namespace("ZEPHYR.ISSUE.Teststep.row");
/**
 * * Readonly view of version row * *
 *
 * @class VersionRow
 */
ZEPHYR.ISSUE.Teststep.StepRow = JIRA.RestfulTable.Row.extend({
	// delegate events
	// events: { "click .release" : "release"
	// },
	/** * Archives a version */
	release : function() {
		this.sync({
			released : true
		});
	},

	/**
	 * * Resets and renders version row in table. This should be called whenever
	 * the model changes.
	 */
	render : function() {
		var instance = this,
        id = this.model.get("id"),
        $el = jQuery(this.el);

		$el.attr("className", "project-config-version"); // reset all classNames
		$el.attr("id", "step-" + id + "-row").attr("data-id", id);

		var renderData = this.model.toJSON(),
		html = ZEPHYR.Templates.Steps.stepRow({teststep:renderData,baseUrl:contextPath,stepId:id}); // render using closure template
		var attachemntViewDialog = AJS.$("#attachment-inlineDialog-1-"+ id);
		if(attachemntViewDialog.length) {
			attachemntViewDialog.remove();
		}
		this.$el.html(html);

		this._assignEvents();

		return this;
	},

	_assignEvents: function () {
        var instance = this;
        this.$(".project-config-operations-trigger").click(function (e) {
        	e.preventDefault();
        	instance.deleteConfirmationDialog(instance.model);
        });

        this.$(".test-step-action-clone").click(function (e) {
        	e.preventDefault();
        	instance.cloneStepConfirmationDialog(instance.model);
        });

        this.$('.test-step-actions').parent().mouseover(function(ev) {
        	AJS.$('#step-' + instance.model.get('id') + '-row .test-step-actions a').css('visibility', 'visible');
        }).mouseout(function(ev) {
        	AJS.$.each(AJS.$('.test-step-actions a.aui-steps-dropdown'), function(i, el) {
        		var selectedData =  AJS.$(el).attr('data-clicked') || '0';
        		if(AJS.$(el).css('visibility') == 'visible' && selectedData == '0') {
        			AJS.$(el).css('visibility', 'hidden');
						}
        	});
        });

        this.$('.test-step-actions a.aui-steps-dropdown').on('click',function(ev) {
        	var selectedRow = AJS.$('#step-' + instance.model.get('id') + '-row .test-step-actions a');

					AJS.$.each(AJS.$('.test-step-actions a.aui-steps-dropdown'), function(i, el) {
        		var selectedData =  AJS.$(el).attr('data-clicked');
						if(AJS.$(el)[0] !== ev.target ) {
        			if(AJS.$(el).css('visibility') == 'visible' && selectedData && selectedData == '1')
        				AJS.$(el).css('visibility', 'hidden').attr('data-clicked', '0');
						}
        	});
					if(AJS.$(ev.target).attr('data-clicked') && AJS.$(ev.target).attr('data-clicked') === '1') {
						selectedRow.attr('data-clicked', '0');
					} else {
						selectedRow.attr('data-clicked', '1').css('visibility', 'visible');
					}
					ev.stopPropagation();
        });

	},

	cloneStepConfirmationDialog: function(teststep) {
	    var instance = this,
	    dialog = new JIRA.FormDialog({
	        id: "entity-" + teststep.get('id') + "-clone-dialog",
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
	        	var innerHtmlStr = ZEPHYR.Templates.Steps.cloneStepConfirmationDialog({teststep: teststep.attributes});
	            callback(innerHtmlStr);
	            AJS.$('#clone-insert-at').bind('focusin', function() {
	            	AJS.$('#teststep-clone-insertat-error').html('');
	            	AJS.$('input[name=clone-append]:checked').prop('checked', false);
	            });
	        },

	        submitHandler: function (ev) {
	            ev.preventDefault();
	            var positionLength = ZEPHYR.ISSUE.StepTable._models.length + 1;
	            //var step = AJS.$(ev.target).find('#clone-test-step').val();
	            var insertAt = AJS.$.trim(AJS.$(ev.target).find('input#clone-insert-at').val());
	            var stepPosition = AJS.$(ev.target).find('input[name=clone-append]:checked').val();
	            if(stepPosition || (insertAt != '' && insertAt >= 1 && insertAt <= positionLength)) {
	            	stepPosition = stepPosition || insertAt;
	            	instance.saveTestStepClone(instance, teststep, stepPosition, function () {
		        		dialog.hide();
		        		AJS.$('#clone-insert-at').unbind('focusin');
		            });
	            } else {
	            	AJS.$('#teststep-clone-insertat-error').html(AJS.I18n.getText("cycle.operation.clone.error.position", positionLength));
	            	AJS.$('input#assign-issue-submit').prop('disabled', false);
	            	AJS.$('.teststep-clone-dialog-form .loading').remove();
	            }
	        }
	    });
	    dialog.show();
	},

	deleteConfirmationDialog: function(teststep) {
	    var instance = this,
	    dialog = new JIRA.FormDialog({
	        id: "entity-" + teststep.get('id') + "-delete-dialog",
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
	        	var innerHtmlStr = ZEPHYR.Templates.Steps.deleteStepsConfirmationDialog({teststep: teststep.attributes});
	            callback(innerHtmlStr);
	        },

	        submitHandler: function (e) {
	        	instance.deleteTestStep(instance, teststep, function () {
	        		dialog.hide();
	            });
	            e.preventDefault();
	        }
	    });

	    dialog.show();
	},

	saveTestStepClone: function(instance, teststep, stepPosition, completed) {
		var entryModel = new JIRA.RestfulTable.EntryModel();
		var step = AJS.I18n.getText("teststep.operation.clone.step.prefix") + ' -';

		if(teststep.get('step') && teststep.get('step') != '') {
			var stepMarkup = teststep.get('step');
			if(/^\s*(#|h[1-6]\.|\*|\|\||bq.|\-|{quote})/.test(stepMarkup)) { // Check for the block element syntax with space in the start.
				step = step + ' \n' + teststep.get('step');
			} else
				step = step + ' ' + teststep.get('step');
		}
		// teststep/{issueId}/clone/{fromStepId}
		entryModel.url = ZEPHYR.ISSUE.StepTable.options.url + '/clone/' + teststep.get('id');

		entryModel.save({
			"data"		: teststep.get('data'),
	    "result"    : teststep.get('result'),
	    "step"		: step,
	    "position"	: stepPosition
		},
		{
			success: function (model, stepsAsArray) {
            	if(completed)
								completed.call();

          ZEPHYR.ISSUE.StepTable.options.entries = stepsAsArray;
					stepsAsArray.forEach(function(step) {
						step['customFieldsOrder'] = ZEPHYR.ISSUE.StepTable.options.customFieldsOrder;
						step['customFieldsValue'] = ZEPHYR.ISSUE.StepTable.options.customFieldsValue;
					})
    			ZEPHYR.ISSUE.StepTable.refreshAll();
			},
			error: function(response) {
            	if(completed)
    				completed.call();
			}
        });
	},

	deleteTestStep: function(instance, teststep, completed) {
		if(AJS.params.ZTestStepDeleteInProgress && AJS.params.ZTestStepDeleteInProgress == 1)
    		return;
    	AJS.params.ZTestStepDeleteInProgress = 1;

    	var delMsgText = AJS.I18n.getText("view.issues.steps.delete.success", instance.model.get('orderId'));
    	var delMsg = AJS.$('<tr id="zephDelMsg" class="aui-message" style="width:100%"><td colspan="7"><b>'+ delMsgText +'</b></td></tr>');
    	instance.$el.before(delMsg);
    	teststep.destroy({
        	data:{id:teststep.get("id")},
        	success:function(stepsAsArray){
        		if(completed)
						completed.call();
        		//setTimeout(function(){
        			AJS.$("#zephDelMsg").fadeOut(250, function () {
        				AJS.$("#zephDelMsg").remove();
        				/*Step deleted, Lets refresh the table*/

        					ZEPHYR.ISSUE.StepTable.options.entries = stepsAsArray;
            			ZEPHYR.ISSUE.StepTable.refreshAll();
            			delete AJS.params.ZTestStepDeleteInProgress;
            			/* No need to toggle hidden class, refresh would recreate all the rows */
        			  });
        		//}, 1000);
        	},
            error:function(){
              delete AJS.params.ZTestStepDeleteInProgress;
            	AJS.$("#zephDelMsg").remove();
            	if(completed)
    						completed.call();
            }
        });
        //e.preventDefault();
	}
});
