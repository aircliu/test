function handleResult(resultData) {
    cartSection = jQuery("#cart_section")
    let cart = resultData["cart_items"]
    if (cart.length === 0) {
        cartSection.append(
            "<p>Cart is empty.</p>" +
            "<a href='movie-list'>Continue shopping</a>"
        )
    } else {
        let total = 0
        let content = ""
        content +=
            "<form method='post' action='update-cart'>" +
            "<table border='1'><tr><th>Movie</th><th>Qty</th><th>Price</th>" +
            "<th>Remove</th></tr>"

        for (let i = 0; i < cart.length; i ++) {
            let item = cart[i]
            let mid = item["mid"]
            let qty = item["quantity"]
            let p = item["price"]
            total += p * qty
            content +=
                "<tr><td>" + item["title"] + "</td>" +
                "<td><button name='dec' value='" + mid + "'>-</button> " + qty + " " +
                "<button name='inc' value='" + mid + "'>+</button></td>" +
                "<td>" + (p*qty).toFixed(2) + "</td>" +
                "<td><button name='del' value='" + mid + "'>X</button></td></tr>"
        }

        content +=
            "</table></form>" +
            "<h3>Total: " + total.toFixed(2) + "</h3>" +
            "<form action='payment.html' method='get'>" +
            "<button>Proceed to payment</button></form>"

        cartSection.append(content)
    }
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/cart",
    success: (resultData) => handleResult(resultData)
});
