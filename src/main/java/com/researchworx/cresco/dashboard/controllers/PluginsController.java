package com.researchworx.cresco.dashboard.controllers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        logger = new CLogger(PluginsController.class, plugin.getMsgOutQueue(), plugin.getRegion(), plugin.getAgent(),
                plugin.getPluginID(), CLogger.Level.Trace);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index(@CookieParam(AuthenticationFilter.SESSION_COOKIE_NAME) String sessionID) {
        try {
            LoginSession loginSession = LoginSessionService.getByID(sessionID);
            PebbleEngine engine = new PebbleEngine.Builder().build();
            PebbleTemplate compiledTemplate = engine.getTemplate("plugins/index.html");

            Map<String, Object> context = new HashMap<>();
            if (loginSession != null)
                context.put("user", loginSession.getUsername());
            context.put("section", "plugins");
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
    @Path("info/{region}/{agent}/{plugin:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response info(@PathParam("region") String region,
                         @PathParam("agent") String agent,
                         @PathParam("plugin") String pluginID) {
        logger.trace("Call to info({}, {}, {})", region, agent, pluginID);
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
            request.setParam("action", "plugininfo");
            request.setParam("action_region", region);
            request.setParam("action_agent", agent);
            request.setParam("action_plugin", pluginID);
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String info = "{}";
            if (response.getParam("plugininfo") != null)
                info = response.getCompressedParam("plugininfo");
            return Response.ok(info, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("info({}, {}, {}) : {}", region, agent, pluginID, e.getMessage());
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
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
            request.setParam("action", "listplugins");
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String plugins = "[]";
            if (response.getParam("pluginslist") != null)
                plugins = response.getCompressedParam("pluginslist");
            return Response.ok(plugins, MediaType.APPLICATION_JSON_TYPE).build();
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
        logger.trace("Call to listByRegion()");
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
            request.setParam("action", "listplugins");
            request.setParam("action_region", region);
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String plugins = "[]";
            if (response.getParam("pluginslist") != null)
                plugins = response.getCompressedParam("pluginslist");
            return Response.ok(plugins, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("listByRegion({}) : {}", region, e.getMessage());
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Path("list/{region}/{agent}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listByAgent(@PathParam("region") String region,
                                @PathParam("agent") String agent) {
        logger.trace("Call to listByAgent({}, {})", region, agent);
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
            request.setParam("action", "listplugins");
            request.setParam("action_region", region);
            request.setParam("action_agent", agent);
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
            String plugins = "[]";
            if (response.getParam("pluginslist") != null)
                plugins = response.getCompressedParam("pluginslist");
            return Response.ok(plugins, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("listByAgent({}, {}) : {}", region, agent, e.getMessage());
            return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Path("local")
    @Produces(MediaType.APPLICATION_JSON)
    public Response local() {
        try {
            if (plugin == null)
                return Response.ok("{}", MediaType.APPLICATION_JSON_TYPE).build();
            MsgEvent request = new MsgEvent(MsgEvent.Type.CONFIG, plugin.getRegion(), plugin.getAgent(),
                    plugin.getPluginID(), "Plugin Inventory Request");
            request.setParam("src_region", plugin.getRegion());
            request.setParam("src_agent", plugin.getAgent());
            request.setParam("src_plugin", plugin.getPluginID());
            request.setParam("dst_region", plugin.getRegion());
            request.setParam("dst_agent", plugin.getAgent());
            request.setParam("configtype", "plugininventory");
            MsgEvent response = plugin.sendRPC(request);
            if (response == null)
                return Response.ok("{\"error\":\"Cresco rpc response was null\"}",
                        MediaType.APPLICATION_JSON_TYPE).build();
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
            return Response.ok(new Scanner(new URL(plugin.getConfig().getStringParam("plugin_repository_url",
                    "http://128.163.217.124:3446/plugins")).openStream(), "UTF-8")
                    .useDelimiter("\\A").next(), MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            if (plugin != null)
                logger.error("repo() : {}", e.getMessage());
            return Response.ok("[]", MediaType.APPLICATION_JSON_TYPE).build();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("add/{json:.*}")
    public Response add(@PathParam("json") String json) {
        JsonElement jElement = new JsonParser().parse(json);
        JsonObject jObject = jElement.getAsJsonObject();
        if (plugin == null)
            return Response.ok("{\"error\":true,\"message\":\"No communication channel open to Cresco Agent.\"}",
                    MediaType.APPLICATION_JSON_TYPE).build();
        logger.info("url: {}, config: {}", jObject.get("url").getAsString(),
                jObject.get("config").getAsString());
        MsgEvent addPlugin = new MsgEvent(MsgEvent.Type.CONFIG, plugin.getRegion(),
                plugin.getAgent(), plugin.getPluginID(),
                "Issuing command to add new plugin to agent");
        addPlugin.setParam("src_region", plugin.getRegion());
        addPlugin.setParam("src_agent", plugin.getAgent());
        addPlugin.setParam("src_plugin", plugin.getPluginID());
        addPlugin.setParam("dst_region", plugin.getRegion());
        addPlugin.setParam("dst_agent", plugin.getAgent());
        addPlugin.setParam("configtype", "pluginadd");
        addPlugin.setParam("pluginurl", jObject.get("url").getAsString());
        addPlugin.setParam("configparams", jObject.get("config").getAsString());
        /*MsgEvent ret = plugin.sendRPC(addPlugin);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<String, String> param : ret.getParams().entrySet()) {
            sb.append("\"");
            sb.append(param.getKey());
            sb.append("\":\"");
            sb.append(param.getValue());
            sb.append("\",");
        }
        sb.append("\"error\":false}");*/
        return Response.ok(/*sb.toString()*/"{}", MediaType.APPLICATION_JSON_TYPE).build();
    }
}
