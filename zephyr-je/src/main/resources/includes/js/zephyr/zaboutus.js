var isLoadedInIframe = function() {
	try {
		return (window !== window.parent);
	} catch(e) {
		return false;
	}
}

var InitPageContent = function(initCallback) {
	if(isLoadedInIframe()) {
		AJS.$(window).load(function(){
			initCallback();
		});
	} else {
		AJS.$(document).ready(function(){
			initCallback();
		});
	}
}

InitPageContent(function(){
	var showAboutDialog = function () {
	    var instance = this,
	    dialog = new JIRA.FormDialog({
	        id: "show-about-dialog",
            width:  "640",
	        content: function (callback) {
	        	/*Short cut of creating view, move it to Backbone View and do it in render() */
				var title = AJS.I18n.getText("com.thed.zephyr.je.topnav.tests.about.label");
				var zfjTitle = AJS.I18n.getText("zephyr.je.admin.plugin.test.section.zephyr.name");
				var customerId = AJS.I18n.getText("zephyr.license.organisationId");
				var sen = AJS.I18n.getText("zephyr.license.sen");
				var zapiTitle = AJS.I18n.getText("zapi.plugin.test.section.zephyr.name");
				var zephyrTestMgmtLabel = AJS.I18n.getText("zephyr.je.about.header");
				var zephyrRightsLabel = AJS.I18n.getText("zephyr.je.about.rights.label");
				var zephyrCreditsLabel = AJS.I18n.getText("zephyr.je.about.credits.label");

				var innerHtmlStr = "<h1 class='dialog-title'>" + title + "</h1>" +
					"<div class='aui-dialog-content'>" +
					"	<br/>" +
					"	<div style='width:600px;background: transparent url(\"" + contextPath + "/download/resources/com.thed.zephyr.je/images/icons/140x40zephyrlogo.png\") scroll no-repeat left center ; margin-left:10px;' class='aui' id='create-about-dialog'>" +
					"		<h2 class='dialog-title' style='align:left; margin-left:175px'>" + zfjTitle + " <small><span id='zfjVersion'></span></small></h2>" +
					"		<ul style='align:left; margin-left:175px; padding:0 0 0 0'>" +
					"			<b>" + customerId + ": </b><span id='zCustomerId'></span>" +
					"			<br/>" +
					"            <small><b>" + sen + ": </b><span id='sen'></span></small>" +
					"			<br/>" +
					"			<span id='zLicnese'></span>" +
					"            <span id='zapiLicSection' style='display: none'>" +
					"			<hr/>" +
					"            <h3 class='dialog-title' style='font-weight: normal;margin-top: -0px;'>" + zapiTitle + " <small><span id='zapiVersion'></span></small></h3>" +
					"                <small><b>" + sen + ": </b><span id='zapiSEN'></span></small>" +
					"			    <br/>" +
					"            </span>" +
					"			<span id='zapiLicnese'></span>" +
					"		</ul>" +
					"		<ul style='margin-left:175px; padding:5px 0 0 0; font-size:0.7em'>" +
					"			&copy; 2011-2018. <a href='http://www.getzephyr.com'>" + zephyrTestMgmtLabel + "</a>. " + zephyrRightsLabel + "." +
					"			<a id='zcredits' href=''>" + zephyrCreditsLabel + "</a>" +
					"		</ul>" +
					"	</div>" +
					"	<br/>" +
					"</div>";
	        	callback(innerHtmlStr);
	        },
	        submitHandler: function (e) {
	            e.preventDefault();
	        }
	    });
	    dialog.show();
	    
	    AJS.$("#zcredits").live("click", function(e){
	    	e.preventDefault();
	    	dialog.hide();
	    	ZEPHYR.About.createCreditDialog();
	    });



        /*Let call a backend REST call to fetch license details.*/
        ZEPHYR.About.fetchLicense('/rest/zephyr/latest/license').success(function(licenseMap) {
	 			//var licenseMap = jQuery.parseJSON(response);
	 			AJS.$("#zCustomerId").html(licenseMap.customerId);
                AJS.$("#sen").text(licenseMap.SEN || "");
	 			AJS.$("#zLicnese").html(licenseMap.licenseInformation);
	 			AJS.$("#zfjVersion").html(licenseMap.version);
	 		});
        /*Let call a backend REST call to fetch ZAPI license details.*/
        ZEPHYR.About.fetchLicense('/rest/zfjapi/latest/zapi').success(function(licenseMap) {
            //var licenseMap = jQuery.parseJSON(response);
            AJS.$("#zapiSEN").text(licenseMap.SEN || "");
            AJS.$("#zapiLicnese").html(licenseMap.licenseInformation);
            AJS.$("#zapiVersion").html(licenseMap.version);
            AJS.$("#zapiLicSection").show();
        }).error(function(response) {
            AJS.log("ZAPI not found");
        });
	}
	/*JIRA 6.x changed the generated link ID (removed lnk), so we are looking for either id*/ 
	AJS.$("a[id^='add-about']").live("click", function(e) {
		window.isAboutZephyrClicked = true;
		e.preventDefault();
		showAboutDialog();
	});
});
AJS.$.namespace("ZEPHYR.About");
ZEPHYR.About.fetchLicense = function(url){
    return AJS.$.get(contextPath + url)
}
/**
 * Fetches license information. Returns jqXHR Object (which extends from deferred, hence all deferred methods can be attached)
 */
