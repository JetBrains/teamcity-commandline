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
package com.jetbrains.teamcity;

import java.io.IOException;
import java.lang.Object;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.IncompatiblePluginError;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.TeamServerSummaryData;
import jetbrains.buildServer.messages.XStreamHolder;
import jetbrains.buildServer.serverProxy.ApplicationFacade;
import jetbrains.buildServer.serverProxy.ClientXmlRpcExecutorFacade;
import jetbrains.buildServer.serverProxy.RemoteAuthenticationServer;
import jetbrains.buildServer.serverProxy.RemoteBuildServer;
import jetbrains.buildServer.serverProxy.RemoteServerProxy;
import jetbrains.buildServer.serverProxy.SessionXmlRpcTarget;
import jetbrains.buildServer.serverProxy.UserSummaryRemoteManager;
import jetbrains.buildServer.serverProxy.VersionChecker;
import jetbrains.buildServer.serverProxy.impl.SessionXmlRpcTargetImpl;
import jetbrains.buildServer.serverSide.auth.AuthenticationFailedException;
import jetbrains.buildServer.version.ServerVersionHolder;
import jetbrains.buildServer.xmlrpc.RemoteCallException;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget.Cancelable;
import jetbrains.buildServer.xstream.ServerXStreamFormat;
import jetbrains.buildServer.xstream.XStreamWrapper;

import com.thoughtworks.xstream.XStream;

public class Server {

  private URL myUrl;
  private SessionXmlRpcTarget mySession;
  private RemoteBuildServer myServerProxy;
  private ArrayList<ProjectData> myProjects;
  private ClientXmlRpcExecutorFacade myAuthenticationProxy;
  private UserSummaryRemoteManager mySummaryProxy;

  public Server(final URL url) {
    myUrl = url;
  }

