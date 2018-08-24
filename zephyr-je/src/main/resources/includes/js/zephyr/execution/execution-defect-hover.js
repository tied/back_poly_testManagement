/**
 * @addon ZEPHYR.defectHover
 * Inline dialog for ZFJ defect information: execution defects and step defects
 */
if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
ZEPHYR.defectHover = function () {};
ZEPHYR.defectHover.issueStatus;
ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS = {
	urlPrefix: contextPath + "/rest/zephyr/latest/stepResult/stepDefects?executionId=",
	urlSuffix: '&expand=executionStatus',
	showDelay: 200,
	width: 500,
	closeOthers: false,
	noBind: true,
	hideCallback: function () {
		this.popup.remove(); // Clean up
	}
};

ZEPHYR.defectHover.show = function (trigger) {
	clearTimeout(AJS.$.data(trigger, "AJS.InlineDialog.delayId") || 0);
	AJS.$.data(trigger, "AJS.InlineDialog.hasDefectAttention", true);
	if (AJS.$.data(trigger, "AJS.InlineDialog") || ZEPHYR.defectHover._locked) {
		// This or another defect hover dialog is already visible.
		return;
	}
	AJS.$.data(trigger, "AJS.InlineDialog.delayId", setTimeout(function () {
		// Don't show the dialog if the trigger has been detached or removed.
		if (AJS.$(trigger).closest("html").length === 0) {
			ZEPHYR.defectHover.hide(trigger);
			return;
		}
		AJS.$.data(trigger, "AJS.InlineDialog", AJS.InlineDialog(
			AJS.$(trigger),
			"zfj-defect-hover-dialog-" + new Date().getTime(),
			function ($contents, _, showPopup) {
				// Call the InlineDialog's url function with its expected arguments.
				ZEPHYR.defectHover._fetchDefectDialogContents($contents, trigger, showPopup); 
			},
		ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS)).show();
	}, ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.showDelay));
};

ZEPHYR.defectHover.hide = function (trigger, showDelay) {
	clearTimeout(AJS.$.data(trigger, "AJS.InlineDialog.delayId") || 0);
	AJS.$.data(trigger, "AJS.InlineDialog.hasDefectAttention", false);
	var dialog = AJS.$.data(trigger, "AJS.InlineDialog");
	if (dialog && !ZEPHYR.defectHover._locked) {
		if (typeof showDelay !== "number") {
			showDelay = ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.showDelay;
		}
		if (showDelay >= 0) {
			// Hide the dialog after the given delay period.
			AJS.$.data(trigger, "AJS.InlineDialog.delayId", setTimeout(function () {
					dialog.hide();
					AJS.$.data(trigger, "AJS.InlineDialog", null);
				}, showDelay));
		} else {
			// Hide the dialog immediately.
			dialog.hide();
			AJS.$.data(trigger, "AJS.InlineDialog", null);
		}
	}
};

ZEPHYR.defectHover._locked = false;
/**
 * Display the inline defects on the triggered element
 */
ZEPHYR.defectHover._fetchDefectDialogContents = function ($contents, trigger, showPopup) {

	AJS.$.get(ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.urlPrefix + encodeURIComponent(trigger.getAttribute("data-executionId")) + ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.urlSuffix, function (response) {
        var _render = function () {
            var executionDefects = ZEPHYR.defectHover._getExecutionDefects(trigger);
            if (AJS.$.data(trigger, "AJS.InlineDialog.hasDefectAttention")) {
                $contents.html(ZEPHYR.Templates.Execution.DefectHover.inlineDefectHover({
                    executionDefects: executionDefects,
                    stepDefects: response.stepDefects,
                    issueStatus: ZEPHYR.defectHover.issueStatus,
                    executionStatus: response.executionStatus,
                    issueKey: trigger.getAttribute("data-issueKey")
                }));
                showPopup();
                ZEPHYR.defectHover._scrollLock('.execution-defects-container');

                // Fix for issue ZFJ-1176: IE10
                if (JIRA.Version.compare("6.2") == 1) {
                    AJS.$('.msie.msie-10').find($contents).css({
                        position: 'absolute',
                        top: 0
                    });
                }

                // Wait for the popup's show animation to complete before binding event handlers
                // on $contents. This ensures the popup doesn't get in the way when the mouse
                // moves over it quickly.
                AJS.$.data(trigger, "AJS.InlineDialog.delayId", setTimeout(function () {
                    $contents.bind({
                        "mousemove": function () {
                            ZEPHYR.defectHover.show(trigger);
                        },
                        "mouseleave": function () {
                            ZEPHYR.defectHover.hide(trigger);
                        }
                    });
                }, ZEPHYR.defectHover.INLINE_DIALOG_OPTIONS.showDelay));
            }
        }
        /**
         * Fetch the issue status
         */
        function fetchIssueStatuses() {
            var deferred = jQuery.Deferred();
            AJS.$.get(contextPath + "/rest/api/2/status", function(response) {
                ZEPHYR.defectHover.issueStatus = response;
                deferred.resolve();
            });
            return deferred.promise();
        }

        if (!ZEPHYR.defectHover.issueStatus)
            AJS.$.when(fetchIssueStatuses()).then(_render);
        else{
            _render();
        }
    });
};

ZEPHYR.defectHover._scrollLock = function(selector, scrollInterval) {
	var DEFAULT_SCROLL_INTERVAL = 30;
	if (typeof selector !== 'string') {
		scrollInterval = selector;
		selector = null;
	}
	if (!scrollInterval) {
		scrollInterval = DEFAULT_SCROLL_INTERVAL;
	}
	AJS.$(selector).on('DOMMouseScroll mousewheel', function(ev) {
		ev.preventDefault();
		var d;
		if (ev.originalEvent.wheelDelta) d = ev.originalEvent.wheelDelta / 120;
		if (ev.originalEvent.detail) d = -ev.originalEvent.detail / 3;
		this.scrollTop -= d * scrollInterval;
	});
},

/**
 * Return the list of execution defects associated with trigger element.
 */
ZEPHYR.defectHover._getExecutionDefects = function(trigger) {	
	var color = trigger.getAttribute("data-color");
	var defects = []
	AJS.$.each(AJS.$(trigger).find('a,span'), function(i, defect) {
		var defectEl = AJS.$(defect);
		var issueKey = defectEl.attr('data-issueKey') || '';
		var status = defectEl.attr('data-status') || '';
		var summary = defectEl.attr('data-summary') || '';
		var maskedIssueKey = defectEl.attr('data-maskedIssueKey') || '';
		var resolution = defectEl.attr('data-defect-resolution') || '';
		defects.push({
			key:		issueKey,
			maskedIssueKey:		maskedIssueKey,
			status: 	status,
			summary:	summary,
			color:		color,
			resolution: resolution
		});
	});
	return defects;
};

/**
 * @param {HTMLElement} context (optional)
 * -- The scope in which to look for ".zfj-defect-hover" elements. The element must have a "data-executionId" attribute
 * 		with target's executionId as the value.
 */
jQuery(document).delegate(".zfj-defect-hover[data-executionId]", {
	"mousemove": function () {
		ZEPHYR.defectHover.show(this);
	},
	"mouseleave": function () {
		ZEPHYR.defectHover.hide(this);
	},
	"click": function () {
		ZEPHYR.defectHover.hide(this, -1);
	}
});