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
