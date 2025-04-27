package fablix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";
        String loginUser= "mytestuser";
        String loginPass= "My6$Password";

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String starId = request.getParameter("starId");
        if (starId == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        JsonObject outObject = new JsonObject();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(loginUrl, loginUser, loginPass);

            // fetch star info
            String starQ = "SELECT name, birthYear FROM stars WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(starQ);
            stmt.setString(1, starId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                String birthYear = rs.getString("birthYear");
                if (birthYear == null) birthYear = "N/A";
                outObject.addProperty("name", name);
                outObject.addProperty("birthYear", birthYear);
            } else {
                out.println("<p>No star found with ID " + starId + "</p>");
            }
            rs.close();
            stmt.close();

            // fetch the star's movies
            String movieQ =
                    "SELECT m.id, m.title " +
                            "FROM stars_in_movies sim JOIN movies m ON sim.movieId = m.id " +
                            "WHERE sim.starId = ? " +
                            "ORDER BY m.year DESC, m.title";
            PreparedStatement mStmt = conn.prepareStatement(movieQ);
            mStmt.setString(1, starId);
            ResultSet mRs = mStmt.executeQuery();
            JsonArray movies = new JsonArray();
            while (mRs.next()) {
                String mid    = mRs.getString("id");
                String mtitle = mRs.getString("title");
                JsonObject movieObject = new JsonObject();
                movieObject.addProperty("id", mid);
                movieObject.addProperty("title", mtitle);
                movies.add(movieObject);
            }
            outObject.add("movies", movies);
            mRs.close();
            mStmt.close();

            conn.close();

        } catch(Exception e) {
            out.println("<p>Exception: " + e.getMessage() + "</p>");
        }

        // Get lastQuery from session and use it in the back link
        HttpSession session = request.getSession();
        String lastQuery = (String) session.getAttribute("lastQuery");
        if (lastQuery == null) lastQuery = "";
        outObject.addProperty("lastQuery", lastQuery);
        out.write(outObject.toString());
        out.close();
    }
}