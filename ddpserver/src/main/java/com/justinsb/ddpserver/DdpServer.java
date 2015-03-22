package com.justinsb.ddpserver;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import com.justinsb.ddpserver.examples.inmem.InMemoryStorage;
import com.justinsb.ddpserver.triggeredpoll.InMemoryInvalidationSystem;
import com.justinsb.ddpserver.triggeredpoll.TriggerDdpDataSource;

/**
 * The entry point for the app
 *
 */
public class DdpServer {
  public static void main(String[] args) throws Exception {
    final TriggerDdpDataSource ddpDataSource = new TriggerDdpDataSource(new InMemoryStorage(),
        new InMemoryInvalidationSystem());

    // Jetty boilerplate
    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(3003);
    server.addConnector(connector);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    DdpEndpoints.register(context, ddpDataSource);

    // Start jetty
    server.start();
    server.join();
  }

}