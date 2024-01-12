
package com.jetbrains.teamcity;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class Debug {

  private boolean debugMode = false;

  private static Debug ourInstance;

  public static Debug getInstance() {
    if (ourInstance == null) {
      ourInstance = new Debug();
    }
    return ourInstance;
  }

  private Debug() {
  }

  public void setDebug(boolean on) {
    debugMode = on;
  }

  public void debug(final Class<?> clazz, final String message) {
    internalLog(clazz, message, null, false);
  }

  public void error(final Class<?> clazz, final String message, final Throwable t) {
    internalLog(clazz, message, t, true);
  }

  private void internalLog(final Class<?> clazz, final String message, @Nullable Throwable t, final boolean forceStderr) {
    Logger.getLogger(clazz).debug(message, t);
    if (debugMode || forceStderr) {
      System.err.println(String.format("%s: %s", clazz.getSimpleName(), message));
      if (t != null) {
        t.printStackTrace(System.err);
      }
    }
  }
}