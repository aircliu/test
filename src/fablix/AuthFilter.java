package fablix;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter("/*")
public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println("AuthFilter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest  r = (HttpServletRequest) req;
        HttpServletResponse s = (HttpServletResponse) res;
        String path = r.getServletPath();
        
        System.out.println("AuthFilter processing path: " + path);
        
        boolean loggedIn = r.getSession(false) != null &&
                r.getSession(false).getAttribute("user") != null;
        boolean publicPath = path.equals("/login")  || path.equals("/login.html") || path.equals("/login.js") ||
                path.equals("/api/login") || path.startsWith("/css") || path.startsWith("/images") ||
                path.equals("/style.css") || path.contains("fonts.googleapis.com");
                
        if (loggedIn || publicPath) {
            chain.doFilter(req, res);        // proceed
        } else {
            System.out.println("Redirecting to login page");
            s.sendRedirect(r.getContextPath() + "/login.html");
        }
    }
    
    @Override
    public void destroy() {
        System.out.println("AuthFilter destroyed");
    }
}