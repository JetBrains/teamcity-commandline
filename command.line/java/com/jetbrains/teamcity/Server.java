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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.*;
import jetbrains.buildServer.serverProxy.RemoteServerFacade;
import jetbrains.buildServer.serverProxy.RemoteServerFacadeImpl;
import jetbrains.buildServer.serverProxy.SessionXmlRpcTarget;
import jetbrains.buildServer.serverProxy.impl.SessionXmlRpcTargetImpl;
import jetbrains.buildServer.serverSide.TriggeredByBuilder;
import jetbrains.buildServer.serverSide.auth.AuthenticationFailedException;
import jetbrains.buildServer.xmlrpc.RemoteCallException;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget.Cancelable;
import org.jetbrains.annotations.NotNull;

public class Server {

  private final URL myUrl;
  private SessionXmlRpcTarget mySession;
  private RemoteServerFacade myServerFacade;
  private List<ProjectData> myProjects;

  public Server(final URL url) {
    myUrl = url;
  }

  public void connect() throws ECommunicationException {
    try {
      final int timeout = getTimeout();
      mySession = new SessionXmlRpcTargetImpl(myUrl.toExternalForm(), "Command Line Tool", timeout);
      Debug.getInstance().debug(Server.class, String.format("XmlRpc session %s created. Timeout set to %s", mySession.describeMe(), timeout));
    } catch (Throwable e) {
      throw new ECommunicationException(String.format("Could not connect to server %s", myUrl), Util.getRootCause(e));
    }
  }

  private int getTimeout() {
    final String timeoutStr = System.getProperty(Constants.XMLRPC_TIMEOUT_SYSTEM_PROPERTY);
    if (timeoutStr != null) {
      try {
        final int timeout = new Integer(timeoutStr.trim());
        if (timeout > 0) {
          return timeout;
        }
      } catch (Throwable t) {
        Debug.getInstance().error(Server.class, "Could not parse timeout", t);
      }
    }
    return Constants.DEFAULT_XMLRPC_TIMEOUT;
  }

  public void logon(final String username, final String password) throws ECommunicationException, EAuthorizationException {
    mySession.setCredentials(username, password);
    try {
      mySession.authenticate(new Cancelable() {

        public long sleepingPeriod() {
          return 0;
        }

        public boolean isCanceled() {
          return false;
        }
      });
    } catch (AuthenticationFailedException e) {
      throw new EAuthorizationException(Util.getRootCause(e));
    } catch (RemoteCallException e) {
      throw new ECommunicationException(Util.getRootCause(e));
    }
  }

  private RemoteServerFacade getServerFacade() {
    if (myServerFacade == null) {
      myServerFacade = new RemoteServerFacadeImpl(mySession);
    }
    return myServerFacade;
  }

  public int getCurrentUser() {
    final Integer userId = mySession.getUserId();
    return userId != null ? userId : -1;
  }

  @SuppressWarnings("rawtypes")
  public synchronized Collection<ProjectData> getProjects() throws ECommunicationException {
    if (myProjects == null) {
      myProjects = getServerFacade().getRegisteredProjects();
    }
    return myProjects;
  }

  public synchronized Collection<BuildTypeData> getConfigurations() throws ECommunicationException {
    final Collection<ProjectData> allProjects = getProjects();
    final ArrayList<BuildTypeData> configurations = new ArrayList<BuildTypeData>(allProjects.size() * 5);
    for (ProjectData project : allProjects) {
      configurations.addAll(project.getBuildTypes());
    }
    return configurations;
  }

  public TeamServerSummaryData getSummary() throws ECommunicationException {
    return getServerFacade().getSummaryData(String.valueOf(getCurrentUser()));
  }

  public String getURL() {
    return mySession.getServerURL();
  }

  @NotNull
  public AddToQueueResult addRemoteRunToQueue(@NotNull List<AddToQueueRequest> batch) throws ECommunicationException {
    final TriggeredByBuilder builder = new TriggeredByBuilder();
    builder.addParameter(TriggeredByBuilder.USER_PARAM_NAME, String.valueOf(getCurrentUser()));
    builder.addParameter(TriggeredByBuilder.IDE_PLUGIN_PARAM_NAME, "Command line remote run");
    builder.addParameter(TriggeredByBuilder.TYPE_PARAM_NAME, "commandLineRemoteRun");
    return getServerFacade().addToQueue(batch, builder.toString());
  }

  public Collection<String> getApplicableConfigurations(final Collection<String> urls) throws ECommunicationException {
    return getServerFacade().getSuitableConfigurations(urls);
  }

  public void dispose() {
    mySession.dispose();
  }
}
