package fablix;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
@WebServlet("/movie-list")
public class MovieListServlet extends HttpServlet {
    private static final String URL  = "jdbc:mysql://localhost:3306/moviedb";
    private static final String USER = "mytestuser";
    private static final String PASS = "My6$Password";
    /* ---------- helper to clamp page size ---------- */
    private int safeSize(String val) {
        try {
            int n = Integer.parseInt(val);
            // Replace switch expression with traditional switch
            switch (n) {
                case 10:
                case 25:
                case 50:
                case 100:
                    return n;
                default:
                    return 10;
            }
        } catch (Exception e) { return 10; }
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        /* -------- read search / browse params -------- */
        String title    = req.getParameter("title");
        String yearStr  = req.getParameter("year");
        String director = req.getParameter("director");
        String star     = req.getParameter("star");
        String genre    = req.getParameter("genre");
        String initial  = req.getParameter("initial");
        /* -------- sorting & paging params ------------ */
        String sort1 = Optional.ofNullable(req.getParameter("sort1")).orElse("title"); // title|rating
        String dir1   = Optional.ofNullable(req.getParameter("dir1")).orElse("asc");     // asc|desc
        String sort2 = Optional.ofNullable(req.getParameter("sort2")).orElse("rating"); // rating|title
        String dir2   = Optional.ofNullable(req.getParameter("dir2")).orElse("asc");    // asc|desc
        int size  = safeSize(req.getParameter("size"));
        int page  = Math.max(1, Integer.parseInt(
                     Optional.ofNullable(req.getParameter("page")).orElse("1")));
        int offset = (page - 1) * size;
        /* -------- build dynamic SQL ------------------ */
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT m.id, m.title, m.year, m.director, r.rating\n");
        sql.append("FROM movies m\n");
        sql.append("LEFT JOIN ratings r ON m.id = r.movieId\n");
        List<String> where   = new ArrayList<>();
        List<Object> params  = new ArrayList<>();
        if (star  != null && !star.isEmpty()) {
            sql.append("JOIN stars_in_movies sim ON m.id = sim.movieId\n");
            sql.append("JOIN stars s            ON sim.starId = s.id\n");
        }
        if (genre != null && !genre.isEmpty()) {
            sql.append("JOIN genres_in_movies gim ON m.id = gim.movieId\n");
            sql.append("JOIN genres g             ON gim.genreId = g.id\n");
        }
        if (title    != null && !title.isEmpty())    { where.add("m.title LIKE ?");        params.add('%'+title+'%'); }
        if (yearStr  != null && !yearStr.isEmpty())  {
            try {
                where.add("m.year = ?");
                params.add(Integer.parseInt(yearStr));
            } catch (NumberFormatException ignored) { /* bad year → ignore */ }
        }
        if (director != null && !director.isEmpty()) { where.add("m.director LIKE ?");     params.add('%'+director+'%'); }
        if (star     != null && !star.isEmpty())     { where.add("s.name LIKE ?");         params.add('%'+star+'%'); }
        if (genre    != null && !genre.isEmpty())    { where.add("g.name = ?");            params.add(genre); }
        if (initial  != null) {
            if ("*".equals(initial))
                where.add("m.title REGEXP '^[^0-9A-Za-z]'");
            else {
                where.add("UPPER(m.title) LIKE ?");
                params.add(initial.toUpperCase()+"%");
            }
        }
        
        // Build the WHERE clause for both count and data queries
        String whereClause = "";
        if (!where.isEmpty())
            whereClause = "WHERE " + String.join(" AND ", where) + " ";
            
        // Create COUNT query to determine total results
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT COUNT(DISTINCT m.id) as total\n");
        countSql.append("FROM movies m\n");
        countSql.append("LEFT JOIN ratings r ON m.id = r.movieId\n");
        
        // Add the same JOINs to the count query
        if (star != null && !star.isEmpty()) {
            countSql.append("JOIN stars_in_movies sim ON m.id = sim.movieId\n");
            countSql.append("JOIN stars s            ON sim.starId = s.id\n");
        }
        if (genre != null && !genre.isEmpty()) {
            countSql.append("JOIN genres_in_movies gim ON m.id = gim.movieId\n");
            countSql.append("JOIN genres g             ON gim.genreId = g.id\n");
        }
        
        // Add WHERE clause to count query
        countSql.append(whereClause);
        
        // Complete the main query
        sql.append(whereClause);
        sql.append("ORDER BY ").append(sort1).append(' ').append(dir1)
           .append(',').append(' ').append(sort2).append(' ').append(dir2)
           .append(" LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);
        
        /* -------- execute query & render ------------- */
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(URL, USER, PASS)) {
                // First, get total count for pagination
                int totalMovies = 0;
                try (PreparedStatement countPs = con.prepareStatement(countSql.toString())) {
                    for (int i = 0; i < params.size() - 2; i++) {
                        countPs.setObject(i+1, params.get(i));
                    }
                    ResultSet countRs = countPs.executeQuery();
                    if (countRs.next()) {
                        totalMovies = countRs.getInt("total");
                    }
                }
                
                // Calculate if this is the last page
                int totalPages = (int) Math.ceil((double) totalMovies / size);
                boolean isLastPage = (page >= totalPages);
                
                // Now execute the main query
                try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) ps.setObject(i+1, params.get(i));
                    ResultSet rs = ps.executeQuery();
                    out.println("<!DOCTYPE html><html><head><title>Movies</title>");
                    out.println("<link href=\"https://fonts.googleapis.com/css2?family=Poppins:wght@400;600&display=swap\" rel=\"stylesheet\">");
                    out.println("<link href=\"style.css\" rel=\"stylesheet\">");
                    out.println("</head><body>");
                    
                    /* Checkout button always visible */
                    out.println("<p style='text-align:right'><a href='cart.html'>Checkout 🛒</a></p>");
                    
                    /* Layout container for side-by-side search forms */
                    out.println("<div style='display:flex; gap:20px; margin-bottom:20px;'>");
                    
                    /* -------- Add search form at left side -------- */
                    out.println("<div style='flex:1; padding:10px; background:#f8f8f8; border:1px solid #ddd;'>");
                    out.println("<h3>Search Movies</h3>");
                    out.println("<form action='movie-list' method='get'>");
                    out.println("  <div style='margin-bottom:8px;'>");
                    out.println("    <label style='display:inline-block; width:80px;'>Title:</label>");
                    out.println("    <input type='text' name='title' value='" + safe(title) + "'>");
                    out.println("  </div>");
                    out.println("  <div style='margin-bottom:8px;'>");
                    out.println("    <label style='display:inline-block; width:80px;'>Star:</label>");
                    out.println("    <input type='text' name='star' value='" + safe(star) + "'>");
                    out.println("  </div>");
                    out.println("  <div style='margin-bottom:8px;'>");
                    out.println("    <label style='display:inline-block; width:80px;'>Director:</label>");
                    out.println("    <input type='text' name='director' value='" + safe(director) + "'>");
                    out.println("  </div>");
                    out.println("  <div style='margin-bottom:8px;'>");
                    out.println("    <label style='display:inline-block; width:80px;'>Year:</label>");
                    out.println("    <input type='text' name='year' value='" + safe(yearStr) + "'>");
                    out.println("  </div>");
                    out.println("  <div>");
                    out.println("    <button type='submit'>Search</button>");
                    out.println("    <a href='movie-list'><button type='button'>Clear</button></a>");
                    out.println("  </div>");
                    out.println("</form>");
                    out.println("</div>");
                    
                    /* -------- Add genre search at right side -------- */
                    out.println("<div style='flex:1; padding:10px; background:#f8f8f8; border:1px solid #ddd;'>");
                    out.println("<h3>Search by Genres</h3>");
                    out.println("<div style='display:flex; flex-wrap:wrap; max-height:180px; overflow-y:auto;'>");
                    
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        try (Connection genreCon = DriverManager.getConnection(URL, USER, PASS);
                             PreparedStatement genrePs = genreCon.prepareStatement("SELECT name FROM genres ORDER BY name ASC")) {
                            ResultSet genreRs = genrePs.executeQuery();
                            
                            while (genreRs.next()) {
                                String genreName = genreRs.getString("name");
                                out.println("<div style='margin:5px 10px;'>");
                                out.println("<a href='movie-list?genre=" + 
                                    URLEncoder.encode(genreName, StandardCharsets.UTF_8) + 
                                    "' style='text-decoration:none; color:#333; background:#e9e9e9; padding:5px 10px; border-radius:3px;'>" + 
                                    genreName + "</a>");
                                out.println("</div>");
                            }
                        }
                    } catch (Exception ex) {
                        out.println("<p>Error loading genres: " + ex.getMessage() + "</p>");
                    }
                    
