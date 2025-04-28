package fablix;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "SingleMovieServlet",  urlPatterns = "/api/single-movie")
public class SingleMovieServlet extends HttpServlet {

    private static final String URL  = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String movieId = req.getParameter("movieId");
        if (movieId == null) {
            resp.getWriter().println("No movieId provided");
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outJson = new JsonObject();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {

                /* basic info + rating */
                PreparedStatement main = con.prepareStatement(
                    "SELECT m.title,m.year,m.director,r.rating " +
                    "FROM movies m LEFT JOIN ratings r ON m.id=r.movieId " +
                    "WHERE m.id=?");
                main.setString(1, movieId);
                ResultSet rs = main.executeQuery();
                if (!rs.next()) { out.println("Movie not found");
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }

                outJson.addProperty("title", esc(rs.getString("title")));
                outJson.addProperty("year", rs.getString("year"));
                outJson.addProperty("director", esc(rs.getString("director")));
                outJson.addProperty("rating", rs.getString("rating")==null?"N/A":rs.getString("rating"));
                rs.close(); main.close();

                JsonArray genres = new JsonArray();
                /* ---- ALL genres alphabetical ---- */
                try (PreparedStatement gq = con.prepareStatement(
                        "SELECT g.name FROM genres_in_movies gim " +
                        "JOIN genres g ON gim.genreId=g.id " +
                        "WHERE gim.movieId=? " +
                        "ORDER BY g.name")) {
                    gq.setString(1, movieId);
                    ResultSet gr = gq.executeQuery();
                    while (gr.next()) {
                        JsonObject genJson = new JsonObject();
                        genJson.addProperty("name", esc(gr.getString("name")));
                        genJson.addProperty("name-encoded",  java.net.URLEncoder.encode(gr.getString("name"),"UTF-8" ));
                        genres.add(genJson);
                    }
                }
                outJson.add("genres", genres);
                /* ---- ALL stars ordered by movie-count desc ---- */
                JsonArray stars = new JsonArray();
                try (PreparedStatement sq = con.prepareStatement(
                        "SELECT s.id,s.name,(SELECT count(DISTINCT movieId) FROM stars_in_movies subSm WHERE subSm.starId = s.id) as cnt " +
                        "FROM stars_in_movies sim " +
                        "JOIN stars s ON sim.starId=s.id " +
                        "WHERE sim.movieId=? " +
                        "ORDER BY cnt DESC, s.name ASC")) {
                    sq.setString(1, movieId);
                    ResultSet sr = sq.executeQuery();
                    while (sr.next()) {
                        JsonObject starJson = new JsonObject();
                        starJson.addProperty("id", sr.getString("id"));
                        starJson.addProperty("name", esc(sr.getString("name")));
                        stars.add(starJson);
                    }
                }
                outJson.add("stars", stars);
            }
        } catch (Exception e) {
            out.println("<p>Exception: "+esc(e.getMessage())+"</p>");
            e.printStackTrace();
        }

        /* ---- Add to Cart button and Back to List with SAME state ---- */
        String last = String.valueOf(req.getSession().getAttribute("lastQuery"));
        outJson.addProperty("lastQuery", last);

        out.write(outJson.toString());
    }

    private static String esc(String s){
        return s==null?"":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}