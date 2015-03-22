package com.justinsb.ddpserver;

import java.io.IOException;
import java.util.concurrent.ConcurrentMap;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * A direct WebSocket implementation of DDP; without SockJS
 * 
 * SockJS can use WebSockets (and does); this implementation is used by server-to-server links
 * 
 * It isn't that different from the SockJS implementation; in Meteor itself the /websocket endpoint just redirects to
 * SockJS's websocket endpoint (/sockjs/websocket)
 *
 */
public class DdpWebsocketEndpoint extends Endpoint {
  static final Logger log = LoggerFactory.getLogger(DdpWebsocketEndpoint.class);

  final DdpDataSource dataSource;

  final ConcurrentMap<String, DdpSession> sessions = Maps.newConcurrentMap();

  public DdpWebsocketEndpoint(DdpDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void onOpen(final Session session, EndpointConfig config) {
    DdpConnection connection = new DdpConnection() {

      @Override
      public void sendMessage(String json) throws IOException {
        synchronized (this) {
          // if (!connection.write(json)) {
          // throw new IOException("Error writing message");
          // }
          // try {
          session.getBasicRemote().sendText(json);
          // } catch (IOException ex) {
          // log.log(Level.WARNING, "Error sending raw websocket data", ex);
          // }
        }
      }

      @Override
      public void close() throws IOException {
        session.close();
      }
    };

    final DdpSession ddpSession = new DdpSession(dataSource, connection);

    session.addMessageHandler(new MessageHandler.Whole<String>() {
      @Override
      public void onMessage(String message) {
        ddpSession.gotMessage(message);
      }
    });

    sessions.put(session.getId(), ddpSession);
  }

  @Override
  public void onClose(Session session, CloseReason closeReason) {
    String sessionId = session.getId();
    DdpSession ddpSession = sessions.remove(sessionId);
    if (ddpSession == null) {
      log.warn("onClose for non-existent session: {}", sessionId);
      return;
    }

    ddpSession.onClose();
  }

  @Override
  public void onError(Session session, Throwable thr) {
    String sessionId = session.getId();
    DdpSession ddpSession = sessions.get(sessionId);
    if (ddpSession == null) {
      log.warn("onError for non-existent session: {}", sessionId);
      return;
    }

    ddpSession.unexpectedError(thr);
  }

}
