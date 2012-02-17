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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.naming.directory.InvalidAttributesException;

import jetbrains.buildServer.core.runtime.IProgressMonitor;

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.Util;
import com.jetbrains.teamcity.resources.TCAccess;

public class Login implements ICommand {

  private static final String ID = Messages.getString("Login.command.id"); //$NON-NLS-1$

  private String myResultDescription;

  public void execute(Server nullServer, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
    final String url = args.getArgument(CommandRunner.HOST_ARG);
    final String user = getUser(args);
    final String password = getPassword(args);
    // try to login
    try {
      final Server server = new Server(new URL(url));
      monitor.beginTask(MessageFormat.format(Messages.getString("CommandRunner.connecting.step.name"), url)); //$NON-NLS-1$
      server.connect();
      monitor.done();
      monitor.beginTask(Messages.getString("CommandRunner.logging.step.name")); //$NON-NLS-1$
      server.logon(user, password);
      monitor.done();
      // ok. let's store
      TCAccess.getInstance().setCredential(url, user, password);
      myResultDescription = MessageFormat.format(Messages.getString("Login.result.ok.pattern"), user); //$NON-NLS-1$

    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public void validate(Args args) throws IllegalArgumentException {
    if (!args.hasArgument(CommandRunner.HOST_ARG)) {
      throw new IllegalArgumentException(MessageFormat.format("missing {0}", CommandRunner.HOST_ARG));
    }
  }

  private String getPassword(Args args) {
    if (args.hasArgument(CommandRunner.PASSWORD_ARG)) {
      return args.getArgument(CommandRunner.PASSWORD_ARG);
    } else {
      return Util.readConsoleInput(Messages.getString("Login.password.prompt"), true); //$NON-NLS-1$
    }
  }

  private String getUser(Args args) {
    if (args.hasArgument(CommandRunner.USER_ARG)) {
      return args.getArgument(CommandRunner.USER_ARG);
    } else {
      return Util.readConsoleInput(Messages.getString("Login.username.prompt"), false); //$NON-NLS-1$
    }
  }

  public String getCommandDescription() {
    return Messages.getString("Login.help.description"); //$NON-NLS-1$
  }

  public String getId() {
    return ID;
  }

  public String getUsageDescription() {
    return MessageFormat.format(Messages.getString("Login.help.usage.pattern"), //$NON-NLS-1$
        getCommandDescription(), getId(), CommandRunner.HOST_ARG, CommandRunner.USER_ARG, CommandRunner.PASSWORD_ARG);
  }

  public boolean isConnectionRequired(final Args args) {
    return false;
  }

  public String getResultDescription() {
    return myResultDescription;
  }

}
