$(document)
    .ready(function () {

        $('.ui.form')
            .form({
                fields: {
                    title: ['empty'],
                    url: ['url', 'empty'],
                    description: ['empty']
                }
            })
        ;
    });
