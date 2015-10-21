$(document)
    .ready(function () {

        $('.ui.form')
            .form({
                fields: {
                    url: {
                        identifier  : 'url',
                        rules: [
                            {
                                type   : 'url',
                                prompt : 'Please enter a url'
                            }
                        ]
                    },
                }
            })
        ;
});

