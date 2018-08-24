/**
 * @addon ZEPHYR.wikiEditor
 * Convert wiki to html
 */
if(typeof ZEPHYR == 'undefined') ZEPHYR = {};
ZEPHYR.wikiEditor = function () {};

ZEPHYR.wikiEditor.displayWikiPreview = function(wrapperEl, callback) {
	var markupContent = wrapperEl.find('textarea').val(),
		issueKey = wrapperEl.find('textarea').data('issue-key');

    wrapperEl.find('textarea').hide();
    AJS.$.ajax({
 	   type: 'POST',
 	   url: getRestURL() + "/util/render",
 	   data: JSON.stringify({
 		   "rendererType": "zephyr-wiki-renderer",
 		   "unrenderedMarkup": markupContent,
 		   "issueKey": issueKey
 	   }),
 	   contentType: "application/json",
 	   success: function(response) {
 		   if(callback)
 			  callback();
 		   wrapperEl.find('.wiki-preview-content').html(response.renderedHTML).show().addClass('active');
 		   ZEPHYR.wikiEditor.resizeWikiPreviewContent(wrapperEl);
 	   }
    });
}

ZEPHYR.wikiEditor.resizeWikiPreviewContent = function(wrapperEl) {
	if(wrapperEl.closest('tbody.ui-sortable').length > 0) {
		var $height = wrapperEl.find('.wiki-preview-content').height();
		wrapperEl.height($height + 17);
		// wrapperEl.find('.wiki-field-tools').css({'margin-top': $height + 2});				
	}
}

ZEPHYR.wikiEditor.displayWikiEditor = function(wrapperEl) {
	wrapperEl.find('.wiki-preview-content').hide().removeClass('active');
	wrapperEl.find('textarea').show();
	wrapperEl.find('textarea').focus();
	wrapperEl.find('textarea').keyup();
}

// Attach events for preview link
ZEPHYR.wikiEditor._attachWikiToolEvents = function(wrapperEl) {
	wrapperEl.find('.wiki-field-tools .zephyr-wiki-preview_link').bind('click', function(ev) {
		ev.preventDefault();

		if(AJS.$(this).hasClass('fullscreen')) {
			var _selectedLink = AJS.$(this);
			_selectedLink.find('span').removeClass('wiki-renderer-icon').addClass('icon loading');
			ZEPHYR.wikiEditor.displayWikiPreview(wrapperEl, function() {
				_selectedLink.removeClass('fullscreen').addClass('selected');
				_selectedLink.find('span').addClass('wiki-renderer-icon').removeClass('icon loading');
			});
		} else if(AJS.$(this).hasClass('selected')) {
			AJS.$(this).removeClass('selected').addClass('fullscreen');
			ZEPHYR.wikiEditor.displayWikiEditor(wrapperEl);
		}
	});
}

ZEPHYR.wikiEditor._attachWikiTools = function(wrapperEl) {
	var wikiToolsView = ZEPHYR.Templates.WikiEditor.attachWikiTools();
	if(wrapperEl.find('.textarea').length == 0) {
		wrapperEl.append(wikiToolsView);
	} else
		wrapperEl.find('.textarea').after(wikiToolsView);

	ZEPHYR.wikiEditor._attachWikiToolEvents(wrapperEl);
}

// TODO: update the element  class names
ZEPHYR.wikiEditor.init = function(wrapperEl) {
	wrapperEl.find('.wiki-preview-content').remove();
	wrapperEl.find('.wiki-field-tools').remove();

	ZEPHYR.wikiEditor._attachWikiTools(wrapperEl);
}
