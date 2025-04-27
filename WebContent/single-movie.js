function handleResult(resultData) {
    let movieContent = jQuery("#movie-content")
    let content = "<div class='detail-page-container'><h1>" + resultData["title"] + "</h1>" +
        "<div class='info'>Year: " + resultData["year"] + "</div>" +
        "<div class='info'>Director: " + resultData["director"] + "</div>" +
        "<div class='info'>Rating: " + resultData["rating"] + "</div>" +
        "<div class='info'>Genres: " +
        createGenresHyperlinkList(resultData["genres"]) +
        "</div>" +
        "<div class='info'>Stars: " +
        createStarHyperlinkList(resultData["stars"]) +
        "</div>"

    content += "<div class='button-group'>" +
        "<form action='add-to-cart' method='post' style='display:inline-block'>" +
        "  <input type='hidden' name='movieId' value='" + movieId + "'>" +
        "  <button type='submit' class='cart'>Add to Cart</button>" +
        "</form>&nbsp;" +
        "<a class='button' href='movie-list?"+ resultData["lastQuery"] +"'>Back to Movie List</a>" +
        "</div></div>"
    movieContent.append(content)
}

let movieId = getParameterByName('movieId');

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/single-movie?movieId=" + movieId,
    success: (resultData) => handleResult(resultData)
});
