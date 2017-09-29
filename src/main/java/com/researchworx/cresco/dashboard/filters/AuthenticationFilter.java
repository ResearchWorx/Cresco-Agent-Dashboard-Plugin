package com.researchworx.cresco.dashboard.filters;

import com.researchworx.cresco.dashboard.Plugin;
import com.researchworx.cresco.dashboard.controllers.RootController;
import com.researchworx.cresco.dashboard.models.LoginSession;
import com.researchworx.cresco.dashboard.services.LoginSessionService;
import com.researchworx.cresco.library.utilities.CLogger;

import javax.annotation.security.PermitAll;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;


@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
    public static final String SESSION_COOKIE_NAME = "crescoAgentSessionID";
    private static final int TIMEOUT_IN_MINUTES = 120;
    private static Response REDIRECT_LOGOUT;
    private static CLogger logger;

    public static void connectPlugin(Plugin plugin) {
        logger = new CLogger(AuthenticationFilter.class, plugin.getMsgOutQueue(), plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), CLogger.Level.Trace);
    }

    @Context
    private ResourceInfo resourceInfo;

    public AuthenticationFilter() {
        try {
            URI logout_uri = new URI("/logout");
            REDIRECT_LOGOUT = Response.seeOther(logout_uri).build();
        } catch (URISyntaxException e) {
            System.out.println("Failed to generate logout redirect");
            REDIRECT_LOGOUT = Response.serverError().build();
        }
    }

    private Response toLogin(NewCookie redirectCookie) {
        try {
            URI login_uri = new URI("/login");
            return Response.seeOther(login_uri).cookie(redirectCookie).build();
        } catch (URISyntaxException e) {
            System.out.println("Failed to generate login redirect");
            return Response.serverError().build();
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            Method method = resourceInfo.getResourceMethod();
            if (method.isAnnotationPresent(PermitAll.class))
                return;

            // Then check is the service key exists and is valid.
            String serviceKey = requestContext.getHeaderString("X-Auth-API-Service-Key");
            if (serviceKey != null) {
                return;
            }

            Cookie sessionCookie = requestContext.getCookies().get(SESSION_COOKIE_NAME);
            if (sessionCookie == null) {
                NewCookie redirectCookie = new NewCookie(RootController.LOGIN_REDIRECT_COOKIE_NAME, "/" + requestContext.getUriInfo().getPath(), null, null, null, 60 * 60, false);
                requestContext.abortWith(toLogin(redirectCookie));
                return;
            }
            LoginSession loginSession = LoginSessionService.getByID(sessionCookie.getValue());
            if (loginSession == null) {
                requestContext.abortWith(REDIRECT_LOGOUT);
                return;
            }
            Calendar timeout = Calendar.getInstance();
            timeout.add(Calendar.MINUTE, -1 * TIMEOUT_IN_MINUTES);
            if (loginSession.getLastSeenAsDate().before(timeout.getTime()) && !loginSession.getRemememberMe()) {
                requestContext.abortWith(REDIRECT_LOGOUT);
                return;
            }
            LoginSessionService.seen(loginSession);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
