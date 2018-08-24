GADGET = {};
var MAX_LEGENDS = 10;


/**
 * Configuration screen descriptor for the execution status distribution gadget
 */
GADGET.descriptor = function (gadget, args, baseUrl) {

	gadget.gadgetType = "pastExecutionStatus";
	
    var curTitle = gadget.getPref('pastExecutionStatusName')
    if(curTitle == "Zephyr Past Test Executions")
        curTitle = gadget.getMsg('je.gadget.testcase.execution.date.status.title');
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
            JE.gadget.fields.title(gadget, "pastExecutionStatusName", 'je.gadget.common.title.label', curTitle),
            JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
            JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
            JE.gadget.fields.cyclePicker(gadget, "projectId", "version", "cycles",false),
            JE.gadget.fields.chartTypePicker(gadget, "chartType","pastExecutionStatus"),
			JE.gadget.fields.pastTestProgressGroupPicker(gadget, "howMany",false),
            JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
            JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
            AJS.gadget.fields.nowConfigured()
        ]
    };
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
            key: "executionStatusCountPerDate",
            ajaxOptions: function () {
                var gadget = this;
                var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
                var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
                var cycles = gadgets.util.unescapeString(gadget.getPref("cycles"));
                var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));
                return {
                    url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/pastExecutionsStatusCount?projectId="+ projectId +"&versionId=" + versionId +"&cycleIds=" + cycles+"&howMany="+howMany,
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
    var howMany = gadgets.util.unescapeString(gadget.getPref("howMany"));
    
    // empty content and set root classes
    gadget.getView().empty();
    var body = AJS.$('body');

    // Update the title of the gadget
    var gadgetId = AJS.$(window).attr('name');
    var gadgetTitle = gadget.getPref('pastExecutionStatusName');
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

    var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $ggWrapper);
    var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.past.testcase.execution.status.subtitle') + '</h4>';
    
    var backToAssigneeButton = '<form class="aui" style="position: absolute;right: 26px;z-index: 9999"><button style="display: none;" class="button" id="backToAssignee">Back</button></form>';
    
    AJS.$(header).appendTo($ggWrapper);
    AJS.$(backToAssigneeButton).appendTo($ggWrapper);

    var $tableWrapper = AJS.$('<div style="max-height: 500px; overflow-y: auto;" id="table-wrapper"></div>');
    AJS.$($tableWrapper).appendTo($ggWrapper);

	
    
    var renderTabularChart = function() {
    
    	var chartData = args.executionStatusCountPerDate.data;
        var chartTable;
        chartTable = '<table class="aui tablesorter" id="pastExecutionStatusTable" style="border-collapse:collapse;">';
        chartTable += '<thead> <tr>';

        chartTable += ' <th class="thStyle">'+"Date"+'</th>';

        AJS.$.each(columns, function(key, value) {
            chartTable += ' <th class="thStyle">'+gadgets.util.escapeString(key)+'</th>'
        });
        chartTable+=' <th class="thStyle">' + gadget.getMsg('je.gadget.testcase.cycle.once.exist.testcase.label') + '</th>';
        chartTable+=' <th class="thStyle">' + gadget.getMsg('je.gadget.testcase.cycle.total.testcase.distinct.remaining') + '</th>';
        chartTable+=' <th class="thStyle">'+gadget.getMsg('je.gadget.testcase.cycle.total.testcase.label')+'</th>';
		chartTable += ' </tr></thead>';

        chartTable += '<tbody>';
		Object.keys(chartData).sort(function(a, b) {
			var dateASplit = a.split("-");
			var dateA = new Date(parseInt(dateASplit[0]), parseInt(dateASplit[1]) - 1, parseInt(dateASplit[2]))
			var dateBSplit = b.split("-");
			var dateB = new Date(parseInt(dateBSplit[0]), parseInt(dateBSplit[1]) - 1, parseInt(dateBSplit[2]))
			return dateA - dateB;  
		}).forEach(function(key){
			var dateFormat = d3.time.format("%d/%b/%Y");
			var dateSplit = key.split("-");
			var date = new Date(parseInt(dateSplit[0]), parseInt(dateSplit[1]) - 1, parseInt(dateSplit[2]))
            var dateKey = gadgets.util.escapeString(dateFormat(date));
            chartTable += '<tr>';
            chartTable += '<td class="tdOrdering">' + dateKey + '</td>';
            var executionStatusData = chartData[key];

            if(executionStatusData !=null) {
                for(var status in executionStatusData) {
                     chartTable += '<td class="tdOrdering">' + executionStatusData[status] + '</td>';
                }
            }
            chartTable += '</tr>';
        });
        chartTable += '</tbody>';
        chartTable += '</table>';

		AJS.$(chartTable).appendTo($tableWrapper);
    }
    
    /**
 * dataPoints: no of
 */
 
	var renderStackBarChart =function(chartData, colors){ 

		if(chartData.length == 0) return false;
		AJS.$("#chartdiv").empty();
	
		try{
			var chartLength = chartData.length,
				widthOffset = 0;
			if(chartLength > 10) {
				widthOffset = 20;
			}
			// Remove the tooltip element if already exist
			AJS.$('.d3-tip').remove();
			// Define the tooltip element
			var tip = zephyrd3.tip()
		      			.attr('class', 'zephyr-d3-tip')
		      			.html(function(data) {
		      				var percentExec = ((data.y1 - data.y0)/data.total * 100).toFixed(2);
		      				var _statusName = JE.gadget.utils.htmlEncode(data.name);
		      				if(_statusName.indexOf(' ') == -1){
		      					_statusName = '<span style="word-break: break-all;">' + _statusName + '</span>';
	      					}
		      				return '<span>' + _statusName + ' ' + (data.y1 - data.y0) + ' of ' + data.total + ' </span><br/>' + percentExec + '%';
		      			})
		      			.offset([-12, 0]);
	
			var margin 	= {top: 20, right: 20, bottom: 0, left: 40},
				minWidth = AJS.$('#chartdiv').width() || 500;
				
				//This is to ensure that even if the project and version name is long the chart and the legends at bottom dont overlap.
				margin.bottom = ((AJS.$("#zephyr-gadget-header").text().length/60)  + 1) * 40;
				
				var width 	= minWidth - margin.left - margin.right;
			    height 	= 450 - widthOffset,
				x 		= d3.scale.ordinal().rangeRoundBands([0, width], 0.1),
				y 		= d3.scale.linear().range([height, 0]),
				color 	= d3.scale.ordinal().range(colors),
				xAxis 	= d3.svg.axis().scale(x).orient("bottom").tickFormat(function(d, i){
					var width = x.rangeBand();
					var testDateValue = chartData[i].testDate;
					testDateValue = testDateValue && testDateValue.replace(/\-/g, "/");
					var testDate = new Date(testDateValue);
					var dateFormat = d3.time.format("%d-%b");
					var labelText = dateFormat(testDate);
					return labelText;
				}),
				yAxis 	= d3.svg.axis().scale(y).orient('left').ticks(5).tickSize(-width);
			var legendArray = getLegendColorsArray(chartData);
			var svg = d3.select("#chartdiv")
						.append("svg")
					    .attr("width", width + margin.left + margin.right)
					    .attr("height", height + margin.top + margin.bottom)
					    .append("g")
					    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
	
			// Call the tooltip on svg element.
			svg.call(tip);
			AJS.$('.d3-tip').css('max-width', '200px');
			
			chartData.sort(function(a, b) {
					var testDateA = a.testDate;
					testDateA = testDateA && testDateA.replace(/\-/g, "/");
					var testDateB = b.testDate;
					testDateB = testDateB && testDateB.replace(/\-/g, "/");

			 		var dateA = new Date(testDateA).getTime();
					var dateB = new Date(testDateB).getTime();
					return dateA > dateB ? 1 : -1; 
			});
			// Calculate the x and y axis domain
			var max = d3.max(chartData, function(d) { return d.total;});
			x.domain(chartData.map(function(d) { return d.testDate; }));
			y.domain([0, (max + 1)]);
	
			// x-axis
		    if(chartLength > 10) {
		    	svg.append("g")
					.attr("class", "x axis")
					.attr('transform', 'translate(0,' + height + ')')
					.call(xAxis)
				    .selectAll("text")
				    .style("text-anchor", "end")
				    .attr("dx", "-.8em")
				    .attr("dy", ".15em")
				    .attr("transform", function (d) {
					    return "rotate(-60)";
					})
					.selectAll('.x.axis g');
		    } else {
		    	svg.append("g")
					.attr("class", "x axis")
					.attr('transform', 'translate(0,' + height + ')')
					.call(xAxis)
				    .selectAll('.x.axis g');
		    }
	
			// Append title for the x axis labels.
			svg.selectAll(".x.axis text")
				.append('title')
			 	.text(function(d, i) {return chartData[i].testDate;});
	
			// y-axis
			var yAxis = svg.append("g")
				.attr("class", "y axis")
				.call(yAxis);
				
				yAxis.selectAll("line")
					.attr("y2", function(d) { return (+d3.select(this).attr("y2") - 0.5);})
	
			  //line function for averageLine
	        var valueline = d3.svg.line()
		      .x(function(d) { return x(d.testDate) + x.rangeBand()/2; })
		      .y(function(d) { return y(d.total); });
		      
		    
			
			// Create bar elements
			var bar = svg.selectAll("g.bar")
						 .data(chartData)
						 .enter()
						 .append("g")
						 .attr("class", "g")
						 .attr("transform", function(d) { return "translate(" + x(d.testDate) + ",0)"; });
	
			bar.selectAll("rect")
			   .data(function(d) { return d.values; })
			   .enter().append("rect")
			   .attr("width", x.rangeBand())
			   .attr("y", function(d) { return y(d.y1); })
			   .attr("height", function(d) { return y(d.y0) - y(d.y1); })
			   .style("fill", function(d) { return d.color; })
			   .on('mouseover', tip.show)
			   .on('mouseout', tip.hide);
			svg.append("path")        // Add the valueline path.
			.attr("class", "totalLine")
        	.attr("d", valueline(chartData));
			
			svg.append("text")
       				 .attr("transform", "rotate(-90)")
				     .attr("y", 0 - margin.left)
				     .attr("x",0 - (height / 2))
				     .attr("dy", "1em")
				     .style("text-anchor", "middle")
				     .style('font-weight','bold')
				     .attr("font-size", "14px")
					 .text("Executions");
					 
			if(width < 320)
	  			width = 320;
			// Empty the legend element
		    d3.select('#chartLegend').html('');
	
			var legend = d3.select('#chartLegend')
						    .append('svg')
							.attr('width', width)
						    .style({
						    	'position': 'absolute',
						    	'left': 	'50%',
						    	'margin-left': -(width/2)
						    })
						    .append("g")
						    .attr('class', 'legend')
						    .attr('width', width);
			// legend variables
			var lx = 0, lWidth = 0, ly = 0, tWidth = 0;
			legend.selectAll('g')
			  		.data(legendArray)
			  		.enter()
			  		.append('g')
			  		.each(function(d, i) {
						var g = d3.select(this);
	
						if(i != 0) lx += lWidth + 10;
						if(lx > width - 60) ly = ly + 20, lx = 0;
	
							g.append("rect")
							 .attr("x",  lx)
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
							if(i != 0) tWidth += labelWidth + 20;
							else tWidth += labelWidth + 10;
	
							g.append("text")
							  .attr("x", lx)
							  .attr("y", ly + 9)
							  .attr("height", 35)
							  .attr("width", labelWidth)
							  .style("fill", "#545454")
							  .text(d.label)
							  .append('title')
							  .text(d.label);
			  		});
			ly = ly + 35; // Minimum height for one row is 35
			AJS.$('#chartLegend').css('height', ly +'px');
			var _chartHeightWithLegend = AJS.$('#chartdiv').height();
			var _totalChartHeight = (_chartHeightWithLegend + ly + widthOffset + margin.top + margin.bottom);
			AJS.$('.gg-wrapper').css('height', _totalChartHeight + 'px');
			d3.select('#chartLegend > svg').style({
				'height': ly + 'px'
			});
			// Center the legends based on their width.
			if(tWidth < width) {
				d3.select('#chartLegend > svg').style({
			    	'margin-left': -(tWidth/2)
			    });
			}
		}
		catch(exe){
			//First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
			//Since we are calling chart again during resize, do nothing in first call!
			console.log(exe);
		}
	}
    var columns = args.status;
    if(args.executionStatusCountPerDate != null && args.executionStatusCountPerDate.data != null){
        if(chartType === 'table') {
            renderTabularChart();
        } else if(chartType === 'stack'){
        	var issueTable;
			var execStatusSeries = [];
			var plot;
			var colors = [];
	        var chartData = [];
	
			//Let's make sure we have width at least 100 px. IE do not like -ve width. - Refer to Bug 4248
			var width = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
			issueTable = '<div id="chartdiv" align="center" style="height:500px; width:' + width + '; minWidth:400px; margin:auto"/><div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';
	
			AJS.$(issueTable).appendTo($ggWrapper);
	        var chartData = getParsedChartData({
	        	data: args.executionStatusCountPerDate.data,
	        	statusSeries: args.executionStatusCountPerDate.statusSeries
	        });
	
			for (var statusId in args.executionStatusCountPerDate.statusSeries){
				if(args.executionStatusCountPerDate.statusSeries[statusId]){
					colors.push({label: args.executionStatusCountPerDate.statusSeries[statusId].name, color: args.executionStatusCountPerDate.statusSeries[statusId].color});
				}
			}
			renderStackBarChart(chartData, colors);
			AJS.$(window).unbind("resize");
			AJS.$(window).bind("resize", function(){
				var newWidth = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
				var currentWidth = AJS.$("#chartdiv").width() - 50;
				/*No need to refresh when config is showing up*/
				if(AJS.$("#config").css("display") == "none"){
					AJS.$("#chartdiv").css("width", newWidth);
					renderStackBarChart(chartData, colors);
				}
			});
		}
    }
    gadgets.window.adjustHeight();
};


