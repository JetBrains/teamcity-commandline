package jetbrains.buildServer.serverProxy;

import com.jetbrains.teamcity.ECommunicationException;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.AddToQueueRequest;
import jetbrains.buildServer.AddToQueueResult;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.TeamServerSummaryData;
import org.jetbrains.annotations.NotNull;

public interface RemoteServerFacade {
  @NotNull
  List<ProjectData> getRegisteredProjects() throws ECommunicationException;

  @NotNull
  TeamServerSummaryData getSummaryData(final String userId) throws ECommunicationException;

  /**
   * @param urls file URLs to find match for
   * @return collection of internal build configuration IDs, which are suitable for these files
   */
  @NotNull
  Collection<String> getSuitableConfigurations(final Collection<String> urls) throws ECommunicationException;

  @NotNull
  String getRemoteProtocolVersion();

  @NotNull
  AddToQueueResult addToQueue(@NotNull List<AddToQueueRequest> batch, @NotNull String comment) throws ECommunicationException;
}
