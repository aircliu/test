package fablix;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        /* Simply redirect to MovieListServlet with the same query string */
        resp.sendRedirect("movie-list" + (req.getQueryString() == null ?
                "" : "?" + req.getQueryString()));
    }
}