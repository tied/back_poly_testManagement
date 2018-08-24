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
				return {
					url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/util/versionBoard-list?projectId="+ projectId +"&versionId=" + versionId,
					contentType: "application/json",
					complete: function(xhr, status, response){
	        			   if(xhr.status != 200 ){
	        				   JE.gadget.zephyrError(xhr, gadget);
	           			   }
					}
				};
			}
		},
	    {
	    	key: "schedules",
	    	ajaxOptions: function () {
	    		var gadget = this;
	    		var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
	    		var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
	    		var groupFld = gadgets.util.unescapeString(gadget.getPref("groupFld"));
	    		var showOthers = gadgets.util.unescapeString(gadget.getPref("groupFld"));
    			var itemsXAxis = gadgets.util.unescapeString(gadget.getPref("itemsXAxis"));
    			var sprintCycleParams = '';
	    		if(groupFld == 'sprint-cycle') {
	    			var itemsPerGroup = gadgets.util.unescapeString(gadget.getPref("itemsPerGroup"));
	    			sprintCycleParams = '&itemsPerGroup=' + itemsPerGroup;
	    		}
	    		return {
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/count?projectId="+ projectId +"&versionId=" + versionId+"&groupFld="+groupFld + '&itemsXAxis=' + itemsXAxis  + '&showOthers=' + showOthers + sprintCycleParams,
	    			contentType: "application/json",
	    			complete: function(xhr, status, response){
         			   if(xhr.status != 200 ){
         				  JE.gadget.zephyrError(xhr, gadget);
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
	var groupFld = gadgets.util.unescapeString(gadget.getPref("groupFld"));

	// empty content and set root classes
	gadget.getView().empty();
	var body = AJS.$('body');
	//body.addClass('days-remaining-gadget');

	// Update the title of the gadget
	var gadgetId = AJS.$(window).attr('name');
	var gadgetTitle = gadget.getPref('testcase_execution_name');
	JE.gadget.utils.setGadgetTitle(gadgetId, gadgetTitle);

	/**
	 * Renders an error message
	 */
	var showErrorMessage = function(message) {
		gadget.getView().append(AJS.$('<div class="gg-error-message">'+message+'</div>'));
	};

	// check whether we got a version
	/*if (! version || ! version.id) { // with auto we get back a partial version object instead of no version object!
		showErrorMessage(gadget.getMsg('gh.gadget.days.remaining.noReleaseDatesWarning'));
		return;
	} else if (! version.isDisplayable) {
		showErrorMessage(gadget.getMsg('gh.gadget.days.remaining.noReleaseDateText'));
		return;
	}*/

	// root element
	var $zWrapper = AJS.$('<div style="height: 600px; "/>').attr('class', 'gg-wrapper zephyr-chart');
	gadget.getView().append($zWrapper);

	// title with project and version links
	var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $zWrapper);
	var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.testcase.execution.subtitle', JE.gadget.fields.getPickerLabel(gadget, JE.gadget.fields.executionGroupFldWithSprintPickerLov, groupFld)) + '</h4>';
	AJS.$(header).appendTo($zWrapper);

	var issueTable;
	var execStatusSeries = [];
	var plot;
	if(args.schedules != null && args.schedules.data != null && args.schedules.data.length > 0) {
		var colors = [];
        var chartData = [];
        var showOthers = gadgets.util.unescapeString(gadget.getPref("showOthers"));
        var itemsXAxis = gadgets.util.unescapeString(gadget.getPref("itemsXAxis"));
        var itemsPerGroup = gadgets.util.unescapeString(gadget.getPref("itemsPerGroup"));
        var sortSprintBy = gadgets.util.unescapeString(gadget.getPref("sortSprintBy"));
        ZEPHYR.Execution.chartType = 'gadget';

		//Let's make sure we have width at least 100 px. IE do not like -ve width. - Refer to Bug 4248
		var width = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
		issueTable = '<div id="chartdiv" align="center" style="height:500px; width:' + width + '; minWidth:400px; margin:auto"/><div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';

		AJS.$(issueTable).appendTo($zWrapper);
        var chartData = ZEPHYR.Execution.getParsedChartData({
        	groupType: args.schedules.groupFld,
        	showOthers: showOthers,
        	itemsXAxis: itemsXAxis,
        	itemsPerGroup: itemsPerGroup,
        	sortSprintBy: sortSprintBy,
        	data: args.schedules.data,
        	statusSeries: args.schedules.statusSeries,
        	othersLabel: gadget.getMsg('je.gadget.common.others.label'),
        	noSprintLabel: gadget.getMsg('je.gadget.common.no.sprint.label')
        });

		for (var statusId in args.schedules.statusSeries){
			if(args.schedules.statusSeries[statusId]){
				colors.push({label: args.schedules.statusSeries[statusId].name, color: args.schedules.statusSeries[statusId].color});
			}
		}

		//ZFJ-2172 Added exclusion of 'UNEXECUTED' status for group 'USER'.
		if (groupFld == 'user') {
			function removeUnexecutedStatusForUserGroup(element) {
				return element.label != 'UNEXECUTED';
			}
			colors = colors.filter(removeUnexecutedStatusForUserGroup);
		}

		if(groupFld == 'sprint-cycle') {
		//	showChartGroupedBySprint(chartData.groupedData, chartData.innerColumns, colors);
		} else {
			plot = showChart(chartData, colors);
		}

		//showChartGroupedBySprint();
		AJS.$(window).unbind("resize");
		AJS.$(window).bind("resize", function(){
			var newWidth = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
			var currentWidth = AJS.$("#chartdiv").width() - 50;
			/*No need to refresh when config is showing up*/
			if(AJS.$("#config").css("display") == "none"){
				AJS.$("#chartdiv").css("width", newWidth);
				if(groupFld == 'sprint-cycle') {
					ZEPHYR.Execution.getParsedChartData({
			        	groupType: args.schedules.groupFld,
			        	showOthers: showOthers,
			        	itemsXAxis: itemsXAxis,
			        	itemsPerGroup: itemsPerGroup,
			        	sortSprintBy: sortSprintBy,
			        	data: args.schedules.data,
			        	statusSeries: args.schedules.statusSeries,
			        	othersLabel: gadget.getMsg('je.gadget.common.others.label'),
			        	noSprintLabel: gadget.getMsg('je.gadget.common.no.sprint.label')
			        });
				} else {
					showChart(chartData, colors);
				}
			}
		});
	} else {
        $zWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
        AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.nodata.label'), closeable: false});
        AJS.$(window).unbind("resize");
    }
};

/**
 * dataPoints: no of
 */
function showChart(chartData, colors){
	/*var chart = new FusionCharts("../../../download/resources/com.thed.zephyr.je:zephyr-gadget-fusion-chart-resources/fusioncharts/FCF_StackedColumn3D.swf", "ChartId", "600", "500");
	chart.setDataXML(xmlData);
	chart.setTransparent(true);
	chart.render("chartdiv");
	*/

	if(chartData.length == 0) return false;
	AJS.$("#chartdiv").empty();

	try{
		var chartLength = chartData.length,
			widthOffset = 0;
		if(chartLength > 10) {
			widthOffset = 20;
		}
		// Remove the tooltip element if already exist
		AJS.$('.zephyr-d3-tip').remove();
		// Define the tooltip element
		var tip = zephyrd3.tip()
	      			.attr('class', 'zephyr-d3-tip')
	      			.html(function(data) {
                        var _statusName = JE.gadget.utils.htmlEncode(data.name);
                        if(_statusName.indexOf(' ') == -1){
                            _statusName = '<span style="word-break: break-all;">' + _statusName + '</span>';
                        }
	      				if(data.total == 0) {
                            return '<span>' + _statusName + ' ' + (data.y1 - data.y0) + ' of ' + data.total + ' </span><br/>';
						} else {
                            var percentExec = ((data.y1 - data.y0)/data.total * 100).toFixed(2);
                            return '<span>' + _statusName + ' ' + (data.y1 - data.y0) + ' of ' + data.total + ' </span><br/>' + percentExec + '%';
                        }
					})
	      			.offset([-12, 0]);

		var margin 	= {top: 20, right: 20, bottom: 60, left: 40},
			minWidth = AJS.$('#chartdiv').width() || 500,
		    width 	= minWidth - margin.left - margin.right;
		    height 	= 450 - widthOffset,
			x 		= d3.scale.ordinal().rangeRoundBands([0, width], 0.1),
			y 		= d3.scale.linear().range([height, 0]),
			color 	= d3.scale.ordinal().range(colors),
			xAxis 	= d3.svg.axis().scale(x).orient("bottom").tickFormat(function(d, i){
				var width = x.rangeBand();
				var labelText = chartData[i].testCycleName;
				if(chartLength > 10) {
					if(labelText.length > 8) return labelText.substr(0, 7) + '...'; else return labelText;
				} else {
					var el = AJS.$('<span />').css({'display': 'none', 'white-space': 'nowrap'}).text(labelText).appendTo('#chartdiv');
					var labelWidth = AJS.$(el).width();
					AJS.$(el).remove();
					var slength = Math.floor(width / labelWidth * labelText.length) - 2;
					if(labelWidth > width) return labelText.substr(0, slength) + '...'; else return labelText; // TODO: find an alternative to truncate using css
				}
			}),
			yAxis 	= d3.svg.axis().scale(y).orient('left').tickSize(-width);
		
		var legendArray = getLegendColorsArray(chartData);
	
		var svg = d3.select("#chartdiv")
					.append("svg")
				    .attr("width", width + margin.left + margin.right)
				    .attr("height", height + margin.top + margin.bottom)
				    .append("g")
				    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

		// Call the tooltip on svg element.
		svg.call(tip);
		AJS.$('.zephyr-d3-tip').css('max-width', '200px');
		// Calculate the x and y axis domain
		var max = d3.max(chartData, function(d) { return d.total;});
		x.domain(chartData.map(function(d) { return d.testCycle; }));
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
		 	.text(function(d, i) {return chartData[i].testCycleName;});

		// y-axis
		svg.append("g")
			.attr("class", "y axis")
			.call(yAxis);

		// Create bar elements
		var bar = svg.selectAll("g.bar")
					 .data(chartData)
					 .enter()
					 .append("g")
					 .attr("class", "g")
					 .attr("transform", function(d) { return "translate(" + x(d.testCycle) + ",0)"; });

		bar.selectAll("rect")
		   .data(function(d) { return d.values; })
		   .enter().append("rect")
		   .attr("width", x.rangeBand())
		   .attr("y", function(d) { return y(d.y1); })
		   .attr("height", function(d) { return y(d.y0) - y(d.y1); })
		   .style("fill", function(d) { return d.color; })
		   .on('mouseover', tip.show)
		   .on('mouseout', tip.hide);

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


//	return plot
}

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
}

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
        			   }
        		   }
        	   };
           }
       }
   ];
};

/**
* Configuration screen descriptor for the days remaining gadget
*/
GADGET.descriptor = function (gadget, args, baseUrl) {
	var curTitle = gadget.getPref('testcase_execution_name')
	/*If pref is default, then internationalize it, else leave whatever user has chosen*/
	if(curTitle == "Test Execution")
		curTitle = gadget.getMsg('je.gadget.testcase.execution.title');
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
            JE.gadget.fields.title(gadget, "testcase_execution_name", 'je.gadget.common.title.label', curTitle),
			JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
			JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
			JE.gadget.fields.executionGroupFldPicker(gadget, "projectId", "groupFld", false, args.projectOptions.options),
			JE.gadget.fields.getNumberOfItems(gadget, 'itemsXAxis', 'groupFld'),
			//GH.gadget.fields.checkbox(gadget, "showMasterOrChild", 'gh.gadget.common.options.parentdetails', gadget.getPref('showMasterOrChild') == 'true'),
			JE.gadget.fields.checkbox(gadget, "showOthers", gadget.getMsg('je.gadget.common.showOthers.label'), gadget.getPref('showOthers') == 'true'),
			JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
	        JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
           AJS.gadget.fields.nowConfigured()
       ]
   };
};