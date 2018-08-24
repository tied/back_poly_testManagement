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
            key: "executionStatusPerCycle",
            ajaxOptions: function () {
                var gadget = this;
                var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
                var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
                var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
                var folders = gadgets.util.unescapeString(gadget.getPref("folders"));
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/executionsStatusCountByCycle?projectId="+ projectId +"&versionId=" + versionId+"&cycles=" + cycles+"&folders=" + folders,
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
    var folders = gadgets.util.unescapeString(gadget.getPref("folders"));

    // empty content and set root classes
    gadget.getView().empty();
    var body = AJS.$('body');

    // Update the title of the gadget
    var gadgetId = AJS.$(window).attr('name');
    var gadgetTitle = gadget.getPref('cycleExecutionStatusName');
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

    var chartType = gadget.getPref('chartType');

    if(chartType === 'pie') {
        var legendWrapper = '<div class="legend-wrapper"><div id="legendType"></div><ul class="pie-legend"></ul></div>';
        gadget.getView().append(legendWrapper);
    }

    var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $ggWrapper);
    var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.testcase.execution.cycle.status.subtitle') + '</h4>';
    AJS.$(header).appendTo($ggWrapper);

    var $tableWrapper = AJS.$('<div style="max-height: 500px; overflow-y: auto;" id="table-wrapper"></div>');
    AJS.$($tableWrapper).appendTo($ggWrapper);

    var legendsContent = '';
    var otherLegendsContent = '';
    var otherLegendsCount;

    var renderOtherLegends = function(d, total) {
        var cycleIds = cycles.split("|").join(",");
        var folderIds = folders.split("|").join(",");
        var hrefLink;
        if(cycleIds) {
            if(folderIds) {
            	hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' + ' AND folderId IN (' + addSlashes(folderIds) + ')' +
                ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
            } else {
            	hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' +
                ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
            }
        } else {
            hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
        }
        otherLegendsContent += '<div><span title="'+d.data.title+'" class="inlineDialogEllipses"><a href="'+baseUrl + '/secure/enav/#?query='+encodeURIComponent(hrefLink)+'" class="pie-legend-item-label" title="'+d.data.title+'">'+d.data.title+'</a>' +
            '</span><span style="float: right;">'+total+'</span></div>';
    }

    var renderLegend = function(d, color, header, count, index) {
        if(index === MAX_LEGENDS) {
            legendsContent += '<li class="pie-legend-item"><div class="pie-legend-content"><div class="pie-legend-item-wrapper"><div class="pie-other-legend-value pie-legend-item-value">'+count+'</div><div class="pie-legend-item-label-wrapper"><div class="icon piechart-fill legend-icon" style="background-color: '+color+'"></div><a id="otherLegendsinlineDialog" class="pie-legend-item-label" title="Other...">Other...</a></div></div></div></li>';
            if(!isNaN(parseInt(count, 10))) {
                otherLegendsCount = parseInt(count, 10);
            }
            renderOtherLegends(d, count);
            return;
        }
        if(index > MAX_LEGENDS) {
            if(!isNaN(parseInt(count, 10))) {
                otherLegendsCount += parseInt(count, 10);
                otherLegendsCount = parseInt(otherLegendsCount, 10);
            }
            renderOtherLegends(d, count);
            return;
        }
        var cycleIds = cycles.split("|").join(",");
        var folderIds = folders.split("|").join(",");
        var hrefLink;
        if(cycleIds) {
            if(folderIds) {
            	hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' + ' AND folderId IN (' + addSlashes(folderIds) + ')' +
                ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
            } else {
            	hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' +
                ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
            }
        } else {
            hrefLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(d.data.title)) + '"';
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
    }

    var renderPieChart = function(data) {
        var pieChartOptions = { height: 500, width: 700, graph: $ggWrapper, MAX_LEGENDS: MAX_LEGENDS, pieWrapperClass: 'execution-donut-wrapper' };
        var donutDrillDownChart = new DonutDrillDownChart(pieChartOptions);
        var header = 'Execution Status';
        donutDrillDownChart.render(data, null, function(d, color, i) {
            renderLegend(d, color, header, d.data.total, i);
        }, function() {
            initDialog(header);
        });
    }

    var renderTabularChart = function() {
        var chartTable;
        var query;

        chartTable = '<table class="aui tablesorter" id="executionStatusTable" style="border-collapse:collapse;">';
        chartTable += '<thead> <tr>';
        chartTable += ' <th class="thStyle">'+"Execution Status"+'</th>';
        chartTable += ' <th class="thStyle">'+"Count"+'</th>';

        chartTable += ' </tr></thead>';

        chartTable += '<tbody>';

        args.executionStatusPerCycle.forEach(function(status) {
            var cycleIds = cycles.split("|").join(",");
            var folderIds = folders.split("|").join(",");
            if(cycleIds) {
                if(folderIds) {
                	query = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' + ' AND folderId IN (' + addSlashes(folderIds) + ')' + ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(status.statusName)) + '"';
                } else {
                	query = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND cycleId IN (' + addSlashes(cycleIds) + ')' + ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(status.statusName)) + '"';
                }
            } else {
                query = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"' + ' AND executionStatus="' + addSlashes(gadgets.util.unescapeString(status.statusName)) + '"';
            }
            var queryLink = "<a href='"+ baseUrl + '/secure/enav/#?query='+encodeURIComponent(query) +"'>"+gadgets.util.escapeString(status.statusName)+ "</a>";

            chartTable += '<tr>';
            /*chartTable += '<td class="tdOrdering">' + gadgets.util.escapeString(status.statusName) + '</td>';*/
            chartTable += '<td class="tdOrdering">' + queryLink + '</td>';
            chartTable += '<td class="tdOrdering">' + status.statusCount + '</td>';
            chartTable += '</tr>';
        });
        chartTable += '</tbody>';
        chartTable += '</table>';

        AJS.$(chartTable).appendTo($tableWrapper);
    }

    if(args.executionStatusPerCycle != null ){
        var chartData = args.executionStatusPerCycle;
        if(chartType === 'table') {
            renderTabularChart();
        } else if(chartType === 'pie') {
            var data = [];
            var zeroDataCount = 0;
            chartData.forEach(function(cd) {
                var obj = {};
                obj['title'] = gadgets.util.escapeString(cd['statusName']);
                obj['total'] = cd['statusCount'];
                if(!obj['total']) {
                    zeroDataCount++;
                }
                obj['color'] = cd['statusColor'];
                data.push(obj);
            });
            if(zeroDataCount === chartData.length) {
                $ggWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
                AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.nodata.label'), closeable: false});
            } else {
                renderPieChart(data);
            }
        }
    } else AJS.$(window).unbind("resize");
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
    var curTitle = gadget.getPref('cycleExecutionStatusName')
    if(curTitle == "Test Execution Progress")
        curTitle = gadget.getMsg('je.gadget.testcase.execution.cycle.status.title');
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
            JE.gadget.fields.title(gadget, "cycleExecutionStatusName", 'je.gadget.common.title.label', curTitle),
            JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
            JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
            JE.gadget.fields.cycleMultiPicker(gadget, "projectId", "version", "cycles",false),
            JE.gadget.fields.folderMultiPicker(gadget, "projectId", "version", "cycles", "folders"),
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
