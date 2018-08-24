AJS.$(document).on('keydown', '#comment', function (e) {
    if ((e.keyCode === AJS.$.ui.keyCode.ENTER)) {
    	if(AJS.$(this).parents('#comment-add-dialog').length){
    		if(AJS.$('#mentionDropDown').length){
				AJS.$('#mentionDropDown').find('li.aui-list-item.active a').trigger('click');
			}
	        e.stopPropagation();
	        return true;
    	}
    }
});