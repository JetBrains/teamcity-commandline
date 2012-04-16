package jetbrains.buildServer.commandline;

import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.serverSide.impl.MockPersonalSupport;
import jetbrains.buildServer.serverSide.impl.MockVcsSupport;
import jetbrains.buildServer.vcs.*;
import org.jetbrains.annotations.NotNull;

/**
* Created by IntelliJ IDEA.
* User: kir
* Date: Sep 28, 2009
* Time: 7:15:20 PM
* To change this template use File | Settings | File Templates.
*/
class PathPrefixesSupport {
  public static MockVcsSupport registerIncludeRuleVcsMappingSupport(final List<VcsClientMapping> prefixes, VcsManagerEx vcsManager) {
    final MockVcsSupport support = new MockVcsSupport("mock") {
      @Override
      public VcsPersonalSupport getPersonalSupport() {
        return new IncludeRuleBasedMock(prefixes);
      }
    };
    vcsManager.registerVcsSupport(support);
    return support;
  }

  public static MockVcsSupport registerVcsRootMappingSupport(final List<VcsClientMapping> prefixes, VcsManagerEx vcsManager) {
    final MockVcsSupport support = new MockVcsSupport("mock") {
      @Override
      public VcsPersonalSupport getPersonalSupport() {
        return new VcsRootBasedMock(prefixes);
      }
    };
    vcsManager.registerVcsSupport(support);
    return support;
  }

  private static class IncludeRuleBasedMock extends MockPersonalSupport implements IncludeRuleBasedMappingProvider {
    private final Collection<VcsClientMapping> myPrefixes;

    private IncludeRuleBasedMock(final Collection<VcsClientMapping> prefixes) {
      this.myPrefixes = prefixes;
    }

    public Collection<VcsClientMapping> getClientMapping(@NotNull final VcsRoot vcsRoot,
                                                                     @NotNull final IncludeRule includeRule)
      throws VcsException {
      return myPrefixes;
    }

  }

  private static class VcsRootBasedMock extends MockPersonalSupport implements VcsRootBasedMappingProvider {
    private final Collection<VcsClientMapping> myPrefixes;

    private VcsRootBasedMock(final Collection<VcsClientMapping> prefixes) {
      this.myPrefixes = prefixes;
    }

    public Collection<VcsClientMapping> getClientMapping(@NotNull final VcsRoot vcsRoot) throws VcsException {
      return myPrefixes;
    }
  }
}
