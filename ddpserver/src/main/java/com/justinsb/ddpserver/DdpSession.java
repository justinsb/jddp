package com.justinsb.ddpserver;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * A single connection to the DDP server
 *
 */
public class DdpSession {

  private static final Logger log = LoggerFactory.getLogger(DdpSession.class);

  final DdpConnection connection;
  final Map<String, DdpSubscription> subscriptions = Maps.newHashMap();

  final DdpDataSource dataSource;
  final DdpMergeBox mergeBox;

  final Map<Object, Object> state = Maps.newHashMap();

  public DdpSession(DdpDataSource dataSource, DdpConnection connection) {
    this.dataSource = dataSource;

    this.connection = connection;

    this.mergeBox = new DdpMergeBox(this);
  }

  public void gotMessage(final String message) {
    log.info("Got message: {}", message);

    try {
      JsonObject json = (JsonObject) new JsonParser().parse(message);
      String msg = json.get("msg").getAsString();
      if (msg.equals("connect")) {
        onConnect(json);
      } else if (msg.equals("ping")) {
        onPing(json);
      } else if (msg.equals("sub")) {
        onSubscribe(json);
      } else if (msg.equals("unsub")) {
        onUnsubscribe(json);
      } else if (msg.equals("method")) {
        onMethod(json);
      } else {
        throw new IllegalArgumentException("Unknown message: " + msg);
      }
    } catch (Exception e) {
      log.warn("Error processing message ({})", message, e);
      unexpectedError(e);
    }
  }

  private void onSubscribe(JsonObject json) throws Exception {
    String subscriptionId = json.get("id").getAsString();
    String name = json.get("name").getAsString();
    JsonArray params = json.get("params").getAsJsonArray();

    DdpPublish publishFunction = this.dataSource.getPublishFunction(this, name, params);
    if (publishFunction == null) {
      sendNosub(subscriptionId, new MeteorError(404, "Subscription not found"));
      return;
    }

    DdpPublishContext context = new DdpPublishContext(this, subscriptionId, name);
    DdpSubscription ddpSubscription;

    try {
      ddpSubscription = publishFunction.subscribe(context, name, params);
    } catch (Exception e) {
      sendNosub(subscriptionId, e);
      return;
    }

    addSubscription(ddpSubscription);
  }

  private void sendNosub(String subscriptionId, Exception e) {
    JsonObject nosub = new JsonObject();
    nosub.addProperty("msg", "nosub");
    nosub.addProperty("id", subscriptionId);
    nosub.add("error", DdpJson.buildError(e));
    try {
      sendMessage(nosub);
    } catch (IOException e1) {
      this.unexpectedError(e1);
    }
  }

  public void addSubscription(DdpSubscription subscription) throws Exception {
    subscriptions.put(subscription.subscriptionId, subscription);

    subscription.begin();
  }

  private void onUnsubscribe(JsonObject json) throws IOException {
    String id = json.get("id").getAsString();
    DdpSubscription subscription = getSubscription(id);
    if (subscription == null) {
      // XXX: Subscription not found?
      throw new IllegalArgumentException();
    }

    subscription.end();
  }

  private void onMethod(final JsonObject json) throws IOException {
    // {"msg":"method","method":"/lists/insert","params":[{"name":"Meteor Principles","incompleteCount":7}],"id":"1"}

    String methodId = json.get("id").getAsString();
    String method = json.get("method").getAsString();
    JsonArray params = json.get("params").getAsJsonArray();

    JsonObject response = new JsonObject();
    response.addProperty("id", methodId);
    response.addProperty("msg", "result");

    try {
      DdpMethodResult result = dataSource.executeMethod(this, methodId, method, params);
      Futures.addCallback(result.getCompletion(), new FutureCallback<Boolean>() {

        @Override
        public void onSuccess(Boolean result) {
          sendComplete(methodId);
        }

        @Override
        public void onFailure(Throwable t) {
          sendComplete(methodId);
        }

      });

      if (result.getResult() != null) {
        response.add("result", result.getResult());
      }
    } catch (Exception e) {
      sendComplete(methodId);
      log.warn("Got error from method", e);
      JsonObject error = DdpJson.buildError(e);
      response.add("error", error);
    }

    sendMessage(response);
  }

  protected void sendComplete(String methodId) {
    JsonObject msg = new JsonObject();
    msg.addProperty("msg", "updated");
    JsonArray methods = new JsonArray();
    methods.add(new JsonPrimitive(methodId));
    msg.add("methods", methods);
    try {
      sendMessage(msg);
    } catch (IOException e) {
      unexpectedError(e);
    }
  }

  private DdpSubscription getSubscription(String id) {
    return subscriptions.get(id);
  }

  private void onConnect(JsonObject connect) throws IOException {
    String session = UUID.randomUUID().toString();
    JsonObject connected = new JsonObject();
    connected.addProperty("msg", "connected");
    connected.addProperty("session", session);
    sendMessage(connected);
  }

  private void onPing(JsonObject ping) throws IOException {
    JsonObject pong = new JsonObject();
    pong.addProperty("msg", "pong");

    JsonElement id = ping.get("id");
    if (id != null) {
      pong.addProperty("id", id.getAsString());
    }
    sendMessage(pong);
  }

  public void sendMessage(JsonObject msg) throws IOException {
    String json = msg.toString();
    log.info("Sending: {}", json);
    connection.sendMessage(json);
  }

  public void notifyMethodSync(List<String> methodIds) throws IOException {
    JsonObject updated = new JsonObject();
    updated.addProperty("msg", "updated");

    JsonArray methods = new JsonArray();
    for (String methodId : methodIds) {
      methods.add(new JsonPrimitive(methodId));
    }
    updated.add("methods", methods);
    sendMessage(updated);
  }

  public void unexpectedError(Throwable t) {
    log.error("Unexpected error", t);
    try {
      connection.close();
    } catch (IOException e) {
      log.warn("Ignoring error closing connection (in response to unexpected error)", e);
    }
  }

  public void onClose() {
    log.info("Connection closed");

    for (DdpSubscription subscription : subscriptions.values()) {
      subscription.disconnected();
    }

    // Aggressively free the merge-box data
    this.mergeBox.disconnected();
  }

  public <T> T getState(Class<T> clazz) {
    T v = (T) this.state.get(clazz);
    return v;
  }

  public <T> void setState(Class<T> clazz, T instance) {
    this.state.put(clazz, instance);
  }

  public void recalculateSubscriptions() {
    log.info("Recalculating subscriptions");

    for (DdpSubscription subscription : subscriptions.values()) {
      try {
        subscription.recalculate();
      } catch (Exception e) {
        log.warn("Error recalculating subscription {}", subscription, e);
      }
    }

  }

}
