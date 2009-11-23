package jetbrains.buildServer.commandline;

import java.util.*;
import jetbrains.buildServer.serverSide.impl.personal.PersonalPatchUtil;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.*;

/**
* @author kir
*/
class VcsRootMappingGenerator {
  private final List<MappingElement> myMappings = new ArrayList<MappingElement>();
  private final Set<String> myGeneratedLeftParts = new HashSet<String>();
  private final VcsRootBasedMappingProvider myMappingProvider;

  private final VcsRootEntry myCurrentEntry;
  private final SVcsRoot myVcsRoot;

  public VcsRootMappingGenerator(final VcsRootBasedMappingProvider mappingProvider, final VcsRootEntry vcsRootEntry) {
    myMappingProvider = mappingProvider;
    myCurrentEntry = vcsRootEntry;
    myVcsRoot = (SVcsRoot)vcsRootEntry.getVcsRoot();
  }

  public List<MappingElement> generateMappings() throws VcsException {

    final List<VcsClientMapping> vcsClientMappings = new ArrayList<VcsClientMapping>(myMappingProvider.getClientMapping(myVcsRoot));
    Collections.sort(vcsClientMappings);

    for (IncludeRule includeRule : myCurrentEntry.getCheckoutRules().getIncludeRules()) {

      for (VcsClientMapping mapping : vcsClientMappings) {

        if (filteredByRule(mapping, includeRule)) continue;

        final String leftPart = buildLeftPart(includeRule, mapping);
        if (myGeneratedLeftParts.contains(leftPart)) continue;
        myGeneratedLeftParts.add(leftPart);

        myMappings.add(new MappingElement(
          leftPart,
          buildRightPart(myVcsRoot, includeRule, mapping),
          MappingGenerator.makeDescription(myVcsRoot, includeRule)));
      }

    }
    Collections.reverse(myMappings);
    return myMappings;
  }

  private String buildLeftPart(final IncludeRule includeRule, final VcsClientMapping mapping) {
    return MappingGenerator.normalizePath(VcsSupportUtil.removeFromAddTo(mapping.getTargetPath(), includeRule));
  }

  private String buildRightPart(final SVcsRoot vcsRoot, final IncludeRule includeRule, final VcsClientMapping mapping) {
    String targetMappedTo = includeRule.map(mapping.getTargetPath());
    String rightPart;
    if (targetMappedTo != null) {
      rightPart = mapping.getVcsUrlInfo();
    }
    else {
      rightPart = mapping.getVcsUrlInfo() + "/" + includeRule.getFrom();
    }
    return vcsRoot.getVcsName() + PersonalPatchUtil.SEPARATOR + rightPart;
  }

  private boolean filteredByRule(final VcsClientMapping info2TargetPath, final IncludeRule includeRule) {
    return info2TargetPath.getTargetPath().length() != 0 && includeRule.map(info2TargetPath.getTargetPath()) == null;
  }
}
