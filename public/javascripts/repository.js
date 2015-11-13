/**
 * Created by nicolas on 8/7/15.
 */

function getMean(data, attribute) {
    return _(data)
        .pluck(attribute)
        .reduce(function (mean, score) {
            return (mean + score) / 2
        }, data[0][attribute])
}

function updateVotes(data){
    $("#" + data.id).remove();
    createGuideElement(data)
}

function upvote(id) {
    $.ajax({
        url: '' + window.location.href + '/quickstart/' + id + '/upvote',
        method: "POST",
        success: updateVotes
    })
}

function downvote(id) {
    $.ajax({
        url: '' + window.location.href + '/quickstart/' + id + '/downvote',
        method: "POST",
        success: updateVotes
    })
}

function buildQuickstartGuides(data) {
    data.forEach(createGuideElement);
}

function appendAndSort(listDOM, div){
    var sort_by_name = function(a, b) {
        return  b.getAttribute("upvote") - a.getAttribute("upvote");
    }
    var list = [];
    for (var i in listDOM) {
        if (listDOM[i].nodeType == 1 && listDOM[i].getAttribute("upvote")!= null) { // get rid of the whitespace text nodes
            list.push(listDOM[i]);
        }
    }
    list.push(div)
    list.sort(sort_by_name);
    return list

}
function createGuideElement (guide){
    var div = document.createElement('div');
    div.className = 'comment';
    div.id = guide.id;

    var a = document.createElement('a');
    a.className = 'author';
    a.setAttribute('href', guide.url);
    a.textContent = guide.title;

    var description = document.createElement('div');
    description.className = 'text';
    description.textContent = guide.description;

    var thumbsup = document.createElement('span');
    thumbsup.innerText = guide.upvote;

    var thumbsdown = document.createElement('span');
    thumbsdown.innerText = guide.downvote;

    var icon = document.createElement('i');
    icon.className = 'ui thumbs up icon';
    if( $('.right.menu').find('.avatar').length >= 1) {
        icon.setAttribute("style", "cursor: pointer");

        icon.onclick= function(){ upvote( guide.id ); } ;
    }
    thumbsup.appendChild(icon);

    icon = document.createElement('i');
    icon.id = 'thumbsdown';
    icon.className = 'ui thumbs down icon';
    if( $('.right.menu').find('.avatar').length >= 1) {
        icon.onclick = function () {
            downvote(guide.id);
        };
        icon.setAttribute("style", "cursor: pointer");

    }
    thumbsdown.appendChild(icon);


    div.appendChild(a);
    div.appendChild(description);
    div.appendChild(thumbsup);
    div.appendChild(thumbsdown);
    div.setAttribute("upvote", guide.upvote);


    var listDOM = document.getElementById("guides").childNodes;
    var list = appendAndSort(listDOM, div)
    for (var i = 0; i < list.length; i++) {
        document.getElementById("guides").appendChild(list[i]);
    }

}

function buildParallelCoordinates(data) {

    if (_.isEmpty(data)){
        return;
    }

    data = data.map(function (score) {
        return {
            timestamp: score.timestamp,
            Documentation: score.docScore,
            Maturity: score.maturityScore,
            Design: score.designScore,
            Support: score.supportScore
        }
    });

    var margin = {top: 30, right: 10, bottom: 30, left: 10},
        container = $('#scoreChart'),
        width = container.parent().width() - margin.left - margin.right,
        height = 250 - margin.top - margin.bottom;

    var opacity = d3.scale.linear()
            .domain(d3.extent(data, function (p) {
                return +p.timestamp;
            }))
            .range([0.2, 1]),
        x = d3.scale.ordinal().rangePoints([0, width], 1),
        y = {};

    var line = d3.svg.line(),
        axis = d3.svg.axis().orient("left"),
        background,
        foreground,
        dimensions;

    // Returns the path for a given data point.
    function path(d) {
        return line(dimensions.map(function (p) {
            return [x(p), y[p](d[p])];
        }));
    }

    // Handles a brush event, toggling the display of foreground lines.
    function brush() {
        var actives = dimensions.filter(function (p) {
                return !y[p].brush.empty();
            }),
            extents = actives.map(function (p) {
                return y[p].brush.extent();
            });
        foreground.style("display", function (d) {
            return actives.every(function (p, i) {
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
    x.domain(dimensions = d3.keys(data[0]).filter(function (d) {
        return d !== "timestamp" && (y[d] = d3.scale.linear()
                .domain([0, 5])
                .range([height, 0]));
    }));

    // Add grey background lines for context.
    background = svg.append("g")
        .attr("class", "background")
        .selectAll("path")
        .data(data)
        .enter().append("path")
        .attr("d", path)
        .style("opacity", function (d) {
            return opacity(d.timestamp);
        });

    // Add blue foreground lines for focus.
    foreground = svg.append("g")
        .attr("class", "foreground")
        .selectAll("path")
        .data(data)
        .enter().append("path")
        .attr("d", path)
        .style("opacity", function (d) {
            return opacity(d.timestamp);
        });

    // Add the mean to the graph
    svg.append("g")
        .attr("class", "mean")
        .selectAll("path")
        .data([{
            timestamp: data[data.length - 1].timestamp + 10,
            Documentation: getMean(data, "Documentation"),
            Maturity: getMean(data, "Maturity"),
            Design: getMean(data, "Design"),
            Support: getMean(data, "Support")
        }])
        .enter().append("path")
        .attr("d", path);

    // Add a group element for each dimension.
    var g = svg.selectAll(".dimension")
        .data(dimensions)
        .enter().append("g")
        .attr("class", "dimension")
        .attr("transform", function (d) {
            return "translate(" + x(d) + ")";
        });

    // Add an axis and title.
    g.append("g")
        .attr("class", "axis")
        .each(function (d) {
            d3.select(this).call(axis.scale(y[d]));
        })
        .append("text")
        .style("text-anchor", "middle")
        .attr("y", -9)
        .text(function (d) {
            return d;
        });

    // Add and store a brush for each axis.
    g.append("g")
        .attr("class", "brush")
        .each(function (d) {
            d3.select(this).call(y[d].brush = d3.svg.brush().y(y[d]).on("brush", brush));
        })
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
        .attr("y", height + 21)
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
    .ready(function () {
        $.ajax({
            url: '' + window.location.href + '/lastFeedback',
            success: buildParallelCoordinates
        });

        $.ajax({
            url: '' + window.location.href + '/quickstart/guides',
            success: buildQuickstartGuides
        });

        $('#badgeButton').click(function(){
            $('#badgeModal').modal('show')
        });
    });