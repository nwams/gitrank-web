$(document)
    .ready(function () {
        $(".form").validate({
            errorClass: "input-error",
            rules: {
                url: {
                    required: true,
                    url: true
                }
            }
        });
    });

