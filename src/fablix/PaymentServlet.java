package fablix;

import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet(name = "PaymentServlet", urlPatterns ="/api/payment")
public class PaymentServlet extends HttpServlet {

    private static final String URL ="jdbc:mysql://localhost:3306/moviedb";
    private static final String USER="mytestuser";
    private static final String PASS="My6$Password";

    @Override
    protected void doGet(HttpServletRequest req,HttpServletResponse resp)
            throws IOException {
        PrintWriter out = resp.getWriter();
        resp.setContentType("application/json");
        JsonObject outJson = new JsonObject();
        HttpSession s=req.getSession();
        @SuppressWarnings("unchecked")
        Map<String,Integer> cart  =(Map<String,Integer>) s.getAttribute("cart");
        @SuppressWarnings("unchecked")
        Map<String,Float> prices =(Map<String,Float>) s.getAttribute("prices");
        float total=0;
        for(String m:cart.keySet()) total+=cart.get(m)*prices.get(m);
        outJson.addProperty("total",total);
        out.write(outJson.toString());
        out.close();
    }

    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)
            throws IOException {

        String fn  = req.getParameter("first");
        String ln  = req.getParameter("last");
        String cc  = req.getParameter("cc");
        String exp = req.getParameter("exp");
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();
        JsonObject outJson = new JsonObject();

        boolean ok=false;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try(Connection c=DriverManager.getConnection(URL,USER,PASS);
                PreparedStatement ps = c.prepareStatement(
                  "SELECT 1 FROM creditcards WHERE id=? AND firstName=? AND lastName=? AND expiration=?")) {
                ps.setString(1, cc); ps.setString(2, fn); ps.setString(3, ln); ps.setString(4, exp);
                ok = ps.executeQuery().next();
            }
        } catch (Exception e) { e.printStackTrace(); }

        if (!ok) {
            outJson.addProperty("message", "Payment info incorrect");
            outJson.addProperty("success", false);
        } else {
            HttpSession s = req.getSession();
            s.setAttribute("payName", fn + " " + ln);
            s.setAttribute("payCc", cc);
            outJson.addProperty("success", true);
        }
        out.write(outJson.toString());
        out.close();
    }
}