                    out.println("</div>");
                    out.println("</div>");
                    
                    /* Close the flex container for side-by-side search forms */
                    out.println("</div>");
                    
                    /* -------- Browse by genre links -------- */
                    out.println("<div style='margin: 20px 0; text-align: center;'>");
                    out.println("<h2 style='color: #333;'>Browsing by movie genres</h2>");
                    out.println("<div style='display: flex; justify-content: center;'>");

                    // Four columns of genres
                    out.println("<div style='margin: 0 15px; text-align: center;'>");
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                        try (Connection genreCon = DriverManager.getConnection(URL, USER, PASS);
                             PreparedStatement genrePs = genreCon.prepareStatement("SELECT name FROM genres ORDER BY name ASC")) {
                            ResultSet genreRs = genrePs.executeQuery();
                            
                            // Count total genres for column distribution
                            List<String> allGenres = new ArrayList<>();
                            while (genreRs.next()) {
                                allGenres.add(genreRs.getString("name"));
                            }
                            
                            int totalGenres = allGenres.size();
                            int genresPerColumn = (totalGenres + 3) / 4; // Ceil division to distribute across 4 columns
                            
                            out.println("<div style='display: flex; flex-wrap: wrap; justify-content: center;'>");
                            
                            // First column
                            out.println("<div style='margin: 0 15px; text-align: center;'>");
                            for (int i = 0; i < Math.min(genresPerColumn, totalGenres); i++) {
                                String genreName = allGenres.get(i);
                                out.println("<div style='margin: 8px 0;'><a href='movie-list?genre=" + 
                                    URLEncoder.encode(genreName, StandardCharsets.UTF_8) + 
                                    "' text-decoration: none;'>" + genreName + "</a></div>");
                            }
                            out.println("</div>");
                            
                            // Second column
                            out.println("<div style='margin: 0 15px; text-align: center;'>");
                            for (int i = genresPerColumn; i < Math.min(2*genresPerColumn, totalGenres); i++) {
                                String genreName = allGenres.get(i);
                                out.println("<div style='margin: 8px 0;'><a href='movie-list?genre=" + 
                                    URLEncoder.encode(genreName, StandardCharsets.UTF_8) + 
                                    "' text-decoration: none;'>" + genreName + "</a></div>");
                            }
                            out.println("</div>");
                            
                            // Third column
                            out.println("<div style='margin: 0 15px; text-align: center;'>");
                            for (int i = 2*genresPerColumn; i < Math.min(3*genresPerColumn, totalGenres); i++) {
                                String genreName = allGenres.get(i);
                                out.println("<div style='margin: 8px 0;'><a href='movie-list?genre=" + 
                                    URLEncoder.encode(genreName, StandardCharsets.UTF_8) + 
                                    "' text-decoration: none;'>" + genreName + "</a></div>");
                            }
                            out.println("</div>");
                            
                            // Fourth column
                            out.println("<div style='margin: 0 15px; text-align: center;'>");
                            for (int i = 3*genresPerColumn; i < totalGenres; i++) {
                                String genreName = allGenres.get(i);
                                out.println("<div style='margin: 8px 0;'><a href='movie-list?genre=" + 
                                    URLEncoder.encode(genreName, StandardCharsets.UTF_8) + 
                                    "' text-decoration: none;'>" + genreName + "</a></div>");
                            }
                            out.println("</div>");
                            
                            out.println("</div>"); // Close flex container
                        }
                    } catch (Exception ex) {
                        out.println("<p>Error loading genres: " + ex.getMessage() + "</p>");
                    }
                    out.println("</div>"); // Close column container
                    out.println("</div>"); // Close genre section

                    /* -------- Browse by title links -------- */
                    out.println("<div style='margin: 30px 0; text-align: center;'>");
                    out.println("<h2 style='color: #333;'>Browsing by movie title</h2>");
                    out.println("<div style='display: flex; justify-content: center;'>");
                    out.println("<div style='margin: 10px 0;'>");
                    // A-Z links
                    for (char ch = 'A'; ch <= 'Z'; ch++) {
                        out.println("<a href='movie-list?initial=" + ch + 
                            "' style='display: inline-block; margin: 0 5px; text-decoration: none;'>" + ch + "</a>");
                    }
                    out.println("</div>");
                    out.println("</div>");
                    out.println("<div style='display: flex; justify-content: center;'>");
                    out.println("<div style='margin: 10px 0;'>");
                    // 0-9 links and *
                    for (char ch = '0'; ch <= '9'; ch++) {
                        out.println("<a href='movie-list?initial=" + ch + 
                            "' style='display: inline-block; margin: 0 5px; text-decoration: none;'>" + ch + "</a>");
                    }
                    out.println("<a href='movie-list?initial=*' style='display: inline-block; margin: 0 5px; text-decoration: none;'>*</a>");
                    out.println("</div>");
                    out.println("</div>");
                    out.println("</div>"); // Close title browse section
                    
                    /* -------- controls (sort & paging) ---- */
                    out.println("<form style='margin-bottom:10px'>");
                    out.println("  <input type='hidden' name='title'    value='" + safe(title) + "'>");
                    out.println("  <input type='hidden' name='year'     value='" + safe(yearStr) + "'>");
                    out.println("  <input type='hidden' name='director' value='" + safe(director) + "'>");
                    out.println("  <input type='hidden' name='star'     value='" + safe(star) + "'>");
                    out.println("  <input type='hidden' name='genre'    value='" + safe(genre) + "'>");
                    out.println("  <input type='hidden' name='initial'  value='" + safe(initial) + "'>");
                    out.println("  Sort by:");
                    out.println("  <select name='sort1'>");
                    out.println("    <option value='title'" + sel("title".equals(sort1)) + ">title</option>");
                    out.println("    <option value='rating'" + sel("rating".equals(sort1)) + ">rating</option>");
                    out.println("  </select>");
                    out.println("  <select name='dir1'>");
                    out.println("    <option value='asc'" + sel("asc".equals(dir1)) + ">asc</option>");
                    out.println("    <option value='desc'" + sel("desc".equals(dir1)) + ">desc</option>");
                    out.println("  </select>");
                    out.println("  <select name='sort2'>");
                    out.println("    <option value='rating'" + sel("rating".equals(sort2)) + ">rating</option>");
                    out.println("    <option value='title'" + sel("title".equals(sort2)) + ">title</option>");
                    out.println("  </select>");
                    out.println("  <select name='dir2'>");
                    out.println("    <option value='asc'" + sel("asc".equals(dir2)) + ">asc</option>");
                    out.println("    <option value='desc'" + sel("desc".equals(dir2)) + ">desc</option>");
                    out.println("  </select>");
                    out.println("  Page size:");
                    out.println("  <select name='size'>");
                    out.println("    <option value='10'" + sel(size==10) + ">10</option>");
                    out.println("    <option value='25'" + sel(size==25) + ">25</option>");
                    out.println("    <option value='50'" + sel(size==50) + ">50</option>");
                    out.println("    <option value='100'" + sel(size==100) + ">100</option>");
                    out.println("  </select>");
                    out.println("  <button type='submit'>Apply</button>");
                    out.println("</form>");
                    
                    /* -------- table header ---------------- */
                    out.println("<table><tr>");
                    out.println("  <th>Title</th><th>Year</th><th>Director</th>");
                    out.println("  <th>Genres</th><th>Stars</th><th>Rating</th><th></th>");
                    out.println("</tr>");
                    boolean any = false;
                    while (rs.next()) {
                        any = true;
                        String mid    = rs.getString("id");
                        String mtitle = rs.getString("title");
                        int    myear  = rs.getInt("year");
                        String mdir   = rs.getString("director");
                        String mrat   = rs.getString("rating");
                        /* ---- first 3 genres alphabetical ---- */
                        String genres;
                        try (PreparedStatement gq = con.prepareStatement(
                            "SELECT g.name " +
                            "FROM genres_in_movies gim " +
                            "JOIN genres g ON gim.genreId = g.id " +
                            "WHERE gim.movieId = ? " +
                            "ORDER BY g.name " +
                            "LIMIT 3")) {
                            gq.setString(1, mid);
                            try (ResultSet grs = gq.executeQuery()) {
                                genres = join(grs, ",", 1,
                                        row -> {
                                            String gname = row[0];
                                            return "<a href='movie-list?genre="+
                                                    URLEncoder.encode(gname, StandardCharsets.UTF_8)+"'>"+
                                                    gname+"</a>";
                                        });
                            }
                        }
                        /* ---- first 3 stars by count desc ---- */
                        String stars;
                        try (PreparedStatement sq = con.prepareStatement(
                            "SELECT s.id, s.name, (SELECT count(DISTINCT movieId) FROM stars_in_movies subSm WHERE subSm.starId = s.id) as cnt " +
                            "FROM stars_in_movies sim " +
                            "JOIN stars s ON sim.starId = s.id " +
                            "WHERE sim.movieId = ? " +
                            "ORDER BY cnt DESC, s.name ASC " +
                            "LIMIT 3")) {
                            sq.setString(1, mid);
                            try (ResultSet srs = sq.executeQuery()) {
                                stars = join(srs, ",", 2,
                                        row -> "<a href='single-star.html?starId="+row[0]+"'>"+row[1]+"</a>");
                            }
                        }
                        out.println("<tr>");
                        out.println("  <td><a href='single-movie.html?movieId=" + mid + "'>" + esc(mtitle) + "</a></td>");
                        out.println("  <td>" + myear + "</td><td>" + esc(mdir) + "</td>");
                        out.println("  <td>" + genres + "</td><td>" + stars + "</td>");
                        out.println("  <td>" + (mrat == null ? "N/A" : mrat) + "</td>");
                        out.println("  <td>");
                        out.println("    <form action='add-to-cart' method='post' style='display:inline'>");
                        out.println("      <input type='hidden' name='movieId' value='" + mid + "'>");
                        out.println("      <button type='submit'>Add&nbsp;to&nbsp;Cart</button>");
                        out.println("    </form>");
                        out.println("  </td>");
                        out.println("</tr>");
                    }
                    out.println("</table>");
                    if (!any) out.println("<p>No movies found.</p>");
                    /* -------- Prev / Next buttons ---------- */
                    out.println("<form style='margin-top:8px'>");
                    out.println("  <input type='hidden' name='title' value='" + safe(title) + "'>");
                    out.println("  <input type='hidden' name='year' value='" + safe(yearStr) + "'>");
                    out.println("  <input type='hidden' name='director' value='" + safe(director) + "'>");
                    out.println("  <input type='hidden' name='star' value='" + safe(star) + "'>");
                    out.println("  <input type='hidden' name='genre' value='" + safe(genre) + "'>");
                    out.println("  <input type='hidden' name='initial' value='" + safe(initial) + "'>");
                    out.println("  <input type='hidden' name='sort1' value='" + sort1 + "'>");
                    out.println("  <input type='hidden' name='dir1' value='" + dir1 + "'>");
                    out.println("  <input type='hidden' name='sort2' value='" + sort2 + "'>");
                    out.println("  <input type='hidden' name='dir2' value='" + dir2 + "'>");
                    out.println("  <input type='hidden' name='size' value='" + size + "'>");
                    out.println("  <button name='page' value='" + (page-1) + "' " + (page==1?"disabled":"") + ">Prev</button>");
                    out.println("  <button name='page' value='" + (page+1) + "' " + (isLastPage?"disabled":"") + ">Next</button>");
                    out.println("</form>");
                    /* -------- save lastQuery in session ---- */
                    HttpSession session = req.getSession();
                    session.setAttribute("lastQuery",
                            req.getQueryString()==null? "" : req.getQueryString());
                    out.println("</body></html>");
                }
            }
        } catch (Exception e) {
            resp.getWriter().println("Error: "+e.getMessage());
            e.printStackTrace();
        }
    }
    /* --------------- helpers ------------------------ */
    private static String esc(String s){
        return s==null ? "" : s.replace("&","&amp;")
                              .replace("<","&lt;")
                              .replace(">","&gt;");
    }
    private static String safe(String s){ return s==null?"":esc(s); }
    private static String sel(boolean b){ return b?" selected":""; }
    /** join helper for ResultSet → comma-list */
    private static String join(ResultSet rs, String delim, int cols,
                               RowFormatter fmt) throws SQLException {
        StringBuilder sb = new StringBuilder();
        while (rs.next()) {
            if (sb.length() > 0) sb.append(delim).append(' ');
            String[] row = new String[cols];
            for (int i = 0; i < cols; i++) row[i] = rs.getString(i+1);
            sb.append(fmt.apply(row));
        }
        return sb.toString();
    }
    @FunctionalInterface private interface RowFormatter {
        String apply(String[] row);
    }
}