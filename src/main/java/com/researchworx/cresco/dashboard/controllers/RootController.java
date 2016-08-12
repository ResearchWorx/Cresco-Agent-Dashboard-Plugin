package com.researchworx.cresco.dashboard.controllers;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.researchworx.cresco.dashboard.Plugin;
import com.researchworx.cresco.dashboard.filters.AuthenticationFilter;
import com.researchworx.cresco.dashboard.models.LoginSession;
import com.researchworx.cresco.dashboard.services.AlertService;
import com.researchworx.cresco.dashboard.services.LoginSessionService;
import com.researchworx.cresco.library.utilities.CLogger;

import javax.annotation.security.PermitAll;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class RootController {
    private static Plugin plugin = null;
    private static CLogger logger = null;
    private static final String LOGIN_ERROR_COOKIE_NAME = "crescoAgentLoginError";
    public static final String LOGIN_REDIRECT_COOKIE_NAME = "crescoAgentLoginRedirect";

    public static void connectPlugin(Plugin in_plugin) {
        plugin = in_plugin;
        logger = new CLogger(RootController.class, in_plugin.getMsgOutQueue(), in_plugin.getRegion(), in_plugin.getAgent(), in_plugin.getPluginID(), CLogger.Level.Trace);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            LoginSession loginSession = LoginSessionService.getByID(sessionID);
            PebbleEngine engine = new PebbleEngine.Builder().build();
            PebbleTemplate compiledTemplate = engine.getTemplate("index.html");

            Map<String, Object> context = new HashMap<>();
            context.put("user", loginSession.getUsername());
            context.put("section", "root");
            context.put("page", "index");

            Writer writer = new StringWriter();
            compiledTemplate.evaluate(writer, context);

            return Response.ok(writer.toString()).build();
        } catch (PebbleException e) {
            return Response.ok("PebbleException: " + e.getMessage()).build();
        } catch (IOException e) {
            return Response.ok("IOException: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.ok("Server error: " + e.getMessage()).build();
        }
    }

    @PermitAll
    @GET
    @Path("login")
    @Produces(MediaType.TEXT_HTML)
    public Response getLogin(@CookieParam(LOGIN_REDIRECT_COOKIE_NAME) String redirect,
                             @CookieParam(LOGIN_ERROR_COOKIE_NAME) String error) {
        try {
            PebbleEngine engine = new PebbleEngine.Builder().build();
            PebbleTemplate compiledTemplate = engine.getTemplate("login.html");

            Map<String, Object> context = new HashMap<>();
            if (redirect != null)
                context.put("redirect", redirect);
            else
                context.put("redirect", "/");
            if (error != null)
                context.put("error", error);

            NewCookie deleteRedirect = new NewCookie(LOGIN_REDIRECT_COOKIE_NAME, null, null, null, null, 0, false);
            NewCookie deleteError = new NewCookie(LOGIN_ERROR_COOKIE_NAME, null, null, null, null, 0, false);

            Writer writer = new StringWriter();
            compiledTemplate.evaluate(writer, context);

            return Response.ok(writer.toString()).cookie(deleteRedirect, deleteError).build();
        } catch (PebbleException e) {
            return Response.ok("PebbleException: " + e.getMessage()).build();
        } catch (IOException e) {
            return Response.ok("IOException: " + e.getMessage()).build();
        } catch (Exception e) {
            return Response.serverError().build();
        }
    }

    @PermitAll
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postLogin(@FormParam("username") String username,
                              @FormParam("password") String password,
                              @FormParam("rememberMe") Boolean rememberMe,
                              @FormParam("redirect") String redirect) {
        try {
            if (plugin == null) {
                if (username == null || username.equals("") || !username.toLowerCase().trim().equals("admin") ||
                        password == null || password.equals("") || !password.toLowerCase().trim().equals("cresco")) {
                    NewCookie errorCookie = new NewCookie(LOGIN_ERROR_COOKIE_NAME, "Invalid Username or Password!", null, null, null, 60 * 60, false);
                    return Response.seeOther(new URI("/login")).cookie(errorCookie).build();
                }
            } else {
                if (username == null || username.equals("") || !username.toLowerCase().trim().equals(plugin.getConfig().getStringParam("username", "admin").toLowerCase().trim()) ||
                        password == null || password.equals("") || !password.toLowerCase().trim().equals(plugin.getConfig().getStringParam("password", "cresco").toLowerCase().trim())) {
                    NewCookie errorCookie = new NewCookie(LOGIN_ERROR_COOKIE_NAME, "Invalid Username or Password!", null, null, null, 60 * 60, false);
                    return Response.seeOther(new URI("/login")).cookie(errorCookie).build();
                }
            }
            LoginSession loginSession = LoginSessionService.create(username.trim(), rememberMe != null);
            return Response.seeOther(new URI(redirect))
                    .cookie(new NewCookie(AuthenticationFilter.SESSION_COOKIE_NAME, loginSession.getId(), null, null, null, 60 * 60 * 24 * 365 * 10, false))
                    .build();
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        }
    }

    @PermitAll
    @GET
    @Path("logout")
    public Response getLogout(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            LoginSessionService.delete(sessionID);
            NewCookie deleteSession = new NewCookie(AuthenticationFilter.SESSION_COOKIE_NAME, null, null, null, null, 0, false);
            return Response.seeOther(new URI("/login")).cookie(deleteSession).build();
        } catch (URISyntaxException e) {
            return Response.serverError().build();
        }
    }

    @GET
    @Path("notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            List<Object[]> alerts = AlertService.notifications();
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (Object[] alert : alerts) {
                sb.append("{\"id\":\"");
                sb.append(alert[0]);
                sb.append("\",");

                sb.append("\"created\":");
                sb.append(alert[1]);
                sb.append(",");

                sb.append("\"msg\":\"");
                sb.append(alert[2]);
                sb.append("\"},");
            }
            sb.append("]");
            if (alerts.size() > 0)
                sb.deleteCharAt(sb.lastIndexOf(","));
            return Response.ok(sb.toString(), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("list() : {}", e.getMessage());
            return Response.ok("[]", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }
}
