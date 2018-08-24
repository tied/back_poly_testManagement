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
	    	key: "cycle",
	    	ajaxOptions: function () {
	    		var gadget = this;
	    		var cycleId = gadgets.util.unescapeString(gadget.getPref("cycleId"));
	    		if(!cycleId)
	    			return {};
	    		return {
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/cycle/"+cycleId,
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
	    		//var daysprevious = gadgets.util.unescapeString(gadget.getPref("daysPreviously"));
	    		var cycleId = gadgets.util.unescapeString(gadget.getPref("cycleId"));
	    		var sprintId = gadgets.util.unescapeString(gadget.getPref("sprintId"));
	    		
	    		var dataObj = {"projectId":projectId, "groupFld": "timePeriod", "periodName":"daily", "versionId":versionId, "graphType":"burndown"};
	            if(sprintId && sprintId != -1) {
	            	dataObj.sprintId = sprintId;
	            }
	            if(cycleId && cycleId != 0) {
	            	dataObj.cycleId = cycleId;
	            }

	    		return {
	    			//url: baseUrl + "/rest/zephyr/latest/schedule/unexecutedschedules?projectId="+ projectId +"&periodName=daily&daysprevious="+daysprevious+ "&versionId=" + versionId + "&cycleId="+cycleId,
	    			//url: baseUrl + "/rest/zephyr/latest/schedule/unexecutedschedules?projectId="+ projectId +"&periodName=daily" + "&versionId=" + versionId + "&cycleId="+cycleId,
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/execution/count",
	    			data:dataObj,
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
	var cycleName = (args.cycle && args.cycle.name) ? args.cycle.name: gadget.getMsg("je.gadget.common.cycles.all.label");
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
		
	// root element
	var $zWrapper = AJS.$('<div style="height: 600px; "/>').attr('class', 'gg-wrapper zephyr-chart');
	gadget.getView().append($zWrapper);
	
	// title with project and version links
	var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $zWrapper);
	var header = '<h4 id="zephyr-gadget-header-cycle" style="text-align:center;">' + cycleName + '</h4>';
	AJS.$(header).appendTo($zWrapper);

	// Attach the sprint name to the header if has access to software project
	if(args.versionInfo.hasAccessToSoftware) {
		var sprintId = gadgets.util.unescapeString(gadget.getPref("sprintId"));
		var headerHMTL = '';
		if(!AJS.$('#zephyr-gadget-header').length) {
			AJS.$('#zephyr-gadget-header-cycle').prepend('<h3 style="text-align:center;" id="zephyr-gadget-header"/>');
		} else {
			headerHMTL = AJS.$('#zephyr-gadget-header').html();
		}
		if(sprintId) {
			if(sprintId != -1) {
				AJS.$.ajax({
			        url: "/rest/agile/1.0/sprint/" + sprintId,
			        contentType: "application/json",
			        success: function (sprint) {
			        	AJS.$('#zephyr-gadget-header').html(headerHMTL + ' / ' + sprint.name);
			        }
			    });
			}
		}
	}
	
	var issueTable;
	var execStatusSeries = [];
	var plot;
	
	// Get the size of an object
	//Note: We can not determine length of associative array by just doing args.schedules.data.length. It will come as undefined. 
	//http://stackoverflow.com/questions/5223/length-of-javascript-associative-array
	var size = Object.size(args.schedules.data.UnexecutedGraphDataMap);

	//Let's make sure we have width at least 100 px. IE do not like -ve width. - Refer to Bug 4248 
	var width = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
	issueTable = '<div id="chartdiv" align="center" style="height:500px; width:' + width + '; minWidth:400px; margin:auto"/>'
			   + '<div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';
	AJS.$(issueTable).appendTo($zWrapper)
	
	if(args.schedules != null && args.schedules.data != null && size > 0){
		var timeSortedDataArray1 = doSortByDate(args.schedules.data.UnexecutedGraphDataMap);  
		var timeSortedDataArray2 = doSortByDate(args.schedules.data.PredictionGraphDataMap); 
		var timeSeries = [
		                  {data:timeSortedDataArray1, dashes:{show:false}, lines:{show:true}}, 
		                  {data:timeSortedDataArray2, dashes:{show:true}, lines:{show:false}}
						 ];		

		var data = [];
		for(var i=0; i<timeSortedDataArray1.length; i++ ) {
			data.push({date: timeSortedDataArray1[i][0]});
		}
		for( var i=0; i<timeSortedDataArray2.length; i++ ) {
			data.push({date: timeSortedDataArray2[i][0]});
		}	
		
		// Remove the tooltip element if already exist
		AJS.$('.zephyr-d3-tip').remove();
		// Define the tooltip element
		var avgExecRate = gadget.getMsg("je.gadget.unexecuted.schedules.tooltip.avgrate");
		var unExecRemaining = gadget.getMsg("je.gadget.unexecuted.schedules.tooltip.remaining");
		var eta = gadget.getMsg("je.gadget.unexecuted.schedules.tooltip.expected.date");
		var tipDirection = 'n';
		var tipOffset = [-12, 0];
		
		var tip = zephyrd3.tip()
	      			.attr('class', 'zephyr-d3-tip')
	      			.html(function(d) {
	      				return '<span>'+avgExecRate+' <b>' + parseFloat(args.schedules.executionRate).toFixed(2) +
	      						'</b><br/>'+unExecRemaining+' ' + args.schedules.executionsRemaining +
	      						'<br/>'+eta+' ' + args.schedules.completionDate + '</span><br/>';
	      			})
	      			.offset(function(d) {
	      				return tipOffset;
	      			})
	      			.direction(function(d) {
	      				return tipDirection;
					});
			
		var formatDate = d3.time.format("%b - %d");
		// Set the dimensions of the graph
		var margin 	= {top: 30, right: 30, bottom: 30, left: 50},
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
			var _tipHalfWidth = 125; 
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
};

Object.size = function(obj) {
    var size = 0, key;
    for (key in obj) {
        if (obj.hasOwnProperty(key)) size++;
    }
    return size;
};

function doSortByDate(responseData) {
    var series1Array = new Array();
    jQuery.each(responseData, function(key,value){
        series1Array.push([key,value]);
    });
    // Sort on Timestamp!
    series1Array.sort( function(a,b){ return (a[0] - b[0]); } ); 
    return series1Array;
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
	if(curTitle == "Unexecuted Schedules")
		curTitle = gadget.getMsg('je.gadget.unexecuted.schedules.title');
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
			JE.gadget.fields.sprintPicker(gadget, "projectId", "version", 'sprintId', 'cycleId'),
			JE.gadget.fields.cycleBySprintPicker(gadget, "projectId","version", "sprintId", "cycleId"),
			//JE.gadget.fields.daysPreviouslyGroupPicker(gadget, "daysPreviously"),
			JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
	        JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
           AJS.gadget.fields.nowConfigured()
       ]
   };
};