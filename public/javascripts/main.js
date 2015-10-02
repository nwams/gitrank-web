$(document)
    .ready(function () {

        function goToRepoInputUrl() {
            window.location.href = window.location.protocol + "//" + window.location.hostname + ":"
                + window.location.port + '/github/' + $("#goToRepoInput")[0].value
        }

        $('.popup')
            .popup();

        $('.ui.rating')
            .rating();

        $('#goToRepoButton').click(goToRepoInputUrl);

        $('#goToRepoInput').keyup(function (e) {
            if (e.keyCode === 13) {
                goToRepoInputUrl()
            }
        });

        $('#docScore')
            .rating('setting', 'onRate', function (value) {
                $('#scoreDocumentation').val(value)
            });
        $('#matScore')
            .rating('setting', 'onRate', function (value) {
                $('#scoreMaturity').val(value)
            });
        $('#desScore')
            .rating('setting', 'onRate', function (value) {
                $('#scoreDesign').val(value)
            });
        $('#supScore')
            .rating('setting', 'onRate', function (value) {
                $('#scoreSupport').val(value)
            });
    });

