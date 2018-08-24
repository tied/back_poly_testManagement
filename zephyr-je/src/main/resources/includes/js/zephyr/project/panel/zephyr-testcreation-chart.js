function doSortByDate(responseData) {
    var series1Array = new Array();
    jQuery.each(responseData, function(key,value){
        series1Array.push([key,value]);
    });
    // Sort on Timestamp!
    series1Array.sort( function(a,b){ return (a[0] - b[0]); } ); 
    return [series1Array];
}

function doSortExecutionsByDate(responseData) {
    var series1Array = new Array();
    var totalExecutions = 0;
    
    jQuery.each(responseData, function(key,value){
        series1Array.push([key, value["total"]]);
        totalExecutions += value["total"];
    });
    
    // Sort on Timestamp!
    series1Array.sort( function(a,b){ return (a[0] - b[0]); } ); 
    return { 
    		 "dataArray":[series1Array], 
    		 "totalExecutions":totalExecutions
    	   };
}

// Chart for Test Creation and Test Execution
function showChart(chartData, chartID, strokeColor, pathClassName, areaColor) {
	var max 		= d3.max(chartData, function(d) { return d.value;} );
	var left 		= 22;
	var maxLength 	= max; // As don't want convert max to string so using maxLength variable
	// For executions/tests count greater than 999 increase the left
	if(maxLength.toString().length > 3) left = 35;
	
	// Set the dimensions of the graph
	var margin = {top: 20, right: 20, bottom: 20, left: left},
    	width = 410 - margin.left - margin.right,
    	height = 300 - margin.top - margin.bottom;
	
	var x = d3.time.scale().range([0, width]).clamp(true),
		y = d3.scale.linear().range([height, 0]);
	var formatDate = d3.time.format("%b - %d");
	
	var xAxis = d3.svg.axis().scale(x).orient("bottom").ticks(7).tickFormat(function(d, i) { if(i != 0) return formatDate(d); }),
		yAxis = d3.svg.axis().scale(y).orient("left").tickSize(-width);
	

	x.domain(d3.extent(chartData.map(function(d) { return (d.date); })));
	y.domain([0, max + 1]);

	var line = d3.svg.line()
				 .x(function(d) { return x(d.date); })
				 .y(function(d) { return y(d.value); });

	var area = d3.svg.area()
				 .x(function(d) { return x(d.date); })
				 .y1(function(d) { return y(d.value); })
				 .y0(function(d) { return y(0); });

	var svg = d3.select("#" + chartID).append("svg")
				.attr("width", width + margin.left + margin.right)
				.attr("height", height + margin.top + margin.bottom)
				.style({
					'margin-top': "-15px",
					'margin-bottom': "-5px"
				});

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
			.attr("class", "area")
			.style("fill", areaColor)
	    	.datum(chartData)
	    	.attr("d", area);
	
	groupEl.append("path")
      	.datum(chartData)
      	.attr("class", pathClassName)
      	.attr("d", line(chartData));

	groupEl.selectAll("dot")
		.data(chartData)
		.enter().append("circle")
		.attr("r", 1.5)
		.attr("cx", function(d) { return x(d.date); })
		.attr("cy", function(d) { return y(d.value); })
		.style("stroke", strokeColor)
		.style("stroke-width", "3px");
}

refreshCharts = function(){
    var activeTab = AJS.$("li.active a#pdb_test_panel_section-panel, li.active a#com\\.thed\\.zephyr\\.je\\:pdb_test_panel_section-panel, li.active-tab a#aui-test-summary-tab, li.active-tab a#aui-test-cycles-tab");
    if(activeTab.length < 1){
        return;
    }
    AJS.$('#zfj-permission-message-bar').addClass('active');
    var pKey = AJS.$("#projKey").val();
    
    /*Let call a backend REST call to fetch testcase creation data*/
    AJS.$.ajax({
        url: contextPath + "/rest/zephyr/latest/zchart/testsCreated?projectKey="+ pKey +"&daysPrevious=30&periodName=daily",
        type : "get",
        contentType: "application/json",
        success : function(response) {
            var timeSortedDataArray = doSortByDate(response["TestsCreationMap"]),
                charDiv 			= AJS.$("#testcases-creation-chart-id"),
                chartDivId 			= "testcases-creation-chart-id",
                pathClassName 		= "summary-creation-line",
                strokeColor 		= "#BCC3C3",
                areaColor			= "#DBF7CE",
                chartData 			= [];

            for(var i = 0; i< timeSortedDataArray.length; i++) {
                for(var j = 0; j <  timeSortedDataArray[i].length; j++) {
                    chartData.push({date: timeSortedDataArray[i][j][0], value: timeSortedDataArray[i][j][1]});
                }
            }

            AJS.$(charDiv).removeClass('loading').show().empty(); /*Removes the wait icon*/
            showChart(chartData, chartDivId, strokeColor, pathClassName, areaColor);
            AJS.$("#chart-details-id").html(AJS.I18n.getText('project.testcase.summary.creation.chart.label', '<strong>', response["TestsCreationCount"], '</strong>', '<strong>'+response["TestsCreationPeriod"]+'</strong>'));

        } // end of success
    }); //end of Ajax call.

    var pid = AJS.$("#projId").val();
    AJS.$.ajax({
        //url: contextPath + "/rest/zephyr/latest/schedule/executionsByTerm?projectId="+pid + "&daysprevious=30&periodName=daily",
        url: contextPath + "/rest/zephyr/latest/execution/count?projectId="+pid + "&daysPrevious=30&groupFld=timePeriod&periodName=daily",
        type : "get",
        contentType: "application/json",
        global:false,
        success : function(response) {
            var result 			= doSortExecutionsByDate(response["data"]),
                dataArray 		= result.dataArray,
                chartData		= [],
                chartDivId 		= "execution-creation-chart-id",
                pathClassName 	= "summary-execution-line",
                strokeColor		= "#BCC3C3",
                areaColor		= "#DBF7CE",
                charDiv 		= AJS.$("#execution-creation-chart-id");

            for(var i = 0; i< dataArray.length; i++) {
                for(var j = 0; j <  dataArray[i].length; j++) {
                    chartData.push({date: dataArray[i][j][0], value: dataArray[i][j][1]});
                }
            }

            AJS.$(charDiv).empty(); /*Removes the wait icon*/
            showChart(chartData, chartDivId, strokeColor, pathClassName, areaColor);
            AJS.$("#execution-details-id").html(AJS.I18n.getText('project.testcase.summary.execution.chart.label', '<strong>', result["totalExecutions"], '</strong>', '<strong>30</strong>'));
            AJS.$('#zfj-permission-message-bar').removeClass('active');
        }
    });
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
	var _location = window.location.href,
		isProjectCentricViewEnabled = AJS.$('#isProjectCentricViewEnabled').val(),
		activeTab = AJS.$("input#zephyr-proj-tab").val();
	/**
	 * Instead of checking if summary tab present checking if other tabs are not present,
	 * Since in case the URL does not contain any tab selected then default should be test-summary tab
	 */
	if((isProjectCentricViewEnabled && (_location.indexOf('traceability-tab') == -1) && _location.indexOf('test-cycles-tab') == -1) || (!isProjectCentricViewEnabled && activeTab == 'summary-tab')) {
	    JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, panel, reason){
	        refreshCharts();
	    })
	    refreshCharts();
	}
 });


