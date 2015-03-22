package com.justinsb.ddpserver;


public class DdpMethodContext {
  final DdpSession session;
  final String methodId;
  final String method;

  public DdpMethodContext(DdpSession session, String methodId, String method) {
    this.session = session;
    this.methodId = methodId;
    this.method = method;
  }

  public DdpSession getSession() {
    return session;
  }

  public String getMethodId() {
    return methodId;
  }

  public String getMethod() {
    return method;
  }

}
