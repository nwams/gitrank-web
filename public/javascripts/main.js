$(document)
    .ready(function () {

        function goToRepoInputUrl() {

            var repoName = $("#goToRepoInput")[0].value,
                baseUrl = window.location.protocol + "//" + window.location.hostname + ":"
                    + window.location.port;

            if (repoName === ''){
                window.location.href = baseUrl;
            } else {
                window.location.href = baseUrl + '/github/' + repoName;
            }
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
        $('#docScore').rating({
            initialRating: $('#scoreDocumentation').val()
        })
        $('#matScore').rating({
            initialRating: $('#scoreMaturity').val()
        })
        $('#desScore').rating({
            initialRating: $('#scoreDesign').val()
        })
        $('#supScore').rating({
            initialRating: $('#scoreSupport').val()
        })

        emojify.setConfig({img_dir : 'assets/lib/emojify.js/dist/images/basic'});
        emojify.run();

        $('.ui.search')
            .search({
                apiSettings: {
                    url: '/search/repo?queryString={query}'
                },
                fields: {
                    results: "results",
                    title:"title"
                },
                onSelect: function value(result){
                    $("#goToRepoInput").val(result.title);
                    goToRepoInputUrl();
                }
            });

    });



