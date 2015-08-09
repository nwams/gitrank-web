/**
 * Created by nicolas on 8/7/15.
 */

var data = [
    {documentation: 4, maturity: 4, design: 2, support: 0},
    {documentation: 2, maturity: 4, design: 1, support: 0},
    {documentation: 1, maturity: 4, design: 3, support: 1},
    {documentation: 4, maturity: 4, design: 3, support: 2},
    {documentation: 3, maturity: 4, design: 4, support: 0}
];

var pc = d3.parcoords()("#scoreChart")
    .data(data)
    .render()
    .ticks(3)
    .createAxes();