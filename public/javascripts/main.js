
$(document)
  .ready(function() {

    $('.popup')
      .popup();

    $('.ui.rating')
        .rating();

    $('#goToRepoButton')
        .click(function(){
          window.location.href = window.location.protocol + '/github/'+ $("#goToRepoInput")[0].value
        })

  });

