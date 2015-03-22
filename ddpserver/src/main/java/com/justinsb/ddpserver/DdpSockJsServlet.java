package com.justinsb.ddpserver;

import java.io.IOException;

import org.projectodd.sockjs.SockJsConnection;
import org.projectodd.sockjs.SockJsServer;
import org.projectodd.sockjs.servlet.SockJsServlet;

import javax.servlet.ServletException;

/**
 * SockJS-based listener for DDP
 */
public class DdpSockJsServlet extends SockJsServlet {

  private static final long serialVersionUID = 1L;

  final DdpDataSource dataSource;

  public DdpSockJsServlet(DdpDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void init() throws ServletException {
    SockJsServer sockJsServer = new SockJsServer();

    // // Various options can be set on the server, such as:
    // echoServer.options.responseLimit = 4 * 1024;

    // onConnection is the main entry point for handling SockJS connections
    sockJsServer.onConnection(new SockJsServer.OnConnectionHandler() {
      @Override
      public void handle(final SockJsConnection connection) {
        final DdpSession ddpSession = new DdpSession(dataSource, new DdpConnection() {

          @Override
          public void sendMessage(String json) throws IOException {
            synchronized (this) {
              if (!connection.write(json)) {
                throw new IOException("Error writing message");
              }
            }
          }

          @Override
          public void close() {
            connection.close();
          }
        });

        getServletContext().log("SockJS client connected");

        // onData gets called when a client sends data to the server
        connection.onData(new SockJsConnection.OnDataHandler() {
          @Override
          public void handle(String message) {
            ddpSession.gotMessage(message);
          }
        });

        // onClose gets called when a client disconnects
        connection.onClose(new SockJsConnection.OnCloseHandler() {
          @Override
          public void handle() {
            ddpSession.onClose();
          }
        });
      }
    });

    setServer(sockJsServer);
    // Don't forget to call super.init() to wire everything up
    super.init();
  }
}