package fablix;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.*;

/** Handles the “Add to Cart” button (POST). */
@WebServlet("/add-to-cart")
public class AddToCartServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String movieId = req.getParameter("movieId");
        if (movieId == null) {
            resp.sendError(400, "missing movieId");
            return;
        }

        HttpSession session = req.getSession();

        /* --- cart = Map<movieId, qty> ---------------- */
        @SuppressWarnings("unchecked")
        Map<String, Integer> cart =
                (Map<String, Integer>) session.getAttribute("cart");
        if (cart == null) cart = new HashMap<>();

        cart.put(movieId, cart.getOrDefault(movieId, 0) + 1);
        session.setAttribute("cart", cart);

        /* --- prices = Map<movieId, price> ------------ */
        @SuppressWarnings("unchecked")
        Map<String, Float> prices =
                (Map<String, Float>) session.getAttribute("prices");
        if (prices == null) prices = new HashMap<>();

        prices.computeIfAbsent(
                movieId, id -> 5f + (id.hashCode() & 7));  // $5 – $12
        session.setAttribute("prices", prices);

        /* Return to the page the user came from */
        String referer = req.getHeader("referer");
        resp.sendRedirect(referer == null ? "movie-list" : referer);
    }
}
