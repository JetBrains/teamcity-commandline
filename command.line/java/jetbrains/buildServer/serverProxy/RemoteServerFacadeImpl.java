
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
  private final SessionXmlRpcTarget mySession;

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
      final ProjectData projectData = XStreamUtil.deserializeObject(typeData);
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
          if (serializedStr != null) {
            Debug.getInstance().debug(CommandRunner.class, "Error unzipping and deserializing response: " + new String(serializedStr));
          }

          throw new RuntimeException("Error unzipping and deserializing response: " + e.getMessage(), e);
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
  
  private final Map<String, RemoteHandlerFacade> myRemoteFacades = new HashMap<String, RemoteHandlerFacade>();
  
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

      try {
        final String remote = getRemoteProtocolVersion();
        final String local = getLocalProtocolVersion();
        Debug.getInstance().debug(CommandRunner.class, String.format("Checking protocol compatibility. Found local=%s, remote=%s", local, remote));
        if (!remote.equals(local)) {
          throw new ECommunicationException(String.format(Messages.getString("CommandRunner.incompatible.plugin.error.message.pattern"), remote, local));
        }
      } catch (Exception e1) {
        Debug.getInstance().error(CommandRunner.class, "Error checking server version", e1);
      }

      throw new ECommunicationException(e);
    }
  }

  private String getLocalProtocolVersion() {
    return ServerVersionHolder.getVersion().getPluginProtocolVersion();
  }
}