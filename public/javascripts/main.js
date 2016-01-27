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

        $('#goToRepoButton').click(goToRepoInputUrl);

        $('#goToRepoInput').keyup(function (e) {
            if (e.keyCode === 13) {
                goToRepoInputUrl()
            }
        });


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



