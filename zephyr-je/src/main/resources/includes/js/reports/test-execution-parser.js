/**
 * Zephyr Test Board View
 */
if (typeof ZEPHYR == 'undefined') { var ZEPHYR = {}; }
if (typeof ZEPHYR.Execution == 'undefined') { ZEPHYR.Execution = {}; }

ZEPHYR.Execution.bucketizeChartData = function(chartData, statusSeries) {
	var chartList = [];
	/*Iterate over all the column data, one at a time*/
	for (var i in chartData){
		var executionData = {
			testCycle:		chartData[i].id,
			testCycleName:  chartData[i].name,
			total:			chartData[i].cnt.total,
			values:			[]
		};
		var y0 = 0, yT = 0;
		/*Now, lets dynamically create the chart series */
		for (var statusId in statusSeries){				
			var statusCnt = chartData[i].cnt[statusId] || 0 ;

			yT = y0 + statusCnt;
			executionData.values.push({
				name: statusSeries[statusId].name,
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

ZEPHYR.Execution.parseOtherCntData = function(othersLabel, othersData) {
	var othersJSON = {
		name: othersLabel,
		cnt: {}
	};
	_.each(othersData, function(otherData) {
		_.each(otherData.cnt, function(oCnt, cntKey){ 
			if(othersJSON.cnt && othersJSON.cnt[cntKey]) {
				othersJSON.cnt[cntKey] = othersJSON.cnt[cntKey] + oCnt
			} else {
				othersJSON.cnt[cntKey] = oCnt;
			}
		});
	});
	
	return othersJSON;
}

ZEPHYR.Execution.parseChartData = function(data, statusSeries, showOthers, itemsXAxis, othersLabel) {
	var chartData = data;
	var totalCount = data.length;
	var itemsXAxis = parseInt(itemsXAxis);
	if(itemsXAxis) {
		chartData = data.slice(0, itemsXAxis);
		/**
		 * Show Others, if the no of items is is less than total count and showOthers is enabled
		 */
		if(totalCount > itemsXAxis && (showOthers == 'true' || showOthers == true)) {
			var othersData = _.rest(data, itemsXAxis);			
			var othersJSON = ZEPHYR.Execution.parseOtherCntData(othersLabel, othersData);
			chartData.push(othersJSON);
		}
	}
	var chartList = ZEPHYR.Execution.bucketizeChartData(chartData, statusSeries);
	return chartList;
}


ZEPHYR.Execution.getSprintDetailsFromSprintIds = function(sprintIds, callBack) {
	ZEPHYR.Execution.sprints = [];
	if(ZEPHYR.Execution.sprintCount != 0)
		return;
	else
		ZEPHYR.Execution.sprintCount = 1;	
	
	for(var i=0; i <sprintIds.length; i++) {
		ZEPHYR.Execution.getSprintDetailsFromId(sprintIds[i], sprintIds.length, callBack);
	}
}

ZEPHYR.Execution.getSprintDetailsFromId = function(sprintId, sprintKeyLength, callBack) {
	AJS.$.ajax({
        url: "/rest/agile/1.0/sprint/" + sprintId,
        contentType: "application/json",
        success: function (sprint) {
        	ZEPHYR.Execution.sprints.push(sprint);
             if(ZEPHYR.Execution.sprintCount == sprintKeyLength) {
             	ZEPHYR.Execution.sprintCount = 0;
             	callBack();
             } else
             	ZEPHYR.Execution.sprintCount++;
        },
        error: function() {
        	var _sprintId = sprintId.toString();
        	if(sprintId) {
	        	if(!ZEPHYR.Execution.dataGroupedBySprint['-1']) 
	        		ZEPHYR.Execution.dataGroupedBySprint['-1'] = [];
	        	if(ZEPHYR.Execution.dataGroupedBySprint[sprintId]) { // Add the data to No Sprint
	        		for (var i=0; i < ZEPHYR.Execution.dataGroupedBySprint[sprintId].length; i++) {
	        			ZEPHYR.Execution.dataGroupedBySprint['-1'].push( ZEPHYR.Execution.dataGroupedBySprint[sprintId][i] );
	        		}
	        		delete ZEPHYR.Execution.dataGroupedBySprint[sprintId];	
	        	}
        	}
        	
        	if(ZEPHYR.Execution.sprintCount == sprintKeyLength) {
             	ZEPHYR.Execution.sprintCount = 0;
             	callBack();
             } else
             	ZEPHYR.Execution.sprintCount++;
        	AJS.$('#ag-sys-msg').remove();
        }
    });
}

ZEPHYR.Execution.sortSprintsByParam = function(sprintList, sortSprintBy) {
	var sortedSprintList = [];
	// Calculate the total
	_.each(sprintList, function(sprint, key) {
		// Set the total execution count per sprint
		sprintList[key].total = _.reduce(sprint, function(memo, num){ return memo + num.cnt.total; }, 0);
	});
	if(sortSprintBy == 'count') {
		// Sort the list in decending order
		var sortedSprintList = _.sortBy(sprintList, function(sprint){ return sprint['total']; }).reverse();
	} else if(sortSprintBy == 'date') {
		// Sort the sprints by startDate
		var sortedSprintList = _.sortBy(sprintList, function(sprint){ 
			if(sprint['startDate']) {
				var _startDate = new Date(sprint['startDate']);
				return _startDate.getTime();
			}
		});
	} else {
		// Sort by name is the default action
		var sortedSprintList = ZEPHYR.Execution.sortSprintListByName(sprintList);
	}
	return sortedSprintList;
}

ZEPHYR.Execution.parseOtherSprintData = function(othersData, othersLabel) {
	var index = 0;
	var othersList = [];
		othersList['sprintId'] = -2; // Considering SprintId is -2 for others
		othersList['sprintName'] = othersLabel;
		othersList['total'] = 0;
	_.each(othersData, function(otherData) {
		othersList['total'] += parseInt(otherData.total);
		_.each(otherData, function(oCnt, cntKey){ 
			if(cntKey != 'total' || cntKey != 'sprintId') {
				othersList[index] = oCnt;
				index++;
			}
		});
	});
	
	return othersList;
}

/**
 * Sort by name
 */
ZEPHYR.Execution.sortSprintListByName = function(sprintList) {
	return _.sortBy(sprintList, function(sprint) {
		if(sprint['sprintId'] > -1) {			// If 'No Sprint' do not sort by name so that it is placed at end
			return sprint['sprintName'].toLowerCase();
		}
	});
}

ZEPHYR.Execution.filterSprintList = function(sprintList, showOthers, othersLabel, itemsXAxis, itemsPerGroup) {
	var chartData = [];
	var totalCount = sprintList.length;
	itemsXAxis = parseInt(itemsXAxis);
	itemsPerGroup = parseInt(itemsPerGroup);
	
	if(itemsXAxis) {
		chartData = sprintList.slice(0, itemsXAxis);
		chartData = ZEPHYR.Execution.sortSprintListByName(chartData); 	// Sort the list by name before 'Others' as to place 'Others' at end
		/**
		 * Show Others, if the no of items is is less than total count and showOthers is enabled
		 */
		if(totalCount > itemsXAxis && (showOthers == 'true' || showOthers == true)) {
			var othersData = _.rest(sprintList, itemsXAxis);			
			var otherData = ZEPHYR.Execution.parseOtherSprintData(othersData, othersLabel);
			chartData.push(otherData);
		}
	} else {
		chartData = ZEPHYR.Execution.sortSprintListByName(sprintList); 
	}
	var groupedData = [];
	if(itemsPerGroup) {
		_.each(chartData, function(data) {
			var sortedList = _.sortBy(data, function(cycle){ return cycle.cnt.total; }).reverse();
			var _totalCount = sortedList.length;
			var _grpData = sortedList.slice(0, itemsPerGroup); 
			_grpData = _.sortBy(_grpData, function(cycle){ if(cycle.cnt.total) return (cycle.name).toLowerCase(); });
			_grpData['total'] = data['total'];
			_grpData['sprintId'] = data['sprintId'];
			_grpData['sprintName'] = data['sprintName'];
			_grpData['startDate'] = data['startDate'] || '';
			_grpData['endDate'] = data['endDate'] || '';
			_grpData['state'] = data['state'] || '';
			/**
			 * Show Others, if the no of items is is less than total count and showOthers is enabled
			 */
			if(_totalCount > itemsPerGroup && (showOthers == 'true' || showOthers == true)) {
				var othersData = _.rest(sortedList, itemsPerGroup);			
				var othersJSON = ZEPHYR.Execution.parseOtherCntData(othersLabel, othersData);
			//	othersJSON['name'] = 'zfj-item-others';
				_grpData.push(othersJSON);
			}
			groupedData.push(_grpData);
		});
	} else {
		groupedData = chartData;
	}
	
	return groupedData;
}

ZEPHYR.Execution.bucketizeSprintCycleData = function(dataGroupedBySprint, statusSeries, showOthers, othersLabel, itemsXAxis, itemsPerGroup, sortSprintBy, noSprintLabel) {
	var chartDataGroupedBySprint = [];
	var colors = [];
	var chartData = [];
	var innerColumns = {};
	// Merge sprints data with sprint cycleData
	_.each(ZEPHYR.Execution.sprints, function(sprint) {
		if(dataGroupedBySprint[sprint.id]) {
			dataGroupedBySprint[sprint.id].sprintId = sprint.id;
			dataGroupedBySprint[sprint.id].sprintName = sprint.name;
			dataGroupedBySprint[sprint.id].startDate = sprint.startDate || '';
			dataGroupedBySprint[sprint.id].endDate = sprint.endDate || '';
			dataGroupedBySprint[sprint.id].state = sprint.state || '';
		}
	});
	var sortedSprintList = ZEPHYR.Execution.sortSprintsByParam(dataGroupedBySprint, sortSprintBy);
	var filterSprintList = ZEPHYR.Execution.filterSprintList(sortedSprintList, showOthers, othersLabel, itemsXAxis, itemsPerGroup);
	_.each(filterSprintList, function(chartJSONList, key) {	
		var executionData = {
			sprintId:		chartJSONList['sprintId'],
			sprintName:  	chartJSONList['sprintName'],
			total:			chartJSONList['total'],
			values:			[]
		};
		_.each(chartJSONList, function(dataList, i) {
			var y0 = 0, yT = 0;
			if(!dataList.id)
				dataList.id = -2; // Cycle id for 'Others'
			/*Now, lets dynamically create the chart series */
			for (var statusId in statusSeries){				
				var statusCnt = dataList.cnt[statusId] || 0 ;
				if(statusCnt) {
					yT = y0 + statusCnt;
					executionData.values.push({
						name: statusSeries[statusId].name,
						cycleName: dataList.name,
						cycleId: dataList.id,
						total: dataList.cnt.total,
						y0:		y0,
						y1:		yT,
						color: statusSeries[statusId].color
					});
					y0 = y0 + statusCnt;
				}
			}
		});
		chartDataGroupedBySprint.push(executionData);
	});
	for (var statusId in statusSeries){
		if(statusSeries[statusId]){
			colors.push({label: statusSeries[statusId].name, color: statusSeries[statusId].color});
		}
	}	
	showChartGroupedBySprint(chartDataGroupedBySprint, colors, showOthers, itemsPerGroup, othersLabel);
}

ZEPHYR.Execution.parseSprintCycleData = function(data, statusSeries, showOthers, itemsXAxis, othersLabel, itemsPerGroup, sortSprintBy, noSprintLabel) {
	ZEPHYR.Execution.dataGroupedBySprint = _.groupBy(data, function(cycle){ 
		if(!cycle.sprintId) { 
			cycle.sprintId = '-1';		// If cycle does not belong to any sprint add it to 'No Sprint'
		} 
		return cycle.sprintId; 
	});
	var ChartData = {};
	var sprintIds = _.keys(ZEPHYR.Execution.dataGroupedBySprint);
	sprintIds = _.without(sprintIds, '-1', -1); // Filter out the -1 (No sprint)
	if(sprintIds && sprintIds.length) {
		ZEPHYR.Execution.sprintCount = 0;
		ZEPHYR.Execution.getSprintDetailsFromSprintIds(sprintIds, function() {
			// Add 'No Sprint' data
			ZEPHYR.Execution.sprints.push({
				id: -1,
				name: noSprintLabel
			});
			ZEPHYR.Execution.bucketizeSprintCycleData(ZEPHYR.Execution.dataGroupedBySprint, statusSeries, showOthers, othersLabel, itemsXAxis, itemsPerGroup, sortSprintBy, noSprintLabel);
		});
	} else {
		// Add 'No Sprint' data
		ZEPHYR.Execution.sprints = [{
			id: -1,
			name: noSprintLabel
		}];
		ZEPHYR.Execution.bucketizeSprintCycleData(ZEPHYR.Execution.dataGroupedBySprint, statusSeries, showOthers, othersLabel, itemsXAxis, itemsPerGroup, sortSprintBy, noSprintLabel);
	}
	return ChartData;
}

ZEPHYR.Execution.getParsedChartData = function(chartJSON) {
	var groupType = chartJSON.groupType;
	// Passing othersLabel as the way to get i18n messages in gadgets and report are different
	if(groupType == 'sprint-cycle') {
		ZEPHYR.Execution.parseSprintCycleData(chartJSON.data, chartJSON.statusSeries, chartJSON.showOthers, 
				chartJSON.itemsXAxis, chartJSON.othersLabel, chartJSON.itemsPerGroup, chartJSON.sortSprintBy, chartJSON.noSprintLabel);
	} else {
		return ZEPHYR.Execution.parseChartData(chartJSON.data, chartJSON.statusSeries, chartJSON.showOthers, chartJSON.itemsXAxis, chartJSON.othersLabel);
	}
}

function showChartGroupedBySprint(data, colors, showOthers, itemsPerGroup, othersLabel) {
	if(data.length == 0) return false;
	AJS.$("#chartdiv").empty();
	itemsPerGroup = parseInt(itemsPerGroup);
	
	try{	
		/**
		 * itemsPerGroup is max of values length
		 */
		var _itemsPerGroup = d3.max(data, function(d) { return d.values.length; });
		if(itemsPerGroup == 0 || _itemsPerGroup < itemsPerGroup) {
			itemsPerGroup = _itemsPerGroup;
		}
		if(showOthers == 'true') {
			itemsPerGroup++;
		}
		
		var chartLength = data.length,
			widthOffset = 0;
		if(chartLength > 10)
			widthOffset = 20;
		// Remove the tooltip element if already exist
		AJS.$('.zephyr-d3-tip').remove();
		// Define the tooltip element
		var tip = zephyrd3.tip()
	      			.attr('class', 'zephyr-d3-tip')
	      			.html(function(data) {
	      				var _cycleName = '';
	      				if(data.cycleId != -2) {
	      					_cycleName = 'CycleName: ' + data.cycleName + '<br/>';
	      					if(data.cycleName.indexOf(' ') == -1) { // Condition if no space then break all words
	      						_cycleName = '<span style="word-break: break-all;">' + _cycleName + '</span>'; 
	      					}
	      				} else {
	      					_cycleName = othersLabel + '<br/>';
	      				}
	      				var _statusName = data.name;
	      				if(_statusName.indexOf(' ') == -1){ // Condition if no space then break all words
	      					_statusName = '<span style="word-break: break-all;">' + _statusName + '</span>'; 
      					}
	      				var percentExec = ((data.y1 - data.y0)/data.total * 100).toFixed(2);
	      				return '<span>' + _cycleName + _statusName + ' ' + (data.y1 - data.y0) + ' of ' + data.total + ' </span><br/>' + percentExec + '%';
	      			})
	      			.offset([-12, 0]);
		
		var margin 	= {top: 30, right: 40, bottom: 60, left: 40},
		    height 	= 450 - widthOffset,
		    minWidth = AJS.$('#chartdiv').width() || 500,
		    width 	= minWidth - margin.left - margin.right;
 
		var x0 = d3.scale.ordinal()
		    .rangeRoundBands([0, width]);
		 
		var x1 = d3.scale.ordinal();
		 
		var y = d3.scale.linear()
		    .range([height, 0]);
		 
		var xAxis = d3.svg.axis()
		    .scale(x0)
		    .orient("bottom")
		    .tickFormat(function (d, i) {
                var width = x0.rangeBand();
                var labelText = d;
                if(chartLength > 10) {
					if(labelText.length > 8) return labelText.substr(0, 7) + '...'; else return labelText;
				} else {
	                var el = AJS.$('<span style="display:none;">' + labelText + '</span>').appendTo('#chartdiv');
	                var labelWidth = AJS.$(el).width();
	                AJS.$(el).remove();
	                var slength = Math.floor(width / labelWidth * labelText.length) - 2;
	                if (labelWidth > width) return labelText.substr(0, slength) + '...'; else return labelText; // TODO: find an alternative to truncate using css
				}
            });
		 
		var yAxis = d3.svg.axis()
		    .scale(y)
		    .orient("left")
		    .tickSize(-width);
 
		var color = d3.scale.ordinal().range(colors);
 
		var svg = d3.select("#chartdiv").append("svg")
					.attr("width", width + margin.left + margin.right)
					.attr("height", height + margin.top + margin.bottom)
					.append("g")	
					.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
		// Call the tooltip on svg element.
		svg.call(tip);
		AJS.$('.zephyr-d3-tip').css('max-width', '200px');
  		color.domain(colors); // Color domain 
  		x0.domain(data.map(function(d) { return d.sprintName; })); // Sprint axis domain
 		x1.domain(d3.range(itemsPerGroup)).rangeRoundBands([0, x0.rangeBand()]); // Cycle axis domain
  		y.domain([0, d3.max(data, function(d) {
  			var maxColumn = d3.max(d.values, function(columns) {return columns.total;});
  			return maxColumn;
  		})]);  		
  		
  		if(chartLength > 10 && ZEPHYR.Execution.chartType == 'gadget') {
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
	  			.attr("transform", "translate(0," + height + ")")
	  			.call(xAxis);
	    }
  		
  		// Append title for the x axis labels.
  		svg.selectAll(".x.axis text")
  			.append('title')
  			.text(function(d, i) {
  				return d;
  			});
  		/**
  		 * Calculate the width of the tick line
  		 * So that there will be separator between the group
  		 * Taking the first tick and calculating the positon and setting it to tickX
  		 */
  		var tickX = 0;  		
  		var $tickEls = AJS.$(".x.axis .tick");
  		if($tickEls.length && $tickEls[0]) {
  			$tickEl = AJS.$($tickEls[0]);
  			var transform = d3.transform(AJS.$($tickEl).attr('transform')).translate;
  			tickX = transform[0];
  		}
  		// Position the tick to the right of the axis
  		if(tickX) {
	  		svg.selectAll(".x.axis line")
	  			.attr('x1', tickX)
	  			.attr('x2', tickX);
  		}
  		
  		svg.append("g")
  			.attr("class", "y axis")
  			.call(yAxis)
  			.append("text")
  			.attr("transform", "rotate(-90)")
  			.attr("y", 6)
  			.attr("dy", ".7em")
  			.style("text-anchor", "end");
 
  		var stackedBar = svg.selectAll(".stackedbar")
  			.data(data)
  			.enter().append("g")
  			.attr("class", "g")
  			.attr("transform", function(d) { 
  				return "translate(" + x0(d.sprintName) + ",0)"; 
  			});

			
  		stackedBar.selectAll("rect")
  			.data(function(d) {
  				var groupWidth = x1.rangeBand();
  				var columnPadding = 6;
				var columnCount = -1;
  				d.values.forEach(function(column) {
  					if(column.y0 == 0)
  						columnCount++;
  					var columnWidth = columnCount * groupWidth + columnPadding;
  					column.columnWidth = columnWidth;
  				});
  				return d.values; 
  			})
  			.enter()
  			.append("rect")
	  		.attr("width", ( x1.rangeBand()/1.1))
  			.attr("x", function(d, i) {
  				return d.columnWidth;
  			})
  			.attr("y", function(d) { 
  				return y(d.y1); 
  			})
  			.attr("height", function(d) { 
  				return y(d.y0) - y(d.y1); 
  			})
  			.style("fill", function(d) { 
  				return (d.color); 
  			})
  			.on('mouseover', tip.show)
  			.on('mouseout', tip.hide);
  		
  		if(width < 400)
  			width = 400 - margin.left - margin.right;
  		// Empty the legend element
  		d3.select('#chartLegend').html('');
  		var legendStyleJSON = {
			'position': 'absolute',
			'left': 	'50%',
			'margin-left': -(width/2) + 'px'
		};
  		var legend = d3.select('#chartLegend')
				    .append('svg')
					.attr('width', width)
				    .style(legendStyleJSON)
				    .append("g")
				    .attr('class', 'legend')
				    .attr('width', width);
  		
  		// legend variables
  		var lx = 0, lWidth = 0, ly = 0, tWidth = 0;
  		legend.selectAll('g')
	  		.data(colors)
	  		.enter()
	  		.append('g')
	  		.each(function(d, i) {		  			
				var g = d3.select(this);
				
				if(i != 0) lx += lWidth + 10;
				if(lx > width - 50) ly = ly + 20, lx = 0;		
				g.append("rect")
				 .attr("x",  lx)
				 .attr("y", ly)
				 .attr("width", 10)
				 .attr("height", 10)
				 .style("fill", d.color);
				
				// Calculate the width of the legend label
				var el = AJS.$('<span style="display:none;">' + d.label + '</span>').appendTo('body');
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
        if(ZEPHYR.Execution.chartType == 'report') {
        	AJS.$('#chartLegend').height(100);
        }
        if(ZEPHYR.Execution.chartType == 'gadget') {
        	ly = ly + 35; // Minimum height for one row is 35
			AJS.$('#chartLegend').css('height', (ly) +'px');   
			var _chartHeightWithLegend = AJS.$('#chartdiv').height();
			var _totalChartHeight = (_chartHeightWithLegend + ly + widthOffset + margin.top + margin.bottom);
			AJS.$('.gg-wrapper').css('height', _totalChartHeight +'px');
			d3.select('#chartLegend > svg').style({
				'height': ly +'px'
			});
        }
		// Center the legends based on their width.
		if(tWidth < width) {
			d3.select('#chartLegend > svg').style({
		    	'margin-left': -(tWidth/2) + 'px'
		    });
		}
	}
	catch(exe){
		//First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
		//Since we are calling chart again during resize, do nothing in first call!
		console.log(exe);
	}	 
}