  public Server(final URL url, final Proxy proxy) {
    this(url);
    // myProxy = proxy;
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
        final int timeout = new Integer(timeoutStr.trim()).intValue();
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

  private RemoteBuildServer getServerProxy() throws ECommunicationException {
    if (myServerProxy == null) {
      myServerProxy = new RemoteServerProxy(mySession, new ApplicationFacadeStub(), new VersionCheckerStub());
    }
    return myServerProxy;
  }

  private UserSummaryRemoteManager getSummaryProxy() throws ECommunicationException {
    if (mySummaryProxy == null) {
      mySummaryProxy = new SummaryManager(mySession, new ApplicationFacadeStub(), UserSummaryRemoteManager.HANDLER, new VersionCheckerStub());
    }
    return mySummaryProxy;
  }

  private ClientXmlRpcExecutorFacade getAuthenticationProxy() throws ECommunicationException {
    if (myAuthenticationProxy == null) {
      myAuthenticationProxy = new ClientXmlRpcExecutorFacade(mySession, new ApplicationFacadeStub(), RemoteAuthenticationServer.REMOTE_AUTH_SERVER, new VersionCheckerStub());
    }
    return myAuthenticationProxy;
  }

  public String getLocalProtocolVersion() {
    return ServerVersionHolder.getVersion().getPluginProtocolVersion();
  }

  public String getRemoteProtocolVersion() throws ECommunicationException {
    try {
      return getAuthenticationProxy().callXmlRpc("getServerVersion");
    } catch (Exception e) {
      throw new ECommunicationException(e);
      // Debug.getInstance().error(getClass(), e.getMessage(), e);
      // return Constants.UNKNOWN_STRING;
    }
  }

  public void logout() {
    mySession.logout();
  }

  public int getCurrentUser() {
    return mySession.getUserId();
  }

  @SuppressWarnings("rawtypes")
  public synchronized Collection<ProjectData> getProjects() throws ECommunicationException {
    if (myProjects == null) {
      final RemoteBuildServer serverProxy = getServerProxy();
      final Vector builds = serverProxy.getRegisteredProjects(false);
      myProjects = new ArrayList<ProjectData>(builds.size());
      for (Object typeData : builds) {
        final ProjectData projectData = (ProjectData) deserializeObject(typeData);
        myProjects.add(projectData);
      }
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

  public synchronized BuildTypeData getConfiguration(final String id) throws ECommunicationException {
    final Collection<BuildTypeData> allConfigurations = getConfigurations();
    for (final BuildTypeData config : allConfigurations) {
      if (id.equals(config.getId())) {
        return config;
      }
    }
    return null;
  }

  public TeamServerSummaryData getSummary() throws ECommunicationException {
    final byte[] serializedStr = getSummaryProxy().getGZippedSummaryByVcsUris(String.valueOf(getCurrentUser()), new Vector());
    try {
      final TeamServerSummaryData data = unzipAndDeserializeObject(serializedStr);
      return data;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // TODO: move to utils
  private final static XStreamHolder ourXStreamHolder = new XStreamHolder() {
    protected void configureXStream(XStream xStream) {
      ServerXStreamFormat.formatXStream(xStream);
    }
  };

  // TODO: move to utils
  private static <T> T deserializeObject(final Object typeData) {
    return XStreamWrapper.<T> deserializeObject((String) typeData, ourXStreamHolder);
  }

  // TODO: move to utils
  private <T> T unzipAndDeserializeObject(final Object typeData) throws IOException {
    return XStreamWrapper.<T> unzipAndDeserializeObject((byte[]) typeData, ourXStreamHolder);
  }

  static class VersionCheckerStub implements VersionChecker {

    public void checkServerVersion() throws IncompatiblePluginError {
    }
  }

  static class ApplicationFacadeStub implements ApplicationFacade {

    public Cancelable createCancelable() {
      return null;
    }

    public void onProcessCanceled() {
    }

  }

  public String getURL() {
    return mySession.getServerURL();
  }

  public AddToQueueResult addRemoteRunToQueue(ArrayList<AddToQueueRequest> batch, String myComments) throws ECommunicationException {
    return deserializeObject(getServerProxy().addToQueue(XStreamWrapper.serializeObjects(batch, ourXStreamHolder), myComments));
  }

  @SuppressWarnings("rawtypes")
  public Collection<String> getApplicableConfigurations(final Collection<String> urls) {
    final ClientXmlRpcExecutorFacade xmlExecutor = new ClientXmlRpcExecutorFacade(mySession, new ApplicationFacadeStub(), "VersionControlServer", new VersionCheckerStub());
    final Vector ids = xmlExecutor.callXmlRpc("getSuitableConfigurations", new Vector<String>(urls));
    final HashSet<String> buffer = new HashSet<String>(ids.size());
    for (final Object id : ids) {
      buffer.add(String.valueOf(id));
    }
    return buffer;
  }

  class SummaryManager extends ClientXmlRpcExecutorFacade implements UserSummaryRemoteManager {

    public SummaryManager(XmlRpcTarget target, ApplicationFacade applicationFacade, String handlerName, VersionChecker checker) {
      super(target, applicationFacade, handlerName, checker);
    }

    public byte[] getGZippedSummaryByVcsUris(String userId, Vector vcsUris) {
      return callXmlRpc("getGZippedSummaryByVcsUris", userId, vcsUris);
    }

    public byte[] getGZippedSummaryByBuildTypes(String userId, Vector buildTypeIds) {
      return callXmlRpc("getGZippedSummaryByBuildTypes", userId, buildTypeIds);
    }

    public Vector<?> getRunningBuildsStatus() {
      return new Vector<Object>();
    }

    public String getSummary(String userId, boolean specifiedUserChangesOnly) {
      return null;
    }

    public int getTotalNumberOfEvents(String serializedSubscription) {
      return 0;
    }

    public byte[] getFilteredGZippedSummary(String arg0, @SuppressWarnings("rawtypes") Vector arg1) {
      // TODO Auto-generated method stub
      return null;
    }

    @SuppressWarnings("rawtypes")
    public Vector getWatchedRunningBuildsStatus(String arg0, boolean arg1, Vector arg2) {
      // TODO Auto-generated method stub
      return null;
    }

  }

}
