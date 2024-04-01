package org.example.task2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.TimezoneUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CookieThymeleafServlet extends HttpServlet {
    private TemplateEngine templateEngine;

    @Override
    public void init() throws ServletException {
        templateEngine = new TemplateEngine();

        JakartaServletWebApplication jakartaServletWebApplication  = JakartaServletWebApplication
                .buildApplication(this.getServletContext());

        WebApplicationTemplateResolver
                templateResolver = new WebApplicationTemplateResolver(jakartaServletWebApplication);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setOrder(templateEngine.getTemplateResolvers().size());
        templateResolver.setCacheable(false);
        templateResolver.setCharacterEncoding("UTF-8");
        templateEngine.addTemplateResolver(templateResolver);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");

        String timezone = request.getParameter("timezone");

        Cookie lastTimezoneCookie = getLastTimezoneCookie(request);

        ZoneId zoneId = determineZoneId(timezone, lastTimezoneCookie);

        updateLastTimezoneCookie(response, zoneId.getId());

        String formattedTime = getCurrentTime(zoneId);

        Context context = createContext(formattedTime);

        templateEngine.process("time", context, response.getWriter());
    }

    private Cookie getLastTimezoneCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("lastTimezone")) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private ZoneId determineZoneId(String timezone, Cookie lastTimezoneCookie) {
        if (timezone != null && !timezone.isEmpty()) {
            try {
                TimezoneUtils.validateTimezone(timezone);
                return ZoneId.of(timezone);
            } catch (IllegalArgumentException e) {
                return ZoneId.of("UTC");
            }
        } else if (lastTimezoneCookie != null && !lastTimezoneCookie.getValue().isEmpty()) {
            return ZoneId.of(lastTimezoneCookie.getValue());
        } else {
            return ZoneId.of("UTC");
        }
    }

    private void updateLastTimezoneCookie(HttpServletResponse response, String timezone) {
        Cookie timezoneCookie = new Cookie("lastTimezone", timezone);
        timezoneCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(timezoneCookie);
    }

    private String getCurrentTime(ZoneId zoneId) {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(zoneId);
        LocalDateTime currentTime = zonedDateTime.toLocalDateTime();
        return currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    }

    private Context createContext(String formattedTime) {
        Context context = new Context();
        context.setVariable("formattedTime", formattedTime);
        return context;
    }      
}
