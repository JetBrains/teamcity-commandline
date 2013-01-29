package jetbrains.buildServer.commandline;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.vcs.SVcsRoot;
import jetbrains.buildServer.vcs.VcsException;
import jetbrains.buildServer.vcs.VcsManager;
import jetbrains.buildServer.vcs.VcsRootEntry;
import jetbrains.vcs.api.VcsSettings;
import jetbrains.vcs.api.services.tc.MappingGeneratorService;
import jetbrains.vcs.api.services.tc.VcsMappingElement;

public class MappingGenerator {
  private final VcsManager myVcsManager;
  private final SBuildType myBuildType;
  private final List<VcsMappingElement> myMappings = new ArrayList<VcsMappingElement>();

  private VcsRootEntry myCurrentEntry;

  public MappingGenerator(final VcsManager vcsManager, final SBuildType buildType) {
    myVcsManager = vcsManager;
    myBuildType = buildType;
  }

  public List<VcsMappingElement> getMappings() {
    return myMappings;
  }

  public void generateVcsMapping() {

    for (VcsRootEntry entry : myBuildType.getVcsRootEntries()) {
      myCurrentEntry = entry;
      generateMappingForEntry();
    }
  }

  private void generateMappingForEntry() {
    try {
      // TODO: VcsEntry extends VcsSettings?
      final VcsSettings vcsSettings = ((SVcsRoot)myCurrentEntry.getVcsRoot()).createVcsSettings(myCurrentEntry.getCheckoutRules());
      final MappingGeneratorService mappingGenerator = myVcsManager.getVcsService(vcsSettings, MappingGeneratorService.class);

      if (mappingGenerator != null) {
        myMappings.addAll(mappingGenerator.generateMapping());
      }

    } catch (VcsException e) {
      Loggers.SERVER.warn(e);
      // TODO
    }
  }
}
