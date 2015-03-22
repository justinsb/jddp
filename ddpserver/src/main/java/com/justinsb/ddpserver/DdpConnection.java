package com.justinsb.ddpserver;

import java.io.IOException;

/**
 * An abstract DDP connection (SockJS or WebSockets)
 *
 */
public interface DdpConnection {

  void close() throws IOException;

  void sendMessage(String json) throws IOException;

}
