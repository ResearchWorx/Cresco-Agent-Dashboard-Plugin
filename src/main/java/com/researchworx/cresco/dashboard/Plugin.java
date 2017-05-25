package com.researchworx.cresco.dashboard;

import com.google.auto.service.AutoService;
import com.researchworx.cresco.dashboard.controllers.*;
import com.researchworx.cresco.dashboard.filters.AuthenticationFilter;
import com.researchworx.cresco.dashboard.filters.NotFoundExceptionHandler;
import com.researchworx.cresco.dashboard.utilities.SessionFactoryManager;
import com.researchworx.cresco.library.plugin.core.CPlugin;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

@AutoService(CPlugin.class)
public class Plugin extends CPlugin {
    private HttpServer server;

    public void start() {
        setExec(new Executor(this));
        server = startServer("http://[::]:" + config.getStringParam("port", "3445") + "/");
        logger.info("Server up");
    }

    @Override
    public void cleanUp() {
        SessionFactoryManager.close();
        server.shutdownNow();
        logger.info("Server down");
    }

    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    private HttpServer startServer(String baseURI) {
        final OutputStream nullOutputStream = new OutputStream() { @Override public void write(int b) { } };
        Logger.getLogger("").addHandler(new ConsoleHandler() {{ setOutputStream(nullOutputStream); }});
        final ResourceConfig rc = new ResourceConfig()
                .register(AuthenticationFilter.class)
                .register(NotFoundExceptionHandler.class)
                .register(RootController.class)
                .register(AlertsController.class)
                .register(AgentsController.class)
                .register(PluginsController.class)
                .register(RegionsController.class)
                .register(GlobalController.class)
                ;

        AuthenticationFilter.connectPlugin(this);
        RootController.connectPlugin(this);
        AlertsController.connectPlugin(this);
        AgentsController.connectPlugin(this);
        PluginsController.connectPlugin(this);
        RegionsController.connectPlugin(this);
        GlobalController.connectPlugin(this);

        HttpServer server =  GrizzlyHttpServerFactory.createHttpServer(URI.create(baseURI), rc);
        HttpHandler handler = new CLStaticHttpHandler(Plugin.class.getClassLoader(), "includes/");
        server.getServerConfiguration().addHttpHandler(handler, "/includes");
        return server;
    }

    public static void main(String[] args) throws IOException {
        final String BASE_URI = "http://[::]:3445/";
        final OutputStream nullOutputStream = new OutputStream() { @Override public void write(int b) { } };
        Logger.getLogger("").addHandler(new ConsoleHandler() {{ setOutputStream(nullOutputStream); }});
        final ResourceConfig rc = new ResourceConfig()
                .register(AuthenticationFilter.class)
                .register(NotFoundExceptionHandler.class)
                .register(RootController.class)
                .register(AlertsController.class)
                .register(AgentsController.class)
                .register(PluginsController.class)
                .register(RegionsController.class)
                .register(GlobalController.class)
                ;

        HttpServer server =  GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
        HttpHandler handler = new CLStaticHttpHandler(Plugin.class.getClassLoader(), "includes/");
        server.getServerConfiguration().addHttpHandler(handler, "/includes");

        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl\nHit enter to stop it...", BASE_URI));
        System.in.read();
        SessionFactoryManager.close();
        server.shutdownNow();
    }
}
