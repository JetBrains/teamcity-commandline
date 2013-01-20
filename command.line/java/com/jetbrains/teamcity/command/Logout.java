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

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import com.jetbrains.teamcity.resources.TCAccess;
import java.text.MessageFormat;
import javax.naming.directory.InvalidAttributesException;
import jetbrains.buildServer.core.runtime.IProgressMonitor;

public class Logout implements ICommand {

  private static final String ID = Messages.getString("Logout.command.id"); //$NON-NLS-1$

  private String myResultDescription;

  public void execute(Server server, Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException {
    final String url = args.getArgument(CommandRunner.HOST_ARG);
    TCAccess.getInstance().removeCredential(url);
    myResultDescription = MessageFormat.format(Messages.getString("Logout.result.ok.pattern"), url); //$NON-NLS-1$
    return;
  }

  public void validate(Args args) throws IllegalArgumentException {
    if (!args.hasArgument(CommandRunner.HOST_ARG)) {
      throw new IllegalArgumentException(MessageFormat.format("missing {0}", CommandRunner.HOST_ARG));
    }
  }

  public String getCommandDescription() {
    return Messages.getString("Logout.help.description"); //$NON-NLS-1$
  }

  public String getId() {
    return ID;
  }

  public String getUsageDescription() {
    return MessageFormat.format(Messages.getString("Logout.help.usage.pattern"), getCommandDescription(), getId(), CommandRunner.HOST_ARG); //$NON-NLS-1$
  }

  public boolean isConnectionRequired(final Args args) {
    return false;
  }

  public String getResultDescription() {
    return myResultDescription;
  }

}
