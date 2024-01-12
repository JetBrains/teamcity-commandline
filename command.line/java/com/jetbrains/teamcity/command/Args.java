
package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class Args {

  public static final String DEBUG_ARG = Messages.getString("Args.debug.switch.name"); //$NON-NLS-1$

  public static final String SILENT_ARG = Messages.getString("Args.silent.switch.name"); //$NON-NLS-1$  

  public static final String DEBUG_CLEAN_OFF = Messages.getString("Args.do.not.delete.file.after.run.switch.name"); //$NON-NLS-1$

  static HashMap<String, Pattern> ourRegisteredArgs = new HashMap<String, Pattern>();

  private final String[] myArgs;

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
        match |= pattern.matcher(myArgsLine).find();
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

  /**
   * Return list of values for arguments which support them, like --param name1=value --param name2=value2
   * In the case above, array of "name1=value","name2=value2" will be returned
   * @param argName
   * @return see above
   */
  public List<String> getArgValues(@NotNull String argName) {
    return Util.getArgumentValues(myArgs, argName);
  }
}