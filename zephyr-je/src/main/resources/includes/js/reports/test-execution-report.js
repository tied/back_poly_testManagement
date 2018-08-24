/**
 * @namespace ZEPHYR.REPORT.EXECUTION
 */
ZEPHYR.REPORT.EXECUTION = function () {
}
ZEPHYR.REPORT.EXECUTION.prototype = AJS.$.extend(ZEPHYR.REPORT.EXECUTION.prototype, ZEPHYR.REPORT.prototype);
ZEPHYR.REPORT.EXECUTION.prototype = AJS.$.extend(ZEPHYR.REPORT.EXECUTION.prototype, {
    getExecutions: function (projectId, versionId, grpFld, showOthers, itemsXAxis, itemsPerGroup, sortSprintBy) {
        var thiz = this;
        var ajaxOptions =  {
            url: getRestURL() + "/execution/count?projectId=" + projectId + "&versionId=" + versionId + "&groupFld=" + grpFld,
            type: 'GET',
            contentType:"application/json",
            success: function (data) {
                if (data) {
                    if(Object.keys(data).length === 0) {
                        AJS.$('#' + reqParams.chartDivId).html('No data found for chart ' + reqParams.title).css('color', '#1F77B4');
                    } else {
                        var schedules = {"schedules":data};
                        thiz.drawChart(data, grpFld, showOthers, itemsXAxis, itemsPerGroup, sortSprintBy);
                    }
                }
            },
            error: function (jqXhr, status, errorThrown) {
            	AJS.log("Error in fetching test execution chart data: " + errorThrown);
            	var message = errorThrown;
                AJS.$("#zephyr-test-report").append('<div class="zephyr-aui-message-bar" id="zephyr-aui-message-bar"></div>');
            	if(jqXhr.responseText) {
            		try {
	            		var messageJSON = JSON.parse(jqXhr.responseText);
	            		if(messageJSON && messageJSON.errorDescHtml) {
	            			message = messageJSON.errorDescHtml;
	            		}
            		} catch(exe) {
            			message = jqXhr.responseText;
            		}
            	}
            	showErrorMessage(message, null, true);
            }
        };
        AJS.$.ajax(ajaxOptions);
    },
    paint: function () {
        this.getExecutions(this.projectId, this.versionId, this.grpFld, this.showOthers, this.itemsXAxis, this.itemsPerGroup, this.sortSprintBy);
    },
    drawChart: function (schedules, grpFld, showOthers, itemsXAxis, itemsPerGroup, sortSprintBy) {
        var chartDiv;
        var plot;
        if (schedules && schedules.data && schedules.data.length > 0) {
            var colors = [];
            ZEPHYR.Execution.chartType = 'report';

            // Attach the chart div with legends
            chartDiv = '<div id="chartdiv" align="center" style="height:100%; width:100%; min-width:800px; margin:auto"/>' +
			'<div id="chartLegend" style="width:100%; margin: 0 auto; position: relative; overflow: hidden;"/>';
            AJS.$(chartDiv).appendTo(AJS.$('#zephyr-test-report'));

            var chartData = ZEPHYR.Execution.getParsedChartData({
            	groupType: grpFld,
            	showOthers: showOthers,
            	itemsXAxis: itemsXAxis,
            	itemsPerGroup: itemsPerGroup,
            	sortSprintBy: sortSprintBy,
            	data: schedules.data,
            	statusSeries: schedules.statusSeries,
            	othersLabel: AJS.I18n.getText('je.gadget.common.others.label'),
            	noSprintLabel: AJS.I18n.getText('je.gadget.common.no.sprint.label')
           });

           for (var statusId in schedules.statusSeries) {
                if (schedules.statusSeries[statusId]) {
                    colors.push({
                        label: schedules.statusSeries[statusId].name,
                        color: schedules.statusSeries[statusId].color
                    });
                }
            }

           	//ZFJ-2172 Added exclusion of 'UNEXECUTED' status for group 'USER'.
   			if (grpFld == 'user') {
   				function removeUnexecutedStatusForUserGroup(element) {
   					return element.label != 'UNEXECUTED';
   				}
   				colors = colors.filter(removeUnexecutedStatusForUserGroup);
   			}

	        if(grpFld != 'sprint-cycle') {
	        	plot = this.showChart(chartData, colors);
	        }
        }
    },
    htmlEncode: function(value){
        return AJS.$('<div/>').text(value).html();
    },
    showChart: function (chartData, colors) {
        var that = this;
        if (chartData.length == 0) return false;
        AJS.$("#chartdiv").empty();

        try {
            // Remove the tooltip element if already exist
            AJS.$('.zephyr-d3-tip').remove();
            // Define the tooltip element

            var tip = zephyrd3.tip()
                .attr('class', 'zephyr-d3-tip')
                .html(function (data) {
                    var percentExec = ((data.y1 - data.y0) / data.total * 100).toFixed(2);
                    var _statusName = htmlEncode(data.name);
      				if(_statusName.indexOf(' ') == -1){
      					_statusName = '<span style="word-break: break-all;">' + _statusName + '</span>';
  					}
                    return '<span>' + _statusName + ' ' + (data.y1 - data.y0) + ' of ' + data.total + ' </span><br/>' + percentExec + '%';
                })
                .offset([-12, 0]);

            var margin = {top: 20, right: 20, bottom: 60, left: 40},
            	minWidth = AJS.$('#chartdiv').width() || 800,
                width = minWidth - margin.left - margin.right,
                height = 450,
                x = d3.scale.ordinal().rangeRoundBands([0, width], 0.1),
                y = d3.scale.linear().range([height, 0]),
                color = d3.scale.ordinal().range(colors),
                xAxis = d3.svg.axis().scale(x).orient("bottom").tickFormat(function (d, i) {
                    var width = x.rangeBand();
                    var labelText = chartData[i].testCycleName;
                    var el = AJS.$('<span style="display:none;">' + that.htmlEncode(labelText) + '</span>').appendTo('#chartdiv');
                    var labelWidth = AJS.$(el).width();
                    AJS.$(el).remove();
                    var slength = Math.floor(width / labelWidth * labelText.length) - 2;
                    if (labelWidth > width) return labelText.substr(0, slength) + '...'; else return labelText; // TODO: find an alternative to truncate using css
                }),
                yAxis = d3.svg.axis().scale(y).orient('left').tickSize(-width);

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
            var max = d3.max(chartData, function (d) {
                return d.total;
            });
            x.domain(chartData.map(function (d) {
                return d.testCycle;
            }));
            y.domain([0, (max + 1)]);

            // x-axis
            svg.append("g")
                .attr("class", "x axis")
                .attr('transform', 'translate(0,' + height + ')')
                .call(xAxis)
                .selectAll('.x.axis g');

            // Append title for the x axis labels.
            svg.selectAll(".x.axis text")
                .append('title')
                .text(function (d, i) {
                    return chartData[i].testCycleName;
                });

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
                .attr("transform", function (d) {
                    return "translate(" + x(d.testCycle) + ",0)";
                });

            bar.selectAll("rect")
                .data(function (d) {
                    return d.values;
                })
                .enter().append("rect")
                .attr("width", x.rangeBand())
                .attr("y", function (d) {
                    return y(d.y1);
                })
                .attr("height", function (d) {
                    return y(d.y0) - y(d.y1);
                })
                .style("fill", function (d) {
                    return d.color;
                })
                .on('mouseover', tip.show)
                .on('mouseout', tip.hide);

            // Empty the legend element
            d3.select('#chartLegend').html('');
            var legendStyleJSON = {
        			'position': 'absolute',
        			'left': 	'50%',
        			'margin-left': -(width/2)
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
                .each(function (d, i) {
                    var g = d3.select(this);

                    if (i != 0) lx += lWidth + 10;
                    if (lx > (width - 50)) ly = ly + 20, lx = 0;
                    g.append("rect")
                        .attr("x", lx)
                        .attr("y", ly)
                        .attr("width", 10)
                        .attr("height", 10)
                        .style("fill", d.color);

                    // Calculate the width of the legend label
                    var el = AJS.$('<span style="display:none;">' + that.htmlEncode(d.label) + '</span>').appendTo('#chartLegend');
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
                        .text(d.label);
                });
            AJS.$('#chartLegend').height(100);
    		// Center the legends based on their width.
    		if(tWidth < width) {
    			d3.select('#chartLegend > svg').style({
    		    	'margin-left': -(tWidth/2)
    		    });
    		}
        }
        catch (exe) {
            //First time after gadget configuration update, height and width are coming as zero or so and hence exception is thrown.
            //Since we are calling chart again during resize, do nothing in first call!
            console.log(exe);
        }
    }
});
ZEPHYR.REPORT.attachItemsPerGroup = function(selectedItemsPerGroup, selectedSortSprintBy) {
	var _itemsLabel = AJS.I18n.getText('je.gadget.common.number.cycles.per.sprint.label');
	var _sortSprintByLabel = AJS.I18n.getText('je.gadget.common.sprint.sortby.label');
	var _sortByCountLabel = AJS.I18n.getText('je.gadget.common.sprint.sortby.count.label');
	var _sortByDateLabel = AJS.I18n.getText('je.gadget.common.sprint.sortby.date.label');
	var _sortByCountChecked = (selectedSortSprintBy == 'count')? 'checked="checked"': '';
	var _sortByDateChecked = (selectedSortSprintBy == 'date')? 'checked="checked"': '';
	var itemsPerGroupHTML = '<div class="field-group" id="itemsPerGroup-field-group"><fieldset class="group"><legend for="itemsPerGroup">' + _itemsLabel + '</legend><div id="input_itemsPerGroup" class="builder-container"><input class="text zephyr_allow_only_number zephyr_numbers_no_decimals" type="text" id="itemsPerGroup" name="itemsPerGroup" value="' + selectedItemsPerGroup + '"></div><div id="itemsPerGroup-error" class="error"></div></fieldset></div>';
	itemsPerGroupHTML += '<div class="field-group" id="sortSprintBy-field-group"><fieldset class="group"><legend for="sortSprintBy">' + _sortSprintByLabel +
		'</legend><div id="input_sortSprintBy" class="builder-container"><div id="radio_sortSprintBy" class="builder-container">' +
		'<input type="radio" name="sortSprintBy_radio" id="sortSprintBy_count"' + _sortByCountChecked + ' value="count">&nbsp;' + _sortByCountLabel +
		'&nbsp;&nbsp;<input name="sortSprintBy_radio" type="radio" id="sortSprintBy_date" ' + _sortByDateChecked + ' value="date">&nbsp;' + _sortByDateLabel + '<input type="hidden" id="sortSprintBy" name="sortSprintBy" value="' + selectedSortSprintBy+ '"></div></div<div id="itemsPerGroup-error" class="error"></div></fieldset></div>';
	return itemsPerGroupHTML;

}
ZEPHYR.REPORT.getInputHTMLBasedOnGroup = function(groupLabel, inputPref, selectedValue) {
	var html = '<div class="field-group" id="itemsXAxis-field-group"><fieldset class="group"><legend for="itemsXAxis">'+ groupLabel+'</legend><div id="input_itemsXAxis" class="builder-container"><input class="text zephyr_allow_only_number zephyr_numbers_no_decimals" type="text" id="'+inputPref+'" name="'+inputPref+'" value="'+selectedValue+'" /></div><div id="'+inputPref+'-error" class="error"></div></fieldset></div>';
	return html;
}

ZEPHYR.REPORT.getGroupLabel = function(groupFldUserPrefValue) {
	if(groupFldUserPrefValue == 'user') {
		return AJS.I18n.getText('je.gadget.common.number.users.label');
	} else if(groupFldUserPrefValue == 'cycle') {
		return AJS.I18n.getText('je.gadget.common.number.cycles.label');
	} else if(groupFldUserPrefValue == 'component'){
		return AJS.I18n.getText('je.gadget.common.number.components.label');
	} else if(groupFldUserPrefValue == 'sprint-cycle') {
		return AJS.I18n.getText('je.gadget.common.number.sprints.label');
	}
}
ZEPHYR.REPORT.attachTestExecutionFields = function(selectedGroup, reportJSON) {
	var selectedValue = (reportJSON && reportJSON.itemsXAxis) ? reportJSON.itemsXAxis :0;
	var $attachEl = AJS.$('input[name=showOthers]').closest('.field-group');
	var groupLabel = ZEPHYR.REPORT.getGroupLabel(selectedGroup);
	var fieldsHTML = ZEPHYR.REPORT.getInputHTMLBasedOnGroup(groupLabel, 'itemsXAxis', selectedValue);
	AJS.$('#itemsPerGroup-field-group, #sortSprintBy-field-group, #itemsXAxis-field-group').remove();
	if(selectedGroup == 'sprint-cycle') {
		var selectedItemsPerGroup = (reportJSON && reportJSON.itemsPerGroup) ? reportJSON.itemsPerGroup : 3;
		var selectedSortSprintBy = (reportJSON && reportJSON.sortSprintBy) ? reportJSON.sortSprintBy: 'count';
		fieldsHTML += ZEPHYR.REPORT.attachItemsPerGroup(selectedItemsPerGroup, selectedSortSprintBy);
	}
	$attachEl.after(fieldsHTML);
	AJS.$('input[name=itemsXAxis], input[name=itemsPerGroup]').blur(function(ev) {
		if(isNaN(this.value)) {
			this.value.replace(/[^0-9\.]/g,'');
		}
		if(!this.value || this.value < 0) {
			this.value = 0;
		}
	});
	AJS.$('input[name=sortSprintBy_radio]').change(function() {
		var $hiddenSortSprintBy = AJS.$('#sortSprintBy');
		$hiddenSortSprintBy.attr('value', this.value);
	});
}

ZEPHYR.REPORT.getReportParams = function(grpFld) {
	var reportJSON = {};
	reportJSON.itemsXAxis = ZephyrURL('?itemsXAxis') || 0;
	if(grpFld == 'sprint-cycle') {
		reportJSON.itemsPerGroup = ZephyrURL('?itemsPerGroup') || 3;
		reportJSON.sortSprintBy = ZephyrURL('?sortSprintBy') || 'count';
	}
	return reportJSON;
}

/**
 * get hasAccessToSoftware for project
 * Remove the 'hasAccessToSoftware' option from the project select
 */
ZEPHYR.REPORT.getHasAccessToSoftware = function() {
	var $projectHasAccessToSWOption = AJS.$('#projectId_select').find('option[value=hasAccessToSoftware]');
	var hasAccessToSoftware = false;

	if($projectHasAccessToSWOption) {
		hasAccessToSoftware = $projectHasAccessToSWOption.text().trim();
	}
	$projectHasAccessToSWOption.remove();
	return hasAccessToSoftware;
}

/**
 * If the user does not have access to software project then remove the group by 'sprint-cycle' option
 */
ZEPHYR.REPORT.removeSprintGroupByForNonSoftware = function(hasAccessToSoftware) {
	if(!hasAccessToSoftware || hasAccessToSoftware != 'true') {
		var $groupBySprintOption = AJS.$('#groupFld_select').find('option[value=sprint-cycle]');

		$groupBySprintOption.remove();
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
	var hasAccessToSoftware = ZEPHYR.REPORT.getHasAccessToSoftware();
	var reportKey = ZephyrURL('?reportKey');
	if (reportKey && reportKey.indexOf(ZEPHYR.REPORT.KEYS.EXECUTION) > -1) {
		ZEPHYR.REPORT.removeSprintGroupByForNonSoftware(hasAccessToSoftware);
        var selectedGroup = AJS.$('#groupFld_select').val();
		var reportJSON = ZEPHYR.REPORT.getReportParams(selectedGroup);
		ZEPHYR.REPORT.attachTestExecutionFields(selectedGroup, reportJSON);
		AJS.$('#groupFld_select').change(function(ev) {
			var selectedGroup = this.value;
			ZEPHYR.REPORT.attachTestExecutionFields(selectedGroup);
		});
	}
});