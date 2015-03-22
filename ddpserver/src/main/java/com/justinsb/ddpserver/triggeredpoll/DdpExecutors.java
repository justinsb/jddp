package com.justinsb.ddpserver.triggeredpoll;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DdpExecutors {

  public static final Executor DEFAULT_EXECUTOR = Executors.newCachedThreadPool();

}
