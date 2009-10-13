package jetbrains.buildServer.commandline;

import java.util.*;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.impl.personal.PersonalPatchUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.vcs.*;

public class MappingGenerator {
  private final VcsManager myVcsManager;
  private final SBuildType myBuildType;
  private final List<MappingElement> myMappings = new ArrayList<MappingElement>();

  private VcsRootEntry myCurrentEntry;

  public MappingGenerator(final VcsManager vcsManager, final SBuildType buildType) {
    myVcsManager = vcsManager;
    myBuildType = buildType;
  }

  public List<MappingElement> getMappings() {
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
      final VcsPersonalSupport personalSupport = getPersonalSupport();

      if (personalSupport instanceof VcsClientMappingProvider) {
        obtainMappingUsing((VcsClientMappingProvider)personalSupport);
      }
    } catch (VcsException e) {
      Loggers.SERVER.warn(e);
      // TODO
    }
  }

  private VcsPersonalSupport getPersonalSupport() {
    final String vcsName = (myCurrentEntry.getVcsRoot()).getVcsName();
    return myVcsManager.findVcsPersonalSupportByName(vcsName);
  }

  private void obtainMappingUsing(final VcsClientMappingProvider personalSupport) throws VcsException {
    if (personalSupport instanceof IncludeRuleBasedMappingProvider) {

      buildMappingForIncludeRules((IncludeRuleBasedMappingProvider)personalSupport);
    }
    else if (personalSupport instanceof VcsRootBasedMappingProvider) {

      buildMappingForVcsRoot((VcsRootBasedMappingProvider) personalSupport);
    }
  }

  private void buildMappingForVcsRoot(final VcsRootBasedMappingProvider mappingProvider) throws VcsException {
    VcsRootMappingGenerator generator = new VcsRootMappingGenerator(mappingProvider, myCurrentEntry);
    myMappings.addAll(generator.generateMappings());
  }


  private void buildMappingForIncludeRules(final IncludeRuleBasedMappingProvider mappingProvider) throws VcsException {
    final SVcsRoot vcsRoot = (SVcsRoot)myCurrentEntry.getVcsRoot();

    for (IncludeRule includeRule : myCurrentEntry.getCheckoutRules().getIncludeRules()) {

      final List<VcsClientMapping> vcsClientMappings =
        new ArrayList<VcsClientMapping>(mappingProvider.getClientMapping(vcsRoot, includeRule));
      Collections.sort(vcsClientMappings);
      Collections.reverse(vcsClientMappings);

      for (VcsClientMapping info2TargetPath : vcsClientMappings) {

        final String leftPart = normalizePath(info2TargetPath.getTargetPath());
        final String rightPart = vcsRoot.getVcsName() + PersonalPatchUtil.SEPARATOR +
                                 StringUtil.removeTailingSlash(info2TargetPath.getVcsUrlInfo());
        myMappings.add(new MappingElement(leftPart, rightPart, makeDescription(vcsRoot, includeRule)));
      }

    }
  }

  static String normalizePath(final String path) {
    return replaceEmptyWithDot(StringUtil.removeTailingSlash(path));
  }

  private static String replaceEmptyWithDot(final String result) {
    return "".equals(result) ? "." : result;
  }

  static String makeDescription(final SVcsRoot vcsRoot, final IncludeRule includeRule) {
    if ("".equals(includeRule.getFrom()) && "".equals(includeRule.getTo())) {
      return vcsRoot.getDescription();
    }
    return vcsRoot.getDescription() + "; " + includeRule.toDescriptiveString();
  }

}
