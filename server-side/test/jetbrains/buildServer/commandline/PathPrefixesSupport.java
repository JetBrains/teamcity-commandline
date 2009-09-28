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

  public static void register(final List<VcsUrlInfo2TargetPath> prefixes, VcsManagerEx vcsManager) {
    vcsManager.registerVcsSupport(new MockVcsSupport("mock") {
      @Override
      public VcsPersonalSupport getPersonalSupport() {
        return new MockPersonalSupport() {
          @Override
          public Collection<VcsUrlInfo2TargetPath> getPossiblePathPrefixes(@NotNull final VcsRoot vcsRoot,
                                                                           @NotNull final IncludeRule includeRule)
            throws VcsException {
            return prefixes;
          }
        };
      }
    });
  }

}
