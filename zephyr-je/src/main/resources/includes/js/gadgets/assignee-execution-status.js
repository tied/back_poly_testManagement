GADGET = {};
var MAX_LEGENDS = 10;

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
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/versionBoard-list?projectId="+ projectId +"&versionId=" + versionId,
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
            key: "status",
            ajaxOptions: function () {
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/teststatus-list",
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
            key: "executionStatusCountPerAssignee",
            ajaxOptions: function () {
                var gadget = this;
                var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
                var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
                var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/executionsStatusCountPerAssigneeForCycle?projectId="+ projectId +"&versionId=" + versionId+"&cycles=" + cycles,
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

GADGET.template = function (gadget, args, baseUrl) {
    var project = args.versionInfo.project;
    var version = args.versionInfo.version;
    var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
    // empty content and set root classes
    gadget.getView().empty();
    var body = AJS.$('body');

    // Update the title of the gadget
    var gadgetId = AJS.$(window).attr('name');
    var gadgetTitle = gadget.getPref('assigneeExecutionStatusName');
    var chartType = gadget.getPref('chartType');
    JE.gadget.utils.setGadgetTitle(gadgetId, gadgetTitle);

    /**
     * Renders an error message
     */
    var showErrorMessage = function(message) {
        gadget.getView().append(AJS.$('<div class="gg-error-message">'+message+'</div>'));
    };


    // root element
    var $ggWrapper = AJS.$('<div style="height: auto "/>').attr('class', 'gg-wrapper zephyr-chart');
    gadget.getView().append($ggWrapper);
    if(chartType === 'pie') {
        var legendWrapper = '<div class="legend-wrapper"><div id="legendType"></div><ul class="pie-legend"></ul></div>';
        gadget.getView().append(legendWrapper);
    }

    var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $ggWrapper);
    var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.testcase.execution.assignee.status.subtitle') + '</h4>';
    var backToAssigneeButton = '<form class="aui" style="position: absolute;right: 26px;z-index: 9999"><button style="display: none;" class="button" id="backToAssignee">Back</button></form>';
    AJS.$(header).appendTo($ggWrapper);
    AJS.$(backToAssigneeButton).appendTo($ggWrapper);

    var $tableWrapper = AJS.$('<div style="max-height: 500px; overflow-y: auto;" id="table-wrapper"></div>');
    AJS.$($tableWrapper).appendTo($ggWrapper);

    var renderTabularChart = function() {
        var chartTable;
        var query;
        var cycleIds = cycles.split("|").join(",");

        chartTable = '<table class="aui tablesorter" id="defectTable" style="border-collapse:collapse;">';
        chartTable += '<thead> <tr>';
        chartTable += ' <th class="thStyle">'+"Assignee Name"+'</th>';
        var index = 0;
        for(var key in args.executionStatusCountPerAssignee) {
            var executionStatusData = args.executionStatusCountPerAssignee[key];
            if (index == 0 && executionStatusData !=null){
                Object.keys(executionStatusData).forEach(function(status) {
                    chartTable += ' <th class="thStyle">' + gadgets.util.escapeString(status) + '</th>'
                });
                chartTable += ' </tr></thead>';
                chartTable += '<tbody>';
            }

            var n = key.indexOf("Inactive");
            var assigneeKey = key;
            if(n != -1) {
                assigneeKey = key.substring(0,n-2);
            }
            assigneeKey = gadgets.util.escapeString(assigneeKey);
            query =  'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND assignee="' + addSlashes(assigneeKey) + '"';

            if(cycleIds) {
                query = query + ' AND cycleId IN (' + addSlashes(cycleIds) + ')';
            }

            var assigneeNameLink = "<a href='"+ baseUrl + '/secure/enav/#?query='+encodeURIComponent(query) +"'>"+key+ "</a>";
            chartTable += '<tr>';
            chartTable += '<td class="tdOrdering">' + assigneeNameLink + '</td>';


            if(executionStatusData !=null) {
                for(var status in executionStatusData) {
                    var appendedQuery = query + ' AND executionStatus="' + addSlashes(status) + '"';
                    var assigneeExecStatusCountLink = "<a href='"+ baseUrl + '/secure/enav/#?query='+encodeURIComponent(appendedQuery) +"'>"+executionStatusData[status]+ "</a>";
                    chartTable += '<td class="tdOrdering">' + assigneeExecStatusCountLink + '</td>';
                }
            }
            chartTable += '</tr>';
            query='';
            index++;
        }
        chartTable += '</tbody>';
        chartTable += '</table>';
        AJS.$(chartTable).appendTo($tableWrapper);
    }

    var drillDown = function(title, donutDrillDownChart) {
        if($ggWrapper.find('.donut-chart-wrapper')){
            $ggWrapper.find('.donut-chart-wrapper').remove();
        } else {
            $ggWrapper.find('svg').remove();
        }
        AJS.$('#inline-dialog-legendDialog').remove();
        AJS.$('.pie-legend').html('');
        legendsContent = '';
        otherLegendsContent = '';
        otherLegendsCount = null;
        var statusData = [];
        var chartStatuses = args.executionStatusCountPerAssignee[title];
        for(var k in chartStatuses) {
            var statuses = {};
            statuses['title'] = gadgets.util.escapeString(k);
            statuses['total'] = chartStatuses[k];
            statuses['color'] = columns[k];
            statusData.push(statuses);
        }
        var header = 'Execution Status';
        donutDrillDownChart.render(statusData, null, function(d, color, i) {
            renderLegend(d, color, header, d.data.total, i,title);
        }, function() {
            initDialog(header);
        });
        AJS.$('#backToAssignee').show();
        gadgets.window.adjustHeight();
    };

    var legendsContent = '';
    var otherLegendsContent = '';
    var otherLegendsCount;
    var assigneeQuery;

    var renderOtherLegends = function(d, total, header,title) {
        var hrefLink;
        var cycleIds = cycles.split("|").join(",");
        if(header === 'Assignee') {
            var n = d.data.title.indexOf("Inactive");
            var assigneeKey = d.data.title;
            if(n != -1) {
                assigneeKey = d.data.title.substring(0,n-2);
            }
            hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND assignee="' + addSlashes(assigneeKey) + '"';
        } else if(header === 'Execution Status'){
            var n = title.indexOf("Inactive");
            var assigneeKey = title;
            if(n != -1) {
                assigneeKey = title.substring(0,n-2);
            }
            assigneeQuery = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND assignee="' + addSlashes(assigneeKey) + '"';
            hrefLink = assigneeQuery +' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
        }

        if(cycleIds) {
            hrefLink = hrefLink + ' AND cycleId IN (' + addSlashes(cycleIds) + ')';
        }
        otherLegendsContent += '<div><span class="inlineDialogEllipses"><a href="'+baseUrl + '/secure/enav/#?query='+encodeURIComponent(hrefLink)+'" class="pie-legend-item-label" title="'+d.data.title+'">'+d.data.title+'</a></span><span style="float: right;">'+total+'</span></div>';
        //AJS.$('#otherLegends').append(otherLegendsContent);
    }

    var renderLegend = function(d, color, header, count, index, title) {
        if(index === MAX_LEGENDS) {
            legendsContent += '<li class="pie-legend-item"><div class="pie-legend-content"><div class="pie-legend-item-wrapper"><div class="pie-other-legend-value pie-legend-item-value">'+count+'</div><div class="pie-legend-item-label-wrapper"><div class="icon piechart-fill legend-icon" style="background-color: '+color+'"></div><a id="otherLegendsinlineDialog" class="pie-legend-item-label" title="Other...">Other...</a></div></div></div></li>';
            if(!isNaN(parseInt(count, 10))) {
                otherLegendsCount = parseInt(count, 10);
            }
            renderOtherLegends(d, count, header,title);
            return;
        }
        if(index > MAX_LEGENDS) {
            if(!isNaN(parseInt(count, 10))) {
                otherLegendsCount += parseInt(count, 10);
                otherLegendsCount = parseInt(otherLegendsCount, 10);
            }
            renderOtherLegends(d, count, header,title);
            return;
        }
        var cycleIds = cycles.split("|").join(",");
        var hrefLink;
        if(header === 'Assignee') {
            var n = d.data.title.indexOf("Inactive");
            var assigneeKey = d.data.title;
            if(n != -1) {
                assigneeKey = d.data.title.substring(0,n-2);
            }
            hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND assignee="' + addSlashes(assigneeKey) + '"';
        } else if(header === 'Execution Status'){
            var n = title.indexOf("Inactive");
            var assigneeKey = title;
            if(n != -1) {
                assigneeKey = title.substring(0,n-2);
            }
            assigneeQuery = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND assignee="' + addSlashes(assigneeKey) + '"';
            hrefLink = assigneeQuery +' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
        }

        if(cycleIds) {
            hrefLink = hrefLink + ' AND cycleId IN (' + addSlashes(cycleIds) + ')';
        }

        legendsContent += '<li class="pie-legend-item"><div class="pie-legend-content"><div class="pie-legend-item-wrapper"><div class="pie-legend-item-value">'+count+'</div><div class="pie-legend-item-label-wrapper"><div class="icon piechart-fill legend-icon" style="background-color: '+color+'"></div><a href="'+baseUrl + '/secure/enav/#?query='+encodeURIComponent(hrefLink)+'" class="pie-legend-item-label" title="'+d.data.title+'">'+d.data.title+'</a></div></div></div></li>';
    };

    var initDialog = function(header) {
        otherLegendsCount = (otherLegendsCount == 0) ? '0' : (otherLegendsCount || '');
        AJS.$('.pie-legend').append(legendsContent);
        AJS.$('.pie-other-legend-value').html(otherLegendsCount);
        AJS.$('#legendType').html(header);
        AJS.InlineDialog(AJS.$("#otherLegendsinlineDialog"), "legendDialog",
            function(content, trigger, showPopup) {
                content.css({"padding":"20px"}).html(otherLegendsContent);
                showPopup();
                return false;
            }, {
                onHover: true,
                hideDelay: 1000
            }
        );
    };

    var renderPieChart = function(data) {
        var pieChartOptions = { height: 500, width: 600, graph: $ggWrapper, MAX_LEGENDS: MAX_LEGENDS };
        var donutDrillDownChart = new DonutDrillDownChart(pieChartOptions);
        var header = 'Assignee';
        donutDrillDownChart.render(data, function(title) {
            drillDown(title, donutDrillDownChart);
        }, function(d, color, i) {
            renderLegend(d, color, header, d.data.total, i, '');
        }, function() {
            initDialog(header);
        });
        AJS.$('#backToAssignee').on('click', function(ev) {
            ev.preventDefault();
            AJS.$(this).hide();
            AJS.$('.pie-legend').html('');
            legendsContent = '';
            otherLegendsContent = '';
            otherLegendsCount = null;
            //$ggWrapper.find('svg').remove();
            if($ggWrapper.find('.donut-chart-wrapper')){
                $ggWrapper.find('.donut-chart-wrapper').remove();
            } else {
                $ggWrapper.find('svg').remove();
            }
            renderPieChart(data);
            gadgets.window.adjustHeight();
        });
    }

    var columns = args.status;
    if(args.executionStatusCountPerAssignee != null ){
        if(chartType === 'table') {
            renderTabularChart();
        } else if(chartType === 'pie'){
            var chartData = args.executionStatusCountPerAssignee;
            var data = [];
            var zeroDataCount = 0;
            for(var key in chartData) {
                var obj = {};
                obj['title'] = gadgets.util.escapeString(key);
                var statuses = chartData[key];
                obj['total'] = 0;
                for(var i in statuses) {
                    obj['total'] += statuses[i];
                }
                if(!obj['total']) {
                    zeroDataCount++;
                }
                obj['statuses'] = statuses;
                data.push(obj);
            }
            if(zeroDataCount === Object.keys(chartData).length) {
                $ggWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
                AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.nodata.label'), closeable: false});
            } else {
                renderPieChart(data);
            }
        }
    }
    gadgets.window.adjustHeight();
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
 * Configuration screen descriptor for the execution status distribution gadget
 */
GADGET.descriptor = function (gadget, args, baseUrl) {
    var curTitle = gadget.getPref('assigneeExecutionStatusName')
    if(curTitle == "Zephyr Test Execution Assignment")
        curTitle = gadget.getMsg('je.gadget.testcase.execution.assignee.status.title');
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
            JE.gadget.fields.title(gadget, "assigneeExecutionStatusName", 'je.gadget.common.title.label', curTitle),
            JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
            JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
            JE.gadget.fields.cycleMultiPicker(gadget, "projectId", "version", "cycles",false),
            JE.gadget.fields.chartTypePicker(gadget, "chartType"),
            JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
            JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
            AJS.gadget.fields.nowConfigured()
        ]
    };
};

function addSlashes(str) {
    if(str && isNaN(str)) {
        return str.replace(/\\/g, '\\\\').
        replace(/\u0008/g, '\\b').
        replace(/\t/g, '\\t').
        replace(/\n/g, '\\n').
        replace(/\f/g, '\\f').
        replace(/\r/g, '\\r').
        replace(/'/g, '\\\'').
        replace(/"/g, '\\"');
    } else
        return str;
}