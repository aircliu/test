function handleResult(resultData) {
    let priceSection = jQuery("#price-section")
    priceSection.append(
        "<p>Total: " + resultData["total"].toFixed(2) + "</p>"
    )
}

function handlePostResult(resultData) {
    if (resultData["success"]) {
        $(location).attr('href', 'confirm.html')
    } else {
        let msgSection = jQuery("#message-section")
        msgSection.empty()
        msgSection.append(
            "<p style='color:red'>" + resultData["message"] + "</p>"
        )
    }
}

jQuery("#cc-form").submit(function (event) {
    event.preventDefault();
    let formData = $(this).serialize();
    jQuery.ajax({
        dataType: "json",
        data: formData,
        method: "POST",
        url: "api/payment",
        success: (resultData) => handlePostResult(resultData)
    });
})

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/payment",
    success: (resultData) => handleResult(resultData)
});
