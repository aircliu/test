function handlePostResult(resultData) {
    if (resultData["success"]) {
        $(location).attr('href', 'main')
    } else {
        let msgSection = jQuery("#message-section")
        msgSection.empty()
        msgSection.append(
            "<p style='color:red'>" + resultData["message"] + "</p>"
        )
    }
}

jQuery("#login-form").submit(function (event) {
    event.preventDefault();
    let formData = $(this).serialize();
    jQuery.ajax({
        dataType: "json",
        data: formData,
        method: "POST",
        url: "api/login",
        success: (resultData) => handlePostResult(resultData)
    });
})
