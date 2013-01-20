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
package jetbrains.buildServer.serverProxy;

import com.jetbrains.teamcity.Debug;
import com.jetbrains.teamcity.ECommunicationException;
import com.jetbrains.teamcity.XStreamUtil;
import com.jetbrains.teamcity.command.CommandRunner;
import com.jetbrains.teamcity.command.Messages;
import java.util.*;
import jetbrains.buildServer.*;
import jetbrains.buildServer.version.ServerVersionHolder;
import jetbrains.buildServer.xmlrpc.XmlRpcTarget;
import org.jetbrains.annotations.NotNull;

public class RemoteServerFacadeImpl extends RemoteBuildServerImpl implements RemoteServerFacade {
  private SessionXmlRpcTarget mySession;

  public RemoteServerFacadeImpl(SessionXmlRpcTarget target) {
    super(target, new ApplicationFacadeStub(), new VersionCheckerStub());
    mySession = target;
  }

  @NotNull
  public List<ProjectData> getRegisteredProjects() throws ECommunicationException {
    Vector projects = remoteCall(new ServerCommand<Vector>() {
      public Vector execute() {
        return RemoteServerFacadeImpl.super.getRegisteredProjects(false);
      }

      public String describe() {
        return "getRegisteredProjects";
      }
    });
    List<ProjectData> projectsData = new ArrayList<ProjectData>(projects.size());
    for (Object typeData : projects) {
      final ProjectData projectData = (ProjectData) XStreamUtil.deserializeObject(typeData);
      projectsData.add(projectData);
    }
    return projectsData;
  }

  @NotNull
  public TeamServerSummaryData getSummaryData(final String userId) throws ECommunicationException {
    return remoteCall(new ServerCommand<TeamServerSummaryData>() {
      public TeamServerSummaryData execute() {
        final byte[] serializedStr = getRemoteHandlerFacade(UserSummaryRemoteManager.HANDLER).callXmlRpc("getGZippedSummary", userId);
        try {
          return XStreamUtil.unzipAndDeserializeObject(serializedStr);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

      public String describe() {
        return "getSummary";
      }
    });
  }

  @NotNull
  public Collection<String> getSuitableConfigurations(final Collection<String> urls) throws ECommunicationException {
    final Vector ids = remoteCall(new ServerCommand<Vector>() {
      public Vector execute() {
        return getRemoteHandlerFacade(RemoteVersionControlServer.VERSION_CONTROL_SERVER).callXmlRpc("getSuitableConfigurations", new Vector<String>(urls));
      }

      public String describe() {
        return "getSuitableConfigurations";
      }
    });
    final Set<String> buffer = new HashSet<String>(ids.size());
    for (final Object id : ids) {
      buffer.add(String.valueOf(id));
    }
    return buffer;
  }

  @NotNull
  public String getRemoteProtocolVersion() {
    return getRemoteHandlerFacade(RemoteAuthenticationServer.REMOTE_AUTH_SERVER).callXmlRpc("getServerVersion");
  }

  @NotNull
  public AddToQueueResult addToQueue(@NotNull final List<AddToQueueRequest> batch, @NotNull final String triggeredBy) throws ECommunicationException {
    return XStreamUtil.deserializeObject(
      remoteCall(new ServerCommand<String>() {
        public String execute() {
          return RemoteServerFacadeImpl.super.addToQueue(XStreamUtil.serializeObjects(batch), triggeredBy);
        }

        public String describe() {
          return "addToQueue";
        }
      }));
  }

  private static class RemoteHandlerFacade extends ClientXmlRpcExecutorFacade {
    public RemoteHandlerFacade(@NotNull XmlRpcTarget target, @NotNull String handlerName) {
      super(target, new ApplicationFacadeStub(), handlerName, new VersionCheckerStub());
    }
  }
  
  private Map<String, RemoteHandlerFacade> myRemoteFacades = new HashMap<String, RemoteHandlerFacade>();
  
  @NotNull
  private synchronized RemoteHandlerFacade getRemoteHandlerFacade(@NotNull String handlerName) {
    RemoteHandlerFacade facade = myRemoteFacades.get(handlerName);
    if (facade == null) {
      facade = new RemoteHandlerFacade(mySession, handlerName);
      myRemoteFacades.put(handlerName, facade);
    }

    return facade;
  }

  static class VersionCheckerStub implements VersionChecker {
    public void checkServerVersion() throws IncompatiblePluginError {
    }
  }

  static class ApplicationFacadeStub implements ApplicationFacade {
    public XmlRpcTarget.Cancelable createCancelable() {
      return null;
    }

    public void onProcessCanceled() {
    }
  }

  private static interface ServerCommand<T> {
    T execute();

    String describe();
  }

  private <T> T remoteCall(@NotNull ServerCommand<T> command) throws ECommunicationException {
    try {
      return command.execute();
    } catch (Exception e) {
      Debug.getInstance().error(CommandRunner.class, "Remote call failed: " + command.describe(), e);

      final String remote = getRemoteProtocolVersion();
      final String local = getLocalProtocolVersion();
      Debug.getInstance().debug(CommandRunner.class, String.format("Checking protocol compatibility. Found local=%s, remote=%s", local, remote));
      if (!remote.equals(local)) {
        throw new ECommunicationException(String.format(Messages.getString("CommandRunner.incompatible.plugin.error.message.pattern"), remote, local));
      }

      throw new ECommunicationException(e);
    }
  }

  private String getLocalProtocolVersion() {
    return ServerVersionHolder.getVersion().getPluginProtocolVersion();
  }
}
