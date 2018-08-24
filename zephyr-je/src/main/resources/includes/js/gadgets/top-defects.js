GADGET = {};

/**
 * Returns the ajax requests that should be executed prior to calling GADGET.template
 */
GADGET.templateArgs = function(baseUrl) {
	return [
		{
			key: "versionInfo",
			ajaxOptions: function () {
				var gadget = this;
				var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
				var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
                var queryParams = {"projectId": projectId, "versionId":versionId}
				return {
					url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/versionBoard-list",
                    data:queryParams,
					contentType: "application/json",
					complete: function(xhr, status, response){
	        			   if(xhr.status != 200 ){
	        				   JE.gadget.zephyrError(xhr, gadget);
	        				   gadgets.window.adjustHeight();
	           			   }
					}
				};
			}
		},
	    {
	    	key: "defects",
	    	ajaxOptions: function () {
	    		var gadget = this;
	    		var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
	    		var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
	    		//var groupFld = gadgets.util.unescapeString(gadget.getPref("groupFld"));
	    		var selectedStatuses = gadgets.util.unescapeString(gadget.getPref("statusNames"));
	    		var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));

                var queryParams = {"projectId": projectId, "versionId": versionId, "issueStatuses": selectedStatuses, "howMany":howMany};
	    		return {
	    			//url: baseUrl + "/rest/zephyr/latest/schedule/getTopDefects?pid="+ projectId +"&vid=" + versionId+"&testStatuses="+selectedStatuses + "&howMany="+ howMany,
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/topDefects",
                    data:queryParams,
	    			contentType: "application/json",
	    			complete: function(xhr, status, response) {
         			   if(xhr.status != 200 ){
         				  JE.gadget.zephyrError(xhr, gadget);
         				  gadgets.window.adjustHeight();
           			   }
					}
	    		};
	    	}
	    }
	];
};

/**
* Returns the ajax requests that should be executed prior to calling GADGET.descriptor.
* The descriptor will get the results in the args parameter, with each request response added to the "key" field
*/
GADGET.descriptorArgs = function (baseUrl) {
	return [
       {
           key: "projectOptions",
           ajaxOptions: function(){
        	   var gadget = this;
        	   return {
        		   url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/project-list",
        		   contentType: "application/json",
        		   complete: function(xhr, status, response){
        			   if(xhr.status != 200 ){
        				   JE.gadget.zephyrError(xhr, gadget);
        				   gadgets.window.adjustHeight();
        			   }
        		   }
        	   };
           }
       }
     ];
};

/**
* Configuration screen descriptor for Top Defects Gadget!
*/ 
GADGET.descriptor = function (gadget, args, baseUrl) {

	var curTitle = gadget.getPref('topDefectsName')
	/*If pref is default, then internationalize it, else leave whatever user has chosen*/
	if(curTitle == "Top Defects Impacting Testing")
		curTitle = gadget.getMsg('je.gadget.top.defects.title');

   return {
       theme : function()
       {
           if (gadgets.window.getViewportDimensions().width < 450)
           {
               return "gdt top-label";
           }
           else
           {
               return "gdt";
           }
       }(),
       fields: [
            JE.gadget.fields.title(gadget, "topDefectsName", 'je.gadget.common.title.label', curTitle),
			JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
			JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
			JE.gadget.fields.topDefectsGroupPicker(gadget, "howMany", false),
			JE.gadget.fields.statusesPicker(gadget, "projectId", "statusNames"),
			JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
	        JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
            AJS.gadget.fields.nowConfigured()
       ]
   };
};

GADGET.template = function (gadget, args, baseUrl) {
	var project = args.versionInfo.project;
	var version = args.versionInfo.version;
	var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));
	var groupFld = gadgets.util.unescapeString(gadget.getPref("groupFld"));
	
	// empty content and set root classes
	gadget.getView().empty();
	var body = AJS.$('body');
	
	// Update the title of the gadget
	var gadgetId = AJS.$(window).attr('name');
	var gadgetTitle = gadget.getPref('topDefectsName');
	JE.gadget.utils.setGadgetTitle(gadgetId, gadgetTitle);

	/**
	 * Renders an error message
	 */
	var showErrorMessage = function(message) {
		gadget.getView().append(AJS.$('<div class="gg-error-message">'+message+'</div>'));
	};
	
	// root element
	var $zWrapper = AJS.$('<div style="height: auto "/>').attr('class', 'gg-wrapper zephyr-chart');
	gadget.getView().append($zWrapper);
	
	// title with project and version links
	var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $zWrapper);
	var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.top.defects.subtitle', howMany) + '</h4>';
	AJS.$(header).appendTo($zWrapper);
	var tableTop = '<table class="aui tablesorter" id="defectTable" style="border-collapse:collapse;">';
	tableTop += '<thead> <tr>' + 
				' <th class="thStyle">'+ gadget.getMsg('project.cycle.schedule.table.column.defect') + " " + gadget.getMsg("project.cycle.schedule.table.column.id")+'</th>' +
				' <th class="thStyle">'+ gadget.getMsg('project.cycle.schedule.table.column.summary') +'</th>' +
				' <th class="thStyle">'+ gadget.getMsg('project.cycle.schedule.table.column.status') +'</th>' +
				' <th class="thStyle">Tests Affected</th>' +
				' </tr></thead>';

	if(args.defects != null && args.defects.data != null){
		var seriesTemp = {};
		tableTop += '<tbody>';
		for (var i in args.defects.data) {
			tableTop += '<tr>';
			var defectIdsHtml = "<a href='"+ baseUrl + "/browse/" + args.defects.data[i].defectKey + "'>" + args.defects.data[i].defectKey + "</a>";
			var baseNavHtml = '/secure/IssueNavigator.jspa?reset=true&jqlQuery=';
			var associatedTestIdCount="";
			for(var test in args.defects.data[i].associatedTestIds) {
				 associatedTestIdCount += "issue="+args.defects.data[i].associatedTestIds[test];
				if(test < args.defects.data[i].associatedTestIds.length-1) {
					associatedTestIdCount += " OR ";
				}
			}
			
			baseNavHtml += escape(associatedTestIdCount)
			var associatedTestCountHtml = '<a href='+ baseUrl + baseNavHtml + '>' + args.defects.data[i].testCount + '</a>';
			tableTop += '<td class="tdOrdering">' + defectIdsHtml + '</td>';
			tableTop += '<td class="tdOrdering">' + args.defects.data[i].defectSummary + '</td>';
			tableTop += '<td class="tdOrdering">' + args.defects.data[i].defectStatus + '</td>';
			tableTop += '<td class="tdOrdering">' + associatedTestCountHtml + '</td>';

			tableTop += '</tr>';
		}
		tableTop += "</tbody>";
	}
	tableTop += '</table>';
	AJS.$(tableTop).appendTo($zWrapper);

	if(args.defects != null && args.defects.data != null && args.defects.data.length > 0) {
		AJS.$("#defectTable").tablesorter({sortList: [[3,1]]
		}).appendTo($zWrapper);
	} 
};