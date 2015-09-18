/**
 * Created by nicolas on 8/7/15.
 */

function getMean(data, attribute){
    return _(data)
        .pluck(attribute)
        .reduce(function(mean, score){
            return (mean + score) / 2
        }, data[0][attribute])
}

function buildParallelCoordinates(data){

    data = data.map(function(score){return {
        timestamp: score.timestamp,
        Documentation: score.docScore,
        Maturity: score.maturityScore,
        Design: score.designScore,
        Support: score.supportScore
    }});

    var margin = {top: 30, right: 10, bottom: 30, left: 10},
        container = $('#scoreChart'),
        width = container.parent().width() - margin.left - margin.right,
        height = 250 - margin.top - margin.bottom;

    var opacity = d3.scale.linear()
            .domain(d3.extent(data, function(p) { return +p.timestamp; }))
            .range([0.2,1]),
        x = d3.scale.ordinal().rangePoints([0, width], 1),
        y = {};

    var line = d3.svg.line(),
        axis = d3.svg.axis().orient("left"),
        background,
        foreground;

    // Returns the path for a given data point.
    function path(d) {
        return line(dimensions.map(function(p) { return [x(p), y[p](d[p])]; }));
    }

    // Handles a brush event, toggling the display of foreground lines.
    function brush() {
        var actives = dimensions.filter(function(p) { return !y[p].brush.empty(); }),
            extents = actives.map(function(p) { return y[p].brush.extent(); });
        foreground.style("display", function(d) {
            return actives.every(function(p, i) {
                return extents[i][0] <= d[p] && d[p] <= extents[i][1];
            }) ? null : "none";
        });
    }

    var svg = d3.select("#scoreChart").append("svg")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom)
        .append("g")
        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

    // Extract the list of dimensions and create a scale for each.
    x.domain(dimensions = d3.keys(data[0]).filter(function(d) {
        return d != "timestamp" && (y[d] = d3.scale.linear()
                .domain([0,5])
                .range([height, 0]));
    }));

    // Add grey background lines for context.
    background = svg.append("g")
        .attr("class", "background")
        .selectAll("path")
        .data(data)
        .enter().append("path")
        .attr("d", path)
        .style("opacity", function(d){
            return opacity(d.timestamp);
        });

    // Add blue foreground lines for focus.
    foreground = svg.append("g")
        .attr("class", "foreground")
        .selectAll("path")
        .data(data)
        .enter().append("path")
        .attr("d", path)
        .style("opacity", function(d){
            return opacity(d.timestamp);
        });

    // Add the mean to the graph
    svg.append("g")
        .attr("class", "mean")
        .selectAll("path")
        .data([{
            timestamp: data[data.length-1].timestamp + 10,
            Documentation: getMean(data, "Documentation"),
            Maturity: getMean(data,"Maturity"),
            Design: getMean(data,"Design"),
            Support: getMean(data,"Support")
        }])
        .enter().append("path")
        .attr("d", path);

    // Add a group element for each dimension.
    var g = svg.selectAll(".dimension")
        .data(dimensions)
        .enter().append("g")
        .attr("class", "dimension")
        .attr("transform", function(d) { return "translate(" + x(d) + ")"; });

    // Add an axis and title.
    g.append("g")
        .attr("class", "axis")
        .each(function(d) { d3.select(this).call(axis.scale(y[d])); })
        .append("text")
        .style("text-anchor", "middle")
        .attr("y", -9)
        .text(function(d) { return d; });

    // Add and store a brush for each axis.
    g.append("g")
        .attr("class", "brush")
        .each(function(d) { d3.select(this).call(y[d].brush = d3.svg.brush().y(y[d]).on("brush", brush)); })
        .selectAll("rect")
        .attr("x", -8)
        .attr("width", 16);

    // Legend for the mean line
    svg.append('rect')
        .attr("x", width / 2 - 60)
        .attr("y", height + 10)
        .attr("rx", 2)
        .attr("ry", 2)
        .attr("width", 15)
        .attr("height", 15)
        .attr("stroke", "#1B1C1D")
        .attr("stroke-width", 1)
        .attr("fill", "#DB2828");

    svg.append('text')
        .attr("x", width / 2 - 40)
        .attr("y", height + 21 )
        .text("Mean");

    // Legend for the casual lines
    svg.append('rect')
        .attr("x", width / 2)
        .attr("y", height + 10)
        .attr("rx", 2)
        .attr("ry", 2)
        .attr("width", 15)
        .attr("height", 15)
        .attr("stroke", "#1B1C1D")
        .attr("stroke-width", 1)
        .attr("fill", 'steelblue');

    svg.append('text')
        .attr("x", width / 2 + 20)
        .attr("y", height + 21)
        .text("Last feedback scores");
}

$(document)
    .ready(function() {
        $.ajax({
            url: '' + window.location.href + '/lastFeedback',
            success: buildParallelCoordinates
        });
    });