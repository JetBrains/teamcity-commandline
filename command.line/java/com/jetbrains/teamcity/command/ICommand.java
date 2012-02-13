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

import com.jetbrains.teamcity.EAuthorizationException;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.ERemoteError;
import com.jetbrains.teamcity.Server;
import javax.naming.directory.InvalidAttributesException;
import jetbrains.buildServer.core.runtime.IProgressMonitor;

public interface ICommand {

  public String getId();

  public void execute(final Server server, final Args args, final IProgressMonitor monitor) throws EAuthorizationException, ECommunicationException, ERemoteError, InvalidAttributesException;

  public boolean isConnectionRequired(final Args args);

  /**
   * @return The Command usage description. Uses for help
   */
  public String getUsageDescription();

  /**
   * @return Short Command purpose description. Uses for help
   */
  public String getCommandDescription();

  /**
   * @return Result of the Command execution. Uses for result printing
   */
  public String getResultDescription();

  /**
   * @throws IllegalArgumentException
   *           if passed args is wrong
   */
  public void validate(final Args args) throws IllegalArgumentException;

}
