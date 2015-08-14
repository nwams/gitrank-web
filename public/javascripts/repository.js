/**
 * Created by nicolas on 8/7/15.
 */

$(document)
    .ready(function() {


        var data = [];

        for (var i = 0; i < 100 ; i++){
            data.push({
                Timestamp: i*10 + 100000,
                Documentation: Math.random() * 5,
                Maturity: Math.random() * 5,
                Design: Math.random() * 5,
                Support: Math.random() * 5,
                mean: 0
            })
        }

        data.push({
            Timestamp: 2000000,
            Documentation: _(data).pluck("Documentation").reduce(function(mean, score){return (mean + score) / 2}, data[0].Documentation),
            Maturity: _(data).pluck("Maturity").reduce(function(mean, score){return (mean + score) / 2}, data[0].Maturity),
            Design: _(data).pluck("Design").reduce(function(mean, score){return (mean + score) / 2}, data[0].Design),
            Support: _(data).pluck("Support").reduce(function(mean, score){return (mean + score) / 2}, data[0].Support),
            mean: 1
        });

        var visu = d3.parcoords()("#scoreChart")
            .data(data)
            .hideAxis(["Timestamp", "mean"])
            .color(color)
            .alpha(0.4)
            .composite("darken")
            .margin({ top: 20, left: 0, bottom: 50, right: 0 })
            .render()
            .brushMode("1D-axes")  // enable brushing
            .interactive();  // command line mode

        var explore_count = 0;
        var exploring = {};
        var explore_start = false;

        visu.svg
            .selectAll(".dimension")
            .style("cursor", "pointer")
            .on("click", function(d) {
                exploring[d] = d in exploring ? false : true;
                event.preventDefault();
                if (exploring[d]) d3.timer(explore(d,explore_count));
            });

        var scoreChart = $('svg'),
            width = scoreChart.width(),
            height = scoreChart.height();

        // Legend for the mean
        visu.svg
            .append('rect')
            .attr("x", width / 2 - 60)
            .attr("y", height - 50)
            .attr("width", 15)
            .attr("height", 15)
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .attr("fill", "#DB2828");

        visu.svg
            .append('text')
            .attr("x", width / 2 - 40)
            .attr("y", height - 36)
            .text("Mean");

        // Legend for the casual lines
        visu.svg
            .append('rect')
            .attr("x", width / 2)
            .attr("y", height - 50)
            .attr("width", 15)
            .attr("height", 15)
            .attr("stroke", "black")
            .attr("stroke-width", 1)
            .attr("fill", "#2185D0");

        visu.svg
            .append('text')
            .attr("x", width / 2 + 20)
            .attr("y", height - 36)
            .text("User values");


        /**
         * Function used to select the min and max to be displayed on a given dimension
         *
         * @param dimension
         * @returns {Function}
         */
        function explore(dimension) {
            if (!explore_start) {
                explore_start = true;
                d3.timer(visu.brush);
            }
            var speed = (Math.round(Math.random()) ? 1 : -1) * (Math.random()+0.5);
            return function(t) {
                if (!exploring[dimension]) return true;
                var domain = visu.yscale[dimension].domain();
                var width = (domain[1] - domain[0])/4;

                var center = width*1.5*(1+Math.sin(speed*t/1200)) + domain[0];

                visu.yscale[dimension].brush.extent([
                    d3.max([center-width*0.01, domain[0]-width/400]),
                    d3.min([center+width*1.01, domain[1]+width/100])
                ])(visu.g()
                        .filter(function(d) {
                            return d == dimension;
                        })
                );
            };
        }

        /**
         * From a line in the data returns the color it should be displayed with.
         * Red for the mean
         * Blue for the others
         *
         * @param {Object} d Data entry
         * @returns {*}
         */
        function color(dataEntry){
            if (dataEntry.mean == 1){
                return "#DB2828"
            }
            return "#2185D0"
        }

        $('.ui.rating')
            .rating();

        $('.feedbackLink').click(function(){
            $('#feedbackModal').modal('show');
        });

    });