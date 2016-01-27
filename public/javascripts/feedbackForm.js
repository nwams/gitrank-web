/**
 * Created by nicolas on 10/21/15.
 */
$(document)
    .ready(function () {

        var docScoreInput = $('#scoreDocumentation');
        $('#docScore').rating({
            initialRating: docScoreInput.val(),
            onRate: function(value){
                docScoreInput.val(value)
            }
        });

        var matScoreInput =  $('#scoreMaturity');
        $('#matScore').rating({
            initialRating: matScoreInput.val(),
            onRate: function(value){
                matScoreInput.val(value)
            }
        });

        var designScoreInput = $('#scoreDesign');
        $('#desScore').rating({
            initialRating: designScoreInput.val(),
            onRate: function(value){
                designScoreInput.val(value)
            }
        });

        var supScoreInput = $('#scoreSupport');
        $('#supScore').rating({
            initialRating: supScoreInput.val(),
            onRate: function(value){
                supScoreInput.val(value)
            }
        });

        $('.ui.form')
            .form({
                fields: {
                    scoreDocumentation: {
                        rules: [{
                            type: 'integer[0..5]',
                            prompt: 'Documentation should be rated from 1 to 5 stars'
                        }]
                    },
                    scoreMaturity: {
                        rules: [{
                            type: 'integer[0..5]',
                            prompt: 'Maturity should be rated from 1 to 5 stars'
                        }]
                    },
                    scoreDesign: {
                        rules: [{
                            type: 'integer[0..5]',
                            prompt: 'Design should be rated from 1 to 5 stars'
                        }]
                    },
                    scoreSupport: {
                        rules: [{
                            type: 'integer[0..5]',
                            prompt: 'Support should be rated from 1 to 5 stars'
                        }]
                    },
                    feedback: ['empty']
                }
            })
        ;

    });
