package com.researchworx.cresco.dashboard.controllers;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.error.PebbleException;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.researchworx.cresco.dashboard.Plugin;
import com.researchworx.cresco.dashboard.filters.AuthenticationFilter;
import com.researchworx.cresco.dashboard.models.LoginSession;
import com.researchworx.cresco.dashboard.services.LoginSessionService;
import com.researchworx.cresco.library.messaging.MsgEvent;
import com.researchworx.cresco.library.utilities.CLogger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

@Path("agents")
public class AgentsController {
    private static Plugin plugin = null;
    private static CLogger logger = null;

    public static void connectPlugin(Plugin inPlugin) {
        plugin = inPlugin;
        logger = new CLogger(AgentsController.class, plugin.getMsgOutQueue(), plugin.getRegion(),
                plugin.getAgent(), plugin.getPluginID(), CLogger.Level.Trace);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            LoginSession loginSession = LoginSessionService.getByID(sessionID);
            PebbleEngine engine = new PebbleEngine.Builder().build();
            PebbleTemplate compiledTemplate = engine.getTemplate("agents/index.html");

            Map<String, Object> context = new HashMap<>();
            if (loginSession != null)
                context.put("user", loginSession.getUsername());
            context.put("section", "agents");
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

    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        logger.trace("Call to list()");
        try {
            if (plugin == null)
                return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
            MsgEvent request = new MsgEvent(MsgEvent.Type.EXEC, plugin.getRegion(), plugin.getAgent(),
                    plugin.getPluginID(), "Agent List Request");
            request.setParam("src_region", plugin.getRegion());
            request.setParam("src_agent", plugin.getAgent());
            request.setParam("src_plugin", plugin.getPluginID());
            request.setParam("dst_region", plugin.getRegion());
            request.setParam("dst_agent", plugin.getAgent());
            request.setParam("dst_plugin", "plugin/0");
            request.setParam("is_regional", Boolean.TRUE.toString());
            request.setParam("is_global", Boolean.TRUE.toString());
            request.setParam("globalcmd", "true");
            request.setParam("action", "listagents");
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String agents = "[]";
            if (response.getParam("agentslist") != null)
                agents = response.getCompressedParam("agentslist");
            return Response.ok(agents, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("list() : {}", e.getMessage());
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Path("list/{region}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listByRegion(@PathParam("region") String region) {
        logger.trace("Call to listByRegion({})", region);
        try {
            if (plugin == null)
                return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
            MsgEvent request = new MsgEvent(MsgEvent.Type.EXEC, plugin.getRegion(), plugin.getAgent(),
                    plugin.getPluginID(), "Agent List Request");
            request.setParam("src_region", plugin.getRegion());
            request.setParam("src_agent", plugin.getAgent());
            request.setParam("src_plugin", plugin.getPluginID());
            request.setParam("dst_region", plugin.getRegion());
            request.setParam("dst_agent", plugin.getAgent());
            request.setParam("dst_plugin", "plugin/0");
            request.setParam("is_regional", Boolean.TRUE.toString());
            request.setParam("is_global", Boolean.TRUE.toString());
            request.setParam("globalcmd", "true");
            request.setParam("action", "listagents");
            request.setParam("action_region", region);
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String agents = "[]";
            if (response.getParam("agentslist") != null)
                agents = response.getCompressedParam("agentslist");
            return Response.ok(agents, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("listByRegion({}) : {}", region, e.getMessage());
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Path("resources/{region}/{agent}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resources(@PathParam("region") String region,
                              @PathParam("agent") String agent) {
        logger.trace("Call to resources({}, {})", region, agent);
        try {
            if (plugin == null)
                return Response.ok("{\"regions\":[]}", MediaType.APPLICATION_JSON_TYPE).build();
            MsgEvent request = new MsgEvent(MsgEvent.Type.EXEC, plugin.getRegion(), plugin.getAgent(),
                    plugin.getPluginID(), "Region List Request");
            request.setParam("src_region", plugin.getRegion());
            request.setParam("src_agent", plugin.getAgent());
            request.setParam("src_plugin", plugin.getPluginID());
            request.setParam("dst_region", plugin.getRegion());
            request.setParam("dst_agent", plugin.getAgent());
            request.setParam("dst_plugin", "plugin/0");
            request.setParam("is_regional", Boolean.TRUE.toString());
            request.setParam("is_global", Boolean.TRUE.toString());
            request.setParam("globalcmd", Boolean.TRUE.toString());
            request.setParam("action", "resourceinfo");
            request.setParam("action_region", region);
            request.setParam("action_agent", agent);
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String regions = "[]";
            if (response.getParam("resourceinfo") != null)
                regions = response.getCompressedParam("resourceinfo");
            return Response.ok(regions, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            if (plugin != null)
                logger.error("resources({}, {}) : {}", region, agent, sw.toString());
            return Response.ok("{\"regions\":[]}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }
}
