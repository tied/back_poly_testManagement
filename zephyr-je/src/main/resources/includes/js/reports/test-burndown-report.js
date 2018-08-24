/**
 * @namespace ZEPHYR.REPORT.BURNDOWN
 */
ZEPHYR.REPORT.BURNDOWN = function () {
}
ZEPHYR.REPORT.BURNDOWN.prototype = AJS.$.extend(ZEPHYR.REPORT.BURNDOWN.prototype, ZEPHYR.REPORT.prototype);
ZEPHYR.REPORT.BURNDOWN.prototype = AJS.$.extend(ZEPHYR.REPORT.BURNDOWN.prototype, {
    getBurndownExecutions: function (projectId, versionId, sprintId, cycle) {
        var thiz = this;
		var dataObj = {"projectId":projectId, "groupFld":"timePeriod", "periodName":"daily", "versionId":versionId, "graphType":"burndown"}
        if(sprintId && sprintId != -1 && !isNaN(sprintId)) {
        	dataObj.sprintId = sprintId;
        }
        if(cycle && cycle != 0) {
        	dataObj.cycleId = cycle;
        }
        var ajaxOptions =  {
            url: getRestURL() + "/execution/count",
			data:dataObj,
            type: 'GET',
            contentType:"application/json",
            success: function (data) {
                if (data) {
                    if(Object.keys(data).length === 0) {
                        AJS.$('#' + reqParams.chartDivId).html('No data found for chart ' + reqParams.title).css('color', '#1F77B4');
                    } else {
                        thiz.drawChart(data);
                    }
                }
            },
            error: function (jqXhr, status, errorThrown) {
                AJS.log("Error in fetching burndown chart data: " + errorThrown);
                AJS.$("#zephyr-test-report").append(errorThrown);
            }
        };
        AJS.$.ajax(ajaxOptions);
    },
    paint: function () {
    	this.getBurndownExecutions(this.projectId, this.versionId, this.sprintId, this.cycle);
    },
    drawChart: function (schedules) {
        var chartDiv,
        	plot,
        	size = Object.keys(schedules.data.UnexecutedGraphDataMap).length;
        if (schedules && schedules.data && size > 0) {
            var doSortByDate = function(responseData) {
                    var series1Array = new Array();
                    AJS.$.each(responseData, function(key,value){
                        series1Array.push([key,value]);
                    });
                    // Sort on Timestamp!
                    series1Array.sort( function(a,b){ return (a[0] - b[0]); } ); 
                    return series1Array;
                },
            	timeSortedDataArray1 = doSortByDate(schedules.data.UnexecutedGraphDataMap), 
            	timeSortedDataArray2 = doSortByDate(schedules.data.PredictionGraphDataMap), 
            	timeSeries = [
                              {data:timeSortedDataArray1, dashes:{show:false}, lines:{show:true}}, 
                              {data:timeSortedDataArray2, dashes:{show:true}, lines:{show:false}}
                             ],
				data = [];

            for(var i=0; i<timeSortedDataArray1.length; i++ ) {
                data.push({date: timeSortedDataArray1[i][0]});
            }
            for( var i=0; i<timeSortedDataArray2.length; i++ ) {
                data.push({date: timeSortedDataArray2[i][0]});
            }   

            chartDiv = '<div id="chartdiv" align="center" style="height:100%; width:100%; min-width:400px; margin:auto"/><div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';

            AJS.$(chartDiv).appendTo(AJS.$('#zephyr-test-report'));
            plot = this.showChart(schedules, data, timeSortedDataArray1, timeSortedDataArray2);
        }
    },
    showChart: function (schedules, data, timeSortedDataArray1, timeSortedDataArray2) {
        if (data.length == 0) return false;
        AJS.$("#chartdiv").empty();

        try {
            // Remove the tooltip element if already exist
            AJS.$('.zephyr-d3-tip').remove();
            
            var avgExecRate = AJS.I18n.getText("je.gadget.unexecuted.schedules.tooltip.avgrate");
			var unExecRemaining = AJS.I18n.getText("je.gadget.unexecuted.schedules.tooltip.remaining");
			var eta = AJS.I18n.getText("je.gadget.unexecuted.schedules.tooltip.expected.date");
			var tipDirection = 'n';
			var tipOffset = [-12, 0];
			var tip = zephyrd3.tip()
		      			.attr('class', 'zephyr-d3-tip')
		      			.html(function(d) {
		      				return '<span>'+avgExecRate+' <b>' + parseFloat(schedules.executionRate).toFixed(2) +
		      						'</b><br/>'+unExecRemaining+' ' + schedules.executionsRemaining +
		      						'<br/>'+eta+' ' + schedules.completionDate + '</span><br/>';
		      			})
		      			.offset(function(d) {
		      				return tipOffset;
		      			})
		      			.direction(function(d) {
		      				return tipDirection;
						});
			
			var formatDate = d3.time.format("%b - %d");
			// Set the dimensions of the graph
			var margin 	= {top: 30, right: 20, bottom: 30, left: 50},
				minWidth = AJS.$('#chartdiv').width() || 500,
			    width 	= minWidth - margin.left - margin.right;
				height	= 500 - margin.top - margin.bottom,
				x 		= d3.time.scale().range([0, width]),
				y 		= d3.scale.linear().range([height, 0]),
				xAxis 	= d3.svg.axis().scale(x).orient("bottom").ticks(4).tickFormat(formatDate),
				yAxis 	= d3.svg.axis().scale(y).orient("left").tickSize(-width),
				max 	= d3.max([d3.max(timeSortedDataArray1, function(d) { return d[1];} ), 
				    	          d3.max(timeSortedDataArray2, function(d) { return d[1];} )]);
			
			var area = d3.svg.area()
						.interpolate("monotone")
						.x(function(d) { return x(d[0]); })
						.y0(height)
						.y1(function(d) { return y(d[1]); });
			
			var line = d3.svg.line()
						 .x(function(d) { return x(d[0]); })
						 .y(function(d) { return y(d[1]); });
						
			var svg = d3.select("#chartdiv")
						.append("svg")
						.attr("width", width + margin.left + margin.right)
						.attr("height", height + margin.top + margin.bottom);		
			
			x.domain(d3.extent(data.map(function(d) { return (d.date); })));
			y.domain([0, max + 1]);	
			
			svg.call(tip);
			
			var groupEl = svg.append("g")
							.attr("transform", "translate(" + margin.left + "," + margin.top + ")");	
			
			groupEl.append("g")
					.attr("class", "x axis")
					.attr("transform", "translate(0," + height + ")")
					.call(xAxis);
			// Find the ticks and if less than 0 remove it.
			groupEl.selectAll(".tick").each(function(data) {
			  var tick = d3.select(this);
			  // pull the transform data out of the tick
			  var transform = d3.transform(tick.attr("transform")).translate;
			  if(transform[0] < 0) {
				  tick.remove();
			  }
			});
			groupEl.append("g")
					.attr("class", "y axis")
					.call(yAxis);
			
			groupEl.append("path")
			      .datum(timeSortedDataArray1)
			      .attr("class", "burndown-line")
			      .attr("d", line(timeSortedDataArray1))
			      .attr("d", area(timeSortedDataArray1))
			      .style("fill", function(d) { return '#E1EFEF'; })
			      
		    groupEl.selectAll("dot")
				  .data(timeSortedDataArray1)
				  .enter()
				  .append("circle")
				  .attr("r", 1.5)
				  .attr("cx", function(d) { return x(d[0]); })
				  .attr("cy", function(d) { return y(d[1]); })
				  .style("stroke", "#B3D7D8")
				  .style("stroke-width", "3px")
				  .on('mouseover', function(d) {
					  var mousePosition = d3.mouse(this);
					  setDirectionParameters(mousePosition);
					  tip.show(d, this);
				  })
				  .on('mouseout', tip.hide);				
	      		
			groupEl.append("path")
			      	.datum(timeSortedDataArray1)
			      	.attr("class", "line")
			      	.attr("d", line(timeSortedDataArray2))
			  		.style("stroke-dasharray", (5,5));	
			
			/**
			 * Set the offset and direction parameters for the tooltip
			 */
			var setDirectionParameters = function(mousePosition) {
				var tipWidth = AJS.$('#chartdiv').width() - 80; // Width - margin
				var _marginLeft = 50;
				var _tipHalfWidth = 145; 
				tipDirection = 'n';
				tipOffset = [-12, 0];
				
				if(mousePosition[0] < (_tipHalfWidth - _marginLeft)) { 
					tipDirection = 'e';
					tipOffset = [0, 12];
				} else if(mousePosition[0] > (tipWidth - _tipHalfWidth)) {
					tipDirection = 'w';
					tipOffset = [0, -12];
				}
			}
        }
        catch (exe) {
            //First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
            //Since we are calling chart again during resize, do nothing in first call!
            console.log(exe);
        }
    }
});

