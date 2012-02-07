package jetbrains.buildServer.serverProxy;

import com.jetbrains.teamcity.ECommunicationException;
import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.TeamServerSummaryData;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public interface RemoteServerFacade {
  @NotNull
  List<ProjectData> getRegisteredProjects() throws ECommunicationException;

  @NotNull
  TeamServerSummaryData getSummaryData(final String userId) throws ECommunicationException;

  @NotNull
  Collection<String> getSuitableConfigurations(final Collection<String> urls) throws ECommunicationException;

  @NotNull
  String getRemoteProtocolVersion();

  @NotNull
  AddToQueueResult addToQueue(@NotNull List<AddToQueueRequest> batch, @NotNull String comment) throws ECommunicationException;
}
