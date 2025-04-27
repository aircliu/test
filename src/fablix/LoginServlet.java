package fablix;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final String URL  = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        JsonObject outJson = new JsonObject();
        String email = req.getParameter("email");
        String pw    = req.getParameter("password");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection c = DriverManager.getConnection(URL, USER, PASS);
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT firstName, password FROM customers WHERE email=?")) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getString("password").equals(pw)) { // ✅ correct
                        HttpSession s = req.getSession(true);
                        s.setAttribute("user", rs.getString("firstName"));
                        outJson.addProperty("success", true);
                    }
                    else {  // ❌ wrong
                        outJson.addProperty("success", false);
                        outJson.addProperty("message", "Invalid password.");
                    }
                } else {                                 // ❌ wrong
                    outJson.addProperty("success", false);
                    outJson.addProperty("message", "Invalid email.");
                }
            }
            out.write(outJson.toString());
        } catch (Exception e) {
            outJson.addProperty("success", false);
            outJson.addProperty("message", "Server error: " + e.getMessage());
            out.write(outJson.toString());
            out.close();
        }
    }
}