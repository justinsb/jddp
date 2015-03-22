package com.justinsb.ddpserver;

public class MeteorError extends Exception {

  private static final long serialVersionUID = 1L;

  final int error;
  final String reason;

  public MeteorError(int error, String reason) {
    this.error = error;
    this.reason = reason;
  }

  public int getCode() {
    return error;
  }

  public String getReason() {
    return reason;
  }

}
