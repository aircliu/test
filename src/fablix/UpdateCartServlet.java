package fablix;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

@WebServlet("/update-cart")
public class UpdateCartServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req,HttpServletResponse resp)
            throws IOException {

        HttpSession s = req.getSession();
        @SuppressWarnings("unchecked")
        Map<String,Integer> cart =
                (Map<String,Integer>) s.getAttribute("cart");
        if (cart == null) { resp.sendRedirect("cart"); return; }

        if (req.getParameter("inc")!=null) {
            String id=req.getParameter("inc");
            cart.put(id, cart.get(id)+1);
        }
        if (req.getParameter("dec")!=null) {
            String id=req.getParameter("dec");
            cart.put(id, Math.max(1, cart.get(id)-1));
        }
        if (req.getParameter("del")!=null) {
            cart.remove(req.getParameter("del"));
        }
        resp.sendRedirect("cart.html");
    }
}
