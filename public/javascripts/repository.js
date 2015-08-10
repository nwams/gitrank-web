/**
 * Created by nicolas on 8/7/15.
 */

$(document)
    .ready(function() {


        var data = [];

        for (var i = 0; i < 100 ; i++){
            data.push({Documentation: Math.random() * 5, Maturity: Math.random() * 5, Design: Math.random() * 5, Support: Math.random() * 5})
        }

        d3.parcoords()("#scoreChart")
            .data(data)
            .render()
            .ticks(3)
            .createAxes();

        $('.popup')
            .popup()
        ;

    });