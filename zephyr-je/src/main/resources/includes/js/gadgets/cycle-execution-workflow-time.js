GADGET = {};
var MAX_LEGENDS = 10;


function dataPresent(data) {
	 var isDataPresent = false;
	if(data.length  != 0 || Object.keys(data[0]).length != 0) {
		if (Object.keys(data[0]).forEach(function (e) {
			if (Object.keys(data[0][e]).length != 0) {
				isDataPresent = true;
			}
		}));
	}
	return isDataPresent;
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
	var curTitle = gadget.getPref('cycleExecutionTimeName');
	JE.gadget.fields.foldersArray = [];
    if(curTitle == "Test Execution Time Tracking")
        curTitle = gadget.getMsg('je.gadget.testcase.cycle.execution.time.title');
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
            JE.gadget.fields.title(gadget, "cycleExecutionTimeName", 'je.gadget.common.title.label', curTitle),
            JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
            JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
            JE.gadget.fields.cycleMultiPicker(gadget, "projectId", "version", "cycles",true),
            JE.gadget.fields.folderMultiPicker(gadget, "projectId", "version", "cycles", "folders"),
            JE.gadget.fields.chartTypePicker(gadget, "chartType","cycleExecutionTimeName"),
            JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
            JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
            AJS.gadget.fields.nowConfigured()
        ]
    };
};


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
            key: "executionTimeTrackingPerCycle",
            ajaxOptions: function () {
                var gadget = this;
                var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
                var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
                var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
                var folders = gadgets.util.unescapeString(gadget.getPref("folders"));
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/executionsTimeTrackingByCycle?projectId="+ projectId +"&versionId=" + versionId+"&cycles=" + cycles+"&folders=" + folders,
                    contentType: "application/json",
                    complete: function(xhr, status, response){
                        if(xhr.status != 200 ){
                            JE.gadget.zephyrError(xhr, gadget);
                            gadgets.window.adjustHeight();
                        }
                    },
					error: function (xhr, status, response) {
                        JE.gadget.zephyrError(xhr, gadget);
                        gadgets.window.adjustHeight();
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
    var gadgetTitle = gadget.getPref('cycleExecutionTimeName');
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

    if(chartType === 'bar') {
        var legendWrapper = '<div class="legend-wrapper"><div id="legendType"></div><ul class="pie-legend"></ul></div>';
        gadget.getView().append(legendWrapper);
    }

    var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
    if (cycles == undefined || cycles == null || cycles.length == 0){
        showErrorMessage(gadget.getMsg('zephyr.common.error.required','Cycle'))
	}

    var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $ggWrapper);
    var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.testcase.cycle.execution.time.title') + '</h4>';
    AJS.$(header).appendTo($ggWrapper);

    var $tableWrapper = AJS.$('<div style="max-height: 500px; overflow-y: auto;" id="table-wrapper"></div>');
    AJS.$($tableWrapper).appendTo($ggWrapper);

    var renderGroupedBarChart = function(chartData) {

	    if(chartData.length == 0) return false;
			AJS.$("#chartdiv").empty();
			try {
				var chartLength = chartData.length,
					widthOffset = 0;
				if (chartLength > 10) {
					widthOffset = 20;
				}
				// Remove the tooltip element if already exist
				AJS.$('.d3-tip').remove();
				//Define the tooltip element
				var tip = zephyrd3.tip()
					.attr('class', 'zephyr-d3-tip')
					.html(function (data) {
						return '<span>' + data.text + ': ' + data.label;
					}).offset([-12, 0]);

				AJS.$('.d3-tip').css('max-width', '200px');

				var margin = {
						top: 20,
						right: 20,
						bottom: 30,
						left: 40
					},
					minWidth = AJS.$('#chartdiv').width() || 500;


				var color = d3.scale.ordinal()
					.range(["#808080", "#C0C0C0", "#d5d5d5", "#92c5de", "#0571b0"]);

				var legendColorArray = [];
				chartData[0].values.sort(function (f, s) {
					if (f.name > s.name) {
						return 1;
					} else {
						return -1;
					};
				});
				var data = chartData;
				var paddingBetweenRects = 0.1;
				if (data.length > 0) {
					var cycleNames = data.map(function (d) {
						if (d.folderName && folders) {
							return d.cycleName + ' / ' + d.folderName;
						}
						return d.cycleName;
					});
					var rateNames = ["totalExecutionEstimatedTime", "totalExecutionLoggedTime"];
					var yMaxValLength = d3.max(data, function (cycleName) {
						return d3.max(cycleName.values, function (d) {
							return d.value;
						});
					}).toString().length;

					if (yMaxValLength > 3) {
						margin.left = yMaxValLength * 14;
					}

					if (data.length === 1) {
						paddingBetweenRects = 0.4;
					}

					var width = minWidth - margin.left - margin.right,
						height = 400 - widthOffset;

					var x0 = d3.scale.ordinal()
						.rangeRoundBands([0, width], paddingBetweenRects);

					var x1 = d3.scale.ordinal();

					var y = d3.scale.linear()
						.range([height, 0]);

					var xAxis = d3.svg.axis()
						.scale(x0)
						.tickSize(0)
						.orient("bottom")
						.tickFormat(function (d, i) {
							var width = x0.rangeBand();
							var txt = svg.append("text").text(d);
							var labelWidth = txt.node().getBBox().width;
							txt.remove();
							var slength = Math.floor(width / labelWidth * d.length) - 2;
							if (labelWidth > width) return d.substr(0, slength) + '...';
							else return d;
						});

					var yAxis = d3.svg.axis()
						.scale(y)
						.orient("left")
						.tickSize(-width);

					// Calculate the x and y axis domain
					x0.domain(cycleNames);
					x1.domain(rateNames).rangeRoundBands([0, x0.rangeBand()]);
					y.domain([0, d3.max(data, function (cycleName) {
						return d3.max(cycleName.values, function (d) {
							return d.value;
						});
					})]);


					var svg = d3.select('#chartdiv').append("svg")
						.attr("width", width + margin.left + margin.right)
						.attr("height", height + margin.top + margin.bottom)
						.append("g")
						.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

					// Call the tooltip on svg element.
					svg.call(tip);
					var xAxis = svg.append("g")
						.attr("class", "x axis")
						.attr("transform", "translate(0," + height + ")")
						.call(xAxis);

					xAxis.selectAll("text")
						.append('title')
						.text(function (d) {
							return d;
						});

					var yAxisCall = svg.append("g")
						.attr("class", "y axis")
						.style('opacity', '0')
						.call(yAxis);

					svg.append("text")
						.attr("transform", "rotate(-90)")
						.attr("y", 0 - margin.left)
						.attr("x", 0 - (height / 2))
						.attr("dy", "1em")
						.style("text-anchor", "middle")
						.style('font-weight', 'bold')
						.attr("font-size", "14px")
						.text("Execution Time in Hours");

					svg.select('.y').transition().duration(500).delay(1300).style('opacity', '1');

					var slice = svg.selectAll(".slice")
						.data(data)
						.enter().append("g")
						.attr("class", "g")
						.attr("transform", function (d) {
							if (folders) {
								return "translate(" + x0(d.cycleName + " / " + d.folderName) + ",0)";
							} else {
								return "translate(" + x0(d.cycleName) + ",0)";
							}
						});

					slice.selectAll("rect")
						.data(function (d) {
							return d.values;
						})
						.enter().append("rect")
						.attr("width", x1.rangeBand())
						.attr("x", function (d) {
							return x1(d.name);
						})
						.style("fill", function (d) {
							return color(d.name)
						})
						.attr("y", function (d) {
							return y(0);
						})
						.attr("height", function (d) {
							return height - y(0);
						})
						.on("mouseover", function (d) {
							d3.select(this).style("fill", d3.rgb(color(d.name)).darker(2));
							tip.show(d);
						})
						.on("mouseout", function (d) {
							d3.select(this).style("fill", color(d.name));
							tip.hide();
						});

					slice.selectAll("rect")
						.transition()
						.delay(1000)
						.duration(1000)
						.attr("y", function (d) {
							if(((height - y(d.value)) < 5) && (d.value != 0)) {
								return height - 5;
							}
							return y(d.value);
						})
						.attr("height", function (d) {
							//Explicitly assigning a height of 5px incase the bar is very small since it would be invisible in the UI.
							if(((height - y(d.value)) < 5) && (d.value != 0)) {
								return 5;
							}
							return height - y(d.value);
						});

					color.domain().forEach(function (e, i) {
						legendColorArray.push({
							label: e,
							color: color(e)
						})
					});

					//Legend
					d3.select('#chartLegend').html('');

					var legend = d3.select('#chartLegend')
						.append('svg')
						.attr('width', width)
						.style({
							'position': 'absolute',
							'left': '50%',
							'margin-left': -(width / 2)
						})
						.append("g")
						.attr('class', 'legend')
						.attr('width', width);

					// legend variables
					var lx = 0,
						lWidth = 0,
						ly = 0,
						tWidth = 0;

					legend.selectAll('g')
						.data(legendColorArray)
						.enter()
						.append('g')
						.each(function (d, i) {
							var g = d3.select(this);

							if (i != 0) lx += lWidth + 10;
							if (lx > width - 60) ly = ly + 20, lx = 0;

							if (d.label != 'folderName') {
								g.append("rect")
									.attr("x", lx)
									.attr("y", ly)
									.attr("width", 10)
									.attr("height", 10)
									.style("fill", d.color);

								// Calculate the width of the legend label
								var el = AJS.$('<span style="display:none;">' + JE.gadget.utils.htmlEncode(d.label) + '</span>').appendTo('body');
								var labelWidth = AJS.$(el).width();
								AJS.$(el).remove();
								lWidth = labelWidth;
								lx = lx + 13;
								if (i != 0) tWidth += labelWidth + 20;
								else tWidth += labelWidth + 10;

								g.append("text")
									.attr("x", lx)
									.attr("y", ly + 9)
									.attr("height", 35)
									.attr("width", labelWidth)
									.style("fill", "#545454")
									.text(d.label)
									.style("text-transform", "capitalize")
									.append('title')
									.text(d.label)
							}
						});

					ly = ly + 35; // Minimum height for one row is 35
					AJS.$('#chartLegend').css('height', ly + 'px');
					var _chartHeightWithLegend = AJS.$('#chartdiv').height();
					var _totalChartHeight = (_chartHeightWithLegend + ly + widthOffset + margin.top + margin.bottom);
					AJS.$('.gg-wrapper').css('height', _totalChartHeight + 'px');
					d3.select('#chartLegend > svg').style({
						'height': ly + 'px'
					});
					// Center the legends based on their width.
					if (tWidth < width) {
						d3.select('#chartLegend > svg').style({
							'margin-left': -(tWidth / 2)
						});
					}

				}
			}
			catch(exe){
				//First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
				//Since we are calling chart again during resize, do nothing in first call!
				console.log(exe);
			}
	}

    var renderTabularChart = function() {
        var chartTable;
        chartTable = '<table class="aui tablesorter" id="executionWorkflowTimeTable" style="border-collapse:collapse;">';
        chartTable += '<thead> <tr>';

        chartTable += ' <th class="thStyle">'+gadget.getMsg('je.gadget.common.cycle.label')+'</th>';
		if(folders || (!folders && !cycles)){
			chartTable += ' <th class="thStyle">'+gadget.getMsg('je.gadget.common.folder.label')+'</th>';
		}
        chartTable+=' <th class="thStyle">'+gadget.getMsg('je.gadget.testcase.cycle.total.estimated.time')+'</th>';
        chartTable+=' <th class="thStyle">'+gadget.getMsg('je.gadget.testcase.cycle.total.logged.time')+'</th>';
        chartTable += ' </tr></thead>';

        chartTable += '<tbody>';
		var chartData = [args.executionTimeTrackingPerCycle.data];
		var parsedData = getParsedChartData(chartData);

		for(var i =0; i < parsedData.length; i++) {
			var queryLink = 'project="' + addSlashes(project.key) + '"' + ' AND fixVersion="' + addSlashes(version.name) + '"'
			queryLink += ' AND cycleId IN (' + addSlashes(parsedData[i].cycleId) + ')';
			chartTable += '<tr>';
			chartTable += '<td class="tdOrdering"><a href="'+baseUrl + '/secure/enav/#?query='+encodeURIComponent(queryLink)+'" class="pie-legend-item-label" title="'+ parsedData[i].cycleName +'">' + parsedData[i].cycleName + '</td>';
			if((parsedData[i].folderName && folders) || (!folders && !cycles)) {
				if(folders) {
					queryLink += ' AND folderId IN (' + addSlashes(folders) + ')';
				}
				if(parsedData[i].folderName == parsedData[i].cycleName) {
					chartTable += '<td class="tdOrdering">-</td>';
				}
				else {
					chartTable += '<td class="tdOrdering"><a href="'+baseUrl + '/secure/enav/#?query='+encodeURIComponent(queryLink)+'" class="pie-legend-item-label" title="'+ parsedData[i].folderName +'">' + parsedData[i].folderName + '</a></td>';
				}
			}
			var totalExecutionEstimatedTime = parsedData[i].values[0].label;
			var totalExecutionLoggedTime = parsedData[i].values[1].label;
		 			chartTable += '<td class="tdOrdering">' + totalExecutionEstimatedTime + '</td>';
                 	chartTable += '<td class="tdOrdering">' + totalExecutionLoggedTime + '</td>';
			chartTable += '</tr>';
			}

        chartTable += '</tbody>';
        chartTable += '</table>';

		AJS.$(chartTable).appendTo($tableWrapper);
		AJS.$.tablesorter.addParser({
			// set a unique id
			id: 'timesorter',
			is: function(s) {
				// return false so this parser is not auto detected
				return false;
			},
			format: function(s) {
				//Check if the string 's' contains both hours and seconds
				if((s.indexOf('h') !== -1) && (s.indexOf('m') !== -1)) {
					var mIndex = s.indexOf('m');
					var hIndex = s.indexOf('h');
					var minutes = +s.slice(hIndex + 1, mIndex);
					var hours = +s.slice(0,hIndex);
					return ((minutes*60) + (hours * 60 * 60));
				}
				//If string 's' has only either of minutes 'm' or hours 'h'
				var num = +s.slice(0,-1);
				var time = s.slice(-1);
				if(time == 'm') {
					return num * 60
				}
				else {
					return num * 60 * 60;
				}
			  },
			// set type, either numeric or text
			type: 'text'
		});

        if(args.executionTimeTrackingPerCycle != null && args.executionTimeTrackingPerCycle.data != null && [args.executionTimeTrackingPerCycle.data].length > 0) {
			//If folders are present then the sorter should work on columns 2 and 3,
			//else on columns 1 & 2.
			if(folders) {
				AJS.$("#executionWorkflowTimeTable").tablesorter({
					headers: {
						3: {
							sorter: 'timesorter'
						},
						4: {
							sorter: 'timesorter'
						},
					},
				 }).appendTo($tableWrapper);
			}
			else {
				AJS.$("#executionWorkflowTimeTable").tablesorter({
					headers: {
						2: {
							sorter: 'timesorter'
						},
						3: {
							sorter: 'timesorter'
						},
					},
				 }).appendTo($tableWrapper);
			}
		}
	    }

	var chartData = args.executionTimeTrackingPerCycle.data;
	var parsedChartData = null;
    if(chartData != null ){
		var chartData = [chartData];
		if(!dataPresent(chartData)) {
			$ggWrapper.empty();
			$ggWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
			AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.nodata.label'), closeable: false});
			return;
		}
		if(chartType === 'table') {
            renderTabularChart();
        } else if(chartType === 'bar') {
            var data = [];
			var width = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
			issueTable = '<div id="chartdiv" align="center" style="height:500px; width:' + width + '; minWidth:400px; margin:auto"/><div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';
	
			AJS.$(issueTable).appendTo($ggWrapper);
				parsedChartData = getParsedChartData(chartData);
				renderGroupedBarChart(parsedChartData);
				AJS.$(window).unbind("resize");
				AJS.$(window).bind("resize", function(){
					var newWidth = gadget.getView()[0].parentNode.offsetWidth - 50;
					var currentWidth = AJS.$("#chartdiv").width();
					/*No need to refresh when config is showing up*/
					if(AJS.$("#config").css("display") == "none"){
						AJS.$("#chartdiv").css("width", newWidth);
						renderGroupedBarChart(parsedChartData);
					}
				});
        }
    } else { 
    	$ggWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
                AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.execution.workflow.disabled.label'), closeable: false});
    	AJS.$(window).unbind("resize");
    }
};

var getParsedChartData = function(chartJSON) {
	var chartData = chartJSON[0];
	var chartList = [];
	for (var cycle in chartData) {
		for(var folder in chartData[cycle]) {
			var obj = {
				cycleName: cycle.slice(0, cycle.lastIndexOf("-")),
				cycleId:  cycle.slice(cycle.lastIndexOf("-") + 1),
				folderName: folder.slice(folder.indexOf("-") + 1),
				values: [
					{
						label: chartData[cycle][folder].totalExecutionEstimatedTimeDurationStr,
						value: (chartData[cycle][folder].totalExecutionEstimatedTime / 3600),
						text: "Estimated Time Duration",
						name: "totalExecutionEstimatedTime"
					}, {
						label: chartData[cycle][folder].totalExecutionLoggedTimeDurationStr,
						value: (chartData[cycle][folder].totalExecutionLoggedTime/ 3600),
						text: "Logged Time Duration",
						name: "totalExecutionLoggedTime"
					}
				]
			};
			chartList.push(obj);
		}
	};
	return chartList;
}
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
