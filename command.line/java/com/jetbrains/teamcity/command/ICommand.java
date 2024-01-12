
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