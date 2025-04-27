function handleResult(resultData) {
    cartSection = jQuery("#confirmation_section")
    let errorMessage = resultData["error_message"]
    if (errorMessage) {
        cartSection.append(
            "<h1>Order Processing Error</h1>" +
            "<p>We encountered an error recording your order. Please try again.</p>" +
            "<p><small>Error details: " + errorMessage + "</small></p>"
        )
    } else {
        let content = ""
        content += "<h1>Order Confirmed!</h1>" +
            "<p>Thank you for your order. Your payment has been processed successfully.</p>" +
            "<table border='1'><tr><th>Sale ID</th><th>Movie</th><th>Qty</th>"

        let transaction = resultData["transactions"]
        for (let i = 0; i < transaction.length; i ++) {
            let item = transaction[i]
            content +=
                "<tr><td>" + item["sales_id"] + "</td>" +
                "<td>" + item["movie_title"] + "</td>" +
                "<td>1</td></tr>"
        }

        content += "</table><p>Total amount: $" + resultData["total"] + "</p>"
        cartSection.append(content)
    }
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/confirm",
    success: (resultData) => handleResult(resultData)
});
