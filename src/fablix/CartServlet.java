package fablix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private static final String URL  = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";

    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)
            throws IOException {

        HttpSession s = req.getSession();
        @SuppressWarnings("unchecked")
        Map<String, Integer> cart =
                (Map<String, Integer>) s.getAttribute("cart");
        @SuppressWarnings("unchecked")
        Map<String, Float> prices =
                (Map<String, Float>) s.getAttribute("prices");
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        try {
            JsonObject outputJason = new JsonObject();
            JsonArray jsonArray = new JsonArray();
            if (cart != null && !cart.isEmpty()) {
                Map<String, String> movieTitleMap = getMovieTitleMap(new ArrayList<>(cart.keySet()));
                for (String mid : cart.keySet()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("mid", mid);
                    item.addProperty("title", movieTitleMap.get(mid));
                    item.addProperty("price", prices.get(mid));
                    item.addProperty("quantity", cart.get(mid));
                    jsonArray.add(item);
                }
            }
            outputJason.add("cart_items", jsonArray);
            out.write(outputJason.toString());
            resp.setStatus(200);
        } catch (Exception e) {
            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            req.getServletContext().log("Error:", e);
            resp.setStatus(500);
        }
    }

    private Map<String, String> getMovieTitleMap(List<String> ids) throws ClassNotFoundException, SQLException {
        Map<String, String> movieTitleMap = new HashMap<>();
        Class.forName("com.mysql.cj.jdbc.Driver");
        Connection con = DriverManager.getConnection(URL, USER, PASS);
        StringBuilder sb = new StringBuilder(
                String.join(", ", Collections.nCopies(ids.size(), "?")));
        PreparedStatement main = con.prepareStatement(
                String.format("SELECT m.id, m.title " +
                        "FROM movies m " +
                        "WHERE m.id in (%s)", sb));
        for (int i = 1; i <= ids.size(); i++) {
            main.setString(i, ids.get(i-1));
        }
        ResultSet rs = main.executeQuery();
        while (rs.next()) {
            movieTitleMap.put(rs.getString("id"), rs.getString("title"));
        }
        rs.close();
        con.close();
        return movieTitleMap;
    }
}
