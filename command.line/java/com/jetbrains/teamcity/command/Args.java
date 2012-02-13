/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class Args {

  public static final String DEBUG_ARG = Messages.getString("Args.debug.switch.name"); //$NON-NLS-1$

  public static final String SILENT_ARG = Messages.getString("Args.silent.switch.name"); //$NON-NLS-1$  

  public static final String DEBUG_CLEAN_OFF = Messages.getString("Args.do.not.delete.file.after.run.switch.name"); //$NON-NLS-1$

  static HashMap<String, Pattern> ourRegisteredArgs = new HashMap<String, Pattern>();

  private String[] myArgs;

  private String myArgsLine = "";

  private String myCommandId;

  private boolean isDebugOn;

  private boolean isSilentOn;

  private boolean isCleanOff;

  public static void registerArgument(final String argName, final String argPattern) {
    final Pattern pattern = Pattern.compile(argPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);
    ourRegisteredArgs.put(argName, pattern);
  }

  public Args(final String argline) {
    this(argline.split("\\s+"));
  }

  public Args(final String[] args) {
    if (args == null || args.length == 0) {
      myArgs = new String[] { Help.ID };
      return;
    }
    final LinkedList<String> list = new LinkedList<String>(Arrays.asList(args));
    // extract CommandId
    myCommandId = list.get(0);
    list.remove(0);
    // remove -debug argument
    if (list.contains(DEBUG_ARG)) {
      list.remove(DEBUG_ARG);
      isDebugOn = true;
    }
    // remove -debug-clean-off argument
    if (list.contains(DEBUG_CLEAN_OFF)) {
      list.remove(DEBUG_CLEAN_OFF);
      isCleanOff = true;
    }
    // remove -silent
    if (list.contains(SILENT_ARG)) {
      list.remove(SILENT_ARG);
      isSilentOn = true;
    }
    myArgs = list.toArray(new String[list.size()]);
    for (String arg : myArgs) {
      myArgsLine += arg + " ";
    }
    myArgsLine = myArgsLine.trim();
  }

  public String getCommandId() {
    return myCommandId;
  }

  public boolean hasArgument(final String... arguments) {
    boolean match = false;
    boolean registeredFound = false;
    for (final String arg : arguments) {
      if (ourRegisteredArgs.containsKey(arg)) {
        registeredFound = true;
        final Pattern pattern = ourRegisteredArgs.get(arg);
        match |= pattern.matcher(myArgsLine).matches();
      }
    }
    if (registeredFound) {
      return match;
    }

    return Util.hasArgument(myArgs, arguments);
  }

  public String getArgument(final String... arguments) {
    return Util.getArgumentValue(myArgs, arguments);
  }

  public String[] getArguments() {
    return myArgs;
  }

  public String getLastArgument() {
    if (myArgs != null && myArgs.length > 0) {
      return myArgs[myArgs.length - 1];
    }
    return null;
  }

  @Override
  public String toString() {
    return String.format("", myCommandId, Arrays.toString(myArgs)); //$NON-NLS-1$
  }

  public boolean isDebugOn() {
    return isDebugOn;
  }

  public boolean isCleanOff() {
    return isCleanOff;
  }

  public boolean isSilentOn() {
    return isSilentOn;
  }

}
