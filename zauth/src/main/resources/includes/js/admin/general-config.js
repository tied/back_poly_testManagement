AJS.$(function ($) {
	AJS.$('#zdelete').attr("disabled", "disabled");
	AJS.$('#zadd').attr("disabled", "disabled");
	
	var updateButtonStatus = function(event){
		if(AJS.$(this).find('option:selected').length > 0)
			AJS.$('#zdelete').removeAttr("disabled"); 
		else
			AJS.$('#zdelete').attr("disabled", "disabled");
	}
	AJS.$('#zauthSelectedIP').live('click', updateButtonStatus)
	
	AJS.$('#ipToAdd').keyup(function(){
		if(this.value.length > 0)
			AJS.$('#zadd').removeAttr("disabled"); 
		else
			AJS.$('#zadd').attr("disabled", "disabled");
	})
})