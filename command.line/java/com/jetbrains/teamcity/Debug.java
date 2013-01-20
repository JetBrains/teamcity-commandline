/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.teamcity;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

public class Debug {

  private static Map<String, Logger> ourLoggers = new HashMap<String, Logger>();

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
    internalLog(clazz, message, System.err);
  }

  public void error(final Class<?> clazz, final String message) {
    internalLog(clazz, message, System.err);
  }

  public void error(final Class<?> clazz, final String message, final Throwable t) {
    internalThrowable(clazz, message, t, System.err);
  }

  private void internalLog(final Class<?> clazz, final String message, PrintStream dest) {
    final Logger logger = getLogger(clazz);
    logger.debug(message);
    if (debugMode) {
      dest.println(String.format("%s: %s", clazz.getSimpleName(), message));
    }
  }

  private void internalThrowable(final Class<?> clazz, final String message, Throwable t, PrintStream dest) {
    final Logger logger = getLogger(clazz);
    logger.debug(message, t);
    if (debugMode) {
      dest.println(String.format("%s: %s", clazz.getSimpleName(), message));
      if (t != null) {
        t.printStackTrace(dest);
      }
    }
  }

  private Logger getLogger(Class<?> clazz) {
    final String key = clazz.getName();
    if (ourLoggers.containsKey(key)) {
      return ourLoggers.get(key);
    }
    final Logger logger = Logger.getLogger(clazz);
    ourLoggers.put(key, logger);
    return logger;
  }

}
