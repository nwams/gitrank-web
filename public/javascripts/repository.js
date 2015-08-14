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
                Support: Math.random() * 5
            })
        }

        var visu = d3.parcoords()("#scoreChart")
            .data(data)
            .hideAxis(["Timestamp"])
            .render()
            .ticks(3)
            .createAxes();

        var alphaScale = d3.time.scale()
            .domain(_.pluck(data, 'Timestamp'))
            .range([0.125, 0.250, 0.375, 0.5 , 0.625, 0.750, 0.875, 1]);

        $('.ui.rating')
            .rating();

        $('.feedbackLink').click(function(){
            $('#feedbackModal').modal('show');
        });

    });