ZEPHYR.REPORT.validateVersionSelection = function(verionId) {
	if(verionId == -2 || verionId == -3) { // 'Unrealeased' or 'Released'
		AJS.$('#versionId_summary').addClass('error')
			.css('display', 'block')
			.html(AJS.I18n.getText("zephyr-je.pdb.traceability.validate.select.version.message"));
		AJS.$('#next_submit').attr('disabled', 'disabled');
	} else {
		AJS.$('#versionId_summary').removeClass('error')
			.hide()
			.html('');
		AJS.$('#next_submit').removeAttr('disabled');
	}
}

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
	var reportKey = ZephyrURL('?reportKey');
	if (reportKey && reportKey.indexOf(ZEPHYR.REPORT.KEYS.BURN_DOWN) > -1) {
		var _selectedVersion = AJS.$('#versionId_select').val();
		if(_selectedVersion == -2 || _selectedVersion == -3) // If 'Unrealeased' or 'Released' version is selected then, select 'Unscheduled'.
			AJS.$('#versionId_select').val(-1);
		var isProjectTypeSoftware = ZEPHYR.REPORT.prototype.initSprintOrCycleBasedOnProjectType();
		if(isProjectTypeSoftware) {
			AJS.$('#versionId_select').change(function () {
				ZEPHYR.REPORT.validateVersionSelection(this.value);
				ZEPHYR.REPORT.prototype.prepareCycleBySprintSelectOption();
			});
			AJS.$('#sprintId_select').change(function () {
				ZEPHYR.REPORT.prototype.updateSprintSelectOptions(this.value, AJS.$('#cycle_select'));
			});	
		} else {
			AJS.$('#versionId_select').change(function () {
				ZEPHYR.REPORT.validateVersionSelection(this.value);
				ZEPHYR.REPORT.prototype.prepareCycleSelectOption();
			});
		}
	}
});