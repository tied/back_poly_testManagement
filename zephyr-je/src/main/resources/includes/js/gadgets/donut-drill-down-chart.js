function DonutDrillDownChart(options) {
	var $graph = options.graph || AJS.$('body');
	this.graph = d3.select($graph[0]);
	this.height = options.height || 500;
	this.width = options.width || 600;
    this.pieWrapperClass = options.pieWrapperClass || '';
	this.pieRadius = (Math.min(this.width, this.height) / 2) - 30;
    this.pieOuterRadius = this.pieRadius - 150;
    this.pieInnerRadius = this.pieRadius - 60;
    this.pieInnerRadiusOver = this.pieRadius - 55;
    this.pieTranslateX = this.width / 2;
    this.MAX_LEGENDS = options.MAX_LEGENDS;
    this.trimText = function() {
        var self = d3.select(this),
            textLength = self.node().getComputedTextLength(),
            text = self.text();
        while (textLength > (100 - 2 * 5) && text.length > 0) {
            text = text.slice(0, -1);
            self.text(text + '...');
            textLength = self.node().getComputedTextLength();
        }
    }
}
DonutDrillDownChart.prototype.render = function(data, drillDown, renderLegend, onCompletion) {

	var radius = this.pieRadius;

    var that = this;

    var c20c = d3.scale.category20c();

    function colors(n) {
        var colors_g = ["#9b59b6", "#3498db", "#95a5a6", "#e74c3c", "#34495e", "#2ecc71", "#1abc9c", "#16a085", "#27ae60", "#2980b9", "#8e44ad", "#2c3e50", "#f1c40f", "#e67e22", "#f39c12", "#d35400", "#c0392b", "#bdc3c7", "#7f8c8d"];
        return colors_g[n % colors_g.length];
    }

    function htmlDecode(value){
        return AJS.$('<div/>').html(value).text();
    }

    var arc = d3.svg.arc()
            .outerRadius(this.pieOuterRadius)
            .innerRadius(this.pieInnerRadius);

    var arcMouseOver = d3.svg.arc()
        .outerRadius(this.pieOuterRadius)
        .innerRadius(this.pieInnerRadiusOver);

    // var tip = zephyrd3.tip()
    //       .attr('class', 'zephyr-d3-tip')
    //       .offset([-10, 0])
    //       .html(function(d) {
    //         return '<div>'+ d.data.title +'</div>';
    //       });

    var pie = d3.layout.pie()
            .value(function(d) {
            	return d.total;
            });
    var svg = this.graph
        .append('div')
        .attr('class','donut-chart-wrapper '+ this.pieWrapperClass)
        .append('svg:svg')
        .attr('class', 'donut-chart')
        .attr('width', this.width)
        .attr('height', 400)
        .append('g')
        .attr('transform', 'translate(' + this.pieTranslateX + ',' + (this.height / 2 - 80) + ')');

    var g = svg.selectAll('.arc')
        .data(pie(data))
        .enter().append('g')
        .attr('class', function(d) {
            return 'chart-arc';
        })
        .attr('startAngle', function(d) {
            return d.startAngle;
        })
        .attr('endAngle', function(d) {
            return d.endAngle;
        });

    // if(data && data.length) {
    //     g.call(tip);
    // }
    var onPathClick = function(d) {
        d3.select(this)
            .transition()
            .duration(800);
        //tip.hide(d);
        if(drillDown && typeof drillDown === 'function') {
            drillDown(d.data.title, d);
        }
    };

    var hoverText = svg.append('text').attr('text-anchor', 'middle');
    
    g.append('path')
        .attr('id', function(d, i) {
            return 'wavy' + i;
        })
        .attr('class', function(d, i) {
            if(i === that.MAX_LEGENDS) {
                return 'otherLegend';
            }
            return '';
        })
        .attr('d', arc)
        .style('fill', function(d, i) {
            var boxColor = colors(i);
            if(d.data && d.data.color) {
                boxColor = d.data.color;
            }
            renderLegend(d, boxColor, i);
            return boxColor;
        })
        .on('mouseover', function(d) {
            d3.select(this)
                .transition()
                .duration(800)
                .attr('d', arcMouseOver);

            var title = gadgets.util.unescapeString(d.data.title);
            // if(jQuery(this).attr('class').indexOf('otherLegend') > -1) {
            //     title = 'Other';
            // }
            //TODO: when pie will show other section
            hoverText.text(title).style({
                'font-size': '14px'
            }).each(that.trimText);

            //tip.show(d);
        })
        .on('mouseout', function(d) {
            d3.select(this)
                .transition()
                .duration(800)
                .attr('d', arc);

            hoverText.text('');

            //tip.hide(d);
        })
        .on('click', _.debounce(onPathClick, 500));

    if(onCompletion && typeof onCompletion === 'function') {
        onCompletion();
    }
}