function getLegendColorsArray(data) {
	var legendObj = {};
	var legendArray = [];
	data.forEach(function (row) {
		row.values.forEach(function (d) {
			if (d.y0 - d.y1) {
				legendObj[d.name] = d.color;
			}
		})
	});
	for(var prop in legendObj) {
		legendArray.push({ label: prop, color: legendObj[prop]});
	};
	return legendArray;
};

var getParsedChartData = function(chartJSON) {

	var chartData = chartJSON.data;
	var statusSeries = chartJSON.statusSeries;
	var chartList = [];
	/*Iterate over all the column data, one at a time*/
	for (var i in chartData){
		var executionData = {
			testDate:		i,
			total:			chartData[i].totalTests,
			values:			[]
		};
		var y0 = 0, yT = 0;
		statusSeries[-2]={color: "#000000", name: "unscheduled", id: -2, desc: "Test was not scheduled."}
		/*Now, lets dynamically create the chart series */
		for (var statusId in statusSeries){
			var statusCnt = chartData[i][statusSeries[statusId].name] || 0 ;

			yT = y0 + statusCnt;
			executionData.values.push({
				//unscheduled coming in lowecase in response, Explicitly making the unscheduled to uppercase
				name: (statusSeries[statusId].name == "unscheduled" ? "UNSCHEDULED" : statusSeries[statusId].name),
				total: executionData.total,
				y0:		y0,
				y1:		yT,
				color: statusSeries[statusId].color
			});
			y0 = y0 + statusCnt;
		}
		chartList.push(executionData);
    }
	
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