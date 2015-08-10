/**
 * Created by nicolas on 8/7/15.
 */

$(document)
    .ready(function() {
        var data = [
            {Documentation: 4, Maturity: 4, Design: 2, Support: 0},
            {Documentation: 2, Maturity: 4, Design: 1, Support: 0},
            {Documentation: 1, Maturity: 4, Design: 3, Support: 1},
            {Documentation: 4, Maturity: 4, Design: 3, Support: 2},
            {Documentation: 3, Maturity: 4, Design: 4, Support: 0}
        ];

        d3.parcoords()("#scoreChart")
            .data(data)
            .render()
            .ticks(3)
            .createAxes();

        $('.popup')
            .popup()
        ;

    });