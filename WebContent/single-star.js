function handleResult(resultData) {
    let starContent = jQuery("#star-content")
    content = "<div class='detail-page-container'>"
    if (resultData["name"]) {
        content += "<h1>" + resultData["name"] + "</h1>" +
            "<div class='info'>Birth Year: " + resultData["birthYear"] + "</div>" +
            "<div class='info'>Movies: " +
            createMovieHyperlinkList(resultData["movies"]) +
            "</div>" +
            "<a class='button' href='movie-list?" + resultData["lastQuery"] + "'>Back to Movie List</a>"
    } else {
        content += "<p>No star found with ID " + starId + "</p>"
    }
    starContent.append(content)
}

let starId = getParameterByName('starId');

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-star?starId=" + starId,
    success: (resultData) => handleResult(resultData)
});
