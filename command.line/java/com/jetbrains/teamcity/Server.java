
package com.jetbrains.teamcity;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.*;
import jetbrains.buildServer.core.runtime.IProgressMonitor;
import jetbrains.buildServer.core.runtime.IProgressStatus;
import jetbrains.buildServer.core.runtime.ProgressStatus;
import jetbrains.buildServer.serverProxy.RemoteServerFacade;
import jetbrains.buildServer.serverProxy.RemoteServerFacadeImpl;
import jetbrains.buildServer.serverProxy.SessionXmlRpcTarget;
import jetbrains.buildServer.serverProxy.impl.SessionXmlRpcTargetImpl;
import jetbrains.buildServer.serverSide.TriggeredByBuilder;
import jetbrains.buildServer.serverSide.auth.AuthenticationFailedException;
import jetbrains.buildServer.serverSide.userChanges.PreTestedCommitType;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.version.ServerVersionHolder;
import jetbrains.buildServer.xmlrpc.RemoteCallException;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget.Cancelable;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.jetbrains.annotations.NotNull;

public class Server {

  public static final String UPLOAD_URL = "uploadChanges.html";
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
      mySession = new SessionXmlRpcTargetImpl(myUrl.toExternalForm(), getUserAgent(), timeout);
      Debug.getInstance().debug(Server.class, String.format("XmlRpc session %s created. Timeout set to %s", mySession.describeMe(), timeout));
    } catch (Throwable e) {
      throw new ECommunicationException(String.format("Could not connect to server %s", myUrl), Util.getRootCause(e));
    }
  }

  @NotNull
  private static String getUserAgent() {
    final String version = ServerVersionHolder.getVersion().getDisplayVersion();
    return "Command Line Tool/" + version;
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

  public long createChangeList(@NotNull final File patchFile, @NotNull final String comment, @NotNull final IProgressMonitor monitor) throws ECommunicationException {

    HttpConnection connection = null;
    try {
      monitor.beginTask("Sending patch to TeamCity Server");
      connection = getHttpConnection();

      final PostMethod postMethod = new PostMethod(createUploadPatchUrl());
      final BufferedInputStream content = new BufferedInputStream(new FileInputStream(patchFile));

      try {
        addAuthorizationHeader(postMethod);
        postMethod.setRequestHeader("Connection", "close");
        postMethod.setRequestHeader("Accept", "text/plain");
        postMethod.addRequestHeader("User-Agent", mySession.getUserAgent());

        postMethod.setRequestEntity(new InputStreamRequestEntity(content, patchFile.length()));
        postMethod.setQueryString(new NameValuePair[] { new NameValuePair("userId", String.valueOf(getCurrentUser())),
          new NameValuePair("description", comment),
          new NameValuePair("date", String.valueOf(System.currentTimeMillis())),
          new NameValuePair("commitType", String.valueOf(PreTestedCommitType.NONE.getId())), });
        postMethod.execute(new HttpState(), connection);
      } finally {
        content.close();
      }

      if (postMethod.getStatusCode() >= 400) {
        throw new ECommunicationException("Error creating change list on server with /" + UPLOAD_URL + ": " + postMethod.getResponseBodyAsString() +
                                          "; take a look at TeamCity/logs/teamcity-server.log file for details. HTTP Status code: " + postMethod.getStatusCode());
      }

      // post requests to queue
      final String response = postMethod.getResponseBodyAsString();

      monitor.status(new ProgressStatus(IProgressStatus.INFO, String.format("sent %d bytes", patchFile.length())));
      monitor.done();
      return Long.parseLong(response);

    } catch (IOException e) {
      throw new ECommunicationException(e);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
  }

  @NotNull
  private String createUploadPatchUrl() {
    String result = getURL();
    if (!result.endsWith("/")) {
      result += "/";
    }
    result += UPLOAD_URL;
    return result;
  }

  @NotNull
  private HttpConnection getHttpConnection() throws IOException {
    final SimpleHttpConnectionManager manager = new SimpleHttpConnectionManager(true);
    HostConfiguration hostConfiguration = new HostConfiguration();
    hostConfiguration.setHost(new URI(getURL(), false));
    HttpConnection connection = manager.getConnection(hostConfiguration);

    if (!connection.isOpen()) {
      connection.open();
    }
    return connection;
  }

  private void addAuthorizationHeader(@NotNull PostMethod method) {
    final String crePair = mySession.getUsername() + ":" + mySession.getPassword();
    try {
      String encoded = Base64.getEncoder().encodeToString(crePair.getBytes("US-ASCII")); // we expect ASCII login name and password here
      method.addRequestHeader("Authorization", "Basic " + encoded);
    } catch (UnsupportedEncodingException e) {
      ExceptionUtil.rethrowAsRuntimeException(e);
    }
  }



  public void dispose() {
    mySession.dispose();
  }
}