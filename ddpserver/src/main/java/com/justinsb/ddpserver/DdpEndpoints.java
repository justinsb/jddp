package com.justinsb.ddpserver;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Configurator;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import com.justinsb.ddpserver.examples.inmem.InMemoryStorage;
import com.justinsb.ddpserver.triggeredpoll.InMemoryInvalidationSystem;
import com.justinsb.ddpserver.triggeredpoll.TriggerDdpDataSource;

public class DdpEndpoints {
  public static void register(ServletContextHandler context, DdpDataSource ddpDataSource) throws Exception {
    // Add a CORS filter to allow (any) CORS request
    context.addFilter(CorsFilter.class, "/sockjs/*", EnumSet.allOf(DispatcherType.class));
    context.addFilter(CorsFilter.class, "/websocket", EnumSet.allOf(DispatcherType.class));

    // Add our DDP SockJS servlet
    DdpSockJsServlet ddpServlet = new DdpSockJsServlet(ddpDataSource);
    context.addServlet(new ServletHolder(ddpServlet), "/sockjs/*");

    // Initialize javax.websocket layer
    ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

    // Meteor server will redirect /websocket -> /sockjs/websocket,
    // "in order to not expose sockjs to clients that want to use raw websockets"
    // We just have a direct servlet

    String websocketPath = "/websocket";
    Configurator serverEndpointConfigurator = new Configurator() {

      @Override
      public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        if (!endpointClass.equals(DdpWebsocketEndpoint.class)) {
          throw new IllegalStateException();
        }
        return (T) new DdpWebsocketEndpoint(ddpDataSource);
      }
    };
    ServerEndpointConfig websocketConfig = ServerEndpointConfig.Builder
        .create(DdpWebsocketEndpoint.class, websocketPath).configurator(serverEndpointConfigurator).build();
    wscontainer.addEndpoint(websocketConfig);
  }

}