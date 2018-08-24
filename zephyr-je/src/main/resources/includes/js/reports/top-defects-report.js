/**
 * @namespace ZEPHYR.REPORT.TOPDEFECTS
 */
ZEPHYR.REPORT.TOPDEFECTS = function () {
}
ZEPHYR.REPORT.TOPDEFECTS.prototype = AJS.$.extend(ZEPHYR.REPORT.TOPDEFECTS.prototype, ZEPHYR.REPORT.prototype);
ZEPHYR.REPORT.TOPDEFECTS.prototype = AJS.$.extend(ZEPHYR.REPORT.TOPDEFECTS.prototype, {
    getDefects: function (projectId, versionId, defectsCount, pickStatus) {
        var thiz = this,
        	queryParams = {"projectId": projectId, "versionId": versionId, "issueStatuses": pickStatus, "howMany":defectsCount},
        	ajaxOptions =  {
	            url: getRestURL() + "/execution/topDefects",
	            data:queryParams,
	            type: 'GET',
	            contentType:"application/json",
	            success: function (data) {
	                if (data) {
	                    if(Object.keys(data).length === 0) {
	                        AJS.$('#' + reqParams.chartDivId).html('No data found for chart ' + reqParams.title).css('color', '#1F77B4');
	                    } else {
	                        thiz.drawChart(data, defectsCount);
	                    }
	                }
	            },
	            error: function (jqXhr, status, errorThrown) {
	                AJS.log("Zephyr Chart Macro: unable to retrieve data for the selected server using AppLink: " + reqParams.serverId);
	                thiz.zeeDataOrServerNotFound(AJS.$("#chart-container." + reqParams.chartDivId), AJS.I18n.getText('insert.zee.macro.message.no.data.server.user.message'));
	            }
        	};
        AJS.$.ajax(ajaxOptions);
    },
    paint: function () {
    	this.getDefects(this.projectId, this.versionId, this.defectsCount, this.pickStatus);
    },
    drawChart: function (data, defectsCount) {

    	var $zephyrTestReport = AJS.$('#zephyr-test-report'),
    		header = '<h4 style="text-align:center;">' + AJS.I18n.getText('je.gadget.top.defects.subtitle', defectsCount) + '</h4>';
    		
		AJS.$(header).appendTo($zephyrTestReport);

		var tableTop = '<table class="aui tablesorter" id="defectTable">';
		tableTop += '<thead> <tr>' + 
				' <th class="thStyle">'+ AJS.I18n.getText('project.cycle.schedule.table.column.defect') + " " + AJS.I18n.getText("project.cycle.schedule.table.column.id")+'</th>' +
				' <th class="thStyle">'+ AJS.I18n.getText('project.cycle.schedule.table.column.summary') +'</th>' +
				' <th class="thStyle">'+ AJS.I18n.getText('project.cycle.schedule.table.column.status') +'</th>' +
				' <th class="thStyle">Tests Affected</th>' +
				' </tr></thead>';

		if(data && data.data){
			var seriesTemp = {};
			tableTop += '<tbody>';
			for (var i in data.data) {
				tableTop += '<tr>';
				var defectIdsHtml = "<a href='"+ contextPath + "/browse/" + data.data[i].defectKey + "'>" + data.data[i].defectKey + "</a>";
				var baseNavHtml = '/secure/IssueNavigator.jspa?reset=true&jqlQuery=';
				var associatedTestIdCount="";
				for(var test in data.data[i].associatedTestIds) {
					 associatedTestIdCount += "issue="+data.data[i].associatedTestIds[test];
					if(test < data.data[i].associatedTestIds.length-1) {
						associatedTestIdCount += " OR ";
					}
				}
				
				baseNavHtml += escape(associatedTestIdCount)
				var associatedTestCountHtml = '<a href='+ contextPath + baseNavHtml + '>' + data.data[i].testCount + '</a>';
				tableTop += '<td class="tdOrdering">' + defectIdsHtml + '</td>';
				var defectSummary = data.data[i].defectSummary;
				var findReplace = [[/&/g, "&amp;"], [/</g, "&lt;"], [/>/g, "&gt;"], [/"/g, "&quot;"]]
				for(var item in findReplace) {
					defectSummary = defectSummary.replace(findReplace[item][0], findReplace[item][1]);
				}
				tableTop += '<td class="tdOrdering">' +  defectSummary + '</td>';
				tableTop += '<td class="tdOrdering">' + data.data[i].defectStatus + '</td>';
				tableTop += '<td class="tdOrdering">' + associatedTestCountHtml + '</td>';

				tableTop += '</tr>';
			}
			tableTop += "</tbody>";
		}
		tableTop += '</table>';
		AJS.$(tableTop).appendTo($zephyrTestReport);
    }
});