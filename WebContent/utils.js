function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function createGenresHyperlinkList(jsonArray) {
    let html = "";
    for (let i = 0; i < jsonArray.length; i++) {
        if (i > 0) {
            html += ', ';
        }
        html += '<a href="movie-list?genre=' + jsonArray[i]['name-encoded'] + '">' + jsonArray[i]['name'] + '</a>';
    }
    return html;
}

function createStarHyperlinkList(jsonArray) {
    let html = "";
    for (let i = 0; i < jsonArray.length; i++) {
        if (i > 0) {
            html += ', ';
        }
        html += '<a href="single-star.html?starId=' + jsonArray[i]['id'] + '">' + jsonArray[i]['name'] + '</a>';
    }
    return html;
}

function createMovieHyperlinkList(jsonArray) {
    let html = "";
    for (let i = 0; i < jsonArray.length; i++) {
        if (i > 0) {
            html += ', ';
        }
        html += '<a href="single-movie.html?movieId=' + jsonArray[i]['id'] + '">' + jsonArray[i]['title'] + '</a>';
    }
    return html;
}

