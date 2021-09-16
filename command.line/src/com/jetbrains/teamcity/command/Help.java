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
package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.*;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.TreeSet;
import javax.naming.directory.InvalidAttributesException;
import jetbrains.buildServer.core.runtime.IProgressMonitor;

class Help implements ICommand {

  static final String ID = Messages.getString("Help.command.id"); //$NON-NLS-1$
  private String myResultDescription;

  public void execute(Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {

    final StringBuffer buffer = new StringBuffer();

    buffer.append(MessageFormat.format(Messages.getString("Help.command.header"), //$NON-NLS-1$
        Build.build));

    final String commandId = args != null && args.getArguments() != null && args.getArguments().length > 0 ? args.getArguments()[0] : null;
    if (args != null && args.getCommandId() != null && args.getCommandId().equals(ID) && commandId != null && !commandId.equals(ID)) {// help
                                                                                                                                      // command
                                                                                                                                      // used
      buffer.append(printDescription(commandId));
      myResultDescription = buffer.toString();
      return;

    }
    if (args != null && args.getCommandId() != null && !args.getCommandId().equals(ID)) { // no
                                                                                          // help
                                                                                          // command
                                                                                          // used
      buffer.append(printDescription(args.getCommandId()));
      myResultDescription = buffer.toString();

    } else {// nothing passed
      buffer.append(printDefault());
      myResultDescription = buffer.toString();
    }

  }

  private String printDefault() {
    final StringBuffer buffer = new StringBuffer();
    buffer.append(Messages.getString("Help.tool.usage.description")); //$NON-NLS-1$
    // print command list
    buffer.append(Messages.getString("Help.available.commands.list.header")); //$NON-NLS-1$
    final TreeSet<ICommand> knownCommands = new TreeSet<ICommand>(new Comparator<ICommand>() {
      public int compare(ICommand o1, ICommand o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });

    knownCommands.addAll(CommandRegistry.getInstance().commands());
    for (final ICommand command : knownCommands) {
      buffer.append(MessageFormat.format(Messages.getString("Help.available.commands.list.pattern"), prepare(String.valueOf(command.getId())), String.valueOf(command.getCommandDescription()))); //$NON-NLS-1$
    }
    buffer.append(Messages.getString("Help.command.usage.text")); //$NON-NLS-1$
    buffer.append(printGlobalOptions());
    return buffer.toString();
  }

  private static String prepare(final String s) {
    return s.length() < 4 ? s + "\t" : s;
  }

  private String printGlobalOptions() {
    final String globalOptions = Messages.getString("Help.global.options.header"); //$NON-NLS-1$
    return MessageFormat.format(globalOptions, CommandRunner.HOST_ARG, CommandRunner.USER_ARG, CommandRunner.PASSWORD_ARG, Args.DEBUG_ARG, Args.DEBUG_CLEAN_OFF);
  }

  private String printDescription(final String commandId) {
    StringBuffer buffer = new StringBuffer();
    final ICommand command = CommandRegistry.getInstance().getCommand(commandId);
    if (command != null && command.getId().equals(commandId)) {
      buffer.append(command.getUsageDescription());
      buffer.append(printGlobalOptions());
      return buffer.toString();
    } else {
      buffer.append(MessageFormat.format(Messages.getString("Help.no.one.registered.command.found.message"), commandId)); //$NON-NLS-1$
      buffer.append(printDefault());
      return buffer.toString();
    }
  }

  public String getId() {
    return ID;
  }

  public boolean isConnectionRequired(final Args args) {
    return false;
  }

  public String getUsageDescription() {
    return getCommandDescription();
  }

  public String getCommandDescription() {
    return Messages.getString("Help.command.description"); //$NON-NLS-1$
  }

  public String getResultDescription() {
    return myResultDescription;
  }

  public void validate(Args args) throws IllegalArgumentException {
    // TODO Auto-generated method stub

  }

}