//ZEPHYR.About.fetchLicense = function(url){
//	return AJS.$.get(contextPath + url)
//}

ZEPHYR.About.createCreditDialog = function(){
	var creditDialog = new JIRA.FormDialog({
        id: "show-zcredits-dialog",
        content: function (callback) {
        	/*Short cut of creating view, move it to Backbone View and do it in render() */
			var title = AJS.I18n.getText("com.thed.zephyr.je.topnav.tests.about.label");
			var creditsDesc = AJS.I18n.getText("zephyr.je.about.credits.desc");

			var innerHtmlStr = "<h1 class='dialog-title'> " + title + " </h1>" +
				"<div class='aui-dialog-content' style='overflow-y:auto;height:400px;align:left; margin-left:100px; padding:0 0 0 0'>" + creditsDesc + "<p/>" +
				"	<br/>" +
				"	Anil Kumar <br/>" +
				"	Anu Kumari <br/>" +
				"	Asharani Kambegowda <br/>" +
				"	Bhanjan Gouda <br/>" +
				"	Chris Miller <br/>" +
				"	Deepa Esturi <br/>" +
				"	Emily Schneider <br/>" +
				"	Francis Adanza <br/>" +
				"	Govind Drolia <br/>" +
				"	Issam Bandak <br/>" +
				"	Karthik Raman <br/>" +
				"	Kavana Padmaraj <br/>" +
				"	Kavya Bavikatti <br/>" +
				"	Kiran Malla <br/>" +
				"	Manoj Behera <br/>" +
				"	Maura Ramil <br/>" +
				"	Mukul Sharma <br/>" +
				"	Nirav Shah <br/>" +
				"	Nitesh Singh <br/>" +
				"	Preethi Balasubramanian <br/>" +
				"	Priyanka Bishi <br/>" +
				"	Ritu Gandhi <br/>" +
				"	Roopa Sharma <br/>" +
				"	Samir Shah <br/>" +
				"	Sanjay Zalavadia <br/>" +
				"	Sanjeev Pande <br/>" +
				"	Shailesh Mangal <br/>" +
				"	Sharmila Namani <br/>" +
				"	Veera Budideti <br/>" +
				"	Vidhyavathi Sudhakar <p/>" +
				"	and 64 lbs of coffee! <br/>" +
				"</div>";
        	callback(innerHtmlStr);
        },
        submitHandler: function (e) {
            e.preventDefault();
        }
    });
	creditDialog.show();
}; 

/**
 * one of the following must be non null
 * params: contains selector and position w.r.t to that selector
 * output: relevant only when called from Soy. It appends the div to the output. 
 */
ZEPHYR.About.evalMessage = function(params, output, callback){
	var evalMsgPlaceHolder = "<div class='zfjEvalLic aui-message info' style='margin:10px 10px 10px 10px;'></div>";
	ZEPHYR.About.fetchLicense('/rest/zephyr/latest/license').success(function(licenseMap){
		if(licenseMap.isEval === 'true'){
			var today = new Date();
			var expDate = new Date(licenseMap.expDate);
			var body;
			if(expDate >= today){
				body = AJS.I18n.getText('license.eval.message.valid.body', licenseMap.expDateFormatted);
			}else{
				body = AJS.I18n.getText('license.eval.message.expired.body', licenseMap.expDateFormatted);
			}
			body += AJS.I18n.getText('license.eval.message.remaining.body', "<a href='https://marketplace.atlassian.com/plugins/com.thed.zephyr.je'>", "</a>")
			var msg =	"<div class='title'>" +
						"	<span class='aui-icon icon-info'></span>" +
						"	<strong>" + AJS.I18n.getText('license.eval.message.heading') + "</strong>" +
						"</div> <small>" + body + "</small>";
			console.log(msg)
			AJS.$('.zfjEvalLic').html( msg );
			if(callback){
				callback();
			}
		}else{
			AJS.$('.zfjEvalLic').remove();
		}
	})
	if(!output && !params && !callback)
		return evalMsgPlaceHolder
	else if(output){
		output.append(evalMsgPlaceHolder);
	}else {
		params = params || {selector:this, position:'after'}
		var operation = params.position || 'after' //(any valid jQuery operation e.g. after, before, prepend)
		
		AJS.$(params.selector)[operation](evalMsgPlaceHolder);
		
	}	
}