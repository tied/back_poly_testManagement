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
				//AJS.$('body').append("<span class='overlay-icon icon loading' style='vertical-align: middle;text-align:center'></span>");
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
	    	key: "testcases",
	    	ajaxOptions: function () {
	    		var gadget = this;
	    		var projectId = gadgets.util.unescapeString(gadget.getPref("projectId"));
	    		var versionId = gadgets.util.unescapeString(gadget.getPref("version"));
	    		var groupFld = gadgets.util.unescapeString(gadget.getPref("groupFld"));
	    		return {
	    			url: baseUrl + "/rest/"+JE.gadget.utils.getRestResourcePath()+"/latest/test/count?projectId="+ projectId +"&versionId=" + versionId+"&groupFld="+groupFld,
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
	var gadgetTitle = gadget.getPref('testcase_creation_name');
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
	var $ggWrapper = AJS.$('<div style="height: 600px; "/>').attr('class', 'gg-wrapper zephyr-chart');
	gadget.getView().append($ggWrapper);
	
	// title with project and version links
	var $headerHtml = JE.gadget.utils.addProjectVersionHeader(gadget, args.versionInfo, $ggWrapper);
	var header = '<h4 style="text-align:center;">' + gadget.getMsg('je.gadget.testcase.creation.subtitle', JE.gadget.fields.getPickerLabel(gadget, JE.gadget.fields.testcaseGroupFldPickerLov, groupFld)) + '</h4>';
	AJS.$(header).appendTo($ggWrapper);

	var issueTable;
	var cycleXML="";
	var plot;
	if(args.testcases.data != null && args.testcases.data.length > 0){
		var chartData = [];
        
        /*Iterate over all the column data, one at a time*/
 		for (var i in args.testcases.data){
			/*Now, lets dynamically create the chart series */
			chartData.push({x: args.testcases.data[i].name,  y: args.testcases.data[i].cnt});
        }
		var creationSeries = {label:"Tests", data:chartData};
		//Let's make sure we have width at least 100 px. IE do not like -ve width. - Refer to Bug 4248 
		//var width = (gadget.getView()[0].parentNode.offsetWidth - 50) <= 50 ? 100 : (gadget.getView()[0].parentNode.offsetWidth - 50);
		issueTable = '<div id="chartdiv" align="center" style="height:500px; width:'+ (gadget.getView()[0].parentNode.offsetWidth - 50) +'px; margin:auto"/><div id="chartLegend" align="bottom" style="height:50px; width:100%; position:fixed; bottom:0px;"/>';
		
		AJS.$(issueTable).appendTo($ggWrapper);
		
		showChart(creationSeries);
		AJS.$(window).unbind("resize");
		AJS.$(window).bind("resize", function(){
			var newWidth = gadget.getView()[0].parentNode.offsetWidth - 50;
			var currentWidth = AJS.$("#chartdiv").width();
			/*No need to refresh when config is showing up*/
			if(AJS.$("#config").css("display") == "none"){
				AJS.$("#chartdiv").css("width", newWidth);
				showChart(creationSeries);
			}
		});
	} else {
		AJS.$(window).unbind("resize");
        $ggWrapper.append('<div id="noDataMsg" style="padding: 20px; margin-top: 20px; margin-bottom: 50px;"></div>');
        AJS.messages.info("#noDataMsg", {body: gadget.getMsg('je.gadget.nodata.label'), closeable: false});
		return;
    }
};

function showChart(chartData){
	/*var chart = new FusionCharts("../../../download/resources/com.thed.zephyr.je:zephyr-gadget-fusion-chart-resources/fusioncharts/FCF_StackedColumn3D.swf", "ChartId", "600", "500");
	chart.setDataXML(xmlData);	
	chart.setTransparent(true);
	chart.render("chartdiv");
	*/
	
	if(chartData.length == 0) return false;
	AJS.$("#chartdiv").empty();
	try{
		var data = chartData.data;
		var legendJSON = [];
		legendJSON.push({"color": "#336699", "label": chartData.label});
		
		// Remove the tooltip element if already exist
		AJS.$('.zephyr-d3-tip').remove();
		var tip = zephyrd3.tip()
			      	.attr('class', 'zephyr-d3-tip')
			      	.html(function(data) { return '<span>Tests ' + data.y + ' of ' + data.y + '</span><br/>100.00%'})
			      	.offset([-12, 0]);
		
		var margin 	= {top: 20, right: 20, bottom: 60, left: 40},
			minWidth = AJS.$('#chartdiv').width() || 500,
		    width 	= minWidth - margin.left - margin.right;
		    height 	= 450,
	        x  		= d3.scale.ordinal().rangeBands([0, width - margin.left - margin.right], 0.1),
	        y  		= d3.scale.linear().range([height, 0]),
	        yAxis 	= d3.svg.axis().scale(y).orient('left').tickSize(-width),
	        xAxis 	= d3.svg.axis().scale(x).orient('bottom').tickFormat(function(d, i){
							var width = x.rangeBand();
							var txt = svg.append("text").text(d);
							var labelWidth = txt.node().getBBox().width;
							txt.remove();
							var slength = Math.floor(width / labelWidth * d.length) - 2;
							if(labelWidth > width) return d.substr(0, slength) + '...'; else return d; // TODO: find an alternative to truncate using css
					});

	    var svg = d3.select('#chartdiv')
	      		.append('svg')
	      		.attr('width', width)
	      		.attr('height', height + margin.top + margin.bottom)
	      		.append('g')
	      		.attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
	    
	    var max = d3.max(data, function(d) { return d.y; });
	    var xDomain = data.map(function(d) { return d.x; });
	    x.domain(xDomain);
	    y.domain([0, max+1]);
	    
	    // Attach the tooltip
	    svg.call(tip);
	    	    
	    // x axis
	    svg.append("g")
	    	.attr("class", "x axis")
	    	.attr('transform', 'translate(0,' + height + ')')
	    	.call(xAxis)
	    	.selectAll('.x.axis g');
	    
		// Append title for the x axis labels.
		svg.selectAll(".x.axis text")
		 	.append('title')
		 	.text(function(d) {/*var d_text = AJS.$('<div></div>').html(d).text();*/ return d;});
		//Fix for ZFJ-3255

	    // y axis
	    svg.append("g")
	    	.attr("class", "y axis")
	    	.call(yAxis);
	    
	    // Bars
	    var bars = svg.selectAll('g.bar')
	      			  .data(data)
	      			  .enter().append('g')
	      			  .attr('class', 'bar')
	      			  .attr('transform', function (d) { return "translate(" + x(d.x) + ", 0)" })

	    bars.append('rect')
	      	.attr('width', function() { return x.rangeBand() })
	      	.attr('height', function(d) { return height - y(d.y) })
	      	.attr('y', function(d) { return y(d.y) })
	      	.on('mouseover', tip.show)
	      	.on('mouseout', tip.hide);	
	    
	    // Empty the legend element
	    d3.select('#chartLegend').html('');
	    
		// legend   
	    var legend = d3.select('#chartLegend')
			    .append('svg')
			    .attr('width', width)
			    .append("g")
			    .attr('width', width)
				.attr("x", width - 65)
				.attr("y", 25);
		
		legend.selectAll('g')
			.data(legendJSON)
			.enter()
			.append('g')
      		.each(function(d, i) {
		        var g = d3.select(this);
		        g.append("rect")
		          .attr("x", width/2)
		          .attr("y", i*25)
		          .attr("width", 10)
		          .attr("height", 10)
		          .style("fill", d.color);
		        
		        g.append("text")
		          .attr("x", (width/2) + 15)
		          .attr("y", i * 25 + 9)
		          .attr("height", 35)
		          .attr("width", 100)
		          .style("fill", "#545454")
		          .text(d.label);
	
		      });
	}
	catch(exe){
		//First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
		//Since we are calling chart again during resize, do nothing in first call!
		console.log(exe);
	}
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
	var curTitle = gadget.getPref('testcase_creation_name')
	if(curTitle == "Test Distribution")
		curTitle = gadget.getMsg('je.gadget.testcase.creation.title');
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
            JE.gadget.fields.title(gadget, "testcase_creation_name", 'je.gadget.common.title.label', curTitle),
			JE.gadget.fields.projectPicker(gadget, "projectId", args.projectOptions.options),
			JE.gadget.fields.versionPicker(gadget, "projectId", "version", false, false),
			JE.gadget.fields.testcaseGroupFldPicker(gadget, "groupFld", false),
			//GH.gadget.fields.checkbox(gadget, "showMasterOrChild", 'gh.gadget.common.options.parentdetails', gadget.getPref('showMasterOrChild') == 'true'),
			JE.gadget.fields.checkbox(gadget, "showProjectName", gadget.getMsg('je.gadget.common.projectPref.label'), gadget.getPref('showProjectName') == 'true'),
	        JE.gadget.fields.checkbox(gadget, "showVersionName", gadget.getMsg('je.gadget.common.versionPref.label'), gadget.getPref('showVersionName') == 'true'),
           AJS.gadget.fields.nowConfigured()
       ]
   };
};