package com.researchworx.cresco.dashboard.controllers;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.researchworx.cresco.dashboard.Plugin;
import com.researchworx.cresco.dashboard.filters.AuthenticationFilter;
import com.researchworx.cresco.dashboard.helpers.NewPlugin;
import com.researchworx.cresco.dashboard.models.LoginSession;
import com.researchworx.cresco.dashboard.services.LoginSessionService;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.utilities.CLogger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@Path("plugins")
public class PluginsController {
    private static Plugin plugin = null;
    private static CLogger logger = null;

    public static void connectPlugin(Plugin inPlugin) {
        plugin = inPlugin;
        logger = new CLogger(PluginsController.class, plugin.getMsgOutQueue(), plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), CLogger.Level.Trace);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            LoginSession loginSession = LoginSessionService.getByID(sessionID);
            PebbleEngine engine = new PebbleEngine.Builder().build();
            PebbleTemplate compiledTemplate = engine.getTemplate("plugins/index.html");

            Map<String, Object> context = new HashMap<>();
            context.put("user", loginSession.getUsername());
            context.put("section", "plugins");
            context.put("page", "index");
            if (plugin == null)
                context.put("pluginPath", "plugins/");
            else {
                String jarfile = plugin.getConfig().getStringParam("jarfile");
                context.put("pluginPath", jarfile.substring(0, jarfile.lastIndexOf('/') + 1));
            }

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

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            if (plugin == null)
                return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
            MsgEvent request = new MsgEvent(MsgEvent.Type.CONFIG, plugin.getRegion(), plugin.getAgent(), plugin.getPluginID(), "Plugin Inventory Request");
            request.setParam("src_region", plugin.getRegion());
            request.setParam("src_agent", plugin.getAgent());
            request.setParam("src_plugin", plugin.getPluginID());
            request.setParam("dst_region", plugin.getRegion());
            request.setParam("dst_agent", plugin.getAgent());
            request.setParam("configtype", "plugininventory");
            MsgEvent response = plugin.sendRPC(request);
            String plugins = "[]";
            if (response.getParam("pluginlist") != null)
                plugins = response.getParam("pluginlist");
            return Response.ok(plugins, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("list() : {}", e.getMessage());
            return Response.ok("[]", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Path("repository")
    @Produces(MediaType.APPLICATION_JSON)
    public Response repo() {
        try {
            return Response.ok(new Scanner(new URL("http://128.163.217.124:3446/plugins").openStream(), "UTF-8").useDelimiter("\\A").next(), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("repo() : {}", e.getMessage());
            return Response.ok("[]", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces(MediaType.APPLICATION_JSON)
    @Path("add")
    public Response add(NewPlugin newPlugin) {
        logger.info("url: {}, config: {}", newPlugin.url, newPlugin.config);
        return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
    }
}
