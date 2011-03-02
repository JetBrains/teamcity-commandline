/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.jetbrains.teamcity.runtime;

import java.io.PrintStream;

public class ConsoleProgressMonitor implements IProgressMonitor {

  private static final String SPACE = " ";
  private static final String DONE = "done";

  private PrintStream myOut;
  private Thread myCurrentTicker;

  public ConsoleProgressMonitor(PrintStream out) {
    myOut = out;
  }

  public IProgressMonitor beginTask(String name) {
    if (name != null && name.trim().length() > 0) {
      myOut.append(name).append("...");
      stopTicker();
      myCurrentTicker = new Thread(new Ticker(myOut), "Console Progress Monitor");
      myCurrentTicker.start();
    }
    return this;
  }

  private void stopTicker() {
    if (myCurrentTicker != null && myCurrentTicker.isAlive()) {
      myCurrentTicker.interrupt();
    }
  }

  public IProgressMonitor done() {
    stopTicker();
    myOut.print(SPACE);
    myOut.println(DONE);
    return this;
  }

  public IProgressMonitor done(String message) {
    if (message == null) {
      done();
    } else {
      stopTicker();
      myOut.print(SPACE);
      myOut.println(message);
    }
    return this;
  }

  public IProgressMonitor worked(final String step) {
    if (step != null) {
      myOut.print(step);
    }
    return this;
  }

  class Ticker implements Runnable {

    public Ticker(PrintStream out) {
      myOut = out;
    }

    public void run() {
      while (true) {
        try {
          Thread.sleep(1000);
          myOut.print(".");
        } catch (InterruptedException e) {
          return;
        }
      }
    }

  }

}