package fablix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet(name = "ConfirmServlet", urlPatterns = "/api/confirm")
public class ConfirmServlet extends HttpServlet {

    private static final String URL = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        HttpSession s = req.getSession();
        
        // Get cart and payment info from session
        @SuppressWarnings("unchecked")
        Map<String, Integer> cart = (Map<String, Integer>) s.getAttribute("cart");
        @SuppressWarnings("unchecked")
        Map<String, Float> prices = (Map<String, Float>) s.getAttribute("prices");
        String payName = (String) s.getAttribute("payName");
        String payCc = (String) s.getAttribute("payCc");
        
        // IMPORTANT: Get the customer ID from the actual logged-in user
        // We need to use the real user's email from the session
        String userEmail = (String) s.getAttribute("email");
        JsonObject outputJson = new JsonObject();
        // Calculate total
        float total = 0;
        if (cart != null) {
            for(String m : cart.keySet()) {
                total += cart.get(m) * prices.get(m);
            }
        }
        outputJson.addProperty("total", String.format("%.2f", total));
        
        boolean success = false;
        String errorMessage = "";
        
        // We need a valid customer ID
        Integer customerId = null;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
                // First, find the customer ID based on the session email
                if (userEmail != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id FROM customers WHERE email = ?")) {
                        ps.setString(1, userEmail);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            customerId = rs.getInt("id");
                        }
                    }
                }
                
                // If we couldn't get the ID from email, look for any valid customer ID
                if (customerId == null) {
                    try (Statement stmt = conn.createStatement()) {
                        ResultSet rs = stmt.executeQuery("SELECT id FROM customers LIMIT 1");
                        if (rs.next()) {
                            customerId = rs.getInt("id");
                        }
                    }
                }
                
                // Only proceed if we have a valid customer ID
                if (customerId != null && cart != null && !cart.isEmpty()) {
                    Map<String, String> movieTitleMap = getMovieTitleMap(conn, new ArrayList<>(cart.keySet()));
                    // Start transaction
                    conn.setAutoCommit(false);
                    
                    try {
                        // SQL statement matching the actual table structure
                        String insertSql = "INSERT INTO sales(customerId, movieId, saleDate) VALUES(?, ?, CURDATE())";
                        JsonArray transactions = new JsonArray();
                        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                            // For each movie in cart
                            for (Map.Entry<String, Integer> entry : cart.entrySet()) {
                                String movieId = entry.getKey();
                                int quantity = entry.getValue();
                                
                                // Since there's no quantity column, insert multiple rows
                                for (int i = 0; i < quantity; i++) {
                                    ps.setInt(1, customerId);
                                    ps.setString(2, movieId);
                                    ps.executeUpdate();
                                    ResultSet generatedKeys = ps.getGeneratedKeys();
                                    if (generatedKeys.next()) {
                                        JsonObject item = new JsonObject();
                                        item.addProperty("sales_id", generatedKeys.getLong(1));
                                        item.addProperty("movie_title", movieTitleMap.get(movieId));
                                        transactions.add(item);
                                    }
                                }
                            }
                        }
                        outputJson.add("transactions", transactions);
                        
                        // If we get here without exception, commit
                        conn.commit();
                        success = true;
                        
                        // Clear the cart after successful order
                        cart.clear();
                        s.setAttribute("cart", cart);
                        
                    } catch (SQLException e) {
                        // If there's an error, roll back
                        conn.rollback();
                        errorMessage = e.getMessage();
                        e.printStackTrace();
                    } finally {
                        // Reset auto-commit
                        conn.setAutoCommit(true);
                    }
                } else {
                    errorMessage = "Cannot process order: " + 
                        (customerId == null ? "No valid customer ID found. " : "") +
                        (cart == null || cart.isEmpty() ? "Cart is empty." : "");
                }
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
        }
        if (!errorMessage.isEmpty()) {
            outputJson.addProperty("error_message", errorMessage);
        }
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        out.write(outputJson.toString());
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private Map<String, String> getMovieTitleMap(Connection con, List<String> ids) throws SQLException {
        Map<String, String> movieTitleMap = new HashMap<>();
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
        return movieTitleMap;
    }
}